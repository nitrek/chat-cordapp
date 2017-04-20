"use strict";

const app = angular.module("chatModule", ["ui.bootstrap"]);

app.controller("chatCtrl", function ($http, $interval) {
    const chatApp = this;
    const apiBaseURL = "/api/chat/";

    $http.get(apiBaseURL + "me").then((response) => chatApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => chatApp.peers = response.data.peers);

    chatApp.updateMessages = () => {
        $http.get(apiBaseURL + "messages").then((response) => {
            // We build an empty dictionary mapping peers to their messages.
            chatApp.messages = {};
            chatApp.peers.forEach(function (peer) {
                chatApp.messages[peer] = [];
            });

            // We fill the dictionary.
            response.data.forEach(function (data) {
                const message = data.state.data;
                let counterparty = message.target;
                let sender = "Me: ";
                if (message.origin != chatApp.thisNode) {
                    counterparty = message.origin;
                    sender = "Them: ";
                }

                // We display the messages in reverse chronological order.
                chatApp.messages[counterparty].unshift([sender, message.message]);
            });
        });
    };

    chatApp.send = (recipient) => {
        const sendMsgEndpoint = apiBaseURL + `message?target=${recipient}&message=${chatApp.form.message}`;
        $http.get(sendMsgEndpoint).then(() => {}, () => {});
        // We reset the text input.
        chatApp.form.message = "";
    };

    // We poll the server for new messages every 100 milliseconds.
    $interval(chatApp.updateMessages, 100);
});