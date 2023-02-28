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

import {AfterContentInit, Component, EventEmitter, OnDestroy, OnInit, Output, ViewChild} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, CurrentCollection, WSMessage} from "../common/basic.form.component";
import {ActivatedRoute, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {Subject} from "rxjs";
import {map} from "rxjs/operators";
import {IndexIncrStatus, K8sPodState, LogType} from "./misc/RCDeployment";
import {TisResponseResult} from "../common/tis.plugin";

@Component({
  template: `
      <nz-spin size="large" [nzSpinning]="this.formDisabled">

          <div style="margin-top: 8px;" *ngIf="true">
              <nz-alert *ngIf="true" nzType="info" [nzDescription]="unableToUseK8SController" nzShowIcon></nz-alert>
              <ng-template #unableToUseK8SController>
                  可直接打开Flink控制台<a target="_blank" [href]="this.dto.flinkJobDetail.clusterCfg.jobManagerAddress.uRL+'/#/job/'+this.dto.flinkJobDetail.jobId+'/overview'"><i nz-icon nzType="link" nzTheme="outline"></i>控制台</a>
              </ng-template>
          </div>
          <nz-tabset [nzTabBarExtraContent]="extraTemplate" nzSize="large" [(nzSelectedIndex)]="tabSelectIndex">
              <nz-tab nzTitle="基本">
                  <ng-template nz-tab [ngSwitch]="this.dto.state === 'DISAPPEAR'">
                      <!--                      <nz-alert *ngIf="this.dto.incrProcessLaunchHasError" nzType="error" [nzDescription]="errorTpl" nzShowIcon></nz-alert>-->
                      <!--                      <ng-template #errorTpl>-->
                      <!--                          增量处理节点启动有误-->
                      <!--                          <button nz-button nzType="link" (click)="tabSelectIndex=2">查看启动日志</button>-->
                      <!--                      </ng-template>-->
                      <!--                      <incr-build-step4-running-tab-base [msgSubject]="msgSubject" [dto]="dto"></incr-build-step4-running-tab-base>-->

                      <ng-container *ngSwitchCase="true">
                          <nz-alert class="alter-notice"
                                    nzType="warning"
                                    nzMessage="异常状态"
                                    [nzDescription]="alterNotice"
                                    nzShowIcon
                          ></nz-alert>
                          <ng-template #alterNotice>服务端获取不到该Job状态信息，可能是因为Flink-Cluster重启导致，请手动
                              <button (click)="route2SavepointTab()" nz-button nzSize="small" nzType="primary"><i nz-icon nzType="rollback" nzTheme="outline"></i>恢复</button>
                          </ng-template>
                      </ng-container>
                      <ng-container *ngSwitchCase="false">

                          <nz-descriptions style="margin-left: 10px" [nzTitle]="descTitle" [nzExtra]="extraTpl">
                              <nz-descriptions-item nzTitle="ID">{{this.dto.flinkJobDetail.jobId}}</nz-descriptions-item>
                              <nz-descriptions-item nzTitle="Start Time">{{this.dto.flinkJobDetail.startTime | date : "yyyy/MM/dd HH:mm:ss"}}</nz-descriptions-item>
                              <nz-descriptions-item nzTitle="End Time">{{this.dto.flinkJobDetail.endTime | date : "yyyy/MM/dd HH:mm:ss"}}</nz-descriptions-item>
                              <nz-descriptions-item nzTitle="Duration">{{this.dto.flinkJobDetail.duration | timeconsume}}</nz-descriptions-item>
                          </nz-descriptions>
                          <ng-template #descTitle>
                              <nz-space [nzSplit]="spaceSplit">
                                  <ng-template #spaceSplit>
                                      <nz-divider nzType="vertical"></nz-divider>
                                  </ng-template>
                                  <span *nzSpaceItem>{{this.dto.flinkJobDetail.name}}
                                  </span>
                                  <span *nzSpaceItem>
                              <nz-tag [nzColor]="this.dto.flinkJobDetail.statusColor">
                                  <i *ngIf="this.dto.flinkJobDetail.statusColor === 'processing'" nz-icon nzType="sync" nzSpin></i> {{this.dto.flinkJobDetail.jobStatus}}</nz-tag>
                              <button *ngIf="dto.state === 'STOPED'" (click)="route2SavepointTab()" nz-button nzSize="small" nzType="primary"><i nz-icon nzType="rollback" nzTheme="outline"></i>恢复</button>
                              </span>
                                  <span *nzSpaceItem>
                                  <nz-tag style="margin: 0" *ngFor="let s of this.dto.flinkJobDetail.jobVerticesPerState" [nzColor]="s.stateColor">{{s.count}}</nz-tag>
                              </span>
                              </nz-space>
                          </ng-template>
                          <ng-template #extraTpl>
                              <button *ngIf="this.dto.flinkJobDetail.cancelable" nz-button (click)="manageChannel()"><i nz-icon nzType="setting" nzTheme="outline"></i>操作</button>
                          </ng-template>
                          <tis-page [rows]="this.dto.flinkJobDetail.sources" [showPagination]="false">
                              <tis-col title="Name" width="20">
                                  <ng-template let-rr="r">
                                      <a target="_blank" nz-tooltip [nzTooltipTitle]="rr.fullName" nzOverlayClassName="tooltip-pree"
                                         [href]="this.dto.flinkJobDetail.clusterCfg.jobManagerAddress.uRL +'/#/job/'+ this.dto.flinkJobDetail.jobId +'/overview/'+ rr.jobVertexId +'/detail'">{{rr.name}}</a>
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Status" width="10">
                                  <ng-template let-rr="r">
                                      <nz-tag [nzColor]="rr.executionStateColor"><i *ngIf="rr.executionStateColor === 'processing'" nz-icon nzType="sync" nzSpin></i>{{rr.executionState}}</nz-tag>
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Bytes Received" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.jobVertexMetrics.bytesRead}}B
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Records Received" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.jobVertexMetrics.recordsRead}}
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Bytes Sent" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.jobVertexMetrics.bytesWritten}}
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Records Sent" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.jobVertexMetrics.recordsWritten}}
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Parallelism" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.parallelism}}
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Start Time" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.startTime | date : "yyyy/MM/dd HH:mm:ss"}}
                                  </ng-template>
                              </tis-col>
                              <tis-col title="Duration" width="10">
                                  <ng-template let-rr="r">
                                      {{rr.duration | timeconsume}}
                                  </ng-template>
                              </tis-col>
                              <tis-col title="End Time" width="10">
                                  <ng-template let-rr="r">
                                      <ng-container [ngSwitch]="rr.endTime > 0">
                                      <span *ngSwitchCase="true">
                                       {{rr.endTime | date : "yyyy/MM/dd HH:mm:ss"}}
                                      </span>
                                      </ng-container>
                                  </ng-template>
                              </tis-col>
                          </tis-page>
                      </ng-container>
                  </ng-template>
              </nz-tab>
              <nz-tab nzTitle="配置">
                  <ng-template nz-tab>
                      <h3>基本信息</h3>
                      <div class="item-block">
                          <tis-plugins [disabled]="true" [errorsPageShow]="false" [shallInitializePluginItems]="false" [plugins]="[{name: 'incr-config', require: true}]"></tis-plugins>
                      </div>
                      <h3>Source/Sink信息</h3>
                      <div class="item-block">
                          <tis-plugins [disabled]="true" [errorsPageShow]="false" [shallInitializePluginItems]="false" [plugins]="[{    name: 'mq', require: true  }, {    name: 'sinkFactory', require: true  }]"></tis-plugins>
                      </div>
                  </ng-template>
              </nz-tab>
              <nz-tab [nzTitle]="settingTemplate">
                  <ng-template #settingTemplate>
                      <i nz-icon nzType="setting" nzTheme="outline"></i>操作
                  </ng-template>

                  <nz-page-header class="danger-control-title" nzTitle="一般操作">
                  </nz-page-header>

                  <nz-list class="ant-advanced-search-form ant-advanced-search-form-normal" nzBordered>
                      <nz-list-item>
                          <span nz-typography>停止增量实例 <em class="typography-desc">停止过程中会记录当前任务的savepoint，以便重启之用</em></span>
                          <button nz-button nzType="primary" [disabled]="dto.state !== 'RUNNING'" (click)="incrChannelStop()"><i nz-icon nzType="stop" nzTheme="outline"></i>停止</button>
                      </nz-list-item>
                  </nz-list>
                  <nz-page-header class="danger-control-title" nzTitle="危险操作" nzSubtitle="以下操作可能造成某些组件功能不可用">
                  </nz-page-header>

                  <nz-list class="ant-advanced-search-form ant-advanced-search-form-danger" nzBordered>
                      <nz-list-item>
                          <span nz-typography>删除增量实例</span>
                          <button nz-button nzType="primary" nzDanger (click)="incrChannelDelete()"><i nz-icon nzType="delete" nzTheme="outline"></i>删除</button>
                      </nz-list-item>
                  </nz-list>
              </nz-tab>
              <nz-tab nzTitle="Savepoint">
                  <ng-template nz-tab>
                      <incr-build-step4-running-savepoint (afterRelaunch)="afterRelaunch($event)" [dto]="this.dto"></incr-build-step4-running-savepoint>
                  </ng-template>
              </nz-tab>
          </nz-tabset>
          <ng-template #extraTemplate>
              <!--
               <button nz-button nz-dropdown [nzDropdownMenu]="menu4">
                   操作
                   <i nz-icon nzType="down"></i>
               </button>
               <nz-dropdown-menu #menu4="nzDropdownMenu">
                   <ul nz-menu>
                       <li nz-menu-item><i nz-icon nzType="delete" nzTheme="outline"></i>删除</li>
                   </ul>
               </nz-dropdown-menu>
              -->
          </ng-template>
      </nz-spin>
  `,
  styles: [
      `
          .pods {
              margin-top: 12px;
          }

          nz-descriptions {
              margin-top: 15px;
          }

          nz-tab {
              padding-left: 10px;
          }

          .danger-control-title {
              margin-top: 10px;
              padding: 0px 0;
          }

          .ant-advanced-search-form {
              padding: 10px;
              #background: #fbfbfb;
              border: 2px solid;
              border-radius: 6px;
              margin-bottom: 10px;
              clear: both;
          }

          .ant-advanced-search-form-danger {
              border-color: #d97f85;
          }

          .ant-advanced-search-form-normal {
              border-color: #91d5ff;
          }

          .typography-desc {
              font-size: 10px;
              color: #999999;
          }

          [nz-row] {
              margin-bottom: 10px;
          }
    `
  ]
})
export class IncrBuildStep4RunningComponent extends AppFormComponent implements AfterContentInit, OnDestroy {
  private componentDestroy: boolean;
  tabSelectIndex = 0;
  @Output() nextStep = new EventEmitter<any>();
  @Output() preStep = new EventEmitter<any>();
  dto: IndexIncrStatus = new IndexIncrStatus();
  msgSubject: Subject<WSMessage>;

  // 实时流量配置
  constructor(tisService: TISService, route: ActivatedRoute, private router: Router, modalService: NzModalService, notification: NzNotificationService) {
    super(tisService, route, modalService, notification);
  }

  protected initialize(app: CurrentCollection): void {
  }

  ngAfterContentInit(): void {
  }


  ngOnInit(): void {
    super.ngOnInit();
    this.route.fragment.subscribe((r) => {
      if (r === 'podlog') {
        this.tabSelectIndex = 2;

        let firstPod = this.dto.getFirstPod();
        if (firstPod) {
          this.startMonitorMqTagsStatus('incrdeploy-change:' + firstPod.name);
        } else {
          throw  new Error("have not found any pod");
        }
      } else {
        // this.startMonitorMqTagsStatus('mq_tags_status');
      }
    })
  }

  public startMonitorMqTagsStatus(logtype: string) {
    // console.log(this.currentApp);
    this.msgSubject = this.getWSMsgSubject(logtype);
  }


  ngOnDestroy(): void {
    this.componentDestroy = true;
    if (this.msgSubject) {
      this.msgSubject.unsubscribe()
    }
  }

  incrChannelStop() {
    this.modalService.confirm({
      nzTitle: '停止',
      nzContent: `是否要停止增量实例'${this.currentApp.appName}'`,
      nzOkText: '执行',
      nzCancelText: '取消',
      nzOnOk: () => {
        this.httpPost('/coredefine/corenodemanage.ajax', "event_submit_do_incr_stop=y&action=core_action").then((r) => {
          if (r.success) {
            this.successNotify(`已经成功停止增量实例${this.currentApp.appName}`);
            //  this.router.navigate(["."], {relativeTo: this.route});
            // this.nextStep.next(this.dto);
            // IndexIncrStatus.getIncrStatusThenEnter(this, (incrStatus) => {
            this.dto = r.bizresult;
            this.tabSelectIndex = 0;
            // });
          }
        });
      }
    });
  }

  /**
   * 删除增量通道
   */
  incrChannelDelete() {
    this.modalService.confirm({
      nzTitle: '删除',
      nzContent: `是否要删除增量实例'${this.currentApp.appName}'`,
      nzOkText: '执行',
      nzCancelText: '取消',
      nzOnOk: () => {
        this.httpPost('/coredefine/corenodemanage.ajax', "event_submit_do_incr_delete=y&action=core_action").then((r) => {
          if (r.success) {
            this.successNotify(`已经成功删除增量实例${this.currentApp.appName}`);
            //  this.router.navigate(["."], {relativeTo: this.route});
            this.nextStep.next(this.dto);
          }
        });
      }
    });
  }

  manageChannel() {
    this.tabSelectIndex = 2;
  }


  route2SavepointTab() {
    this.tabSelectIndex = 3;
  }

  afterRelaunch(result: TisResponseResult) {
    if (result.success) {
      // this.router.initialNavigation()
      // this.router.navigate([], {relativeTo: this.route, replaceUrl: true});
      //  IndexIncrStatus.getIncrStatusThenEnter(this, (incrStatus) => {
      this.dto = result.bizresult;
      this.tabSelectIndex = 0;
      // });
    }
  }
}

interface TisIncrStatus {
  summary: IncrSummary;
  tags: Array<TagState>;
}

interface TagState {
  tag: string;
  trantransferIncr: number;
}

interface IncrSummary {
  solrConsume: number;
  tableConsumeCount: number;
}
