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

require('mixins/common/widgets/widget_section');
var testHelpers = require('test/helpers');

describe('App.WidgetSectionMixin', function () {

  var mixin;

  beforeEach(function () {
    mixin = Em.Object.create(App.WidgetSectionMixin, {
      layoutNameSuffix: '_suffix',
      sectionNameSuffix: '_section_suffix',
      content: Em.Object.create()
    });
  });

  describe('#isAmbariMetricsInstalled', function () {

    var cases = [
      {
        services: Em.A([]),
        isAmbariMetricsInstalled: false,
        title: 'Ambari Metrics not installed'
      },
      {
        services: Em.A([
          {
            serviceName: 'AMBARI_METRICS'
          }
        ]),
        isAmbariMetricsInstalled: true,
        title: 'Ambari Metrics installed'
      }
    ];

    beforeEach(function () {
      this.stub = sinon.stub(App.Service, 'find');
    });

    afterEach(function () {
      App.Service.find.restore();
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        this.stub.returns(item.services);
        expect(mixin.get('isAmbariMetricsInstalled')).to.equal(item.isAmbariMetricsInstalled);
      });
    });
  });
  
  describe('#defaultLayoutName', function() {
    
    it('should return service layout name', function() {
      mixin.set('content.serviceName', 'S1');
      mixin.propertyDidChange('defaultLayoutName');
      expect(mixin.get('defaultLayoutName')).to.be.equal("default_s1_suffix");
    });
  
    it('should return system layout name', function() {
      mixin.set('content.serviceName', null);
      mixin.propertyDidChange('defaultLayoutName');
      expect(mixin.get('defaultLayoutName')).to.be.equal("default_system_suffix");
    });
  });
  
  describe('#userLayoutName', function() {
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function() {
      App.router.get.restore();
    });
  
    it('should return service layout name', function() {
      mixin.set('content.serviceName', 'S1');
      mixin.propertyDidChange('userLayoutName');
      expect(mixin.get('userLayoutName')).to.be.equal("admin_s1_suffix");
    });
  
    it('should return system layout name', function() {
      mixin.set('content.serviceName', null);
      mixin.propertyDidChange('userLayoutName');
      expect(mixin.get('userLayoutName')).to.be.equal("admin_system_suffix");
    });
  });
  
  describe('#sectionName', function() {
    
    it('should return service layout name', function() {
      mixin.set('content.serviceName', 'S1');
      mixin.propertyDidChange('sectionName');
      expect(mixin.get('sectionName')).to.be.equal("S1_section_suffix");
    });
    
    it('should return system layout name', function() {
      mixin.set('content.serviceName', null);
      mixin.propertyDidChange('sectionName');
      expect(mixin.get('sectionName')).to.be.equal("SYSTEM_section_suffix");
    });
  });
  
  describe('#isServiceWithEnhancedWidgets', function() {
    beforeEach(function() {
      sinon.stub(App.StackService, 'find').returns(Em.Object.create({
        isServiceWithWidgets: false
      }));
    });
    afterEach(function() {
      App.StackService.find.restore();
    });
    
    it('should return false', function() {
      mixin.set('content.serviceName', 'S1');
      mixin.propertyDidChange('isServiceWithEnhancedWidgets');
      expect(mixin.get('isServiceWithEnhancedWidgets')).to.be.false;
    });
  
    it('should return true', function() {
      mixin.reopen({
        sectionName: 'SYSTEM_HEATMAPS'
      });
      mixin.propertyDidChange('isServiceWithEnhancedWidgets');
      expect(mixin.get('isServiceWithEnhancedWidgets')).to.be.true;
    });
  });
  
  describe('#isHDFSFederatedSummary', function() {
    beforeEach(function() {
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function() {
      App.get.restore();
    });
    
    it('should be true', function() {
      mixin.set('content.serviceName', 'HDFS');
      mixin.set('sectionNameSuffix', '_SUMMARY');
      mixin.propertyDidChange('isHDFSFederatedSummary');
      expect(mixin.get('isHDFSFederatedSummary')).to.be.true;
    });
  });
  
  describe('#widgets', function() {
    
    it('should return no widgets when not loaded', function() {
      mixin.set('isWidgetsLoaded', false);
      mixin.propertyDidChange('widgets');
      expect(mixin.get('widgets')).to.be.empty;
    });
  
    it('should return widgets', function() {
      mixin.set('isWidgetsLoaded', true);
      mixin.set('activeWidgetLayout', {
        widgets: [{}]
      });
      mixin.propertyDidChange('widgets');
      expect(mixin.get('widgets')).to.be.eql([{}]);
    });
  });
  
  describe('#switchNameServiceLayout', function() {
    
    it('should set selectedNSWidgetLayout', function() {
      mixin.switchNameServiceLayout({context: {}});
      expect(mixin.get('selectedNSWidgetLayout')).to.be.eql({});
    });
  });
  
  describe('#getActiveWidgetLayout', function() {
  
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('activeWidgetLayout should be empty', function() {
      mixin.getActiveWidgetLayout();
      expect(mixin.get('activeWidgetLayout')).to.be.empty;
    });
  
    it('activeNSWidgetLayouts should be empty', function() {
      mixin.getActiveWidgetLayout();
      expect(mixin.get('activeNSWidgetLayouts')).to.be.empty;
    });
  
    it('isWidgetsLoaded should be empty', function() {
      mixin.reopen({
        isServiceWithEnhancedWidgets: false
      });
      mixin.getActiveWidgetLayout();
      expect(mixin.get('isWidgetsLoaded')).to.be.true;
    });
  
    it('App.ajax.send should be called', function() {
      mixin.reopen({
        isServiceWithEnhancedWidgets: true,
        sectionName: 'section1'
      });
      mixin.getActiveWidgetLayout();
      expect(testHelpers.findAjaxRequest('name', 'widgets.layouts.active.get')[0].data).to.be.eql({
        userName: 'admin',
        sectionName: 'section1',
        urlParams: 'WidgetLayoutInfo/section_name=section1'
      });
    });
  });
  
  describe('#getActiveWidgetLayoutSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'createLayouts');
      sinon.stub(mixin, 'getNameNodeWidgets').returns({
        done: function(callback) {
          callback({
            items: [
              {
                WidgetInfo: {
                  tag: 'space1'
                }
              },
              {
                WidgetInfo: {
                  tag: null
                }
              }
            ]
          });
        }
      });
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        masterComponentGroups: [
          {
            name: 'space1'
          },
          {
            name: 'space2'
          }
        ]
      }));
      sinon.stub(mixin, 'postNNWidgets');
    });
    afterEach(function() {
      mixin.createLayouts.restore();
      mixin.getNameNodeWidgets.restore();
      App.HDFSService.find.restore();
      mixin.postNNWidgets.restore();
    });
    
    it('createLayouts should be called when isHDFSFederatedSummary=false', function() {
      mixin.reopen({
        isHDFSFederatedSummary: false
      });
      mixin.getActiveWidgetLayoutSuccessCallback({items: []});
      expect(mixin.createLayouts.calledWith({items: []})).to.be.true;
    });
  
    it('postNNWidgets should be called when isHDFSFederatedSummary=true', function() {
      mixin.reopen({
        isHDFSFederatedSummary: true
      });
      mixin.getActiveWidgetLayoutSuccessCallback({items: []});
      expect(mixin.postNNWidgets.calledWith({
        WidgetInfo: {
          tag: null
        }
      }, 1, {items: []})).to.be.true;
    });
  });
  
  describe('#postNNWidgets', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'postWidget').returns({
        done: Em.clb
      });
      sinon.stub(mixin, 'createLayouts');
    });
    afterEach(function() {
      mixin.postWidget.restore();
      mixin.createLayouts.restore();
    });
    
    it('createLayouts should be called', function() {
      mixin.postNNWidgets({
        href: '',
        WidgetInfo: {
          id: 1,
          cluster_name: 'cl1',
          author: 'me',
          metrics: JSON.stringify({metrics: []}),
          values: JSON.stringify({values: []})
        }
      }, 1, {});
      expect(mixin.createLayouts.calledWith({})).to.be.true;
    });
  });
  
  describe('#getNameNodeWidgets', function() {
    
    it('App.ajax.send should be called', function() {
      mixin.getNameNodeWidgets();
      expect(testHelpers.findAjaxRequest('name', 'widgets.get')[0].data).to.be.eql({
        urlParams: 'WidgetInfo/widget_type.in(GRAPH,NUMBER,GAUGE)&WidgetInfo/scope=CLUSTER&WidgetInfo/metrics.matches(.*\"component_name\":\"NAMENODE\".*)&fields=*'
      });
    });
  });
  
  describe('#postWidget', function() {
    
    it('App.ajax.send should be called', function() {
      mixin.postWidget({data: []});
      expect(testHelpers.findAjaxRequest('name', 'widgets.wizard.add')[0].data).to.be.eql({
        data: {data: []}
      });
    });
  });
  
  describe('#getAllActiveWidgetLayouts', function() {
  
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      mixin.getAllActiveWidgetLayouts();
      expect(testHelpers.findAjaxRequest('name', 'widgets.layouts.all.active.get')[0].data).to.be.eql({
        userName: 'admin'
      });
    });
  });
  
  describe('#createLayouts', function() {
    beforeEach(function() {
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        isLoaded: true,
        masterComponentGroups: [{}, {}]
      }));
      sinon.stub(mixin, 'createFederationWidgetLayouts');
      sinon.stub(mixin, 'getWidgetLayoutSuccessCallback');
      sinon.stub(mixin, 'createUserWidgetLayoutCallback');
      sinon.stub(mixin, 'getAllActiveWidgetLayouts').returns({
        done: Em.clb
      });
      sinon.stub(mixin, 'getDefaultWidgetLayoutByName').returns({
        done: function(callback) {
          callback({
            items: [
              {
                WidgetLayoutInfo: {}
              }
            ]
          });
        }
      });
      sinon.stub(mixin, 'createUserWidgetLayout').returns({
        done: Em.clb
      });
    });
    afterEach(function() {
      App.HDFSService.find.restore();
      mixin.createFederationWidgetLayouts.restore();
      mixin.getWidgetLayoutSuccessCallback.restore();
      mixin.createUserWidgetLayoutCallback.restore();
      mixin.getAllActiveWidgetLayouts.restore();
      mixin.createUserWidgetLayout.restore();
    });
    
    it('createFederationWidgetLayouts should be called', function() {
      mixin.reopen({
        isHDFSFederatedSummary: true
      });
      mixin.createLayouts({items: [{}]});
      expect(mixin.createFederationWidgetLayouts.calledWith({items: [{}]})).to.be.true;
    });
  
    it('getWidgetLayoutSuccessCallback should be called', function() {
      mixin.reopen({
        isHDFSFederatedSummary: false
      });
      mixin.createLayouts({items: [{}]});
      expect(mixin.getWidgetLayoutSuccessCallback.calledWith({items: [{}]})).to.be.true;
    });
  
    it('createUserWidgetLayoutCallback should be called', function() {
      mixin.createLayouts({items: []});
      expect(mixin.createUserWidgetLayoutCallback.calledOnce).to.be.true;
    });
  });
  
  describe('#createUserWidgetLayoutCallback', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'saveActiveWidgetLayouts').returns({
        done: Em.clb
      });
      sinon.stub(mixin, 'getActiveWidgetLayout');
    });
    afterEach(function() {
      mixin.saveActiveWidgetLayouts.restore();
      mixin.getActiveWidgetLayout.restore();
    });
    
    it('saveActiveWidgetLayouts should be called', function() {
      mixin.createUserWidgetLayoutCallback(
        {resources: [{WidgetLayoutInfo: {id: 1}}]},
        {items: [{WidgetLayoutInfo: {id: 2}}]});
      expect(mixin.saveActiveWidgetLayouts.calledWith(
        {
          "WidgetLayouts": [
            {id: 2},
            {id: 1}
          ]
        }
      )).to.be.true;
    });
  
    it('getActiveWidgetLayout should be called', function() {
      mixin.createUserWidgetLayoutCallback(
        {resources: [{WidgetLayoutInfo: {id: 1}}]},
        {items: [{WidgetLayoutInfo: {id: 2}}]});
      expect(mixin.getActiveWidgetLayout.calledOnce).to.be.true;
    });
  });
  
  describe('#getWidgetLayoutSuccessCallback', function() {
    beforeEach(function() {
      sinon.stub(App.widgetMapper, 'map');
      sinon.stub(App.widgetLayoutMapper, 'map');
      sinon.stub(App.WidgetLayout, 'find').returns([
        {
          layoutName: 'layout1',
          sectionName: 'section1'
        },
        {
          layoutName: 'layout2',
          sectionName: 'section1'
        }
      ]);
      mixin.reopen({
        isHDFSFederatedSummary: true,
        userLayoutName: 'layout1',
        sectionName: 'section1'
      });
      mixin.getWidgetLayoutSuccessCallback({items: [{WidgetLayoutInfo: {id: 1}}]});
    });
    afterEach(function() {
      App.widgetMapper.map.restore();
      App.widgetLayoutMapper.map.restore();
      App.WidgetLayout.find.restore();
    });
    
    it('App.widgetMapper.map should be called', function() {
      expect(App.widgetMapper.map.calledWith({id: 1})).to.be.true;
    });
  
    it('App.widgetLayoutMapper.map should be called', function() {
      expect(App.widgetLayoutMapper.map.calledWith({items: [{WidgetLayoutInfo: {id: 1}}]})).to.be.true;
    });
    
    it('activeWidgetLayout should be set', function() {
      expect(mixin.get('activeWidgetLayout')).to.be.eql({
        layoutName: 'layout1',
        sectionName: 'section1'
      });
    });
  
    it('activeNSWidgetLayouts should be set', function() {
      expect(mixin.get('activeNSWidgetLayouts')).to.be.eql([{
        layoutName: 'layout2',
        sectionName: 'section1'
      }]);
    });
  
    it('selectedNSWidgetLayout should be set', function() {
      expect(mixin.get('selectedNSWidgetLayout')).to.be.eql({
        layoutName: 'layout2',
        sectionName: 'section1'
      });
    });
  
    it('isWidgetsLoaded should be true', function() {
      expect(mixin.get('isWidgetsLoaded')).to.be.true;
    });
  });
  
  describe('#getDefaultWidgetLayoutByName', function() {
    
    it('App.ajax.send should be called', function() {
      mixin.getDefaultWidgetLayoutByName('layout1');
      expect(testHelpers.findAjaxRequest('name', 'widget.layout.get')[0].data).to.be.eql({
        urlParams: 'WidgetLayoutInfo/layout_name=layout1'
      });
    });
  });
  
  describe('#createUserWidgetLayout', function() {
  
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      mixin.createUserWidgetLayout({
        display_name: 'l1',
        layout_name: 'L1',
        section_name: 's1',
        widgets: [{
          WidgetInfo: {
            id: 1
          }
        }]
      });
      expect(testHelpers.findAjaxRequest('name', 'widget.layout.create')[0].data).to.be.eql({
        data: {
          "WidgetLayoutInfo": {
            "display_name": 'l1',
            "layout_name": 'L1',
            "scope": "USER",
            "section_name": 's1',
            "user_name": 'admin',
            "widgets": [{
              id: 1
            }]
          }
        }
      });
    });
  });
  
  describe('#updateUserWidgetLayout', function() {
    
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function() {
      App.router.get.restore();
    });
    
    it('App.ajax.send should be called', function() {
      mixin.updateUserWidgetLayout({
        display_name: 'l1',
        layout_name: 'L1',
        section_name: 's1',
        id: 1,
        widgets: [{
          WidgetInfo: {
            id: 1
          }
        }]
      });
      expect(testHelpers.findAjaxRequest('name', 'widget.layout.edit')[0].data).to.be.eql({
        data: {
          "WidgetLayoutInfo": {
            "display_name": 'l1',
            "layout_name": 'L1',
            "id": 1,
            "scope": "USER",
            "section_name": 's1',
            "user_name": 'admin',
            "widgets": [{
              id: 1
            }]
          }
        },
        layoutId: 1
      });
    });
  });
  
  describe('#saveActiveWidgetLayouts', function() {
  
    beforeEach(function() {
      sinon.stub(App.router, 'get').returns('admin');
    });
    afterEach(function() {
      App.router.get.restore();
    });
  
  
    it('App.ajax.send should be called', function() {
      mixin.saveActiveWidgetLayouts([]);
      expect(testHelpers.findAjaxRequest('name', 'widget.activelayouts.edit')[0].data).to.be.eql({
        data: [],
        userName: 'admin'
      });
    });
  });
  
  describe('#removeWidgetLayout', function() {
   
    it('App.ajax.send should be called', function() {
      mixin.removeWidgetLayout(1);
      expect(testHelpers.findAjaxRequest('name', 'widget.layout.delete')[0].data).to.be.eql({
        layoutId: 1
      });
    });
  });
  
  describe('#saveWidgetLayout', function() {
    
    it('App.ajax.send should be called', function() {
      mixin.saveWidgetLayout(
        [Em.Object.create({id: 1})],
        Em.Object.create({
          displayName: 'l1',
          layoutName: 'L1',
          sectionName: 's1',
          scope: 'USER',
          id: 1
        })
      );
      expect(testHelpers.findAjaxRequest('name', 'widget.layout.edit')[0].data).to.be.eql({
        data: {
          "WidgetLayoutInfo": {
            "display_name": 'l1',
            "layout_name": 'L1',
            "id": 1,
            "scope": "USER",
            "section_name": 's1',
            "widgets": [{
              id: 1
            }]
          }
        },
        layoutId: 1
      });
    });
  });
  
  describe('#clearActiveWidgetLayout', function() {
  
    it('activeWidgetLayout should be empty', function() {
      mixin.clearActiveWidgetLayout();
      expect(mixin.get('activeWidgetLayout')).to.be.empty;
    });
  
    it('activeNSWidgetLayouts should be empty', function() {
      mixin.clearActiveWidgetLayout();
      expect(mixin.get('activeNSWidgetLayouts')).to.be.empty;
    });
  });
  
  describe('#createFederationWidgetLayouts', function() {
    var newLayout = {
      widgets: [
        {
          WidgetInfo: {
            id: 1
          }
        },
        {
          WidgetInfo: {
            id: 2
          }
        }
      ]
    };
    beforeEach(function() {
      sinon.stub(mixin, 'getDefaultWidgetLayoutByName').returns({
        done: function(callback) {
          callback({
            items: [{
              WidgetLayoutInfo: newLayout
            }]
          });
        }
      });
      sinon.stub(mixin, 'getNameNodeWidgets').returns({
        done: function(callback) {
          callback({
            items: [
              {
                WidgetInfo: {
                  tag: 'tag1',
                  id: 2
                }
              }
            ]
          });
        }
      });
      sinon.stub(mixin, 'createSingleLayout');
      sinon.stub(mixin, 'createMultipleLayouts');
    });
    afterEach(function() {
      mixin.getDefaultWidgetLayoutByName.restore();
      mixin.getNameNodeWidgets.restore();
      mixin.createSingleLayout.restore();
      mixin.createMultipleLayouts.restore();
    });
    
    it('createSingleLayout should be called', function() {
      mixin.createFederationWidgetLayouts({items: [{}]});
      expect(mixin.createSingleLayout.calledWith(
        [{}],
        newLayout,
        [{
          WidgetInfo: {
            id: 1
          }
        }],
        {
          all: [{
            WidgetInfo: {
              tag: 'tag1',
              id: 2
            }
          }],
          tag1: [
            {
              WidgetInfo: {
                tag: 'tag1',
                id: 2
              }
            }
          ]
        }
      )).to.be.true;
    });
  
    it('createMultipleLayouts should be called', function() {
      mixin.createFederationWidgetLayouts({items: [{}, {}]});
      expect(mixin.createMultipleLayouts.calledWith(
        [{}, {}],
        newLayout,
        {
          all: [{
            WidgetInfo: {
              tag: 'tag1',
              id: 2
            }
          }],
          tag1: [
            {
              WidgetInfo: {
                tag: 'tag1',
                id: 2
              }
            }
          ]
        }
      )).to.be.true;
    });
  });
  
  describe('#createSingleLayout', function() {
    beforeEach(function() {
      sinon.stub(mixin, 'removeWidgetLayout').returns({done: Em.clb});
      sinon.stub(mixin, 'createUserWidgetLayout').returns({done: function(callback) {
          return callback({
            resources: [
              {
                WidgetLayoutInfo: {
                  id: 1
                }
              }
            ]
          });
        }
      });
      sinon.stub(mixin, 'saveActiveWidgetLayouts').returns({done: Em.clb});
      sinon.stub(mixin, 'getActiveWidgetLayout');
      mixin.createSingleLayout(
        [{
          WidgetLayoutInfo: {
            id: 1
          }
        }],
        {},
        [],
        {space1: []}
      );
    });
    afterEach(function() {
      mixin.removeWidgetLayout.restore();
      mixin.createUserWidgetLayout.restore();
      mixin.saveActiveWidgetLayouts.restore();
      mixin.getActiveWidgetLayout.restore();
    });
    
    it('removeWidgetLayout should be called', function() {
      expect(mixin.removeWidgetLayout.calledWith(1)).to.be.true;
    });
  
    it('createUserWidgetLayout should be called', function() {
      expect(mixin.createUserWidgetLayout.calledTwice).to.be.true;
    });
  
    it('saveActiveWidgetLayouts should be called', function() {
      expect(mixin.saveActiveWidgetLayouts.calledWith({
        "WidgetLayouts": [{id: 1}, {id: 1}]
      })).to.be.true;
    });
  
    it('getActiveWidgetLayout should be called', function() {
      expect(mixin.getActiveWidgetLayout.calledOnce).to.be.true;
    });
  });
  
  describe('#createMultipleLayouts', function() {
    beforeEach(function() {
      sinon.stub(App.HDFSService, 'find').returns(Em.Object.create({
        masterComponentGroups: [{name: 'C1'}, {name: 'C2'}]
      }));
      sinon.stub(mixin, 'createUserWidgetLayout').returns({
        done: function(callback) {
          callback({
            resources: [{
              WidgetLayoutInfo: {
                id: 1
              }
            }]
          });
        }
      });
      sinon.stub(mixin, 'saveActiveWidgetLayouts').returns({
        done: Em.clb
      });
      sinon.stub(mixin, 'updateUserWidgetLayout').returns({
        done: Em.clb
      });
      sinon.stub(mixin, 'getActiveWidgetLayout');
      mixin.reopen({
        userLayoutName: 'layout1'
      });
      mixin.createMultipleLayouts(
        [
          {
            WidgetLayoutInfo: {
              layout_name: 'prefix_nameservice_C2',
              id: 2
            }
          },
          {
            WidgetLayoutInfo: {
              layout_name: 'prefix_nameservice_all',
              display_name: 'All',
              widgets: [],
              id: 3
            }
          }
        ],
        {},
        {C2: []}
      );
    });
    afterEach(function() {
      App.HDFSService.find.restore();
      mixin.createUserWidgetLayout.restore();
      mixin.saveActiveWidgetLayouts.restore();
      mixin.updateUserWidgetLayout.restore();
      mixin.getActiveWidgetLayout.restore();
    });
    
    it('createUserWidgetLayout should be called', function() {
      expect(mixin.createUserWidgetLayout.calledOnce).to.be.true;
    });
  
    it('saveActiveWidgetLayouts should be called', function() {
      expect(mixin.saveActiveWidgetLayouts.calledWith({
        "WidgetLayouts": [
          {id: 1},
          {id: 2},
          {id: 3}
        ]
      })).to.be.true;
    });
  
    it('updateUserWidgetLayout should be called', function() {
      expect(mixin.updateUserWidgetLayout.calledOnce).to.be.true;
    });
  
    it('getActiveWidgetLayout should be called', function() {
      expect(mixin.getActiveWidgetLayout.calledOnce).to.be.true;
    });
  });
  
});
