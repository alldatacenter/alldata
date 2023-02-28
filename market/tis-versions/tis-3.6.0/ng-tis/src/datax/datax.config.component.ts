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

import {ChangeDetectionStrategy, Component, OnInit} from '@angular/core';
import {TISService} from '../common/tis.service';
import {ActivatedRoute, Router} from '@angular/router';
import {AppFormComponent, BasicFormComponent, CurrentCollection} from '../common/basic.form.component';

import {NzModalService} from "ng-zorro-antd/modal";
import {DataxAddComponent, DataxDTO} from "../base/datax.add.component";
import {ExecModel} from "../base/datax.add.step7.confirm.component";
import {PluginsComponent} from "../common/plugins.component";
import {Descriptor} from "../common/tis.plugin";

// import {ExecModel} from "../base/datax.add.step7.confirm.component";


@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  template: `
      <!--      <tis-plugins [errorsPageShow]="false"-->
      <!--                   [formControlSpan]="20" [shallInitializePluginItems]="false" [showSaveButton]="false" [disabled]="true"-->
      <!--                   [plugins]="[{name: 'dataxReader', require: true, extraParam: 'justGetItemRelevant_true,dataxName_' + this.dto.dataxPipeName}]"></tis-plugins>-->
      <datax-config *ngIf="dto"  [dtoooo]="dto" [execModel]="execModel"></datax-config>
  `,
  styles: [`
  `]
})
export class DataxConfigComponent extends AppFormComponent implements OnInit {

  public dto: DataxDTO = null;



  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService, private router: Router) {
    super(tisService, route, modalService);
  }

  protected initialize(app: CurrentCollection): void {
    DataxAddComponent.getDataXMeta(this, app).then((dto) => {
      // console.log(dto);
      this.dto = dto;
    });
  }


  get execModel(): ExecModel {
    return ExecModel.Reader;
  }
}
