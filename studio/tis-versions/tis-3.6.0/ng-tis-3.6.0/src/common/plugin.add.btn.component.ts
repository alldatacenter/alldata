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


import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";
import {Descriptor} from "./tis.plugin";
import {BasicFormComponent} from "./basic.form.component";
import {TISService} from "./tis.service";
import {NzDrawerRef, NzDrawerService} from "ng-zorro-antd/drawer";
import {PluginManageComponent} from "../base/plugin.manage.component";
import {NzButtonSize} from "ng-zorro-antd/button/button.component";

@Component({
  selector: 'tis-plugin-add-btn',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <ng-container [ngSwitch]="this.descriptors.length> 0 ">
          <ng-container *ngSwitchCase="true">
              <button [style]="btnStyle" nz-button nz-dropdown [nzSize]="btnSize" [nzDropdownMenu]="menu" [disabled]="this.disabled">
                  <ng-content></ng-content>
              </button>
              <nz-dropdown-menu #menu="nzDropdownMenu">
                  <ul nz-menu>
                      <li nz-menu-item *ngFor="let d of descriptors" (click)="addNewPluginItem(d)">
                          <a href="javascript:void(0)" >{{d.displayName}}</a>
                      </li>
                      <li nz-menu-divider></li>
                      <li nz-menu-item (click)="addNewPlugin()">
                          <a href="javascript:void(0)" ><i nz-icon nzType="api" nzTheme="outline"></i>添加</a>
                      </li>
                  </ul>
              </nz-dropdown-menu>
          </ng-container>
          <ng-container *ngSwitchCase="false">
              <button [style]="btnStyle" nz-button nzType="default" [nzSize]="'small'" (click)="addNewPlugin()" [disabled]="this.disabled">
                  <i nz-icon nzType="api" nzTheme="outline"></i>添加
              </button>
          </ng-container>
      </ng-container>

  `
})
export class PluginAddBtnComponent extends BasicFormComponent {
  @Input()
  descriptors: Array<Descriptor> = [];
  @Input()
  extendPoint: string | Array<String>;
  @Input()
  disabled: boolean;

  @Input()
  btnSize: NzButtonSize = 'default';

  @Input()
  btnStyle = '';

  @Output()
  addPlugin = new EventEmitter<Descriptor>();

  @Output()
  afterPluginAddClose = new EventEmitter<Descriptor>();



  constructor(tisService: TISService
    , private drawerService: NzDrawerService) {
    super(tisService);
  }

  addNewPluginItem(desc: Descriptor) {
    this.addPlugin.emit(desc);
  }

  addNewPlugin() {
    const drawerRef = PluginManageComponent.openPluginManage(this.drawerService, this.extendPoint);

    drawerRef.afterClose.subscribe(() => {
      this.afterPluginAddClose.emit();
    })
  }


}
