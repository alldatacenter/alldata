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
import {Component, OnInit, AfterViewInit} from "@angular/core";
import {FormControl} from "@angular/forms";
import {FormsModule} from "@angular/forms";

import {TREE_ACTIONS, KEYS, IActionMapping, ITreeOptions} from "angular-tree-component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ToasterModule, ToasterService, ToasterConfig} from "angular2-toaster";
import * as $ from "jquery";
import {HttpService} from '../../service/http.service';
import {Router} from "@angular/router";

@Component({
  selector: "app-create-measure",
  templateUrl: "./create-measure.component.html",
  styleUrls: ["./create-measure.component.css"]
})
export class CreateMeasureComponent implements AfterViewInit {
  constructor(private router: Router) {
  }

  click(type) {
    this.router.navigate(["/createmeasure" + type]);
  }

  ngAfterViewInit() {
    $("#panel-2 >.panel-body").css({
      height:
      $("#panel-1 >.panel-body").outerHeight() +
      $("#panel-1 >.panel-footer").outerHeight() -
      $("#panel-2 >.panel-footer").outerHeight()
    });
    $("#panel-4 >.panel-body").css({
      height:
      $("#panel-3 >.panel-body").outerHeight() +
      $("#panel-3 >.panel-footer").outerHeight() -
      $("#panel-4 >.panel-footer").outerHeight()
    });
  }
}
