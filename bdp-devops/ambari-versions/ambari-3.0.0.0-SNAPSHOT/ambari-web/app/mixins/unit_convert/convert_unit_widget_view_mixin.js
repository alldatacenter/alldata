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
 * Unit conversion mixin for widget views.
 * @type {Em.Mixin}
 */
App.ConvertUnitWidgetViewMixin = Em.Mixin.create(App.BaseUnitConvertMixin, {
  /**
   * Get converted value according to widget value format from specified config property value.
   *
   * @method widgetValueByConfigAttributes
   * @param {String|Number} value - config property value to convert
   * @param {Boolean} [returnObject=false] - returned value should be an array of objects
   * @returns {Number|Object[]}
   */
  widgetValueByConfigAttributes: function(value, returnObject) {
    return this.convertValue(value, this._converterGetPropertyAttributes(), this._converterGetWidgetUnits(), returnObject);
  },

  /**
   * Get converted value according to config property unit format from specified widget value.
   *
   * @param {String|Number|Object[]} value - widget value to convert
   * @returns {String}
   * @method configValueByWidget
   */
  configValueByWidget: function(value) {
    var cfgValue = this.convertValue(value, this._converterGetWidgetUnits(), this._converterGetPropertyAttributes());
    return '' + this.get('config.stackConfigProperty.valueAttributes.type') === 'int' ? Math.round(cfgValue) : cfgValue;
  },

  /**
   * @private
   * @method _converterGetWidgetUnits
   * @throw Error
   * @returns {String|String[]}
   */
  _converterGetWidgetUnits: function() {
    var widgetAttributes = this.get('config.stackConfigProperty.widget');
    var widgetUnits = Em.getWithDefault(widgetAttributes, 'units.0.unit-name', false) || Em.getWithDefault(widgetAttributes, 'units.0.unit', false);
    Em.assert('Invalid widget units for ' + this.get('config.name') + ' widget object: ' + JSON.stringify(widgetAttributes), widgetUnits);
    return widgetUnits;
  },

  /**
   * @private
   * @method _converterGetPropertyAttributes
   * @throw Error
   * @returns {String|String[]}
   */
  _converterGetPropertyAttributes: function() {
    var propertyAttributes = this.get('config.stackConfigProperty.valueAttributes');
    var propertyUnits = Em.getWithDefault(propertyAttributes, 'unit', false) || Em.getWithDefault(propertyAttributes, 'type', false);
    Em.assert('Invalid property unit type for ' + this.get('config.name') + ' valueAttributes: ' + JSON.stringify(propertyAttributes), propertyUnits);
    return propertyUnits;
  }
});
