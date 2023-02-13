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

import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {TISService} from "../common/tis.service";
import {BasicFormComponent} from "../common/basic.form.component";
import {AppDesc, ConfirmDTO} from "./addapp-pojo";
import {NzModalService} from "ng-zorro-antd/modal";
import {TisInputTool} from "../common/form.component";
import {Item} from "../common/tis.plugin";

// 文档：https://angular.io/docs/ts/latest/guide/forms.html
@Component({
  selector: 'addapp-form',
  // templateUrl: '/runtime/addapp.htm'
  template: `
      <tis-steps type="createIndex" [step]="0"></tis-steps>
      <tis-form [fieldsErr]="errorItem">
          <tis-page-header [showBreadcrumb]="false" [result]="result">
              <tis-header-tool>
                  <button nz-button nzType="primary" (click)="createIndexStep1Next()">下一步</button>
              </tis-header-tool>
          </tis-page-header>
          <tis-ipt #indexName title="索引名称" name="projectName" require="true">
              <nz-input-group nzSize="large" nzAddOnBefore="search4">
                  <input required type="text" [id]="indexName.name" nz-input [(ngModel)]="model.name" (ngModelChange)="indexNameValChange( indexName)" name="name"/>
              </nz-input-group>
          </tis-ipt>

          <tis-ipt #workflow title="数据源" name="workflow" require="true">
              <nz-select nzSize="large" style="width: 6em" [(ngModel)]="model.dsType" nzAllowClear nzPlaceHolder="请选择">
                  <nz-option nzValue="tab" nzLabel="数据表"></nz-option>
                  <nz-option nzValue="df" nzLabel="DF"></nz-option>
              </nz-select>
              &nbsp;
              <ng-container [ngSwitch]="model.dsType">
                  <ng-container *ngSwitchCase="'tab'">
                      <tis-table-select nzStyle="width: calc(100% - 12em)" nzSize="large" [(ngModel)]="model.tabCascadervalues"></tis-table-select>
                  </ng-container>
                  <ng-container *ngSwitchCase="'df'">
                      <nz-select nzSize="large" style="width: calc(100% - 12em)" nzPlaceHolder="请选择" name="workflow" nzDropdownMatchSelectWidth="true" [(ngModel)]="model.workflow">
                          <nz-option *ngFor="let p of usableWorkflow" [nzValue]="p.id+':'+p.name" [nzLabel]="p.name"></nz-option>
                      </nz-select>
                      <a class="tis-link-btn" [routerLink]="['/','offline','wf_add']">创建数据流</a>
                  </ng-container>
              </ng-container>
          </tis-ipt>

          <tis-ipt #dptId title="所属部门" name="dptId" require="true">
              <nz-select nzSize="large" style="width: calc(100% - 6em)" nzPlaceHolder="请选择" name="dptId" class="form-control" [(ngModel)]="model.dptId">
                  <nz-option *ngFor="let pp of model.dpts" [nzValue]="pp.value" [nzLabel]="pp.name"></nz-option>
              </nz-select>
              <a class="tis-link-btn" [routerLink]="['/','base','departmentlist']">部门管理</a>
          </tis-ipt>

          <tis-ipt #recept title="接口人" name="recept" require="true">
              <input nzSize="large" nz-input [id]="recept.name" [(ngModel)]="model.recept" name="recept"
                     placeholder="小明">
          </tis-ipt>
      </tis-form>
      <!-- Content here -->
  `
  , styles: [
      `
    `
  ]
})
export class AddAppFormComponent extends BasicFormComponent implements OnInit {
  errorItem: Item = Item.create([]);
  // model = new Application(
  //   '', 'Lucene6.0', -1, new Crontab(), -1, ''
  // );
  model = new AppDesc();
  tplenum: any[];
  usableWorkflow: any[];
  @Output() nextStep = new EventEmitter<any>();
  @Input() dto: ConfirmDTO;


  constructor(tisService: TISService, modalService: NzModalService) {
    super(tisService, modalService);
  }


  ngOnInit(): void {

    this.httpPost('/runtime/changedomain.ajax'
      , 'action=add_app_action&emethod=get_create_app_master_data')
      .then((r) => {
        if (r.success) {
          this.model.dpts = r.bizresult.bizlinelist;
          this.tplenum = r.bizresult.tplenum;
          this.usableWorkflow = r.bizresult.usableWorkflow;
        }
      });
    if (this.dto) {
      this.model = this.dto.appform;
    }
  }

  // 执行下一步
  public createIndexStep1Next(): void {
    let dto = new ConfirmDTO();
    dto.appform = this.model;
    this.jsonPost('/runtime/addapp.ajax?action=add_app_action&emethod=validate_app_form'
      , dto.appform)
      .then((r) => {
        this.processResult(r);
        if (r.success) {
          // console.log(dto);
          this.nextStep.emit(dto);
        } else {
          this.errorItem = Item.processFieldsErr(r);
        }
      });
  }

  indexNameValChange(indexName: TisInputTool) {
    delete indexName.itemProp.error;
  }
}
