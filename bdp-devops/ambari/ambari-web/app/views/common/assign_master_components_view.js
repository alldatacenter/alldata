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

App.AssignMasterComponentsView = Em.View.extend({

  templateName: require('templates/common/assign_master_components'),

  /**
   * Title to be shown on the page
   * @type {String}
   */
  title: '',

  showTitle: true,

  /**
   * Alert message to be shown on the page
   * @type {String}
   */
  alertMessage: '',

  /**
   * If install more than 25 hosts, should use App.InputHostView for hosts selection
   * Otherwise - App.SelectHostView
   * @type {bool}
   */
  shouldUseInputs: Em.computed.gt('controller.hosts.length', 25),

  isWizardStep: true,

  isBackButtonVisible: true,

  didInsertElement: function () {
    this.get('controller').loadStep();
  },

  willDestroyElement: function () {
    this.get('controller').clearStepOnExit();
  }
});

App.InputHostView = Em.TextField.extend(App.SelectHost, {

  attributeBindings: ['disabled'],
  /**
   * Saved typeahead component
   * @type {$}
   */
  typeahead: null,

  classNames: ['form-control'],

  /**
   * When <code>value</code> (host_info) is changed this method is triggered
   * If new hostname is valid, this host is assigned to master component
   * @method changeHandler
   */
  changeHandler: function() {
    if (!this.shouldChangeHandlerBeCalled()) return;
    var host = this.get('controller.hosts').findProperty('host_name', this.get('value'));
    if (Em.isNone(host)) {
      this.get('controller').updateIsHostNameValidFlag(this.get("component.component_name"), this.get("component.serviceComponentId"), false);
      this.get('controller').updateIsSubmitDisabled();
      return;
    }
    this.get('controller').assignHostToMaster(this.get("component.component_name"), host.get('host_name'), this.get("component.serviceComponentId"));
    this.tryTriggerRebalanceForMultipleComponents();
    this.get('controller').updateIsSubmitDisabled();
  }.observes('controller.hostNameCheckTrigger'),

  didInsertElement: function () {
    this.initContent();
    var value = this.get('content').findProperty('host_name', this.get('component.selectedHost')).get('host_name');
    this.set("value", value);
    var content = this.get('content').mapProperty('host_info'),
        self = this,
        updater = function (item) {
          return self.get('content').findProperty('host_info', item).get('host_name');
        },
        typeahead = this.$().typeahead({items: 10, source: content, updater: updater, minLength: 0});
    typeahead.on('blur', function() {
      self.change();
    }).on('keyup', function(e) {
      self.set('value', $(e.currentTarget).val());
      self.change();
    });
    this.set('typeahead', typeahead);
    App.popover($("[rel=popover]"), {'placement': 'right', 'trigger': 'hover'});
  },

  /**
   * Extract hosts from controller,
   * filter out available to selection and
   * push them into Em.Select content
   * @method initContent
   */
  initContent: function () {
    this._super();
    this.updateTypeaheadData(this.get('content').mapProperty('host_info'));
  },

  /**
   * Update <code>source</code> property of <code>typeahead</code> with a new list of hosts
   * @param {string[]} hosts
   * @method updateTypeaheadData
   */
  updateTypeaheadData: function(hosts) {
    if (this.get('typeahead')) {
      this.get('typeahead').data('typeahead').source = hosts;
    }
  }

});

App.SelectHostView = App.DropdownView.extend(App.SelectHost, {

  qaAttr: 'select-host-for-component',

  didInsertElement: function () {
    this.initContent();
    this.set("value", this.get("component.selectedHost"));
    App.popover($("[rel=popover]"), {'placement': 'right', 'trigger': 'hover'});
  },

  /**
   * Handler for selected value change
   * @method change
   */
  changeHandler: function () {
    if (!this.shouldChangeHandlerBeCalled()) return;
    this.get('controller').assignHostToMaster(this.get("component.component_name"), this.get("value"), this.get("component.serviceComponentId"));
    this.tryTriggerRebalanceForMultipleComponents();
  }.observes('controller.hostNameCheckTrigger'),

  /**
   * On change DOM event handler
   * @method change
   */
  change: function () {
    this._super();
    this.initContent();
  }

});

App.AddControlView = Em.View.extend({

  /**
   * DOM node class attribute
   * @type {string}
   */
  uniqueId: Em.computed.format('{0}-add', 'componentName'),

  /**
   * Current component name
   * @type {string}
   */
  componentName: null,

  tagName: "div",

  classNames: ["label", 'extra-component'],

  classNameBindings: ['uniqueId'],

  'data-qa': 'add-master',

  template: Em.Handlebars.compile('+'),

  /**
   * Onclick handler
   * Add selected component
   * @method click
   */
  click: function () {
    this.get('controller').addComponent(this.get('componentName'));
  }
});

App.RemoveControlView = Em.View.extend({
  /**
   * DOM node class attribute
   * @type {string}
   */
  uniqueId: Em.computed.format('{0}-{1}-remove', 'componentName', 'serviceComponentId'),

  classNameBindings: ['uniqueId'],

  /**
   * Index for multiple component
   * @type {number}
   */
  serviceComponentId: null,

  /**
   * Current component name
   * @type {string}
   */
  componentName: null,

  tagName: "div",

  'data-qa': 'remove-master',

  classNames: ["label", 'extra-component'],

  template: Em.Handlebars.compile('-'),

  /**
   * Onclick handler
   * Remove current component
   * @method click
   */
  click: function () {
    this.get('controller').removeComponent(this.get('componentName'), this.get("serviceComponentId"));
  }
});
