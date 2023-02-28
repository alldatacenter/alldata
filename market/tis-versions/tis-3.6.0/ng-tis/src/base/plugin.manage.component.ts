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
import {TISService} from '../common/tis.service';
import {BasicFormComponent} from '../common/basic.form.component';

import {Pager} from "../common/pagination.component";
import {ActivatedRoute, Params, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {Observable, Subject} from "rxjs";
import {NzDrawerRef, NzDrawerService} from "ng-zorro-antd/drawer";
import {IFieldError} from "../common/tis.plugin";

enum PluginTab {
  avail = 'avaliable',
  installed = 'installed',
  updateCenter = 'update-center'
}

// 查看操作日志
@Component({
  template: `
      <tis-page-header *ngIf="!drawerModel" title="插件管理" [showBreadcrumb]="true">
      </tis-page-header>
      <nz-alert *ngIf="updateSiteLoadErr" nzType="error" nzMessage="错误" [nzDescription]="updateSiteLoadErrTpl" nzShowIcon></nz-alert>
      <ng-template #updateSiteLoadErrTpl>
          加载远端仓库元数据异常: {{updateSiteLoadErr.action_error_msg}}
          <button nz-button nzType="primary" (click)="reloadUpdateSite()" [disabled]="this.formDisabled" nzSize="small"><i nz-icon nzType="redo" nzTheme="outline"></i>重试</button>
      </ng-template>
      <nz-spin [nzSpinning]="this.formDisabled" [nzSize]="'large'">
          <nz-tabset [nzTabBarExtraContent]="extraTemplate" [nzSelectedIndex]="selectedIndex">
              <nz-tab nzTitle="可安装" (nzClick)="openAvailable()">
                  <ng-template nz-tab>
                      <nz-affix class="tool-bar" [nzOffsetTop]="20">
                          <button [nzSize]="'small'" [disabled]="!canInstall" nz-button nzType="primary" (click)="installPlugin()">
                              <i nz-icon nzType="cloud-download" nzTheme="outline"></i>安装
                          </button>
                      </nz-affix>
                      <tis-page [rows]="avaliablePlugs">
                          <tis-col title="安装" width="4">
                              <ng-template let-item="r">
                                  <label nz-checkbox
                                         [(ngModel)]="item.checked" [ngModelOptions]="{standalone: true}"></label>
                              </ng-template>
                          </tis-col>
                          <tis-col title="插件" (search)="queryAvailablePlugin($event)" width="15">
                              <ng-template let-item="r">
                                  <a href="javascript:void(0)">{{item.name}}</a>
                                  <div class="tis-tags">
                                      <span>作者:</span>
                                      <nz-tag>TIS官方</nz-tag>
                                      <br/>
                                      <span>费用:</span>
                                      <nz-tag [nzColor]="'green'">免费</nz-tag>
                                      <br/>
                                      <span>版本:</span>{{ item.version }} <br/>
                                      <span>打包时间:</span>
                                      <nz-tag>{{item.releaseTimestamp| date : "yyyy/MM/dd HH:mm"}}</nz-tag>
                                  </div>
                              </ng-template>
                          </tis-col>
                          <tis-col title="详细">
                              <ng-template let-item="r">
                                  <div class="item-block" *ngIf="item.multiClassifier">
                                      <form nz-form>
                                          <nz-form-item>
                                              <nz-form-control [nzValidateStatus]="pluginErrs.get(item.name) ? 'error' :''"
                                                               [nzErrorTip]="pluginErrs.get(item.name)?pluginErrs.get(item.name).content:''" nzHasFeedback>
                                                  <nz-select [(ngModel)]="item.selectedClassifier" [ngModelOptions]="{standalone: true}" nzAllowClear nzPlaceHolder="有这些版本的包可选择" nzShowSearch>
                                                      <ng-container *ngFor="let c of item.arts">
                                                          <ng-template #t>
                                                              <div class="tis-tags">
                                                                  <span>包大小:</span>
                                                                  <nz-tag>{{c.sizeLiteral}}</nz-tag>
                                                                  <br/>
                                                              </div>
                                                          </ng-template>
                                                          <nz-option-group [nzLabel]="t">
                                                              <nz-option [nzValue]="c.classifierName" [nzLabel]="c.classifierName"></nz-option>
                                                          </nz-option-group>
                                                      </ng-container>
                                                  </nz-select>
                                              </nz-form-control>
                                          </nz-form-item>
                                      </form>
                                  </div>
                                  <div class="item-block">
                                      <markdown [data]="item.excerpt" class="excerpt"></markdown>
                                      <div class="tis-tags" *ngIf="item.dependencies.length >0">
                                          <span>依赖:</span>
                                          <nz-tag [nzColor]="'blue'" *ngFor="let d of item.dependencies">{{d.name}}:{{d.value}}</nz-tag>
                                      </div>
                                  </div>
                              </ng-template>
                          </tis-col>
                      </tis-page>
                  </ng-template>
              </nz-tab>
              <nz-tab nzTitle="已安装" (nzClick)="openInstalledPlugins()">
                  <ng-template nz-tab>
                      <tis-page [rows]="installedPlugs">
                          <tis-col title="插件" (search)="queryIntalledPlugin($event)" width="15">
                              <ng-template let-item="r">
                                  <a href="javascript:void(0)">{{item.name}}</a><i class="classifier-desc" *ngIf="item.classifier">{{item.classifier}}</i>
                                  <div class="tis-tags">
                                      <span>作者:</span>
                                      <nz-tag>TIS官方</nz-tag>
                                      <br/>
                                      <span>费用:</span>
                                      <nz-tag [nzColor]="'green'">免费</nz-tag>
                                      <br/>
                                      <span>版本:</span>{{ item.version }} <br/>
                                      <span>打包时间:</span>
                                      <nz-tag>{{item.releaseTimestamp| date : "yyyy/MM/dd HH:mm"}}</nz-tag>
                                  </div>

                              </ng-template>
                          </tis-col>
                          <tis-col title="详细">
                              <ng-template let-item="r">
                                  <div class="item-block">
                                      <markdown [data]="item.excerpt" class="excerpt"></markdown>
                                      <div class="tis-tags" *ngIf="item.dependencies.length >0">
                                          <span>依赖:</span>
                                          <nz-tag [nzColor]="'blue'" *ngFor="let d of item.dependencies">{{d.name}}:{{d.value}}</nz-tag>
                                      </div>
                                      <div class="tis-tags">
                                      </div>
                                  </div>
                              </ng-template>
                          </tis-col>
                      </tis-page>
                  </ng-template>
              </nz-tab>
              <nz-tab nzTitle="安装状态" (nzClick)="openUpdateCenter()">
                  <ng-template nz-tab>
                      <update-center (loading)="updateCenterLoading($event)"></update-center>
                  </ng-template>
              </nz-tab>
          </nz-tabset>
          <ng-template #extraTemplate>
          </ng-template>
      </nz-spin>
  `, styles: [
      `
            .classifier-desc {
                display: block;
                font-size: 7px;
                color: #989898;
            }

            .tis-tags {
                margin-bottom: 5px;
            }

            .tis-tags span {
                display: inline-block;
                margin-right: 5px;
                color: #b7b7b7;
            }

            .excerpt {
                color: #5e5e5e;
                padding: 5px 0 5px 0px;
            }
    `
  ]
})
export class PluginManageComponent extends BasicFormComponent implements OnInit {

  pager: Pager = new Pager(1, 1);
  avaliablePlugs: Array<any> = [];
  installedPlugs: Array<PluginInfo> = [];
  selectedIndex = 0;

  updateSiteLoadErr: UpdateSiteLoadErr;

  /**
   * 当前是否在抽屉模式
   */
  drawerModel = false;

  // 目标扩展点接口名
  extendPoint: string | Array<String>;

  paramObservable: Observable<Params>;

  pluginErrs: Map<string, IFieldError> = new Map();

  public static openPluginManage(drawerService: NzDrawerService, extendPoint: string | Array<String>): NzDrawerRef<PluginManageComponent, any> {
    const drawerRef = drawerService.create<PluginManageComponent, {}, {}>({
      nzWidth: "70%",
      nzPlacement: "right",
      nzTitle: `插件管理`,
      nzContent: PluginManageComponent,
      nzContentParams: {drawerModel: true, extendPoint: extendPoint}
    });
    return drawerRef;
  }

  constructor(tisService: TISService, modalService: NzModalService, private router: Router, private route: ActivatedRoute) {
    super(tisService, modalService);
  }

  get canInstall(): boolean {
    return this.avaliablePlugs.find((p) => p.checked)
  }

  get canUnInstall(): boolean {
    return this.installedPlugs.find((p) => p.checked) !== undefined
  }

  ngOnInit(): void {

    this.paramObservable = this.drawerModel ? new Subject<Params>() : this.route.params;
    this.paramObservable.subscribe((params: Params) => {
      let tab = params["tab"];
      switch (tab) {
        case PluginTab.updateCenter:
          this.selectedIndex = 2;
          break;
        case PluginTab.installed: {
          this.selectedIndex = 1;
          this.fetchInstalledPlugins(null);
          break;
        }
        case PluginTab.avail:
        default: {
          this.selectedIndex = 0;
          this.fetchAvailablePlugins(null);
        }
      }

    });
    this.triggerSubPath(null);
  }

  private fetchAvailablePlugins(query: string) {
    this.httpPost('/coredefine/corenodemanage.ajax'
      , `action=plugin_action&emethod=get_available_plugins${this.buildExtendPointParam()}${!!query ? '&query=' + query : ''}`)
      .then((r) => {
        this.pager = Pager.create(r);
        // this.logs = r.bizresult.rows;
        let err = this.pager.payload.find((p) => p.updateSiteLoadErr);

        if (err) {
          this.updateSiteLoadErr = err;
        }
        this.avaliablePlugs = r.bizresult.rows;
      });
  }

  reloadUpdateSite() {

    this.httpPost('/coredefine/corenodemanage.ajax'
      , `action=plugin_action&emethod=reload_update_site_meta${this.buildExtendPointParam()}`)
      .then((r) => {
        if (r.success) {
          // this.installedPlugs = r.bizresult;
          this.updateSiteLoadErr = null;
          this.pager = Pager.create(r);
          this.avaliablePlugs = r.bizresult.rows;
          this.selectedIndex = 0;
        }
      });
  }

  private fetchInstalledPlugins(query: string) {
    this.httpPost('/coredefine/corenodemanage.ajax'
      , 'action=plugin_action&emethod=get_installed_plugins' + this.buildExtendPointParam() + (!!query ? '&query=' + query : ''))
      .then((r) => {
        if (r.success) {
          this.installedPlugs = r.bizresult;
        }
      });
  }

  private buildExtendPointParam(): string {
    //  console.log(this.extendPoint);
    let isArray = Array.isArray(this.extendPoint);
    let epParam: Array<string> = isArray ? <Array<string>>this.extendPoint : [<string>this.extendPoint];
    return !!this.extendPoint ? (epParam).map((e) => `&extendpoint=${e}`).join('') : '';
  }

  goPage(event: number) {

  }

  installPlugin() {
    this.pluginErrs = new Map();
    let willInstall: Array<any> = this.avaliablePlugs.filter((p) => p.checked);
    if (willInstall.length < 1) {
      this.modalService.error({
        nzTitle: "错误",
        nzContent: "请选择要安装的插件"
      });
      return;
    }

    this.jsonPost('/coredefine/corenodemanage.ajax?action=plugin_action&emethod=install_plugins'
      , willInstall)
      .then((r) => {
        if (r.success) {
          this.goto(PluginTab.updateCenter)
        } else {
          r.errorfields.forEach((i) => {
            i.forEach((e) => {
              e.forEach((fieldErr) => {
                this.pluginErrs.set(fieldErr.name, fieldErr);
              })
            });
          });
        }
      });

  }

  openInstalledPlugins() {
    this.goto(PluginTab.installed);
  }

  openUpdateCenter() {
    this.goto(PluginTab.updateCenter);
  }

  openAvailable() {
    this.goto(PluginTab.avail);
  }

  private goto(subpath: string) {
    if (this.drawerModel) {
      this.triggerSubPath(subpath);
    } else {
      this.router.navigate(["/base/plugin-manage", subpath], {relativeTo: this.route});
    }
  }

  private triggerSubPath(subpath: string) {
    if (this.drawerModel) {
      let s = <Subject<Params>>this.paramObservable;
      s.next({"tab": subpath});
    }
  }

  updateCenterLoading(load: boolean) {
    this.formDisabled = load;
  }

  queryAvailablePlugin(event: { query: string; reset: boolean }) {
    this.fetchAvailablePlugins(event.reset ? null : event.query);
  }

  queryIntalledPlugin(event: { query: string; reset: boolean }) {
    this.fetchInstalledPlugins(event.reset ? null : event.query);
  }


}

interface PluginInfo {
  name: string;
  version: string;
  website: string;
  checked: boolean;
}

interface UpdateSiteLoadErr {
  action_error_msg: string;
}
