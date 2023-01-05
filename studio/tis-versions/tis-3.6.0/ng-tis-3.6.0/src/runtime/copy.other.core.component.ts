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
import {Component} from '@angular/core';
import {TISService} from '../common/tis.service';
// import {ScriptService} from '../service/script.service';
import {BasicFormComponent} from "../common/basic.form.component";
import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  // templateUrl: '/runtime/jarcontent/copy_config_from_other_app.htm?an={dddd}'
  template: `
      <fieldset [disabled]='formDisabled'>
          <div class="modal-header">
              <h4 class="modal-title">从其他索引拷贝配置</h4>
              <button type="button" class="close" aria-label="Close">
                  <span aria-hidden="true">&times;</span>
              </button>
          </div>
          <div class="modal-body">
              <tis-msg [result]="result"></tis-msg>
              <form #configfrom method="post" role="form">
                  <input type="hidden" name="action" value="add_app_action"/>
                  <input type="hidden" name="event_submit_do_copy_config_from_other_app" value="y"/>
                  <input type="hidden" name="toAppId" value="-1"/>
                  <h3>请选择配置部门</h3>
                  <div class="form-group">
                      <p class="appselectboxcontrol">

                          <span class="navbar-text" style="width:4em;">业务线：</span>
                          <select id="bizidmain" (change)="departmentChange()" [(ngModel)]="departmentId" name="combDptid">
                              <option value="">请选择</option>
                              <option
                                      value="356">/2dfire/Engineering department
                              </option>
                              <option
                                      value="360">/2dfire/crm
                              </option>
                              <option
                                      value="358">/2dfire/data-center
                              </option>
                              <option
                                      value="359">/2dfire/resale
                              </option>
                              <option
                                      value="357">/2dfire/supplyGoods
                              </option>
                          </select>
                          <span class="navbar-text">索引：</span>
                         <!--
                          <select id="appidsmain" name="combAppid" [(ngModel)]="appId">
                              <option value="">请选择</option>
                              <option *ngFor="let o of ops" [value]="o.name">{{o.name}}</option>
                          </select>
                          -->
                          <br/>
                      </p>
                      <div class="form-group">
                          <label for="query">应用</label>
                          <input type="text" class="form-control" name="appnamesuggest" id="query" size="40" placeholder="search4"/>
                          <input type="hidden" id="hidappname" name="hiddenAppnamesuggest"/>
                      </div>
                  </div>
                  <button type="submit" class="btn btn-primary" (click)="doCopy(configfrom)">执行拷贝</button>
              </form>
          </div>
      </fieldset>
  `
})
export class CopyOtherCoreComponent extends BasicFormComponent {
  departmentId: any;

  constructor(tisService: TISService, modalService: NzModalService) {
    super(tisService, modalService);
  }

  // 执行拷贝
  public doCopy(configfrom: any): void {

  }

  departmentChange() {
  }
}
