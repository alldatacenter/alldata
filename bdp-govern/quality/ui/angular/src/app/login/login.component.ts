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
import {ServiceService} from "../service/service.service";
import {UserService} from "../service/user.service";
import {Router} from "@angular/router";
import {LocationStrategy, HashLocationStrategy} from "@angular/common";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: "app-login",
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"],
  providers: [ServiceService, UserService]
})
export class LoginComponent implements OnInit {
  ntAccount: string;
  timestamp: Date;
  fullName: string;
  results: any;

  constructor(
    private router: Router,
    private http: HttpClient,
    public serviceService: ServiceService,
    public userService: UserService
  ) {
  }

  loginBtnWait() {
    $("#login-btn")
      .addClass("disabled")
      .text("Logging in......");
  }

  loginBtnActive() {
    $("#login-btn")
      .removeClass("disabled")
      .text("Log in");
  }

  showLoginFailed() {
    $("#loginMsg")
      .show()
      .text("Login failed. Try again.");
  }

  // resizeMainWindow(){
  //     $('#mainWindow').height(window.innerHeight-50);
  // }

  submit(event) {
    if (event.which == 13) {
      //enter
      event.preventDefault();
      $("#login-btn").click();
      $("#login-btn").focus();
    }
  }

  focus($event) {
    $("#loginMsg").hide();
  }

  login() {
    var name = $("input:eq(0)").val();
    var password = $("input:eq(1)").val();
    var loginUrl = this.serviceService.config.uri.login;
    this.loginBtnWait();
    this.http.post(loginUrl, {username: name, password: password}).subscribe(
      data => {
        this.results = data;
        if (this.results.status == 0) {
          //logon success
          if ($("input:eq(2)").prop("checked")) {
            this.userService.setCookie("ntAccount", this.results.ntAccount, 30);
            this.userService.setCookie("fullName", this.results.fullName, 30);
          } else {
            this.userService.setCookie("ntAccount", this.results.ntAccount, 0);
            this.userService.setCookie("fullName", this.results.fullName, 0);
          }
          this.loginBtnActive();
          window.location.replace("/");
        } else {
          this.showLoginFailed();
          this.loginBtnActive();
        }
      },
      err => {
        this.showLoginFailed();
        this.loginBtnActive();
      }
    );
  }

  ngOnInit() {
    this.ntAccount = this.userService.getCookie("ntAccount");
    this.fullName = this.userService.getCookie("fullName");
    this.timestamp = new Date();
  }
}
