import {AfterContentInit, Component, EventEmitter, Input, Output} from "@angular/core";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";
import {IndexIncrStatus} from "./misc/RCDeployment";
import {Descriptor, HeteroList, Item, ItemPropVal, PluginSaveResponse, PluginType, SavePluginEvent} from "../common/tis.plugin";
import {TISService} from "../common/tis.service";
import {ActivatedRoute} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {IntendDirect} from "../common/MultiViewDAG";
import {DataxAddStep4Component} from "../base/datax.add.step4.component";
import {TransferDirection, TransferItem} from "ng-zorro-antd/transfer";
import {PluginsComponent} from "../common/plugins.component";
import {NzNotificationService} from "ng-zorro-antd/notification";

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
  template: `
      <tis-steps type="createIncr" [step]="1"></tis-steps>
      <tis-page-header [showBreadcrumb]="false" [result]="result">
          <tis-header-tool>
          </tis-header-tool>
      </tis-page-header>
      <nz-spin nzSize="large" [nzSpinning]="formDisabled">
          <nz-tabset [nzTabBarExtraContent]="extraTemplate" [(nzSelectedIndex)]="tabSelectIndex">
              <nz-tab nzTitle="配置">
                  <ng-template nz-tab>
                      <selected-tables [showPagination]="false" [direction]="'right'" [items]="items" [disabled]="false" [descriptor]="incrSourceDescriptor" [batchSettableTabs]="[]"
                                       [pluginMetas]="pluginMetas" [subFormHetero]="this.subFormHetero" [stat]="{checkAll: false, checkHalf: false}" [subFieldForms]="subFieldForms"
                                       [dataXReaderTargetName]="'dataxName_' + this.currentApp.name+','+targetItemDesc_incr_process_extend"
                                       (onItemSelect)="onItemSelect($event)" (onItemSelectAll)="onItemSelectAll($event)" [skipSubformDescNullError]="skipSubformDescNullError"></selected-tables>
                  </ng-template>
              </nz-tab>
              <!--              <nz-tab nzTitle="执行脚本">-->
              <!--                  <ng-template nz-tab>-->
              <!--                      <div style="height: 800px">-->
              <!--                          <tis-codemirror name="schemaContent" [(ngModel)]="dto.incrScriptMainFileContent" [config]="codeMirrorCfg"></tis-codemirror>-->
              <!--                      </div>-->
              <!--                  </ng-template>-->
              <!--              </nz-tab>-->
          </nz-tabset>
          <ng-template #extraTemplate>
              <nz-affix [nzOffsetTop]="10">
                  <button nz-button nzType="default" (click)="createIndexStepPre()"><i nz-icon nzType="backward" nzTheme="outline"></i>上一步</button>&nbsp;
                  <button nz-button nzType="primary" (click)="createIndexStep1Next()" [nzLoading]="this.formDisabled"><i nz-icon nzType="save" nzTheme="outline"></i>保存&下一步</button>
                  &nbsp;
                  <button nz-button nzType="default" (click)="cancelStep()">取消</button>
              </nz-affix>
          </ng-template>
      </nz-spin>
  `,
  styles: [
      ` nz-step {
          margin: 20px;
      }
    `
  ]
})
export class IncrBuildStep1ExtendSelectedTabPropsComponent extends AppFormComponent implements AfterContentInit {
  // private _incrScript: string;
  @Output() nextStep = new EventEmitter<any>();
  @Output() preStep = new EventEmitter<any>();
  _dto: IndexIncrStatus;
  incrSourceDescriptor: Descriptor;

  targetItemDesc_incr_process_extend = 'targetItemDesc_incr_process_extend';

  skipSubformDescNullError = true;

  @Input()
  public set dto(status: IndexIncrStatus) {
    this._dto = status;
    let desc = new Descriptor()
    desc.impl = status.incrSourceDesc.impl;
    desc.displayName = status.incrSourceDesc.displayName;
    desc.extendPoint = status.incrSourceDesc.extendPoint;
    this.incrSourceDescriptor = desc;
    //  console.log(this.incrSourceDescriptor);
  }

  tabSelectIndex = 0;
  items: TransferItem[] = [];

  pluginMetas: PluginType[] = [];
  subFormHetero: HeteroList = new HeteroList();

  subFieldForms: Map<string /*tableName*/, Array<Item>> = new Map();

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService, notification: NzNotificationService) {
    super(tisService, route, modalService, notification);
  }

  ngOnInit(): void {
    // console.log([this.dto.readerDesc.endType, this.dto.writerDesc.endType]);
    // if (!this.dto.k8sPluginInitialized) {
    //   // this.plugins.push({name: 'incr-config', require: true});
    // }
    super.ngOnInit();
  }


  // get incrScript(): string {
  //   return this._incrScript;
  // }
  //
  // set incrScript(value: string) {
  //   this._incrScript = value;
  // }


  protected initialize(app: CurrentCollection): void {

    DataxAddStep4Component.initializeSubFieldForms(this, this.getPluginMetas(app), undefined // this.dto.readerDescriptor.impl
      , (subFieldForms: Map<string /*tableName*/, Array<Item>>, subFormHetero: HeteroList, readerDesc: Descriptor) => {
        // console.log(subFieldForms);
        this.subFieldForms = subFieldForms;
        this.subFormHetero = subFormHetero;

        readerDesc.subFormMeta.idList.forEach((subformId) => {
          // let direction: TransferDirection = ('right');
          this.items.push({
            key: subformId,
            title: subformId,
            direction: 'right',
            disabled: false,
            meta: Object.assign({id: `${subformId}`}, readerDesc.subFormMeta)
          });
        });
        this.items = [...this.items];
      });
  }

  getPluginMetas(app: CurrentCollection): PluginType {
    // console.log(this.dto.readerDesc);
    if (this.pluginMetas.length < 1) {
      this.pluginMetas = [{
        skipSubformDescNullError: this.skipSubformDescNullError,
        name: "dataxReader", require: true
        , extraParam: `targetDescriptorName_${this._dto.readerDesc.displayName},targetDescriptorImpl_${this._dto.readerDesc.impl},${this.targetItemDesc_incr_process_extend},subFormFieldName_selectedTabs,dataxName_${app.name},maxReaderTableCount_9999`
      }];
    }
    return this.pluginMetas[0];
  }

  // get pluginMetas(): PluginType[] {
  //   return this.getPluginMetas(this.)
  // }

  ngAfterContentInit(): void {
  }

  createIndexStep1Next() {
    // let e = new SavePluginEvent();
    // e.notShowBizMsg = true;
    // e.serverForward = "coredefine:core_action:create_incr_sync_channal";
    // this.savePlugin.emit(e);
    // let desc: Descriptor = this.subFormHetero.descriptorList[0];
    // console.log(desc);
    for (let index = 0; index < this.items.length; index++) {
      let tabName = this.items[index].key;
      if (!this.subFormHetero.items[0].vals[tabName]) {
        this.errNotify(`请为表:"${tabName}"设置扩展属性`);
        return;
      }
    }

    let savePluginEvent = new SavePluginEvent();
    savePluginEvent.notShowBizMsg = true;
    PluginsComponent.postHeteroList(this, this.pluginMetas, [this.subFormHetero], savePluginEvent, true, (result) => {
      if (result.success) {
        this.nextStep.emit(this._dto);
      } else {
        this.result = result;
      }
    });

  }

  // private compileAndPackageIncr() {
  //   let url = '/coredefine/corenodemanage.ajax?emethod=compileAndPackage&action=core_action';
  //   this.jsonPost(url, {}).then((result) => {
  //     if (result.success) {
  //       // 执行编译打包
  //       this.nextStep.emit(this.dto);
  //     } else {
  //       let errFields = result.errorfields;
  //       if (errFields.length > 0) {
  //         let errFieldKey = "incr_script_compile_error";
  //         let item: Item = Item.create([errFieldKey]);
  //         Item.processErrorField(errFields[0], [item]);
  //         if ("error" === item.vals[errFieldKey].error) {
  //           this.tabSelectIndex = 1;
  //         }
  //       }
  //     }
  //   });
  // }

  cancelStep() {
  }

  // buildStep1ParamsSetComponentAjax(event: PluginSaveResponse) {
  //   if (event.saveSuccess) {
  //     if (event.hasBiz()) {
  //       let biz = event.biz();
  //       this.dto.incrSourceDesc = biz.incrSourceDesc;
  //       this.dto.incrScriptMainFileContent = biz.incrScriptMainFileContent;
  //       if (this.dto.incrSourceDesc.extendSelectedTabProp) {
  //         let n: IntendDirect = {dto: this.dto, cpt: DataxAddStep4Component};
  //         this.nextStep.emit(n);
  //         return;
  //       }
  //     }
  //     this.nextStep.emit(this.dto);
  //   }
  //
  //   setTimeout(() => {
  //     this.formDisabled = event.formDisabled;
  //   })
  // }

  createIndexStepPre() {
    this.preStep.emit(this._dto);
  }

  onItemSelect(event: any) {
  }

  onItemSelectAll(event: any) {
  }
}
