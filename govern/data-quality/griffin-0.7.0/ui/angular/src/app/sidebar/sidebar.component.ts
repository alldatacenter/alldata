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
import {Component, OnInit} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Router} from "@angular/router";
import {ChartService} from "../service/chart.service";
import {DatePipe} from "@angular/common";
import {ServiceService} from "../service/service.service";
import {TruncatePipe} from "./truncate.pipe";
import * as $ from "jquery";

@Component({
  selector: "app-sidebar",
  templateUrl: "./sidebar.component.html",
  styleUrls: ["./sidebar.component.css"],
  providers: [ChartService, ServiceService]
})
export class SidebarComponent implements OnInit {
  constructor(
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService,
    public chartService: ChartService
  ) {
  }

  finalData = [];
  chartOption = new Map();
  orgWithMeasure: any;
  mesWithJob: any;

  pageInit() {
    var health_url = this.serviceService.config.uri.statistics;
    this.http.get(health_url).subscribe(
      data => {
        this.sideBarList(null);
      },
      err => {
      }
    );
  }

  onResize(event) {
    if (window.innerWidth < 992) {
      $("#rightbar").css("display", "none");
    } else {
      $("#rightbar").css("display", "block");
      this.resizeSideChart();
    }
  }

  resizeSideChart() {
    $("#side-bar-metrics").css({
      height:
      $("#mainContent").height() - $("#side-bar-stats").outerHeight() + 70
    });
    for (let i = 0; i < this.finalData.length; i++) {
      for (let j = 0; j < this.finalData[i].metrics.length; j++) {
        if (!this.finalData[i].metrics[j].tag) {
          this.draw(this.finalData[i].metrics[j], i, j);
        }
      }
    }
  }

  draw(metric, parentIndex, index) {
    $("#side" + parentIndex + index).toggleClass("collapse");
    var chartId = "chart" + parentIndex + "-" + index;
    document.getElementById(chartId).style.width =
      $(".panel-heading").innerWidth() - 40 + "px";
    document.getElementById(chartId).style.height = "200px";
    this.chartOption.set(chartId, this.chartService.getOptionSide(metric));
    var self = this;
    $("#" + chartId).unbind("click");
    $("#" + chartId).click(function (e) {
      self.router.navigate([
        "/detailed/" + self.finalData[parentIndex].metrics[index].name
      ]);
    });
  }

  getOption(parent, i) {
    return this.chartOption.get("chart" + parent + "-" + i);
  }

  checkvalue(job) {
    return job.metricValues.length === 0;
  }

  sideBarList(sysName) {
    let url_dashboard = this.serviceService.config.uri.dashboard;
    this.http.get(url_dashboard).subscribe(data => {
      this.mesWithJob = JSON.parse(JSON.stringify(data));
      for (let i = 0; i < this.mesWithJob.length; i++) {
        if (this.mesWithJob[i].some(this.checkvalue)) {
          this.mesWithJob[i].splice(i, 1);
        }
      }
      for (let mesName in this.mesWithJob) {
        var jobs = this.mesWithJob[mesName];
        if (
          jobs.length > 0 && jobs[0].type == "ACCURACY"
        ) {
          var jobs = this.mesWithJob[mesName];
          var node = null;
          node = new Object();
          node.name = mesName;
          node.dq = 0;
          node.metrics = [];
          node.type = "ACCURACY";
          for (let i = 0; i < jobs.length; i++) {
            if (jobs[i].metricValues.length != 0) {
              var someMetrics = jobs[i].metricValues.slice(0, 30);
              jobs[i].metricValues = JSON.parse(
                JSON.stringify(someMetrics)
              );
              var metricNode = {
                name: "",
                timestamp: "",
                dq: 0,
                details: []
              };
              metricNode.details = JSON.parse(
                JSON.stringify(jobs[i].metricValues)
              );
              metricNode.name = jobs[i].name;
              metricNode.timestamp = jobs[i].metricValues[0].tmst;
              metricNode.dq =
                jobs[i].metricValues[0].value.matched /
                jobs[i].metricValues[0].value.total *
                100;
              node.metrics.push(metricNode);
            }
          }
        } else {
          continue;
        }
        this.finalData.push(node);
      }
    });
  }

  ngOnInit() {
    this.sideBarList(null);
  }
}
