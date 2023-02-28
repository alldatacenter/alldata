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

import {AfterContentInit, Component, ContentChildren, EventEmitter, Input, OnInit, Output, QueryList} from "@angular/core";
import {NzModalService} from "ng-zorro-antd/modal";
import {PageHeaderLeftComponent} from "./pager.header.component";
import {TisResponseResult} from "./tis.plugin";


// const typeCreateIndex = "createIndex";

export enum StepType {
  CreateIndex = "createIndex",
  CreateIncr = "createIncr",
  CreateDatax = "createDatax",
  CreateWorkderOfDataX = "CreateWorkderOfDataX",
  CreateFlinkCluster = "CreateFlinkCluster",
  UpdateDataxWriter = "UpdateDataxWriter",
  UpdateDataxReader = 'UpdateDataxReader'
}

// implements OnInit, AfterContentInit
@Component({
  selector: 'tis-steps',
  template: `
      <div class="tis-steps">
          <h2 class="caption">{{processMap.get(this.type).caption}}</h2>
          <nz-steps [nzCurrent]="step">
              <nz-step *ngFor="let s of this.processMap.get(this.type).steps let i = index" [nzTitle]="stepLiteria[i]" [nzDescription]="s"></nz-step>
          </nz-steps>
      </div>
  `,
  styles: [
      `
          .caption {
              color: #71c4ff;
              font-size: 22px;
          }

          .tis-steps {
              margin: 20px 10px 20px 0;
          }
    `
  ]
})
export class TisStepsComponent implements AfterContentInit, OnInit {
  processMap = new Map<string, CaptionSteps>();
  stepLiteria = ["第一步", "第二步", "第三步", "第四步", "第五步", "第六步", "第七步", "第八步", "第九步"]
  @Input()
  type: StepType;

  @Input()
  step = 0;

  constructor() {
    // let createIndexPhase: Array<string> = ;
    this.processMap.set(StepType.CreateIndex, new CaptionSteps("索引实例添加", ["基本信息", "元数据信息", "服务器节点", "确认"]));
    this.processMap.set(StepType.CreateIncr, new CaptionSteps("增量同步添加", ["引擎选择", "Source/Sink配置", "Stream脚本确认",  "状态确认"]));
    this.processMap.set(StepType.CreateDatax, new CaptionSteps("数据管道添加", ["基本信息", "Reader设置", "Writer设置", "表映射", "确认"]));
    this.processMap.set(StepType.UpdateDataxReader, new CaptionSteps("数据管道 Reader 更 新", ["Reader设置", "Writer设置", "表映射", "确认"]));
    this.processMap.set(StepType.UpdateDataxWriter, new CaptionSteps("数据管道 Writer 更 新", ["Writer设置", "表映射", "确认"]));
    this.processMap.set(StepType.CreateWorkderOfDataX, new CaptionSteps("数据管道分布式执行器添加", ["K8S基本信息", "K8S资源规格", "确认"]));
    this.processMap.set(StepType.CreateFlinkCluster, new CaptionSteps("Flink Native Cluster执行器添加", ["K8S基本信息", "确认"]));
  }

  ngOnInit(): void {

  }

  ngAfterContentInit() {

  }
}

@Component({
  selector: 'tis-steps-tools-bar',
  template: `
      <tis-page-header [result]="result" [showBreadcrumb]="false">
          <tis-page-header-left *ngIf="this.title">{{title}}</tis-page-header-left>
          <tis-header-tool>
              <ng-container *ngIf="cancel.observers.length>0">
                  <button nz-button (click)="cancelSteps()"><i nz-icon nzType="logout" nzTheme="outline"></i>取消</button> &nbsp;
              </ng-container>
              <ng-container *ngIf="goBackBtnShow && goBack.observers.length>0">
                  <button nz-button (click)="goBack.emit($event)"><i nz-icon nzType="step-backward" nzTheme="outline"></i>上一步</button> &nbsp;
              </ng-container>
              <ng-container *ngIf="goOnBtnShow && goOn.observers.length>0">
                  <button nz-button nzType="primary" (click)="goOn.emit($event)"><i nz-icon nzType="step-forward" nzTheme="outline"></i>下一步</button>
              </ng-container>
              <ng-content select="final-exec-controller"></ng-content>
          </tis-header-tool>
      </tis-page-header>
  `,
  styles: [
      `
          tis-page-header-left {
              color: #b7d6ff;
              padding-left: 10px;
              border-left: 6px solid rgba(140, 170, 255, 0.31);
          }
    `
  ]
})
export class TisStepsToolbarComponent implements AfterContentInit {

  @Input()
  goOnBtnShow = true;
  @Input()
  goBackBtnShow = true;

  @Input()
  result: TisResponseResult;

  @Output() cancel = new EventEmitter<any>();
  @Output() goBack = new EventEmitter<any>();
  @Output() goOn = new EventEmitter<any>();

  @Input() title: string;

  @ContentChildren(PageHeaderLeftComponent) headerLeftSelect: QueryList<PageHeaderLeftComponent>;

  constructor(private modal: NzModalService) {
  }

  ngAfterContentInit() {
  }

  cancelSteps() {
    this.modal.confirm({
      nzTitle: '<i>确认</i>',
      nzContent: '<b>您是否确定要退出此流程</b>',
      nzOnOk: () => {
        this.cancel.emit();
      }
    });
  }
}

class CaptionSteps {
  constructor(public caption: string, public steps: Array<string>) {
  }
}
