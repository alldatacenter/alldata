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
import {DataTableModule} from "angular2-datatable";
import {Router} from "@angular/router";
import {FormControl} from "@angular/forms";
import {FormsModule} from "@angular/forms";
import {ServiceService} from "../service/service.service";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ToasterModule, ToasterService} from "angular2-toaster";
import * as $ from "jquery";

@Component({
  selector: "app-measure",
  templateUrl: "./measure.component.html",
  providers: [ServiceService],
  styleUrls: ["./measure.component.css"]
})
export class MeasureComponent implements OnInit {
  results: any;
  public visible = false;
  public visibleAnimate = false;
  deletedRow: any;
  sourceTable: string;
  targetTable: string;
  deleteId: number;
  deleteIndex: number;
  private toasterService: ToasterService;

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => (this.visible = false), 300);
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains("modal")) {
      this.hide();
    }
  }

  constructor(
    toasterService: ToasterService,
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
    this.toasterService = toasterService;
  }

  remove(row) {
    this.visible = true;
    setTimeout(() => (this.visibleAnimate = true), 100);
    this.deleteId = row.id;
    this.deleteIndex = this.results.indexOf(row);
    this.deletedRow = row;
    $("#save").removeAttr("disabled");
    if (this.deletedRow["measure.type"] !== "external") {
      var sourcedata = this.deletedRow["data.sources"][0].connector.config;
      this.sourceTable = sourcedata["table.name"];
    }
    if (this.deletedRow["dq.type"] === "accuracy") {
      var targetdata = this.deletedRow["data.sources"][1].connector.config;
      this.targetTable = targetdata["table.name"];
    } else {
      this.targetTable = "";
    }
  }

  confirmDelete() {
    var deleteModel = this.serviceService.config.uri.deleteModel;
    let deleteUrl = deleteModel + "/" + this.deleteId;
    $("#save").attr("disabled", "true");
    this.http.delete(deleteUrl).subscribe(
      data => {
        var self = this;
        setTimeout(function () {
          self.results.splice(self.deleteIndex, 1);
          self.hide();
        }, 200);
      },
      err => {
        this.toasterService.pop("error", "Error!", "Failed to delete measure!");
        console.log("Error when deleting measure!");
      }
    );
  }

  ngOnInit(): void {
    var allModels = this.serviceService.config.uri.allModels;
    this.http.get(allModels).subscribe(data => {
      // for(let measure in data){
      //   data[measure].trueName = data[measure].name;
      //   if(data[measure].type !== 'griffin'){
      //     data[measure].type = data[measure]['evaluate.rule'].rules[0]["dq.type"];
      //   }else{
      //     data[measure].type = '';
      //   }
      // }
      let trans = Object.keys(data).map(function (index) {
        let measure = data[index];
        if (measure["measure.type"] === "external") {
          measure["dq.type"] = "external";
        }
        //FIXME if needed avoiding null, for calling string.toLowerCase() in html <td>{{row["dq.type"].toLowerCase()}}</td>
        if (!!!measure["dq.type"]) {
          measure["dq.type"] = "unknown";
        }
        return measure;
      });
      // this.results = Object.assign([],trans).reverse();
      this.results = Object.assign([], trans).sort(function (a, b) {
        return b.id - a.id;
      });
    });
  }
}
