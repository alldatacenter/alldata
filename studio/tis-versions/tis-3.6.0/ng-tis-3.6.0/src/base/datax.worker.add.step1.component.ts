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

import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, BasicFormComponent, CurrentCollection} from "../common/basic.form.component";

import {NzModalService} from "ng-zorro-antd/modal";
import {HeteroList, PluginSaveResponse, PluginType, SavePluginEvent} from "../common/tis.plugin";
import {PluginsComponent} from "../common/plugins.component";
import {DataxWorkerDTO} from "../runtime/misc/RCDeployment";
import {ActivatedRoute, Router} from "@angular/router";
import {NzNotificationService} from "ng-zorro-antd/notification";


@Component({
  template: `
      <tis-steps [type]="this.dto.processMeta.stepsType" [step]="0"></tis-steps>
      <tis-page-header [showBreadcrumb]="false">
          <tis-header-tool>
              <button nz-button nzType="primary" (click)="createStep1Next()">下一步</button>
          </tis-header-tool>
      </tis-page-header>
      <nz-spin [nzSpinning]="this.formDisabled" class="item-block">
          <tis-plugins [formControlSpan]="20" [pluginMeta]="[pluginCategory]"
                       (afterSave)="afterSaveReader($event)" [savePlugin]="savePlugin" [showSaveButton]="false"
                       [shallInitializePluginItems]="false" [_heteroList]="hlist" #pluginComponent></tis-plugins>
      </nz-spin>
  `
})
export class DataxWorkerAddStep1Component extends AppFormComponent implements AfterViewInit, OnInit {
  hlist: HeteroList[] = [];
  savePlugin = new EventEmitter<SavePluginEvent>();
  @Input() dto: DataxWorkerDTO;
  @Output() nextStep = new EventEmitter<any>();
  @Output() preStep = new EventEmitter<any>();
  pluginCategory: PluginType = {name: 'datax-worker', require: true};

  constructor(tisService: TISService, route: ActivatedRoute, modalService: NzModalService) {
    super(tisService, route, modalService);
  }

  // get currentApp(): CurrentCollection {
  //   return new CurrentCollection(0, this.dto.processMeta.targetName);
  // }
  createStep1Next() {
    let e = new SavePluginEvent();
    e.notShowBizMsg = true;
    let appTisService: TISService = this.tisService;
    appTisService.currentApp = new CurrentCollection(0, this.dto.processMeta.targetName);
    e.basicModule = this;
    this.savePlugin.emit(e);
  }

  protected initialize(app: CurrentCollection): void {
  }

  ngAfterViewInit() {
  }


  ngOnInit(): void {

    // console.log(appTisService.currentApp);
    this.httpPost('/coredefine/corenodemanage.ajax'
      , `action=datax_action&emethod=worker_desc&targetName=${this.dto.processMeta.targetName}`)
      .then((r) => {
        if (r.success) {
          let rList = PluginsComponent.wrapDescriptors(r.bizresult.pluginDesc);
          let desc = Array.from(rList.values());
          this.hlist = PluginsComponent.pluginDesc(desc[0], this.pluginCategory)
        }
      });
  }

  afterSaveReader(e: PluginSaveResponse) {
    if (e.saveSuccess) {
      this.nextStep.emit(this.dto);
    }
  }


}

