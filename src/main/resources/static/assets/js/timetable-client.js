/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
var stompClient = null;

function connect() {
  var socket = new SockJS('/timingclient');
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function (frame) {
    // setConnected(true);
    console.log('Connected: ' + frame);
    stompClient.subscribe('/user/timing/client-ack', function(message) {
      var jsonMessage = JSON.parse(message.body);
      console.log(message);
      //showSessionData(jsonMessage);
    });
    var sessionId = $("#sessionId").val();
    stompClient.subscribe('/timing/' + sessionId + '/reload', function (message) {
      // var jsonMessage = JSON.parse(message.body);
      console.log(message);
      reloadPage();
    });
    stompClient.subscribe('/timing/' + sessionId + '/driver', function (message) {
      var jsonMessage = JSON.parse(message.body);
      console.log(jsonMessage);
      showDriverData(jsonMessage);
    });
    sendConnectAck();
  }, function (frame) {
    console.log('Error: ' + frame);
  });
}

function sendConnectAck() {
  stompClient.send("/app/timingclient", {}, JSON.stringify({'sessionId': $("#sessionId").val(), 'text': 'Hello'}));
}

function sendRcTimestamp(millis, driverId, userId) {
  stompClient.send("/app/rctimestamp", {}, JSON.stringify({'messageType': 'replayTime', 'timestamp': millis, 'driverId': driverId, 'userId': userId}));
}

function reloadPage() {
  console.log("reload page")
  window.location.reload();
}

function showDriverData(message) {
  var baseId = '#driver-' + message.iracingId;

  $(baseId + "-name").attr("class", message.name.cssClassString);
  $(baseId + "-drivingTime")
      .attr("class", message.drivingTime.cssClassString)
      .text(message.drivingTime.value);
}
  // function localTime(zonedTime) {
  //   return moment(zonedTime, 'HH:mm:ssZZ').local().format('HH:mm:ss')
  // }

