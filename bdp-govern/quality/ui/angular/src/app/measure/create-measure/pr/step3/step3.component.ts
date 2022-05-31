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
import {Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {AfterViewChecked} from "@angular/core";
import {ProfilingStep1, ProfilingStep2, ProfilingStep3} from './../pr.component'

@Component({
  selector: "app-pr-step-3",
  templateUrl: "./step3.component.html",
  styleUrls: ["./step3.component.css"]
})
export class PrStep3Component implements AfterViewChecked, OnInit {
  @Input() step1: ProfilingStep1;
  @Input() step2: ProfilingStep2;
  @Input() step3: ProfilingStep3;

  @Output() prevStep: EventEmitter<Object> = new EventEmitter<Object>();
  @Output() nextStep: EventEmitter<Object> = new EventEmitter<Object>();

  constructor() {
  }

  getData(evt) {
    this.step3.config = evt;
    this.step3.timezone = evt.timezone;
    this.step3.where = evt.where;
    this.step3.size = evt.num + evt.timetype;
    this.step3.needpath = evt.needpath;
    this.step3.path = evt.path;
  }

  nextChildStep() {
    this.nextStep.emit(this.step3);
  }

  prevChildStep() {
    this.prevStep.emit(this.step3);
  }

  ngOnInit() {
  }

  ngAfterViewChecked() {
  }
}
