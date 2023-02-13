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
import {Component, Injector, Input, OnInit} from '@angular/core';

import {BasicFormComponent} from '../common/basic.form.component';
import {NzModalService} from "ng-zorro-antd/modal";


// 设置全局参数
@Component({
  // templateUrl: '/runtime/config_file_parameters_set.htm'
  template: `
    <!--from modal frame-->
    <fieldset [disabled]='formDisabled'>
      <div class="modal-header">
        <h4 class="modal-title">设置全局配置参数</h4>
        <button type="button" class="close" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
      </div>
      <div class="modal-body">
         <tis-page-header [showBreadcrumb]="false" [result]="result" >
             <button nz-button nzType="primary" nzDanger  (click)="deleteParam()">
                 <i class="fa fa-trash" aria-hidden="true"></i>删除
             </button> &nbsp;
             <button nz-button nzType="primary"
                     (click)="event_submit_do_set_parameter(form)">更新
             </button>
         </tis-page-header>
        <fieldset id="uploaddialog">
          <form #form>
            <input type="hidden" name="action" value="config_file_parameters_action"/>
            <input type="hidden" name="emethod" value="set_parameter"/>
            <div class="form-group">

              <h4>{{resparam.keyName}}:</h4>
              <input type="text" class="form-control" id="inputValue"
                     [(ngModel)]="resparam.value" name="keyvalue"/>
            </div>
          </form>
        </fieldset>
      </div>
    </fieldset>
  `
})
export class GlobalUpdateParamComponent extends BasicFormComponent implements OnInit {


  resparam: any = {value: '', keyName: ''};
  rpidVal: number;

  constructor(tisService: TISService, modalService: NzModalService
    , private injector: Injector) {
    super(tisService, modalService);
  }

  ngOnInit(): void {
    this.httpPost('/runtime/config_file_parameters_set.ajax'
      , 'event_submit_do_get_param=y&action=config_file_parameters_action&rpid=' + this.rpidVal)
      .then(result => {
        if (result.success) {
          this.resparam = result.bizresult;
        }
      });
  }

  @Input() set rpid(val: number) {
    this.rpidVal = val;

  }


  // 添加参数
  public event_submit_do_set_parameter(form: any) {
    this.submitForm('/runtime/config_file_parameters_set.ajax'
      , form);
  }

  deleteParam() {
  }
}
