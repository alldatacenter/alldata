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

import {TISService} from "../common/tis.service";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";

import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {Component, EventEmitter, Injectable, Input, Output} from "@angular/core";
import {DataxDTO} from "./datax.add.component";
import {ActivatedRoute, Router} from "@angular/router";
import {StepType} from "../common/steps.component";

export const DATAX_PREFIX_DB = "dataxDB_";

@Injectable()
export abstract class BasicDataXAddComponent extends AppFormComponent {

  @Output()
  public nextStep = new EventEmitter<any>();
  @Output()
  protected preStep = new EventEmitter<any>();
  @Input()
  public dto: DataxDTO;

  public _offsetStep = -1;

  protected constructor(tisService: TISService, modalService: NzModalService, protected r: Router, route: ActivatedRoute, notification?: NzNotificationService) {
    super(tisService, route, modalService, notification);
  }

  public get stepType(): StepType {
    return this.dto.processModel; //  ? StepType.UpdateDataxReader : StepType.CreateDatax;
  }

  protected initialize(app: CurrentCollection): void {
  }

  public offsetStep(step: number) {
    if (this._offsetStep > -1) {
      return this._offsetStep;
    }
    switch (this.dto.processModel) {
      case StepType.UpdateDataxReader:
        this._offsetStep = step - 1;
        break;
      case StepType.UpdateDataxWriter:
        this._offsetStep = step - 2;
        break;
      default:
        this._offsetStep = step;
    }
    return this._offsetStep;
  }

  // public get componentName(): string {
  //   return this.constructor.name;
  // }
  // tisService: TISService, protected route: ActivatedRoute, modalService: NzModalService


  cancel() {
    if (this.dto.processModel === StepType.CreateDatax) {
      this.r.navigate(['/base/applist'], {relativeTo: this.route});
    } else {
      this.r.navigate(['/x', this.dto.dataxPipeName, "config"], {relativeTo: this.route});
    }
  }

  goback() {
    this.preStep.next(this.dto);
  }
}


