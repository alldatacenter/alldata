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

import {AfterContentInit, Component, Input} from "@angular/core";


// implements OnInit, AfterContentInit
@Component({
  selector: 'tis-breadcrumb',
  template: `
      <nz-breadcrumb>
          <nz-breadcrumb-item>
              Home
          </nz-breadcrumb-item>
          <nz-breadcrumb-item>
              <a>Application List</a>
          </nz-breadcrumb-item>
          <nz-breadcrumb-item>
              An Application
          </nz-breadcrumb-item>
      </nz-breadcrumb>
  `,
  styles: [
      `
          nz-breadcrumb {
              margin: 10px 0 20px 0;
          }
    `
  ]
})
export class TisBreadcrumbComponent implements AfterContentInit {
  @Input()
  result: { success: boolean, msg: any[], errormsg: any[] }
    = {success: false, msg: [], errormsg: []};

  public get showSuccessMsg(): boolean {

    return (this.result != null) && (this.result.success === true)
      && (this.result.msg !== null) && this.result.msg.length > 0;

  }

  public get showErrorMsg(): boolean {
    return this.result != null && !this.result.success
      && this.result.errormsg && this.result.errormsg.length > 0;
  }

  ngAfterContentInit() {

  }

  jsonStr(v: any): string {
    return JSON.stringify(v);
  }


}
