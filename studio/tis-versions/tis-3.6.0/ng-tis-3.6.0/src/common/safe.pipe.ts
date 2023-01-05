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

import {Pipe, PipeTransform} from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';
import {HeteroList, Item, PluginType} from "./tis.plugin";

@Pipe({name: 'safe'})
export class SafePipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {
  }

  public transform(url: string): any {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}

@Pipe({
  name: 'pluginDescCallback',
  pure: false
})
export class PluginDescCallbackPipe implements PipeTransform {
  transform(items: any[], h: HeteroList, pluginMetas: PluginType[], callback: (h: HeteroList, pluginMetas: PluginType[], item: any) => boolean): any {
    if (!items || !callback) {
      return items;
    }
    return items.filter(item => callback(h, pluginMetas, item));
  }
}

// @Pipe({
//   name: 'itemFilter',
//   pure: false
// })
// export class PluginItemFilterCallbackPipe implements PipeTransform {
//   transform(items: Item[], callback: (item: Item) => boolean): any {
//     if (!items || !callback) {
//       return items;
//     }
//     return items.filter(item => callback(item));
//   }
// }



