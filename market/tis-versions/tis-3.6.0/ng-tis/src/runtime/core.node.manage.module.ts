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

import {NgModule, OnInit} from '@angular/core';
import {CorenodemanageComponent} from './corenodemanage.component';
import {TriggerDumpComponent} from './trigger_dump.component';
import {CoreNodeRoutingModule} from './core.node.manage-routing.module';

import {CommonModule} from '@angular/common';
import {CorenodemanageIndexComponent} from './core.node.manage.index.component';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {IndexQueryComponent, QueryResultRowContentComponent} from './index.query.component';
import {PojoComponent} from './pojo.component';
import {CopyOtherCoreComponent} from './copy.other.core.component';
import {SyncConfigComponent} from './sync.cfg.component';
import {TisCommonModule} from "../common/common.module";
import {MembershipComponent} from "./membership.component";
import {MonitorComponent} from "./monitor.component";
import {CorePluginConfigComponent} from "./core.plugin.config.component";
import {NzLayoutModule} from 'ng-zorro-antd/layout';
import {NzCollapseModule} from 'ng-zorro-antd/collapse';
import {IncrBuildComponent} from "./incr.build.component";

import {NzStepsModule} from 'ng-zorro-antd/steps';
import {NzButtonModule} from 'ng-zorro-antd/button';
import {IncrBuildStep0Component} from "./incr.build.step0.component";
// import {IncrBuildStep2Component} from "./incr.build.step2.component";
import {IncrBuildStep3Component} from "./incr.build.step3.component";
import {IncrBuildStep1Component, IncrBuildStep1ExecEngineSelectComponent} from "./incr.build.step1.component";
import {IncrBuildStep2SetSinkComponent} from "./incr.build.step2.setSink.components";
import {IncrBuildStep4RunningComponent} from "./incr.build.step4.running.component";
import {NgTerminalModule} from 'ng-terminal';
import {NzTabsModule} from 'ng-zorro-antd/tabs';
import {NzFormModule} from 'ng-zorro-antd/form';
import {NzInputModule} from 'ng-zorro-antd/input';
import {NzSelectModule} from 'ng-zorro-antd/select';
import {NzInputNumberModule} from 'ng-zorro-antd/input-number';
import {ChartsModule} from 'ng2-charts';

import {NzDividerModule} from 'ng-zorro-antd/divider';
import {NzIconModule} from 'ng-zorro-antd/icon';
import {NzTableModule} from 'ng-zorro-antd/table';
import {NzTagModule} from 'ng-zorro-antd/tag';
import {NzPopoverModule} from 'ng-zorro-antd/popover';
import {IncrBuildStep4RunningTabBaseComponent} from "./incr.build.step4.running.tab-base.component";
import {LineChartComponent} from "./line.chart.component";
import {IncrBuildStep4StopedComponent} from "./incr.build.step4.stoped.component";
import {IncrBuildStep1ExtendSelectedTabPropsComponent} from "./incr.build.step1.extend.selected.tab.props.component";


@NgModule({
  id: 'coremanage',
  imports: [CommonModule, CoreNodeRoutingModule, FormsModule, TisCommonModule, NzLayoutModule, NzCollapseModule
    , NzStepsModule, NzButtonModule, NzTabsModule, NgTerminalModule, NzFormModule, NzInputModule, ReactiveFormsModule, NzSelectModule, NzInputNumberModule
    , ChartsModule, NzDividerModule, NzIconModule, NzTableModule, NzTagModule, NzPopoverModule
  ],
  declarations: [LineChartComponent,
    TriggerDumpComponent, CorePluginConfigComponent, QueryResultRowContentComponent, IncrBuildStep4RunningTabBaseComponent,
    IndexQueryComponent, PojoComponent,
    CorenodemanageComponent, CorenodemanageIndexComponent,
    CopyOtherCoreComponent
    , SyncConfigComponent
    , MembershipComponent, MonitorComponent, IncrBuildComponent, IncrBuildStep0Component
    , IncrBuildStep1Component, IncrBuildStep1ExecEngineSelectComponent, IncrBuildStep2SetSinkComponent, IncrBuildStep1ExtendSelectedTabPropsComponent ,
    // IncrBuildStep2Component,
    IncrBuildStep3Component, IncrBuildStep4RunningComponent , IncrBuildStep4StopedComponent
  ],
  providers: [
    // {provide: NZ_ICON_DEFAULT_TWOTONE_COLOR, useValue: '#00ff00'}, // 不提供的话，即为 Ant Design 的主题蓝色
    // {provide: NZ_ICONS, useValue: icons}
  ],
  exports: [
    //  IncrPodLogsStatusComponent
  ],
  entryComponents: [TriggerDumpComponent, PojoComponent,
    SyncConfigComponent,
    CopyOtherCoreComponent
    , MonitorComponent, MembershipComponent, IncrBuildStep0Component, IncrBuildStep1Component, IncrBuildStep3Component, IncrBuildStep4RunningComponent]
})
export class CoreNodeManageModule implements OnInit {
  ngOnInit(): void {

  }
}
