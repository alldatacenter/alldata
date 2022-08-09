/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var dataUtils = require('utils/data_manipulation');

/**
 * Transitional list of timezones (used to create list of shownTimezone @see shownTimezone)
 *
 * <code>utcOffset</code> - offset-value (0, 180, 240 etc)
 * <code>formattedOffset</code> - formatted offset-value ('+00:00', '-02:00' etc)
 * <code>value</code> - timezone's name (like 'Europe/Athens')
 * <code>region</code> - timezone's region (for 'Europe/Athens' it will be 'Europe')
 * <code>city</code> - timezone's city (for 'Europe/Athens' it will be 'Athens')
 *
 * @typedef {{utcOffset: number, formattedOffset: string, value: string, region: string, city: string}} formattedTimezone
 */

/**
 * List of timezones used in the user's settings popup
 *
 * <code>utcOffset</code> - offset-value (0, 180, 240 etc)
 * <code>value</code> - string like '120180|Europe'
 * <code>label</code> - string like '(UTC+02:00) Europe / Athens, Kiev, Minsk'
 * <code>zones</code> - list of zone-objects from <code>moment.tz</code> included to the <code>value</code>
 *
 * @typedef {{utcOffset: number, label: string, value: string, label: string, zones: object[]}} shownTimezone
 */

module.exports = Em.Object.create({

  /**
   * @type {shownTimezone[]}
   * @readOnly
   */
  timezones: [],

  /**
   * Map of <code>timezones</code>
   * Key - timezone value (like '(UTC+01:00) Region / City1, City2')
   * Value - zone-object
   *
   * @type {object}
   * @readOnly
   */
  timezonesMappedByValue: function () {
    var ret = {};
    this.get('timezones').forEach(function (tz) {
      ret[tz.value] = tz;
    });
    return ret;
  }.property('timezones.[]'),

  init: function () {
    this.set('timezones', this._parseTimezones());
    return this._super();
  },

  /**
   * Load list of timezones from moment.tz
   * Zones "Etc/*" and abbreviations are excluded
   *
   * @returns {string[]}
   */
  getAllTimezoneNames: function () {
    return moment.tz.names().filter(function (timeZoneName) {
      return timeZoneName.indexOf('Etc/') !== 0 && timeZoneName !== timeZoneName.toUpperCase();
    });
  },

  /**
   * Try detect user's timezone using timezoneOffset and moment.tz
   * Checking current year January and July offsets
   * If <code>region</code> is provided, timezone for it is returned and not first valid
   *
   * @param {string} [region] preferred region (may be 'Europe', 'America', 'Africa', 'Asia' etc)
   * @returns {string}
   */
  detectUserTimezone: function (region) {
    region = (region || '').toLowerCase();
    var currentYear = new Date().getFullYear();
    var jan = new Date(currentYear, 0, 1);
    var jul = new Date(currentYear, 6, 1);
    var janOffset = jan.getTimezoneOffset();
    var julOffset = jul.getTimezoneOffset();
    var timezones = this.get('timezones');

    var validZones = [];

    for (var i = 0; i < timezones.length; i++) {
      var zones = timezones[i].zones;
      for (var j = 0; j < zones.length; j++) {
        var zone = moment.tz.zone(zones[j].value);
        if ((zone.offset(jan) === janOffset) && (zone.offset(jul) === julOffset)) {
          validZones.pushObject(timezones[i].value);
        }
      }
    }
    if (validZones.length) {
      if (region) {
        for (i = 0; i < validZones.length; i++) {
          if (validZones[i].toLowerCase().indexOf(region) !== -1) {
            return validZones[i];
          }
        }
        // Timezone for `region` wasn't found
        return validZones[0];
      }
      // `region` isn't provided, so return first valid timezone
      return validZones[0];
    }
    // are you from Venus?
    return '';
  },

  /**
   * Reformat timezones list and sort it by utcOffset and timeZoneName
   *
   * @private
   * @method _parseTimezones
   * @returns {shownTimezone[]}
   */
  _parseTimezones: function () {
    var currentYear = new Date().getFullYear();
    var jan = new Date(currentYear, 0, 1);
    var jul = new Date(currentYear, 6, 1);
    var zones = this.getAllTimezoneNames().map(function (timeZoneName) {
      var zone = moment(new Date()).tz(timeZoneName);
      var z = moment.tz.zone(timeZoneName);
      var offset = zone.format('Z');
      var regionCity = timeZoneName.split('/');
      var region = regionCity[0];
      var city = regionCity.length === 2 ? regionCity[1] : '';
      return {
        groupByKey: z.offset(jan) + '' + z.offset(jul),
        utcOffset: zone.utcOffset(),
        formattedOffset: offset,
        value: timeZoneName,
        region: region,
        city: city.replace(/_/g, ' ')
      };
    }).sort(function (zoneA, zoneB) {
      if (zoneA.utcOffset === zoneB.utcOffset) {
        if (zoneA.value === zoneB.value) {
          return 0;
        }
        return zoneA.value < zoneB.value ? -1 : 1;
      } else {
        if(zoneA.utcOffset === zoneB.utcOffset) {
          return 0;
        }
        return zoneA.utcOffset < zoneB.utcOffset ? -1 : 1;
      }
    });

    return this._groupTimezones(zones);
  },

  /**
   * Group timezones by <code>groupByKey</code>
   * Group timezones in the each group by <code>region</code>
   * <code>city</code> for each regions are joined into string 'city1, city2, city3' (empty cities and abbreviations are ignored)
   * Example:
   * <pre>
   *   var zones = [
   *    {groupByKey: '1', formattedOffset: '+01:00', value: 'a/Aa', region: 'a', city: 'Aa'},
   *    {groupByKey: '1', formattedOffset: '+01:00', value: 'a/Bb', region: 'a', city: 'Bb'},
   *    {groupByKey: '2', formattedOffset: '+02:00', value: 'a/Cc', region: 'a', city: 'Cc'},
   *    {groupByKey: '2', formattedOffset: '+02:00', value: 'a/Dd', region: 'a', city: 'Dd'},
   *    {groupByKey: '1', formattedOffset: '+01:00', value: 'b/Ee', region: 'b', city: 'Ee'},
   *    {groupByKey: '1', formattedOffset: '+01:00', value: 'b/Ff', region: 'b', city: 'Ff'},
   *    {groupByKey: '2', formattedOffset: '+02:00', value: 'b/Gg', region: 'b', city: 'Gg'},
   *    {groupByKey: '2', formattedOffset: '+02:00', value: 'b/Hh', region: 'b', city: 'Hh'},
   *    {groupByKey: '2', formattedOffset: '+02:00', value: 'b/II', region: 'b', city: 'II'}, // will be ignored, because city is abbreviation
   *    {groupByKey: '2', formattedOffset: '+02:00', value: 'b',    region: 'b', city: ''  }  // will be ignored, because city is empty
   *   ];
   *   var groupedZones = _groupTimezones(zones);
   *   // groupedZones is:
   *   [
   *    {utcOffset: 1, label: '(UTC+01:00) a / Aa, Bb', value: '1|a'},
   *    {utcOffset: 1, label: '(UTC+01:00) b / Ee, Ff', value: '1|b'},
   *    {utcOffset: 2, label: '(UTC+02:00) a / Cc, Dd', value: '2|a'},
   *    {utcOffset: 2, label: '(UTC+02:00) b / Gg, Hh', value: '2|b'}
   *   ]
   * </pre>
   *
   * @param {formattedTimezone[]} zones
   * @returns {shownTimezone[]}
   * @method _groupTimezones
   * @private
   */
  _groupTimezones: function (zones) {
    var z = dataUtils.groupPropertyValues(zones, 'groupByKey');
    var newZones = [];
    Object.keys(z).forEach(function (offset) {
      var groupedByRegionZones = dataUtils.groupPropertyValues(z[offset], 'region');
      Object.keys(groupedByRegionZones).forEach(function (region) {
        var cities = groupedByRegionZones[region].mapProperty('city').filter(function (city) {
          return city !== '' && city !== city.toUpperCase();
        }).uniq().join(', ');
        var formattedOffset = Em.get(groupedByRegionZones[region], 'firstObject.formattedOffset');
        var utcOffset = Em.get(groupedByRegionZones[region], 'firstObject.utcOffset');
        var value = Em.get(groupedByRegionZones[region], 'firstObject.groupByKey') + '|' + region;
        var abbr = moment.tz(Em.get(groupedByRegionZones[region], 'firstObject.value')).format('z');
        newZones.pushObject({
          utcOffset: utcOffset,
          label: '(UTC' + formattedOffset + ' ' + abbr + ') ' + region + (cities ? ' / ' + cities : ''),
          value: value,
          zones: groupedByRegionZones[region]
        });
      });
    });
    return newZones.sortProperty('utcOffset');
  }

});