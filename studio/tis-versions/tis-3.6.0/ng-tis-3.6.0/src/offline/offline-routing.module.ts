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
import {OffileIndexComponent} from './offline.index.component';
import {DatasourceComponent} from './ds.component';
import {WorkflowComponent} from './workflow.component';
import {RouterModule, Routes} from '@angular/router';
import {WorkflowAddComponent} from "./workflow.add.component";
import {BuildProgressComponent} from "../common/core.build.progress.component";
import {FullBuildHistoryComponent} from "../common/full.build.history.component";

const coreNodeRoutes: Routes = [
  {
    path: '', component: OffileIndexComponent,
    children: [
      {
        path: '',
        children: [
          {
            path: 'ds',
            component: DatasourceComponent,
          },
          {
            path: 'wf',
            component: WorkflowComponent
          },
          {
            path: 'wf/build_history/:wfid/:taskid',
            component: BuildProgressComponent,
            data: {showBreadcrumb: true}
          },
          {
            path: 'wf/build_history/:wfid',
            component: FullBuildHistoryComponent,
            data: {showBreadcrumb: true}
          },
          {
            path: 'wf_add',
            component: WorkflowAddComponent
          },
          {
            path: 'wf_update/:name',
            component: WorkflowAddComponent
          },
          {
            path: 'wf_update/:name/build_history/:wfid/:taskid',
            component: BuildProgressComponent,
            data: {showBreadcrumb: true}
          },
          {
            path: 'wf_update/:name/build_history/:wfid',
            component: FullBuildHistoryComponent,
            data: {showBreadcrumb: true}
          },
          {
            path: '',
            component: DatasourceComponent
          }
        ]
      }
    ]
  },

];

@NgModule({
  imports: [
    RouterModule.forChild(coreNodeRoutes)
  ],
  declarations: [],
  exports: [
    RouterModule
  ]
})
export class OfflineRoutingModule {
}
