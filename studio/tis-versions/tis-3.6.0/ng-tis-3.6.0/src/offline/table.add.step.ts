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

import {EventEmitter, Inject, Injectable, Input, Output} from '@angular/core';
import {TISService} from '../common/tis.service';
import {BasicFormComponent} from '../common/basic.form.component';
import {Router} from '@angular/router';
import {Location} from '@angular/common';
import {IntendDirect} from "../common/MultiViewDAG";


@Injectable()
export class TableAddStep extends BasicFormComponent {
  // @Input() isShow: boolean;
  @Output() previousStep: EventEmitter<any> = new EventEmitter();
  @Output() nextStep = new EventEmitter<IntendDirect | any>();

  constructor(protected tisService: TISService, protected router: Router
    , protected localtion: Location) {
    super(tisService);
  }

  // 执行下一步
  public createPreviousStep(form: any): void {
    this.previousStep.emit(form);
  }

  // 执行下一步
  public createNextStep(form: any): void {
    this.nextStep.emit(form);
  }

  // protected goHomePage(tableId: number): void {
  //   // this.router.navigate(['/t/offline'], {queryParams: {tableId: tableId}});
  // }

  protected goBack(): void {
    this.localtion.back();
  }
}
