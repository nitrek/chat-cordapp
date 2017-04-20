"use strict";

const app = angular.module("yoModule", ["ui.bootstrap"]);

app.controller("yoCtrl", function ($http, $interval) {
    const yoApp = this;
    const apiBaseURL = "/api/yo/";

    $http.get(apiBaseURL + "me").then((response) => yoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => yoApp.peers = response.data.peers);

    yoApp.updateYos = () => {
        $http.get(apiBaseURL + "yos").then((response) => {
            // We build an empty dictionary mapping peers to their yos.
            yoApp.yos = {};
            yoApp.peers.forEach(function (peer) {
                yoApp.yos[peer] = [];
            });

            // We fill the dictionary.
            response.data.forEach(function (data) {
                const yo = data.state.data;
                let counterparty = yo.target;
                let sender = "Me: ";
                if (yo.origin != yoApp.thisNode) {
                    counterparty = yo.origin;
                    sender = "Them: ";
                }

                // We display the yos in reverse chronological order.
                yoApp.yos[counterparty].unshift([sender, yo.yo]);
            });
        });
    };

    yoApp.send = (recipient) => {
        const sendMsgEndpoint = apiBaseURL + `yo?target=${recipient}&message=${yoApp.form.message}`;
        $http.get(sendMsgEndpoint).then(() => {}, () => {});
        // We reset the text input.
        yoApp.form.message = "";
    };

    // We poll the server for new yos every 100 milliseconds.
    $interval(yoApp.updateYos, 100);
});