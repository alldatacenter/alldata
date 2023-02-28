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

import {AfterViewInit, Component, ComponentFactoryResolver, OnInit, ViewChild, ViewContainerRef} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";

import {ActivatedRoute} from "@angular/router";
import {EditorConfiguration} from "codemirror";
import {MultiViewDAG} from "../common/MultiViewDAG";
import {IncrBuildStep1Component, IncrBuildStep1ExecEngineSelectComponent} from "./incr.build.step1.component";
import {IncrBuildStep3Component} from "./incr.build.step3.component";
import {IncrBuildStep4RunningComponent} from "./incr.build.step4.running.component";
import {NzIconService} from 'ng-zorro-antd/icon';
import {CloseSquareFill} from "@ant-design/icons-angular/icons";
import {NzModalService} from "ng-zorro-antd/modal";
import {IndexIncrStatus} from "./misc/RCDeployment";
import {IncrBuildStep0Component} from "./incr.build.step0.component";
import {IncrBuildStep2SetSinkComponent} from "./incr.build.step2.setSink.components";
import {throwError} from "rxjs";
import {IncrBuildStep4StopedComponent} from "./incr.build.step4.stoped.component";
import {IncrBuildStep1ExtendSelectedTabPropsComponent} from "./incr.build.step1.extend.selected.tab.props.component";


@Component({
  template: `
      <nz-spin nzSize="large" [nzSpinning]="formDisabled" style="min-height: 300px">
          <ng-template #container></ng-template>
          {{ multiViewDAG.lastCpt?.name}}
      </nz-spin>`
})
export class IncrBuildComponent extends AppFormComponent implements AfterViewInit, OnInit {

  private _incrScript: string;
  @ViewChild('container', {read: ViewContainerRef, static: true}) containerRef: ViewContainerRef;

  multiViewDAG: MultiViewDAG;

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService
    , private _componentFactoryResolver: ComponentFactoryResolver, private _iconService: NzIconService) {
    super(tisService, route, modalService);
    _iconService.addIcon(CloseSquareFill);
  }

  protected initialize(app: CurrentCollection): void {
  }

  ngAfterViewInit() {
  }

  ngOnInit(): void {
    // 配置步骤前后跳转状态机
    let configFST: Map<any, { next: any, pre: any }> = new Map();
    configFST.set(IncrBuildStep0Component, {next: IncrBuildStep1ExecEngineSelectComponent, pre: null});
    configFST.set(IncrBuildStep1ExecEngineSelectComponent, {next: IncrBuildStep1Component, pre: IncrBuildStep0Component});
    configFST.set(IncrBuildStep1Component, {next: IncrBuildStep2SetSinkComponent, pre: IncrBuildStep1ExecEngineSelectComponent});
    configFST.set(IncrBuildStep1ExtendSelectedTabPropsComponent, {next: IncrBuildStep2SetSinkComponent, pre: IncrBuildStep1Component});
    configFST.set(IncrBuildStep2SetSinkComponent, {next: IncrBuildStep3Component, pre: IncrBuildStep1Component});
    configFST.set(IncrBuildStep3Component, {next: IncrBuildStep4RunningComponent, pre: IncrBuildStep2SetSinkComponent});
    configFST.set(IncrBuildStep4RunningComponent, {next: IncrBuildStep0Component, pre: IncrBuildStep3Component});
    // configFST.set(IncrBuildStep4StopedComponent, {next: IncrBuildStep0Component, pre: IncrBuildStep3Component});
    this.multiViewDAG = new MultiViewDAG(configFST, this._componentFactoryResolver, this.containerRef);

    // this.route.params.subscribe((_) => {
    //    console.log("xxxxxx");
    // });

    //  this.multiViewDAG.loadComponent(IncrBuildStep1Component, null);
    IndexIncrStatus.getIncrStatusThenEnter(this, (incrStatus) => {
      let k8sRCCreated = incrStatus.k8sReplicationControllerCreated;
      // console.log(incrStatus.flinkJobDetail);
      switch (incrStatus.state) {
        case "NONE":
          // 脚本还未创建
          this.multiViewDAG.loadComponent(IncrBuildStep0Component, null);
          break;
        case "DISAPPEAR":
        case "STOPED":
        case "RUNNING":
          // 增量已经在集群中运行，显示增量状态
          this.multiViewDAG.loadComponent(IncrBuildStep4RunningComponent, incrStatus);
          break;
        // this.multiViewDAG.loadComponent(IncrBuildStep4StopedComponent, incrStatus);
        // break;
        default:
          throw new Error("illegal state:" + incrStatus.state);
      }

      // if (k8sRCCreated) {
      //
      // } else {
      //
      // }
      this.incrScript = incrStatus.incrScriptMainFileContent;
    });
  }


  get incrScript(): string {
    return this._incrScript;
  }

  set incrScript(value: string) {
    this._incrScript = value;
  }

}

