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
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {DatasourceComponent} from "./ds.component";
import {WorkflowComponent} from "./workflow.component";
import {OfflineRoutingModule} from "./offline-routing.module";
import {OffileIndexComponent} from "./offline.index.component";
import {DbAddComponent} from "./db.add.component";
import {TableAddComponent} from "./table.add.component";
import {TableAddStep1Component} from "./table.add.step1.component";
import {WorkflowAddComponent} from "./workflow.add.component";
import {NzTabsModule} from 'ng-zorro-antd/tabs';

import {NzGridModule} from 'ng-zorro-antd/grid';
import {NzTableModule} from 'ng-zorro-antd/table';
import {TisCommonModule} from "../common/common.module";
import {NzButtonModule} from 'ng-zorro-antd/button';
import {NzCascaderModule} from 'ng-zorro-antd/cascader';
import {NzFormModule} from 'ng-zorro-antd/form';

import {WorkflowAddDbtableSetterComponent} from "./workflow.add.dbtable.setter.component";
import {BasiManageModule} from "../base/base.manage.module";
import {NzSelectModule} from 'ng-zorro-antd/select';
import {WorkflowAddJoinComponent} from "./workflow.add.join.component";
import {WorkflowAddUnionComponent} from "./workflow.add.union.component";
import {WorkflowAddNestComponent} from "./workflow.add.nest.component";
import {NzTreeModule} from 'ng-zorro-antd/tree';
import {NzLayoutModule} from 'ng-zorro-antd/layout';
import {NzInputModule} from 'ng-zorro-antd/input';
import {NzDescriptionsModule} from 'ng-zorro-antd/descriptions';
import {NzNotificationModule} from 'ng-zorro-antd/notification';
import {NzDividerModule} from 'ng-zorro-antd/divider';
import {NZ_ICON_DEFAULT_TWOTONE_COLOR, NZ_ICONS} from 'ng-zorro-antd/icon';

import {NzCollapseModule} from 'ng-zorro-antd/collapse';
import {WorkflowERComponent} from "./workflow.er.component";
import {WorkflowAddErCardinalityComponent} from "./workflow.add.er.cardinality.component";
import {IconDefinition} from "@ant-design/icons-angular";
import {DeleteOutline} from "@ant-design/icons-angular/icons";
import {WorkflowAddErMetaComponent} from "./workflow.add.er.meta.component";


const icons: IconDefinition[] = [DeleteOutline];

@NgModule({
  id: 'offline',
  imports: [ // CommonModule,
    NzCollapseModule,
    NzTabsModule,
    NzInputModule, NzTableModule,
    FormsModule, BasiManageModule, NzTreeModule, NzLayoutModule, NzDescriptionsModule
    , OfflineRoutingModule, // TreeModule,
    TisCommonModule, NzButtonModule, NzCascaderModule
    , CommonModule, NzFormModule, NzSelectModule, ReactiveFormsModule, NzNotificationModule
    , NzDividerModule, NzGridModule
  ],
  providers: [
    {provide: NZ_ICON_DEFAULT_TWOTONE_COLOR, useValue: '#00ff00'}, // 不提供的话，即为 Ant Design 的主题蓝色
    {provide: NZ_ICONS, useValue: icons}
  ],
  declarations: [
    WorkflowComponent, DatasourceComponent, OffileIndexComponent, DbAddComponent, WorkflowAddErMetaComponent
    , TableAddComponent, TableAddStep1Component, // TableAddStep2Component,
    WorkflowAddComponent,
    WorkflowAddDbtableSetterComponent, WorkflowAddJoinComponent, WorkflowAddUnionComponent
    // tslint:disable-next-line:whitespace
    , WorkflowAddNestComponent, WorkflowERComponent, WorkflowAddErCardinalityComponent
  ],
  entryComponents: [WorkflowComponent, DatasourceComponent, OffileIndexComponent, DbAddComponent, TableAddComponent, WorkflowAddErMetaComponent
    , WorkflowAddComponent,
    WorkflowAddDbtableSetterComponent, WorkflowAddJoinComponent, WorkflowAddUnionComponent
    , WorkflowAddNestComponent, WorkflowAddErCardinalityComponent
  ]
})
export class OfflineModule {
}
