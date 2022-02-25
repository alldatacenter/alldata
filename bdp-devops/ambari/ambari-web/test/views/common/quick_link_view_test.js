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
require('views/common/quick_view_link_view');
var testHelpers = require('test/helpers');

describe('App.QuickViewLinks', function () {

  var quickViewLinks = App.QuickLinksView.create({
    content: Em.Object.create()
  });

  describe("#ambariProperties", function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({p: 1});
    });
    afterEach(function () {
      App.router.get.restore();
    });
    it("ambariProperties are updated", function () {
      expect(quickViewLinks.get('ambariProperties')).to.eql({p: 1});
    });
  });

  describe("#didInsertElement()", function () {
    beforeEach(function () {
      sinon.stub(App.router, 'get').returns({p: 1});
      sinon.stub(quickViewLinks, 'loadQuickLinksConfigurations');
    });
    afterEach(function () {
      App.router.get.restore();
      quickViewLinks.loadQuickLinksConfigurations.restore();
    });
    it("loadQuickLinksConfigurations is called once", function () {
      quickViewLinks.didInsertElement();
      expect(quickViewLinks.loadQuickLinksConfigurations.calledOnce).to.be.true;
    });
  });

  describe("#willDestroyElement()", function () {

    beforeEach(function () {
      quickViewLinks.setProperties({
        configProperties: [{}],
        quickLinks: [{}]
      });
      quickViewLinks.willDestroyElement();
    });

    it("configProperties empty", function () {
      expect(quickViewLinks.get('configProperties')).to.be.empty;
    });

    it("quickLinks empty", function () {
      expect(quickViewLinks.get('quickLinks')).to.be.empty;
    });
  });

  describe("#setQuickLinks()", function () {
    beforeEach(function () {
      var mock = {
        done: Em.clb
      };
      sinon.stub(quickViewLinks, 'setConfigProperties').returns(mock);
      sinon.stub(quickViewLinks, 'getQuickLinksHosts').returns({fail: Em.clb});
      sinon.stub(App, 'get').returns(true);
    });
    afterEach(function () {
      quickViewLinks.setConfigProperties.restore();
      quickViewLinks.getQuickLinksHosts.restore();
      App.get.restore();
    });
    it("getQuickLinksHosts should be called", function () {
      quickViewLinks.setQuickLinks();
      expect(quickViewLinks.getQuickLinksHosts.calledOnce).to.be.true;
      expect(quickViewLinks.get('showNoLinks')).to.be.true;
    });
  
    it("showNoLinks should be true when failed", function () {
      quickViewLinks.setQuickLinks();
      expect(quickViewLinks.get('showNoLinks')).to.be.true;
    });
  });


  describe("#loadQuickLinksConfigSuccessCallback()", function () {
    var mock;

    beforeEach(function () {
      sinon.stub(App.store, 'commit', Em.K);
      mock = sinon.stub(quickViewLinks, 'getQuickLinksConfiguration');
    });
    afterEach(function () {
      App.store.commit.restore();
      mock.restore();
    });
    it("requiredSites consistent", function () {
      var quickLinksConfigHBASE = {
        protocol: {
          type: "http"
        },
        links: [
          {
            port: {
              site: "hbase-site"
            }
          }
        ]
      };
      var quickLinksConfigYARN = {
        protocol: {
          checks: [
            {
              site: "yarn-site"
            }
          ],
          type: "https"
        },
        links: [
          {
            port: {
              site: "yarn-site"
            },
            host: {
              site: "yarn-env"
            }
          }
        ]
      };
      quickViewLinks.set('content.serviceName', 'HBASE');
      mock.returns(quickLinksConfigHBASE);
      quickViewLinks.loadQuickLinksConfigSuccessCallback({items: []});
      quickViewLinks.set('content.serviceName', 'YARN');
      mock.returns(quickLinksConfigYARN);
      quickViewLinks.loadQuickLinksConfigSuccessCallback({items: []});
      expect(quickViewLinks.get('requiredSiteNames')).to.be.eql(["core-site", "hdfs-site", "admin-properties", "hbase-site", "yarn-site", "yarn-env"]);
    });
  });

  describe("#getQuickLinksHosts()", function () {
    beforeEach(function () {
      sinon.stub(App.HostComponent, 'find').returns([
        Em.Object.create({
          componentName: 'HBASE_MASTER',
          hostName: 'host1'
        })
      ]);
      sinon.stub(App.QuickLinksConfig, 'find').returns([
        {
          links: [
            {
              component_name: 'HBASE_MASTER'
            }
          ]
        }
      ]);
    });
    afterEach(function () {
      App.HostComponent.find.restore();
      App.QuickLinksConfig.find.restore();
    });
    it("call $.ajax", function () {
      quickViewLinks.getQuickLinksHosts();
      var args = testHelpers.findAjaxRequest('name', 'hosts.for_quick_links');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(quickViewLinks);
      expect(args[0].data).to.be.eql({
        clusterName: App.get('clusterName'),
        hosts: 'host1',
        urlParams: ''
      });
    });
    it("call $.ajax, HBASE service", function () {
      quickViewLinks.set('content.serviceName', 'HBASE');
      quickViewLinks.getQuickLinksHosts();
      var args = testHelpers.findAjaxRequest('name', 'hosts.for_quick_links');
      expect(args[0]).exists;
      expect(args[0].sender).to.be.eql(quickViewLinks);
      expect(args[0].data).to.be.eql({
        clusterName: App.get('clusterName'),
        hosts: 'host1',
        urlParams: ',host_components/metrics/hbase/master/IsActiveMaster'
      });
    });
  });

  describe("#setQuickLinksSuccessCallback()", function () {
    var getQuickLinks;
    beforeEach(function () {
      this.mock = sinon.stub(quickViewLinks, 'getHosts');
      getQuickLinks = sinon.stub(quickViewLinks, 'getQuickLinksConfiguration');
      getQuickLinks.returns({});
      sinon.stub(quickViewLinks, 'setEmptyLinks');
      sinon.stub(quickViewLinks, 'setSingleHostLinks');
      sinon.stub(quickViewLinks, 'setMultipleHostLinks');
      sinon.stub(quickViewLinks, 'setMultipleGroupLinks');
      quickViewLinks.set('content.quickLinks', []);
    });
    afterEach(function () {
      this.mock.restore();
      getQuickLinks.restore();
      quickViewLinks.setEmptyLinks.restore();
      quickViewLinks.setSingleHostLinks.restore();
      quickViewLinks.setMultipleHostLinks.restore();
      quickViewLinks.setMultipleGroupLinks.restore();
      quickViewLinks.get('masterGroups').clear();
    });
    it("no hosts", function () {
      this.mock.returns([]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setEmptyLinks.calledOnce).to.be.true;
    });
    it("has overridden hosts", function () {
      this.mock.returns([]);
      getQuickLinks.returns({ links: [{ host: {site: "yarn-env"} }] });
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setEmptyLinks.calledOnce).to.be.false;
    });
    it("quickLinks is not configured", function () {
      this.mock.returns([{}]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setEmptyLinks.calledOnce).to.be.false;
    });
    it("single host", function () {
      this.mock.returns([{hostName: 'host1'}]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setSingleHostLinks.calledWith([{hostName: 'host1'}])).to.be.true;
    });
    it("multiple hosts", function () {
      this.mock.returns([{hostName: 'host1'}, {hostName: 'host2'}]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setMultipleHostLinks.calledWith(
        [{hostName: 'host1'}, {hostName: 'host2'}]
      )).to.be.true;
    });
    it("multiple grouped hosts", function () {
      this.mock.returns([{hostName: 'host1'}, {hostName: 'host2'}]);
      quickViewLinks.set('masterGroups', [{}, {}]);
      quickViewLinks.setQuickLinksSuccessCallback();
      expect(quickViewLinks.setMultipleGroupLinks.calledWith(
        [{hostName: 'host1'}, {hostName: 'host2'}]
      )).to.be.true;
    });
  });

  describe("#getPublicHostName()", function () {
    it("host present", function () {
      var hosts = [{
        Hosts: {
          host_name: 'host1',
          public_host_name: 'public_name'
        }
      }];
      expect(quickViewLinks.getPublicHostName(hosts, 'host1')).to.equal('public_name');
    });
    it("host absent", function () {
      expect(quickViewLinks.getPublicHostName([], 'host1')).to.be.null;
    });
  });

  describe("#setConfigProperties()", function () {
    beforeEach(function () {
      sinon.stub(App.router.get('configurationController'), 'getCurrentConfigsBySites');
    });
    afterEach(function () {
      App.router.get('configurationController').getCurrentConfigsBySites.restore();
    });
    it("getCurrentConfigsBySites called with correct data", function () {
      quickViewLinks.set('requiredSiteNames', ['hdfs-site']);
      quickViewLinks.setConfigProperties();
      expect(App.router.get('configurationController').getCurrentConfigsBySites.calledWith(['hdfs-site'])).to.be.true;
    });
  });

  describe("#setEmptyLinks()", function () {
    it("empty links are set", function () {
      quickViewLinks.setEmptyLinks();
      expect(quickViewLinks.get('quickLinks')).to.eql([{
        label: quickViewLinks.get('quickLinksErrorMessage'),
      }]);
      expect(quickViewLinks.get('isLoaded')).to.be.true;
    });
  });

  describe("#processOozieHosts()", function () {
    it("host status is valid", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'OOZIE_SERVER',
        workStatus: 'STARTED',
        hostName: 'host1'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processOozieHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("host status is invalid", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'OOZIE_SERVER',
        workStatus: 'INSTALLED',
        hostName: 'host1'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processOozieHosts([host]);
      expect(quickViewLinks.get('quickLinksErrorMessage')).to.equal(Em.I18n.t('quick.links.error.oozie.label'));
    });
  });

  describe("#processHdfsHosts()", function () {
    beforeEach(function () {
      quickViewLinks.set('content.activeNameNodes', []);
      quickViewLinks.set('content.standbyNameNodes', []);
    });
    it("active namenode host", function () {
      quickViewLinks.get('content.activeNameNodes').pushObject(Em.Object.create({hostName: 'host1'}));
      var host = {hostName: 'host1'};
      quickViewLinks.processHdfsHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("standby namenode host", function () {
      quickViewLinks.get('content.standbyNameNodes').pushObject(Em.Object.create({hostName: 'host1'}));
      var host = {hostName: 'host1'};
      quickViewLinks.processHdfsHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
  });

  describe("#processHbaseHosts()", function () {
    it("isActiveMaster is true", function () {
      var response = {
        items: [
          {
            Hosts: {
              host_name: 'host1'
            },
            host_components: [
              {
                HostRoles: {
                  component_name: 'HBASE_MASTER'
                },
                metrics: {
                  hbase: {
                    master: {
                      IsActiveMaster: 'true'
                    }
                  }
                }
              }
            ]
          }
        ]
      };
      var host = {hostName: 'host1'};
      quickViewLinks.processHbaseHosts([host], response);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("isActiveMaster is false", function () {
      var response = {
        items: [
          {
            Hosts: {
              host_name: 'host1'
            },
            host_components: [
              {
                HostRoles: {
                  component_name: 'HBASE_MASTER'
                },
                metrics: {
                  hbase: {
                    master: {
                      IsActiveMaster: 'false'
                    }
                  }
                }
              }
            ]
          }
        ]
      };
      var host = {hostName: 'host1'};
      quickViewLinks.processHbaseHosts([host], response);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
    it("isActiveMaster is undefined", function () {
      var response = {
        items: [
          {
            Hosts: {
              host_name: 'host1'
            },
            host_components: [
              {
                HostRoles: {
                  component_name: 'HBASE_MASTER'
                }
              }
            ]
          }
        ]
      };
      var host = {hostName: 'host1'};
      quickViewLinks.processHbaseHosts([host], response);
      expect(host.status).to.be.undefined;
    });
  });

  describe("#processYarnHosts()", function () {
    it("haStatus is ACTIVE", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'RESOURCEMANAGER',
        hostName: 'host1',
        haStatus: 'ACTIVE'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processYarnHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.active'));
    });
    it("haStatus is STANDBY", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'RESOURCEMANAGER',
        hostName: 'host1',
        haStatus: 'STANDBY'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processYarnHosts([host]);
      expect(host.status).to.equal(Em.I18n.t('quick.links.label.standby'));
    });
    it("haStatus is undefined", function () {
      quickViewLinks.set('content.hostComponents', [Em.Object.create({
        componentName: 'RESOURCEMANAGER',
        hostName: 'host1'
      })]);
      var host = {hostName: 'host1'};
      quickViewLinks.processYarnHosts([host]);
      expect(host.status).to.be.undefined;
    });
  });

  describe("#findHosts()", function () {
    beforeEach(function () {
      sinon.stub(quickViewLinks, 'getPublicHostName').returns('public_name');
      sinon.stub(App.MasterComponent, 'find').returns([
        Em.Object.create({
          componentName: "C1",
          hostNames: ["host1", "host2"]
        })
      ]);
    });
    afterEach(function () {
      quickViewLinks.getPublicHostName.restore();
      App.MasterComponent.find.restore();
    });
    it("public_name from getPublicHostName", function () {
      expect(quickViewLinks.findHosts('C1', {})).to.eql([
        {
          hostName: 'host1',
          publicHostName: 'public_name',
          componentName: 'C1'
        },
        {
          hostName: 'host2',
          publicHostName: 'public_name',
          componentName: 'C1'
        }
      ]);
    });
  });

  describe('#setProtocol', function () {
    var tests = [
      //Yarn
      {
        serviceName: "YARN",
        configProperties: [
          {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTPS_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:[
              {property:"yarn.http.policy",
                desired:"HTTPS_ONLY",
                site:"yarn-site"}
            ]
          }
        },
        m: "https for yarn (checks for https passed)",
        result: "https"
      },
      {
        serviceName: "YARN",
        configProperties: [
          {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTP_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"http",
            checks:[
              {property:"yarn.http.policy",
                desired:"HTTP_ONLY",
                site:"yarn-site"}
            ]
          }
        },
        m: "http for yarn (checks for http passed)",
        result: "http"
      },
      {
        serviceName: "YARN",
        configProperties: [
          {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTP_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:[
              {property:"yarn.http.policy",
                desired:"HTTPS_ONLY",
                site:"yarn-site"}
            ]
          }
        },
        m: "http for yarn (checks for https did not pass)",
        result: "http"
      },
      {
        serviceName: "YARN",
        configProperties: [
          {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTPS_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"http",
            checks:[
              {property:"yarn.http.policy",
                desired:"HTTP_ONLY",
                site:"yarn-site"}
            ]
          }
        },
        m: "https for yarn (checks for http did not pass)",
        result: "https"
      },
      {
        serviceName: "YARN",
        configProperties: [
          {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTP_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"HTTP_ONLY",
            checks:[
              {property:"yarn.http.policy",
                desired:"HTTPS_ONLY",
                site:"yarn-site"}
            ]
          }
        },
        m: "http for yarn (override checks with specific protocol type)",
        result: "http"
      },
      {
        serviceName: "YARN",
        configProperties: [
          {type: 'yarn-site', properties: {'yarn.http.policy': 'HTTPS_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"HTTPS_ONLY",
            checks:[
              {property:"yarn.http.policy",
                desired:"HTTPS_ONLY",
                site:"yarn-site"}
            ]
          }
        },
        m: "https for yarn (override checks with specific protocol type)",
        result: "https"
      },
      //Any service - override hadoop.ssl.enabled
      {
        serviceName: "MyService",
        configProperties: [
          {type: 'myservice-site', properties: {'myservice.http.policy': 'HTTPS_ONLY'}},
          {type: 'hdfs-site', properties: {'dfs.http.policy':'HTTP_ONLY'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:[
              {property:"myservice.http.policy",
                desired:"HTTPS_ONLY",
                site:"myservice-site"}
            ]
          }
        },
        m: "https for MyService (checks for https passed, override hadoop.ssl.enabled)",
        result: "https"
      },
      //Oozie
      {
        serviceName: "OOZIE",
        configProperties: [
          {type: 'oozie-site', properties: {'oozie.https.port': '12345', 'oozie.https.keystore.file':'/tmp/oozie.jks', 'oozie.https.keystore.pass':'mypass'}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"HTTPS",
            checks:
              [
                {
                  "property":"oozie.https.port",
                  "desired":"EXIST",
                  "site":"oozie-site"
                },
                {
                  "property":"oozie.https.keystore.file",
                  "desired":"EXIST",
                  "site":"oozie-site"
                },
                {
                  "property":"oozie.https.keystore.pass",
                  "desired":"EXIST",
                  "site":"oozie-site"
                }
              ]
          }
        },
        m: "https for oozie (checks for https passed)",
        result: "https"
      },
      {
        serviceName: "OOZIE",
        configProperties: [
          {type: 'oozie-site', properties: {"oozie.base.url":"http://c6401.ambari.apache.org:11000/oozie"}}
        ],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:
              [
                {
                  "property":"oozie.https.port",
                  "desired":"EXIST",
                  "site":"oozie-site"
                },
                {
                  "property":"oozie.https.keystore.file",
                  "desired":"EXIST",
                  "site":"oozie-site"
                },
                {
                  "property":"oozie.https.keystore.pass",
                  "desired":"EXIST",
                  "site":"oozie-site"
                }
              ]
          }
        },
        m: "http for oozie (checks for https did not pass)",
        result: "http"
      },
      //Ranger: HDP 2.2
      {
        serviceName: "RANGER",
        configProperties: [{type: 'ranger-site', properties: {'http.enabled': 'false'}}],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:
              [
                {
                  "property":"http.enabled",
                  "desired":"false",
                  "site":"ranger-site"
                }
              ]
          }
        },
        m: "https for ranger (HDP2.2, checks passed)",
        result: "https"
      },
      {
        serviceName: "RANGER",
        configProperties: [{type: 'ranger-site', properties: {'http.enabled': 'true'}}],
        quickLinksConfig: {
          protocol:{
            type:"HTTPS",
            checks:
              [
                {
                  "property":"http.enabled",
                  "desired":"false",
                  "site":"ranger-site"
                }
              ]
          }
        },
        m: "http for ranger (HDP2.2, checks for https did not pass)",
        result: "http"
      },
      //Ranger: HDP 2.3
      {
        serviceName: "RANGER",
        configProperties:
          [
            {
              type: 'ranger-admin-site',
              properties: {'ranger.service.http.enabled': 'false', 'ranger.service.https.attrib.ssl.enabled': 'true'}
            }
          ],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:
              [
                {
                  "property":"ranger.service.http.enabled",
                  "desired":"false",
                  "site":"ranger-admin-site"
                },
                {
                  "property":"ranger.service.https.attrib.ssl.enabled",
                  "desired":"true",
                  "site":"ranger-admin-site"
                }
              ]
          }
        },

        m: "https for ranger (HDP2.3, checks passed)",
        result: "https"
      },
      {
        serviceName: "RANGER",
        configProperties:
          [
            {
              type: 'ranger-admin-site',
              properties: {'ranger.service.http.enabled': 'true', 'ranger.service.https.attrib.ssl.enabled': 'false'}
            }
          ],
        quickLinksConfig: {
          protocol:{
            type:"https",
            checks:
              [
                {
                  "property":"ranger.service.http.enabled",
                  "desired":"false",
                  "site":"ranger-admin-site"
                },
                {
                  "property":"ranger.service.https.attrib.ssl.enabled",
                  "desired":"true",
                  "site":"ranger-admin-site"
                }
              ]
          }
        },
        m: "http for ranger (HDP2.3, checks for https did not pass)",
        result: "http"
      }
    ];

    tests.forEach(function (t) {
      it(t.m, function () {
        quickViewLinks.set('servicesSupportsHttps', t.servicesSupportsHttps);
        expect(quickViewLinks.setProtocol(t.configProperties, t.quickLinksConfig.protocol)).to.equal(t.result);
      });
    });
  });

  describe('#resolvePlaceholders', function() {
    beforeEach(function() {
      quickViewLinks.setProperties({
      configProperties: [{
          'type': 'config-type1',
          'properties': {'property1': 'value1'}
        }],
        actualTags: [""],
        quickLinks: [{}]
      });
    }),
    it("replaces placeholders from config", function () {
      expect(quickViewLinks.resolvePlaceholders('${config-type1/property1}')).to.equal('value1');
    }),
    it("leaves url as it is if config-type was not found", function () {
      expect(quickViewLinks.resolvePlaceholders('${unknown-config-type/property1}')).to.equal('${unknown-config-type/property1}');
    }),
    it("leaves url as it is if property was not found", function () {
      expect(quickViewLinks.resolvePlaceholders('${config-type1/unknown-property}')).to.equal('${config-type1/unknown-property}');
    })
  }),

  describe('#setPort', function () {
    var testData = [
      Em.Object.create({
        'protocol': 'http',
        'port':{
          'http_property':'yarn.timeline-service.webapp.address',
          'http_default_port':'8188',
          'https_property':'yarn.timeline-service.webapp.https.address',
          'https_default_port':'8090',
          'regex': '\\w*:(\\d+)',
          'site':'yarn-site'
        },
        'configProperties':
          [
            {
              'type': 'yarn-site',
              'properties': {'yarn.timeline-service.webapp.address': 'c6401.ambari.apache.org:8188'}
            }
          ],
        'result': '8188'
      }),

      Em.Object.create({
        'protocol': 'https',
        'port':{
          'http_property':'yarn.timeline-service.webapp.address',
          'http_default_port':'8188',
          'https_property':'yarn.timeline-service.webapp.https.address',
          'https_default_port':'8090',
          'regex': '\\w*:(\\d+)',
          'site':'yarn-site'
        },
        'configProperties':
          [
            {
              'type': 'yarn-site',
              'properties': {'yarn.timeline-service.webapp.https.address': 'c6401.ambari.apache.org:8090'}
            }
          ],
        'result': '8090'
      }),

      Em.Object.create({
        'protocol': 'https',
        'port':{
          'http_property':'oozie.base.url',
          'http_default_port':'11000',
          'https_property':'oozie.https.port',
          'https_default_port':'11443',
          'regex': '\\w*:(\\d+)',
          'https_regex': '(\\d+)',
          'site':'oozie-site'
        },
        'configProperties':
          [
            {
              'type': 'oozie-site',
              'properties':
                {
                  'oozie.base.url': 'c6401.ambari.apache.org:11000/oozie',
                  'oozie.https.port' : '11444'
                }
            }
          ],
        'result': '11444'
      }),

      Em.Object.create({
        'protocol': 'http',
        'port':{
          'http_property':'oozie.base.url',
          'http_default_port':'11000',
          'https_property':'oozie.https.port',
          'https_default_port':'11443',
          'regex': '\\w*:(\\d+)',
          'https_regex': '(\\d+)',
          'site':'oozie-site'
        },
        'configProperties':
          [
            {
              'type': 'oozie-site',
              'properties':
                {
                  'oozie.base.url': 'c6401.ambari.apache.org:11002/oozie',
                  'oozie.https.port' : '11444'
                }
            }
          ],
        'result': '11002'
      })
    ];

    after(function () {
      quickViewLinks.set('configProperties', []);
    });

    testData.forEach(function (item) {
      it(item.service_id + ' ' + item.protocol, function () {
        quickViewLinks.set('configProperties', item.configProperties || []);
        expect(quickViewLinks.setPort(item.port, item.protocol, item.configProperties)).to.equal(item.result);
      })
    }, this);
  });

  describe("#getHosts()", function() {

    beforeEach(function() {
      sinon.stub(quickViewLinks, 'processOozieHosts').returns(['oozieHost']);
      sinon.stub(quickViewLinks, 'processHdfsHosts').returns(['hdfsHost']);
      sinon.stub(quickViewLinks, 'processHbaseHosts').returns(['hbaseHost']);
      sinon.stub(quickViewLinks, 'processYarnHosts').returns(['yarnHost']);
      sinon.stub(quickViewLinks, 'findHosts').returns(['host1']);
      sinon.stub(App.QuickLinksConfig, 'find').returns([
        Em.Object.create({
          id: 'OOZIE',
          links: [
            {
              component_name: 'OOZIE_SERVER'
            }
          ]
        }),
        Em.Object.create({
          id: 'HDFS',
          links: [
            {
              component_name: 'NAMENODE'
            }
          ]
        }),
        Em.Object.create({
          id: 'HBASE',
          links: [
            {
              component_name: 'HBASE_MASTER'
            }
          ]
        }),
        Em.Object.create({
          id: 'YARN',
          links: [
            {
              component_name: 'RESOURCEMANAGER'
            }
          ]
        }),
        Em.Object.create({
          id: 'STORM',
          links: [
            {
              component_name: 'STORM_UI_SERVER'
            }
          ]
        }),
        Em.Object.create({
          id: 'ACCUMULO',
          links: [
            {
              component_name: 'ACCUMULO_MONITOR'
            }
          ]
        }),
        Em.Object.create({
          id: 'ATLAS',
          links: [
            {
              component_name: 'ATLAS_SERVER'
            }
          ]
        }),
        Em.Object.create({
          id: 'MAPREDUCE2',
          links: [
            {
              component_name: 'HISTORYSERVER'
            }
          ]
        }),
        Em.Object.create({
          id: 'AMBARI_METRICS',
          links: [
            {
              component_name: 'METRICS_GRAFANA'
            }
          ]
        }),
        Em.Object.create({
          id: 'LOGSEARCH',
          links: [
            {
              component_name: 'LOGSEARCH_SERVER'
            }
          ]
        }),
        Em.Object.create({
          id: 'HIVE',
          links: [
            {
              component_name: 'METRICS_GRAFANA'
            },
            {
              component_name: 'HIVE_SERVER_INTERACTIVE'
            }
          ]
        })
      ]);
    });
    afterEach(function() {
      quickViewLinks.processOozieHosts.restore();
      quickViewLinks.processHdfsHosts.restore();
      quickViewLinks.processHbaseHosts.restore();
      quickViewLinks.findHosts.restore();
      quickViewLinks.processYarnHosts.restore();
      App.QuickLinksConfig.find.restore();
    });

    var tests = [
      {
        serviceName: 'OOZIE',
        callback: 'processOozieHosts',
        result: ['oozieHost']
      },
      {
        serviceName: 'HDFS',
        callback: 'processHdfsHosts',
        result: ['hdfsHost']
      },
      {
        serviceName: 'HBASE',
        callback: 'processHbaseHosts',
        result: ['hbaseHost']
      },
      {
        serviceName: 'YARN',
        callback: 'processYarnHosts',
        result: ['yarnHost']
      },
      {
        serviceName: 'STORM'
      },
      {
        serviceName: 'ACCUMULO'
      },
      {
        serviceName: 'ATLAS'
      },
      {
        serviceName: 'MAPREDUCE2'
      },
      {
        serviceName: 'AMBARI_METRICS'
      },
      {
        serviceName: 'LOGSEARCH'
      },
      {
        serviceName: 'HIVE',
        result: ['host1', 'host1']
      }
    ];

    tests.forEach(function (_test) {
      var serviceName = _test.serviceName;
      describe(serviceName, function () {
        var componentNames;

        beforeEach(function () {
          componentNames = App.QuickLinksConfig.find().findProperty('id', serviceName).get('links').mapProperty('component_name');
          this.result = quickViewLinks.getHosts({}, serviceName);
        });

        it('hosts', function () {
          expect(this.result).to.be.eql(_test.result || ['host1']);
        });

        it('components', function () {
          expect(quickViewLinks.findHosts.callCount).to.be.equal(componentNames.length);
        });

        if (_test.callback) {
          it('callback is called once', function () {
            expect(quickViewLinks[_test.callback].calledOnce).to.be.true;
          });
        }
      });
    });

    it("custom service without master", function() {
      expect(quickViewLinks.getHosts({}, 'S1')).to.be.empty;
    });
  });

  describe('#reverseType', function () {

    Em.A([
      {
        input: 'https',
        output: 'http'
      },
      {
        input: 'http',
        output: 'https'
      },
      {
        input: 'some',
        output: ''
      }
    ]).forEach(function (test) {
      it(JSON.stringify(test.input) + ' -> ' + JSON.stringify(test.output), function () {
        expect(quickViewLinks.reverseType(test.input)).to.be.equal(test.output)
      });
    });

  });

  describe('#meetDesired', function () {

    var configProperties = [
      {type: 't1', properties: {p1: 1234, p2: null, p3: 'CUSTOM'}}
    ];

    it('no needed config property', function () {
      expect(quickViewLinks.meetDesired([], '', '', '')).to.be.false;
    });

    it('desiredState is `NOT_EXIST` and currentPropertyValue is null', function () {
      expect(quickViewLinks.meetDesired(configProperties, 't1', 'p2', 'NOT_EXIST')).to.be.true;
    });

    it('desiredState is `NOT_EXIST` and currentPropertyValue is not null', function () {
      expect(quickViewLinks.meetDesired(configProperties, 't1', 'p1', 'NOT_EXIST')).to.be.false;
    });

    it('desiredState is `EXIST` and currentPropertyValue is null', function () {
      expect(quickViewLinks.meetDesired(configProperties, 't1', 'p2', 'EXIST')).to.be.false;
    });

    it('desiredState is `EXIST` and currentPropertyValue is not null', function () {
      expect(quickViewLinks.meetDesired(configProperties, 't1', 'p1', 'EXIST')).to.be.true;
    });

    it('desiredState is `CUSTOM` and currentPropertyValue is `CUSTOM`', function () {
      expect(quickViewLinks.meetDesired(configProperties, 't1', 'p3', 'CUSTOM')).to.be.true;
    });

    it('desiredState is `CUSTOM` and currentPropertyValue is not `CUSTOM`', function () {
      expect(quickViewLinks.meetDesired(configProperties, 't1', 'p2', 'CUSTOM')).to.be.false;
    });

  });

});
