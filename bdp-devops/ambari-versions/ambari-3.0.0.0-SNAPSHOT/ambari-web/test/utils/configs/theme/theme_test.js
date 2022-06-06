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

require('utils/configs/theme/theme');

describe('App.configTheme', function() {

  describe("#resolveConfigThemeConditions()", function () {

    beforeEach(function() {
      this.mockThemeCondition = sinon.stub(App.ThemeCondition, 'find');
      sinon.stub(App.configTheme, 'calculateConfigCondition').returns(true);
      this.mockId = sinon.stub(App.config, 'configId');
      sinon.stub(App.configTheme, 'getThemeResource').returns(Em.Object.create({
        configProperties: ['conf1']
      }));
    });

    afterEach(function() {
      this.mockId.restore();
      this.mockThemeCondition.restore();
      App.configTheme.calculateConfigCondition.restore();
      App.configTheme.getThemeResource.restore();
    });

    it("theme configs empty", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: false
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: []
        })
      ]);
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.called).to.be.false;
      expect(configs[0].get('hiddenBySection')).to.be.false;
    });

    it("theme resource is not 'config'", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: false
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: ''
        })
      ]);
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.called).to.be.false;
      expect(configs[0].get('hiddenBySection')).to.be.false;
    });

    it("property_value_attributes is null", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: false
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: 'config',
          then: {
            property_value_attributes: null
          },
          id: 'c1'
        })
      ]);
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.calledOnce).to.be.true;
      expect(App.configTheme.getThemeResource.called).to.be.false;
      expect(configs[0].get('hiddenBySection')).to.be.false;
    });

    it("property_value_attributes.visible is null", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: false
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: 'config',
          then: {
            property_value_attributes: {
              visible: null
            }
          },
          id: 'c1'
        })
      ]);
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.calledOnce).to.be.true;
      expect(App.configTheme.getThemeResource.called).to.be.false;
      expect(configs[0].get('hiddenBySection')).to.be.false;
    });

    it("config not in the theme", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: false
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: 'config',
          then: {
            property_value_attributes: {
              visible: true
            }
          },
          id: 'c1'
        })
      ]);
      this.mockId.returns('conf2');
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.calledOnce).to.be.true;
      expect(App.configTheme.getThemeResource.calledOnce).to.be.true;
      expect(configs[0].get('hiddenBySection')).to.be.false;
    });

    it("configCondition type is 'config' and hiddenBySection is true", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: true
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: 'config',
          then: {
            property_value_attributes: {
              visible: true
            }
          },
          id: 'c1',
          type: 'config'
        })
      ]);
      this.mockId.returns('conf1');
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.calledOnce).to.be.true;
      expect(App.configTheme.getThemeResource.calledOnce).to.be.true;
      expect(configs[0].get('hiddenBySection')).to.be.true;
    });

    it("hiddenBySection should be true", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: false
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: 'config',
          then: {
            property_value_attributes: {
              visible: false
            }
          },
          id: 'c1',
          type: 'config'
        })
      ]);
      this.mockId.returns('conf1');
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.calledOnce).to.be.true;
      expect(App.configTheme.getThemeResource.calledOnce).to.be.true;
      expect(configs[0].get('hiddenBySection')).to.be.true;
    });

    it("hiddenBySection should be false", function() {
      var configs = [Em.Object.create({
        name: 'conf1',
        filename: 'file1',
        hiddenBySection: true
      })];
      this.mockThemeCondition.returns([
        Em.Object.create({
          configs: [{}],
          resource: 'config',
          then: {
            property_value_attributes: {
              visible: true
            }
          },
          id: 'c1'
        })
      ]);
      this.mockId.returns('conf1');
      App.configTheme.resolveConfigThemeConditions(configs);
      expect(App.configTheme.calculateConfigCondition.calledOnce).to.be.true;
      expect(App.configTheme.getThemeResource.calledOnce).to.be.true;
      expect(configs[0].get('hiddenBySection')).to.be.false;
    });
  });

  describe("#getThemeResource()", function () {

    beforeEach(function() {
      sinon.stub(App.SubSection, 'find').returns([Em.Object.create({name: 'ss1'})]);
      sinon.stub(App.SubSectionTab, 'find').returns([Em.Object.create({name: 'sst1'})]);
    });

    afterEach(function() {
      App.SubSection.find.restore();
      App.SubSectionTab.find.restore();
    });

    it("configCondition with unknown type", function() {
      expect(App.configTheme.getThemeResource(Em.Object.create())).to.be.null;
    });

    it("configCondition with subsection type", function() {
      expect(App.configTheme.getThemeResource(Em.Object.create({
        name: 'ss1',
        type: 'subsection'
      }))).to.be.eql(Em.Object.create({name: 'ss1'}));
    });

    it("configCondition with subsectionTab type", function() {
      expect(App.configTheme.getThemeResource(Em.Object.create({
        name: 'sst1',
        type: 'subsectionTab'
      }))).to.be.eql(Em.Object.create({name: 'sst1'}));
    });

    it("configCondition with config type", function() {
      expect(App.configTheme.getThemeResource(Em.Object.create({
        configName: 'conf1',
        fileName: 'file1',
        type: 'config'
      }))).to.be.eql(Em.Object.create({
          configProperties: ['conf1__file1']
        }));
    });
  });

  describe("#getConfigThemeActions()", function () {

    beforeEach(function() {
      sinon.stub(App.configTheme, 'getConfigActions').returns([
        Em.Object.create({
          if: '',
          then: 'add',
          else: 'delete',
          hostComponent: 'C1'
        })
      ]);
      this.mock = sinon.stub(App.configTheme, 'calculateConfigCondition');
    });

    afterEach(function() {
      this.mock.restore();
      App.configTheme.getConfigActions.restore();
    });

    it("should add component", function() {
      this.mock.returns(true);
      expect(App.configTheme.getConfigThemeActions([], [])).to.be.eql({
        add: ['C1'],
        delete: []
      });
    });

    it("should delete component", function() {
      this.mock.returns(false);
      expect(App.configTheme.getConfigThemeActions([], [])).to.be.eql({
        add: [],
        delete: ['C1']
      });
    });
  });

  describe("#getConfigActions()", function () {

    beforeEach(function() {
      this.mock = sinon.stub(App.ConfigAction, 'find');
    });

    afterEach(function() {
      this.mock.restore();
    });

    it("action has empty configs", function() {
      this.mock.returns([
        Em.Object.create({
          configs: []
        })
      ]);
      expect(App.configTheme.getConfigActions([], [])).to.be.empty;
    });

    it("empty configs", function() {
      this.mock.returns([
        Em.Object.create({
          configs: [{
            fileName: 'file1',
            configName: 'conf1'
          }]
        })
      ]);
      expect(App.configTheme.getConfigActions([], [])).to.be.empty;
    });

    it("empty storedConfig, config not changed", function() {
      this.mock.returns([
        Em.Object.create({
          configs: [{
            fileName: 'file1',
            configName: 'conf1'
          }]
        })
      ]);
      var config = Em.Object.create({
        filename: 'file1',
        name: 'conf1',
        savedValue: 'val1',
        value: 'val1',
        recommendedValue: 'val1'
      });
      expect(App.configTheme.getConfigActions([config], [])).to.be.empty;
    });

    it("empty storedConfig, savedValue changed", function() {
      this.mock.returns([
        Em.Object.create({
          configs: [{
            fileName: 'file1',
            configName: 'conf1'
          }]
        })
      ]);
      var config = Em.Object.create({
        filename: 'file1',
        name: 'conf1',
        savedValue: 'val1',
        value: 'val2',
        recommendedValue: 'val1'
      });
      expect(App.configTheme.getConfigActions([config], [])).to.be.eql([Em.Object.create({
        configs: [{
          fileName: 'file1',
          configName: 'conf1'
        }]
      })]);
    });

    it("empty storedConfig, recommendedValue changed", function() {
      this.mock.returns([
        Em.Object.create({
          configs: [{
            fileName: 'file1',
            configName: 'conf1'
          }]
        })
      ]);
      var config = Em.Object.create({
        filename: 'file1',
        name: 'conf1',
        value: 'val2',
        recommendedValue: 'val1'
      });
      expect(App.configTheme.getConfigActions([config], [])).to.be.eql([Em.Object.create({
        configs: [{
          fileName: 'file1',
          configName: 'conf1'
        }]
      })]);
    });

    it("storedConfig not changed", function() {
      this.mock.returns([
        Em.Object.create({
          configs: [{
            fileName: 'file1',
            configName: 'conf1'
          }]
        })
      ]);
      var config = Em.Object.create({
        filename: 'file1',
        name: 'conf1',
        savedValue: 'val1',
        value: 'val1',
        recommendedValue: 'val1'
      });
      var storedConfig = {
        filename: 'file1',
        name: 'conf1',
        value: 'val1'
      };
      expect(App.configTheme.getConfigActions([config], [storedConfig])).to.be.empty;
    });

    it("storedConfig changed", function() {
      this.mock.returns([
        Em.Object.create({
          configs: [{
            fileName: 'file1',
            configName: 'conf1'
          }]
        })
      ]);
      var config = Em.Object.create({
        filename: 'file1',
        name: 'conf1',
        savedValue: 'val1',
        value: 'val1',
        recommendedValue: 'val1'
      });
      var storedConfig = {
        filename: 'file1',
        name: 'conf1',
        value: 'val2'
      };
      expect(App.configTheme.getConfigActions([config], [storedConfig])).to.be.eql([Em.Object.create({
        configs: [{
          fileName: 'file1',
          configName: 'conf1'
        }]
      })]);
    });
  });

  describe("#calculateConfigCondition()", function () {
    var testCases = [
      {
        ifStatement: "${file1/conf1}",
        serviceConfigs: [],
        expected: false
      },
      {
        ifStatement: "${file1/conf1}",
        serviceConfigs: [Em.Object.create({
          filename: 'file1.xml',
          name: 'conf1',
          value: 'true'
        })],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}&&${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'true'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'true'
          })
        ],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}&&${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'false'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'false'
          })
        ],
        expected: false
      },
      {
        ifStatement: "${file1/conf1}&&${file1/conf2}===false",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'true'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'false'
          })
        ],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}&&${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'true'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'false'
          })
        ],
        expected: false
      },
      {
        ifStatement: "${file1/conf1}===false&&${file1/conf2}===false",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'false'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'false'
          })
        ],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}||${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'true'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'true'
          })
        ],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}||${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'false'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'true'
          })
        ],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}||${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'true'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'false'
          })
        ],
        expected: true
      },
      {
        ifStatement: "${file1/conf1}||${file1/conf2}",
        serviceConfigs: [
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf1',
            value: 'false'
          }),
          Em.Object.create({
            filename: 'file1.xml',
            name: 'conf2',
            value: 'false'
          })
        ],
        expected: false
      }
    ];

    testCases.forEach(function(test) {
      it("ifStatement: " + test.ifStatement +
         "serviceConfigs: " + JSON.stringify(test.serviceConfigs), function() {
        expect(App.configTheme.calculateConfigCondition(test.ifStatement, test.serviceConfigs)).to.be.equal(test.expected);
      });
    });
  });

});
