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

const App = require('app');

App.ServicesChecksView = Em.View.extend({
  templateName: require('templates/main/admin/stack_upgrade/custom_cluster_checks/custom_services_checks'),
  
  services: function () {
    return App.Service.find().toArray().filter( ( service ) => {
      if (this.get('check').failed_on.indexOf( service.get('id') ) != -1){
        Ember.set(service, 'isSmokeTestDisabled', this.isSmokeTestDisabled( service ) );
        return service;
      }
      else return;
    })
  }.property('App.router.clusterController.isLoaded').volatile(),

  isSmokeTestDisabled: function ( service ) {
    const controller =  App.router.get('mainServiceItemController');
    controller.set('content', service);
    return controller.get('isSmokeTestDisabled');
  },
  
  runSmokeTest: function (event) {
    const service = event.context;
    const controller =  App.router.get('mainServiceItemController');
    controller.set('content', service);
    controller.runSmokeTest( service );
  }


});
