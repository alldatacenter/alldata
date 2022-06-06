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
require('mappers/configs/themes_mapper');
require('models/configs/theme/tab');
require('models/configs/theme/section');
require('models/configs/theme/sub_section');

describe('App.themeMapper', function () {

  describe('#map', function() {
    var json = {
      items: [
        {}
      ]
    };
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'mapThemeLayouts');
      sinon.stub(App.themesMapper, 'mapThemeConfigs');
      sinon.stub(App.themesMapper, 'mapThemeWidgets');
      sinon.stub(App.themesMapper, 'generateAdvancedTabs');
      sinon.stub(App.store, 'safeLoadMany');
      App.themesMapper.map(json, ['S1']);
    });
    afterEach(function() {
      App.store.safeLoadMany.restore();
      App.themesMapper.mapThemeLayouts.restore();
      App.themesMapper.mapThemeConfigs.restore();
      App.themesMapper.mapThemeWidgets.restore();
      App.themesMapper.generateAdvancedTabs.restore();
      App.set('enableLogger', true);
    });

    it('mapThemeLayouts should be called', function() {
      expect(App.themesMapper.mapThemeLayouts.calledWith({}, [])).to.be.true;
    });
    it('mapThemeConfigs should be called', function() {
      expect(App.themesMapper.mapThemeConfigs.calledWith({})).to.be.true;
    });
    it('mapThemeWidgets should be called', function() {
      expect(App.themesMapper.mapThemeWidgets.calledWith({})).to.be.true;
    });
    it('generateAdvancedTabs should be called', function() {
      expect(App.themesMapper.generateAdvancedTabs.calledWith(['S1'])).to.be.true;
    });
    it('App.store.safeLoadMany should be called', function() {
      expect(App.store.safeLoadMany.calledOnce).to.be.true;
    });
  });

  describe('#mapThemeLayouts', function() {
    var json = {
      ThemeInfo: {
        service_name: 'S1',
        theme_data: {
          Theme: {
            name: 'theme1',
            configuration: {
              layouts: [
                {
                  tabs: [
                    {
                      name: 't1',
                      layout: {
                        sections: [
                          {
                            "name": 'SEC1',
                            "display-name": 'sec1',
                            "row-index": 1,
                            "column-index": 2,
                            "row-span": 3,
                            "column-span": 4,
                            "section-columns": 5,
                            "section-rows": 6,
                            "tab_id": 1
                          }
                        ]
                      }
                    }
                  ]
                }
              ]
            }
          }
        }
      }
    };
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'loadSubSections');
      sinon.stub(App.store, 'safeLoadMany');
    });
    afterEach(function() {
      App.themesMapper.loadSubSections.restore();
      App.store.safeLoadMany.restore();
    });

    it('loadSubSections should be called', function() {
      App.themesMapper.mapThemeLayouts(json, []);
      expect(App.themesMapper.loadSubSections.getCall(0).args).to.be.eql([
        {
          "name": 'SEC1',
          "display-name": 'sec1',
          "row-index": 1,
          "column-index": 2,
          "row-span": 3,
          "column-span": 4,
          "section-columns": 5,
          "section-rows": 6,
          "tab_id": 1
        },
        {
          "column_index": 2,
          "column_span": 4,
          "display_name": "sec1",
          "id": "SEC1_S1_t1",
          "name": "SEC1",
          "row_index": 1,
          "row_span": 3,
          "section_columns": 5,
          "section_rows": 6,
          "tab_id": "S1_t1"
        }, 'S1', 'theme1'
      ])
    });

    it('App.store.safeLoadMany should be called', function() {
      App.themesMapper.mapThemeLayouts(json, []);
      expect(App.store.safeLoadMany.getCall(0).args[1]).to.be.eql([{
        "column_index": 2,
        "column_span": 4,
        "display_name": "sec1",
        "id": "SEC1_S1_t1",
        "name": "SEC1",
        "row_index": 1,
        "row_span": 3,
        "section_columns": 5,
        "section_rows": 6,
        "tab_id": "S1_t1"
      }]);
    });

    it('should add tab to tabs', function() {
      var tabs = [];
      App.themesMapper.mapThemeLayouts(json, tabs);
      expect(tabs).to.have.length(1);
    });
  });

  describe('#loadSubSections', function() {
    var section = {
      subsections: [
        {
          "name": 'SSEC1',
          "display-name": 'ssec1',
          "border": 1,
          "row-index": 2,
          "column-index": 3,
          "column-span": 4,
          "row-span": 5,
          "config_properties": [],
          "section_id": 1,
          "depends-on": [],
          "left-vertical-splitter": 6
        }
      ]
    };
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'loadSubSectionTabs');
      sinon.stub(App.themesMapper, 'mapThemeConditions');
      sinon.stub(App.store, 'safeLoadMany');
    });
    afterEach(function() {
      App.store.safeLoadMany.restore();
      App.themesMapper.loadSubSectionTabs.restore();
      App.themesMapper.mapThemeConditions.restore();
    });

    it('loadSubSectionTabs should be called', function() {
      App.themesMapper.loadSubSections(section, {id: 1}, 's1', 't1');
      expect(App.themesMapper.loadSubSectionTabs.getCall(0).args).to.be.eql([
        {
          "name": 'SSEC1',
          "display-name": 'ssec1',
          "border": 1,
          "row-index": 2,
          "column-index": 3,
          "column-span": 4,
          "row-span": 5,
          "config_properties": [],
          "section_id": 1,
          "depends-on": [],
          "left-vertical-splitter": 6
        },
        {
          "border": 1,
          "column_index": 3,
          "column_span": 4,
          "configProperties": [],
          "depends_on": [],
          "display_name": "ssec1",
          "id": "SSEC1_s1_t1",
          "left_vertical_splitter": 6,
          "name": "SSEC1",
          "row_index": 2,
          "row_span": 5,
          "section_id": 1,
          "theme_name": "t1",
        },
        "t1"
      ]);
    });

    it('mapThemeConditions should be called', function() {
      App.themesMapper.loadSubSections(section, {id: 1}, 's1', 't1');
      expect(App.themesMapper.mapThemeConditions.getCall(0).args).to.be.eql([
        [{
          "border": 1,
          "column_index": 3,
          "column_span": 4,
          "configProperties": [],
          "depends_on": [],
          "display_name": "ssec1",
          "id": "SSEC1_s1_t1",
          "left_vertical_splitter": 6,
          "name": "SSEC1",
          "row_index": 2,
          "row_span": 5,
          "section_id": 1,
          "theme_name": "t1"
        }],
        'subsection',
        "t1"
      ]);
    });

    it('App.store.safeLoadMany should be called', function() {
      App.themesMapper.loadSubSections(section, {id: 1}, 's1', 't1');
      expect(App.store.safeLoadMany.getCall(0).args[1]).to.be.eql([
        {
          "border": 1,
          "column_index": 3,
          "column_span": 4,
          "configProperties": [],
          "depends_on": [],
          "display_name": "ssec1",
          "id": "SSEC1_s1_t1",
          "left_vertical_splitter": 6,
          "name": "SSEC1",
          "row_index": 2,
          "row_span": 5,
          "section_id": 1,
          "theme_name": "t1"
        }
      ]);
    });

    it('sub_sections should be set', function() {
      var parsedSection = {id: 1};
      App.themesMapper.loadSubSections(section, parsedSection);
      expect(parsedSection.sub_sections).to.have.length(1);
    });
  });

  describe('#loadSubSectionTabs', function() {
    var subSection = {
      "subsection-tabs": [
        {
          "name": 'SEC1',
          "display-name": 'sec1',
          "depends-on": [],
          "sub_section_id": 'SUB1'
        }
      ]
    };
    beforeEach(function() {
      sinon.stub(App.themesMapper, 'mapThemeConditions');
      sinon.stub(App.store, 'safeLoadMany');
    });
    afterEach(function() {
      App.themesMapper.mapThemeConditions.restore();
      App.store.safeLoadMany.restore();
    });

    it('mapThemeConditions should be called', function() {
      App.themesMapper.loadSubSectionTabs(subSection, {id: 2}, "t1");
      expect(App.themesMapper.mapThemeConditions.getCall(0).args).to.be.eql([
        [{
          "depends_on": [],
          "display_name": "sec1",
          "id": "SEC1",
          "is_active": true,
          "name": "SEC1",
          "sub_section_id": 2,
          "theme_name": "t1"
        }],
        'subsectionTab',
        "t1"
      ]);
    });

    it('App.store.safeLoadMany should be called', function() {
      App.themesMapper.loadSubSectionTabs(subSection, {id: 2}, "t1");
      expect(App.store.safeLoadMany.getCall(0).args[1]).to.be.eql([
        {
          "depends_on": [],
          "display_name": "sec1",
          "id": "SEC1",
          "is_active": true,
          "name": "SEC1",
          "sub_section_id": 2,
          "theme_name": "t1"
        }
      ]);
    });

    it('sub_section_tabs should be set', function() {
      var parsedSubSection = {id: 3};
      App.themesMapper.loadSubSectionTabs(subSection, parsedSubSection);
      expect(parsedSubSection.sub_section_tabs).to.have.length(1);
    });
  });

  describe('#mapThemeConfigs', function() {
    var json;

    beforeEach(function() {
      json = {
        ThemeInfo: {
          service_name: 'S1',
          theme_data: {
            Theme: {
              configuration: {
                placement: {
                  configs: [{
                    "subsection-name": '',
                    "subsection-tab-name": '',
                    "depends-on": [{}]
                  }]
                }
              }
            }
          }
        }
      };
      this.mockSection = sinon.stub(App.SubSection, 'find');
      this.mockSectionTab = sinon.stub(App.SubSectionTab, 'find');
      sinon.stub(App.configsCollection, 'getConfig').returns({id: 'foo'});
      sinon.stub(App.themesMapper, 'getConfigByAttributes').returns({});
      sinon.stub(App.themesMapper, 'mapThemeConfigConditions');
    });
    afterEach(function() {
      this.mockSection.restore();
      this.mockSectionTab.restore();
      App.configsCollection.getConfig.restore();
      App.themesMapper.getConfigByAttributes.restore();
      App.themesMapper.mapThemeConfigConditions.restore();
    });

    it('should call mapThemeConfigConditions', function() {
      App.themesMapper.mapThemeConfigs(json);
      expect(App.themesMapper.mapThemeConfigConditions.calledOnce).to.be.true;
    });

    it('should call getConfigByAttributes', function() {
      App.themesMapper.mapThemeConfigs(json);
      expect(App.themesMapper.getConfigByAttributes.calledOnce).to.be.true;
    });

    it('should set config to subSection', function() {
      json.ThemeInfo.theme_data.Theme.configuration.placement.configs[0]["subsection-name"] = 'ss-1';
      var subSection = Em.Object.create({
        configProperties: [],
        isLoaded: true
      });
      this.mockSection.returns(subSection);
      App.themesMapper.mapThemeConfigs(json);
      expect(subSection.get('configProperties')).to.be.eql(['foo']);
    });

    it('should set config to SubSectionTab', function() {
      json.ThemeInfo.theme_data.Theme.configuration.placement.configs[0]["subsection-tab-name"] = 'sst-1';
      var subSectionTab = Em.Object.create({
        configProperties: [],
        isLoaded: true
      });
      this.mockSectionTab.returns(subSectionTab);
      App.themesMapper.mapThemeConfigs(json);
      expect(subSectionTab.get('configProperties')).to.be.eql(['foo']);
    });
  });

  describe('#mapThemeConfigConditions', function () {
    beforeEach(function () {
      sinon.stub(App.store, 'safeLoadMany');
    });
    afterEach(function () {
      App.store.safeLoadMany.restore();
    });

    it('App.store.safeLoadMany should be called', function () {
      var configConditions = [
        {
          configs: ['foo/bar']
        }
      ];
      var configProperty = {
        id: 'c1',
        name: 'config1',
        filename: 'file1.xml'
      };
      App.themesMapper.mapThemeConfigConditions(configConditions, configProperty);
      expect(App.store.safeLoadMany.getCall(0).args[1][0]).to.be.eql({
        "config_name": "config1",
        "configs": [
          {
            "configName": "bar",
            "fileName": "foo.xml"
          }
        ],
        "file_name": "file1.xml",
        "id": "c1_0",
        "resource": "config",
        "type": "config"
      });
    });
  });

  describe('#mapThemeConditions', function () {
    beforeEach(function () {
      sinon.stub(App.store, 'safeLoadMany');
    });
    afterEach(function () {
      App.store.safeLoadMany.restore();
    });

    it('App.store.safeLoadMany should be called', function () {
      var subSections = [
        {
          id: 'ss-1',
          name: 'sub-section',
          "depends_on": [
            {
              configs: ['foo/bar']
            }
          ]
        }
      ];

      App.themesMapper.mapThemeConditions(subSections, 'type1', "t1");
      expect(App.store.safeLoadMany.getCall(0).args[1][0]).to.be.eql({
        "id": "ss-1_0",
        "name": 'sub-section',
        "configs": [
          {
            "configName": "bar",
            "fileName": "foo.xml"
          }
        ],
        "resource": "config",
        "theme_name": "t1",
        "type": "type1"
      });
    });
  });

  describe('#mapThemeWidgets', function() {
    var json = {
      ThemeInfo: {
        theme_data: {
          Theme: {
            configuration: {
              widgets: [
                {
                  widget: {
                    type: 'foo'
                  },
                  config: 'foo/bar'
                }
              ]
            }
          }
        }
      }
    };
    beforeEach(function() {
      this.mockConfig = sinon.stub(App.configsCollection, 'getConfig');
      this.mockConfigByName = sinon.stub(App.configsCollection, 'getConfigByName');
    });
    afterEach(function() {
      this.mockConfig.restore();
      this.mockConfigByName.restore();
    });

    it('should set widget to configProperty', function() {
      var configProperty = Em.Object.create();
      this.mockConfig.returns(configProperty);
      App.themesMapper.mapThemeWidgets(json);
      expect(configProperty.get('widget')).to.be.eql({type: 'foo'});
      expect(configProperty.get('widgetType')).to.be.equal('foo');
    });
    it('should set widget to uiOnlyProperty', function() {
      var uiOnlyProperty = Em.Object.create();
      this.mockConfigByName.returns(uiOnlyProperty);
      App.themesMapper.mapThemeWidgets(json);
      expect(App.configsCollection.getConfigByName.calledWith('bar', 'foo.xml')).to.be.true;
      expect(uiOnlyProperty.get('widget')).to.be.eql({type: 'foo'});
      expect(uiOnlyProperty.get('widgetType')).to.be.equal('foo');
    });
  });

  describe('#getConfigId', function() {
    it('should return config id', function() {
      expect(App.themesMapper.getConfigId({config: 'foo/bar'})).to.be.equal('bar__foo');
    });
    it('should return null', function() {
      expect(App.themesMapper.getConfigId(null)).to.be.null;
    });
  });

  describe('#generateAdvancedTabs', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns([]);
      sinon.stub(App.store, 'safeLoadMany');
    });
    afterEach(function() {
      App.StackService.find.restore();
      App.store.safeLoadMany.restore();
    });

    it('App.store.safeLoadMany should be called', function() {
      App.themesMapper.generateAdvancedTabs(['S1']);
      expect(App.store.safeLoadMany.getCall(0).args[1][0]).to.be.eql({
        id: 'S1_advanced',
        name: 'advanced',
        display_name: 'Advanced',
        is_advanced: true,
        service_name: 'S1'
      });
    });
  });

  describe('#getConfigByAttributes', function() {
    var configLink = {
      "config": 'foo/bar',
      "property_value_attributes": {
        "ui_only_property": {}
      }
    };
    beforeEach(function() {
      sinon.stub(App.configsCollection, 'getConfig').returns(null);
      sinon.stub(App.configsCollection, 'add');
    });
    afterEach(function() {
      App.configsCollection.getConfig.restore();
      App.configsCollection.add.restore();
    });

    it('App.configsCollection.add should be called', function() {
      var subSection = Em.Object.create({configProperties: []});
      var subSectionTab = Em.Object.create({configProperties: []});
      App.themesMapper.getConfigByAttributes({}, configLink, subSection, subSectionTab, 'S1');
      expect(App.configsCollection.add.getCall(0).args[0]).to.be.eql({
        id: 'bar__foo',
        isRequiredByAgent: false,
        showLabel: false,
        isOverridable: false,
        recommendedValue: true,
        name: 'bar',
        isUserProperty: false,
        filename: 'foo.xml',
        fileName: 'foo.xml',
        isNotSaved: false,
        serviceName: 'S1',
        copy: '',
        stackName: 'HDP',
        stackVersion: '2.3'
      });
    });

    it('should return configProperty', function() {
      var subSection = Em.Object.create({configProperties: []});
      var subSectionTab = Em.Object.create({configProperties: []});
      expect(App.themesMapper.getConfigByAttributes({}, configLink, subSection, subSectionTab, 'S1')).to.be.eql({
        id: 'bar__foo',
        isRequiredByAgent: false,
        showLabel: false,
        isOverridable: false,
        recommendedValue: true,
        name: 'bar',
        isUserProperty: false,
        filename: 'foo.xml',
        fileName: 'foo.xml',
        isNotSaved: false,
        serviceName: 'S1',
        copy: '',
        stackName: 'HDP',
        stackVersion: '2.3'
      });
    });

    it('should add config to SubSection properties', function() {
      var subSection = Em.Object.create({configProperties: []});
      var subSectionTab = Em.Object.create({configProperties: []});
      App.themesMapper.getConfigByAttributes({}, configLink, subSection, subSectionTab, 'S1');
      expect(subSection.get('configProperties')).to.be.eql(['bar__foo']);
    });
  });
});
