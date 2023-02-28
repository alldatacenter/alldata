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
import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnDestroy, OnInit, Output} from "@angular/core";
import {TISService} from "../common/tis.service";
import {NzModalService} from "ng-zorro-antd/modal";

/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 *   This program is free software: you can use, redistribute, and/or modify
 *   it under the terms of the GNU Affero General Public License, version 3
 *   or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *   FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

@Component({
  selector: "update-center",
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <nz-alert [style]="'margin:10px'" *ngIf="connCheckErr" nzType="error" nzMessage="服务端异常" [nzDescription]="connCheckErrTpl" nzShowIcon></nz-alert>
      <ng-template #connCheckErrTpl>
          {{connCheckErr.detailedStatus}}
      </ng-template>
      <nz-list nzItemLayout="vertical" [nzDataSource]="plugins">
          <ng-container *ngFor="let item of plugins">
              <nz-list-item *ngIf="item.type === 'InstallationJob'">
                  <nz-list-item-meta>
                      <nz-list-item-meta-title>
                          {{item.name}} <i *ngIf="item.containClassifier" class="classifier-desc">({{item.classifier}})</i>
                          <ng-container [ngSwitch]="item.status.type">
                              <nz-tag *ngSwitchCase="'Success'" nzColor="success">
                                  <i nz-icon nzType="check-circle"></i>
                                  <span>success</span>
                              </nz-tag>
                              <nz-tag *ngSwitchCase="'Installing'" nzColor="processing">
                                  <i nz-icon nzType="sync" nzSpin></i>
                                  <span>Installing</span>
                              </nz-tag>
                              <nz-tag *ngSwitchCase="'Failure'" nzColor="error">
                                  <i nz-icon nzType="close-circle"></i>
                                  <span>Failure</span>
                              </nz-tag>
                              <nz-tag *ngSwitchCase="'Pending'" nzColor="warning">
                                  <i nz-icon nzType="coffee"></i>
                                  <span>Pending</span>
                              </nz-tag>
                          </ng-container>
                      </nz-list-item-meta-title>
                  </nz-list-item-meta>
                  <ng-container [ngSwitch]="item.status.type">
                      <div *ngSwitchCase="'Success'">
                      </div>
                      <div *ngSwitchCase="'Installing'">
                          <ng-template #percent>
                              {{item.status.percentage}}% <i class="percentage-desc">{{item.status.download}}</i>
                          </ng-template>
                          <nz-progress nzStrokeLinecap="round" [nzFormat]="percent" nzType="circle" [nzPercent]="item.status.percentage < 1 ? 1 : item.status.percentage"></nz-progress>
                      </div>
                      <pre *ngSwitchCase="'Failure'">{{item.status.problemStackTrace}}</pre>
                  </ng-container>
                  <nz-list-item-extra>
                  </nz-list-item-extra>
              </nz-list-item>
          </ng-container>
      </nz-list>

  `, styles: [
      `
            .percentage-desc {
                display: block;
                font-size: 8px;
                color: #989898;
            }
            .classifier-desc {
                font-size: 7px;
                color: #989898;
            }
    `
  ]
})
export class PluginUpdateCenterComponent extends BasicFormComponent implements OnInit, OnDestroy {

  plugins: Array<any> = [];
  hasDestroy = false;

  @Output()
  loading = new EventEmitter<boolean>();
  firstLoad = true;

  connCheckErr: ConnectionCheckJobError;

  constructor(tisService: TISService, modalService: NzModalService, private cd: ChangeDetectorRef) {
    super(tisService, modalService);
    cd.detach();
  }

  ngOnDestroy(): void {
    this.hasDestroy = true;
  }

  ngOnInit(): void {
    if (this.firstLoad) {
      this.loading.emit(true);
    }
    this.httpPost('/coredefine/corenodemanage.ajax'
      , `action=plugin_action&emethod=get_update_center_status`)
      .then((r) => {
        if (this.firstLoad) {
          this.loading.emit(false);
          this.firstLoad = false;
        }
        this.plugins = r.bizresult;
        let err = this.plugins.find((p) => {
          return p.error !== undefined;
        });
        if (err) {
          this.connCheckErr = Object.assign(new ConnectionCheckJobError(), err);
        }
        let hasAnyInstalling =
          this.plugins.find((p) => {
            return p.type === 'InstallationJob' && (p.status.type === 'Installing' || p.status.type === 'Pending');
          }) !== undefined;
        this.cd.detectChanges();
        if (hasAnyInstalling && !this.hasDestroy) {
          setTimeout(() => {
            this.ngOnInit();
          }, 2000);
        }
      });
  }
}

class ConnectionCheckJobError {
  type: string;
  statuses: Array<string>;

  get detailedStatus(): string {
    if (!this.statuses) {
      return '';
    }
    return this.statuses.join(" ");
  }
}
