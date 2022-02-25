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
require('models/configs/service_config_version');

var model;

function getModel() {
  return App.ServiceConfigVersion.createRecord({});
}

describe('App.ServiceConfigVersion', function () {

  beforeEach(function () {
    model = getModel();
  });

  App.TestAliases.testAsComputedAnd(getModel(), 'canBeMadeCurrent', ['isCompatible', '!isCurrent']);

  App.TestAliases.testAsComputedTruncate(getModel(), 'authorFormatted', 'author', 15, 15);

  App.TestAliases.testAsComputedTruncate(getModel(), 'briefNotes', 'fullNotes', 81, 81, '');

  App.TestAliases.testAsComputedNotEqualProperties(getModel(), 'moreNotesExists', 'fullNotes', 'briefNotes');

  describe("#configGroupName", function() {

    it("not default group", function() {
      model.reopen({
        groupName: 'g1',
        isDefault: false
      });
      expect(model.get('configGroupName')).to.equal('g1');
    });

    it("default group", function() {
      model.reopen({
        isDefault: true
      });
      expect(model.get('configGroupName')).to.equal(Em.I18n.t('common.default'));
    });

  });

  describe("#fullNotes", function() {

    it("notes is null", function() {
      model.set('notes', null);
      expect(model.get('fullNotes')).to.equal(Em.I18n.t('dashboard.configHistory.table.notes.no'));
    });

    it("notes is empty", function() {
      model.set('notes', "");
      expect(model.get('fullNotes')).to.equal(Em.I18n.t('dashboard.configHistory.table.notes.no'));
    });

    it("notes has value", function() {
      model.set('notes', "notes-value");
      expect(model.get('fullNotes')).to.equal('notes-value');
    });

  });

  describe("#createdDate", function() {

    it("should return created date", function() {
      model.set('createTime', 1450267588961);
      moment.tz.setDefault('America/Los_Angeles');
      expect(model.get('createdDate')).to.equal('Wed, Dec 16, 2015 04:06');
    });

  });

  describe("#timeSinceCreated", function () {

    before(function () {
      sinon.stub($, 'timeago').returns('timeago');
    });
    after(function () {
      $.timeago.restore()
    });

    it("should return time since created", function () {
      model.set('rawCreateTime', 1450267588961);
      expect(model.get('timeSinceCreated')).to.equal('timeago');
    });

  });

  describe("#isRestartRequired", function() {

    it("service.isRestartRequired is false", function() {
      model.set('service', Em.Object.create({
        isRestartRequired: false
      }));
      expect(model.get('isRestartRequired')).to.be.false;
    });

    it("non-current version", function() {
      model.set('service', Em.Object.create({
        isRestartRequired: true
      }));
      model.set('isCurrent', false);
      expect(model.get('isRestartRequired')).to.be.false;
    });

    it("version has no hosts", function() {
      model.setProperties({
        service: Em.Object.create({
          isRestartRequired: true
        }),
        isCurrent: true,
        hosts: []
      });
      expect(model.get('isRestartRequired')).to.be.false;
    });

    it("version hosts don't need restart", function() {
      model.setProperties({
        service: Em.Object.create({
          isRestartRequired: true,
          restartRequiredHostsAndComponents: {}
        }),
        isCurrent: true,
        hosts: ['host1']
      });
      expect(model.get('isRestartRequired')).to.be.false;
    });

    it("version hosts need restart", function() {
      model.setProperties({
        service: Em.Object.create({
          isRestartRequired: true,
          restartRequiredHostsAndComponents: {'host1': {}}
        }),
        isCurrent: true,
        hosts: ['host1']
      });
      expect(model.get('isRestartRequired')).to.be.true;
    });

  });

  describe("#disabledActionMessages", function() {
    var testCases = [
      {
        input: {
          isDisplayed: false,
          isCurrent: false
        },
        expected: {
          view: '',
          compare: '',
          revert: ''
        }
      },
      {
        input: {
          isDisplayed: true,
          isCurrent: false
        },
        expected: {
          view: Em.I18n.t('dashboard.configHistory.info-bar.view.button.disabled'),
          compare: Em.I18n.t('dashboard.configHistory.info-bar.compare.button.disabled'),
          revert: ''
        }
      },
      {
        input: {
          isDisplayed: false,
          isCurrent: true
        },
        expected: {
          view: '',
          compare: '',
          revert: Em.I18n.t('dashboard.configHistory.info-bar.revert.button.disabled')
        }
      },
      {
        input: {
          isDisplayed: true,
          isCurrent: true
        },
        expected: {
          view: Em.I18n.t('dashboard.configHistory.info-bar.view.button.disabled'),
          compare: Em.I18n.t('dashboard.configHistory.info-bar.compare.button.disabled'),
          revert: Em.I18n.t('dashboard.configHistory.info-bar.revert.button.disabled')
        }
      }
    ];

    testCases.forEach(function(test) {
      it("isDisplayed = " + test.input.isDisplayed + ", isCurrent = " + test.input.isCurrent, function() {
        model.setProperties(test.input);
        expect(model.get('disabledActionMessages')).to.eql(test.expected);
      });
    });

  });

  describe("#disabledActionAttr", function() {
    var testCases = [
      {
        input: {
          isDisplayed: false,
          isCurrent: false,
          isDisabled: false
        },
        expected: {
          view: false,
          compare: false,
          revert: false
        }
      },
      {
        input: {
          isDisplayed: true,
          isCurrent: false,
          isDisabled: false
        },
        expected: {
          view: 'disabled',
          compare: 'disabled',
          revert: false
        }
      },
      {
        input: {
          isDisplayed: false,
          isCurrent: false,
          isDisabled: true
        },
        expected: {
          view: false,
          compare: 'disabled',
          revert: 'disabled'
        }
      },
      {
        input: {
          isDisplayed: false,
          isCurrent: true,
          isDisabled: false
        },
        expected: {
          view: false,
          compare: false,
          revert: 'disabled'
        }
      },
      {
        input: {
          isDisplayed: true,
          isCurrent: true,
          isDisabled: true
        },
        expected: {
          view: 'disabled',
          compare: 'disabled',
          revert: 'disabled'
        }
      }
    ];

    testCases.forEach(function(test) {
      it("isDisplayed = " + test.input.isDisplayed + ", isCurrent = " + test.input.isCurrent + ", isDisabled = " + test.input.isDisabled, function() {
        model.setProperties(test.input);
        expect(model.get('disabledActionAttr')).to.eql(test.expected);
      });
    });

  });

});
