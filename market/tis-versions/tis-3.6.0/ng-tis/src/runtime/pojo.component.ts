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
import {Component, ElementRef, ViewChild} from '@angular/core';
import {TISService} from '../common/tis.service';
// import {BasicEditComponent} from '../corecfg/basic.edit.component';
// import {ScriptService} from '../service/script.service';

import {AppFormComponent, CurrentCollection} from '../common/basic.form.component';
import {ActivatedRoute} from '@angular/router';
import {EditorConfiguration} from "codemirror";
import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  template: `
          <tis-codemirror  [config]="codeMirrirOpts" [ngModel]="pojoJavaContent"></tis-codemirror>
  `,
})
export class PojoComponent extends AppFormComponent {
  // private code: ElementRef;

  pojoJavaContent: string;

  // @ViewChild('codeArea', {static: false}) set codeArea(e: ElementRef) {
  //   this.code = e;
  // }

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService) {
    super(tisService, route, modalService);
  }

  get codeMirrirOpts(): EditorConfiguration {
    return {
      mode: "text/x-java",
      lineNumbers: true
    };
  }

  protected initialize(app: CurrentCollection): void {
    console.log(app);
    this.httpPost('/coredefine/corenodemanage.ajax'
      , 'action=core_action&emethod=get_pojo_data')
      .then((r) => {
        if (r.success) {
          //       this.code.nativeElement.innerHTML = r.bizresult;
          this.pojoJavaContent = r.bizresult;
        }
      });
  }

}
