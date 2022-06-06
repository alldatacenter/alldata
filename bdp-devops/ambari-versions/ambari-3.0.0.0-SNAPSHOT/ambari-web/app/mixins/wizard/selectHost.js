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
 * Mixin for host select view on step 5 install wizard
 * Implements base login for select, input types
 * Each view which use this mixin should redefine method <code>changeHandler</code> (with
 * observing <code>controller.hostNameCheckTrigger</code>)
 *
 * @type {Ember.Mixin}
 */
App.SelectHost = Em.Mixin.create({

  /**
   * Element of <code>controller.servicesMasters</code>
   * Binded from template
   * @type {object}
   */
  component: null,

  /**
   * List of avaiable host names
   * @type {string[]}
   */
  content: [],

  /**
   * Host component name
   * @type {string}
   */
  componentName: null,

  /**
   * Handler for selected value change
   * Triggers <code>changeHandler</code> execution
   * @method change
   */
  change: function () {
    if ('destroyed' === this.get('state')) return;
    this.set('controller.lastChangedComponent', this.get('component.component_name'));
    this.get('controller').toggleProperty('hostNameCheckTrigger');
  },

  /**
   * Add or remove <code>error</code> class from parent div-element
   * @param {bool} flag true - add class, false - remove
   * @method updateErrorStatus
   */
  updateErrorStatus: function(flag) {
    var parentBlock = this.$().parent('div');
    /* istanbul ignore next */
    if (flag) {
      parentBlock.removeClass('error');
    }
    else {
      parentBlock.addClass('error');
    }
  },

  /**
   * If component is multiple (defined in <code>controller.multipleComponents</code>),
   * should update <code>controller.componentToRebalance</code> and trigger rebalancing
   * @method tryTriggerRebalanceForMultipleComponents
   */
  tryTriggerRebalanceForMultipleComponents: function() {
    var componentIsMultiple = this.get('controller.multipleComponents').contains(this.get("component.component_name"));
    if(componentIsMultiple) {
      this.get('controller').set('componentToRebalance', this.get("component.component_name"));
      this.get('controller').incrementProperty('rebalanceComponentHostsCounter');
    }
  },

  /**
   * Handler for value changing
   * Should be redeclared in child views
   * Each redeclarion should contains code:
   * <code>
   *   if (!this.shouldChangeHandlerBeCalled()) return;
   * </code>
   * @method changeHandler
   */
  changeHandler: function() {}.observes('controller.hostNameCheckTrigger'),

  /**
   * If view is destroyed or not current component's host name was changed we should do nothing
   * This method should be called from <code>changeHandler</code>:
   * <code>
   *   changeHandler: function() {
   *     if (!this.shouldChangeHandlerBeCalled()) return;
   *     // your code
   *   }.observes(...)
   * </code>
   * @returns {boolean}
   * @method shouldChangeHandlerBeCalled
   */
  shouldChangeHandlerBeCalled: function() {
    return !(('destroyed' === this.get('state')) || (this.get('controller.lastChangedComponent') !== this.get("component.component_name")));
  },

  /**
   * If <code>component.isHostNameValid</code> was changed,
   * error status should be updated according to new value
   * @method isHostNameValidObs
   */
  isHostNameValidObs: function() {
    this.updateErrorStatus(this.get('component.isHostNameValid'));
  }.observes('component.isHostNameValid'),

  /**
   * Recalculate available hosts
   * This should be done only once per Ember loop
   * @method rebalanceComponentHosts
   */
  rebalanceComponentHosts: function () {
    Em.run.once(this, 'rebalanceComponentHostsOnce');
  }.observes('controller.rebalanceComponentHostsCounter'),

  /**
   * Recalculate available hosts
   * @method rebalanceComponentHostsOnce
   */
  rebalanceComponentHostsOnce: function() {
    if (this.get('component.component_name') === this.get('controller.componentToRebalance')) {
      this.initContent();
    }
  },

  /**
   * Get available hosts
   * multipleComponents component can be assigned to multiple hosts,
   * shared hosts among the same component should be filtered out
   * @return {string[]}
   * @method getAvailableHosts
   */
  getAvailableHosts: function () {
    var hosts = this.get('controller.hosts').slice(),
      componentName = this.get('component.component_name'),
      multipleComponents = this.get('controller.multipleComponents'),
      occupiedHosts = this.get('controller.selectedServicesMasters')
        .filterProperty('component_name', componentName)
        .mapProperty('selectedHost')
        .without(this.get('component.selectedHost'));

    if (multipleComponents.contains(componentName)) {
      hosts = hosts.filter(function (host) {
        return !occupiedHosts.contains(host.get('host_name'));
      }, this);
    }
    return hosts.sortProperty('host_name');
  },

  /**
   * Extract hosts from controller,
   * filter out available to selection and
   * push them into form element content
   * @method initContent
   */
  initContent: function () {
    this.set("content", this.getAvailableHosts());
  }

});
