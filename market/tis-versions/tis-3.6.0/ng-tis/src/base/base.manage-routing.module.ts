/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import {NgModule} from '@angular/core';

import {BaseMangeIndexComponent} from './base.manage.index.component';

import {Routes, RouterModule} from '@angular/router';
import {DepartmentListComponent} from './department.list.component';
import {ApplistComponent} from './applist.component';
import {OperationLogComponent} from '../common/operation.log.component';
import {AddAppStepFlowComponent} from './addapp.step.flow.component';
import {BaseConfigComponent} from "./base-config.component";
import {SnapshotsetComponent} from "../common/snapshotset.component";
import {SchemaEditVisualizingModelComponent, SchemaXmlEditComponent} from "../corecfg/schema-xml-edit.component";
import {DataxAddComponent} from "./datax.add.component";
import {DataxWorkerComponent} from "./datax.worker.component";
import {ProcessMeta} from "../runtime/misc/RCDeployment";
import {PluginManageComponent} from "./plugin.manage.component";
import {StepType} from "../common/steps.component";
import {ErrorListComponent} from "./error.list.component";


const dataXWorkerCfg: { processMeta: ProcessMeta }
  = {
  processMeta: {
    targetName: "datax-worker"
    , pageHeader: "DataX分布式执行器"
    , notCreateTips: "还未创建DataX执行器，创建之后可以将DataX构建任务提交到K8S集群，高效并行执行DataX数据同步任务"
    , createButtonLabel: "创建DataX执行器"
    , stepsType: StepType.CreateWorkderOfDataX
    , supportK8SReplicsSpecSetter: true
  }
};

const flinkClusterCfg: { processMeta: ProcessMeta }
  = {
  processMeta: {
    targetName: "flink-cluster"
    , pageHeader: "Flink Native Cluster执行器"
    , createButtonLabel: "创建Flink Native Cluster执行器"
    , notCreateTips: "还未创建Flink Native Cluster执行器，创建之后可以将Flink Job提交到K8S集群，高效并行执行数据实时同步任务"
    , stepsType: StepType.CreateFlinkCluster
    , supportK8SReplicsSpecSetter: false
  }
};

const basemanageRoutes: Routes = [
  {
    path: '', component: BaseMangeIndexComponent,
    children: [
      {
        path: '',
        children: [
          {
            path: 'applist',
            component: ApplistComponent
          },
          {
            path: 'basecfg',
            children: [
              {
                path: '',
                component: BaseConfigComponent
              },
              {
                path: ':tab',
                component: BaseConfigComponent
              }
            ]
          }
          ,
          {
            path: 'plugin-manage',
            children: [
              {
                path: '',
                component: PluginManageComponent
              },
              {
                path: ':tab',
                component: PluginManageComponent
              }
            ]
          }
          ,
          {   // 添加索引
            path: 'appadd',
            component: AddAppStepFlowComponent
          },
          {   // 配置模版一览
            path: 'tpl/snapshotset',
            component: SnapshotsetComponent,
            data: {
              showBreadcrumb: true,
              template: true
            }
          },
          {
            path: 'tpl/xml_conf/:restype/:snapshotid',
            component: SchemaXmlEditComponent
          },
          {
            path: 'tpl/schema_visual/:snapshotid',
            component: SchemaEditVisualizingModelComponent
          },
          {
            path: 'departmentlist',
            component: DepartmentListComponent
          },
          {
            path: 'operationlog',
            component: OperationLogComponent,
            data: {showBreadcrumb: true}
          },
          {
            path: "sys-errors",
            component: ErrorListComponent,
            data: {showBreadcrumb: true}
          },
          {
            path: '',
            component: ApplistComponent
          },
          {
            path: 'dataxadd',
            component: DataxAddComponent
          },
          {
            path: dataXWorkerCfg.processMeta.targetName,
            component: DataxWorkerComponent,
            data: dataXWorkerCfg
          },
          {
            path: flinkClusterCfg.processMeta.targetName,
            component: DataxWorkerComponent,
            data: flinkClusterCfg
          },
          {
            path: flinkClusterCfg.processMeta.targetName + '/:targetTab',
            component: DataxWorkerComponent,
            data: flinkClusterCfg
          },
          {
            path: dataXWorkerCfg.processMeta.targetName + '/:targetTab',
            component: DataxWorkerComponent,
            data: dataXWorkerCfg
          }
        ]
      }
    ]
  },

];

@NgModule({
  imports: [
    RouterModule.forChild(basemanageRoutes)
  ],
  declarations: [], exports: [
    RouterModule
  ]
})
export class BaseMangeRoutingModule {
}
