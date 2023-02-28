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

import {TISService} from '../common/tis.service';
import {Component, Input, OnInit} from '@angular/core';
import {BasicFormComponent} from '../common/basic.form.component';

import {NzModalRef, NzModalService} from "ng-zorro-antd/modal";
import {Item} from "../common/tis.plugin";


// 部门管理
@Component({
  // templateUrl: '/runtime/bizdomainlist.htm'
  template: `
      <tis-form [fieldsErr]="errorItem" [spinning]="this.formDisabled">
          <tis-page-header [showBreadcrumb]="false" [result]="result">
              <tis-header-tool>
                  <button nz-button nzType="primary" (click)="submitDptForm()">提交</button>
              </tis-header-tool>
          </tis-page-header>
          <tis-ipt title="业务线名称" name="parentId" require="true">
              <nz-select [nzDropdownRender]="renderTemplate" [(ngModel)]="model.parentId" nzPlaceHolder="请选择">
                  <nz-option *ngFor=" let b of bizlines" [nzValue]="b.dptId" [nzLabel]="b.name"></nz-option>
              </nz-select>
          </tis-ipt>
          <tis-ipt #dptName title="部门名称" name="name" require="true">
              <input required type="text" [id]="dptName.name" nz-input [(ngModel)]="model.name" [name]="dptName.name"/>
          </tis-ipt>
      </tis-form>
      <ng-template #renderTemplate>
          <nz-divider></nz-divider>
          <div class="container">
              <input type="text" nz-input #inputElement/>
              <a class="add-item" (click)="addItem(inputElement)"><i nz-icon nzType="plus"></i>添加</a>
          </div>
      </ng-template>

  `,
  styles: [
      `
          nz-divider {
              margin: 4px 0;
          }

          .container {
              display: flex;
              flex-wrap: nowrap;
              padding: 8px;
          }

          input {
          }

          .add-item {
              flex: 0 0 auto;
              padding: 8px;
              display: block;
          }
    `

  ]
})
export class DepartmentAddComponent extends BasicFormComponent implements OnInit {
  errorItem: Item = Item.create([]);
  @Input()
  model: any = {};
  bizlines: Array<BizLine> = [];

  constructor(tisService: TISService, private activeModal: NzModalRef) {
    super(tisService);
  }

  get updateModel(): boolean {
    return this.model.dptId > 0;
  }

  ngOnInit(): void {
    this.httpPost('/runtime/bizdomainlist.ajax',
      'action=bizline_action&emethod=getBizline')
      .then((r) => {
        if (r.success) {
          this.bizlines = r.bizresult;
        }
      });
  }

  submitDptForm() {
    this.jsonPost(`/runtime/addapp.ajax?action=department_action&emethod=${this.updateModel ? 'update' : 'add'}Department`
      , this.model)
      .then((r) => {
        this.processResult(r);
        if (r.success) {
          setTimeout(() => {
            this.activeModal.close(r.bizresult);
          }, 1000)
        } else {
          this.errorItem = Item.processFieldsErr(r);
        }
      });
  }

  addItem(input: HTMLInputElement) {
    const value = input.value;

    this.httpPost('/runtime/addapp.ajax'
      , 'action=bizline_action&emethod=addBizline&name=' + value)
      .then((r) => {

        if (r.success) {
          // console.log(this.bizlines);
          let newBizline = [{dptId: r.bizresult.dptId, name: r.bizresult.name}];
          this.bizlines.forEach((rr) => {
            newBizline.push(rr);
          });
          this.bizlines = newBizline;
        } else {
          this.processResult(r);
        }
      });
  }
}

interface BizLine {
  dptId: number;
  name: string;
}
