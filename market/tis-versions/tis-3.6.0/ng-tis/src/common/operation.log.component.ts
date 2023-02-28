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

import {Component, OnInit} from '@angular/core';
import {TISService} from './tis.service';
import {BasicFormComponent} from './basic.form.component';

import {Pager} from "./pagination.component";
import {ActivatedRoute, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";

// 查看操作日志
@Component({
  template: `

      <tis-page-header title="操作日志" [showBreadcrumb]="showBreadcrumb">
      </tis-page-header>

      <tis-page [spinning]="formDisabled" [pager]="pager" [rows]="logs" (go-page)="goPage($event)">
          <tis-col title="操作者" width="14" field="usrName"></tis-col>
          <tis-col title="操作对象" width="30">
              <ng-template let-l='r'>{{l.tabName}}#{{l.opType}}</ng-template>
          </tis-col>
          <tis-col *ngIf="showBreadcrumb" title="应用" field="appName" >
          </tis-col>
          <tis-col title="创建时间">
              <ng-template let-l='r'>{{l.createTime | date : "yyyy/MM/dd HH:mm:ss"}}</ng-template>
          </tis-col>
          <tis-col title="操作">
              <ng-template let-l='r'>
                  <button nz-button [nzType]="'link'" (click)="operationDetail(l.opId)"><i nz-icon nzType="eye" nzTheme="outline"></i></button>
              </ng-template>
          </tis-col>
      </tis-page>
      <nz-drawer
              [nzBodyStyle]="{ height: 'calc(100% - 55px)', overflow: 'auto', 'padding-bottom': '53px' }"
              [nzMaskClosable]="true"
              [nzWidth]="'40%'"
              [nzVisible]="logVisible"
              nzTitle="日志"
              (nzOnClose)="logViewClose()"
      >
          <pre *nzDrawerContent style="word-wrap:break-word;white-space: pre-wrap;">{{detail}}</pre>
      </nz-drawer>
  `
})
export class OperationLogComponent extends BasicFormComponent implements OnInit {
  logs: any[] = [];
  private detailLog: string;
  pager: Pager = new Pager(1, 1);
  logVisible: boolean;
  showBreadcrumb: boolean;

  constructor(tisService: TISService, modalService: NzModalService, private router: Router, private route: ActivatedRoute) {
    super(tisService, modalService);
  }


  ngOnInit(): void {
    // showBreadcrumb
    let sn = this.route.snapshot;
    this.showBreadcrumb = sn.data["showBreadcrumb"];
    this.route.queryParams.subscribe((param) => {
      this.httpPost('/runtime/operation_log.ajax'
        , `action=operation_log_action&emethod=get_init_data&page=${param['page']}`)
        .then((r) => {
          this.pager = Pager.create(r);
          this.logs = r.bizresult.rows;
        });
    });
  }

  public get showDetail(): boolean {
    return this.detail != null;
  }


  // 显示详细信息
  public operationDetail(opId: number): void {
    this.httpPost(
      '/runtime/operation_detail.ajax?action=operation_log_action&event_submit_do_get_detail=y&opid=' + opId, '')
      .then(result => {
        this.detailLog = result.bizresult.opDesc;
        this.logVisible = true;
      });
  }

  public get detail(): string {
    return this.detailLog;
  }

  goPage(pageNum: number) {
    Pager.go(this.router, this.route, pageNum);
  }

  logViewClose() {
    this.logVisible = false;
    this.detailLog = null;
  }
}
