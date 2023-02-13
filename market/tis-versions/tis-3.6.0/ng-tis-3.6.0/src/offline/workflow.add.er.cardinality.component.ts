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
import {BasicSideBar, ERRuleNode, IDataFlowMainComponent, LinkKey} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';


import {WorkflowAddComponent} from "./workflow.add.component";

import {NzIconService} from 'ng-zorro-antd/icon';
import {DeleteOutline} from "@ant-design/icons-angular/icons";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzDrawerRef} from "ng-zorro-antd/drawer";


@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <nz-spin [nzSpinning]="formDisabled" nzSize="large">

          <sidebar-toolbar (close)="_closeSidebar($event)"
                           (save)="_saveClick()" (delete)="_deleteNode()"></sidebar-toolbar>
          <form class="clear" nz-form>
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
              <div id="control-block">
                  <button nz-button nzSize="small" nzType="primary" (click)="addKeyLink()">添加</button>
              </div>

              <nz-table #rows nzBordered [nzData]="linkKeyList" [nzShowPagination]="false">
                  <thead>
                  <tr>
                      <th width="6%">
                          <button nz-button nzSize="small" nzType="link" (click)="deleteLink()"><i nz-icon nzType="delete" nzTheme="outline"></i></button>
                      </th>
                      <th width="47%">
                          Parent Column
                      </th>
                      <th width="47%">
                          Child Column
                      </th>
                  </tr>
                  </thead>

                  <tbody>
                  <tr *ngFor="let rr of rows.data">
                      <td align="center">
                          <label nz-checkbox [(ngModel)]="rr.checked" [ngModelOptions]="{standalone: true}"></label>
                      </td>
                      <td>
                          <nz-select nzShowSearch nzAllowClear nzPlaceHolder="Select a column" [(ngModel)]="rr.parentKey" [ngModelOptions]="{standalone: true}"
                                     (ngModelChange)="parentKeyChange(rr,$event)">
                              <nz-option *ngFor="let r of this.parentCols" [nzLabel]="r.key" [nzValue]="r.key"></nz-option>
                          </nz-select>
                      </td>
                      <td>
                          <nz-select nzShowSearch nzAllowClear nzPlaceHolder="Select a column" [(ngModel)]="rr.childKey" [ngModelOptions]="{standalone: true}">
                              <nz-option *ngFor="let r of this.childCols" [nzLabel]="r.key" [nzValue]="r.key"></nz-option>
                          </nz-select>
                      </td>
                  </tr>
                  </tbody>

              </nz-table>


          </form>

      </nz-spin>
  `,

  styles: [
      `
          nz-statistic {
              background: white;
              padding: 10px;
          }

          #control-block {
              margin-bottom: 5px;
              margin-top: 10px;
          }

          .clear {
              clear: both;
          }
    `]
})
// ER Cardinality
export class WorkflowAddErCardinalityComponent
  extends BasicSideBar implements OnInit, AfterContentInit, AfterViewInit {
  // listOfOption: Array<{ label: string; value: string }> = [];
  listOfTagOptions: any[] = [];
  erRel: ERRuleNode;

  linkKeyList: LinkKey[] = [];
  parentCols: Array<{ key: string, pk: boolean }> = [];
  childCols: Array<{ key: string, pk: boolean }> = [];

  constructor(tisService: TISService,
              modalService: NzModalService, private cdr: ChangeDetectorRef, private _iconService: NzIconService, drawerRef: NzDrawerRef<BasicSideBar>) {
    super(tisService, modalService, drawerRef);
    this._iconService.addIcon(DeleteOutline);
    // this.cdr.detach();
    // this.formDisabled = true;
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
    if (!this.erRel.topologyName) {
      throw new Error(`param 'topologyName' can not be null`);
    }
    this.jsonPost('/offline/datasource.ajax?emethod=reflect_table_cols&action=offline_datasource_action&topology=' + this.erRel.topologyName,
      [{'key': this.erRel.sourceNode.nodeid}
        , {'key': this.erRel.targetNode.nodeid}])
      .then(result => {
        // console.log("ngAfterViewInit" + this.formDisabled);
        if (result.success) {
          let colsKey: Map<string /**table id*/, Array<{ key: string, pk: boolean }>> = new Map();
          let keySqlCols: Array<{ key: string; cols: Array<{ key: string, pk: boolean }> }> = result.bizresult;
          keySqlCols.forEach((r) => {
            let cols: Array<{ key: string, pk: boolean }> = [];
            r.cols.forEach((c) => {
              cols.push(c);
            });
            colsKey.set(r.key, cols);
          });

          this.parentCols = colsKey.get(this.erRel.targetNode.nodeid);
          this.childCols = colsKey.get(this.erRel.sourceNode.nodeid);
        }

        this.cdr.detectChanges();
      });
  }

  ngAfterContentInit(): void {
  }

  // 执行保存流程
  subscribeSaveClick(graph: any, $: any, nodeid: any, _: IDataFlowMainComponent): void {

    let old = graph.findById(nodeid);
    old.linkrule = {'linkKeyList': this.linkKeyList, 'cardinality': this.erRel.cardinality};
    // = this.linkKeyList;
    // console.log(old);

    this._closeSidebar(null);
  }

  initComponent(_: IDataFlowMainComponent, erRuleNode: ERRuleNode): void {
    this.erRel = erRuleNode;
    this.linkKeyList = erRuleNode.linkKeyList;
  }


  _toggleSidebar() {
  }

  _deleteNode() {
    let edgeId = this.erRel.rel.id;

    let node = this.g6Graph.findById(edgeId);
    this.g6Graph.removeItem(node);
    // this.parentComponent.joinNodeMap.delete(id);
    // this.refeshDependencyOption();
    this._closeSidebar(null);
  }

  addKeyLink() {
    // console.log("dddd");
    let tmp: LinkKey[] = [];
    this.linkKeyList.forEach((r) => {
      tmp.push(r);
    });
    tmp.push(new LinkKey(null, null));
    this.linkKeyList = tmp;
  }

  deleteLink() {
    let tmp: LinkKey[] = [];
    this.linkKeyList.forEach((r) => {
      if (!r.checked) {
        tmp.push(r);
      }
    });
    this.linkKeyList = tmp;
  }


  parentKeyChange(link: LinkKey, parentKey: any) {

    if (this.childCols.findIndex((r) => {
      // console.log(r);
      return r.key === parentKey;
    }) > -1) {
      link.childKey = parentKey;
    }
  }
}







