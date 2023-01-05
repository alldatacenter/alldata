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

import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  // from:/runtime/usradd.htm
  template: `

      <tis-msg [result]="result"></tis-msg>
      <form class="form-horizontal">
          <input type="hidden" name="action" value="user_action"/>
          <input type="hidden" name="event_submit_do_usr_add" value="y"/>

          <div class="row justify-content-end">

              <div class="col-sm-2">
                  <button type="submit" class="btn btn-primary">创建账户</button>
              </div>
          </div>

          <div class="form-group">
              <label for="user_account">账户名</label>
              <input type="text" class="form-control" id="user_account" placeholder="xiaobai@126.com/xiaobai" name="userAccount">
          </div>

          <div class="form-group">
              <label for="real_name">用户名</label>
              <input type="text" id="real_name" class="form-control" placeholder="小白" name="realName"/>
          </div>
          <div class="form-group">
              <label for="inputPassword">密码</label>
              <input type="password" id="inputPassword" class="form-control" placeholder="Password" name="password"/>
          </div>

          <div class="form-group">
              <label for="real_name">设置部门</label>

              <input type="hidden" name="selecteddptid" id="selecteddptid"/>
              <select id="rootdpt" class="form-control" style="width:200px" name="dptid">
                  <option value="-1" selected>请选择</option>
              </select>
          </div>
      </form>
  `
})
export class UserAddComponent extends BasicFormComponent implements OnInit {

  constructor(tisService: TISService, private router: Router
    , modalService: NzModalService) {
    super(tisService, modalService);
  }


  ngOnInit(): void {

  }
}
