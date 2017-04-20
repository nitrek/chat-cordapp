"use strict";

const app = angular.module('yoModule', ['ui.bootstrap']);

app.controller('yoCtrl', function($http, $interval) {
    const yoApp = this;
    // TODO: Change this back to a relative path.
    const apiBaseURL = "http://localhost:10007/api/yo/";

    $http.get(apiBaseURL + "me").then((response) => yoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => {
        yoApp.peers = response.data.peers;
    });

    yoApp.updateYos = () => {
        $http.get(apiBaseURL + "yos").then((response) => {
            console.log(response);
            yoApp.yos = {}
            yoApp.peers.forEach(function(peer) {
                yoApp.yos[peer] = [];
            });

            response.data.forEach(function (data) {
                const yo = data.state.data;
                let counterparty = yo.target;
                let sender = "Me: ";
                if (yo.origin != yoApp.thisNode) {
                    counterparty = yo.origin;
                    sender = "Them: ";
                }

                yoApp.yos[counterparty].unshift([sender, yo.yo]);
            });
        });
    };

    yoApp.send = (recipient) => {
        const sendMsgEndpoint = apiBaseURL + `yo?target=${recipient}&message=${yoApp.form.message}`;
        $http.get(sendMsgEndpoint).then(() => {}, () => {});
        yoApp.form.message = "";
    }

    var promise = $interval(yoApp.updateYos, 100);
});