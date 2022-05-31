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
"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
  var c = arguments.length,
    r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
  if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
  else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
  return c > 3 && r && Object.defineProperty(target, key, r), r;
};
exports.__esModule = true;
var core_1 = require("@angular/core");
// import 'bootstrap/dist/css/bootstrap.css';
var AppComponent = (function () {
  function AppComponent() {
    this.title = 'app';
  }

  AppComponent = __decorate([
    core_1.Component({
      selector: 'app-root',
      template: "\n    <h1>Angular Router</h1>\n    <nav>\n      <a routerLink=\"/measures\" routerLinkActive=\"active\"> Measures</a>\n      <a routerLink=\"/jobs\">jobs</a>\n    </nav>\n    <router-outlet></router-outlet>\n  ",
      templateUrl: './app.component.html',
      styleUrls: ['./app.component.css']
    })
  ], AppComponent);
  return AppComponent;
}());
exports.AppComponent = AppComponent;
