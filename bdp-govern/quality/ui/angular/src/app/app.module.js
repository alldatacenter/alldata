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
var platform_browser_1 = require("@angular/platform-browser");
var core_1 = require("@angular/core");
var router_1 = require("@angular/router");
var app_component_1 = require("./app.component");
var appRoutes = [
  {path: 'health', component: app_component_1.AppComponent},
  {path: 'measures', component: app_component_1.AppComponent},
  {path: 'hero/:id', component: app_component_1.AppComponent},
  {path: 'mydashboard', component: app_component_1.AppComponent},
  {
    path: 'jobs', component: app_component_1.AppComponent,
    data: {title: 'Heroes List'}
  },
  {
    path: '',
    redirectTo: 'health',
    pathMatch: 'full'
  },
  {path: '**', component: app_component_1.AppComponent}
];
var AppModule = (function () {
  function AppModule() {
  }

  AppModule = __decorate([
    core_1.NgModule({
      declarations: [
        app_component_1.AppComponent
      ],
      imports: [
        platform_browser_1.BrowserModule,
        router_1.RouterModule.forRoot(appRoutes, {enableTracing: true} // <-- debugging purposes only
        )
      ],
      providers: [],
      bootstrap: [app_component_1.AppComponent]
    })
  ], AppModule);
  return AppModule;
}());
exports.AppModule = AppModule;
