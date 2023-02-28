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

import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

import {NzLayoutModule} from 'ng-zorro-antd/layout';

import {NzCollapseModule} from 'ng-zorro-antd/collapse';

import {NzStepsModule} from 'ng-zorro-antd/steps';
import {NzButtonModule} from 'ng-zorro-antd/button';
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
import {TisCommonModule} from "../common/common.module";
import {DataxRoutingModule} from "./datax-routing.module";
import {DataxIndexComponent} from "./datax.index.component";
import {DataxMainComponent} from "./datax.main.component";
import {DataxConfigComponent} from "./datax.config.component";
import {CoreNodeManageModule} from "../runtime/core.node.manage.module";

@NgModule({
  id: 'datax',
  imports: [CommonModule, DataxRoutingModule, FormsModule, CoreNodeManageModule, TisCommonModule, NzLayoutModule, NzCollapseModule
    , NzStepsModule, NzButtonModule, NzTabsModule, NgTerminalModule, NzFormModule, NzInputModule, ReactiveFormsModule, NzSelectModule, NzInputNumberModule
    , ChartsModule, NzDividerModule, NzIconModule, NzTableModule, NzTagModule, NzPopoverModule
  ],
  declarations: [DataxIndexComponent, DataxMainComponent, DataxConfigComponent],
  providers: [
    // {provide: NZ_ICON_DEFAULT_TWOTONE_COLOR, useValue: '#00ff00'}, // 不提供的话，即为 Ant Design 的主题蓝色
    // {provide: NZ_ICONS, useValue: icons}
  ],
  entryComponents: []
})
export class DataxModule implements OnInit {
  ngOnInit(): void {

  }
}
