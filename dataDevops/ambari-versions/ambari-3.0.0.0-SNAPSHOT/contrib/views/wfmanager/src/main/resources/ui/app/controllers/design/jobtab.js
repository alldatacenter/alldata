/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/
import Ember from 'ember';

export default Ember.Controller.extend({
  dashboardContext: Ember.inject.service(),
  fromBundleId: null,
  fromCoordId: null,
  actions : {
    close : function(){
      this.sendAction('onCloseJobDetails');
    },
    showWorkflow : function(workflowId){
      this.get('dashboardContext').setCurrentCoordName(this.get('model.coordJobName'));
      this.transitionToRoute('design.jobtab', {
        queryParams: {
          jobType: 'wf',
          id: workflowId,
          fromBundleId: this.get('fromBundleId'),
          fromCoordId: this.get('model.coordJobId')
        }
      });
    },
    showCoord : function(coordJobId){
      this.get('dashboardContext').setCurrentBundleName(this.get('model.bundleJobName'));
      this.transitionToRoute('design.jobtab', {
        queryParams: {
          jobType: 'coords',
          id: coordJobId,
          fromBundleId: this.get('model.bundleJobId')
        }
      });
    },
    back : function (jobType, jobId){
      this.transitionToRoute('design.jobtab', {
        queryParams: {
          jobType: jobType,
          id: jobId,
          fromBundleId : (jobType === 'coords') ? this.get('fromBundleId') : null,
          fromCoordId : null
        }
      });
    },
  }
});
