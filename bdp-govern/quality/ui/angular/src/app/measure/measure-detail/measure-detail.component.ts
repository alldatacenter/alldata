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
import {ActivatedRoute, Router} from "@angular/router";
import "rxjs/add/operator/switchMap";
import {HttpClient} from "@angular/common/http";
import {ServiceService} from "../../service/service.service";
import {MeasureFormatService, Format} from "../../service/measure-format.service";

@Component({
  selector: "app-measure-detail",
  templateUrl: "./measure-detail.component.html",
  providers: [ServiceService, MeasureFormatService],
  styleUrls: ["./measure-detail.component.css"]
})
export class MeasureDetailComponent implements OnInit {
  currentId: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private measureFormatService: MeasureFormatService,
    public serviceService: ServiceService
  ) {
  }

  ruleData: any;
  getModelUrl: string;
  showFullRules: boolean;
  Format: typeof Format = Format;
  format: Format = Format.json;
  ruleDes = [];
  sourceLength: number;
  sourceDB: string;
  targetDB: string;
  sourceTable: string;
  targetTable: string;
  sourcesize: string;
  targetsize: string;
  sourcezone: string;
  targetzone: string;
  sourcewhere: string;
  targetwhere: string;
  sourcepath: string;
  targetpath: string;
  type: string;
  currentrule: string;

  fetchData(value, index) {
    var data = this.ruleData["data.sources"][index].connector;
    var size = value + "size";
    var zone = value + "zone";
    var where = value + "where";
    var path = value + "path";
    var database = value + "DB";
    var table = value + "Table";
    this[size] = data["data.unit"];
    this[zone] = data["data.time.zone"];
    this[where] = data.config.where;
    if (data.predicates.length !== 0) {
      this[path] = data.predicates[0].config.path;
    }
    this[database] = data.config.database;
    this[table] = data.config["table.name"];
  }

  ngOnInit() {
    this.ruleData = {
      evaluateRule: ""
    };
    let getModel = this.serviceService.config.uri.getModel;
    this.currentId = this.route.snapshot.paramMap.get("id");
    this.getModelUrl = getModel + "/" + this.currentId;
    this.http.get(this.getModelUrl).subscribe(
      data => {
        this.ruleData = data;
        if (this.ruleData["measure.type"] === "external") {
          this.ruleData.type = this.ruleData["measure.type"].toLowerCase();
          this.ruleData.dqType = this.ruleData["dq.type"].toLowerCase();
        } else {
          this.ruleData.type = this.ruleData["dq.type"].toLowerCase();
          this.currentrule = this.ruleData["evaluate.rule"].rules;
          if (this.ruleData["rule.description"]) {
            this.ruleDes = this.ruleData["rule.description"].details
          }
          this.fetchData("source", 0);
          if (this.ruleData.type.toLowerCase() === "accuracy") {
            this.fetchData("target", 1);
          } else {
            this.targetDB = "";
            this.targetTable = "";
          }
        }
      },
      err => {
        console.log("error");
        // toaster.pop('error', 'Error when geting record', response.message);
      }
    );
  }

  getRawContent() {
    let content;
    if (!this.showFullRules) {
      content = (this.ruleData['evaluate.rule'] || {})['rules'];
    } else {
      content = this.ruleData;
    }
    return this.measureFormatService.format(content, this.format);
  }
}
