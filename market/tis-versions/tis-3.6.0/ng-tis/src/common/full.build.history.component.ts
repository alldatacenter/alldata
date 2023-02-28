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

import {ChangeDetectorRef, Component, Input, OnInit} from "@angular/core";
import {TISService} from "./tis.service";
import {BasicFormComponent} from "./basic.form.component";

import {Pager} from "./pagination.component";
import {ActivatedRoute, Params, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {DataXJobWorkerStatus} from "../runtime/misc/RCDeployment";
import {TisResponseResult} from "./tis.plugin";

@Component({
  selector: "full-build-history",
  template: `
      <div style="margin-top: 8px;" *ngIf="dataxProcess && dataXWorkerStatus">
          <nz-alert *ngIf="!dataXWorkerStatus.k8sReplicationControllerCreated" nzType="warning" nzMessage="告知" [nzDescription]="unableToUseK8SController" nzShowIcon></nz-alert>
          <ng-template #unableToUseK8SController>
              当前DataX任务执行默认为本地模式（<strong>单机版</strong>），DataX任务只能串型执行，适合非生产环境中使用。如若要在生产环境中使用建议开启 <a target="_blank" [routerLink]="'/base/datax-worker'">K8S DataX执行器</a>
          </ng-template>
          <nz-alert *ngIf="dataXWorkerStatus.installLocal" nzType="error" nzMessage="警告" [nzDescription]="installLocal" nzShowIcon></nz-alert>
          <ng-template #installLocal>
              当前DataX任务执行器需要安装（<strong>单机版</strong>）执行器，请先安装
              <tis-plugin-add-btn [extendPoint]="'com.qlangtech.tis.datax.DataXJobSubmit'" [descriptors]="[]">添加</tis-plugin-add-btn>
          </ng-template>
      </div>
      <tis-page-header title="构建历史" [showBreadcrumb]="this.showBreadcrumb" [breadcrumb]="breadcrumb" [result]="result" (refesh)="refesh()">
          <tis-page-header-left *ngIf="dataxProcess && dataXWorkerStatus">
              <nz-tag *ngIf="dataXWorkerStatus.k8sReplicationControllerCreated" nzColor="processing">
                  <a target="_blank" [routerLink]="'/base/datax-worker'"><i nz-icon nzType="link" nzTheme="outline"></i>分布式K8S执行</a></nz-tag>
          </tis-page-header-left>
          <button (click)="triggerFullBuild()" [disabled]="formDisabled" nz-button nzType="primary">触发构建</button> &nbsp;
      </tis-page-header>
      <tis-page [rows]="buildHistory" [pager]="pager" (go-page)="gotoPage($event)">
          <tis-col title="ID" width="10">
              <ng-template let-rr="r">
                  <a [routerLink]="['./', rr.id]">#{{rr.id}}</a>
              </ng-template>
          </tis-col>
          <tis-col title="状态" width="10">
              <ng-template let-rr='r'>
                  <i nz-icon [nzType]="rr.stateClass" [ngStyle]="{'color':rr.stateColor}" aria-hidden="true"></i>
                  {{rr.literalState}}
              </ng-template>
          </tis-col>
          <tis-col title="阶段描述" width="24">
              <ng-template let-rr='r'>
                  <nz-tag [nzColor]="'blue'">{{rr.startPhase}}</nz-tag>
                  <i nz-icon nzType="arrow-right" nzTheme="outline"></i>
                  <nz-tag [nzColor]="'blue'">{{rr.endPhase}}</nz-tag>
              </ng-template>
          </tis-col>

          <tis-col title="开始时间" width="12">
              <ng-template let-rr='r'>
                  {{rr.createTime | date : "yyyy/MM/dd HH:mm:ss"}}
              </ng-template>
          </tis-col>

          <tis-col title="耗时" width="12">
              <ng-template let-rr='r'>
                  {{rr.consuming}}
              </ng-template>
          </tis-col>
          <tis-col title="触发方式" width="10">
              <ng-template let-rr='r'>{{rr.triggerType}}</ng-template>
          </tis-col>
      </tis-page>
  `,
  styles: [
      `
          nz-alert {
              margin-top: 5px;
          }
    `
  ]
})
export class FullBuildHistoryComponent extends BasicFormComponent implements OnInit {
  pager: Pager = new Pager(1, 1, 0);
  buildHistory: any[] = [];
  wfid: number;

  breadcrumb: string[];

  showBreadcrumb = false;
  @Input()
  dataxProcess = false;
  dataXWorkerStatus: DataXJobWorkerStatus;

  constructor(tisService: TISService, modalService: NzModalService
    , private router: Router, private route: ActivatedRoute
    , private cd: ChangeDetectorRef
  ) {
    super(tisService, modalService);
    cd.detach();
  }


  ngOnInit(): void {

    let data = this.route.snapshot.data;
    let b = data['showBreadcrumb'];
    let datax = data['datax'];
    if (datax) {
      this.dataxProcess = !!datax;
    }
    this.showBreadcrumb = !!b;


    this.route.params
      .subscribe((params: Params) => {
        this.wfid = parseInt(params['wfid'], 10);

        this.route.queryParams.subscribe((p) => {
          this.httpPost('/coredefine/full_build_history.ajax'
            , `emethod=get_full_build_history&action=core_action&page=${p['page']}&wfid=${this.wfid}&getwf=${!this.breadcrumb}`).then((r) => {
            if (!this.breadcrumb) {
              let wfname = r.bizresult.payload[0];
              this.breadcrumb = ['数据流', '/offline/wf', wfname, `/offline/wf_update/${wfname}`];
            }
            this.pager = Pager.create(r);
            this.buildHistory = r.bizresult.rows;

            if (this.dataxProcess) {
              this.httpPost('/coredefine/corenodemanage.ajax'
                , `action=datax_action&emethod=get_datax_worker_meta&disableRcdeployment=true`).then((rr) => {
                if (rr.success) {
                  this.dataXWorkerStatus = rr.bizresult;
                }
                this.cd.reattach();
              });
            } else {
              this.cd.reattach();
            }
          });
        });
      });
  }

// 刷新列表
  public refesh(): void {
    this.ngOnInit();
  }

  public triggerFullBuild(): void {
    let processStrategy = this.dataxProcess ?
      {
        url: "/coredefine/coredefine.ajax",
        post: "action=datax_action&emethod=trigger_fullbuild_task",
        sucMsg: 'DataX任务已经触发'
      } : {
        url: "/coredefine/coredefine.ajax",
        post: "action=core_action&emethod=trigger_fullbuild_task",
        sucMsg: '全量索引构建已经触发'
      };

    if (this.dataXWorkerStatus) {
      this.dataXWorkerStatus.installLocal = false;
    }

    if (this.appNotAware) {
      // 单纯数据流触发
      processStrategy = {
        url: "/offline/datasource.ajax",
        post: `action=offline_datasource_action&emethod=execute_workflow&id=${this.wfid}`,
        sucMsg: '数据流构建已经触发'
      };
    }
    this.httpPost(processStrategy.url, processStrategy.post).then((r) => {
      if (!r.success) {
        // let p =   <Promise<any>>r;
        return;
      }
      let taskid = r.bizresult.taskid;
      let msg: Array<any> = [];
      msg.push({
        'content': processStrategy.sucMsg
        , 'link': {'content': `查看构建状态(${taskid})`, 'href': './' + taskid}
      });
      this.httpPost("/coredefine/coredefine.ajax", `action=core_action&emethod=get_workflow_build_history&taskid=${taskid}`)
        .then((rr) => {
          this.processResultWithTimeout({'success': true, 'msg': msg}, 10000);
          this.buildHistory = [rr.bizresult].concat(this.buildHistory); // .concat()
        });
    }, (r: TisResponseResult) => {
      if (!r.success && r.bizresult && this.dataXWorkerStatus) {
        this.dataXWorkerStatus.installLocal = r.bizresult.installLocal;
      }
    })

  }

  public gotoPage(p: number) {
    Pager.go(this.router, this.route, p);
  }
}
