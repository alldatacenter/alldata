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
 * Helper function for bound property helper registration
 * @memberof App
 * @method registerBoundHelper
 * @param name {String} name of helper
 * @param view {Em.View} view
 */
App.registerBoundHelper = function(name, view) {
  Em.Handlebars.registerHelper(name, function(property, options) {
    options.hash.contentBinding = property;
    return Em.Handlebars.helpers.view.call(this, view, options);
  });
};

/*
 * Return singular or plural word based on Em.I18n, view|controller context property key.
 *
 *  Example: {{pluralize hostsCount singular="t:host" plural="t:hosts"}}
 *           {{pluralize hostsCount singular="@view.hostName"}}
 */
App.registerBoundHelper('pluralize', App.PluralizeView);
/**
 * Return defined string instead of empty if value is null/undefined
 * by default is `n/a`.
 *
 * @param empty {String} - value instead of empty string (not required)
 *  can be used with Em.I18n pass value started with't:'
 *
 * Examples:
 *
 * default value will be returned
 * {{formatNull service.someValue}}
 *
 * <code>empty<code> will be returned
 * {{formatNull service.someValue empty="I'm empty"}}
 *
 * Em.I18n translation will be returned
 * {{formatNull service.someValue empty="t:my.key.to.translate"
 */
App.registerBoundHelper('formatNull', App.FormatNullView);

/**
 * Return formatted string with inserted <code>wbr</code>-tag after each dot or each '_'
 *
 * @param {String} content
 *
 * Examples:
 *
 * returns 'apple'
 * {{formatWordBreak 'apple'}}
 *
 * returns 'apple.<wbr />banana'
 * {{formatWordBreak 'apple.banana'}}
 *
 * returns 'apple.<wbr />banana.<wbr />uranium'
 * {{formatWordBreak 'apple.banana.uranium'}}
 *
 * returns 'apple_<wbr />banana_<wbr />uranium'
 * {{formatWordBreak 'apple_banana_uranium'}}
 *
 * returns 'Very<wbr />Long<wbr />String<wbr />With<wbr />Uppercase'
 * {{formatWordBreak 'VeryLongStringWithUppercase'}}
 */
App.registerBoundHelper('formatWordBreak', App.FormatWordBreakView);

/**
 * Return <i></i> with class that correspond to status
 *
 * @param {string} content - status
 *
 * Examples:
 *
 * {{statusIcon view.status}}
 * returns 'glyphicon glyphicon-cog'
 *
 */
App.registerBoundHelper('statusIcon', App.StatusIconView);

/**
 * Return `span` with formatted service name
 * @param {string} content - serviceName
 */
App.registerBoundHelper('formatRole', App.FormatRoleView);