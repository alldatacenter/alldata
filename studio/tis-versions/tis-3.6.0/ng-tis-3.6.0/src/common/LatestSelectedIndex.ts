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

import {CurrentCollection} from "./basic.form.component";
import {Application, AppType} from "./application";
import {LocalStorageService} from "angular-2-local-storage";
import {Router} from "@angular/router";

const maxQueueSize = 8;
const KEY_LOCAL_STORAGE_LATEST_INDEX = 'LatestSelectedIndex';

export class SelectedIndex {
  public timestamp: number;

  constructor(public name: string, public appType: AppType) {
    this.timestamp = (new Date()).getTime();
  }
}


export class LatestSelectedIndex {
  private _queue: Array<SelectedIndex> = [];

  public static popularSelectedIndex(_localStorageService: LocalStorageService): LatestSelectedIndex {
    let popularSelected: LatestSelectedIndex = _localStorageService.get(KEY_LOCAL_STORAGE_LATEST_INDEX);

    if (popularSelected) {
      popularSelected = Object.assign(new LatestSelectedIndex(), popularSelected); // $.extend(, );
    } else {
      popularSelected = new LatestSelectedIndex();
    }
    return popularSelected;
  }

  public static routeToApp(_localStorageService: LocalStorageService, r: Router, app: Application): Array<SelectedIndex> {
    // console.log(app);
    switch (app.appType) {
      case AppType.DataX:
        r.navigate(['/x/' + app.projectName]);
        break;
      case AppType.Solr:
        r.navigate(['/c/' + app.projectName]);
        break;
      default:
        throw new Error(`Error Type:${app.appType}`);
    }
    if (_localStorageService) {
      let popularSelected: LatestSelectedIndex = _localStorageService.get(KEY_LOCAL_STORAGE_LATEST_INDEX);
      if (!popularSelected) {
        popularSelected = new LatestSelectedIndex();
      } else {
        // Object.assign()
        popularSelected = Object.assign(new LatestSelectedIndex(), popularSelected);
      }
      // console.log(app);
      popularSelected.add(new SelectedIndex(app.projectName, app.appType));
      _localStorageService.set(KEY_LOCAL_STORAGE_LATEST_INDEX, popularSelected);
      // console.log(popularSelected.popularLatestSelected);
      // this.collectionOptionList = popularSelected.popularLatestSelected;
      return popularSelected.popularLatestSelected;
    }
  }

  public add(i: SelectedIndex): void {

    let find = this._queue.find((r) => r.name === i.name);
    if (find) {
      find.timestamp = i.timestamp;
      this._sort();
      return;
    }

    if (this._queue.length < maxQueueSize) {
      this._queue.push(i);
      this._sort();
      return;
    }

    let min = this._queue[0];
    let minIndex = 0;

    let s: SelectedIndex;
    for (let j = 0; j < this._queue.length; j++) {
      s = this._queue[j];
      // if (s.name === i.name) {
      //   s.count++;
      //   return;
      // }
      if (min.timestamp > s.timestamp) {
        min = s;
        minIndex = j;
      }
    }
    this._queue[minIndex] = i;
    // this._queue.sort((a, b) => (b.timestamp - a.timestamp));
    this._sort();
    // console.log(this._queue);
  }

  public addIfNotContain(idx: CurrentCollection): void {

    let find = this._queue.find((r) => {
      return r.name === idx.name;
    });

    if (find) {
      return;
    }

    this.add(new SelectedIndex(idx.name, idx.appTyp));
  }

  private _sort(): void {
    this._queue.sort((a, b) => (b.timestamp - a.timestamp));
  }

  public get popularLatestSelected(): Array<SelectedIndex> {
    // return this._queue.map((r) => r.name);
    return this._queue;
  }

}


