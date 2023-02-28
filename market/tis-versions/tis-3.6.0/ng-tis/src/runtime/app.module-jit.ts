/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule} from "@angular/forms";
import {RouterModule} from "@angular/router";
// import {HttpModule, JsonpModule} from "@angular/http";
// import {NZ_I18N, zh_CN} from 'ng-zorro-antd';
import {registerLocaleData} from '@angular/common';
import zh from '@angular/common/locales/zh';
import {AppComponent} from "./app.component";
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {TISService} from "../common/tis.service";
//
// import {ScriptService} from "../service/script.service";
import {TisCommonModule} from "../common/common.module";
import {CorenodemanageComponent} from "./corenodemanage.component";
import {RootWelcomeComponent} from "./root-welcome-component";
import {MarkdownModule, MarkedOptions, MarkedRenderer} from 'ngx-markdown';
import {NZ_I18N, zh_CN} from "ng-zorro-antd/i18n";


// export function offlineModuleFactory() {
//   return import("../offline/offline.module").then(mod => mod.OfflineModule);
// }
//
// export function usrModuleFactory() {
//   return import("../user/user.module").then(mod => mod.UserModule);
// }
//
// export function coreModuleFactory() {
//   return import("./core.node.manage.module").then(mod => mod.CoreNodeManageModule);
// }
//
// export function baseModuleFactory() {
//   return import("../base/base.manage.module").then(mod => mod.BasiManageModule);
// }

registerLocaleData(zh);

export function markedOptionsFactory(): MarkedOptions {
  const renderer = new MarkedRenderer();

  renderer.link = (href: string | null, title: string | null, text: string) => {
    return `<a href="${href}" target="_blank">${text}</a>`;
  };
  // renderer.code = (code: string, language: string | undefined, isEscaped: boolean) => {
  //   return '<code></code>';
  // };

  return {
    renderer: renderer
  };
}

// https://github.com/angular/angular/issues/11075 loadChildren 子模块不支持aot编译的问题讨论
// router 的配置
@NgModule({
  id: 'tisRoot',
  imports: [BrowserModule, FormsModule, TisCommonModule,
    BrowserAnimationsModule,
    MarkdownModule.forRoot({
      markedOptions: {
        provide: MarkedOptions,
        useFactory: markedOptionsFactory
      }
    }),
    RouterModule.forRoot([
      {  // 索引一览
        path: '',
        component: RootWelcomeComponent
      },
      {  // 索引一览
        path: 'base',
        loadChildren: () => import( "../base/base.manage.module").then(m => m.BasiManageModule)
      },
      {  // 用户权限
        path: 'usr',
        loadChildren: () => import("../user/user.module").then(m => m.UserModule)
      },
      {   // 离线模块
        path: 'offline',
        loadChildren: () => import("../offline/offline.module").then(m => m.OfflineModule)
      },
      {   // 索引控制台
        path: 'c/:name',
        loadChildren: () => import("./core.node.manage.module").then(m => m.CoreNodeManageModule)
      },
      {   // datax控制台
        path: 'x/:name',
        loadChildren: () => import("../datax/datax.module").then(m => m.DataxModule)
      }
    ])
  ],
  declarations: [AppComponent, RootWelcomeComponent
    // CodemirrorComponent///
  ],
  exports: [],
  entryComponents: [],
  bootstrap: [AppComponent],
  // bootstrap: [CodemirrorComponent],
  providers: [TISService, {provide: NZ_I18N, useValue: zh_CN}]

})
export class AppModule {
  constructor() {
  }
}
