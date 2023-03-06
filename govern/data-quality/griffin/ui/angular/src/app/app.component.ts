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
import {Component, Directive, ViewContainerRef, OnInit, AfterViewChecked} from "@angular/core";
import {Router} from "@angular/router";
import {XHRBackend} from '@angular/http';
import * as $ from "jquery";
import {ServiceService} from "./service/service.service";
import {UserService} from "./service/user.service";
import {Location, LocationStrategy, HashLocationStrategy} from "@angular/common";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.css"],
  providers: [ServiceService, UserService]
})
export class AppComponent implements AfterViewChecked, OnInit {
  title = "app";
  ntAccount: string;
  timestamp: Date;
  fullName: string;

  onResize(event) {
    this.resizeMainWindow();
  }

  goback() {
    this.location.back();
  }

  ngOnInit() {
    this.ntAccount = this.userService.getCookie("ntAccount");
    this.fullName = this.userService.getCookie("fullName");
  }

  constructor(
    private router: Router,
    private http: HttpClient,
    private location: Location,
    public serviceService: ServiceService,
    public userService: UserService
  ) {
  }

  resizeMainWindow() {
    $("#mainWindow").height(window.innerHeight - 56 - 20);
  }

  logout() {
    this.ntAccount = undefined;
    this.userService.setCookie("ntAccount", undefined, -1);
    this.userService.setCookie("fullName", undefined, -1);
    this.router.navigate(["login"]);
    window.location.reload();
    // window.location.replace ('login');
  }

  ngAfterViewChecked() {
    this.resizeMainWindow();
    $("#rightbar").css({
      height: $("#mainWindow").height() + 20
    });
    $("#side-bar-metrics").css({
      height:
      $("#mainContent").height() - $("#side-bar-stats").outerHeight()
    });
  }
}
