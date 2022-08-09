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
import {ServiceService} from "../service/service.service";
import * as $ from "jquery";

@Component({
  selector: "app-health",
  templateUrl: "./health.component.html",
  styleUrls: ["./health.component.css"]
})
export class HealthComponent implements OnInit {
  constructor(
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
  }

  chartOption: object;
  finalData = [];
  mesWithJob: any;

  onChartClick($event) {
    let self = this;
    if ($event.data.name) {
      self.router.navigate(["/detailed/" + $event.data.name]);
    }
  }

  resizeTreeMap() {
    $("#chart1").height(
      $("#mainWindow").height() - $(".bs-component").outerHeight()
    );
  }

  parseData(data) {
    var sysId = 0;
    var metricId = 0;
    var result = [];
    for (let sys of data) {
      var item = {
        id: "",
        name: "",
        children: []
      };
      item.id = "id_" + sysId;
      item.name = sys.name;
      if (sys.metrics != undefined) {
        item.children = [];
        for (let metric of sys.metrics) {
          var itemChild = {
            id: "id_" + sysId + "_" + metricId,
            name: metric.name,
            value: 1,
            dq: metric.dq,
            sysName: sys.name,
            itemStyle: {
              normal: {
                color: "#4c8c6f"
              }
            }
          };
          if (metric.dqfail == 1) {
            itemChild.itemStyle.normal.color = "#ae5732";
          } else {
            itemChild.itemStyle.normal.color = "#005732";
          }
          item.children.push(itemChild);
          metricId++;
        }
      }
      item.children = item.children.sort(function (a, b) {
        return (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0);
      });
      result.push(item);
      sysId++;
    }
    return result;
  }

  getLevelOption() {
    return [
      {
        itemStyle: {
          normal: {
            borderWidth: 0,
            gapWidth: 6,
            borderColor: "#000"
          }
        }
      },
      {
        itemStyle: {
          normal: {
            gapWidth: 1,
            borderColor: "#fff"
          }
        }
      }
    ];
  }

  renderTreeMap(res) {
    var data = this.parseData(res);
    var option = {
      title: {
        text: "Data Quality Metrics Heatmap",
        left: "center",
        textStyle: {
          color: "white"
        }
      },
      backgroundColor: "transparent",
      tooltip: {
        formatter: function (info) {
          var dqFormat = info.data.dq > 100 ? "" : "%";
          if (info.data.dq)
            return [
              '<span style="font-size:1.8em;">' +
              info.data.sysName +
              " &gt; </span>",
              '<span style="font-size:1.5em;">' +
              info.data.name +
              "</span><br>",
              '<span style="font-size:1.5em;">dq : ' +
              info.data.dq.toFixed(2) +
              dqFormat +
              "</span>"
            ].join("");
        }
      },
      series: [
        {
          name: "System",
          type: "treemap",
          itemStyle: {
            normal: {
              borderColor: "#fff"
            }
          },
          levels: this.getLevelOption(),
          breadcrumb: {
            show: false
          },
          roam: false,
          nodeClick: "link",
          data: data,
          width: "95%",
          bottom: 0
        }
      ]
    };
    this.resizeTreeMap();
    this.chartOption = option;
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
          var jobs = this.mesWithJob[mesName];
          var node = null;
          node = new Object();
          node.name = mesName;
          node.dq = 0;
          node.metrics = [];
          node.type = "ACCURACY";
          for (let i = 0; i < jobs.length; i++) {
            if (jobs[i].metricValues.length != 0) {
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
      var self = this;
      setTimeout(function function_name(argument) {
        self.renderTreeMap(self.finalData);
      }, 1000);
    });
  }

  ngOnInit() {
    var self = this;
    this.renderData();
    // this.renderTreeMap(this.getMetricService.renderData());
    // setTimeout(function function_name(argument) {
    //   // body...
    //     self.renderTreeMap(self.renderData());

    // })
  }
}
