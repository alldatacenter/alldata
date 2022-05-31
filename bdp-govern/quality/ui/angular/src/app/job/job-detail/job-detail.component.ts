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
import {Component, OnInit} from '@angular/core';
import {ServiceService} from "../../service/service.service";
import {HttpClient} from "@angular/common/http";
import {Router, ActivatedRoute, ParamMap} from "@angular/router";

@Component({
  selector: 'app-job-detail',
  templateUrl: './job-detail.component.html',
  providers: [ServiceService],
  styleUrls: ['./job-detail.component.css']
})
export class JobDetailComponent implements OnInit {

  currentId: string;
  jobData: any;
  measureData: any;
  jobName: string;
  cronExp: string;
  cronTimeZone: string;
  measureName: string;
  measureType: string;
  processType: string;
  rangeConfig = [];
  tableInfo = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    public serviceService: ServiceService) {
  }

  getMeasureById(measureId) {
    let url = this.serviceService.config.uri.getModel + "/" + measureId;
    this.http.get(url).subscribe(
      data => {
        this.measureData = data;
        this.measureName = this.measureData.name;
        this.measureType = this.measureData["dq.type"].toLowerCase();
        this.processType = this.measureData["process.type"].toLowerCase();
        for (let item of this.measureData["data.sources"]) {
          let config = item.connector.config;
          let tableName = config.database + "." + config["table.name"];
          this.tableInfo.push(tableName);
        }
      },
      err => {
        console.log("error");
      }
    );
  }

  ngOnInit() {
    this.currentId = this.route.snapshot.paramMap.get("id");
    var getJobById = this.serviceService.config.uri.getJobById + "?jobId=" + this.currentId;
    this.http.get(getJobById).subscribe(
      data => {
        this.jobData = data;
        this.jobName = this.jobData["job.name"];
        this.cronExp = this.jobData["cron.expression"];
        this.cronTimeZone = this.jobData["cron.time.zone"];
        let mesureId = this.jobData["measure.id"];
        this.getMeasureById(mesureId);
        for (let item of this.jobData["data.segments"]) {
          this.rangeConfig.push(item["segment.range"]);
        }
      },
      err => {
        console.log("error");
        // toaster.pop('error', 'Error when geting record', response.message);
      }
    );
  }

}
