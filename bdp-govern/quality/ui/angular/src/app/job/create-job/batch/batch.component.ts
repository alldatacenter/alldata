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
import {Component, OnInit, AfterViewChecked, ViewChildren} from "@angular/core";
import {FormControl} from "@angular/forms";
import {FormsModule} from "@angular/forms";
import {MaxLengthValidator} from "@angular/forms";
import {NgControlStatus, Validators} from "@angular/forms";
import {PatternValidator} from "@angular/forms";
import {MatDatepickerModule} from "@angular/material";
import {ServiceService} from "../../../service/service.service";
import {AngularMultiSelectModule} from "angular2-multiselect-dropdown/angular2-multiselect-dropdown";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ToasterModule, ToasterService, ToasterConfig} from "angular2-toaster";
import * as $ from "jquery";
import {HttpParams} from "@angular/common/http";
import {Router} from "@angular/router";
import {NouisliderModule} from "ng2-nouislider";
import {HttpClient} from "@angular/common/http";

import {TimeUtils} from "../../../shared/time-utils";

@Component({
  selector: "app-batch",
  templateUrl: "./batch.component.html",
  providers: [ServiceService],
  styleUrls: ["./batch.component.css"]
})
export class BatchComponent implements OnInit, AfterViewChecked {
  constructor(
    toasterService: ToasterService,
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
    this.toasterService = toasterService;
  }

  @ViewChildren("sliderRef") sliderRefs;

  someKeyboard = [];
  someKeyboardConfig = [];
  config: any;
  baseline: string;
  cronExp: string;
  dropdownList = [];
  currentStep = 1;
  maskOpen = false;
  keyupLabelOn = false;
  keydownLabelOn = false;
  createResult = "";
  jobname: string;
  Measures = [];
  measure: string;
  measureid: any;
  timezone = TimeUtils.getBrowserTimeZone();

  newJob = {
    "cron.expression": "",
    "measure.id": "",
    "job.name": "",
    "job.type": "batch",
    "cron.time.zone": "",
    // "cron.time.zone": "GMT+8:00",
    // "predicate.config": {
    //   "interval": "1m",
    //   "repeat": 2
    // },
    "data.segments": [
      // {
      //   "data.connector.index": "source[0]",
      //   "segment.range": {
      //     "begin": "",
      //     "length": ""
      //   }
      // },
      // {
      //   "data.connector.index": "target[0]",
      //   "segment.range": {
      //     "begin": "",
      //     "length": ""
      //   }
      // }
    ]
  };

  beginTime = [];
  timeLength = [];
  originBegin = [];
  originLength = [];

  private toasterService: ToasterService;

  public visible = false;
  public visibleAnimate = false;

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => (this.visible = false), 300);
    this.originBegin = [];
    this.originLength = [];
    $("#save").removeAttr("disabled");
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains("modal")) {
      this.hide();
    }
  }

  close() {
    this.maskOpen = false;
  }

  prev() {
    history.back();
  }

  submit(form) {
    if (!form.valid) {
      this.toasterService.pop("error", "Error!", "Please complete the form!");
      return false;
    }
    this.measureid = this.getMeasureId();
    this.newJob = {
      "job.name": this.jobname,
      "job.type": "batch",
      "measure.id": this.measureid,
      "cron.expression": this.cronExp,
      "cron.time.zone": this.timezone,
      // "cron.time.zone": "GMT+8:00",
      // "predicate.config": {
      // "interval": "1m",
      // "repeat": 2
      // },
      "data.segments": [
        // {
        //   "data.connector.index": "source[0]",
        //   "segment.range": {
        //   "begin": "",
        //   "length": ""
        //   }
        // },
        // {
        //   "data.connector.index": "target[0]",
        //   "segment.range": {
        //   "begin": "",
        //   "length": ""
        //   }
        // }
      ]
    };
    for (let i = 0; i < this.dropdownList.length; i++) {
      var connector = this.dropdownList[i];
      var begin = this.someKeyboard[i][0];
      var length = this.someKeyboard[i][1] - this.someKeyboard[i][0];
      var beginStr = this.getTimeByUnit(begin, connector.size);
      var lengthStr = this.getTimeByUnit(length, connector.size);
      this.newJob["data.segments"].push({
        "data.connector.name": connector.connectorname,
        "as.baseline": true,
        "segment.range": {
          begin: beginStr,
          length: lengthStr
        }
      });
      this.originBegin.push(beginStr);
      this.originLength.push(lengthStr);
    }
    if (this.dropdownList.length == 2) {
      delete this.newJob["data.segments"][1]["as.baseline"];
    }
    this.visible = true;
    setTimeout(() => (this.visibleAnimate = true), 100);
  }

  save() {
    var addJobs = this.serviceService.config.uri.addJobs;
    $("#save").attr("disabled", "true");
    this.http.post(addJobs, this.newJob).subscribe(
      data => {
        this.createResult = data["results"];
        this.hide();
        this.router.navigate(["/jobs"]);
      },
      err => {
        let response = JSON.parse(err.error);
        if (response.code === '40004') {
          this.toasterService.pop("error", "Error!", "Job name already exists!");
        } else {
          this.toasterService.pop("error", "Error!", response.message);
        }
        console.log("Error when creating job");
      }
    );
  }

  onResize(event) {
    this.resizeWindow();
  }

  resizeWindow() {
    var stepSelection = ".formStep";
    $(stepSelection).css({
      height:
      window.innerHeight -
      $(stepSelection).offset().top -
      $("#footerwrap").outerHeight()
    });
    $("fieldset").height(
      $(stepSelection).height() -
      $(stepSelection + ">.stepDesc").height() -
      $(".btn-container").height() -
      200
    );
    $(".y-scrollable").css({
      height: $("fieldset").height() - 20
    });
    $("#data-asset-pie").css({
      height: $("#data-asset-pie")
        .parent()
        .width(),
      width: $("#data-asset-pie")
        .parent()
        .width()
    });
  }

  getTimeByUnit(multiplier, unit) {
    var regex = /^(\d+)([a-zA-Z]+)$/g;
    var arr = regex.exec(unit);
    if (arr.length > 2) {
      var n = parseInt(arr[1]);
      var unitStr = arr[2];
      return ((n * multiplier).toString() + arr[2]);
    } else {
      return multiplier.toString();
    }
  }

  getMeasureId() {
    for (let index in this.Measures) {
      if (this.measure == this.Measures[index].name) {
        return this.Measures[index].id;
      }
    }
  }

  onChange(measure) {
    this.dropdownList = [];
    for (let index in this.Measures) {
      var map = this.Measures[index];
      if (measure == map.name) {
        var source = map["data.sources"];
        for (let i = 0; i < source.length; i++) {
          var connector = source[i].connector;
          if (connector["data.unit"] != undefined) {
            var table =
              connector.config.database +
              "." +
              connector.config["table.name"];
            var size = connector["data.unit"];
            var connectorname = connector["name"];
            var detail = {
              id: i + 1,
              name: table,
              size: size,
              connectorname: connectorname
            };
            this.dropdownList.push(detail);
          }
        }
      }
    }
    for (let i = 0; i < this.dropdownList.length; i++) {
      this.someKeyboard[i] = [-1, 0];
      this.someKeyboardConfig[i] = JSON.parse(JSON.stringify(this.config));
      if (this.sliderRefs._results[i]) {
        this.sliderRefs._results[i].slider.updateOptions({
          range: {
            min: -10,
            max: 0
          }
        });
      }
    }
  }

  changeRange(index, value, i) {
    let newRange = [];
    newRange[i] = [this.someKeyboard[i][0], this.someKeyboard[i][1]];
    newRange[i][index] = value;
    this.updateSliderRange(value, i);
    this.someKeyboard[i] = newRange[i];
  }

  rangeChange(evt, i) {
    var oldmin = this.sliderRefs._results[i].config.range.min;
    if (evt[0] - oldmin <= 2) {
      this.sliderRefs._results[i].slider.updateOptions({
        range: {
          min: oldmin - 10,
          max: 0
        }
      });
    }
    if (evt[0] - oldmin >= 13) {
      this.sliderRefs._results[i].slider.updateOptions({
        range: {
          min: oldmin + 10,
          max: 0
        }
      });
    }
    this.someKeyboard[i] = evt;
  }

  updateSliderRange(value, i) {
    // setTimeout(() => {
    var oldmin = this.sliderRefs._results[i].config.range.min;
    var oldmax = this.sliderRefs._results[i].config.range.max;
    var newmin = Math.floor(value / 10);
    if (value - oldmin <= 3) {
      this.sliderRefs._results[i].slider.updateOptions({
        range: {
          min: newmin * 10,
          max: 0
        }
      });
    }
    // }, 100)
  }

  blinkKeyupLabel() {
    this.keyupLabelOn = true;
    setTimeout(() => {
      this.keyupLabelOn = false;
    }, 450);
  }

  blinkKeydownLabel() {
    this.keydownLabelOn = true;
    setTimeout(() => {
      this.keydownLabelOn = false;
    }, 450);
  }

  ngOnInit() {
    var allModels = this.serviceService.config.uri.allModels + '?type=griffin';
    this.http.get(allModels).subscribe(data => {
      let originData = data;
      for (let i in originData) {
        if (originData[i]["process.type"] === "BATCH") {
          this.Measures.push(originData[i]);
        }
      }
    });
    this.config = {
      behaviour: "drag",
      connect: true,
      start: [-10, 0],
      keyboard: true, // same as [keyboard]="true"
      step: 1,
      pageSteps: 0, // number of page steps, defaults to 10
      range: {
        min: -10,
        max: 0
      },
      pips: {
        mode: "steps",
        density: 10,
        // values: 1,
        stepped: true
      }
    };
  }

  ngAfterViewChecked() {
    this.resizeWindow();
  }
}
