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
import {Component} from "@angular/core";
import {ServiceService} from "../../../service/service.service";
import {TREE_ACTIONS, ITreeOptions} from "angular-tree-component";
import {ToasterService} from "angular2-toaster";
import * as $ from "jquery";
import {HttpClient} from "@angular/common/http";
import {ActivatedRoute, Router} from "@angular/router";
import {AfterViewChecked, ElementRef} from "@angular/core";
import {MeasureFormatService, Format} from "../../../service/measure-format.service";

@Component({
  selector: "app-raw",
  templateUrl: "./raw.component.html",
  providers: [ServiceService, MeasureFormatService],
  styleUrls: ["./raw.component.css"]
})
export class RawComponent implements AfterViewChecked {

  constructor(
    private elementRef: ElementRef,
    private toasterService: ToasterService,
    private measureFormatService: MeasureFormatService,
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
  }

  data = "";
  valid = false;
  Format: typeof Format = Format;
  format: Format;
  createResult: any;
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

  ngAfterViewChecked() {
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
    let measure2Save = this.measureFormatService.parse(this.data, this.format);
    console.log(measure2Save);
    let addModels = this.serviceService.config.uri.addModels;
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
          this.toasterService.pop("error", "Error!", response.message);
        }
        console.log("Error when creating measure");
      }
    );
  }

  onInputChange() {
    let format = this.measureFormatService.determineFormat(this.data);
    if (format) {
      this.format = format;
      this.valid = true;
    }
    else {
      this.format = null;
      this.valid = false;
    }
  }

  changeFormat(format: Format) {
    if (this.valid) {
      let content = this.measureFormatService.parse(this.data, this.format);
      this.data = this.measureFormatService.format(content, format);
      this.format = format;
    }
  }
}
