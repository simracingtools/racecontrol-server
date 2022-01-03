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

function selectTrackForConfig() {
  var trackId = $("#trackSelector option:selected").val();
  var configId = $("#trackId").val();
  window.location = '/stockdata?activeTab=configs'
      + '&selectedTrackId=' + trackId
      + '&selectedTrackConfigId=' + configId;
}

function selectTimezoneFromUtcOffset(timezone) {
  if (!timezone) {
    var utcOffsetHours = moment().utcOffset() / 60;
    var tz = 'GMT';
    if (utcOffsetHours >= 0) {
      tz = tz + '+' + utcOffsetHours;
    } else {
      tz = tz + utcOffsetHours;
    }
    $("#timezone > option").each(function() {
      if (this.value === tz) {
        this.selected = true;
      }
    });
  }
}