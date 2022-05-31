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
import {FormControl} from "@angular/forms";
import {FormsModule} from "@angular/forms";
import {ServiceService} from "../../../service/service.service";
import {TREE_ACTIONS, KEYS, IActionMapping, ITreeOptions} from "angular-tree-component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ToasterModule, ToasterService, ToasterContainerComponent} from "angular2-toaster";
import * as $ from "jquery";
import {HttpClient} from "@angular/common/http";
import {Router} from "@angular/router";
import {DataTableModule} from "angular2-datatable";
import {AfterViewChecked, ElementRef} from "@angular/core";
import {AngularMultiSelectModule} from "angular2-multiselect-dropdown/angular2-multiselect-dropdown";
import {ConfigurationComponent} from "../configuration/configuration.component";

interface pubMeasure {
  name: string;
  metricName: string;
  description: string;
  measureType: string;
  dqType: string;
  owner: string
}

export function createPubMeasure(name: string,
                                 metricName: string,
                                 description: string,
                                 measureType: string,
                                 dqType: string,
                                 owner: string) {
  return {
    name,
    metricName,
    description,
    measureType,
    dqType,
    owner
  }
}

@Component({
  selector: "app-pub",
  templateUrl: "./pub.component.html",
  providers: [ServiceService],
  styleUrls: ["./pub.component.css"]
})
export class PubComponent implements AfterViewChecked, OnInit {
  newMeasure = createPubMeasure("", "", "", "external", "ACCURACY", "test");
  dqTypeOptions = ["accuracy"];
  createResult: any;
  private toasterService: ToasterService;
  public visible = false;
  public visibleAnimate = false;

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => (this.visible = false), 300);
    $("#save").removeAttr("disabled");
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains("modal")) {
      this.hide();
    }
  }

  onResize(event) {
    this.resizeWindow();
  }

  resizeWindow() {
    var stepSelection = ".formStep";
    $(stepSelection).css({
      height: window.innerHeight - $(stepSelection).offset().top
    });
    $("fieldset").height(
      $(stepSelection).height() -
      $(stepSelection + ">.stepDesc").height() -
      $(".btn-container").height() -
      130
    );
    $(".y-scrollable").css({
      height: $("fieldset").height()
    });
  }


  formValidation = function (step) {
    if (step == undefined) {
      step = this.currentStep;
    }
    if (step == 1) {
      return this.selection && this.selection.length > 0;
    } else if (step == 2) {
      var len = 0;
      var selectedlen = 0;
      for (let key in this.selectedItems) {
        selectedlen++;
        len = this.selectedItems[key].length;
        if (len == 0) {
          return false;
        }
      }
      return this.selection.length == selectedlen ? true : false;
    } else if (step == 3) {
      return true;
    } else if (step == 4) {
    }
    return false;
  };

  submit(form) {
    if (!form.valid) {
      this.toasterService.pop(
        "error",
        "Error!",
        "please complete the form in this step before proceeding"
      );
      return false;
    }
    this.visible = true;
    setTimeout(() => (this.visibleAnimate = true), 100);
  }


  save() {
    var measure2Save = {
      name: this.newMeasure.name,
      "metric.name": this.newMeasure.metricName,
      "measure.type": this.newMeasure.measureType,
      description: this.newMeasure.description,
      "dq.type": this.newMeasure.dqType.toUpperCase(),
      owner: this.newMeasure.owner
    }
    console.log(measure2Save);
    var addModels = this.serviceService.config.uri.addModels;
    $("#save").attr("disabled", "true");
    this.http.post(addModels, measure2Save).subscribe(
      data => {
        this.createResult = data;
        this.hide();
        this.router.navigate(["/measures"]);
      },
      err => {
        let response = JSON.parse(err.error);
        if (response.code === '40901') {
          this.toasterService.pop("error", "Error!", "Measure name already exists!");
        } else {
          this.toasterService.pop("error", "Error!", "Error when creating measure");
        }
        console.log("Error when creating measure");
      }
    );
  }

  options: ITreeOptions = {
    displayField: "name",
    isExpandedField: "expanded",
    idField: "id",
    actionMapping: {
      mouse: {
        click: (tree, node, $event) => {
          if (node.hasChildren) {

            TREE_ACTIONS.TOGGLE_EXPANDED(tree, node, $event);
          } else if (node.data.cols) {


          }
        }
      }
    },
    animateExpand: true,
    animateSpeed: 30,
    animateAcceleration: 1.2
  };

  nodeList: object[];
  nodeListTarget: object[];

  constructor(
    private elementRef: ElementRef,
    toasterService: ToasterService,
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
    this.toasterService = toasterService;
  }

  ngOnInit() {
    var allDataassets = this.serviceService.config.uri.dataassetlist;
    this.http.get(allDataassets).subscribe(data => {
      this.nodeList = new Array();

    });

  }

  ngAfterViewChecked() {
    this.resizeWindow();
  }
}
