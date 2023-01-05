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

import {AfterViewInit, Component, ComponentFactoryResolver, OnInit, TemplateRef, Type, ViewChild, ViewContainerRef} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, BasicFormComponent, CurrentCollection} from "../common/basic.form.component";

import {ActivatedRoute, Router} from "@angular/router";
import {MultiViewDAG} from "../common/MultiViewDAG";
// import { NzSafeAny} from "ng-zorro-antd";
import {NzModalService} from "ng-zorro-antd/modal";
import {DataxAddStep1Component} from "./datax.add.step1.component";
import {DataxAddStep2Component, DataXReaderWriterEnum} from "./datax.add.step2.component";
import {DataxAddStep3Component} from "./datax.add.step3.component";
import {DataxAddStep4Component} from "./datax.add.step4.component";
import {DataxAddStep5Component} from "./datax.add.step5.component";
import {DataxAddStep6Component} from "./datax.add.step6.maptable.component";
import {DataxAddStep7Component} from "./datax.add.step7.confirm.component";
import {DataxAddStep6ColsMetaSetterComponent} from "./datax.add.step6.cols-meta-setter.component";
import {StepType} from "../common/steps.component";
import {Descriptor} from "../common/tis.plugin";
import {AddAppDefSchemaComponent} from "./addapp-define-schema.component";
import {PluginsComponent} from "../common/plugins.component";
import {NzSafeAny} from "ng-zorro-antd/core/types";
import {TablePojo} from "../offline/table.add.component";
import {Subject} from "rxjs";


@Component({
  template: `
      <nz-spin nzSize="large" [nzSpinning]="formDisabled" style="min-height: 300px">
          <ng-template #container></ng-template>
      </nz-spin>
      <ng-template #proessErr>当前是更新流程不能进入该页面
      </ng-template>
      {{ multiViewDAG.lastCpt?.name}}
  `
})
export class DataxAddComponent extends AppFormComponent implements AfterViewInit, OnInit {
  @ViewChild('container', {read: ViewContainerRef, static: true}) containerRef: ViewContainerRef;

  @ViewChild('proessErr', {read: TemplateRef, static: true}) proessErrRef: TemplateRef<NzSafeAny>;
  multiViewDAG: MultiViewDAG;

  public static getDataXMeta(cpt: BasicFormComponent, app: CurrentCollection, execId?: string): Promise<DataxDTO> {
    return cpt.httpPost("/coredefine/corenodemanage.ajax"
      , "action=datax_action&emethod=get_data_x_meta")
      .then((r) => {
        // this.processResult(r);
        if (r.success) {
          let dto = new DataxDTO(execId);
          dto.dataxPipeName = app.appName;
          dto.processMeta = r.bizresult.processMeta;
          // this.dto.readerDescriptor = null;
          let wdescIt: IterableIterator<Descriptor> = PluginsComponent.wrapDescriptors(r.bizresult.writerDesc).values();
          let rdescIt: IterableIterator<Descriptor> = PluginsComponent.wrapDescriptors(r.bizresult.readerDesc).values();
          dto.writerDescriptor = wdescIt.next().value;
          dto.readerDescriptor = rdescIt.next().value;
          return dto;
        }
      });
  }


  constructor(tisService: TISService, protected r: Router, route: ActivatedRoute, modalService: NzModalService
    , private _componentFactoryResolver: ComponentFactoryResolver) {
    super(tisService, route, modalService);
  }

  goToDataXCfgManager(app: CurrentCollection): void {
    this.r.navigate(['/x', app.name, 'config'], {relativeTo: this.route});
  }

  protected initialize(app: CurrentCollection): void {
    // console.log("ddd");
    let paramsMap = this.route.snapshot.queryParamMap;
    let execId = paramsMap.get("execId");
    this.route.fragment.subscribe((r) => {
      let cpt: Type<any> = DataxAddStep1Component;
      switch (r) {
        case "reader":
          cpt = DataxAddStep3Component;
          if (!execId) {
            throw new Error("param execId can not be null");
          }
          DataxAddComponent.getDataXMeta(this, app, execId).then((dto) => {
            dto.processModel = StepType.UpdateDataxReader;
            this.multiViewDAG.loadComponent(cpt, dto);
          })
          return;
        case "writer":
          cpt = DataxAddStep5Component;
          if (!execId) {
            throw new Error("param execId");
          }
          DataxAddComponent.getDataXMeta(this, app, execId).then((dto) => {
            dto.processModel = StepType.UpdateDataxWriter;
            this.multiViewDAG.loadComponent(cpt, dto);
          })
          return;
        default:
          if (app) {
            this.modalService.warning({
              nzTitle: "错误",
              nzContent: this.proessErrRef,
              nzOkText: "进入DataX配置编辑",
              nzOnOk: () => {
                this.goToDataXCfgManager(app);
              }
            });
            return;
          }
          this.multiViewDAG.loadComponent(cpt, new DataxDTO());
      }
    })
  }

  ngAfterViewInit() {
  }


  ngOnInit(): void {
    // 配置步骤前后跳转状态机
    let configFST: Map<any, { next: any, pre: any }> = new Map();
    configFST.set(DataxAddStep1Component, {next: DataxAddStep2Component, pre: null});
    configFST.set(DataxAddStep2Component, {next: DataxAddStep3Component, pre: DataxAddStep1Component});
    configFST.set(DataxAddStep3Component, {next: DataxAddStep4Component, pre: DataxAddStep2Component});
    configFST.set(DataxAddStep4Component, {next: DataxAddStep5Component, pre: DataxAddStep3Component});
    configFST.set(DataxAddStep5Component, {next: DataxAddStep6Component, pre: DataxAddStep4Component});
    configFST.set(DataxAddStep6Component, {next: DataxAddStep7Component, pre: DataxAddStep5Component});
    configFST.set(DataxAddStep7Component, {next: null, pre: DataxAddStep6Component});

    configFST.set(DataxAddStep6ColsMetaSetterComponent, {next: DataxAddStep7Component, pre: DataxAddStep5Component});
    // use for elasticsearch writer cols set
    configFST.set(AddAppDefSchemaComponent, {next: DataxAddStep7Component, pre: DataxAddStep5Component});


    this.multiViewDAG = new MultiViewDAG(configFST, this._componentFactoryResolver, this.containerRef);
    /**=====================================================
     * <<<<<<<<<for test
     =======================================================*/
    // DataxAddStep2Component.getDataXReaderWriterEnum(this).then((rwEnum: DataXReaderWriterEnum) => {
    //   let dto = new DataxDTO();
    //   dto.dataxPipeName = "hudi2";
    //   dto.processMeta = {readerRDBMS: true, explicitTable: true, writerRDBMS: true, writerSupportMultiTab: false};
    //   // dto.readerDescriptor = rwEnum.readerDescs.find((r) => "OSS" === r.displayName);
    //   // dto.writerDescriptor = rwEnum.writerDescs.find((r) => "Elasticsearch" === r.displayName);
    //   dto.readerDescriptor = rwEnum.readerDescs.find((r) => "MySQL" === r.displayName);
    //   dto.writerDescriptor = rwEnum.writerDescs.find((r) => "Hudi" === r.displayName);
    //   this.multiViewDAG.loadComponent(DataxAddStep4Component, dto);
    // });
    /**=====================================================
     * for test end>>>>>>>>
     =======================================================*/
    super.ngOnInit();

  }
}


/**
 * 被选中的列
 */
export interface ISelectedCol {
  label: string;
  value: string;
  checked: boolean;
  pk: boolean;
}

export interface ISelectedTabMeta {

  tableName: string,
  selectableCols: Array<ISelectedCol> // r.bizresult
}

class DataxProfile {
  projectName: string;
  recept: string;
  dptId: string;
}

export class DataxDTO {

  public headerStepShow = true;

  dataxPipeName: string;
  profile: DataxProfile = new DataxProfile();
  selectableTabs: Map<string /* table */, ISelectedTabMeta> = new Map();
  readerDescriptor: Descriptor;
  writerDescriptor: Descriptor;

  processMeta: DataXCreateProcessMeta;
  // 流程是否处于更新模式
  processModel: StepType = StepType.CreateDatax;

  componentCallback: { step3: Subject<DataxAddStep3Component>, step4: Subject<DataxAddStep4Component> }
   = {step3: new Subject<DataxAddStep3Component>(), step4: new Subject<DataxAddStep4Component>()};
  tablePojo: TablePojo;

  // dataxAddStep3Callback: (_: DataxAddStep3Component) => void;

  constructor(public execId?: string) {
  }

  get readerImpl(): string {
    if (!this.readerDescriptor) {
      return null;
    }
    return this.readerDescriptor.impl;
  }

  get writerImpl(): string {
    if (!this.writerDescriptor) {
      return null;
    }
    return this.writerDescriptor.impl;
  }
}

export interface DataXCreateProcessMeta {
  readerRDBMS: boolean;
  // DataX Reader 是否有明确的表名
  explicitTable: boolean;

  // writer 是否符合关系型数据库要求
  writerRDBMS: boolean;
  // reader 中是否可以选择多个表，例如像elastic这样的writer中对于column的设置比较复杂，需要在writer plugin页面中完成，所以就不能支持在reader中选择多个表了
  writerSupportMultiTab: boolean;
}

