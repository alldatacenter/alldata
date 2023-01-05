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

import {NgModule} from "@angular/core";

import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {BaseMangeRoutingModule} from "./base.manage-routing.module";
import {ApplistComponent} from "./applist.component";
import {BaseMangeIndexComponent} from "./base.manage.index.component";
import {DepartmentListComponent} from "./department.list.component";
import {AddGlobalParamComponent} from "./global.add.param";
import {GlobalUpdateParamComponent} from "./global.update.param";
import {AddAppFormComponent} from "./addapp-form.component";
import {AddAppStepFlowComponent} from "./addapp.step.flow.component";
import {AddAppFlowDirective} from "./addapp.directive";
import {AddAppConfirmComponent} from "./addapp-confirm.component";
import {TisCommonModule} from "../common/common.module";

import {NzStepsModule} from 'ng-zorro-antd/steps';
import {NzInputModule} from 'ng-zorro-antd/input';
import {NzButtonModule} from 'ng-zorro-antd/button';
import {NzTabsModule} from 'ng-zorro-antd/tabs';
import {BaseConfigComponent} from "./base-config.component";
import {AddappSelectNodesComponent} from "./addapp-select-nodes.component";
import {DepartmentAddComponent} from "./department.add.component";
import {DataxWorkerComponent} from "./datax.worker.component";
import {DataxWorkerAddStep1Component} from "./datax.worker.add.step1.component";
import {DataxWorkerAddStep0Component} from "./datax.worker.add.step0.component";
import {DataxWorkerAddStep2Component} from "./datax.worker.add.step2.component";
import {DataxWorkerAddStep3Component} from "./datax.worker.add.step3.component";
import {DataxWorkerRunningComponent} from "./datax.worker.running.component";

import {MarkdownModule} from "ngx-markdown";
import {ErrorListComponent} from "./error.list.component";


@NgModule({
  id: 'basemanage',
  imports: [MarkdownModule.forChild(), CommonModule, FormsModule, BaseMangeRoutingModule, TisCommonModule, NzStepsModule, NzInputModule, NzButtonModule, NzTabsModule],
  declarations: [
    ApplistComponent, ErrorListComponent, DepartmentAddComponent, BaseMangeIndexComponent, BaseConfigComponent, DepartmentListComponent, AddGlobalParamComponent, GlobalUpdateParamComponent
    , AddAppFormComponent, AddAppStepFlowComponent, AddAppFlowDirective, AddAppConfirmComponent, AddappSelectNodesComponent
    , DataxWorkerComponent, DataxWorkerAddStep1Component, DataxWorkerAddStep0Component, DataxWorkerAddStep2Component, DataxWorkerAddStep3Component
    , DataxWorkerRunningComponent
  ],
  entryComponents: [ApplistComponent
    , BaseMangeIndexComponent, DepartmentListComponent, AddGlobalParamComponent
    , GlobalUpdateParamComponent, AddAppFormComponent, AddAppConfirmComponent, AddappSelectNodesComponent
  ],
  // providers: [TISService,ScriptService]
  exports: [AddAppFlowDirective]
})
export class BasiManageModule {
}
