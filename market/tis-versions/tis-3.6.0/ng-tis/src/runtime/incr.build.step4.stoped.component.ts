import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";
import {AfterContentInit, Component, EventEmitter, OnDestroy, Output, Input} from "@angular/core";
import {TISService} from "../common/tis.service";
import {ActivatedRoute, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {Pager} from "../common/pagination.component";
import {IndexIncrStatus} from "./misc/RCDeployment";
import FlinkSavepoint = flink.job.detail.FlinkSavepoint;
import {TisResponseResult} from "../common/tis.plugin";

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


@Component({
  selector: "incr-build-step4-running-savepoint",
  template: `
      <nz-spin size="large" [nzSpinning]="this.formDisabled">
          <tis-page-header [showBreadcrumb]="false">
              <button nz-button nzType="primary" (click)="createNewSavepoint()" [disabled]="dto.state !== 'RUNNING' ">创建新记录</button>
          </tis-page-header>
          <tis-page [rows]="savepoints" [pager]="pager" (go-page)="gotoPage($event)">
              <tis-col title="SavePoint" width="80">
                  <ng-template let-rr="r">
                      <dl class="sp-info">
                          <dt><i nz-icon nzType="file-text" nzTheme="outline"></i> 路径</dt>
                          <dd> {{rr.path}}</dd>
                          <dt>创建时间</dt>
                          <dd> {{rr.createTimestamp | date:'yyyy/MM/dd HH:mm:ss'}} </dd>
                      </dl>
                  </ng-template>
              </tis-col>
              <tis-col title="操作">
                  <ng-template let-rr='r'>
                      <button nz-button nz-dropdown [nzDropdownMenu]="menu">
                          操作
                          <i nz-icon nzType="down"></i>
                      </button>
                      <nz-dropdown-menu #menu="nzDropdownMenu">
                          <ul nz-menu>
                              <li nz-menu-item  >
                                  <button  nz-button nzType="link" [disabled]="dto.state !== 'STOPED' && dto.state !== 'DISAPPEAR' " (click)="relaunchJob(rr)"><i nz-icon nzType="rollback" nzTheme="outline"></i>恢复任务</button>
                              </li>
                              <li nz-menu-item>
                                  <button  nz-button nzType="link" [disabled]="dto.state !== 'RUNNING'" (click)="discardSavePoint(rr)"><i nz-icon nzType="delete" nzTheme="outline"></i>废弃</button>
                              </li>
                          </ul>
                      </nz-dropdown-menu>
                  </ng-template>
              </tis-col>
          </tis-page>
      </nz-spin>
  `,
  styles: [
      `
          .sp-info dt {
              font-weight: bold;
          }

          .sp-info dd {
              color: #989898;
          }
    `
  ]
})
export class IncrBuildStep4StopedComponent extends AppFormComponent implements AfterContentInit, OnDestroy {

  pager: Pager = new Pager(1, 1, 0);
  @Input()
  dto: IndexIncrStatus = new IndexIncrStatus();
  savepoints: any[] = [];
  @Output() afterRelaunch = new EventEmitter<TisResponseResult>();

  constructor(tisService: TISService, private router: Router, route: ActivatedRoute, modalService: NzModalService, notification: NzNotificationService) {
    super(tisService, route, modalService, notification);
  }

  protected initialize(app: CurrentCollection): void {
    this.savepoints = this.dto.flinkJobDetail.incrJobStatus.savepointPaths;
  }

  relaunchJob(sp: FlinkSavepoint) {
    this.modalService.confirm({
      nzTitle: '恢复任务',
      nzContent: `是否要恢复增量实例'${this.currentApp.appName}'`,
      nzOkText: '执行',
      nzCancelText: '取消',
      nzOnOk: () => {
        this.httpPost('/coredefine/corenodemanage.ajax'
          , "event_submit_do_relaunch_incr_process=y&action=core_action&savepointPath=" + sp.path).then((r) => {
          if (r.success) {
            this.successNotify(`已经成功恢复增量实例${this.currentApp.appName}`);
            //  this.router.navigate(["."], {relativeTo: this.route});
            // this.nextStep.next(this.dto);
            this.afterRelaunch.emit(r);
          }
        });
      }
    });
  }

  discardSavePoint(sp: FlinkSavepoint) {
    this.modalService.confirm({
      nzTitle: '删除Savepoint',
      nzContent: `是否要删除 该路径下的Savepoint：'${sp.path}'`,
      nzOkText: '执行',
      nzCancelText: '取消',
      nzOnOk: () => {
        this.httpPost('/coredefine/corenodemanage.ajax'
          , "event_submit_do_discard_savepoint=y&action=core_action&savepointPath=" + sp.path).then((r) => {
          if (r.success) {
            this.dto = Object.assign(new IndexIncrStatus(), r.bizresult);
            this.initialize(null);
            this.successNotify(`已经成功删除Savepoint${sp.path}`);
            //  this.router.navigate(["."], {relativeTo: this.route});
            // this.nextStep.next(this.dto);
           // this.afterRelaunch.emit(r);
          }
        });
      }
    });
  }

  public gotoPage(p: number) {
    Pager.go(this.router, this.route, p);
  }

  ngAfterContentInit(): void {
  }

  ngOnDestroy(): void {
  }


  createNewSavepoint() {
    this.modalService.confirm({
      nzTitle: '创建最新Savepoint',
      nzContent: `是否要为'${this.currentApp.appName}'创建Savepoint`,
      nzOkText: '执行',
      nzCancelText: '取消',
      nzOnOk: () => {
        this.httpPost('/coredefine/corenodemanage.ajax'
          , "event_submit_do_create_new_savepoint=y&action=core_action").then((r) => {
          if (r.success) {
            this.dto = Object.assign(new IndexIncrStatus(), r.bizresult);
            this.initialize(null);
            this.successNotify(`已经成功为${this.currentApp.appName}创建Savepoint`);
          }
        });
      }
    });
  }


}
