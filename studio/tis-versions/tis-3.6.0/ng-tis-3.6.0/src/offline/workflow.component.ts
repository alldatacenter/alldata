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
 * Created by baisui on 2017/3/29 0029.
 */
import {Component, OnInit} from '@angular/core';
import {TISService} from '../common/tis.service';
import {BasicFormComponent} from '../common/basic.form.component';
import {ActivatedRoute, Router} from '@angular/router';
import {Pager} from "../common/pagination.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";

export class BasicWFComponent extends BasicFormComponent {
  constructor(tisService: TISService, modalService: NzModalService, protected router: Router, protected route: ActivatedRoute, notification?: NzNotificationService) {
    super(tisService, modalService, notification);
  }


  buildHistory(dataflow: Dataflow): void {
    // console.log(dataflow);
    this.router.navigate(
      [`/offline/wf/build_history/`, dataflow.id], {relativeTo: this.route});
  }

  executeWorkflow(dataflow: Dataflow): void {
    let action = `event_submit_do_execute_workflow=y&action=offline_datasource_action&id=${dataflow.id}`;
    this.httpPost('/offline/datasource.ajax', action)
      .then(d => {
        if (d.success) {
          if (d.bizresult) {
            let taskid = d.bizresult.taskid;
            let msg: any = [];
            msg.push({
              'content': '数据流构建已经触发'
              , 'link': {'content': `查看构建状态(${taskid})`, 'href': `./build_history/${dataflow.id}/${taskid}`}
            });

            this.processResult({success: true, 'msg': msg});
          } else {
           // alert("重复触发了");
            this.errNotify("重复触发了");
          }
        } else {
          this.processResult(d);
        }
      });
  }
}


@Component({
  // templateUrl: '/offline/workflowList.htm'
  template: `
      <tis-page-header title="数据流" [result]="result">
          <tis-header-tool>
              <button nz-button nzType="primary" (click)="addWorkflowBtnClick()">
                  <i nz-icon nzType="plus" nzTheme="outline"></i>创建
              </button>
          </tis-header-tool>
      </tis-page-header>

      <tis-page [rows]="workflows" [pager]="pager" [spinning]="formDisabled" (go-page)="gotoPage($event)">
          <tis-col title="名称" width="14">
              <ng-template let-df='r'>
                  <a [routerLink]="['/offline','wf_update',df.name]">{{df.name}}</a>
              </ng-template>
          </tis-col>
          <tis-col title="状态" width="14" [field]="'state'">
          </tis-col>
          <tis-col title="更新时间" width="14">
              <ng-template let-df='r'>
                  {{df.opTime | date: 'yyyy/MM/dd HH:mm'}}
              </ng-template>
          </tis-col>
          <tis-col title="编辑者" width="14">
              <ng-template let-df='r'>
                  {{df.opUserName}}
              </ng-template>
          </tis-col>
          <tis-col title="操作" width="14">
              <ng-template let-df='r'>
                  <button nz-button nz-dropdown nzShape="circle" [nzDropdownMenu]="menu"
                          nzPlacement="bottomLeft"><i nz-icon nzType="more" nzTheme="outline"></i></button>
                  <nz-dropdown-menu #menu="nzDropdownMenu">
                      <ul nz-menu>
                          <li nz-menu-item (click)="editTopology(df)"><i nz-icon nzType="edit" nzTheme="outline"></i>编辑</li>
                          <li nz-menu-item (click)="executeWorkflow(df)"><i nz-icon nzType="play-circle" nzTheme="outline"></i>构建</li>
                          <li nz-menu-item (click)="buildHistory(df)"><i nz-icon nzType="snippets" nzTheme="outline"></i>构建历史</li>
                      </ul>
                  </nz-dropdown-menu>
              </ng-template>
          </tis-col>
      </tis-page>
  `
})
// 工作流
export class WorkflowComponent extends BasicWFComponent implements OnInit {

  workflows: Dataflow[];
  pager: Pager = new Pager(1, 1, 0);

  // formDisabled: boolean = false;

  constructor(protected tisService: TISService, modalService: NzModalService, router: Router, route: ActivatedRoute) {
    super(tisService, modalService, router, route);
  }


  ngOnInit(): void {
    // 查询所有的工作流
    this.route.queryParams.subscribe((params) => {
      let action = `event_submit_do_get_workflows=y&action=offline_datasource_action&page=${params['page']}`;
      this.httpPost('/offline/datasource.ajax', action)
        .then(result => {
          // console.log(result);
          this.initWorkflows(result.bizresult.rows);
          this.pager = Pager.create(result);
        });
    })

  }

  // testmsg(): void {
  //
  //   let msg: any = [];
  //   msg.push({
  //     'content': '全量索引构建已经触发'
  //     , 'link': {'content': '状态日志:' + 12345, 'href': './buildprogress/' + 12345}
  //   });
  //   this.processResult({success: true, 'msg': msg});
  // }

  public processResultPublic(result: any): void {
    this.processResult(result);
  }

  public initWorkflows(result: any): void {
    this.workflows = [];
    for (let workflow of result) {
      let workflow1 = new Dataflow();
      workflow1.id = workflow.id;
      workflow1.name = workflow.name;
      workflow1.opUserName = workflow.opUserName;
      workflow1.gitPath = workflow.gitPath;
      workflow1.opTime = new Date();
      workflow1.opTime.setTime(workflow.opTime);
      workflow1.inChange = workflow.inChange;

      // if (workflow1.inChange === 0) {
      //   workflow1.state = '已发布';
      // } else if (workflow1.inChange === 1) {
      //   workflow1.state = '新建';
      // } else {
      //   workflow1.state = '变更中';
      // }
      this.workflows.push(workflow1);
    }
    // console.log(this.workflows);
  }

  addWorkflowBtnClick(): void {
    this.router.navigate(['/offline/wf_add']);
  }

  deleteWorkflow(id: number): void {
    // console.log(id);
    let action = 'emethod=delete_workflow&action=offline_datasource_action&id=' + id;
    this.httpPost('/offline/datasource.ajax', action)
      .then(result => {
        // console.log(result);
        this.processResult(result);
        if (result.success) {
          this.initWorkflows(result.bizresult);
        }
      });
  }

  deleteWorkflowChange(id: number): void {
    // console.log(id);
    let action = 'event_submit_do_delete_workflow_change=y&action=offline_datasource_action&id=' + id;
    this.httpPost('/offline/datasource.ajax', action)
      .then(result => {
        console.log(result);
        this.processResult(result);
        if (result.success) {
          // this.initWorkflows(result.bizresult);
          this.goToWorkflowChange();
        }
      });
  }

  confirmWorkflowChange(id: number): void {
    console.log(id);
    let action = 'event_submit_do_confirm_workflow_change=y&action=offline_datasource_action&id=' + id;
    this.httpPost('/offline/datasource.ajax', action)
      .then(result => {
        console.log(result);
        this.processResult(result);
        if (result.success) {
          this.goToWorkflowChange();
          // this.initWorkflows(result.bizresult);
        }
      });
  }

  goToWorkflowChange(): void {
    this.router.navigate(['/offline/wf_change_list']);
  }

  editTopology(workflow: Dataflow) {
    this.router.navigate([`/offline/wf_update/${workflow.name}`]);
  }

  gotoPage(page: number) {
    Pager.go(this.router, this.route, page);
  }

  // testTransaction() {
  //
  //   let action = `event_submit_do_test_transaction=y&action=offline_datasource_action`;
  //   this.httpPost('/offline/datasource.ajax', action)
  //     .then(d => {
  //     });
  //
  // }
}


export class Dataflow {
  private _id: number;
  private _name: string;
  // private _opUserId: number;
  private _opUserName: string;
  private _gitPath: string;
  // private _createTime: Date;
  private _opTime: Date;
  private _inChange: number;
  private _state: string;

  constructor() {
  }

  get id(): number {
    return this._id;
  }

  set id(value: number) {
    this._id = value;
  }

  get name(): string {
    return this._name;
  }

  set name(value: string) {
    this._name = value;
  }

  get opUserName(): string {
    return this._opUserName;
  }

  set opUserName(value: string) {
    this._opUserName = value;
  }

  get gitPath(): string {
    return this._gitPath;
  }

  set gitPath(value: string) {
    this._gitPath = value;
  }

  // get createTime(): string {
  //   return this._createTime;
  // }
  //
  // set createTime(value: string) {
  //   this._createTime = value;
  // }

  get opTime(): Date {
    return this._opTime;
  }

  set opTime(value: Date) {
    this._opTime = value;
  }

  get inChange(): number {
    return this._inChange;
  }

  set inChange(value: number) {
    this._inChange = value;
  }


  get state(): string {
    return this._state;
  }

  set state(value: string) {
    this._state = value;
  }
}
