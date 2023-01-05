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

// import {Injectable} from '@angular/core';
// import {ScriptStore} from './script.store';
//
// declare var document: any;
//
// @Injectable()
// export class ScriptService {
//
//   private scripts: any = {};
//
//   constructor() {
//     ScriptStore.forEach((script: any) => {
//       this.scripts[script.name] = {
//         loaded: false,
//         src: script.src
//       };
//     });
//   }
//
//   load(...scripts: string[]): Promise<any[]> {
//     let promises: any[] = [];
//     scripts.forEach((script) => promises.push(this.loadScript(script)));
//     return Promise.all(promises);
//   }
//
//   loadScript(name: string) {
//     return new Promise((resolve, reject) => {
//       // resolve if already loaded
//       if (this.scripts[name].loaded) {
//         resolve({script: name, loaded: true, status: 'Already Loaded'});
//       } else {
//         // load script
//         let script = document.createElement('script');
//         script.type = 'text/javascript';
//         script.src = this.scripts[name].src;
//         if (script.readyState) {  // IE
//           script.onreadystatechange = () => {
//             if (script.readyState === 'loaded' || script.readyState === 'complete') {
//               script.onreadystatechange = null;
//               this.scripts[name].loaded = true;
//               resolve({script: name, loaded: true, status: 'Loaded'});
//             }
//           };
//         } else {  // Others
//           script.onload = () => {
//             this.scripts[name].loaded = true;
//             resolve({script: name, loaded: true, status: 'Loaded'});
//           };
//         }
//         script.onerror = (error: any) => resolve({script: name, loaded: false, status: 'Loaded'});
//         document.getElementsByTagName('head')[0].appendChild(script);
//       }
//     });
//   }
//
// }
