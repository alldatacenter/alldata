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

App.InputRangeComponent = Em.TextField.extend({
  type: 'range',

  action: 'mouseUp',

  mouseUp: function () {
    this.sendAction('action', this.get('value'));
  }
});

App.FocusInputComponent = Ember.TextField.extend({
  becomeFocused: function() {
    this.$().focus().val(this.value);
  }.on('didInsertElement'),
  cancel:function () {
    this.get('targetObject').send(this.get('revert'),'cancel');
  }
});

App.ExpandableInputComponent = Em.TextField.extend({
  classNameBindings:['expanded'],
  focusIn:function  (argument) {
    this.$().parent().addClass('expanded').parent().addClass('expanded-wrap');
  },
  focusOut:function  (argument) {
    this.$().parent().removeClass('expanded').parent().removeClass('expanded-wrap');
  },
  checkBlank:function () {
    if (Em.isBlank(this.get('value')) && !Em.isNone(this.get('value'))) {
      this.set('value', null);
    }
  }.observes('value')
});

App.IntInputComponent = Ember.TextField.extend({
  classNames:['form-control'],
  maxVal:null,
  intVal:function () {
    var val = (!Em.isBlank(this.get('value'))) ? parseFloat(this.get('value')) : null;
    var maxVal = this.get('maxVal');
    this.set('value', (maxVal && maxVal < val)?maxVal:val);
  }.on('change'),
  checkNumber:function () {
    this.set('value', (!Em.isBlank(this.get('value')) && !isNaN(parseFloat(this.get('value')))) ? parseFloat(this.get('value')): null);
  }.observes('value')
});

App.DecimalInputComponent = Ember.TextField.extend({
  classNames:['form-control'],

  initVal: function() {
    this.set('value', (!Em.isBlank(this.get('value')) && !isNaN(parseFloat(this.get('value')))) ? parseFloat(this.get('value')): null);
  }.on('init'),

  keyDown: function(evt) {
    var newChar, val = this.get('value') || 0;
    val = val.toString();
    if ((evt.keyCode > 64 && evt.keyCode < 91) ||
       (evt.keyCode > 185 && evt.keyCode < 190) ||
       (evt.keyCode > 190 && evt.keyCode < 193) ||
       (evt.keyCode > 218 && evt.keyCode < 223)) {
      return false;
    }
    if (evt.keyCode === 190 || evt.keyCode === 9) {
      return true;
    }
    if (evt.keyCode > 95 && evt.keyCode < 106) {
      newChar = (evt.keyCode - 96).toString();
    } else {
      newChar = String.fromCharCode(evt.keyCode);
    }
    if (newChar.match(/[0-9]/)) {
      val = val.substring(0, evt.target.selectionStart) + newChar + val.substring(evt.target.selectionEnd);
    }
    if (/^\d+\.\d{3}$/.test(val)) {
      return false;
    }
    return parseFloat(val);
  },

  debounceId: null,

  cancelDebounceCallback: function() {
    Ember.run.cancel(this.get('debounceId'));
    this.set('debounceId', null);
  },

  initDebounceCallback: function(val) {
    var debounce = Ember.run.debounce(this, function() {
      this.set('value', parseFloat(val));
    }, 8000);
    this.set('debounceId', debounce);
  },

  valueDidChange: function() {
    var val = this.get('value');
    this.cancelDebounceCallback();
    if (/^\d+(\.(\d{1,2})?)?$/.test(val)) {
      if (/^\d+\.[0]$/.test(val) || /^\d+\.$/.test(val)) {
        this.initDebounceCallback(val);
      } else {
        this.set('value', parseFloat(val));
      }
    }
    else
      this.set('value', (!Em.isBlank(this.get('value')) && !isNaN(parseFloat(this.get('value')))) ? parseFloat(val) : null);
  }.observes('value').on('change')
});

App.CapacityInputComponent = App.IntInputComponent.extend({

  totalCapacity:null,
  queue:null,

  keyDown: function(evt) {
    var newChar, val = this.get('value')||0;
    val = val.toString();

    if ((evt.keyCode > 64 && evt.keyCode < 91) ||
      (evt.keyCode > 185 && evt.keyCode < 193) ||
      (evt.keyCode > 218 && evt.keyCode < 223)) {
      return false;
    }

    if (evt.keyCode > 95 && evt.keyCode < 106) {
      newChar = (evt.keyCode - 96).toString();
    } else {
      newChar = String.fromCharCode(evt.keyCode);
    }

    if (newChar.match(/[0-9]/)) {
      val = val.substring(0, evt.target.selectionStart) + newChar + val.substring(evt.target.selectionEnd);
    }

    return parseFloat(val) <= 100;
  }
});

App.MaxCapacityInputComponent = App.CapacityInputComponent.extend({
  isInvalid:false,
  invalid:function (c,o) {
    var queue = this.get('queue'), max_capacity, capacity;

    if (queue.get('maximum_capacity') === null) return;

    max_capacity = +queue.get('maximum_capacity');
    capacity = +queue.get('capacity');

    if (o == 'queue.capacity' && max_capacity < capacity) {
      return queue.set('maximum_capacity',capacity);
    }

    if (max_capacity < capacity && queue.get('isDirty')) {
      queue.get('errors').add('maximum_capacity', 'Maximum must be equal or greater than capacity');
    } else {
      queue.get('errors').remove('maximum_capacity');
    }

  }.observes('queue.maximum_capacity','queue.capacity')
});

App.DecimalInputRangeComponent = Em.TextField.extend({
  type: 'range',
  step: '0.01',
  action: 'mouseUp',

  mouseUp: function () {
    this.sendAction('action', this.get('value'));
  }
});

App.DecimalCapacityInputComponent = Ember.TextField.extend({
  classNames: ['form-control'],
  maxVal: null,
  totalCapacity: null,
  queue: null,

  initVal: function() {
    this.set('value', (!Em.isBlank(this.get('value')) && !isNaN(parseFloat(this.get('value')))) ? parseFloat(this.get('value')): null);
  }.on('init'),

  keyDown: function(evt) {
    var newChar, val = this.get('value') || 0;
    val = val.toString();

    if ((evt.keyCode > 64 && evt.keyCode < 91) ||
      (evt.keyCode > 185 && evt.keyCode < 190) ||
      (evt.keyCode > 190 && evt.keyCode < 193) ||
      (evt.keyCode > 218 && evt.keyCode < 223)) {
      return false;
    }

    //Allow decimal point and tab keys
    if (evt.keyCode === 190 || evt.keyCode === 9) {
      return true;
    }

    if (evt.keyCode > 95 && evt.keyCode < 106) {
      newChar = (evt.keyCode - 96).toString();
    } else {
      newChar = String.fromCharCode(evt.keyCode);
    }

    if (newChar.match(/[0-9]/)) {
      val = val.substring(0, evt.target.selectionStart) + newChar + val.substring(evt.target.selectionEnd);
    }

    //Restricting decimal places to less than or equal to 2
    if (/^\d+\.\d{3}$/.test(val)) {
      return false;
    }

    return parseFloat(val) <= 100;
  },

  debounceId: null,

  cancelDebounceCallback: function() {
    Ember.run.cancel(this.get('debounceId'));
    this.set('debounceId', null);
  },

  initDebounceCallback: function(val, maxVal) {
    var debounce = Ember.run.debounce(this, function() {
      this.set('value', (parseFloat(val) > maxVal)? parseFloat(maxVal) : parseFloat(val));
    }, 8000);
    this.set('debounceId', debounce);
  },

  valueDidChange: function() {
    var val = this.get('value'),
        maxVal = this.get('maxVal');
    this.cancelDebounceCallback();
    if (/^\d+(\.(\d{1,2})?)?$/.test(val)) {
      if (/^\d+\.[0]$/.test(val) || /^\d+\.$/.test(val)) {
        this.initDebounceCallback(val, maxVal);
      } else {
        this.set('value', (parseFloat(val) > maxVal)? parseFloat(maxVal) : parseFloat(val));
      }
    } else {
      this.set('value', (!Em.isBlank(this.get('value')) && !isNaN(parseFloat(this.get('value')))) ? parseFloat(val) : null);
    }
  }.observes('value').on('change')
});

App.DecimalMaxcapacityInputComponent = App.DecimalCapacityInputComponent.extend({
  checkInvalid: function(c, o) {
    var queue = this.get('queue'),
    max_capacity = +queue.get('maximum_capacity'),
    capacity = +queue.get('capacity');

    if (queue.get('maximum_capacity') === null) {
      return;
    }

    if (o === 'queue.capacity' && max_capacity < capacity) {
      return queue.set('maximum_capacity', capacity);
    }

    if (max_capacity < capacity && queue.get('isDirty')) {
      queue.get('errors').add('maximum_capacity', 'Maximum must be greater than or equal to capacity');
    } else {
      queue.get('errors').remove('maximum_capacity');
    }

  }.observes('queue.capacity', 'queue.maximum_capacity')
});
