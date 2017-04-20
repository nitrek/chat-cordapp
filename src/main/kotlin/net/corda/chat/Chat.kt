package net.corda.chat

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Party
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.keys
import net.corda.core.flows.FlowLogic
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.flows.FinalityFlow
import java.security.PublicKey
import java.util.function.Function
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val SERVICE_NODE_NAMES = listOf("Controller", "NetworkMapService")

// API.
@Path("chat")
class ChatApi(val services: CordaRPCOps) {
    private val myLegalName: String = services.nodeIdentity().legalIdentity.name

    @GET
    @Path("message")
    @Produces(MediaType.APPLICATION_JSON)
    fun message(@QueryParam(value = "target") target: String, @QueryParam(value = "message") message: String): Response {
        val messageTo = services.partyFromName(target) ?: throw IllegalArgumentException("Unknown party name.")
        services.startFlowDynamic(MessageFlow::class.java, messageTo, message).returnValue.get()
        return Response.status(Response.Status.CREATED).entity("You just sent a message to ${messageTo.name}").build()
    }

    @GET
    @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    fun messages() = services.vaultAndUpdates().first.filter { it.state.data is Message.State }

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun me() = mapOf("me" to myLegalName)

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun peers() = mapOf("peers" to services.networkMapUpdates().first
            .map { it.legalIdentity.name }
            .filter { it != myLegalName && it !in SERVICE_NODE_NAMES })
}

// Flow.
class MessageFlow(val target: Party, val message: String): FlowLogic<SignedTransaction>() {
    override val progressTracker: ProgressTracker = tracker()
    companion object {
        object CREATING : ProgressTracker.Step("Creating a new Message!")
        object VERIFYING : ProgressTracker.Step("Verifying the Message!")
        object SENDING : ProgressTracker.Step("Sending the Message!")
        fun tracker() = ProgressTracker(CREATING, VERIFYING, SENDING)
    }
    @Suspendable
    override fun call(): SignedTransaction {
        val me = serviceHub.myInfo.legalIdentity
        val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
        progressTracker.currentStep = CREATING
        val signedMessage = TransactionType.General.Builder(notary)
                .withItems(Message.State(me, target, message), Command(Message.Send(), listOf(me.owningKey)))
                .signWith(serviceHub.legalIdentityKey)
                .toSignedTransaction(true)
        progressTracker.currentStep = VERIFYING
        signedMessage.tx.toLedgerTransaction(serviceHub).verify()
        progressTracker.currentStep = SENDING
        return subFlow(FinalityFlow(signedMessage, setOf(me, target))).single()
    }
}

// Contract and state.
class Message : Contract {
    class Send : TypeOnlyCommandData()
    override val legalContractReference: SecureHash = SecureHash.sha256("Yo!")
    override fun verify(tx: TransactionForContract) = requireThat {
        val command = tx.commands.requireSingleCommand<Send>()
        "There can be no inputs when messaging other parties." by (tx.inputs.isEmpty())
        "There must be one output: The message!" by (tx.outputs.size == 1)
        val message = tx.outputs.single() as State
        "No sending messages to yourself!" by (message.target != message.origin)
        "The message must be signed by the sender." by (message.origin.owningKey == command.signers.single())
    }

    data class State(val origin: Party,
                     val target: Party,
                     val message: String,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {
        override val participants: List<CompositeKey> get() = listOf(origin.owningKey, target.owningKey)
        override val contract get() = Message()
        override fun isRelevant(ourKeys: Set<PublicKey>) = ourKeys.intersect(participants.keys).isNotEmpty()
        override fun toString() = "${origin.name} -> ${target.name}: $message"
    }
}

// Plugin.
class ChatPlugin : CordaPluginRegistry() {
    override val webApis = listOf(Function(::ChatApi))
    override val requiredFlows = mapOf(MessageFlow::class.java.name to
            setOf(Party::class.java.name, String::class.java.name))
    override val servicePlugins: List<Function<PluginServiceHub, out Any>> = listOf()
    override val staticServeDirs = mapOf("chat" to javaClass.classLoader.getResource("chatWeb").toExternalForm())
}