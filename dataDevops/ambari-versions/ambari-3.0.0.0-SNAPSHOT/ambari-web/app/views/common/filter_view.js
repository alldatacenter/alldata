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

/**
 * Wrapper View for all filter components. Layout template and common actions are located inside of it.
 * Logic specific for data component(input, select, or custom multi select, which fire any changes on interface) are
 * located in inner view - <code>filterView</code>.
 *
 * If we want to have input filter, put <code>textFieldView</code> to it.
 * All inner views implemented below this view.
 * @type {*}
 */

var App = require('app');

var validator = require('utils/validator');

var wrapperView = Ember.View.extend({
  classNames: ['view-wrapper'],
  layout: Ember.Handlebars.compile('{{yield}} <a href="#" {{action "clearFilter" target="view"}} class="ui-icon ui-icon-circle-close"></a>'),
  template: Ember.Handlebars.compile(
    '{{#if view.fieldId}}<input type="hidden" id="{{unbound view.fieldId}}" value="" />{{/if}}' +
    '{{view view.filterView}}' +
    '{{#if view.showApply}}<button {{action "setValueOnApply" target="view"}} class="apply-btn btn btn-default"><span>{{t common.apply}}</span></button>{{/if}} '
  ),

  value: null,

  /**
   * Column index
   */
  column: null,

  /**
   * If this field is exists we dynamically create hidden input element and set value there.
   * Used for some cases, where this values will be used outside of component
   */
  fieldId: null,

  /**
   * This option is useful for Em.Select, if you want get events when same option selected more than one time a raw.
   * This property is Object[] (for example see App.MainConfigHistoryView.modifiedFilterView),
   * that have followed structure:
   *
   * <code>
   * [
   *  {
   *    values: ['DefinedValue1', 'DefinedValue2'], // this properties should be defined in `content` property
   *    displayAs: 'Choose me' // this value will be displayed
   *  }
   * ]
   * </code>
   * @type {Array}
   *
   **/
  //TODO delete if will not be used
  triggeredOnSameValue: null,

  clearFilter: function () {
    this.set('value', this.get('emptyValue'));
    if (this.get('setPropertyOnApply')) {
      this.setValueOnApply();
    }
    return false;
  },

  setValueOnApply: function() {
    if(this.get('value') == null){
      this.set('value', '')
    }
    this.set(this.get('setPropertyOnApply'), this.get('value'));
    return false;
  },

  /**
   * Use to determine whether filter is clear or not. Also when we want to set empty value
   */
  emptyValue: '',

  /**
   * empty value that actually applied to filtering on server,
   * used with Select filter where displayed and actual filter value are different
   */
  appliedEmptyValue: "",

  /**
   * Whether our <code>value</code> is empty or not
   * @return {Boolean}
   */
  isEmpty: function () {
    if (Em.isNone(this.get('value'))) {
      return true;
    }
    return this.get('value').toString() === this.get('emptyValue').toString();
  },

  setValue: function (value) {
    this.set('value', value);
  },

  /**
   * Show/Hide <code>Clear filter</code> button.
   * Also this method updates computed field related to <code>fieldId</code> if it exists.
   * Call <code>onChangeValue</code> callback when everything is done.
   */
  showClearFilter: function () {
    if (!this.get('parentNode')) {
      return;
    }
    // get the sort view element in the same column to current filter view to highlight them together
    var relatedSort = $(this.get('element')).parents('thead').find('.sort-view-' + this.get('column'));
    if (this.isEmpty()) {
      this.get('parentNode').removeClass('active-filter');
      this.get('parentNode').addClass('notActive');
      relatedSort.removeClass('active-sort');
    } else {
      this.get('parentNode').removeClass('notActive');
      this.get('parentNode').addClass('active-filter');
      relatedSort.addClass('active-sort');
    }

    if (this.get('fieldId')) {
      this.$('> input').eq(0).val(this.get('value'));
    }

    this.onChangeValue();
  }.observes('value'),

  /**
   * Callback for value changes
   */
  onChangeValue: Em.K,

  /**
   * Filter components is located here. Should be redefined
   */
  filterView: Em.View,

  /**
   * Update class of parentNode(hide clear filter button) on page load
   */
  didInsertElement: function () {
    var parent = this.$().parent();
    this.set('parentNode', parent);
    parent.addClass('notActive');
    //TODO delete if will not be used
    //this.checkSelectSpecialOptions();
  },

  /**
   * Check for Em.Select that should use dispatching event when option with same value selected more than one time.
   **/
    //TODO delete if will not be used
  checkSelectSpecialOptions: function () {
    // check predefined property
    if (!this.get('triggeredOnSameValue') || !this.get('triggeredOnSameValue').length) return;
    // add custom additional observer that will handle property changes
    this.addObserver('value', this, 'valueCustomObserver');
    // get the full class attribute to find our select
    var classInlineAttr = this.get('fieldType').split(',')
      .map(function (className) {
        return '.' + className.trim();
      }).join('');
    this.set('classInlineAttr', classInlineAttr);
    this.get('triggeredOnSameValue').forEach(function (triggeredValue) {
      triggeredValue.values.forEach(function (value, index) {
        // option with property `value`
        var $optionEl = $(this.get('element')).find(classInlineAttr)
          .find('option[value="' + value + '"]');
        // should be displayed with `displayAs` caption
        $optionEl.text(triggeredValue.displayAs);
        // the second one option should be hidden
        // as the result, on init stage we show only one option that could be selected
        if (index === 1) {
          $optionEl.css('display', 'none');
        }
      }, this);
    }, this);
  },
  /**
   *
   * Custom observer that used for special case of Em.Select related to dispatching event
   * when option with same value selected more than one time.
   *
   **/
  //TODO delete if will not be used
  valueCustomObserver: function () {
    var hiddenValue;
    this.get('triggeredOnSameValue').forEach(function (triggeredValue) {
      var values = triggeredValue.values;
      // find current selected value from `values` list
      var currentValueIndex = values.indexOf(this.get('value'));
      if (currentValueIndex < 0) return;
      // value assigned to hidden option
      hiddenValue = values[Number(currentValueIndex === 0)];
    }, this);
    if (hiddenValue) {
      // our select
      var $select = $(this.get('element')).find(this.get('classInlineAttr'));
      // now hide option with current value
      $select.find('option[value="{0}"]'.format(this.get('value'))).css('display', 'none');
      // and show option that was hidden
      $select.find('option[value="{0}"]'.format(hiddenValue)).css('display', 'block');
    }
  }
});

/**
 * Simple input control for wrapperView
 */
var textFieldView = Ember.TextField.extend({
  classNames: ['input-sm', 'form-control'],
  type: 'text',
  placeholder: Em.I18n.t('any'),
  valueBinding: "parentView.value"
});

/**
 * Simple multiselect control for wrapperView.
 * Used to render blue button and popup, which opens on button click.
 * All content related logic should be implemented manually outside of it
 */
var componentFieldView = Ember.View.extend({
  classNames: ['btn-group'],
  classNameBindings: ['isFilterOpen:open:'],

  /**
   * Whether popup is shown or not
   */
  isFilterOpen: false,

  /**
   * We have <code>value</code> property similar to inputs <code>value</code> property
   */
  valueBinding: 'parentView.value',

  /**
   * Clear filter to initial state
   */
  clearFilter: function () {
    this.set('value', '');
  },

  /**
   * Onclick handler for <code>cancel filter</code> button
   */
  closeFilter: function () {
    $(document).unbind('click');
    this.set('isFilterOpen', false);
  },

  /**
   * Onclick handler for <code>apply filter</code> button
   */
  applyFilter: function () {
    this.closeFilter();
  },

  /**
   * Onclick handler for <code>show component filter</code> button.
   * Also this function is used in some other places
   */
  clickFilterButton: function () {
    var self = this;
    this.toggleProperty('isFilterOpen');
    if (this.get('isFilterOpen')) {

      var dropDown = this.$('.filter-components');
      var firstClick = true;
      $(document).bind('click', function (e) {
        if (!firstClick && !$(e.target).closest(dropDown).length) {
          self.set('isFilterOpen', false);
          $(document).unbind('click');
        }
        firstClick = false;
      });
    }
  }
});

/**
 * Simple select control for wrapperView
 */
var getSelectFieldView = function() {
  return App.DropdownView.extend({
    selectionBinding: 'parentView.selected',
    contentBinding: 'parentView.content',
    optionValuePath: "value",
    optionLabelPath: "label"
  });
};

/**
 * Result object, which will be accessible outside
 * @type {Object}
 */
module.exports = {
  /**
   * You can access wrapperView outside
   */
  wrapperView: wrapperView,

  /**
   * And also controls views if need it
   */
  textFieldView: textFieldView,
  componentFieldView: componentFieldView,

  /**
   * Quick create input filters
   * @param config parameters of <code>wrapperView</code>
   */
  createTextView: function (config) {
    config.fieldType = config.fieldType || 'input-medium';
    config.filterView = textFieldView.extend({
      classNames: [ config.fieldType ]
    });

    return wrapperView.extend(config);
  },

  /**
   * Quick create multiSelect filters
   * @param config parameters of <code>wrapperView</code>
   */
  createComponentView: function (config) {
    config.clearFilter = function () {
      this.forEachChildView(function (item) {
        if (item.clearFilter) {
          item.clearFilter();
        }
      });
      return false;
    };

    return wrapperView.extend(config);
  },

  /**
   * Quick create select filters
   * @param config parameters of <code>wrapperView</code>
   */
  createSelectView: function (config) {
    config.fieldType = config.fieldType || 'input-medium';
    config.filterView = getSelectFieldView().extend({
      classNames: config.fieldType.split(',').concat('display-inline-block'),
      attributeBindings: ['disabled', 'multiple'],
      disabled: false
    });

    config.valueBinding = 'selected.value';
    config.clearFilter = function () {
      this.set('selected', this.get('content').findProperty('value', this.get('emptyValue')));
    };
    config.setValue = function (value) {
      this.set('selected', this.get('content').findProperty('value', value));
    };
    return wrapperView.extend(config);
  },
  /**
   * returns the filter function, which depends on the type of property
   * @param type
   * @param isGlobal check is search global
   * @return {Function}
   */
  getFilterByType: function (type, isGlobal) {
    switch (type) {
      case 'ambari-bandwidth':
        return function (rowValue, rangeExp) {
          var compareChar = isNaN(rangeExp.charAt(0)) ? rangeExp.charAt(0) : false;
          var compareScale = rangeExp.charAt(rangeExp.length - 1);
          var compareValue = parseFloat(rangeExp.substr(compareChar ? 1 : 0));
          if (rangeExp.length === 1 && compareChar !== false) {
            // User types only '=' or '>' or '<', so don't filter column values
            return true;
          }
          var oneSymbolScales = {
            k: 1024,
            m: 1048576,
            g: 1073741824
          };
          var twoSymbolsScales = {
            KB: 1024,
            MB: 1048576,
            GB: 1073741824
          };

          compareValue *= oneSymbolScales[compareScale] ? oneSymbolScales[compareScale] : oneSymbolScales.g; // default value in GB
          rowValue = jQuery(rowValue).text() ? jQuery(rowValue).text() : rowValue;

          var convertedRowValue;
          if (rowValue === '<1KB') {
            convertedRowValue = 1;
          } else {
            var rowValueScale = rowValue.substr(rowValue.length - 2, 2);
            if (twoSymbolsScales[rowValueScale]) {
              convertedRowValue = parseFloat(rowValue) * twoSymbolsScales[rowValueScale];
            }
          }

          switch (compareChar) {
            case '<':
              return compareValue > convertedRowValue;
            case '>':
              return compareValue < convertedRowValue;
            case false:
            case '=':
              return compareValue === convertedRowValue;
            default:
              return false;
          }
        };
      case 'duration':
        return function (rowValue, rangeExp) {
          var compareChar = isNaN(rangeExp.charAt(0)) ? rangeExp.charAt(0) : false;
          var compareScale = rangeExp.charAt(rangeExp.length - 1);
          var compareValue = parseFloat(rangeExp.substr(compareChar ? 1 : 0));
          if (rangeExp.length === 1 && compareChar !== false) {
            // User types only '=' or '>' or '<', so don't filter column values
            return true;
          }
          var oneSymbolsScales = {
            s: 1000,
            m: 60000,
            h: 3600000
          };
          compareValue *= oneSymbolsScales[compareScale] ? oneSymbolsScales[compareScale] : oneSymbolsScales.s;
          rowValue = jQuery(rowValue).text() ? jQuery(rowValue).text() : rowValue;

          switch (compareChar) {
            case '<':
              return compareValue > rowValue;
            case '>':
              return compareValue < rowValue;
            case false:
            case '=':
              return compareValue == rowValue;
            default:
              return false;
          }
        };
      case 'date':
        return function (rowValue, rangeExp) {
          var timePassed = App.dateTime() - rowValue;
          switch (rangeExp) {
            case 'Past 1 hour':
              return timePassed <= 3600000;
            case 'Past 1 Day':
              return timePassed <= 86400000;
            case 'Past 2 Days':
              return timePassed <= 172800000;
            case 'Past 7 Days':
              return timePassed <= 604800000;
            case 'Past 14 Days':
              return timePassed <= 1209600000;
            case 'Past 30 Days':
              return timePassed <= 2592000000;
            case 'Any':
              return true;
            default:
              return false;
          }
        };
      case 'number':
        return function (rowValue, rangeExp) {
          var compareChar = rangeExp.charAt(0);
          var compareValue;
          if (rangeExp.length === 1) {
            if (isNaN(parseInt(compareChar, 10))) {
              // User types only '=' or '>' or '<', so don't filter column values
              return true;
            }
            compareValue = parseFloat(parseFloat(rangeExp).toFixed(2));
          }
          else {
            if (isNaN(parseInt(compareChar, 10))) {
              compareValue = parseFloat(parseFloat(rangeExp.substr(1, rangeExp.length)).toFixed(2));
            }
            else {
              compareValue = parseFloat(parseFloat(rangeExp.substr(0, rangeExp.length)).toFixed(2));
            }
          }
          rowValue = parseFloat(jQuery(rowValue).text() ? jQuery(rowValue).text() : rowValue);
          switch (compareChar) {
            case '<':
              return compareValue > rowValue;
            case '>':
              return compareValue < rowValue;
            case '=':
            default:
              return compareValue === rowValue;
          }
        };
      case 'sub-resource':
        return function (origin, compareValue) {
          if (!Array.isArray(compareValue) || !compareValue.length) {
            return true;
          }

          return origin.some(function (item) {
            for (var i = 0, l = compareValue.length; i < l; i++) {
              if (Array.isArray(compareValue[i].value)) {
                if (!compareValue[i].value.contains(item.get(compareValue[i].property))) return false;
              } else {
                if (item.get(compareValue[i].property) !== compareValue[i].value) return false;
              }
            }
            return true;
          });
        };
      case 'multiple':
        return function (origin, compareValue) {
          var options = compareValue.split(',');
          var rowValue = typeof origin === "string" ? origin : origin.mapProperty('componentName').join(" ");
          var str = new RegExp(compareValue, "i");
          for (var i = 0; i < options.length; i++) {
            if (!isGlobal) {
              str = new RegExp('(\\W|^)' + options[i] + '(\\W|$)');
            }
            if (rowValue.search(str) !== -1) {
              return true;
            }
          }
          return false;
        };
      case 'boolean':
      case 'select':
        return function (origin, compareValue) {
          return origin === compareValue;
        };
      case 'os':
        return function (origin, compareValue) {
          return origin.getEach('osType').contains(compareValue)
        };
      case 'range':
        return function (origin, compareValue) {
          if (compareValue[1] && compareValue[0]) {
            return origin >= compareValue[0] && origin <= compareValue[1];
          }
          if (compareValue[0]) {
            return origin >= compareValue[0];
          }
          if (compareValue[1]) {
            return origin <= compareValue[1]
          }
          return true;
        };
      case 'alert_status':
        /**
         * origin - alertDefinition.summary
         * compareValue - "OK|WARNING|CRITICAL|UNKNOWN|PENDING"
         * PENDING means that OK is 0, WARNING is 0, CRITICAL is 0 and UNKNOWN is 0
         */
        return function (origin, compareValue) {
          if ('PENDING' === compareValue) {
            var isPending = true;
            Em.keys(origin).forEach(function(state) {
              if (origin[state] && (origin[state].count > 0 || origin[state].maintenanceCount > 0)) {
                isPending = false;
              }
            });
            return isPending;
          }
          return !!origin[compareValue] && (origin[compareValue].count > 0 || origin[compareValue].maintenanceCount > 0);
        };
      case 'alert_group':
        return function (origin, compareValue) {
          return origin.mapProperty('id').contains(compareValue);
        };
      case 'enable_disable':
        return function (origin, compareValue) {
          return origin === (compareValue === 'enabled');
        };
      case 'file_extension':
        return function(origin, compareValue) {
          return origin.endsWith(compareValue);
        };
      case 'string':
      default:
        return function (origin, compareValue) {
          if (validator.isValidMatchesRegexp(compareValue)) {
            var escapedCompareValue = compareValue.replace("(", "\\(").replace(")", "\\)").trim();
            var regex = new RegExp(escapedCompareValue, "i");
            return regex.test(origin);
          }
          return false;
        }
    }
  },

  getComputedServicesList: function () {
    return Em.computed('App.router.clusterController.isLoaded', function () {
      return [
        {
          value: '',
          label: Em.I18n.t('common.all')
        }
      ].concat(App.Service.find().map(function (service) {
        return {
          value: service.get('serviceName'),
          label: service.get('displayName')
        }
      })).concat({
        value: 'AMBARI',
        label: Em.I18n.t('app.name')
      });
    });
  }

};
