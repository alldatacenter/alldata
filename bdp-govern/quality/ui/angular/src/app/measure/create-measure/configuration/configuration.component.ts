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

import {Component, OnInit, EventEmitter, Input, Output} from "@angular/core";
import * as $ from "jquery";

@Component({
  selector: "app-configuration",
  templateUrl: "./configuration.component.html",
  styleUrls: ["./configuration.component.css"]
})
export class ConfigurationComponent implements OnInit {
  @Output() event = new EventEmitter();
  @Input()
  data = {
    where: "",
    timezone: "",
    num: 1,
    timetype: "day",
    needpath: false,
    path: ""
  };
  @Input() location: string;

  constructor() {
  }

  num: number;
  path: string;
  where: string;
  needpath: boolean;
  selectedType: string;
  configuration = {
    where: "",
    timezone: "",
    num: 1,
    timetype: "day",
    needpath: false,
    path: ""
  };
  timetypes = ["day", "hour", "minute"];
  timetype: string;
  timezones: Map<string, string> = new Map<string, string>([
    ["UTC-12(IDL)", "GMT-12"],
    ["UTC-11(MIT)", "GMT-11"],
    ["UTC-10(HST)", "GMT-10"],
    ["UTC-9:30(MSIT)", "GMT-9:30"],
    ["UTC-9(AKST)", "GMT-9"],
    ["UTC-8(PST)", "GMT-8"],
    ["UTC-7(MST)", "GMT-7"],
    ["UTC-6(CST)", "GMT-6"],
    ["UTC-5(EST)", "GMT-5"],
    ["UTC-4(AST)", "GMT-4"],
    ["UTC-3:30(NST)", "GMT-3:30"],
    ["UTC-3(SAT)", "GMT-3"],
    ["UTC-2(BRT)", "GMT-2"],
    ["UTC-1(CVT)", "GMT-1"],
    ["UTC(WET,GMT)", "GMT"],
    ["UTC+1(CET)", "GMT+1"],
    ["UTC+2(EET)", "GMT+2"],
    ["UTC+3(MSK)", "GMT+3"],
    ["UTC+3:30(IRT)", "GMT+3:30"],
    ["UTC+4(META)", "GMT+4"],
    ["UTC+4:30(AFT)", "GMT+4:30"],
    ["UTC+5(METB)", "GMT+5"],
    ["UTC+5:30(IDT)", "GMT+5:30"],
    ["UTC+5:45(NPT)", "GMT+5:45"],
    ["UTC+6(BHT)", "GMT+6"],
    ["UTC+6:30(MRT)", "GMT+6:30"],
    ["UTC+7(IST)", "GMT+7"],
    ["UTC+8(EAT)", "GMT+8"],
    ["UTC+8:30(KRT)", "GMT+8:30"],
    ["UTC+9(FET)", "GMT+9"],
    ["UTC+9:30(ACST)", "GMT+9:30"],
    ["UTC+10(AEST)", "GMT+10"],
    ["UTC+10:30(FAST)", "GMT+10:30"],
    ["UTC+11(VTT)", "GMT+11"],
    ["UTC+11:30(NFT)", "GMT+11:30"],
    ["UTC+12(PSTB)", "GMT+12"],
    ["UTC+12:45(CIT)", "GMT+12:45"],
    ["UTC+13(PSTC)", "GMT+13"],
    ["UTC+14(PSTD)", "GMT+14"],
  ]);
  timezone: string;

  upward() {
    this.configuration = {
      where: this.where,
      timezone: this.timezone,
      num: this.num,
      timetype: this.timetype,
      needpath: this.needpath,
      path: this.path
    };
    this.event.emit(this.configuration);
  }

  ngOnInit() {
    this.where = this.data.where;
    this.timezone = this.data.timezone;
    this.num = this.data.num;
    this.timetype = this.data.timetype;
    this.needpath = this.data.needpath;
    this.path = this.data.path;
  }
}
