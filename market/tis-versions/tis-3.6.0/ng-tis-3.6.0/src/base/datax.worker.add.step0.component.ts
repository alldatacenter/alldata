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

import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {TISService} from "../common/tis.service";
import {BasicFormComponent, CurrentCollection} from "../common/basic.form.component";
import {NzModalService} from "ng-zorro-antd/modal";

import {DataxWorkerDTO} from "../runtime/misc/RCDeployment";

@Component({
  template: `
      <nz-empty style="height: 500px"
                nzNotFoundImage="https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg"
                [nzNotFoundFooter]="footerTpl"
                [nzNotFoundContent]="contentTpl"
      >
          <ng-template #contentTpl>
              <span>{{this.dto.processMeta.notCreateTips}}</span>
          </ng-template>
          <ng-template #footerTpl>
              <button nz-button nzType="primary" (click)="onClick()">{{this.dto.processMeta.createButtonLabel}}</button>
          </ng-template>
      </nz-empty>
  `
})
export class DataxWorkerAddStep0Component extends BasicFormComponent implements AfterViewInit, OnInit {

  @Input() dto: DataxWorkerDTO;
  @Output() nextStep = new EventEmitter<any>();

  constructor(tisService: TISService, modalService: NzModalService) {
    super(tisService, modalService);
  }

  protected initialize(app: CurrentCollection): void {
  }

  ngAfterViewInit() {
  }


  ngOnInit(): void {

  }

  onClick() {
    this.nextStep.emit(this.dto);
  }
}

