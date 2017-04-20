![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Chat CorDapp

Chat with all your friends running Corda nodes!

## Pre-Requisites

You will need the following installed on your machine before you can start:

* Latest [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
  installed and available on your path.
* Latest version of [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
* git

## Getting Set Up

To get started, clone this repository with:

     git clone https://github.com/joeldudleyr3/chat-app.git

And change directories to the newly cloned repo:

     cd chat-app

## Building the Chat CorDapp:

**Unix:** 

     ./gradlew deployNodes

**Windows:**

     gradlew.bat deployNodes

## Running the Nodes:

Once the build finishes, change directories to the folder where the newly
built nodes are located:

**Kotlin:**

     cd build/nodes

**Java:**

     cd build/nodes

The Gradle build script will have created a folder for each node. You'll
see three folders, one for each node and a `runnodes` script. You can
run the nodes with:

**Unix:**

     ./runnodes

**Windows:**

    runnodes.bat

You should now have three Corda nodes running on your machine serving
the Chat CorDapp.

Six windows will open in the terminal - a node shell and a web server for each
node.

## Interacting with the CorDapp via the Web UI

The nodes can be found using the following port numbers output in the web server
terminal window or in the `build.gradle` file.

     NodeA: localhost:10007
     NodeB: localhost:10010

For each node, a web interface is available at localhost:100xx/web/chat.
Choose a counterparty in the left-hand list to start chatting!

## Interacting with the CorDapp via HTTP

The Chat CorDapp defines a couple of HTTP API end-points.

Sending a message:

    http://localhost:10007/api/chat/message?target=NodeB&message=HeyWhatsUp (From NodeA to NodeB)

Showing all your messages:

     http://localhost:10010/api/chat/messages (NodeB)
     
Finding out who you are:

    http://localhost:10010/api/chat/me (NodeB)

Finding out who you can chat with:

    http://localhost:10010/api/yo/peers (NodeA, NodeB)

## Using the RPC Client

Use the gradle command:

     ./gradlew runChatRPCNodeA
     
or 
     
     ./gradlew runChatRPCNodeB (for NodeB)

When running it should enumerate all previously received messages as well as show any new messages
when they are sent to you.

## Using the node shell

The node shell is a great way to test your CorDapps without having to create a user interface. 

When the nodes are up and running, use the following command to send a message to another node:

    flow start MessageFlow target: [NODE_NAME]
    
Where `NODE_NAME` is NodeA or NodeB. The space after the `:` is required. Note you can't sent a message to yourself because that's not cool.

To see all your messages use:

    run vaultAndUpdates

## Further reading

Tutorials and developer docs for CorDapps and Corda are
[here](https://docs.corda.net/).
