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

function saveUser(index) {
  window.location = '/savesiteuser?userId=' + $("#userId-" + index).val()
      + '&role=' + $("#role-" + index).val()
      + '&enabled=' + $("#enabled-" + index).prop('checked')
      + '&locked=' + $("#locked-" + index).prop('checked')
      + '&expired=' + $("#expired-" + index).prop('checked');
}

function confirmUserRemove(index) {
  $("#user-remove-confirm-" + index).modal('show');
}

function confirmSessionRemove(index) {
  $("#session-remove-confirm-" + index).modal('show');
}

function confirmCarClassRemove(index) {
  $("#carclass-remove-confirm-" + index).modal('show');
}

function confirmStaffRemove(index) {
  $("#staff-remove-confirm-" + index).modal('show');
}

function confirmMemberRemove(index, team) {
  $("#staff-remove-confirm-" + index + team).modal('show');
}

function confirmStateRemove(id) {
  $("#state-remove-confirm-" + id).modal('show');
}

function personSelected(data) {
  $("#staff-modal #iracingId").val(data.iracingId);
  $("#staff-modal #name").val(data.value);
  $("#staff-modal #leagueMember").val(data.leagueMember);
  $("#staff-modal #registered").val(data.registered);
  $("#staff-modal #iRacingChecked").val('true');
  $("#staff-modal #check-ir-id").prop("disabled",true);
}

function selectTrackForConfig() {
  var trackId = $("#trackSelector option:selected").val();
  var configId = $("#trackId").val();
  window.location = '/stockdata?activeTab=configs'
      + '&selectedTrackId=' + trackId
      + '&selectedTrackConfigId=' + configId;
}

function editCarClass(ccIndex) {
  $("#carclass-modal #id").val($("#carClass" + ccIndex + "_id").val());
  $("#carclass-modal #name").val($("#carClass" + ccIndex + "_name").val());
  $("#carclass-modal #slots").val($("#carClass" + ccIndex + "_slots").val());
  $("#carclass-modal #wildcards").val($("#carClass" + ccIndex + "_wildcards").val());
  const options = $("#carClass" + ccIndex + "_carIds").val();
  const array = $.parseJSON(options);
  $("#carclass-modal #carIds").val(array);
  $("#carclass-modal").modal('show');
}

function editStaffPerson(personIndex) {
  $("#staff-modal #id").val($("#staff" + personIndex + "_id").val());
  $("#staff-modal #iracingId").val($("#staff" + personIndex + "_iracingId").val());
  $("#staff-modal #eventId").val($("#staff" + personIndex + "_eventId").val());
  $("#staff-modal #name").val($("#staff" + personIndex + "_name").val());
  $("#staff-modal #role").val($("#staff" + personIndex + "_role").val());
  $("#staff-modal #registered").val($("#staff" + personIndex + "_registered").val());
  $("#staff-modal #leagueMember").val($("#staff" + personIndex + "_leagueMember").val());
  $("#staff-modal #iracingChecked").val($("#staff" + personIndex + "_iracingChecked").val());
  $("#staff-modal #save-text").show();
  $("#staff-modal #save-spinner").hide();
  $("#staff-modal").modal('show');
}

function submitStaffModal() {
  $("#staff-modal #save-text").hide();
  $("#staff-modal #save-spinner").show();
  $("#staff-modal #staff-modal-form").submit();
}

function editTeamMember(personIndex, teamId) {
  $("#staff-modal #teamId").val(teamId);
  editStaffPerson(teamId + '_' + personIndex);
}

function addTeamMember(teamId) {
  $("#staff-modal #teamId").val(teamId);
  $("#staff-modal #iracingId").val("");
  $("#staff-modal #name").val("");
  $("#staff-modal #role").val("");
  $("#staff-modal #registered").val("false");
  $("#staff-modal #leagueMember").val("false");
  $("#staff-modal #iracingChecked").val("false");
  $("#staff-modal #save-text").show();
  $("#staff-modal #save-spinner").hide();
  $("#staff-modal").modal('show');
}

function editRegisteredCar(teamId) {
  $("#car-modal-" + teamId + " #teamId").val(teamId);
  $("#car-modal-" + teamId + " #carId").val($("#carId-" + teamId).val());
  const checked = $("#wildcard-" + teamId).val();
  if(checked === "true") {
    $("#car-modal-" + teamId + " #useWildcard1").prop("checked", true);
  } else {
    $("#car-modal-" + teamId + " #useWildcard1").prop("checked", false);
  }
  $("#car-modal-" + teamId).modal('show');
}

function editWorkflowAction(actionId) {
  $("#TeamRegistration-modal-" + actionId + " #id").val($("#action-tr-" + actionId + " #id").val());
  $("#TeamRegistration-modal-" + actionId + " #eventId").val($("#action-tr-" + actionId + " #eventId").val());
  $("#TeamRegistration-modal-" + actionId + " #workflowItemId").val($("#action-tr-" + actionId + " #workflowItemId").val());
  $("#TeamRegistration-modal-" + actionId + " #message").val($("#action-tr-" + actionId + " #editActionMessage").val());
  changeFormGroupDisplay(actionId);
  $("#TeamRegistration-modal-" + actionId).modal('show');
}

function changeFormGroupDisplay(actionId) {
  const stateKey = $("#TeamRegistration-modal-" + actionId + " #targetStateKey").val();
  $("#TeamRegistration-modal-" + actionId + " #targetStateKey > option").each(function() {
    if(this.value === stateKey) {
      $("#TeamRegistration-modal-" + actionId + " #targetStateKey").prop("style", $(this).attr("style"));
    }
  });
}

function changeTeamActionSelectDisplay(actionId) {
  const stateKey = $("#teamActionSelect-" + actionId + " #targetStateKey").val();
  $("#teamActionSelect-" + actionId + " #targetStateKey > option").each(function() {
    if(this.value === stateKey) {
      $("#teamActionSelect-" + actionId + " #targetStateKey").prop("style", $(this).attr("style"));
    }
  });
}

function selectNotMachingItems() {
  $("#discord-modal #selectedItems option").each(function() {
    const optionStyle = $(this).prop("class");
    if( optionStyle === "text-danger") {
      $(this).prop("selected", true);
    }
  });
}

function submitDiscordCleanupForm() {
  $("#discord-modal .tab-content > .active form").submit();
}

function editWorkflowState(workflowIndex, entryIndex) {
  $("#workflow-modal #id").val($("#workflow" + workflowIndex + entryIndex + "_id").val());
  $("#workflow-modal #workflowName").val($("#workflow" + workflowIndex + entryIndex + "_workflowName").val());
  $("#workflow-modal #stateKey").val($("#workflow" + workflowIndex + entryIndex + "_stateKey").text());
  $("#workflow-modal #description").val($("#workflow" + workflowIndex + entryIndex + "_desc").text());
  const followUps = $("#workflow" + workflowIndex + entryIndex + "_followUpIds").val();
  $("#workflow-modal #followUpIds").val($.parseJSON(followUps));
  const roles = $("#workflow" + workflowIndex + entryIndex + "_dutyRoleIndices").val();
  $("#workflow-modal #dutyRoleIndices").val($.parseJSON(roles));
  $("#workflow-modal #color").val($("#workflow" + workflowIndex + entryIndex + "_color").val());
  $("#workflow-modal #textColor").val($("#workflow" + workflowIndex + entryIndex + "_textColor").val());
  const initialState = $("#workflow" + workflowIndex + entryIndex + "_initialState").val();
  if(initialState === "true") {
    $("#workflow-modal #initialState1").prop("checked", true);
  } else {
    $("#workflow-modal #initialState1").prop("checked", false);
  }
  const inActive = $("#workflow" + workflowIndex + entryIndex + "_inActive").val();
  if(inActive === "true") {
    $("#workflow-modal #inActive1").prop("checked", true);
  } else {
    $("#workflow-modal #inActive1").prop("checked", false);
  }

  $("#workflow-modal").modal('show');
}

function checkIRacingLeagueId() {
  $("#irLeagueName").val("");
  $.ajax({
    type: "GET",
    dataType: "json",
    url: "/rest/leagueInfo/" + $("#irLeagueID").val(),
    success: function (data) {
      $("#irLeagueID").val(data.leagueId);
      $("#irLeagueName").val(data.leagueName);
    }
  });
}

function checkIRacingMemberId() {
  $("#staff-modal #name").val("");
  $("#staff-modal #check-icon").hide();
  $("#staff-modal #check-spinner").show();
  $.ajax({
    type: "GET",
    dataType: "json",
    url: "/rest/memberInfo/" + $("#staff-modal #iracingId").val(),
    success: function (data) {
      if(data.value === "") {
        $("#staff-modal #iRacingChecked").val('false');
      } else {
        $("#staff-modal #iRacingChecked").val('true');
      }
      $("#staff-modal #name").val(data.value);
      $("#staff-modal #iracingId").val(data.iracingId);
      $("#staff-modal #leagueMember").val(data.leagueMember);
      $("#staff-modal #registered").val(data.registered);

      $("#staff-modal #check-icon").show();
      $("#staff-modal #check-spinner").hide();
    }
  });
}

function showLocalTime() {
  const regCloses = moment($("#regClosesTime").text(), "DD.MM.YYYY hh:mm ZZ");
  const regOpens = moment($("#regOpensTime").text(), "DD.MM.YYYY hh:mm ZZ");

  $("#regClosesLocal").text(regCloses.local().format("L LT"))
  $("#regOpensLocal").text(regOpens.local().format("L LT"))
}

function registrationOpenCountdown() {
  const regOpens = moment($("#regOpensTime").text(), "DD.MM.YYYY hh:mm ZZ");
  if(moment().isBefore(regOpens)) {
    timeLeft = moment.duration(regOpens.diff(moment()));
    $("#regOpensCountdown").text(timeLeft.days() + " d " + timeLeft.hours() + " h " + timeLeft.minutes() + " m " + timeLeft.seconds() + " s");
  } else {
    $("#regOpensCountdown").text("");
  }
}

function registrationClosedCountdown() {
  const regCloses = moment($("#regClosesTime").text(), "DD.MM.YYYY hh:mm ZZ");
  const regOpens = moment($("#regOpensTime").text(), "DD.MM.YYYY hh:mm ZZ");
  if(moment().isAfter(regOpens) && moment().isBefore(regCloses)) {
    var timeLeft = moment.duration(regCloses.diff(moment()));
    $("#regClosesCountdown").text(timeLeft.days() + " d " + timeLeft.hours() + " h " + timeLeft.minutes() + " m " + timeLeft.seconds() + " s");
  } else {
    $("#regClosesCountdown").text("");
  }
}

function setTimezoneId() {
  let data = "UTC";
  $("#tz-select > option").each(function() {
    if (this.selected === true) {
      data = $(this).attr("data-zone");
    }
  });

  $("#timezone").val(data);
}

function selectTimezoneFromUtcOffset(timezone) {
  if (!timezone) {
    const tz = moment().format("ZZ");
    $("#tz-select > option").each(function() {
      if (this.value === tz) {
        this.selected = true;
        $("#timezone").val($(this).attr("data-zone"));
      }
    });
  }
}