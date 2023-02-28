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

import {Component, OnInit} from '@angular/core';
import {BasicFormComponent} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';
import {Router} from '@angular/router';
import {UserAddComponent} from './user.add.component';

import {Pager} from '../common/pagination.component';
import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  template: `
      <div class="container">

          <tis-page-header title="用户">
              <button class="btn btn-primary" (click)="usradd()"><i class="fa fa-plus" aria-hidden="true"></i>添加</button>
          </tis-page-header>

          <tis-page [rows]="usrs" [pager]="pager" (go-page)="gotoPage($event)">
              <tis-col title="账户" width="14" field="userName"></tis-col>
              <tis-col title="昵称" width="14" field="realName"></tis-col>
              <tis-col title="所属部门" field="dptName"></tis-col>
              <tis-col title="角色" width="20">
                  <ng-template let-u='r'>
   <span [ngSwitch]="u.roleId > -1">
   <i *ngSwitchCase="true">{{u.getRoleName}}</i>
   <i *ngSwitchDefault>初始账户</i></span></ng-template>
              </tis-col>
              <tis-col title="创建时间">
                  <ng-template let-u='r'>{{u.createTime|date : "yyyy/MM/dd HH:mm:ss"}}
                  </ng-template>
              </tis-col>
              <tis-col title="操作">
                  <ng-template let-u='r'>
                      <a href="javascript:void(0)"><i class="fa fa-pencil-square-o" aria-hidden="true"></i></a>
                  </ng-template>
              </tis-col>
          </tis-page>
      </div>
  `
})
export class UserListComponent extends BasicFormComponent implements OnInit {

  usrs: any[] = [];
  pager: Pager = new Pager(1, 2);

  constructor(tisService: TISService, private router: Router, modalService: NzModalService) {
    super(tisService, modalService);
  }


  ngOnInit(): void {
    // console.info( this.tisService.daily);
    this.gotoPage(1);
  }

  public gotoPage(page: number): void {
    this.httpPost('/runtime/usrlist.ajax'
      , 'action=user_action&emethod=get_init_data&page=' + page)
      .then((r) => {
        if (r.success) {
          this.usrs = r.bizresult.rows;
          this.pager = new Pager(r.bizresult.curPage, r.bizresult.totalPage);
        }
      });
  }

  public usradd(): void {
    this.openDialog(UserAddComponent, {nzTitle: "添加用户"});
  }
}
