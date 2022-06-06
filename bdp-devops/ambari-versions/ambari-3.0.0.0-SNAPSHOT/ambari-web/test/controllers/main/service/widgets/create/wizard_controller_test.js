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

App = require('app');

require('controllers/main/service/widgets/create/wizard_controller');


describe('App.WidgetWizardController', function () {
  var controller;

  /**
   * tests the function with following hierarchical queue scenario
   *                   root 
   *                    |
   *                  queue1
   *                 /     \
   *             queue2   queue3
   *                  
   */
  describe("#substitueQueueMetrics", function () {
    beforeEach(function () {
      controller = App.WidgetWizardController.create();
      sinon.stub(App.YARNService, 'find', function () {
        return Em.Object.create({
          'allQueueNames': ["root", "root/queue1", "root/queue1/queue2", "root/queue1/queue3"]
        });
      });
    });
    afterEach(function () {
      App.YARNService.find.restore();
    });


    var testCases = [
      {
        msg: 'AMS Queue metric with regex as name and regex as path should be replaced with actual metric name and path of all existing queues',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            level: 'COMPONENT',
            name: 'yarn.QueueMetrics.Queue=(.+).AppsFailed',
            point_in_time: false,
            service_name: 'YARN',
            temporal: true,
            type: 'GANGLIA',
            widget_id: 'metrics/yarn/Queue/$1.replaceAll("([.])","/")/AppsFailed'
          }
        ],
        expectedResult: [
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/AppsFailed"
          },
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.queue1.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/queue1/AppsFailed"
          },
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.queue1.queue2.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/queue1/queue2/AppsFailed"
          },
          {
            "component_name": "RESOURCEMANAGER",
            "level": "COMPONENT",
            "name": "yarn.QueueMetrics.Queue=root.queue1.queue3.AppsFailed",
            "point_in_time": false,
            "service_name": "YARN",
            "temporal": true,
            "type": "GANGLIA",
            "widget_id": "metrics/yarn/Queue/root/queue1/queue3/AppsFailed"
          }
        ]
      },
      {
        msg: 'JMX Queue metric with regex as name and regex as path should be replaced with actual metric name and path of all existing queues',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics(.+).AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/$1.replaceAll(",q(\d+)=","/").substring(1)/AppsFailed'
          }
        ],
        expectedResult: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/AppsFailed'
          },
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root,q1=queue1.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/queue1/AppsFailed'
          },
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root,q1=queue1,q2=queue2.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/queue1/queue2/AppsFailed'
          },
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics,q0=root,q1=queue1,q2=queue3.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/root/queue1/queue3/AppsFailed'
          }
        ]
      },
      {
        msg: 'AMS Queue metric without regex in name and path should retain same name and path',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            level: 'COMPONENT',
            name: 'yarn.QueueMetrics.Queue.Clustermetrics.AppsFailed',
            point_in_time: false,
            service_name: 'YARN',
            temporal: true,
            type: 'GANGLIA',
            widget_id: 'metrics/yarn/Queue/Clustermetrics/AppsFailed'
          }
        ],
        expectedResult: [
          {
            component_name: 'RESOURCEMANAGER',
            level: 'COMPONENT',
            name: 'yarn.QueueMetrics.Queue.Clustermetrics.AppsFailed',
            point_in_time: false,
            service_name: 'YARN',
            temporal: true,
            type: 'GANGLIA',
            widget_id: 'metrics/yarn/Queue/Clustermetrics/AppsFailed'
          }
        ]
      },
      {
        msg: 'JMX Queue metric without regex in name and path should retain same name and path',
        inputMetrics: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics.clusterMetric.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/clusterMetric/AppsFailed'
          }
        ],
        expectedResult: [
          {
            component_name: 'RESOURCEMANAGER',
            host_component_criteria: 'host_components/HostRoles/ha_state=ACTIVE',
            level: 'HOSTCOMPONENT',
            name: 'Hadoop:service=ResourceManager,name=QueueMetrics.clusterMetric.AppsFailed',
            point_in_time: true,
            service_name: 'YARN',
            temporal: false,
            type: 'JMX',
            widget_id: 'metrics/yarn/Queue/clusterMetric/AppsFailed'
          }
        ]
      }
    ];
    testCases.forEach(function (_testCase) {
      it(_testCase.msg, function () {
        var result = controller.substitueQueueMetrics(_testCase.inputMetrics);
        expect(JSON.stringify(result)).to.equal(JSON.stringify(_testCase.expectedResult));
      });
    });
  });
});
