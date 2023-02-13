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

import {AfterContentInit, AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {BasicSideBar, IDataFlowMainComponent} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';
// @ts-ignore
// import * as $ from 'jquery';
import 'codemirror/mode/sql/sql.js';
import 'codemirror/lib/codemirror.css';
import {EditorConfiguration, fromTextArea} from 'codemirror';
import {NzModalService} from "ng-zorro-antd/modal";
import {NzDrawerRef} from "ng-zorro-antd/drawer";


@Component({
  template: `
      <div>
          <sidebar-toolbar (close)="_closeSidebar($event)"
                           (save)="_saveClick()" (delete)="_deleteNode()"></sidebar-toolbar>

          <form class="clear" nz-form [nzLayout]="'vertical'">

              <p class="item-head"><label>依赖表</label></p>
              <div>
                  <nz-select name="depsTab" nzMode="tags" style="width: 100%;" nzPlaceHolder="Tag Mode"
                             [(ngModel)]="listOfTagOptions">
                      <nz-option *ngFor="let option of listOfOption" [nzLabel]="option.label"
                                 [nzValue]="option.value"></nz-option>
                  </nz-select>
              </div>
              <p class="item-head"><label>SQL</label></p>
              <div id="sqleditorBlock">
                  <textarea #sqleditor name="code-html"></textarea>
              </div>


          </form>

      </div>

  `,

  styles: [
      `
          .CodeMirror {
              width: 100%;
              height: 600px;
              border: #2f2ded;
          }

          .item-head {
              margin: 20px 0px 0px 0px;
          }

          #sqleditorBlock {
              width: 100%;
          }

          .clear {
              clear: both;
          }
    `]
})
// JOIN 节点设置
export class WorkflowAddUnionComponent
  extends BasicSideBar implements OnInit, AfterContentInit, AfterViewInit {


  @ViewChild('sqleditor', {static: false}) sqleditor: ElementRef;
  listOfOption: Array<{ label: string; value: string }> = [];
  listOfTagOptions: any[] = [];

  constructor(tisService: TISService, //
              modalService: NzModalService, drawerRef: NzDrawerRef<BasicSideBar>) {
    super(tisService, modalService, drawerRef);
  }

  ngOnInit(): void {
    const children: Array<{ label: string; value: string }> = [];
    for (let i = 10; i < 36; i++) {
      children.push({label: i.toString(36) + i, value: i.toString(36) + i});
    }
    this.listOfOption = children;
  }


  ngAfterViewInit(): void {
    let sqlmirror = fromTextArea(this.sqleditor.nativeElement, this.sqleditorOption);
    sqlmirror.setValue("select * from mytable;");
  }

  ngAfterContentInit(): void {
  }

  private get sqleditorOption(): EditorConfiguration {
    return {
      mode: "text/x-hive",
      lineNumbers: true,
    };
  }

  subscribeSaveClick(graph: any, $: any, model: any, _: IDataFlowMainComponent): void {

  }

  initComponent(_: IDataFlowMainComponent): void {
  }


  _deleteNode() {
  }
}





