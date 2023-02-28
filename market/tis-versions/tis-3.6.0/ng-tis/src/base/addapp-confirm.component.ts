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

/**
 * Created by baisui on 2017/5/22 0022.
 */
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {BasicFormComponent} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';
import {AppDesc, ConfirmDTO, CoreNodeCandidate, SchemaField} from './addapp-pojo';
import {Router} from '@angular/router';
import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  // templateUrl: '/runtime/addapp_confirm.htm'
  template: `
      <nz-back-top></nz-back-top>
      <tis-steps [type]="'createIndex'" step="3"></tis-steps>

      <nz-spin [nzSpinning]="formDisabled" nzSize="large">
          <form>
              <tis-page-header [result]="result" [showBreadcrumb]="false">
                  <tis-header-tool>
                      <button nz-button (click)="gotoSchemaConfigStep()"><i nz-icon nzType="backward" nzTheme="outline"></i>上一步</button>
                      <button nz-button nzType="primary" (click)="createIndex()"><i class="fa fa-rocket" aria-hidden="true"></i>创建索引</button>
                  </tis-header-tool>
              </tis-page-header>
              <!--
                            <h4>基本信息</h4>
                            <table class="table table-bordered">
                                <tr>
                                    <td align="right" width="15%">索引名称</td>
                                    <td width="35%">search4{{appform.name}}</td>
                                    <td></td>
                                    <td width="35%"></td>
                                </tr>
                                <tr>
                                    <td align="right">数据流</td>
                                    <td>{{appform.workflow}}</td>
                                    <td align="right">所属部门</td>
                                    <td>{{appform.checkedDptName}}</td>
                                </tr>
                                <tr>
                                    <td align="right">接口人</td>
                                    <td>{{appform.recept}}</td>
                                    <td align="right">配置模板</td>
                                    <td>{{appform.tisTpl}}</td>
                                </tr>
                                <tr>
                                    <td align="right"><i class="fa fa-server" aria-hidden="true"></i>服务器节点</td>
                                    <td colspan="3">
                                        {{coreNodeCandidate.hostName}}(已部署{{coreNodeCandidate.solrCoreCount}}个Core节点)
                                    </td>
                                </tr>
                            </table>
              -->
              <h4>基本信息</h4>
              <nz-descriptions  nzBordered>
                  <nz-descriptions-item nzTitle="索引名称">search4{{appform.name}}</nz-descriptions-item>
                  <nz-descriptions-item nzTitle="数据流">{{appform.workflow}}</nz-descriptions-item>
                  <nz-descriptions-item nzTitle="所属部门">{{appform.checkedDptName}}</nz-descriptions-item>
                  <nz-descriptions-item nzTitle="接口人">{{appform.recept}}</nz-descriptions-item>
                  <nz-descriptions-item nzTitle="拓扑结构">
                      Shard:{{coreNodeCandidate.shardCount}}组,Replica:{{coreNodeCandidate.replicaCount}}副本 <br>
                      节点
                      <nz-tag *ngFor="let t of coreNodeCandidate.hosts">{{t.hostName}}</nz-tag>
                  </nz-descriptions-item>
              </nz-descriptions>
              <br/>
              <h4>索引结构</h4>
              <nz-table #fieldlist nzSize="small" [nzData]="fields" [nzShowPagination]="false" [nzFrontPagination]="false">
                  <thead class="thead-default">
                  <tr>
                      <th>#</th>
                      <th>名称</th>
                      <th><span data-placement="top"
                                data-toggle="tooltip">字段类型</span></th>
                      <th>可查询</th>
                      <th>存储</th>
                      <th>扩展属性</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr *ngFor="let f of fieldlist.data">
                      <td>
                          <span>{{f.index}}</span>
                          <i *ngIf="f.uniqueKey" class="fa fa-key" aria-hidden="true"></i>
                          <i *ngIf="f.sharedKey" class="fa fa-share-alt" aria-hidden="true"></i>
                      </td>
                      <td>
                          <label>{{f.name}}</label>
                      </td>
                      <td>
      <span *ngIf="f.split">
       {{f.tokenizerType}}
      </span><span *ngIf="!f.split">
        {{f.fieldtype}}
      </span>
                      </td>
                      <td>
                          <i *ngIf="f.indexed" class="fa fa-check-square" aria-hidden="true"></i>
                      </td>
                      <td>
                          <i *ngIf="f.stored" class="fa fa-check-square" aria-hidden="true"></i>
                      </td>
                      <td>
                          <i *ngIf="f.docval" class="fa fa-check-square" aria-hidden="true">可排序</i>
                          <i *ngIf="f.multiValue" class="fa fa-check-square" aria-hidden="true">可多值</i>
                      </td>
                  </tr>
                  </tbody>
              </nz-table>
          </form>
      </nz-spin>
  `
  , styles: [`
        [nz-button] {
            margin-right: 8px;
        }
  `]
})
export class AddAppConfirmComponent extends BasicFormComponent implements OnInit {
  // Schema编辑中传递过来的提交信息
  @Input() dto: ConfirmDTO;
  @Output('preStep') preStep = new EventEmitter<any>();
  fields: SchemaField[];
  appform: AppDesc;

  constructor(tisService: TISService, private router: Router, modalService: NzModalService) {
    super(tisService, modalService);
  }

  public get coreNodeCandidate(): CoreNodeCandidate {
    return this.dto.coreNode || new CoreNodeCandidate();
  }

  ngOnInit(): void {
    // console.log(this.dto);
    this.fields = this.dto.stupid.model.fields;
    this.appform = this.dto.appform;
  }

  // 执行索引创建
  public createIndex(): void {
    this.jPost(
      `/runtime/addapp.ajax?action=add_app_action&emethod=${this.dto.recreate ? "create_collection" : "advance_add_app"}`
      , this.dto).then((r) => {
      this.processResultWithTimeout(r, 3000, () => {
        if (r.success) {
          // 跳转到/applist列表
          // this.router.navigate(['t/base/applist']);

          this.router.navigate([`/c/search4${this.appform.name}`]);
        }
      });
    });
  }

  // 返回到schema编辑页面
  public gotoSchemaConfigStep(): void {
    return this.preStep.emit(this.dto);
  }
}
