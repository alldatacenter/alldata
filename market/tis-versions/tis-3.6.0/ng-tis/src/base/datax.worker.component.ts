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

import {AfterViewInit, Component, ComponentFactoryResolver, Input, OnInit, ViewChild, ViewContainerRef} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";

import {ActivatedRoute} from "@angular/router";

import {MultiViewDAG} from "../common/MultiViewDAG";
import {NzModalService} from "ng-zorro-antd/modal";
import {DataxWorkerAddStep0Component} from "./datax.worker.add.step0.component";
import {DataxWorkerAddStep1Component} from "./datax.worker.add.step1.component";
import {DataxWorkerAddStep2Component} from "./datax.worker.add.step2.component";
import {DataxWorkerAddStep3Component} from "./datax.worker.add.step3.component";
import {DataxWorkerRunningComponent} from "./datax.worker.running.component";
import {DataXJobWorkerStatus, DataxWorkerDTO, ProcessMeta} from "../runtime/misc/RCDeployment";

@Component({
  template: `
      <tis-page-header [title]="this.processMeta.pageHeader">
      </tis-page-header>
      <nz-spin nzSize="large" [nzSpinning]="formDisabled" style="min-height: 300px">
          <ng-template #container></ng-template>
      </nz-spin>
      {{ multiViewDAG.lastCpt?.name}}
  `
})
export class DataxWorkerComponent extends AppFormComponent implements AfterViewInit, OnInit {
  @ViewChild('container', {read: ViewContainerRef, static: true}) containerRef: ViewContainerRef;

  multiViewDAG: MultiViewDAG;
  processMeta: ProcessMeta;

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService
    , private _componentFactoryResolver: ComponentFactoryResolver) {
    super(tisService, route, modalService);
  }

  protected initialize(app: CurrentCollection): void {
  }

  ngAfterViewInit() {
  }


  ngOnInit(): void {
    this.processMeta = this.route.snapshot.data["processMeta"];
    // 配置步骤前后跳转状态机
    let configFST: Map<any, { next: any, pre: any }> = new Map();
    configFST.set(DataxWorkerAddStep0Component, {next: DataxWorkerAddStep1Component, pre: null});
    if (this.processMeta.supportK8SReplicsSpecSetter) {
      configFST.set(DataxWorkerAddStep1Component, {next: DataxWorkerAddStep2Component, pre: DataxWorkerAddStep0Component});
      configFST.set(DataxWorkerAddStep2Component, {next: DataxWorkerAddStep3Component, pre: DataxWorkerAddStep1Component});
      configFST.set(DataxWorkerAddStep3Component, {next: DataxWorkerRunningComponent, pre: DataxWorkerAddStep2Component});
    } else {
      configFST.set(DataxWorkerAddStep1Component, {next: DataxWorkerAddStep3Component, pre: DataxWorkerAddStep0Component});
      configFST.set(DataxWorkerAddStep3Component, {next: DataxWorkerRunningComponent, pre: DataxWorkerAddStep1Component});
    }

    configFST.set(DataxWorkerRunningComponent, {next: DataxWorkerAddStep0Component, pre: DataxWorkerAddStep3Component});

    this.multiViewDAG = new MultiViewDAG(configFST, this._componentFactoryResolver, this.containerRef);
    this.httpPost('/coredefine/corenodemanage.ajax'
      , `action=datax_action&emethod=get_job_worker_meta&targetName=${this.processMeta.targetName}`)
      .then((r) => {
        if (r.success) {
          let dataXWorkerStatus: DataXJobWorkerStatus = Object.assign(new DataXJobWorkerStatus(), r.bizresult, {processMeta: this.processMeta});
          if (dataXWorkerStatus.k8sReplicationControllerCreated) {
            this.multiViewDAG.loadComponent(DataxWorkerRunningComponent, dataXWorkerStatus);
          } else {
            this.multiViewDAG.loadComponent(DataxWorkerAddStep0Component, Object.assign(new DataxWorkerDTO(), {processMeta: this.processMeta}));
          }
        }
      });
  }
}





