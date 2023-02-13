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

import {Component, OnInit} from "@angular/core";
import {BasicFormComponent} from "./basic.form.component";
import {TISService} from "./tis.service";
import {NzModalRef} from "ng-zorro-antd/modal";

@Component({
  template: `
      <nz-alert nzType="info" nzMessage="初次使用TIS，系统需要对相关配置进行初始化" nzShowIcon></nz-alert>
      <tis-page-header [result]="this.result" [showBreadcrumb]="false">
          <button nz-button nzType="primary" (click)="startInitialize()" [nzLoading]="_startInitialize">开始初始化</button>
      </tis-page-header>
      <nz-progress *ngIf="_startInitialize" [nzPercent]="_percent" nzStatus="active"></nz-progress>
  `
})
export class InitSystemComponent extends BasicFormComponent implements OnInit {
  _startInitialize = false;
  _percent = 0;

  constructor(tisService: TISService, private activeModal: NzModalRef) {
    super(tisService);
  }

  ngOnInit(): void {
  }

  startInitialize() {

    this._startInitialize = true;
    let timer = setInterval(() => {
      if (this._percent >= 100) {
        clearInterval(timer);
        return;
      }
      this._percent += 2;
    }, 500);
    let url = '/runtime/applist.ajax?action=sys_initialize_action&emethod=init';
    this.httpPost(url, '').then((r) => {
      clearInterval(timer);
      if (r.success) {
        this.activeModal.close(r);
      } else {
        this.processResult(r);
      }
    });
  }
}
