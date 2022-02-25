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

var App = require('app');

/**
 * use: {{view App.TimeRangeWidget controllerBinding="App.router.mainChartsController"}}
 * set controller.preset field with preset value
 * widget assign itself to controller like presetWidget (controller.get('presetWidget'))
 * @type {*}
 */
App.TimeRangeWidget = Em.View.extend({
  classNames:['time-range-widget'],
  templateName:require('templates/common/time_range'),
  dateFrom: null,
  dateTo: null,

  /**
   * presets
   */
  presets:[
    Em.Object.create({ label:Em.I18n.t('timeRange.presets.1hour'), value:'1h', period: 3600000, step: 300000}),
    Em.Object.create({ label:Em.I18n.t('timeRange.presets.12hour'), value:'12h', period: 43200000, step: 3600000}),
    Em.Object.create({ label:Em.I18n.t('timeRange.presets.1day'), value:'1d', period: 86400000, step: 3600000}),
    Em.Object.create({ label:Em.I18n.t('timeRange.presets.1week'), value:'1wk', period: 604800000, step: 86400000}),
    Em.Object.create({ label:Em.I18n.t('timeRange.presets.1month'), value:'1mo', period: 2592000000, step: 86400000}),
    Em.Object.create({ label:Em.I18n.t('timeRange.presets.1year'), value:'1yr', period: 31536000000, step: 2592000000})
  ],
  /**
   * chosen preset value
   */
  chosenPreset: null,

  /**
   * return array of chosen presets
   */
  chosenPresets:function () {
    return this.get('chosenPreset') ? [this.get('chosenPreset')] : this.get('defaultPresets');
  }.property('chosenPreset'),

  /**
   * preset item view
   */
  presetView:Em.View.extend({
    tagName:'li',
    classNameBindings:['disabled'],
    disabled: Em.computed.ifThenElse('isActive', 'disabled', false),
    isActive: Em.computed.equalProperties('preset.value', 'widget.chosenPreset.value'),
    template:Em.Handlebars.compile('<a {{action activate view.preset target="view.widget" href="true" }}>{{unbound view.preset.label}}</a>')
  }),

  /**
   * return default selected presets (currently - all)
   */
  defaultPresets:function () {
    var values = [];
    $.each(this.get('presets'), function () {
      if (this.value) {
        values.push(this.value);
      }
    });
    return values;
  }.property(),

  bindToController:function () {
    var thisW = this;
    var controller = this.get('controller');
    controller.set('presetWidget', thisW);
  },

  /**
   * assign this widget to controller, prepare items by presetsConfig
   */
  init:function () {
    this._super();
    this.bindToController();
  },

  /**
   * write active preset to widget
   * @param event
   */
  activate:function (event) {
    if (event.context == this.get('chosenPreset')) {
      this.set('chosenPreset', null);
    } else {
      this.set('chosenPreset', event.context);
    }
  },

  dateFromView: Ember.TextField.extend({
    elementId: 'timeRangeFrom',
    classNames: 'timeRangeFrom',
    attributeBindings:['readonly'],
    readonly: true,
    didInsertElement: function() {
      var self = this;
      this.$().datetimepicker({
        dateFormat: 'dd/mm/yy',
        timeFormat: 'hh:mm',
        maxDate: new Date(),
        onClose:function (dateText, inst) {
          var endDateTextBox = $('#timeRangeTo');
          if (endDateTextBox.val() != '') {
            var testStartDate = new Date(dateText);
            var testEndDate = new Date(endDateTextBox.val());
            if (testStartDate > testEndDate)
              endDateTextBox.val(dateText);
          } else {
            endDateTextBox.val(dateText);
          }
          self.set('dateFrom', dateText);
        },
        onSelect:function (selectedDateTime) {
          var start = $(this).datetimepicker('getDate');
          $('#timeRangeTo').datetimepicker('option', 'minDate', new Date(start.getTime()));
        }
      });
      self.set('dateFrom', this.get('value'));
    }
  }),

  dateToView: Ember.TextField.extend({
    elementId: 'timeRangeTo',
    classNames: 'timeRangeTo',
    attributeBindings:['readonly'],
    readonly: true,
    didInsertElement: function() {
      var self = this;
      this.$().datetimepicker({
        dateFormat: 'dd/mm/yy',
        timeFormat: 'hh:mm',
        maxDate: new Date(),
        onClose:function (dateText, inst) {
          var startDateTextBox = $('#timeRangeFrom');
          if (startDateTextBox.val() != '') {
            var testStartDate = new Date(startDateTextBox.val());
            var testEndDate = new Date(dateText);
            if (testStartDate > testEndDate)
              startDateTextBox.val(dateText);
          } else {
            startDateTextBox.val(dateText);
          }
          self.set('dateTo', dateText);
        },
        onSelect:function (selectedDateTime) {
          var end = $(this).datetimepicker('getDate');
          $('#timeRangeFrom').datetimepicker('option', 'maxDate', new Date(end.getTime()));
        }
      });
      self.set('dateTo', this.get('value'));
    }
  }),

  sliderOptions: Ember.Object.extend({
    end: null,
    period: null,
    start: function() {
      return this.get('end') - this.get('period');
    }.property('end', 'period')
  }),
  nowLabel: null,
  rangeLabel: null,
  buildSlider: function() {
    if (this.get('chosenPreset')) {
      var sliderOptions = this.sliderOptions.create({
        end: function() {
          var endDate = new Date();
          return endDate.getTime();
        }.property(),
        period: this.get('chosenPreset.period'),
        step: this.get('chosenPreset.step'),
        countTimeAgo: function(stepValue) {
          var msAgo = this.get('end') - stepValue;
          return msAgo.toDaysHoursMinutes();
        }
      });
      this.set('nowLabel', 'Now');
      this.set('rangeLabel', new Date(sliderOptions.get('start')));

      var self = this;
      $('#slider').slider({
        range: "max",
        min: sliderOptions.get('start'),
        max: sliderOptions.get('end'),
        value: sliderOptions.get('start'),
        step: sliderOptions.get('step'),
        stop: function(event, ui) {
          self.set('rangeLabel', new Date(ui.value));
//          self.set('rangeLabel', sliderOptions.countTimeAgo(ui.value).h);
        },
        slide: function(event, ui){
          self.set('rangeLabel', new Date(ui.value));
//          self.set('rangeLabel', sliderOptions.countTimeAgo(ui.value).h);
        }
      });
    } else {
      $("#slider").slider("destroy");
    }
  }.observes('chosenPreset')
});