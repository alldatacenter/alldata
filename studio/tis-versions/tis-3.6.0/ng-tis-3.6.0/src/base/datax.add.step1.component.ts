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

import {Component, EventEmitter, OnInit, Output} from "@angular/core";
import {TISService} from "../common/tis.service";
import {NzModalService} from "ng-zorro-antd/modal";
import {Descriptor, HeteroList, Item, PluginSaveResponse, PluginType, SavePluginEvent} from "../common/tis.plugin";
import {PluginsComponent} from "../common/plugins.component";
import {BasicDataXAddComponent} from "./datax.add.base";
import {ActivatedRoute, Router} from "@angular/router";

// 文档：https://angular.io/docs/ts/latest/guide/forms.html
@Component({
  selector: 'addapp-form',
  template: `
      <tis-steps type="createDatax" [step]="0"></tis-steps>
      <nz-spin [nzSpinning]="this.formDisabled">
          <tis-steps-tools-bar [title]="'基本信息'" (cancel)="cancel()" (goOn)="createIndexStep1Next()"></tis-steps-tools-bar>
          <div style="width: 80%;margin: 0 auto;">
              <tis-plugins [formControlSpan]="20" [pluginMeta]="[pluginCategory]"
                           (afterSave)="afterSaveReader($event)" [savePlugin]="savePlugin" [showSaveButton]="false" [shallInitializePluginItems]="false" [_heteroList]="hlist" #pluginComponent></tis-plugins>
          </div>
      </nz-spin>
      <!-- Content here -->
  `
  , styles: [
      `
    `
  ]
})
export class DataxAddStep1Component extends BasicDataXAddComponent implements OnInit {
  errorItem: Item = Item.create([]);
  // model = new AppDesc();

  @Output() nextStep = new EventEmitter<any>();
  savePlugin = new EventEmitter<SavePluginEvent>();
  hlist: HeteroList[] = [];

  pluginCategory: PluginType;


  constructor(tisService: TISService, modalService: NzModalService, r: Router, route: ActivatedRoute) {
    super(tisService, modalService, r, route);
  }

  ngOnInit(): void {
    let dataxNameParam = '';
    if (this.dto.dataxPipeName) {
      dataxNameParam = `&dataxName=${this.dto.dataxPipeName}`;
    }

    this.pluginCategory = {name: 'appSource', require: true, extraParam: 'dataxName_' + this.dto.dataxPipeName}

    this.httpPost('/coredefine/corenodemanage.ajax'
      , 'action=datax_action&emethod=datax_processor_desc' + dataxNameParam)
      .then((r) => {
        if (r.success) {
          let hlist: HeteroList = PluginsComponent.wrapperHeteroList(r.bizresult, this.pluginCategory);
          if (hlist.items.length < 1) {
            Descriptor.addNewItem(hlist, hlist.descriptorList[0], false, (_, p) => p);
          }
          this.hlist = [hlist];
        }
      });
  }

  // 执行下一步
  public createIndexStep1Next(): void {
    let e = new SavePluginEvent();
    e.notShowBizMsg = true;
    this.savePlugin.emit(e);
  }

  afterSaveReader(event: PluginSaveResponse) {
    if (event.saveSuccess) {

      if (event.hasBiz()) {
        let pluginIdentityNames: Array<string> = event.biz();
        for (let i = 0; ; i++) {
          this.dto.dataxPipeName = pluginIdentityNames[i];
          break;
        }
        if (!this.dto.dataxPipeName) {
          throw new Error("have not set dataxPipeName properly");
        }
        this.nextStep.next(this.dto);
      } else {
        throw new Error("have not set biz result");
      }

    }
  }
}
