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

require('models/configs/objects/service_config_category');
require('models/configs/objects/service_config_property');

var serviceConfigProperty,
  serviceConfigPropertyInit,
  configsData = [
    Ember.Object.create({
      category: 'c0',
      overrides: [
        {
          error: true,
          errorMessage: 'error'
        },
        {
          error: true
        },
        {}
      ]
    }),
    Ember.Object.create({
      category: 'c1',
      isValid: false,
      isVisible: true
    }),
    Ember.Object.create({
      category: 'c0',
      isValid: true,
      isVisible: true
    }),
    Ember.Object.create({
      category: 'c1',
      isValid: false,
      isVisible: false
    })
  ],

  overridableFalseData = [
    {
      isOverridable: false
    },
    {
      isEditable: false,
      overrides: configsData[0].overrides
    },
    {
      displayType: 'componentHost'
    }
  ],
  overridableTrueData = [
    {
      isOverridable: true,
      isEditable: true
    },
    {
      isOverridable: true,
      overrides: []
    },
    {
      isOverridable: true
    }
  ],
  removableFalseData = [
    {
      isEditable: false
    },
    {
      hasOverrides: true
    },
    {
      isUserProperty: false,
      isOriginalSCP: true
    }
  ],
  removableTrueData = [
    {
      isEditable: true,
      hasOverrides: false,
      isUserProperty: true
    },
    {
      isEditable: true,
      hasOverrides: false,
      isOriginalSCP: false
    }
  ],
  initPropertyData = [
    {
      initial: {
        displayType: 'password',
        value: 'value',
        recommendedValue: 'recommended'
      },
      result: {
        retypedPassword: 'value',
        recommendedValue: ''
      }
    },
    {
      initial: {
        value: '',
        savedValue: 'default',
        recommendedValue: 'recommended'
      },
      result: {
        value: '',
        recommendedValue: 'recommended'
      }
    },
    {
      initial: {
        value: null,
        savedValue: 'default',
        recommendedValue: 'recommended'
      },
      result: {
        value: 'default',
        recommendedValue: 'recommended'
      }
    }
  ],
  notDefaultFalseData = [
    {
      isEditable: false
    },
    {
      savedValue: null
    },
    {
      value: 'value',
      savedValue: 'value'
    }
  ],
  notDefaultTrueData = {
    isEditable: true,
    value: 'value',
    savedValue: 'default'
  };


function getProperty() {
  return App.ServiceConfigProperty.create();
}

describe('App.ServiceConfigProperty', function () {

  beforeEach(function () {
    serviceConfigProperty = getProperty();
  });

  App.TestAliases.testAsComputedAnd(getProperty(), 'hideFinalIcon', ['!isFinal', 'isNotEditable']);

  App.TestAliases.testAsComputedAnd(getProperty(), 'isActive', ['isVisible', '!hiddenBySubSection', '!hiddenBySection']);

  describe('#placeholder', function () {
      [
        {
          placeholderText: 'foo',
          savedValue: ''
        },
        {
          placeholderText: '',
          savedValue: 'foo'
        },
        {
          placeholderText: 'foo',
          savedValue: 'bar'
        }
      ].forEach(function (item) {
        it('should equal foo, placeholder = ' + JSON.stringify(item.placeholderText), function() {
          serviceConfigProperty.set('isEditable', true);
          serviceConfigProperty.set('placeholderText', item.placeholderText);
          serviceConfigProperty.set('savedValue', item.savedValue);
          expect(serviceConfigProperty.get('placeholder')).to.equal('foo');
        });
    });
    it('should equal null', function() {
      serviceConfigProperty.set('isEditable', false);
      serviceConfigProperty.set('placeholderText', 'foo');
      serviceConfigProperty.set('savedValue', 'bar');
      expect(serviceConfigProperty.get('placeholder')).to.equal(null);
    });
  });
  describe('#isPropertyOverridable', function () {
    overridableFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isPropertyOverridable')).to.be.false;
      });
    });
    overridableTrueData.forEach(function (item) {
      it('should be true', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isPropertyOverridable')).to.be.true;
      });
    });
  });

  App.TestAliases.testAsComputedOr(getProperty(), 'isOverridden', ['overrides.length', '!isOriginalSCP']);

  describe('#isRemovable', function () {
    removableFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isRemovable')).to.be.false;
      });
    });
    removableTrueData.forEach(function (item) {
      it('should be true', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isRemovable')).to.be.true;
      });
    });
  });

  describe('#init', function () {
    initPropertyData.forEach(function (item) {
      describe('should set initial data for ' + JSON.stringify(item), function () {
        beforeEach(function () {
          serviceConfigPropertyInit = App.ServiceConfigProperty.create(item.initial);
        });
        Em.keys(item.result).forEach(function (prop) {
          it(prop, function () {
            expect(serviceConfigPropertyInit.get(prop)).to.equal(item.result[prop]);
          });
        });
      });
    });
  });

  describe('#isNotDefaultValue', function () {
    notDefaultFalseData.forEach(function (item) {
      it('should be false', function () {
        serviceConfigProperty.setProperties(item);
        expect(serviceConfigProperty.get('isNotDefaultValue')).to.be.false;
      });
    });
    it('should be true', function () {
      Em.keys(notDefaultTrueData).forEach(function (prop) {
        serviceConfigProperty.set(prop, notDefaultTrueData[prop]);
      });
      expect(serviceConfigProperty.get('isNotDefaultValue')).to.be.true;
    });
  });

  App.TestAliases.testAsComputedExistsIn(getProperty(), 'cantBeUndone', 'displayType', ['componentHost', 'componentHosts', 'radio button']);

  describe('#isValid', function () {
    it('should be true', function () {
      serviceConfigProperty.set('errorMessage', '');
      expect(serviceConfigProperty.get('isValid')).to.be.true;
    });
    it('should be false', function () {
      serviceConfigProperty.set('errorMessage', 'message');
      expect(serviceConfigProperty.get('isValid')).to.be.false;
    });
  });

  describe('#overrideIsFinalValues', function () {
    it('should be defined as empty array', function () {
      expect(serviceConfigProperty.get('overrideIsFinalValues')).to.eql([]);
    });
  });

  describe('custom validation for `ranger_admin_password`', function () {

    beforeEach(function () {
      this.config = App.ServiceConfigProperty.create({
        name: 'ranger_admin_password',
        displayType: 'password'
      });
    });

    it('value less than 9 symbols is invalid', function () {
      this.config.set('value', 12345678);
      this.config.set('retypedPassword', 12345678);
      expect(this.config.get('isValid')).to.be.false;
      expect(this.config.get('errorMessage')).to.be.equal(Em.I18n.t('errorMessage.config.password.length').format(9));
    });

    it('value with 9 symbols is valid', function () {
      this.config.set('value', 123456789);
      this.config.set('retypedPassword', 123456789);
      expect(this.config.get('isValid')).to.be.true;
      expect(this.config.get('errorMessage')).to.be.equal('');
    });

  });

  App.TestAliases.testAsComputedOr(getProperty(), 'hasIssues', ['error', 'warn', 'overridesWithIssues.length']);

  App.TestAliases.testAsComputedFilterBy(getProperty(), 'overridesWithIssues', 'overrides', 'hasIssues', true);

});
