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

App.ToggleIsRequiredMixin = Em.Mixin.create({
  /**
   * Override isRequired property of the configurations in given situation
   * @param serviceConfigs - make changes only for properties from this service
   */
  overrideConfigIsRequired: function (serviceConfigs) {
    var excludeProperties = [
      {
        name: 'KERBEROS',                                                                // affected service
        exclude: ['kdc_hosts', 'admin_server_host', 'admin_principal', 'admin_password'] // affected properties
      },
      {
        name: 'KERBEROS_GENERAL',                                                                // affected service
        exclude: ['kdc_hosts', 'admin_server_host', 'admin_principal', 'admin_password'] // affected properties
      }
    ];
    var serviceName = serviceConfigs.get('serviceName'),
      service = excludeProperties.findProperty('name', serviceName),
      configs = serviceConfigs.get('configs');

    if (service && !Em.isEmpty(configs)) {
      service.exclude.forEach(function (property) {
        var serviceProperty = configs.findProperty('name', property);
        if (serviceProperty) {
          var value = serviceProperty.get('isRequired');
          Em.set(serviceProperty, "isRequired", !value);
          if (value && serviceProperty.get('value')==='') {
            // clear validation errors because validation does not clear isRequired validations
            Em.set(serviceProperty, "error", false);
            Em.set(serviceProperty, "errorMessage", '');
          }
          // validate property
          serviceProperty.validate();
        }
      });
    }
  }

})