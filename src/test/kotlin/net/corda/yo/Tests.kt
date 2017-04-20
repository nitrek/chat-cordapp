package net.corda.chat

import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.getOrThrow
import net.corda.core.node.services.unconsumedStates
import net.corda.node.utilities.databaseTransaction
import net.corda.testing.*
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals


class ChatTests {
    lateinit var net: MockNetwork
    lateinit var a: MockNetwork.MockNode
    lateinit var b: MockNetwork.MockNode

    @Before
    fun setup() {
        net = MockNetwork()
        val nodes = net.createSomeNodes(2)
        a = nodes.partyNodes[0]
        b = nodes.partyNodes[1]
        net.runNetwork()
    }

    @After
    fun tearDown() {
        net.stopNodes()
    }

    @Test
    fun messageTransactionMustBeWellFormed() {
        // A pre-made Yo to Bob.
        val yo = Message.State(ALICE, BOB, "Yo!")
        // A pre-made dummy state.
        val dummyState = object : ContractState {
            override val contract get() = DUMMY_PROGRAM_ID
            override val participants: List<CompositeKey> get() = listOf()
        }
        // A pre-made dummy command.
        class DummyCommand : TypeOnlyCommandData()
        // Tests.
        ledger {
            // input state present.
            transaction {
                input { dummyState }
                command(ALICE_PUBKEY) { Message.Send() }
                output { yo }
                this.failsWith("There can be no inputs when messaging other parties.")
            }
            // No command.
            transaction {
                output { yo }
                this.failsWith("")
            }
            // Wrong command.
            transaction {
                output { yo }
                command(ALICE_PUBKEY) { DummyCommand() }
                this.failsWith("")
            }
            // Command signed by wrong key.
            transaction {
                output { yo }
                command(MINI_CORP_PUBKEY) { Message.Send() }
                this.failsWith("The message must be signed by the sender.")
            }
            // Sending to yourself is not allowed.
            transaction {
                output { Message.State(ALICE, ALICE, "Yo!") }
                command(ALICE_PUBKEY) { Message.Send() }
                this.failsWith("No sending messages to yourself!")
            }
            transaction {
                output { yo }
                command(ALICE_PUBKEY) { Message.Send() }
                this.verifies()
            }
        }
    }

    @Test
    fun flowWorksCorrectly() {
        val yo = Message.State(a.info.legalIdentity, b.info.legalIdentity, "Yo!")
        val flow = MessageFlow(b.info.legalIdentity, "Yo!")
        val future = a.services.startFlow(flow).resultFuture
        net.runNetwork()
        val stx = future.getOrThrow()
        // Check yo transaction is stored in the storage service and the state in the vault.
        databaseTransaction(b.database) {
            val bTx = b.storage.validatedTransactions.getTransaction(stx.id)
            assertEquals(bTx, stx)
            print("$bTx == $stx")
            val bYo = b.vault.unconsumedStates<Message.State>().single().state.data
            // Strings match but the linearId's will differ.
            assertEquals(bYo.toString(), yo.toString())
            print("$bYo == $yo")
        }
    }
}
