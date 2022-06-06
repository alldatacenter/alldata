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
 * @typedef {object} hostForQuickLink
 * @property {string} hostName
 * @property {string} publicHostName
 * @property {string} componentName
 * @property {?string} status
 */

App.QuickLinksView = Em.View.extend({

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {boolean}
   */
  showQuickLinks: false,

  /**
   * @type {boolean}
   */
  showNoLinks: false,

  /**
   * @type {string}
   */
  quickLinksErrorMessage: '',

  /**
   * Updated quick links. Here we put correct hostname to url
   *
   * @type {object[]}
   */
  quickLinks: [],

  /**
   * @type {object[]}
   */
  configProperties: [],

  /**
   * list of files that contains properties for enabling/disabling ssl
   *
   * @type {string[]}
   */
  requiredSiteNames: [],

  /**
   * services that supports security. this array is used to find out protocol.
   * besides YARN, MAPREDUCE2, ACCUMULO. These services use
   * their properties to know protocol
   */
  servicesSupportsHttps: ["HDFS", "HBASE"],

  masterGroups: [],

  tooltipAttribute: 'quick-links-title-tooltip',

  shouldSetGroupedLinks: false,

  /**
   * @type {object}
   */
  ambariProperties: function () {
    return App.router.get('clusterController.ambariProperties');
  }.property().volatile(),

  didInsertElement: function () {
    this.loadQuickLinksConfigurations();
  },

  willDestroyElement: function () {
    this.get('configProperties').clear();
    this.get('quickLinks').clear();
    this.get('requiredSiteNames').clear();
  },

  /**
   * The flags responsible for data to build quick links:
   * - App.router.clusterController.isServiceMetricsLoaded
   *
   * The flags responsible for correct, up-to-date state of quick links:
   * - App.currentStackVersionNumber
   * - App.router.clusterController.isHostComponentMetricsLoaded
   */
  setQuickLinks: function () {
    if (App.get('router.clusterController.isServiceMetricsLoaded')) {
      this.setConfigProperties().done((configProperties) => {
        this.get('configProperties').pushObjects(configProperties);
        this.getQuickLinksHosts().fail(() => {
          this.set('showNoLinks', true);
        });
      });
    }
  }.observes(
    'App.currentStackVersionNumber',
    'App.router.clusterController.isServiceMetricsLoaded',
    'App.router.clusterController.isHostComponentMetricsLoaded',
    'App.router.clusterController.quickLinksUpdateCounter'
  ),

   /**
   * Request for quick-links config
   *
   * @returns {$.ajax}
   * @method loadQuickLinksConfigurations
   */
  loadQuickLinksConfigurations: function () {
    var serviceName = this.get('content.serviceName');
    return App.ajax.send({
      name: 'configs.quicklinksconfig',
      sender: this,
      data: {
        serviceName: serviceName,
        stackVersionUrl: App.get('stackVersionURL')
      },
      success: 'loadQuickLinksConfigSuccessCallback'
    });
  },

  /**
   * Sucess-callback for quick-links config request
   *
   * @param {object} data
   * @method loadQuickLinksConfigSuccessCallback
   */
  loadQuickLinksConfigSuccessCallback: function (data) {
    App.quicklinksMapper.map(data);
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if (!Em.isNone(quickLinksConfig)) {
      var protocolConfig = Em.get(quickLinksConfig, 'protocol');
      var checks = Em.get(protocolConfig, 'checks');
      var sites = ['core-site', 'hdfs-site', 'admin-properties'];
      if (checks) {
        checks.forEach(function (check) {
          var protocolConfigSiteProp = Em.get(check, 'site');
          if (sites.indexOf(protocolConfigSiteProp) < 0) {
            sites.push(protocolConfigSiteProp);
          }
        }, this);
      }

      var links = Em.get(quickLinksConfig, 'links');
      if (!Em.isEmpty(links)) {
        links.forEach(function (link) {
          if (!link.remove) {
            this.addSite(link, 'host', sites);
            this.addSite(link, 'port', sites);
          }
        }, this);
        this.set('requiredSiteNames', this.get('requiredSiteNames').pushObjects(sites).uniq());
        this.setQuickLinks();
      }
    } else {
      this.set('showNoLinks', true);

    }
  },

  addSite: function(link, linkPropertyName, sites) {
      var config = Em.get(link, linkPropertyName);
      if (config) {
        var siteName = Em.get(config, 'site');
        if (!sites.contains(siteName)) {
          sites.push(siteName);
        }
      }
  },

  /**
   * call for public host names
   *
   * @returns {$.ajax}
   * @method getQuickLinksHosts
   */
  getQuickLinksHosts: function () {
    const links = App.QuickLinksConfig.find().mapProperty('links'),
      components = links.reduce((componentsArray, currentLinksArray) => {
        const componentNames = currentLinksArray.mapProperty('component_name').uniq();
        return [...componentsArray, ...componentNames];
      }, []),
      hosts = App.HostComponent.find().filter(component => {
        return components.contains(component.get('componentName'));
      }).mapProperty('hostName').uniq();

    if (hosts.length === 0) {
      return $.Deferred().reject().promise();
    }

    return App.ajax.send({
      name: 'hosts.for_quick_links',
      sender: this,
      data: {
        clusterName: App.get('clusterName'),
        hosts: hosts.join(','),
        urlParams: this.get('content.serviceName') === 'HBASE' ? ',host_components/metrics/hbase/master/IsActiveMaster' : ''
      },
      success: 'setQuickLinksSuccessCallback'
    });
  },

  /**
   * Success-callback for quick-links hosts request
   *
   * @param {object} response
   * @method setQuickLinksSuccessCallback
   */
  setQuickLinksSuccessCallback: function (response) {
    var serviceName = this.get('content.serviceName');
    var hosts = this.getHosts(response, serviceName);
    var hasQuickLinks = this.hasQuickLinksConfig(serviceName, hosts);
    var hasHosts = false;
    var componentNames = hosts.mapProperty('componentName');
    componentNames.forEach(function(_componentName){
      var component = App.MasterComponent.find().findProperty('componentName', _componentName) ||
        App.SlaveComponent.find().findProperty('componentName', _componentName);
      if (component) {
        hasHosts = hasHosts || !!component.get('totalCount');
      }
    });
    // no need to set quicklinks if
    // 1)current service does not have quick links configured
    // 2)No host component present for the configured quicklinks and has no overridden hosts
    if(hasQuickLinks && (hasHosts || this.hasOverriddenHost())) {
      this.set('showQuickLinks', true);
    } else {
      this.set('showNoLinks', true);
    }

    var isMultipleComponentsInLinks = componentNames.uniq().length > 1;

    if (hosts.length === 0 && !this.hasOverriddenHost()) {
      this.setEmptyLinks();
    } else if (hosts.length === 1 || isMultipleComponentsInLinks || this.hasOverriddenHost()) {
      this.setSingleHostLinks(hosts, response);
    } else if (this.get('masterGroups.length') > 1 || this.get('shouldSetGroupedLinks')) {
      this.setMultipleGroupLinks(hosts);
    } else {
      this.setMultipleHostLinks(hosts);
    }
  },

  hasOverriddenHost: function() {
    var links = Em.get(this.getQuickLinksConfiguration(), 'links');
    return links && links.some(function (each) { return each.host; });
  },

  /**
   * Get public host name by its host name.
   *
   * @method getPublicHostName
   * @param {Object[]} hosts - list of hosts from response
   * @param {string} hostName
   * @return {?string}
   **/
  getPublicHostName: function (hosts, hostName) {
    var host = hosts.findProperty('Hosts.host_name', hostName);
    return host ? Em.get(host, 'Hosts.public_host_name') : null;
  },

  /**
   * Get configs from `configurationController` for provided list of the tags
   *
   * @returns {$.Deferred}
   * @method setConfigProperties
   */
  setConfigProperties: function () {
    this.get('configProperties').clear();
    return App.router.get('configurationController').getCurrentConfigsBySites(this.get('requiredSiteNames'));
  },

  /**
   * Get quick links config for <code>content.serviceName</code>
   *
   * @returns {?App.QuickLinksConfig}
   * @method getQuickLinksConfiguration
   */
  getQuickLinksConfiguration: function () {
    var serviceName = this.get('content.serviceName');
    var self = this;
    var quicklinks = {};
    if (self.hasQuickLinksConfig(serviceName)) {
      quicklinks = App.QuickLinksConfig.find().findProperty('id', serviceName);
      Em.set(quicklinks, 'links', Em.get(quicklinks, 'links').filterProperty('visible', true));
      return quicklinks;
    }
    return null;
  },

  /**
   * Check if <code>serviceName</code> has quick-links config
   *
   * @param {string} serviceName
   * @returns {boolean}
   * @method hasQuickLinksConfig
   */
  hasQuickLinksConfig: function (serviceName) {
    var result = App.QuickLinksConfig.find().findProperty('id', serviceName);
    if (!result) {
      return false;
    }
    var links = result.get('links');
    return Em.isEmpty(links) ? false : links.length !== links.filterProperty('remove').length;
  },

  /**
   *
   * @param {object} link
   * @param {string} host
   * @param {string} protocol
   * @param {object[]} configProperties
   * @param {object} response
   * @returns {?object}
   * @method getHostLink
   */
  getHostLink: function (link, host, protocol, configProperties, response) {
    var serviceName = this.get('content.serviceName');
    if (serviceName === 'MAPREDUCE2' && response) {
      var portConfig = Em.get(link, 'port');
      var siteName = Em.get(portConfig, 'site');
      const siteConfigs = this.get('configProperties').findProperty('type', siteName).properties;
      var hostPortConfigValue = siteConfigs[Em.get(portConfig, protocol + '_config')];
      if (!Em.isNone(hostPortConfigValue)) {
        var hostPortValue = hostPortConfigValue.match(new RegExp('([\\w\\d.-]*):(\\d+)'));
        var hostObj = response.items.findProperty('Hosts.host_name', hostPortValue[1]);
        if (!Em.isNone(hostObj)) {
          host = hostObj.Hosts.public_host_name;
        }
      }
    } else if (serviceName === 'RANGER') {
      const siteConfigs = this.get('configProperties').findProperty('type', 'admin-properties').properties;
      if (siteConfigs['policymgr_external_url']) {
        return {
          label: link.label,
          url: siteConfigs['policymgr_external_url']
        }
      }
    }

    var linkPort = this.setPort(Em.get(link, 'port'), protocol, configProperties);
    if (Em.get(link, 'url') && !Em.get(link, 'removed')) {
      var newItem = {};
      var requiresUserName = Em.get(link, 'requires_user_name');
      var template = Em.get(link, 'url');
      if ('true' === requiresUserName) {
        newItem.url = template.fmt(protocol, host, linkPort, App.router.get('loginName'));
      } else {
        newItem.url = template.fmt(protocol, host, linkPort);
      }
      newItem.label = link.label;
      newItem.url = this.resolvePlaceholders(newItem.url);
      return newItem;
    }
    return null;
  },

  /**
   * Replace placeholders like ${config-type/property-name} in the given URL
   */
  resolvePlaceholders: function(url) {
    return url.replace(/\$\{(\S+)\/(\S+)\}/g, function(match, configType, propertyName) {
      var config = this.get('configProperties').findProperty('type', configType);
      if (config) {
        return config.properties[propertyName] ? config.properties[propertyName] : match;
      } else {
        return match;
      }
    }.bind(this));
  },

  /**
   * set empty links
   *
   * @method setEmptyLinks
   */
  setEmptyLinks: function () {
    //display an error message
    var quickLinks = [{
      label: this.get('quickLinksErrorMessage')
    }];
    this.set('quickLinks', quickLinks);
    this.set('isLoaded', true);
  },

  /**
   * set links that contain only one host
   *
   * @param {hostForQuickLink[]} hosts
   * @param {object} response
   * @method setSingleHostLinks
   */
  setSingleHostLinks: function (hosts, response) {
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if (!Em.isNone(quickLinksConfig)) {
      var quickLinks = [];
      var configProperties = this.get('configProperties');
      var protocol = this.setProtocol(configProperties, quickLinksConfig.get('protocol'));

      var links = Em.get(quickLinksConfig, 'links');
      links.forEach(function (link) {
        var publicHostName = this.publicHostName(link, hosts, protocol);
        if (publicHostName) {
          if (link.protocol) {
            protocol = this.setProtocol(configProperties, link.protocol);
          }
          var newItem = this.getHostLink(link, publicHostName, protocol, configProperties, response); //quicklink generated for the hbs template
          if (!Em.isNone(newItem)) {
            quickLinks.push(newItem);
          }
        }
      }, this);
      this.set('quickLinks', quickLinks);
      this.set('isLoaded', true);
    }
    else {
      this.set('quickLinks', []);
      this.set('isLoaded', false);
    }
  },

  publicHostName: function(link, hosts, protocol) {
    if (link.host) { // if quicklink overrides hostcomponent host name, get host from config
      var configProperties = this.get('configProperties');
      var hostProperty = Em.get(link.host, protocol + '_property');
      var site = configProperties.findProperty('type', Em.get(link.host, 'site'));
      return site && site.properties ? this.parseHostFromUri(site.properties[hostProperty]) : null;
    } else {
      var hostNameForComponent = hosts.findProperty('componentName', link.component_name);
      return hostNameForComponent ? hostNameForComponent.publicHostName : null;
    }
  },

  /**
   * @param {string} uri
   */
  parseHostFromUri: function(uri) {
    if (uri) {
      var match = uri.match(/:\/\/([^/:]+)/i);
      return match != null && match.length == 2 ? match[1] : uri;
    } else {
      return null;
    }
  },

  getMultipleHostLinks: function (hosts) {
    var quickLinksConfig = this.getQuickLinksConfiguration();
    if (Em.isNone(quickLinksConfig)) {
      return;
    }

    var quickLinksArray = [];
    hosts.forEach(function (host) {
      var publicHostName = host.publicHostName;
      var quickLinks = [];
      var configProperties = this.get('configProperties');

      var protocol = this.setProtocol(configProperties, quickLinksConfig.get('protocol'));
      var serviceName = Em.get(quickLinksConfig, 'serviceName');
      var links = Em.get(quickLinksConfig, 'links');
      links.forEach(function (link) {
        var linkRemoved = Em.get(link, 'removed');
        var url = Em.get(link, 'url');
        if (url && !linkRemoved) {
          var hostNameRegExp = new RegExp('([\\w\\W]*):\\d+');
          if (serviceName === 'HDFS') {
            var config;
            var configPropertiesObject = configProperties.findProperty('type', 'hdfs-site');
            if (configPropertiesObject && configPropertiesObject.properties) {
              var properties = configPropertiesObject.properties;
              var nnKeyRegex = new RegExp('^dfs\.namenode\.' + protocol + '-address\.');
              var nnProperties = Object.keys(properties).filter(key => nnKeyRegex.test(key));
              var nnPropertiesLength = nnProperties.length;
              for (var i = nnPropertiesLength; i--;) {
                var propertyName = nnProperties[i];
                var hostNameMatch = properties[propertyName] && properties[propertyName].match(hostNameRegExp);
                if (hostNameMatch && hostNameMatch[1] === host.publicHostName) {
                  config = propertyName;
                  break;
                }
              }
            }
            var portConfig = Em.get(link, 'port');
            Em.set(portConfig, protocol + '_property', config);
            Em.set(link, 'port', portConfig)
          }

          var newItem = this.getHostLink(link, publicHostName, protocol, configProperties); //quicklink generated for the hbs template
          if (!Em.isNone(newItem)) {
            quickLinks.push(newItem);
          }
        }
      }, this);

      if (host.status) {
        quickLinks.set('publicHostNameLabel', Em.I18n.t('quick.links.publicHostName').format(host.publicHostName, host.status));
      } else {
        quickLinks.set('publicHostNameLabel', host.publicHostName);
      }
      quickLinksArray.push(quickLinks);
    }, this);
    return quickLinksArray;
  },

  /**
   * set links that contain multiple hosts
   *
   * @param {hostForQuickLink[]} hosts
   * @method setMultipleHostLinks
   */
  setMultipleHostLinks: function (hosts) {
    const quickLinks = this.getMultipleHostLinks(hosts);
    this.setProperties({
      quickLinksArray: [
        {
          links: quickLinks || []
        }
      ],
      isLoaded: !!quickLinks
    });
  },

  /**
   * set links that contain for multiple grouped hosts
   *
   * @param {hostForQuickLink[]} hosts
   * @method setMultipleGroupLinks
   */
  setMultipleGroupLinks: function (hosts) {
    let isLoaded = true;
    const quickLinksArray = this.get('masterGroups').map(group => {
      const groupHosts = hosts.filter(host => group.hosts.contains(host.hostName)),
        links = this.getMultipleHostLinks(groupHosts);
      if (!links) {
        isLoaded = false;
      }
      return {
        title: group.title,
        links: links || []
      };
    });
    this.setProperties({
      quickLinksArray,
      isLoaded
    });
  },

  /**
   * set status to hosts with OOZIE_SERVER
   *
   * @param {object[]} hosts
   * @returns {object[]}
   * @method processOozieHosts
   */
  processOozieHosts: function (hosts) {
    var activeOozieServers = this.get('content.hostComponents')
      .filterProperty('componentName', 'OOZIE_SERVER')
      .filterProperty('workStatus', 'STARTED')
      .mapProperty('hostName');

    var oozieHostsArray = hosts.filter(function (host) {
      host.status = Em.I18n.t('quick.links.label.active');
      return activeOozieServers.contains(host.hostName);
    }, this);

    if (!oozieHostsArray.length) {
      this.set('quickLinksErrorMessage', Em.I18n.t('quick.links.error.oozie.label'));
    }
    return oozieHostsArray;
  },

  /**
   * set status to hosts with NAMENODE
   *
   * @param {hostForQuickLink[]} hosts
   * @returns {object[]}
   * @method hostForQuickLink
   */
  processHdfsHosts: function (hosts) {
    return hosts.map(host => {
      const {hostName} = host,
        isActiveNameNode = Em.get(this, 'content.activeNameNodes').someProperty('hostName', hostName),
        isStandbyNameNode = Em.get(this, 'content.standbyNameNodes').someProperty('hostName', hostName);
      if (isActiveNameNode) {
        host.status = Em.I18n.t('quick.links.label.active');
      }
      if (isStandbyNameNode) {
        host.status = Em.I18n.t('quick.links.label.standby');
      }
      return host;
    });
  },

  /**
   * set status to hosts with HBASE_MASTER
   *
   * @param {hostForQuickLink[]} hosts
   * @param {object} response
   * @returns {hostForQuickLink[]}
   * @method processHbaseHosts
   */
  processHbaseHosts: function (hosts, response) {
    return hosts.map(function (host) {
      var isActiveMaster;
      response.items.filterProperty('Hosts.host_name', host.hostName).filter(function (item) {
        var hbaseMaster = item.host_components.findProperty('HostRoles.component_name', 'HBASE_MASTER');
        isActiveMaster = hbaseMaster && Em.get(hbaseMaster, 'metrics.hbase.master.IsActiveMaster');
      });
      if (isActiveMaster === 'true') {
        host.status = Em.I18n.t('quick.links.label.active');
      }
      else
        if (isActiveMaster === 'false') {
          host.status = Em.I18n.t('quick.links.label.standby');
        }
      return host;
    }, this);
  },

  /**
   * set status to hosts with RESOURCEMANAGER
   *
   * @param {hostForQuickLink[]} hosts
   * @returns {hostForQuickLink[]}
   * @method processYarnHosts
   */
  processYarnHosts: function (hosts) {
    return hosts.map(function (host) {
      var resourceManager = this.get('content.hostComponents')
        .filterProperty('componentName', 'RESOURCEMANAGER')
        .findProperty('hostName', host.hostName);
      var haStatus = resourceManager && resourceManager.get('haStatus');
      if (haStatus === 'ACTIVE') {
        host.status = Em.I18n.t('quick.links.label.active');
      }
      else
        if (haStatus === 'STANDBY') {
          host.status = Em.I18n.t('quick.links.label.standby');
        }
      return host;
    }, this);
  },

  /**
   * sets public host names for required masters of current service
   *
   * @param {object} response
   * @param {string} serviceName - selected serviceName
   * @returns {hostForQuickLink[]} containing hostName(s)
   * @method getHosts
   */
  getHosts: function (response, serviceName) {
    //The default error message when we cannot obtain the host information for the given service
    this.set('quickLinksErrorMessage', Em.I18n.t('quick.links.error.nohosts.label').format(serviceName));
    var hosts = [];
    var quickLinkConfigs = App.QuickLinksConfig.find().findProperty("id", serviceName);
    if (quickLinkConfigs) {
      var links = quickLinkConfigs.get('links');
      var componentNames = links.mapProperty('component_name').uniq();
      componentNames.forEach(function (_componentName) {
        var componentHosts = this.findHosts(_componentName, response);
        switch (serviceName) {
          case 'OOZIE':
            hosts = hosts.concat(this.processOozieHosts(componentHosts));
            break;
          case "HDFS":
            hosts = hosts.concat(this.processHdfsHosts(componentHosts));
            break;
          case "HBASE":
            hosts = hosts.concat(this.processHbaseHosts(componentHosts, response));
            break;
          case "YARN":
            hosts = hosts.concat(this.processYarnHosts(componentHosts));
            break;
          case "SMARTSENSE":
            if(!App.MasterComponent.find().findProperty('componentName', _componentName)) {
              hosts = [];
              break;
            }
          default:
            hosts = hosts.concat(componentHosts);
            break;
        }
      }, this);
    }
    return hosts;
  },

  /**
   * find host public names
   *
   * @param {string} componentName
   * @param {object} response
   * @returns {hostForQuickLink[]}
   */
  findHosts: function (componentName, response) {
    var hosts = [];
    var component = App.MasterComponent.find().findProperty('componentName', componentName) ||
      App.SlaveComponent.find().findProperty('componentName', componentName);
    if (component) {
      var hostComponents = component.get('hostNames') || [];
      hostComponents.forEach(function (_hostName) {
        var host = this.getPublicHostName(response.items, _hostName);
        if (host) {
          hosts.push({
            hostName: _hostName,
            publicHostName: host,
            componentName: componentName
          });
        }
      }, this);
    }
    return hosts;
  },

  /**
   * 'http' for 'https'
   * 'https' for 'https'
   * empty string otherwise
   *
   * @param {string} type
   * @returns {string}
   * @method reverseType
   */
  reverseType: function (type) {
    if ('https' === type) {
      return 'http';
    }
    if ('http' === type) {
      return 'https';
    }
    return '';
  },

  /**
   *
   * @param {object[]} configProperties
   * @param {string} configType
   * @param {string} property
   * @param {string} desiredState
   * @returns {boolean}
   * @method meetDesired
   */
  meetDesired: function (configProperties, configType, property, desiredState) {
    var currentConfig = configProperties.findProperty('type', configType);
    if (!currentConfig) {
      return false;
    }
    var currentPropertyValue = currentConfig.properties[property];
    if ('NOT_EXIST' === desiredState) {
      return Em.isNone(currentPropertyValue);
    }
    if ('EXIST' === desiredState) {
      return !Em.isNone(currentPropertyValue);
    }
    return desiredState === currentPropertyValue;
  },

  /**
   * setProtocol - if cluster is secure for some services (GANGLIA, MAPREDUCE2, YARN and servicesSupportsHttps)
   * protocol becomes "https" otherwise "http" (by default)
   *
   * @param {Object} configProperties
   * @param {Object} protocolConfig
   * @returns {string} "https" or "http" only!
   * @method setProtocol
   */
  setProtocol: function (configProperties, protocolConfig) {
    var hadoopSslEnabled = false;

    if (!Em.isEmpty(configProperties)) {
      var hdfsSite = configProperties.findProperty('type', 'hdfs-site');
      hadoopSslEnabled = hdfsSite && Em.get(hdfsSite, 'properties') && hdfsSite.properties['dfs.http.policy'] === 'HTTPS_ONLY';
    }

    if (!protocolConfig) {
      return hadoopSslEnabled ? 'https' : 'http';
    }

    var protocolType = Em.get(protocolConfig, 'type');

    if ('HTTPS_ONLY' === protocolType) {
      return 'https';
    }
    if ('HTTP_ONLY' === protocolType) {
      return 'http';
    }

    protocolType = protocolType.toLowerCase();

    var count = 0;
    var checks = Em.get(protocolConfig, 'checks');
    if (!checks) {
      return hadoopSslEnabled ? 'https' : 'http';
    }
    checks.forEach(function (check) {
      var configType = Em.get(check, 'site');
      var property = Em.get(check, 'property');
      var desiredState = Em.get(check, 'desired');
      var checkMeet = this.meetDesired(configProperties, configType, property, desiredState);
      if (!checkMeet) {
        count++;
      }
    }, this);

    return count ? this.reverseType(protocolType) : protocolType;
  },

  /**
   * sets the port of quick link
   *
   * @param {object} portConfigs
   * @param {string} protocol
   * @param {object[]} configProperties
   * @returns {string}
   * @method setPort
   */
  setPort: function (portConfigs, protocol, configProperties) {
    if (!portConfigs) {
      return '';
    }
    var defaultPort = Em.get(portConfigs, protocol + '_default_port');
    var portProperty = Em.get(portConfigs, protocol + '_property');
    var site = configProperties.findProperty('type', Em.get(portConfigs, 'site'));
    var propertyValue = site && site.properties && site.properties[portProperty];

    if (!propertyValue) {
      return defaultPort;
    }

    var regexValue = Em.get(portConfigs, 'regex');
    if (protocol === 'https') {
      var httpsRegex = Em.get(portConfigs, 'https_regex');
      if (httpsRegex) {
        regexValue = httpsRegex;
      }
    }
    regexValue = regexValue.trim();
    if (regexValue) {
      var re = new RegExp(regexValue);
      var portValue = propertyValue.match(re);
      try {
        return portValue[1];
      } catch (err) {
        return defaultPort;
      }
    } else {
      return propertyValue;
    }
  },

  setTooltip: function () {
    Em.run.next(() => {
      if (this.get('showQuickLinks') && this.get('isLoaded') && this.get('quickLinksArray.length')) {
        App.tooltip($(`[rel="${this.get('tooltipAttribute')}"]`));
      }
    });
  }.observes('showQuickLinks', 'isLoaded', 'quickLinksArray.length')
});
