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
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ToasterModule, ToasterService, ToasterConfig} from "angular2-toaster";
import * as $ from "jquery";
import {HttpParams} from "@angular/common/http";
import {Router} from "@angular/router";
import {HttpClient} from "@angular/common/http";

import {TimeUtils} from '../../../shared/time-utils';

@Component({
  selector: 'app-streaming',
  templateUrl: './streaming.component.html',
  styleUrls: ['./streaming.component.css']
})
export class StreamingComponent implements OnInit {

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

  newJob = {
    "cron.expression": "",
    "job.type": "streaming",
    "measure.id": "",
    "job.name": "",
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
    let timezone = TimeUtils.getBrowserTimeZone();
    this.newJob = {
      "job.type": "streaming",
      "job.name": this.jobname,
      "measure.id": this.measureid,
      "cron.expression": this.cronExp,
      "cron.time.zone": timezone,
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
          this.toasterService.pop("error", "Error!", "Error when creating job");
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
      height: $("fieldset").height()
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
  }

  ngOnInit() {
    var allModels = this.serviceService.config.uri.allModels + '?type=griffin';
    this.http.get(allModels).subscribe(data => {
      let originData = data;
      for (let i in originData) {
        if (originData[i]["process.type"] === "STREAING") {
          this.Measures.push(originData[i]);
        }
      }
    });
  }

  ngAfterViewChecked() {
    this.resizeWindow();
  }

}
