/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
import {Component, EventEmitter, Input, Output} from "@angular/core";
import {ProfilingStep1, ProfilingStep2, ProfilingStep3, ProfilingStep4} from "../pr.component";

@Component({
  selector: "app-pr-confirm-modal",
  templateUrl: "./confirmModal.component.html",
  styleUrls: ["./confirmModal.component.css"]
})
export class PrConfirmModal {

  @Input() step1: ProfilingStep1;
  @Input() step2: ProfilingStep2;
  @Input() step3: ProfilingStep3;
  @Input() step4: ProfilingStep4;

  @Output() hide: EventEmitter<Object> = new EventEmitter<Object>();
  @Output() saveMeasure: EventEmitter<Object> = new EventEmitter<Object>();

  constructor() {
  }

  hideModal() {
    this.hide.emit();
  }

  saveModal() {
    this.saveMeasure.emit()
  }
}
