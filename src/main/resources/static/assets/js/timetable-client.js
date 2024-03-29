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
let stompClient = null;

function showRcBulletinDialog(carNumber, eventIndex) {
  $("#carNo").val(carNumber);
  if(eventIndex > -1) {
    let eventTime = $("#event-time-" + eventIndex).text();
    const dotIndex = eventTime.indexOf('.');
    if(dotIndex > 0) {
      eventTime = eventTime.substr(0, dotIndex);
    }
    $("#sessionTime").val(eventTime);
  }
  updateRcBulletinPreview(false);
  $("#rc-bulletin-model").modal('show');
}

function violationSelect() {
  const violationId = $("#violationId").val();
  if( violationId === "0") {
    $("#penalty-params").attr("style", "display: none;");
    updateRcBulletinPreview(false);
  } else {
    $("#penalty-params").attr("style", "");
    const codes = $("#violation-" + violationId).attr("data");
    let firstVisibleSelected = false;
    $("#selectedPenaltyCode option").each(function () {
      if(codes.indexOf($(this).val()) >= 0) {
        $(this).show();
        if(!firstVisibleSelected) {
          $(this).attr('selected',true);
          firstVisibleSelected = true;
          if($(this).attr("data") === "true") {
            $("#penaltySecondsLabel").show();
            $("#penaltySeconds").show();
          } else {
            $("#penaltySecondsLabel").hide();
            $("#penaltySeconds").hide();
          }
        }
      } else {
        $(this).hide();
      }
    })
    updateRcBulletinPreview(true);
  }
}

function penaltySelect() {
  if($("#selectedPenaltyCode option:selected").attr("data") === "true") {
    $("#penaltySecondsLabel").show();
    $("#penaltySeconds").show();
  } else {
    $("#penaltySecondsLabel").hide();
    $("#penaltySeconds").hide();
  }

  updateRcBulletinPreview(true);
}

function messageChange() {
  if($("#violationId").val() === "0") {
    updateRcBulletinPreview(false);
  } else {
    updateRcBulletinPreview(true);
  }
}

function penaltySecondsChange() {
  updateRcBulletinPreview(true);
}

function sessionTimeChange() {
  updateRcBulletinPreview($("#selectedPenaltyCode").is(":visible"));
}

function updateRcBulletinPreview(checkViolation) {
  let preview = $("#sessionType").text().substr(0, 1);
  preview += $("#bulletinNo").val() + " ";
  preview += $("#sessionTime").val();
  preview += " #" + $("#carNo").val();
  if(checkViolation) {
    preview += " - " + $("#violationId option:selected").closest("optgroup").attr("data");
    preview += " " + $("#violationId option:selected").text();
    preview += " " + $("#selectedPenaltyCode option:selected").text();
    if($("#selectedPenaltyCode option:selected").attr("data") === "true") {
      preview += " " + $("#penaltySeconds").val() + " sec";
    }
  }
  if($("message").val() !== "") {
    preview += " - " + $("#message").val();
  }
  $("#bulletin-preview").text(preview);
}

function connect() {
  let socket = new SockJS('/timingclient');
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function (frame) {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/user/timing/client-ack', function(message) {
      console.log(message);
    });
    const sessionId = $("#sessionId").val();
    stompClient.subscribe('/timing/' + sessionId + '/reload', function (message) {
      console.log(message);
      reloadPage();
    });
    stompClient.subscribe('/timing/' + sessionId + '/driver', function (message) {
      const jsonMessage = JSON.parse(message.body);
      console.log(jsonMessage);
      showDriverData(jsonMessage);
    });
    stompClient.subscribe('/timing/' + sessionId + '/event', function (message) {
      const jsonMessage = JSON.parse(message.body);
      console.log(jsonMessage);
      showEventData(jsonMessage);
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

function sendEventFilterChange(userId, event, checked) {
  stompClient.send("/app/eventfilter", {}, JSON.stringify({'messageType': 'eventFilter', 'sessionId': $("#sessionId").val(), 'userId': userId, 'eventType': event, 'checked': checked}));
}

function reloadPage() {
  if($("#rc-bulletin-model").is(":visible")) {
    console.log("bulletin modal visible, suppress page reload")
  } else {
    window.location.reload();
  }
}

function showDriverData(message) {
  let baseId = '#driver-' + message.iracingId;

  $(baseId + "-name").attr("class", message.name.cssClassString);
  $(baseId + "-drivingTime")
      .attr("class", message.drivingTime.cssClassString)
      .text(message.drivingTime.value);
}

function showEventData(message) {
  if($('#selectedTeamId').val() !== 'All' && $('#selectedTeamId').val() !== message.teamId) {
    console.log("Discarding event for team " + message.teamId);
    return;
  }

  const showEvent = $('#' + message.eventType.value).prop('checked')
  if(!showEvent) {
    console.log("Discard event type " + message.eventType.value);
    return;
  }

  const tableSize = $('#eventTableSize').val()
  for(let i = tableSize - 1; i >= 0; i--) {
    if(i > 0) {
      $('#event-time-' + i).text($('#event-time-' + (i - 1)).text())
          .attr('onclick', $('#event-time-' + (i - 1)).attr('onclick'));
      $('#event-type-' + i).text($('#event-type-' + (i - 1)).text())
          .attr("class", $('#event-type-' + (i - 1)).attr('class'))
          .attr('onclick', $('#event-type-' + (i - 1)).attr('onclick'));
      $('#event-lap-' + i).text($('#event-lap-' + (i - 1)).text());
      $('#event-carno-' + i).text($('#event-carno-' + (i - 1)).text());
      $('#event-drivername-' + i).text($('#event-drivername-' + (i - 1)).text());
      $('#event-teamname-' + i).text($('#event-teamname-' + (i - 1)).text());
      $('#event-carname-' + i).text($('#event-carname-' + (i - 1)).text());
    } else {
      $('#event-time-0').text(message.eventTime.value)
          .attr("onclick", 'sendRcTimestamp(' + message.sessionMillis + ', ' + message.teamId + ', ' + $('#userId').val() + ');');
      $('#event-type-0').text(message.eventType.value)
          .attr("onclick", 'sendRcTimestamp(' + message.sessionMillis + ', ' + message.teamId + ', ' + $('#userId').val() + ');')
          .attr("class", message.eventType.cssClassString);
      $('#event-lap-0').text(message.lap.value);
      $('#event-carno-0').text(message.carNo.value);
      $('#event-drivername-0').text(message.driverName.value);
      $('#event-teamname-0').text(message.teamName.value);
      $('#event-carname-0').text(message.carName.value);
    }
  }
}

