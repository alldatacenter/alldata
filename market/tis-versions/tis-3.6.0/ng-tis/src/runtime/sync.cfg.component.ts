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
import {BasicFormComponent} from "../common/basic.form.component";

@Component({
  // templateUrl: '/runtime/jarcontent/sys_daily_resources.htm'
  template: `
      <fieldset [disabled]='formDisabled'>
          <div class="modal-header">
              <h4 class="modal-title">同步线上配置</h4>
              <button type="button" class="close" aria-label="Close" >
                  <span aria-hidden="true">&times;</span>
              </button>
          </div>
          <div class="modal-body">
              <tis-msg [result]="result"></tis-msg>
              <div class="note">DAILY环境中还没有设置配置文件，不能同步</div>
              <br/>
              <div>
                  <div class="note2">DAILY环境配置文件已经同步到线上，不需要再同步了</div>
              </div>
          </div>
      </fieldset>
  `
})
// 将配置同步到线上
export class SyncConfigComponent extends BasicFormComponent {

  constructor(tisService: TISService, ) {
    super(tisService, null);
  }

// 开始将日常环境中的配置同步到线上环境中
  public synchronizeConfigRes(): void {
    //   TIS.ajax({url:'$manageModule.setTarget('jarcontent/snapshotlist.ajax')',
    //     type:'POST',
    //     dataType:"json",
    //     data:"event_submit_do_sync_daily_config=y&action=save_file_content_action&appid=$manageTool.appDomain.appid",
    //     success:function(data){
    //     appendMessage(data,$("#messageblock"))
    //   }
    // });

    this.tisService.httpPost('', '');

  }

}
