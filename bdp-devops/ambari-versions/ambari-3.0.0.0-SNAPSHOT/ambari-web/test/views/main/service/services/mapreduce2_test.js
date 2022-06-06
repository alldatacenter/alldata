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
require('/views/main/service/services/mapreduce2');

describe('App.MainDashboardServiceMapreduce2View', function () {
  var view;

  beforeEach(function() {
    view = App.MainDashboardServiceMapreduce2View.create();
  });

 describe("#titleInfo", function() {

   it("HISTORYSERVER stopped", function() {
     view.set('service', Em.Object.create({
       hostComponents: [
         Em.Object.create({
           componentName: 'HISTORYSERVER',
           workStatus: App.HostComponentStatus.stopped
         })
       ]
     }));
     view.propertyDidChange('titleInfo');
     expect(view.get('titleInfo')).to.be.equal(view.t('services.mapreduce2.history.stopped'));
   });

   it("HISTORYSERVER absent", function() {
     view.set('service', Em.Object.create({
       hostComponents: []
     }));
     view.propertyDidChange('titleInfo');
     expect(view.get('titleInfo')).to.be.equal(view.t('services.mapreduce2.history.unknown'));
   });

   it("HISTORYSERVER started", function() {
     view.set('service', Em.Object.create({
       hostComponents: [
         Em.Object.create({
           componentName: 'HISTORYSERVER',
           workStatus: App.HostComponentStatus.started
         })
       ]
     }));
     view.propertyDidChange('titleInfo');
     expect(view.get('titleInfo')).to.be.equal(view.t('services.mapreduce2.history.running'));
   });
 });
});