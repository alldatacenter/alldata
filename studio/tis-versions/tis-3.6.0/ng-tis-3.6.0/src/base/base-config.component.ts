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
import {Component, EventEmitter, OnInit} from "@angular/core";
import {TISService} from "../common/tis.service";

import {PluginSaveResponse, TisResponseResult} from "../common/tis.plugin";
import {NzModalService} from "ng-zorro-antd/modal";
import {ActivatedRoute, Router} from "@angular/router";

enum PluginCategory {
  Global = 'global',
  Incr = 'incr',
  Offline = 'offline'
}

@Component({
  template: `
      <tis-page-header title="插件配置"></tis-page-header>
      <nz-spin [nzSize]="'large'" [nzSpinning]="this.formDisabled || pluginComponentDisabled">
          <nz-tabset (nzSelectedIndexChange)="tabChange($event)"  [nzSelectedIndex]="selectedIndex">
              <nz-tab nzTitle="全局" (nzClick)="goto('global')" (nzDeselect)="configDeSelect($event)">
                  <ng-template nz-tab>
                      <tis-plugins [showExtensionPoint]="this.showExtensionPoint" [showSaveButton]="true" [plugins]="['k8s-images','params-cfg']" (ajaxOccur)="buildStep1ParamsSetComponentAjax($event)"></tis-plugins>
                  </ng-template>
              </nz-tab>
<!--              <nz-tab nzTitle="实时" (nzClick)="goto('incr')" (nzDeselect)="configDeSelect($event)">-->
<!--                  <ng-template nz-tab>-->
<!--                      <tis-plugins [showExtensionPoint]="this.showExtensionPoint" [showSaveButton]="true" [plugins]="['incr-config']" (ajaxOccur)="buildStep1ParamsSetComponentAjax($event)"></tis-plugins>-->
<!--                  </ng-template>-->
<!--              </nz-tab>-->
              <nz-tab nzTitle="离线" (nzClick)="goto('offline')" (nzDeselect)="configDeSelect($event)">
                  <ng-template nz-tab>
                      <tis-plugins [showExtensionPoint]="this.showExtensionPoint" [showSaveButton]="true" [plugins]="['fs','flat_table_builder']" (ajaxOccur)="buildStep1ParamsSetComponentAjax($event)"></tis-plugins>
                  </ng-template>
              </nz-tab>
          </nz-tabset>
<!--          <ng-template #extraTemplate>-->
<!--              <span>扩展点显示<nz-switch [nzLoading]="formDisabled" (ngModelChange)="showExtendsPointChange($event)" [ngModel]="this.showExtensionPoint.open" nzCheckedChildren="开" nzUnCheckedChildren="关"></nz-switch></span>-->
<!--          </ng-template>-->
      </nz-spin>
  `
  ,
  styles: [`

  `]
})
export class BaseConfigComponent extends BasicFormComponent implements OnInit {
  showExtensionPoint: { open: boolean } = {open: true};
  pluginComponentDisabled = true;
  selectedIndex = 0;

  // savePlugin = new EventEmitter<any>();
  constructor(tisService: TISService, modalService: NzModalService, private router: Router, private route: ActivatedRoute) {
    super(tisService, modalService);
  }

  goto(subpath: string): void {
    this.router.navigate(["/base/basecfg", subpath], {relativeTo: this.route});
  }

  ngOnInit(): void {
    let url = '/coredefine/corenodemanage.ajax';
    this.httpPost(url, 'event_submit_do_get_extension_point_show=y&action=plugin_action')
      .then((r: TisResponseResult) => {
        this.showExtensionPoint = {open: r.bizresult};
      })

    this.route.params.subscribe((param) => {
      let tab = param["tab"];
      switch (tab) {
        case PluginCategory.Incr:
          this.selectedIndex = 1;
          break;
        case PluginCategory.Offline:
          this.selectedIndex = 2;
          break;
        case PluginCategory.Global:
        default:
          this.selectedIndex = 0;
      }
    });
  }

  buildStep1ParamsSetComponentAjax(event: PluginSaveResponse) {
    // console.log(event.formDisabled);
    if (!event.formDisabled) {
      this.pluginComponentDisabled = event.formDisabled;
    }
  }

  tabChange(event: number) {
    // console.log(event);
    if (event > 0) {
      this.pluginComponentDisabled = true;
    }
  }

  configDeSelect(event: void) {
  }

  save() {
    // this.savePlugin.emit();
  }

  showExtendsPointChange(event: boolean) {
    let url = '/coredefine/corenodemanage.ajax';
    this.httpPost(url, 'event_submit_do_switch_extension_point_show=y&action=plugin_action&switch=' + event)
      .then((r: TisResponseResult) => {
        this.showExtensionPoint = {open: event};
      })
  }
}
