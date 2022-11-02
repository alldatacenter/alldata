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

describe('#_overrideConfigIsRequired', function () {
  var instanceObject,
    configs,
    serviceConfig;

  beforeEach(function() {
    var mixinObject = Em.Controller.extend(App.ToggleIsRequiredMixin, {});

    instanceObject = mixinObject.create({});

    configs = Em.A([
      App.ServiceConfigProperty.create({ name: 'kdc_hosts', value: '', category: 'KDC', serviceName: 'KERBEROS', isRequired: true}),
      App.ServiceConfigProperty.create({ name: 'admin_server_host', value: '', category: 'KDC', serviceName: 'KERBEROS', isRequired: true}),
      App.ServiceConfigProperty.create({ name: 'admin_principal', value: '', category: 'KDC', serviceName: 'KERBEROS', isRequired: true}),
      App.ServiceConfigProperty.create({ name: 'admin_password', value: '', category: 'KDC', serviceName: 'KERBEROS', isRequired: true})
    ]);

    configs.forEach(function(config) {
      config.validate(); // make isRequired to trigger validation and to set every property's error flag to true
    });
    serviceConfig = App.ServiceConfig.create({
      'serviceName': 'KERBEROS',
      'configs': configs
    });

  });

  it('should make isRequired = false for kerberos properties', function () {
    instanceObject.overrideConfigIsRequired(serviceConfig);
    expect(configs.everyProperty('isRequired', false)).to.be.true;
    expect(configs.everyProperty('error', false)).to.be.true;
  });

  it('should make isRequired = true for kerberos properties', function () {
    // toggle to false
    instanceObject.overrideConfigIsRequired(serviceConfig);
    // toggle to true
    instanceObject.overrideConfigIsRequired(serviceConfig);
    expect(configs.everyProperty('isRequired', true)).to.be.true;
  });
});