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

import {AfterContentInit, AfterViewInit, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";

import {ActivatedRoute, Router} from "@angular/router";
import {IncrBuildComponent} from "./incr.build.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {IndexIncrStatus} from "./misc/RCDeployment";


@Component({
  template: `
      <tis-steps type="createIncr" [step]="3"></tis-steps>
      <tis-page-header [showBreadcrumb]="false" [result]="result">
          <tis-header-tool>
              <!--
                   <button nz-button nzType="default" (click)="createIndexStepPre()">上一步</button>
                   <button nz-button nzType="primary" (click)="createIndexStepNext()">回到首页</button>
                   <button nz-button nzType="default" (click)="cancelStep()">取消</button>
              -->
          </tis-header-tool>
      </tis-page-header>
      <nz-result
              nzStatus="success"
              [nzTitle]="'已经成功为'+this.currentApp.name+'创建增量通道'"
              nzSubTitle="接下来请进入增量通道管理页面"
      >
          <div nz-result-extra>
              <button nz-button [nzLoading]="formDisabled" nzType="primary" (click)="gotoManage()">进入</button>
          </div>
      </nz-result>
  `
})
export class IncrBuildStep3Component extends AppFormComponent implements AfterContentInit, AfterViewInit {
  @Output() nextStep = new EventEmitter<any>();
  @Output() preStep = new EventEmitter<any>();
  @Input() dto: IndexIncrStatus;
  private currCollection: CurrentCollection;

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService, private router: Router) {
    super(tisService, route, modalService);
  }

  protected initialize(app: CurrentCollection): void {
    this.currCollection = app;
  }

  ngAfterViewInit(): void {
  }

  ngAfterContentInit(): void {
  }

  public createIndexStepPre() {
    this.preStep.emit(this.dto);
  }

  createIncrSyncChannal() {

  }

  createIndexStepNext() {
    this.nextStep.emit(this.dto);
  }

  cancelStep() {
  }

  gotoManage() {
    // this.router.navigate(["."], {relativeTo: this.route});

    IndexIncrStatus.getIncrStatusThenEnter(this, (incrStatus) => {
      let k8sRCCreated = incrStatus.k8sReplicationControllerCreated;
      if (k8sRCCreated) {
        this.nextStep.emit(incrStatus);
      }
    });
  }
}
