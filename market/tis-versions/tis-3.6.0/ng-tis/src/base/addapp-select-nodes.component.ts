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

import {BasicFormComponent} from "../common/basic.form.component";
import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {TISService} from "../common/tis.service";
import {ConfirmDTO, StupidModal} from "./addapp-pojo";
import {   TransferItem} from "ng-zorro-antd/transfer";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService}  from "ng-zorro-antd/notification";


@Component({
  // templateUrl: '/runtime/addapp_confirm.htm'
  template: `
      <tis-steps [type]="'createIndex'" step="2"></tis-steps>
      <nz-spin [nzSpinning]="formDisabled" nzSize="large">
          <tis-page-header [showBreadcrumb]="false" [result]="result">
              <button nz-button nzType="default" (click)="gotoPreStep()"><i nz-icon nzType="backward" nzTheme="outline"></i>上一步</button>
              <button nz-button nzType="primary" (click)="createIndexConfirm()"><i
                      class="fa fa-check-square-o" aria-hidden="true"></i>提交确认
              </button>
          </tis-page-header>
          <p>
              <nz-input-group [nzAddOnBefore]="'Shard'" [nzAddOnAfter]="'组'">
                  <nz-select class="num-select" id="group" name="group" nzSize="large" nzPlaceHolder="请选择" [(ngModel)]="this.dto.coreNode.shardCount">
                      <nz-option *ngFor="let num of shards" [nzValue]="num" [nzLabel]="num"></nz-option>
                  </nz-select>
              </nz-input-group>
              &nbsp;
              <nz-input-group [nzAddOnBefore]="'Replica'" [nzAddOnAfter]="'副本'">
                  <nz-select class="num-select" id="replica" name="replica" nzPlaceHolder="请选择" nzSize="large" [(ngModel)]="this.dto.coreNode.replicaCount">
                      <nz-option *ngFor="let num of replica" [nzValue]="num" [nzLabel]="num"></nz-option>
                  </nz-select>
              </nz-input-group>
          </p>
          <nz-transfer
                  [nzDataSource]="list"
                  [nzDisabled]="disabled"
                  [nzShowSearch]="true"
                  [nzShowSelectAll]="true"
                  [nzRenderList]="[renderList, renderList]"
                  (nzSelectChange)="select($event)"
                  (nzChange)="change($event)"
          >
              <ng-template
                      #renderList
                      let-items
                      let-direction="direction"
                      let-stat="stat"
                      let-disabled="disabled"
                      let-onItemSelectAll="onItemSelectAll"
                      let-onItemSelect="onItemSelect"
              >
                  <nz-table #t [nzData]="convertItems(items)" nzSize="small">
                      <thead>
                      <tr>
                          <th
                                  nzShowCheckbox
                                  [nzDisabled]="disabled"
                                  [nzChecked]="stat.checkAll"
                                  [nzIndeterminate]="stat.checkHalf"
                                  (nzCheckedChange)="onItemSelectAll($event)"
                          ></th>
                          <th>Name</th>
                          <th *ngIf="direction === 'left'">lucene ver</th>
                          <th>Description</th>
                      </tr>
                      </thead>
                      <tbody>
                      <tr *ngFor="let data of t.data" (click)="onItemSelect(data)">
                          <td
                                  nzShowCheckbox
                                  [nzChecked]="data.checked"
                                  [nzDisabled]="disabled || data.disabled"
                                  (nzCheckedChange)="onItemSelect(data)"
                          ></td>
                          <td>{{ data.title }}</td>
                          <td *ngIf="direction === 'left'">
                              <nz-tag [nzColor]="'blue'">{{ data.tag }}</nz-tag>
                          </td>
                          <td>{{ data.description }}</td>
                      </tr>
                      </tbody>
                  </nz-table>
              </ng-template>
          </nz-transfer>
      </nz-spin>
  `
  , styles: [`
        [nz-button] {
            margin-right: 8px;
        }

        .num-select {
            width: 100px;
        }

        nz-input-group {
            width: 200px;
        }
  `]
})
export class AddappSelectNodesComponent extends BasicFormComponent implements OnInit {

  @Output('preStep') preStep = new EventEmitter<ConfirmDTO>();
  // 下一步 确认页面
  @Output('nextStep') nextStep = new EventEmitter<ConfirmDTO>();
  // 第一步中传递过来的提交信息
  @Input() dto: ConfirmDTO;


  get shards(): number[] {
    let result = [];
    for (let i = 1; i <= 16; i++) {
      result.push(i);
    }
    return result;
  }

  get replica(): number[] {
    let result = [];
    for (let i = 1; i <= 4; i++) {
      result.push(i);
    }
    return result;
  }

  // =======================================================================
  list: TransferItem[] = [];
  disabled = false;
  showSearch = false;

  ngOnInit(): void {
    // console.log(this.dto.coreNode.hosts);
    let url = '/runtime/addapp.ajax?action=add_app_action&emethod=get_selectable_node_list';
    this.jsonPost(url, {}).then((r) => {
      // console.log(r.bizresult);
      let nodelist: any = r.bizresult;
      if (!(nodelist instanceof Array)) {
        throw new Error("nodelist is not an Array");
      }


      nodelist.forEach((node) => {
        this.list.push({
          key: node.nodeName,
          title: node.hostName,
          description: `已部署:${node.solrCoreCount}c`,
          direction: (this.dto.coreNode.hosts.findIndex((host) => host.hostName === node.hostName) > -1 ? 'right' : 'left'),
          disabled: false,
          tag: `${node.luceneVersion}`
        });
        this.list = [...this.list];
      });
    })

    // for (let i = 0; i < 20; i++) {
    //   this.list.push({
    //     key: i.toString(),
    //     title: `content${i + 1}`,
    //     description: `description of content${i + 1}`,
    //     disabled: i % 4 === 0,
    //     tag: ['cat', 'dog', 'bird'][i % 3]
    //   });
    // }

    // [2, 3].forEach(idx => (this.list[idx].direction = 'right'));
  }

  convertItems(items: TransferItem[]): TransferItem[] {
    return items.filter(i => !i.hide);
    // return items;
  }

  select(ret: {}): void {
    //  console.log('nzSelectChange', ret);
  }

  change(ret: {}): void {
    // console.log('nzChange', ret);
  }

// =================================================================
  constructor(tisService: TISService, modalService: NzModalService, notification: NzNotificationService) {
    super(tisService, modalService, notification);
  }

  // ngOnInit(): void {
  // }

  createIndexConfirm() {
    let selectedItems = this.list.filter((item) => item.direction === 'right').map((item) => {
      return {hostName: item.key}
    })
    // console.log(selectedItems);
    if (selectedItems.length < 1) {
      this.errNotify('请选择机器节点');
      return;
    }
    this.formDisabled = true;
    this.dto.coreNode.hosts = selectedItems;
    // this.dto.coreNode.hosts.push()
    this.nextStep.emit(this.dto);
  }

  gotoPreStep() {
    this.formDisabled = true;
    this.preStep.emit(this.dto);
  }

}
