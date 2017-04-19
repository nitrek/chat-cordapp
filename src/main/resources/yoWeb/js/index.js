"use strict";

const app = angular.module('yoModule', ['ui.bootstrap']);

app.controller('yoCtrl', function($http) {
    const yoApp = this;
    const apiBaseURL = "http://localhost:10007/api/yo/";

    $http.get(apiBaseURL + "me").then((response) => yoApp.thisNode = response.data.me);
    $http.get(apiBaseURL + "peers").then((response) => {
        yoApp.peers = response.data.peers;
        yoApp.yos = {}
        yoApp.peers.forEach(function(peer) {
            yoApp.yos[peer] = [];
        });
        yoApp.updateYos();
        console.log(yoApp.yos)
    });

    yoApp.updateYos = () => {
        $http.get(apiBaseURL + "yos").then((response) => {
            response.data.forEach(function (data) {
                const yo = data.state.data;

                let counterparty = yo.target;
                let sender = "Me: ";
                if (yo.origin != yoApp.thisNode) {
                    counterparty = yo.origin;
                    sender = "Them: ";
                }

                yoApp.yos[counterparty].push([sender, yo.yo]);
            });
        });
    };
});