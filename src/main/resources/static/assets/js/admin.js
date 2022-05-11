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

function checkTeamMemberStatus(teamId) {
  $('#check-team-icon-' + teamId).hide();
  $('#check-team-spinner-' + teamId).show();
  window.location = '/team-check-members?teamId=' + teamId;
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
  $("#carclass-modal #classOrder").val($("#carClass" + ccIndex + "_order").val());
  const options = $("#carClass" + ccIndex + "_carIds").val();
  const array = $.parseJSON(options);
  $("#carclass-modal #carIds").val(array);
  $("#carclass-modal").modal('show');
}

function editBop(id, carId, carClassId) {
  $("#bop-modal-" + id + " #id").val(id)
  $("#bop-modal-" + id + " #carId").val(carId)
  $("#bop-modal-" + id + " #carClassId").val(carClassId)
  $("#bop-modal-" + id + " #carName").val($("#bop-table #carName-" + id).text())
  $("#bop-modal-" + id + " #maxFuel").val($("#bop-table #maxFuel-" + id).text())
  $("#bop-modal-" + id + " #weightPenalty").val($("#bop-table #weightPenalty-" + id).text())
  $("#bop-modal-" + id + " #enginePowerPercent").val($("#bop-table #enginePowerPercent-" + id).text())
  $("#bop-modal-" + id + " #fuelPercent").val($("#bop-table #fuelPercent-" + id).text())
  $("#bop-modal-" + id).modal('show');
}

function calcEffectiveFuel(carId) {
  const fuelPercent = $('#bop-modal-' + carId + ' #fuelPercent').val();
  const maxFuel = $('#bop-modal-' + carId + ' #maxFuel').val();
  const effectiveFuel = (maxFuel * fuelPercent) / 100;
  $('#bop-modal-' + carId + ' #effectiveFuel').text('(' + number_format(effectiveFuel, 2, '.', ',') + ')');
}

function calcWeight(carId) {
  const penalty = $('#bop-modal-' + carId + ' #weightPenalty').val();
  const weight = $('#bop-modal-' + carId + ' #weight').val();
  const effectiveWeight = parseInt(weight) - parseInt(penalty);
  $('#bop-modal-' + carId + ' #effectiveWeight').text('(' + effectiveWeight + ')');
}

function calcPower(carId) {
  const powerPercent = $('#bop-modal-' + carId + ' #enginePowerPercent').val();
  const power = $('#bop-modal-' + carId + ' #horsePower').val();
  const effectivePower = parseInt(power) + ((power * powerPercent) / 100);
  $('#bop-modal-' + carId + ' #effectivePower').text('(' + number_format(effectivePower, 2, '.', ',') + ')');
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
}

function editTeamMember(personIndex, teamId) {
  $("#staff-modal #teamId").val(teamId);
  editStaffPerson(teamId + '_' + personIndex);
}

function addTeamMember(teamId, irTeamId) {
  $("#staff-modal #teamId").val(teamId);
  $('#staff-modal #teamIrId').val(irTeamId);
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

function hideCompletedTasks() {
  const doFilter = $('#filterCompletedCheck').is(":checked");
  localStorage.setItem('filterCompletedTasks', doFilter);
  filterCompletedTasks(doFilter);
}

function initFilterCompletedCheck() {
  const doFilter = localStorage.getItem('filterCompletedTasks');
  if(doFilter === 'true') {
    $('#filterCompletedCheck').attr('checked', true);
    filterCompletedTasks(true);
  } else {
    $('#filterCompletedCheck').attr('checked', false);
    filterCompletedTasks(false);
  }
}

function filterCompletedTasks(doFilter) {
  if(doFilter) {
    $('#task-table .task-completed').each(function () {
          $(this).hide();
        }
    );
  } else {
    $('#task-table .task-completed').each(function () {
          $(this).show();
        }
    );
  }
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

function editSubsession(subsessionId) {
  $("#subsession-modal #id").val($("#subsessionId-" + subsessionId).val());
  $("#subsession-modal #sessionType").val($("#subSessionType-" + subsessionId).text());
  $("#subsession-modal #irSubsessionId").val($("#irSubsessionId-" + subsessionId).text());
  $("#subsession-modal #hours").val($("#durationHours-" + subsessionId).val());
  $("#subsession-modal #minutes").val($("#durationMinutes-" + subsessionId).val());
  $("#subsession-modal").modal('show');
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

function checkIracingTeamId() {
  $('#registration-form #check-icon').toggle();
  $('#registration-form #check-spinner').toggle();
  const teamId = $('#registration-form #iracingId').val();
  $.ajax({
    type: 'GET',
    dataType: 'json',
    url: '/rest/teamInfo/' + teamId,
    async: true,
    success: function (data) {
      if(data.teamId === 0) {
        $('#registration-form #teamName').val("");
      } else {
        const lastWhitespace = data.teamName.lastIndexOf(' ');
        if(lastWhitespace === -1) {
          $('#registration-form #teamName').val(data.teamName);
        } else {
          const baseName = data.teamName.substring(0, lastWhitespace);
          $('#registration-form #teamName').val(baseName);
          const qualifier = data.teamName.substring(lastWhitespace + 1);
          $('#registration-form #carQualifier').val(qualifier);
        }
      }
    },
    complete: function () {
      $('#registration-form #check-icon').toggle();
      $('#registration-form #check-spinner').toggle();
    }
  });
}

function showLocalTime() {
  $('.offset-time').each(function() {
    const dateTime = moment($(this).text(), "DD.MM.YYYY hh:mm ZZ");
    $(this).siblings().each(function() {
      $(this).text(dateTime.local().format("L LT"))
    })
  })
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

function setFromBrowser(timezone, locale) {
  selectTimezoneFromUtcOffset(timezone);
  selectUserCountry(locale);
}

function selectTimezoneFromUtcOffset(timezone) {
  if (!timezone) {
    const tz = moment().format("ZZ");
    $("#tz-select > option").each(function () {
      if (this.value === tz) {
        this.selected = true;
        $("#timezone").val($(this).attr("data-zone"));
      }
    });
  }
}

function selectUserCountry(locale) {
    if(!locale) {
      const userLang = navigator.language || navigator.userLanguage;
      $("#localeTag").val(userLang.split('-')[0]);
    }
}

function number_format (number, decimals, dec_point, thousands_sep) {
  number = (number + '').replace(/[^0-9+\-Ee.]/g, '');
  var n = !isFinite(+number) ? 0 : +number,
      prec = !isFinite(+decimals) ? 0 : Math.abs(decimals),
      sep = (typeof thousands_sep === 'undefined') ? ',' : thousands_sep,
      dec = (typeof dec_point === 'undefined') ? '.' : dec_point,
      s = '',
      toFixedFix = function (n, prec) {
        var k = Math.pow(10, prec);
        return '' + Math.round(n * k) / k;
      };
  // Fix for IE parseFloat(0.55).toFixed(0) = 0;
  s = (prec ? toFixedFix(n, prec) : '' + Math.round(n)).split('.');
  if (s[0].length > 3) {
    s[0] = s[0].replace(/\B(?=(?:\d{3})+(?!\d))/g, sep);
  }
  if ((s[1] || '').length < prec) {
    s[1] = s[1] || '';
    s[1] += new Array(prec - s[1].length + 1).join('0');
  }
  return s.join(dec);
}