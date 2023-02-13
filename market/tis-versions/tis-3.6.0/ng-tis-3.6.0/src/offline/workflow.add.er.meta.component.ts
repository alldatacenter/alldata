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

import {AfterContentInit, AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {BasicSideBar, ColumnTransfer, ERMetaNode, ERRuleNode, IDataFlowMainComponent, LinkKey, PrimaryIndexColumnName} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';


import {WorkflowAddComponent} from "./workflow.add.component";

import {NzIconService} from 'ng-zorro-antd/icon';
import {DeleteOutline} from "@ant-design/icons-angular/icons";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {NzDrawerRef} from "ng-zorro-antd/drawer";


@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <nz-spin [nzSpinning]="formDisabled" nzSize="large">

          <sidebar-toolbar (close)="_closeSidebar($event)"
                           (save)="_saveClick()" (delete)="_deleteNode()"></sidebar-toolbar>
          <form class="mete-form" nz-form>
              <nz-row id="meta-setter">
                  <nz-col [nzSpan]="7" style="padding: 8px;">
                      <nz-statistic [nzValue]="this.erMetaNode.dumpnode.tabname" [nzTitle]="titleTplOne"></nz-statistic>
                      <ng-template #titleTplOne><i nz-icon nzType="table" nzTheme="outline"></i>Table Name</ng-template>
                  </nz-col>
                  <nz-col [nzSpan]="17" style="border-left: 1px solid #d0d0d0;">
                      <nz-form-item class="meta-item">
                          <nz-form-label [nzSpan]="4">主索引表</nz-form-label>
                          <nz-form-control class="combine-input" [nzSpan]="13">
                              <nz-switch nzSize="small" [(ngModel)]="this.erMetaNode.primaryIndexTab" nzCheckedChildren="是" nzUnCheckedChildren="否" [ngModelOptions]="{standalone: true}"></nz-switch>
                              <ng-container *ngIf="this.erMetaNode.primaryIndexTab"><br/>
                                  <ng-container *ngFor="let pkName of this.erMetaNode.primaryIndexColumnNames">
                                      <nz-select nzSize="small" class="primary-key" nzPlaceHolder="Select Index Primay column" nzShowSearch nzAllowClear [(ngModel)]="pkName.name" (ngModelChange)="primaryKeyChange(pkName,$event)"
                                                 [ngModelOptions]="{standalone: true}">
                                          <nz-option nzCustomContent *ngFor="let r of this.cols" [nzLabel]="r.key" [nzValue]="r.key">
                                              <span class="key-placeholder"><i nz-icon *ngIf="r.pk" nzType="key" nzTheme="outline"></i></span>{{r.key}}
                                          </nz-option>
                                      </nz-select>
                                      <i nz-icon nzType="minus-circle-o" class="dynamic-delete-button" (click)="removePrimaryKey(pkName)"></i>
                                  </ng-container>
                                  <button nzSize="small" nz-button nzType="dashed" class="primary-key" (click)="addPrimaryIndexColumnName($event)">
                                      <i nz-icon nzType="plus"></i>添加主键列
                                  </button>
                              </ng-container>
                              <ng-template #primaryIndexTab>
                                  <i nz-icon nzType="key" nzTheme="outline"></i>
                              </ng-template>
                          </nz-form-control>
                      </nz-form-item>
                      <nz-form-item *ngIf="this.erMetaNode.primaryIndexTab" class="meta-item">
                          <nz-form-label [nzSpan]="4">分区键</nz-form-label>
                          <nz-form-control [nzSpan]="13">
                              <nz-select nzSize="small" class="primary-key" nzPlaceHolder="分区键" nzShowSearch nzAllowClear [(ngModel)]="erMetaNode.sharedKey"
                                         [ngModelOptions]="{standalone: true}">
                                  <nz-option nzCustomContent *ngFor="let r of this.cols" [nzLabel]="r.key" [nzValue]="r.key">
                                      <span class="key-placeholder"><i nz-icon *ngIf="r.pk" nzType="key" nzTheme="outline"></i></span>{{r.key}}
                                  </nz-option>
                              </nz-select>
                          </nz-form-control>
                      </nz-form-item>
                      <nz-form-item class="meta-item">
                          <nz-form-label [nzSpan]="4">监听增量变更</nz-form-label>
                          <nz-form-control [nzSpan]="13">
                              <nz-switch nzSize="small" [(ngModel)]="this.erMetaNode.monitorTrigger" nzCheckedChildren="开" nzUnCheckedChildren="关" [ngModelOptions]="{standalone: true}"></nz-switch>
                              &nbsp;
                              <nz-select nzSize="small" class="primary-key timestamp-select" *ngIf="this.erMetaNode.monitorTrigger"
                                         nzPlaceHolder="Select TimeVerStamp column" nzShowSearch nzAllowClear [(ngModel)]="this.erMetaNode.timeVerColName" [ngModelOptions]="{standalone: true}">
                                  <nz-option nzCustomContent *ngFor="let r of this.cols" [nzLabel]="r.key" [nzValue]="r.key">
                                      <span class="key-placeholder"><i nz-icon *ngIf="r.pk" nzType="key" nzTheme="outline"></i></span>{{r.key}}
                                  </nz-option>
                              </nz-select>
                          </nz-form-control>
                      </nz-form-item>
                  </nz-col>
              </nz-row>

              <!--
              <div style="background: #ECECEC; padding: 30px;">
                  <nz-row [nzGutter]="20">
                      <nz-col [nzSpan]="12">
                          <nz-statistic [nzValue]="erRel.targetNode.tabname" [nzTitle]="'Parent Table'"></nz-statistic>
                      </nz-col>
                      <nz-col [nzSpan]="12">
                          <nz-statistic [nzValue]="erRel.sourceNode.tabname" [nzTitle]="'Child Table'" [nzSuffix]="suffixTplOne"></nz-statistic>
                          <ng-template #suffixTplOne>
                              <nz-radio-group [nzSize]="'small'" [(ngModel)]="erRel.cardinality" [nzButtonStyle]="'solid'" [ngModelOptions]="{standalone: true}">
                                  <label nz-radio-button nzValue="1:1">1:1</label>
                                  <label nz-radio-button nzValue="1:n">1:N</label>
                              </nz-radio-group>
                          </ng-template>
                      </nz-col>
                  </nz-row>
              </div>
              -->
              <div id="control-block" class="clear">
                  <button nz-button nzSize="small" nzType="primary" (click)="addKeyLink()">添加</button>
              </div>
              <nz-table #rows nzBordered [nzData]="this.erMetaNode.columnTransferList" [nzShowPagination]="false">
                  <thead>
                  <tr>
                      <th width="6%">
                          <button nz-button nzSize="small" nzType="link" (click)="deleteLink()"><i nz-icon nzType="delete" nzTheme="outline"></i></button>
                      </th>
                      <th width="30%">
                          Column Name
                      </th>
                      <th width="34%">
                          Transfer func
                      </th>
                      <th width="30%">
                          Params
                      </th>
                  </tr>
                  </thead>

                  <tbody>
                  <tr *ngFor="let rr of rows.data">
                      <td align="center">
                          <label nz-checkbox [(ngModel)]="rr.checked" [ngModelOptions]="{standalone: true}"></label>
                      </td>
                      <td>
                          <nz-select nzShowSearch nzAllowClear nzPlaceHolder="Select a column" [(ngModel)]="rr.colKey" [ngModelOptions]="{standalone: true}"
                                     (ngModelChange)="parentKeyChange(rr,$event)">
                              <nz-option *ngFor="let r of this.cols" [nzLabel]="r.key" [nzValue]="r.key"></nz-option>
                          </nz-select>
                      </td>
                      <td>
                          <nz-select nzShowSearch nzAllowClear nzPlaceHolder="Select a Transfer" [(ngModel)]="rr.transfer" [ngModelOptions]="{standalone: true}">
                              <nz-option *ngFor="let r of this.transfers" [nzLabel]="r.key" [nzValue]="r.key"></nz-option>
                          </nz-select>
                      </td>
                      <td><input nz-input [(ngModel)]="rr.param" [ngModelOptions]="{standalone: true}"/></td>
                  </tr>
                  </tbody>

              </nz-table>
          </form>
      </nz-spin>
  `,

  styles: [
      `
          .primary-key {
              width: 60%;
              margin: 3px 0 3px 0;
          }

          .timestamp-select {
              width: 70%;
          }


          .key-placeholder {
              display: inline-block;
              width: 1.5em;
          }

          .dynamic-delete-button {
              cursor: pointer;
              position: relative;
              top: 4px;
              font-size: 24px;
              color: #999;
              transition: all 0.3s;
          }

          .dynamic-delete-button:hover {
              color: #777;
          }

          #meta-setter div {
              float: left;
          }

          #meta-setter {
              border: 1px solid #d0d0d0;
          }

          #control-block {
              margin-bottom: 5px;
              margin-top: 15px;
          }

          .clear {
              clear: both;
          }

          .meta-item {
              margin-bottom: 0px;
          }

          .mete-form {
          }
    `]
})
// ER Meta,
export class WorkflowAddErMetaComponent
  extends BasicSideBar implements OnInit, AfterContentInit, AfterViewInit {
  // listOfOption: Array<{ label: string; value: string }> = [];
  // listOfTagOptions: any[] = [];
  erMetaNode: ERMetaNode

  cols: Array<{ key: string, pk: boolean }> = [];
  transfers: Array<{ key: string }> = [];

  constructor(tisService: TISService, notification: NzNotificationService,
              modalService: NzModalService, private cdr: ChangeDetectorRef, drawerRef: NzDrawerRef<BasicSideBar>) {
    super(tisService, modalService, drawerRef, notification);
    this.transfers.push({"key": "dateYYYYmmdd"});
    this.transfers.push({"key": "dateYYYYMMddHHmmss"});
    // this.transfers.push({"key": "dateYYYYmmdd"});
  }

  ngOnInit(): void {
    // const children: Array<{ label: string; value: string }> = [];
    // for (let i = 10; i < 36; i++) {
    //   children.push({label: i.toString(36) + i, value: i.toString(36) + i});
    // }
    // this.listOfOption = children;
  }


  ngAfterViewInit(): void {

    // this.jsonPost('/offline/datasource.ajax?emethod=reflect_table_cols&action=offline_datasource_action',
    //   [{'key': this.erRel.sourceNode.nodeid, 'sql': this.erRel.sourceNode.sqlcontent}
    //     , {'key': this.erRel.targetNode.nodeid, 'sql': this.erRel.targetNode.sqlcontent}])
    // if (!this.erRel.topologyName) {
    //   throw new Error(`param 'topologyName' can not be null`);
    // }
    this.jsonPost('/offline/datasource.ajax?emethod=reflect_table_cols&action=offline_datasource_action&topology=' + this.erMetaNode.topologyName,
      [{'key': this.erMetaNode.dumpnode.nodeid}])
      .then(result => {
        if (result.success) {
          let colsKey: Map<string, Array<{ key: string, pk: boolean }>> = new Map();
          let keySqlCols: Array<{ key: string; cols: Array<{ key: string, pk: boolean }> }> = result.bizresult;
          keySqlCols.forEach((r) => {
            let cols: Array<{ key: string, pk: boolean }> = [];
            r.cols.forEach((c) => {
              cols.push(c);
            });
            colsKey.set(r.key, cols);
          });

          this.cols = colsKey.get(this.erMetaNode.dumpnode.nodeid);
        }

        this.cdr.detectChanges();
      });
  }

  ngAfterContentInit(): void {
  }

  // 执行保存流程
  subscribeSaveClick(graph: any, $: any, nodeid: any, _: IDataFlowMainComponent): void {

    let old = graph.findById(nodeid);
//    old.ermeta = ;
    // old.linkrule = {'linkKeyList': this.linkKeyList, 'cardinality': this.erRel.cardinality};
    // = this.linkKeyList;
    // console.log(old);
    //  console.log(this.erMetaNode);

    let nmodel = {'extraMeta': this.erMetaNode};

    // console.log(nodeid);
    // 更新label值
    graph.updateItem(old, nmodel);

    this._closeSidebar(null);
  }

  initComponent(_: IDataFlowMainComponent, erMetaNode: ERMetaNode): void {
    this.erMetaNode = erMetaNode;
  }


  _toggleSidebar() {
  }

  _deleteNode() {
    // let edgeId = this.erRel.rel.id;
    //
    // let node = this.g6Graph.findById(edgeId);
    // this.g6Graph.removeItem(node);
    // this.parentComponent.joinNodeMap.delete(id);
    // this.refeshDependencyOption();
    this._closeSidebar(null);
  }

  addKeyLink() {
    // // console.log("dddd");
    let tmp: ColumnTransfer[] = [];
    this.erMetaNode.columnTransferList.forEach((r) => {
      tmp.push(r);
    });
    tmp.push(new ColumnTransfer(null, null, "value"));
    this.erMetaNode.columnTransferList = tmp;
  }

  deleteLink() {

    //  this.erMetaNode.columnTransferList
    let tmp: ColumnTransfer[] = [];
    this.erMetaNode.columnTransferList.forEach((r) => {
      if (!r.checked) {
        tmp.push(r);
      }
    });
    this.erMetaNode.columnTransferList = tmp;
  }


  parentKeyChange(link: any, parentKey: any) {
    let ct: ColumnTransfer = link;
    // if (this.childCols.findIndex((r) => {
    //   // console.log(r);
    //   return r.key === parentKey;
    // }) > -1) {
    //   link.childKey = parentKey;
    // }
  }

  addPrimaryIndexColumnName(event: MouseEvent) {
    this.erMetaNode.primaryIndexColumnNames.push(new PrimaryIndexColumnName(null, false));
  }

  removePrimaryKey(pkName: PrimaryIndexColumnName) {
    if (this.erMetaNode.primaryIndexColumnNames.length < 2) {
      this.notification.error('错误', '主索引表不能少于一个键');
      return;
    }
    pkName.delete = true;
    let selectedCols = this.erMetaNode.primaryIndexColumnNames;
    this.erMetaNode.primaryIndexColumnNames = [];
    selectedCols.forEach((r) => {
      if (r.delete) {
        return;
      }
      this.erMetaNode.primaryIndexColumnNames.push(r);
    });
  }

  primaryKeyChange(pkName: PrimaryIndexColumnName, event: any) {
    console.log(event);
    let find = this.cols.find((c) => c.key === event && c.pk);
    if (find) {
      pkName.pk = true;
    }
  }
}









