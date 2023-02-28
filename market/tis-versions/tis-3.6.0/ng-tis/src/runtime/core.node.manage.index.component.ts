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

import {Component, OnInit, ViewChild} from '@angular/core';
import {TISService} from '../common/tis.service';
import {RouterOutlet, ActivatedRoute, Params, Router} from '@angular/router';
import 'rxjs/add/operator/switchMap';
import {AppFormComponent, BasicFormComponent, CurrentCollection} from '../common/basic.form.component';

import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";

@Component({
  template: `
      <my-navigate [core]="this.currentApp"></my-navigate>
      <nz-layout class="main-layout">
          <nz-sider [nzWidth]="150" [nzTheme]="'light'">
              <ul nz-menu nzMode="inline">
                  <li nz-menu-item nzMatchRouter nzMatchRouterExact>
                      <a [routerLink]="['./']">
                          <i class="fa fa-tachometer fa-2x" aria-hidden="true"></i>主控台</a>
                  </li>
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./query">
                          <i class="fa fa-search fa-2x" aria-hidden="true"></i>查询</a>
                  </li>
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./snapshotset"><i class="fa fa-history fa-2x" aria-hidden="true"></i>配置变更</a>
                  </li>
<!--
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./plugin"><i class="fa fa-plug fa-2x" aria-hidden="true"></i>插件配置</a>
                  </li>
-->
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./incr_build">
                          <i aria-hidden="true" class="fa fa-truck fa-2x"></i>实时通道</a>
                  </li>

                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./app_build_history" ><i aria-hidden="true" class="fa fa-cog fa-2x"></i>全量构建</a>
                  </li>
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./monitor"><i class="fa fa-bar-chart fa-2x" aria-hidden="true"></i>监控</a>
                  </li>
<!--
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./membership"><i class="fa fa-users fa-2x" aria-hidden="true"></i>权限</a>
                  </li>
-->
                  <li nz-menu-item nzMatchRouter>
                      <a routerLink="./operationlog"><i class="fa fa-pencil fa-2x" aria-hidden="true"></i>操作历史</a>
                  </li>
              </ul>
          </nz-sider>
          <nz-content>
              <router-outlet></router-outlet>
          </nz-content>
      </nz-layout>
      <ng-template #zeroTrigger>
          <i nz-icon nzType="menu-fold" nzTheme="outline"></i>
      </ng-template>

  `,
  styles: [`
      a:link {
          color: black;
      }

      a:visited {
          color: black;
      }

      a:hover {
          color: #0275d8;
      }

      .main-layout {
          height: 92vh;
          clear: both;
      }

      nz-content {
          margin: 0 20px 0 10px;
      }
  `]
})
export class CorenodemanageIndexComponent extends AppFormComponent implements OnInit {
  // app: CurrentCollection = new CurrentCollection(0, '');


  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService, private router: Router) {
    super(tisService, route, modalService);
  }

  protected initialize(app: CurrentCollection): void {
  }

  // isSelected(url: string): boolean {
  //   return this.router.isActive(url, true);
  // }

  ngOnInit(): void {
    super.ngOnInit();
  }

// constructor(tisService: TISService, private router: Router, private route: ActivatedRoute, modalService: NzModalService) {
  //   super(tisService, modalService);
  // }

  // ngOnInit(): void {
  //   this.route.params
  //     .subscribe((params: Params) => {
  //       this.app = new CurrentCollection(0, params['name']);
  //       this.currentApp = this.app;
  //     });
  //  // this.router.isActive()
  //   // this.route.
  // }

// 控制页面上的 业务线选择是否要显示
  // get appSelectable(): boolean {
  //  // return this.tisService.isAppSelectable();
  // }


  gotoFullbuildView(e: MouseEvent) {
    // let url = `/offline/datasource.ajax`;
    // this.httpPost(url, 'action=offline_datasource_action&event_submit_do_get_workflowId=y').then((r) => {
      this.router.navigate(['./app_build_history'], {relativeTo: this.route});
    // });
    e.stopPropagation();
  }
}
