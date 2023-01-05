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
import {ActivatedRoute, Router} from '@angular/router';
import {LocalStorageService} from "angular-2-local-storage";
import {LatestSelectedIndex, SelectedIndex} from "../common/LatestSelectedIndex";
import {Application} from "../common/application";

@Component({
  template: `
      <my-navigate></my-navigate>
      <nz-layout style="overflow-x: hidden">
          <nz-layout>
              <nz-content style=" opacity: 0.8;">
                  <div nz-row [nzGutter]="8">
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true" (click)="gotoIndexList()">
                              <div class="tis-card-content">
                                  <div class="compose">
                                      <h1>实例</h1>
                                      dataX,Solr
                                  </div>
                              </div>
                          </nz-card>
                      </div>
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true" (click)="routerTo('/offline/wf')">
                              <div class="tis-card-content">
                                  <div class="compose">
                                      <h1><i nz-icon nzType="import" nzTheme="outline"></i>数据流</h1>
                                      轻松构建基于TIS的物化视图
                                  </div>
                              </div>
                          </nz-card>
                      </div>
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true" (click)="routerTo('/offline/ds')">
                              <div class="tis-card-content">
                                  <div class="compose">
                                      <h1><i nz-icon nzType="database" nzTheme="outline"></i>数据源</h1>
                                      所有的一切从定义数据源开始
                                  </div>
                              </div>
                          </nz-card>
                      </div>
                  </div>
                  <div nz-row [nzGutter]="8">
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true" (click)="routerTo('/base/basecfg')">
                              <div class="tis-card-content">
                                  <div class="compose">
                                      <h1><i nz-icon nzType="setting" nzTheme="outline"></i>插件配置</h1>
                                      插件参数配置实例化
                                  </div>
                              </div>
                          </nz-card>
                      </div>
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true" (click)="routerTo('/base/operationlog')">
                              <div class="tis-card-content">
                                  <div class="compose">
                                      <h1><i nz-icon nzType="snippets" nzTheme="outline"></i>操作日志</h1>
                                      记录用户在TIS平台的操作流水
                                  </div>
                              </div>
                          </nz-card>
                      </div>
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true">
                              <div class="tis-card-content">
                                  <h1 class="compose"><i nz-icon nzType="user" nzTheme="outline"></i>会员</h1>
                              </div>
                          </nz-card>
                      </div>
                  </div>
                  <div nz-row [nzGutter]="8">
                      <div nz-col nzSpan="8">
                          <nz-card [nzHoverable]="true" (click)="routerTo('/base/plugin-manage')">
                              <div class="tis-card-content">
                                  <div class="compose">
                                      <h1><i nz-icon nzType="api" nzTheme="outline"></i>插件池</h1>
                                      从<i>插件池</i>中选择需要的插件为我所用
                                  </div>
                              </div>
                          </nz-card>
                      </div>
                  </div>
              </nz-content>
              <nz-sider [nzWidth]="400">
                  <nz-list [nzDataSource]="_latestSelected" nzBordered [nzItemLayout]="'horizontal'" [nzHeader]="recentusedindex">
                      <nz-list-item *ngFor="let item of _latestSelected">
                          <span nz-typography><mark>{{item.name}}</mark></span>
                          <button nz-button nzType="link" (click)="enterIndex(item)">进入</button>
                      </nz-list-item>
                  </nz-list>
                  <ng-template #recentusedindex>
                      <nz-page-header class="recent-using-tool" [nzGhost]="false">
                          <nz-page-header-title>最近使用</nz-page-header-title>
                          <nz-page-header-extra>
                              <button nz-button [nzSize]="'small'" nzType="primary" nz-dropdown [nzDropdownMenu]="menu"><i class="fa fa-plus" aria-hidden="true"></i>添加<i nz-icon nzType="down"></i></button>
                              <nz-dropdown-menu #menu="nzDropdownMenu">
                                  <ul nz-menu>
                                      <li nz-menu-item>
                                          <a routerLink="/base/appadd">Solr实例</a>
                                      </li>
                                      <li nz-menu-item>
                                          <a routerLink="/base/dataxadd">数据管道</a>
                                      </li>
                                  </ul>
                              </nz-dropdown-menu>
                          </nz-page-header-extra>
                      </nz-page-header>
                  </ng-template>
              </nz-sider>
          </nz-layout>
          <nz-footer>
              <button nz-button nzType="link" (click)="companyIntrShow=true">杭州晴朗网络科技有限公司©2020</button>
          </nz-footer>
      </nz-layout>
      <!--https://market.aliyun.com/qidian/company/1180716023102499578-->
      <nz-drawer [nzClosable]="true" [nzHeight]="500" [nzVisible]="companyIntrShow" [nzPlacement]="'bottom'" nzTitle="杭州晴朗网络科技有限公司版权所有" (nzOnClose)="companyIntrShow=false">
          <ng-container *nzDrawerContent>
              <div nz-row [nzGutter]="8">
                  <div nz-col nzSpan="8">
                      <nz-card nzTitle="相关">
                          <nz-descriptions [nzColumn]="1">
                              <nz-descriptions-item [nzTitle]="githubRef"><a target="_blank" href="https://github.com/qlangtech/tis">https://github.com/qlangtech/tis</a></nz-descriptions-item>
                              <ng-template #githubRef><i nz-icon nzType="github" nzTheme="outline"></i></ng-template>
                          </nz-descriptions>
                      </nz-card>
                  </div>
                  <div nz-col nzSpan="8">
                      <nz-card nzTitle="钉钉讨论群">
                          <img width="260" src="/images/dingding_talk_group.jpeg"/>
                      </nz-card>
                  </div>
                  <div nz-col nzSpan="8">
                      <nz-card nzTitle="微信公众号">
                          <img width="260" src="/images/weixin_talk_group.jpg"/>
                      </nz-card>
                  </div>
              </div>
          </ng-container>
      </nz-drawer>
  `,
  styles: [
      `
          nz-footer {
              text-align: center;
              position: fixed;
              bottom: 0px;
              width: 100%;
          }

          .recent-using-tool {
              padding: 0px;
              background: #e3e3e3;
          }

          nz-content {
              min-height: 500px;
          }

          [nz-row] {
              margin: 8px 0 8px 0;
          }

          .tis-card-content {
              height: 150px;
              text-align: center;
          }

          #main-entry {
              padding: 10px;
          }

          .tis-card-content .compose {
              text-align: center;
              position: relative;
              top: 50%;
              height: 30px;
              margin-top: -30px;
              #color: #767676;
          }

          .compose h1 {
              margin: 0px;
          }

          nz-sider {
              background: #e3e3e3;
          }

          #backgroundText {
              font-size: 4200%;
              font-family: Arial Black, 黑体;
              color: #ded0c4;
              position: absolute;
              top: 0px;
              left: 20%;
              z-index: -1;
              padding: 0px;
              margin: 0px auto;
              width: 80%;
          }

          #body {
              margin: 0px auto;
              width: 800px;
              height: 800px;
          }
    `
  ]
})
export class RootWelcomeComponent implements OnInit {
  _latestSelected: Array<SelectedIndex> = [];
  companyIntrShow = false;

  constructor(private r: Router, private route: ActivatedRoute, private _localStorageService: LocalStorageService) {
  }

  ngOnInit(): void {

    let popularSelected: LatestSelectedIndex = LatestSelectedIndex.popularSelectedIndex(this._localStorageService);
    this._latestSelected = popularSelected.popularLatestSelected;
  }

  backgroupDbClick(event: MouseEvent) {
  }

  gotoIndexList() {
    this.routerTo('/base/applist');
  }

  routerTo(path: string) {
    this.r.navigate([path]);
  }

  gotoAppAdd() {
    this.routerTo('/base/appadd');
  }

  enterIndex(item: SelectedIndex) {
    // this.r.navigate(['/c', item]);
    let app = new Application();
    app.projectName = item.name;
    app.appType = item.appType;
    LatestSelectedIndex.routeToApp(null, this.r, app);
  }
}
