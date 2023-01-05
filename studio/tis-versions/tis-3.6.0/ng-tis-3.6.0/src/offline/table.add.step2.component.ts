// /**
//  *   Licensed to the Apache Software Foundation (ASF) under one
//  *   or more contributor license agreements.  See the NOTICE file
//  *   distributed with this work for additional information
//  *   regarding copyright ownership.  The ASF licenses this file
//  *   to you under the Apache License, Version 2.0 (the
//  *   "License"); you may not use this file except in compliance
//  *   with the License.  You may obtain a copy of the License at
//  *
//  *       http://www.apache.org/licenses/LICENSE-2.0
//  *
//  *   Unless required by applicable law or agreed to in writing, software
//  *   distributed under the License is distributed on an "AS IS" BASIS,
//  *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  *   See the License for the specific language governing permissions and
//  *   limitations under the License.
//  */
//
// /**
//  * Created by Qinjiu on 5/3/2017.
//  */
//
// import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
// import {TISService} from '../common/tis.service';
// import {TableAddStep} from './table.add.step';
// import {Router} from '@angular/router';
// import {Location} from '@angular/common';
//
// import {TablePojo} from './table.add.component';
// import {NzModalService} from "ng-zorro-antd/modal";
// import {Item, TisResponseResult} from "../common/tis.plugin";
//
// declare var jQuery: any;
//
//
// @Component({
//   selector: 'tableAddStep2',
//   template: `
//       <!--
//         <nz-spin [nzSpinning]="formDisabled" [nzSize]="'large'">
//             <tis-page-header [showBreadcrumb]="false" [result]="this.result">
//                 <button nz-button nzType="default" (click)="createPreviousStep(form)">上一步</button>&nbsp;
//                 <button nz-button nzType="primary" (click)="saveTableConfig(form)" [nzLoading]="this.formDisabled">提交</button>
//             </tis-page-header>
//             <form #form>
//                 <fieldset [disabled]='formDisabled'>
//                     <input type="hidden" name="event_submit_do_add_datasource_table" value="y"/>
//                     <input type="hidden" name="action" value="offline_datasource_action"/>
//                     <div class="form-group">
//                         <label for="selectsql">SELECT SQL</label>
//                         <textarea id="selectsql" class="form-control"
//                                   name="selectSql" rows="15" readonly placeholder="select * from table"
//                                   required (blur)="selectSqlChange(selectSql1.value)" #selectSql1
//                                   [(ngModel)]="step1Form.selectSql"></textarea>
//                     </div>
//                 </fieldset>
//             </form>
//         </nz-spin>
//   -->
//       <tis-form #form [fieldsErr]="errorItem" formLayout="vertical">
//           <tis-page-header [showBreadcrumb]="false" [result]="result">
//               <button nz-button nzType="default" (click)="createPreviousStep(form)">上一步</button>&nbsp;
//               <button nz-button nzType="primary" (click)="saveTableConfig(form)" [nzLoading]="this.formDisabled">提交</button>
//           </tis-page-header>
//           <tis-ipt #sql title="SELECT SQL" name="sql" require="true">
//               <tis-codemirror class="ant-input" [ngModel]="step1Form.selectSql" [config]="{readOnly:true}"></tis-codemirror>
//           </tis-ipt>
//       </tis-form>
//   `
// })
// export class TableAddStep2Component extends TableAddStep implements OnInit {
//   columns: ColumnTypes[] = [];
//   tableName: string;
//   errorItem: Item = Item.create([]);
//   @Input() tablePojo: TablePojo;
//
//   @Input() step1Form: TablePojo;
//   @Output() processSuccess: EventEmitter<TisResponseResult> = new EventEmitter();
//
//   constructor(public tisService: TISService, protected router: Router
//     , protected location: Location) {
//     super(tisService, router, location);
//   }
//
//
//   ngOnInit(): void {
//     // this.confirmDisable = this.tablePojo.isAdd;
//   }
//
//   saveTableConfig(form: any): void {
//     this.jsonPost('/offline/datasource.ajax?event_submit_do_add_datasource_table=y&action=offline_datasource_action', this.step1Form)
//       .then(result => {
//         this.processResult(result);
//         if (result.success) {
//           this.processSuccess.emit(result);
//         }
//       });
//   }
//
//   selectSqlChange(sql: any): void {
//     // this.confirmDisable = true;
//     // console.log(sql);
//     let action = 'event_submit_do_analyse_select_sql=y&action=offline_datasource_action'
//       + '&sql=' + sql + '&dbName='; // + this.step1Form.dbName.value;
//     // console.log(action);
//     this.httpPost('/offline/datasource.ajax', action)
//       .then(
//         result => {
//           this.columns = [];
//           this.processSuccess.emit(result);
//           // console.log(result);
//           if (result.success) {
//             // this.confirmDisable = false;
//             this.columns = [];
//             this.tableName = result.bizresult.tableName;
//             for (let column of result.bizresult.columns) {
//               this.columns.push(new ColumnTypes(column.key, column.dbType, column.hiveType));
//             }
//           } else {
//             // console.log('failed------------');
//             // console.log(result);
//
//           }
//         });
//   }
//
//   focusFunction(sql: any): void {
//     // console.log('sql');
//   }
//
//   focusOutFunction(sql: any): void {
//     // console.log('sql1');
//   }
//
//   testTableConnection(form: any): void {
//   }
// }
//
// export class ColumnTypes {
//   name: string;
//   dbType: string;
//   hiveType: string;
//
//   constructor(name: string,
//               dbType: string,
//               hiveType: string) {
//     this.name = name;
//     this.dbType = dbType;
//     this.hiveType = hiveType;
//   }
//
// }
