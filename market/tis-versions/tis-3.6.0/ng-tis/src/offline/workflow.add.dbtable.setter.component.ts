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

import {
  AfterContentInit,
  AfterViewInit,
  Component,
  OnInit
} from '@angular/core';
import {BasicSideBar, DumpTable, IDataFlowMainComponent} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';


/*
["defaults", "optionHandlers", "defineInitHook", "defineOption", "Init", "helpers",
"registerHelper", "registerGlobalHelper", "inputStyles", "defineMode", "defineMIME",
"defineExtension", "defineDocExtension", "fromTextArea", "off", "on", "wheelEventPixels",
 "Doc", "splitLines", "countColumn", "findColumn", "isWordChar", "Pass", "signal", "Line",
  "changeEnd", "scrollbarModel", "Pos", "cmpPos", "modes", "mimeModes", "resolveMode",
  "getMode", "modeExtensions", "extendMode", "copyState", "startState", "innerMode",
  "commands", "keyMap", "keyName", "isModifierKey", "lookupKey", "normalizeKeyMap",
   "StringStream", "SharedTextMarker", "TextMarker", "LineWidget", "e_preventDefault",
    "e_stopPropagation", "e_stop", "addClass", "contains", "rmClass", "keyNames", "version"]
*/
// import {EditorConfiguration, fromTextArea} from 'codemirror';
import {WorkflowAddComponent} from "./workflow.add.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzDrawerRef} from "ng-zorro-antd/drawer";


//
@Component({
  template: `
      <nz-spin [nzSpinning]="formDisabled" nzSize="large">

          <sidebar-toolbar (close)="_closeSidebar($event)" (save)="_saveClick()" (delete)="deleteNode()"></sidebar-toolbar>

          <!--          <form class="clear" nz-form [nzLayout]="'vertical'">-->
          <!--              <div class="item-head"><label>数据库表</label></div>-->
          <!--              <p>-->
          <!--                  &lt;!&ndash;                  <nz-cascader name="dbTable" class="clear" [nzOptions]="cascaderOptions" [(ngModel)]="cascadervalues"&ndash;&gt;-->
          <!--                  &lt;!&ndash;                               (ngModelChange)="onCascaderChanges($event)"></nz-cascader>&ndash;&gt;-->
          <!--                  <tis-table-select [(ngModel)]="cascadervalues" (onCascaderSQLChanges)="this.sql=$event"></tis-table-select>-->
          <!--              </p>-->

          <!--              <label>SQL</label>-->
          <!--              <div>-->
          <!--                  <tis-codemirror name="sqltext" [(ngModel)]="sql"-->
          <!--                                  [size]="{width:'100%',height:600}" [config]="sqleditorOption"></tis-codemirror>-->
          <!--              </div>-->
          <!--          </form>-->

      </nz-spin>
  `,

  styles: [
      `.clear {
          clear: both;
      }`]
})
export class WorkflowAddDbtableSetterComponent
  extends BasicSideBar implements OnInit, AfterContentInit, AfterViewInit {

  // cascaderOptions: NzCascaderOption[] = [];
  cascadervalues: any = {};
  private dto: DumpTable;
  sql = 'select * from usertable;';

  constructor(tisService: TISService, //
              modalService: NzModalService, drawerRef: NzDrawerRef<BasicSideBar>) {
    super(tisService, modalService, drawerRef);
  }

  ngOnInit(): void {
  }


  initComponent(_: WorkflowAddComponent, dumpTab: DumpTable): void {

    if (dumpTab.tabid) {
      this.cascadervalues = [dumpTab.dbid, dumpTab.cascaderTabId];
      this.sql = dumpTab.sqlcontent;
    }
    //  console.log(this.cascadervalues);
    this.dto = dumpTab;
  }


  ngAfterViewInit(): void {
  }

  ngAfterContentInit(): void {
  }

  get sqleditorOption(): any {
    return {
      'readOnly': true,
    };
  }

  onChanges(event: any) {

  }

  // onCascaderChanges(evt: any[]) {
  //
  //   let tabidtuple = evt[1].split('%');
  //   let action = `emethod=get_datasource_table_by_id&action=offline_datasource_action&id=${tabidtuple[0]}`;
  //   this.httpPost('/offline/datasource.ajax', action)
  //     .then((result) => {
  //       let r = result.bizresult;
  //       this.sql = r.selectSql;
  //     });
  //
  // }

  // 点击保存按钮
  _saveClick() {

    let tab: string = this.cascadervalues[1];
    let tabinfo: string[] = tab.split('%');

    // console.log(this.dto);

    this.saveClick.emit(new DumpTable(this.nodeMeta, this.dto.nodeid
      , this.sql, this.cascadervalues[0], tabinfo[0], tabinfo[1]));
  }

  public subscribeSaveClick(graph: any, $: any, nodeid: string
    , addComponent: IDataFlowMainComponent, d: DumpTable): void {
    let old = graph.findById(nodeid);
    let nmodel = {'label': d.tabname, 'nodeMeta': d};

    // console.log(nodeid);
    // 更新label值
    graph.updateItem(old, nmodel);
    // console.log(old);
    // 将节点注册到map中存储起来
    // console.log(model.id);
    addComponent.dumpTabs.set(nodeid, d);
    addComponent.closePanel();
  }

  // 删除节点
  deleteNode() {

    let id = this.dto.nodeid;
    let node = this.g6Graph.findById(id);
    this.g6Graph.removeItem(node);
    this.parentComponent.dumpTabs.delete(id);
    this._closeSidebar(null);
  }
}







