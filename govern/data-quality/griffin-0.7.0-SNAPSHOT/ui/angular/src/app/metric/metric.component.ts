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
import {ServiceService} from "../service/service.service";
import * as $ from "jquery";

@Component({
  selector: "app-metric",
  templateUrl: "./metric.component.html",
  styleUrls: ["./metric.component.css"],
  providers: [ChartService, ServiceService]
})
export class MetricComponent implements OnInit {
  constructor(
    public chartService: ChartService,
    public serviceService: ServiceService,
    private http: HttpClient,
    private router: Router
  ) {
  }

  data: any;
  finalData = [];
  chartOption = new Map();
  originalData: any;
  measureOptions = [];
  selectedMeasureIndex = 0;
  chartHeight: any;
  proHeight: any;
  mesWithJob: any;

  ngOnInit() {
    this.renderData();
  }

  checkvalue(job) {
    return job.metricValues.length === 0;
  }

  renderData() {
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
          var node = null;
          node = new Object();
          node.name = mesName;
          node.dq = 0;
          node.metrics = [];
          this.measureOptions.push(mesName);
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
      this.originalData = JSON.parse(JSON.stringify(this.finalData));
      var self = this;
      setTimeout(function function_name(argument) {
        self.redraw(self.finalData);
      }, 1000);
    });
  }

  getOption(parent, i) {
    return this.chartOption.get("thumbnail" + parent + "-" + i);
  }

  redraw(data) {
    this.chartHeight = $(".chartItem:eq(0)").width() * 0.8 + "px";
    // this.proHeight = $(".chartItem:eq(0)").width() * 0.2 + "px";
    for (let i = 0; i < data.length; i++) {
      var parentIndex = i;
      for (let j = 0; j < data[i].metrics.length; j++) {
        let index = j;
        let chartId = "thumbnail" + parentIndex + "-" + index;
        let _chartId = "#" + chartId;
        var divs = $(_chartId);
        divs.get(0).style.width = divs.parent().width() + "px";
        divs.get(0).style.height = this.chartHeight;
        this.chartOption.set(
          chartId,
          this.chartService.getOptionThum(data[i].metrics[j])
        );
        // divs.get(0).style.height = (data[i].type == "accuracy") ? this.chartHeight : this.proHeight;
        // this.chartOption.set(
        //   chartId,
        //   this.chartService.getOptionThum(data[i].metrics[j],chartId)
        // );
      }
    }
  }

  goTo(parent, i) {
    this.router.navigate([
      "/detailed/" + this.finalData[parent].metrics[i].name
    ]);
  }

  changeMeasure() {
    this.finalData = [];
    if (this.selectedMeasureIndex == 0) {
      for (let data of this.originalData) {
        this.finalData.push(data);
      }
    } else {
      var measure = this.measureOptions[this.selectedMeasureIndex - 1];
      for (let data of this.originalData) {
        if (data.name === measure) {
          this.finalData.push(JSON.parse(JSON.stringify(data)));
        }
      }
    }
    var self = this;
    setTimeout(function () {
      self.redraw(self.finalData);
    }, 0);
  }
}
