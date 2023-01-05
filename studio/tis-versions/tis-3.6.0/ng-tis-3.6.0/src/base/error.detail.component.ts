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

import {Component, Input, OnInit} from '@angular/core';

import {NzModalService} from "ng-zorro-antd/modal";
import {BasicFormComponent, BasicSideBar} from "../common/basic.form.component";
import {TISService} from "../common/tis.service";
import {EditorConfiguration} from "codemirror";
import {NzDrawerRef} from "ng-zorro-antd/drawer";

// 查看操作日志
@Component({
  template: `
      <nz-alert class="tool-bar" *ngIf="showErrlistLink" nzType="info" [nzMessage]="msgTemplate"></nz-alert>
      <ng-template #msgTemplate>
          <a target="_blank" routerLink="/base/sys-errors"> <i nz-icon nzType="link" nzTheme="outline"></i> 异常列表</a>
      </ng-template>
      <tis-codemirror [ngModel]="content" [config]="codeMirrirOpts" [size]="{width:'100%',height:'100%'}"></tis-codemirror>
  `
})
export class ErrorDetailComponent extends BasicFormComponent implements OnInit {
  @Input()
  logFileName: string;

  @Input()
  showErrlistLink = true;

  content = '';

  constructor(tisService: TISService, modalService: NzModalService, private drawerRef: NzDrawerRef<BasicSideBar>) {
    super(tisService, modalService);
  }

  get codeMirrirOpts(): EditorConfiguration {
    return {
      mode: {name: 'shell', alignCDATA: true},
      lineNumbers: true
    };
  }

  ngOnInit(): void {

    this.httpPost('/runtime/operation_detail.ajax'
      , `action=operation_log_action&event_submit_do_get_error_detail=y&logfilename=${this.logFileName}`)
      .then((result) => {
        this.content = result.bizresult;
      });
  }
}
