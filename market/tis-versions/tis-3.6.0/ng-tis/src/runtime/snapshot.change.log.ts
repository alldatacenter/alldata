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
 * Created by baisui on 2017/4/10 0010.
 */
//
import {Component, OnInit} from '@angular/core';
import {TISService} from '../common/tis.service';
import {BasicEditComponent} from '../corecfg/basic.edit.component';
// import {ScriptService} from '../service/script.service';

import {AppFormComponent, BasicFormComponent, CurrentCollection} from '../common/basic.form.component';
import {ActivatedRoute} from '@angular/router';
import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  // templateUrl: '/runtime/operation_log_special_app.htm'
  template: `
      <fieldset [disabled]='formDisabled'>

          <tis-msg [result]="result"></tis-msg>
          <table class="table table-hover">
              <thead class="thead-default">
              <tr>
                  <th width="100px">操作人</th>
                  <th>日志</th>
                  <th width="200px">时间</th>
              </tr>
              </thead>
              <tbody>
              <tr *ngFor="let l of logs">
                  <td style="padding-right:10px;" align="right">{{l.usrName}}</td>
                  <td>{{l.memo}}</td>
                  <td>{{l.createTime}}</td>
              </tr>
              </tbody>
          </table>
      </fieldset>
  `
})
export class SnapshotChangeLogComponent extends AppFormComponent {
  logs: any[] = [];

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService) {
    super(tisService, route, modalService);
  }

  protected initialize(app: CurrentCollection): void {
    this.httpPost('/runtime/operation_log_special_app.ajax'
      , 'action=operation_log_action&emethod=get_init_data&tab=server_group&opt=updateByExampleSelective')
      .then((r) => {
        if (r.success) {
          this.logs = r.bizresult;
        }
      });
  }
}
