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
import {
  Component,
  OnInit,
  OnChanges,
  SimpleChanges,
  OnDestroy,
  AfterViewInit,
  AfterViewChecked,
  NgZone
} from "@angular/core";
import {ChartService} from "../../service/chart.service";
import {ServiceService} from "../../service/service.service";
import {Router, ActivatedRoute, ParamMap} from "@angular/router";
import "rxjs/add/operator/switchMap";
import {HttpClient} from "@angular/common/http";
import * as $ from "jquery";
import {DataTableModule} from "angular2-datatable";

@Component({
  selector: "app-detail-metric",
  templateUrl: "./detail-metric.component.html",
  styleUrls: ["./detail-metric.component.css"],
  providers: [ChartService, ServiceService]
})
export class DetailMetricComponent implements AfterViewChecked, OnInit {
  objectKeys = Object.keys;

  constructor(
    public chartService: ChartService,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private zone: NgZone,
    public serviceService: ServiceService
  ) {
  }

  selectedMeasure: string;
  chartOption: {};
  data: any;
  currentJob: string;
  finalData: any;
  prodata = [];
  metricName: string;
  size = 300;
  offset = 0;
  profiling = false;
  columnname = [];
  noresults = false;
  missRecordList = [];
  public visible = false;
  public visibleAnimate = false;
  isDownloadClickDisabled = true;

  ngOnInit() {
    this.currentJob = this.route.snapshot.paramMap.get("name");
    var self = this;
    var metricdetail = self.serviceService.config.uri.metricdetail;
    var metricDetailUrl =
      metricdetail +
      "?metricName=" +
      this.currentJob +
      "&size=" +
      this.size +
      "&offset=" +
      this.offset;
    this.http.get(metricDetailUrl).subscribe(
      data => {
        this.data = data;
        if (this.data.length == 0) {
          this.noresults = true;
        }
        if (this.data.length != 0 && this.data[0].value.matched != undefined) {
          var metric = {
            name: "",
            timestamp: 0,
            dq: 0,
            details: []
          };
          metric.name = this.data[0].name;
          metric.timestamp = this.data[0].tmst;
          metric.dq =
            this.data[0].value.matched / this.data[0].value.total * 100;
          metric.details = JSON.parse(JSON.stringify(this.data));
          this.chartOption = this.chartService.getOptionBig(metric);
          this.missRecordList = metric.details.filter(val => val.value.missed !== 0);
          $("#bigChartDiv").height(window.innerHeight - 120 + "px");
          $("#bigChartDiv").width(window.innerWidth - 400 + "px");
          $("#bigChartContainer").show();
        } else if (this.data.length != 0) {
          this.prodata = this.data;
          this.profiling = true;
          for (let item of this.prodata) {
            for (let key in item.value) {
              if (typeof(item.value[key]) != "object") {
                item.value[key].toString();
              } else {
                let keysplit = key.split('-');
                let records = '';
                let record;
                for (let i in item.value[key]) {
                  let name, count;
                  for (let category in item.value[key][i]) {
                    if (category != "count") {
                      name = item.value[key][i][category];
                      count = item.value[key][i].count;
                    }
                  }
                  record = ' (' + name + ',' + count + ') ';
                  records += record;
                }
                delete item.value[key];
                key = key + ' (' + keysplit[0].split("_")[0] + ', count)';
                item.value[key] = records;
              }
            }
          }
          for (let key in this.data[0].value) {
            this.columnname.push(key);
          }
        }
      },
      err => {
        console.log("Error occurs when connect to elasticsearh!");
      }
    );
  }

  onResize(event) {
    this.resizeTreeMap();
  }

  resizeTreeMap() {
    $("#bigChartDiv").height($("#mainWindow").height());
    $("#bigChartDiv").width($("#mainWindow").width());
  }

  getData(metricName) {
  }

  ngAfterViewChecked() {
    $(".main-table").addClass('clone');
  }

  showDownloadSample() {
    this.visible = true;
    setTimeout(() => (this.visibleAnimate = true), 100);
  }

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => (this.visible = false), 300);
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains("close")) {
      this.hide();
    }
  }

  downloadSample(row) {
    this.isDownloadClickDisabled = !this.isDownloadClickDisabled;
    let urlDownload = this.serviceService.config.uri.missRecordDownload + "?jobName=" + row.name + "&ts=" + row.tmst;
    this.http
      .get(urlDownload,
        {responseType: 'blob', observe: 'response'})
      .map(res => {
        return {
          filename: row.name + "_" + row.tmst + '_missRecordSample.json',
          data: res.body
        };
      })
      .subscribe(res => {
        this.isDownloadClickDisabled = !this.isDownloadClickDisabled;
        console.log('start download:', res);
        var url = window.URL.createObjectURL(res.data);
        var a = document.createElement('a');
        document.body.appendChild(a);
        a.setAttribute('style', 'display: none');
        a.href = url;
        a.download = res.filename;
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
      }, error => {
        console.log('download error:', JSON.stringify(error));
      }, () => {
        console.log('Completed file download.')
      });
  }
}
