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

import {Component, ComponentFactoryResolver, Input, OnInit, ViewChild, ViewContainerRef} from '@angular/core';
import {TISService} from '../common/tis.service';
import {BasicFormComponent} from '../common/basic.form.component';

import {ActivatedRoute} from '@angular/router';
// @ts-ignore
import * as $ from 'jquery';
import {NzModalRef} from "ng-zorro-antd/modal";
import {HeteroList, ItemPropVal, TisResponseResult} from "../common/tis.plugin";
import {MultiViewDAG} from "../common/MultiViewDAG";
import {DataxAddStep3Component} from "../base/datax.add.step3.component";
import {DataxAddStep4Component} from "../base/datax.add.step4.component";
import {ExecModel} from "../base/datax.add.step7.confirm.component";
import {TableAddStep1Component} from "./table.add.step1.component";
import {DataxDTO} from "../base/datax.add.component";
import {NzNotificationService} from "ng-zorro-antd/notification";


@Component({
  // templateUrl: '/offline/tableaddstep.htm'
  template: `
      <tis-msg [result]="result"></tis-msg>

      <nz-spin nzSize="large" [nzSpinning]="formDisabled" style="min-height: 300px">
          <ng-template #container></ng-template>
      </nz-spin>
      {{ multiViewDAG.lastCpt?.name}}
      <!--      <div>-->
      <!--          <tableAddStep1 *ngIf="currentIndex===0"-->
      <!--                         (nextStep)="goToNextStep($event)"-->
      <!--                         [tablePojo]="tablePojo"></tableAddStep1>-->
      <!--          <tableAddStep2 *ngIf="currentIndex===1" (previousStep)="goToPreviousStep($event)"-->
      <!--                         (processSuccess)="processTableAddSuccess($event)"-->
      <!--                         [step1Form]="step1Form"-->
      <!--                         [tablePojo]="tablePojo"></tableAddStep2>-->
      <!--      </div>-->
  `
})
export class TableAddComponent extends BasicFormComponent implements OnInit {
  // title: string = 'table add step';
  currentIndex = 0;
  stepsNum = 2;
  step1Form: TablePojo;
  tablePojo: TablePojo;
  // id: number;

  @Input() processMode: { tableid?: number, dbId?: string, dbName?: string, isNew: boolean } = {isNew: true};


  @ViewChild('container', {read: ViewContainerRef, static: true}) containerRef: ViewContainerRef;
  multiViewDAG: MultiViewDAG;

  public static findDBNameProp(hlists: HeteroList[]): ItemPropVal {
    let result: ItemPropVal[] = [undefined];
    hlists.forEach((hlist) => {
      let item = hlist.items[0];
      let itemProps = item.propVals;
      let pp = itemProps.find((ip) => "dbName" === ip.key);
      if (pp) {
        pp.disabled = true;
        result[0] = pp;
        return;
      }
    })
    return result[0];
  }

  constructor(tisService: TISService
    , private activateRoute: ActivatedRoute
    , private activeModal: NzModalRef, private _componentFactoryResolver: ComponentFactoryResolver, notification: NzNotificationService) {
    super(tisService, null, notification);
    this.tablePojo = new TablePojo();
    this.step1Form = new TablePojo();
  }


  get updateMode(): boolean {
    return !this.processMode.isNew;
  }

  ngOnInit(): void {

    let configFST: Map<any, { next: any, pre: any }> = new Map();
    configFST.set(TableAddStep1Component, {next: DataxAddStep3Component, pre: null});
    configFST.set(DataxAddStep3Component, {next: DataxAddStep4Component, pre: TableAddStep1Component});
    configFST.set(DataxAddStep4Component, {next: null, pre: DataxAddStep3Component});

    this.multiViewDAG = new MultiViewDAG(configFST, this._componentFactoryResolver, this.containerRef);
    // ============================================
    let dataXDto = new DataxDTO();
    dataXDto.tablePojo = this.tablePojo;
    dataXDto.headerStepShow = false;

    dataXDto.componentCallback.step3.subscribe((cpt) => {
      cpt.pluginComponent.formControlSpan = 21;
      let pp = TableAddComponent.findDBNameProp(cpt.hlist);
      if (pp) {
        pp.primary = dataXDto.tablePojo.dbName;
      }
    });
    dataXDto.componentCallback.step4.subscribe((cpt) => {
      cpt.execModel = ExecModel.Reader;
      cpt.nextStep.subscribe(() => {
        this.activeModal.close();
        this.successNotify("数据源:" + dataXDto.tablePojo.dbName + "已完成表导入设置");
      })
    });
    if (this.processMode.dbId) {
      this.tablePojo.dbId = this.processMode.dbId;
      this.tablePojo.dbName = this.processMode.dbName;

      TableAddStep1Component.getReaderDescAndMeta(this, dataXDto).then((r) => {
        if (r.success) {
          // if (r.bizresult.dataReaderSettedNotSupport) {
          // this.errNotify("插件:" + r.bizresult.dataReaderSettedNotSupport + " 不支持表导入");
          // return;
          // }
          this.multiViewDAG.loadComponent(r.bizresult.dataReaderSetted ? DataxAddStep4Component : DataxAddStep3Component, dataXDto);
        }
      });

    } else {
      this.multiViewDAG.loadComponent(TableAddStep1Component, dataXDto);
    }
    let mode = this.processMode;
    // console.log(mode);
    this.tablePojo.isAdd = true;
    if (!mode.isNew) {
      // this.id = mode['tableId'];
      this.tablePojo.isAdd = false;
      this.tablePojo.id = mode.tableid;
      // this.title = '修改数据表';

      let action = `action=offline_datasource_action&event_submit_do_get_datasource_table_by_id=y&id=${mode.tableid}`;
      this.httpPost('/offline/datasource.ajax', action)
        .then(result => {
          if (result.success) {
            let t = result.bizresult;
            this.tablePojo = $.extend(this.tablePojo, t);
          }
        });
    }
  }



  goToNextStep(form: TablePojo) {
    this.currentIndex = (this.currentIndex + 1) % this.stepsNum;
    this.step1Form = form;
  }

  goToPreviousStep(form: any) {
    this.currentIndex = (this.currentIndex - 1) % this.stepsNum;
  }


  processTableAddSuccess(e: TisResponseResult) {
    this.activeModal.close(e);
  }
}

export class TablePojo {
  public cols: TabColReflect[] = [];

  set db(val: { name: string, value: string }) {
    this.dbId = val.value;
    this.dbName = val.name;
  }

  constructor(
    // public tableLogicName?: string,
    public partitionNum?: number,
    public dbName?: string,
    public partitionInterval?: number,
    public selectSql?: string,
    public sqlAnalyseResult?: any,
    public isAdd?: boolean,
    public id?: number,
    public dbId?: string,
    public tableName?: string,
  ) {

  }

}

export class TabColReflect {
  public key: string;
  public pk: boolean;
  public type: number;
}
