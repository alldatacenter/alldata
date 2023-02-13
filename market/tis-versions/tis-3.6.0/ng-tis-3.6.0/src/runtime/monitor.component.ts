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

import {Component} from "@angular/core";
import {TISService} from "../common/tis.service";
import {BasicFormComponent} from "../common/basic.form.component";

import {NzModalService} from "ng-zorro-antd/modal";


@Component({
  template: `
      <div style="margin-top: 20px" nz-row [nzGutter]="16">
          <div class="line-chart-block" nz-col nzSpan="12">
              <line-chart queryType="solrQuery"></line-chart>
          </div>
          <div class="line-chart-block"  nz-col nzSpan="12">
              <line-chart queryType="docUpdate"></line-chart>
          </div>
      </div>
  `,
  styles: [
      `
          .line-chart-block {
          }
    `
  ]
})
export class MonitorComponent extends BasicFormComponent {
  constructor(tisService: TISService, modalService: NzModalService) {
    super(tisService, modalService);
  }
}
