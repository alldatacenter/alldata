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
import {AngularMultiSelectModule} from "angular2-multiselect-dropdown/angular2-multiselect-dropdown";
import {ProfilingStep1, ProfilingStep2} from './../pr.component'


@Component({
  selector: "app-pr-step-2",
  templateUrl: "./step2.component.html",
  styleUrls: ["./step2.component.css"]
})
export class PrStep2Component implements AfterViewChecked, OnInit {
  dropdownSettings = {};

  @Input() step1: ProfilingStep1[];
  @Input() step2: ProfilingStep2[];
  @Output() prevStep: EventEmitter<Object> = new EventEmitter<Object>();
  @Output() nextStep: EventEmitter<Object> = new EventEmitter<Object>();

  constructor() {
  }

  showRule() {
    document.getElementById("showrule").style.display = "";
    document.getElementById("notshowrule").style.display = "none";
  }

  back() {
    document.getElementById("showrule").style.display = "none";
    document.getElementById("notshowrule").style.display = "";
  }

  nextChildStep() {
    this.nextStep.emit(this.step2);
  }

  prevChildStep() {
    this.prevStep.emit(this.step2);
  }

  containsRegex(obj) {
    if (!obj) return false;
    return obj.some(rule =>
      rule.itemName == 'Regular Expression Detection Count'
    )
  }

  ngOnInit() {
    this.dropdownSettings = {
      singleSelection: false,
      text: "Select Rule",
      enableCheckAll: false,
      enableSearchFilter: true,
      classes: "myclass",
      groupBy: "category"
    };
  }

  ngAfterViewChecked() {
  }
}
