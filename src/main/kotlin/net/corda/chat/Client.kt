package net.corda.chat

import com.google.common.net.HostAndPort
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import net.corda.client.rpc.CordaRPCClient
import org.slf4j.Logger
import rx.Observable

fun main(args: Array<String>) {
    ChatRPC().main(args)
}

private class ChatRPC {
    companion object {
        val logger: Logger = loggerFor<ChatRPC>()
    }
    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: TemplateClientRPC <node address:port>" }
        val nodeAddress = HostAndPort.fromString(args[0])
        val client = CordaRPCClient(nodeAddress)
        // Can be amended in the com.template.MainKt file.
        client.start("user1", "test")
        val proxy = client.proxy()
        // Grab all signed transactions and all future signed transactions.
        val (transactions: List<SignedTransaction>, futureTransactions: Observable<SignedTransaction>) =
                proxy.verifiedTransactions()
        // Log the existing TemplateStates and listen for new ones.
        futureTransactions.startWith(transactions).toBlocking().subscribe { transaction ->
            transaction.tx.outputs.forEach { output ->
                val state = output.data as Message.State
                logger.info(state.toString())
            }
        }
    }
}
