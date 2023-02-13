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
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {BasicFormComponent} from '../common/basic.form.component';

import {Pager} from '../common/pagination.component';
import {NzModalService} from "ng-zorro-antd/modal";
import {Application, AppType} from "../common/application";
import {LatestSelectedIndex} from "../common/LatestSelectedIndex";
import {LocalStorageService} from "angular-2-local-storage";


// 全局配置文件
@Component({
  template: `
      <form>
          <tis-page-header title="实例">
              <tis-header-tool>
                  <button nz-button nzType="primary" nz-dropdown [nzDropdownMenu]="menu"><i class="fa fa-plus" aria-hidden="true"></i>添加<i nz-icon nzType="down"></i></button>
                  <nz-dropdown-menu #menu="nzDropdownMenu">
                      <ul nz-menu>
                          <li nz-menu-item>
                              <a routerLink="/base/appadd" >Solr实例</a>
                          </li>
                          <li nz-menu-item>
                              <a routerLink="/base/dataxadd">数据管道</a>
                          </li>
                      </ul>
                  </nz-dropdown-menu>
              </tis-header-tool>
          </tis-page-header>
          <tis-page [rows]="pageList" [pager]="pager" [spinning]="formDisabled" (go-page)="gotoPage($event)">
              <tis-col title="实例名称" width="14" (search)="filterByAppName($event)">
                  <ng-template let-app='r'>
                      <button nz-button nzType="link" (click)="gotoApp(app)">{{app.projectName}}</button>
                  </ng-template>
              </tis-col>
              <tis-col title="实例类型">
                  <ng-template let-app="r">
                      <ng-container [ngSwitch]="app.appType">
                          <nz-tag *ngSwitchCase="1" [nzColor]="'processing'">Solr</nz-tag>
                          <nz-tag *ngSwitchCase="2" [nzColor]="'processing'">DataX</nz-tag>
                      </ng-container>
                      <!--
                      <a [routerLink]="['/offline/wf_update',app.dataflowName]">{{app.dataflowName}}</a>
                     -->
                  </ng-template>
              </tis-col>
              <tis-col title="接口人" width="14" field="recept"></tis-col>
              <tis-col title="归属部门" field="dptName">
                  <ng-template let-app='r'>
   <span style="color:#999999;" [ngSwitch]="app.dptName !== null">
   <i *ngSwitchCase="true">{{app.dptName}}</i>
   <i *ngSwitchDefault>未设置</i></span>
                  </ng-template>
              </tis-col>
              <tis-col title="创建时间" width="20">
                  <ng-template let-app='r'> {{app.createTime | date : "yyyy/MM/dd HH:mm:ss"}}</ng-template>
              </tis-col>
          </tis-page>
      </form>
  `
})
export class ApplistComponent extends BasicFormComponent implements OnInit {

  // allrowCount: number;
  pager: Pager = new Pager(1, 1);
  pageList: Array<Application> = [];


  constructor(tisService: TISService, private router: Router, private route: ActivatedRoute, modalService: NzModalService, private _localStorageService: LocalStorageService
  ) {
    super(tisService, modalService);
  }


  ngOnInit(): void {
    this.route.queryParams.subscribe((param) => {

      let nameQuery = '';
      for (let key in param) {
        nameQuery += ('&' + key + '=' + param[key]);
      }
      this.httpPost('/runtime/applist.ajax'
        , 'emethod=get_apps&action=app_view_action' + nameQuery)
        .then((r) => {
          if (r.success) {
            this.pager = Pager.create(r);
            this.pageList = r.bizresult.rows;
          }
        });
    })
  }

  public gotoPage(p: number) {

    Pager.go(this.router, this.route, p);
  }


  // 跳转到索引维护页面
  public gotoAppManage(app: { appId: number }): void {

    this.httpPost('/runtime/changedomain.ajax'
      , 'event_submit_do_change_app_ajax=y&action=change_domain_action&selappid=' + app.appId)
      .then(result => {
        this.router.navigate(['/corenodemanage']);
      });

  }

  public gotAddIndex(): void {
    this.router.navigate(['/base/appadd']);
  }

  /**
   * 使用索引名称来进行查询
   * @param query
   */
  filterByAppName(data: { query: string, reset: boolean }) {
    // console.log(query);
    Pager.go(this.router, this.route, 1, {name: data.reset ? null : data.query});
  }

  gotoApp(app: Application) {
   // console.log(app);
    // <a [routerLink]="['/c',app.projectName]">{{app.projectName}}</a>
    // if (app.appType === AppType.Solr) {
    //   this.router.navigate(['/c', app.projectName]);
    // } else if (app.appType === AppType.DataX) {
    //   this.router.navigate(['/x', app.projectName]);
    // }
    // _localStorageService: LocalStorageService, r: Router, app: Application
    LatestSelectedIndex.routeToApp(this._localStorageService, this.router, app);
  }
}
