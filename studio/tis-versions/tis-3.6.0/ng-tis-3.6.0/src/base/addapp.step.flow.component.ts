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

// import { AdDirective } from './ad.directive';
// import { AdItem }      from './ad-item';
// import { AdComponent } from './ad.component';
// import {AddAppFormComponent} from './addapp-form.component';
import {AddAppFlowDirective} from './addapp.directive';
import {AddAppFormComponent} from './addapp-form.component';
import {AddAppDefSchemaComponent} from './addapp-define-schema.component';
import {
  Component, ViewChild, ComponentFactoryResolver, OnDestroy, ComponentRef, Type,
  AfterContentInit, ViewContainerRef
} from '@angular/core';
import {AddAppConfirmComponent} from './addapp-confirm.component';
import {AppDesc, ConfirmDTO, Option, StupidModal} from "./addapp-pojo";
import {AddappSelectNodesComponent} from "./addapp-select-nodes.component";
import {ActivatedRoute} from "@angular/router";
import {BasicFormComponent} from "../common/basic.form.component";
import {TISService} from "../common/tis.service";

import {NzModalService} from "ng-zorro-antd/modal";

@Component({
  template: `
      <ng-template #container></ng-template>
  `
})
export class AddAppStepFlowComponent extends BasicFormComponent implements AfterContentInit, OnDestroy {

  @ViewChild('container', {read: ViewContainerRef, static: true}) indexAddFlow: ViewContainerRef;

  private configFST: Map<any, { next: any, pre: any }>;


  constructor(private _componentFactoryResolver: ComponentFactoryResolver
    , private  route: ActivatedRoute, tisService: TISService, modalService: NzModalService) {
    super(tisService, modalService);
  }

  public preStepClick(e: any): void {
    // console.info('dddd');
  }

  ngAfterContentInit() {

    // 配置步骤前后跳转状态机
    this.configFST = new Map();


    this.configFST.set(AddAppFormComponent, {next: AddAppDefSchemaComponent, pre: null});

    this.configFST.set(AddAppDefSchemaComponent, {next: AddappSelectNodesComponent, pre: AddAppFormComponent});

    this.configFST.set(AddappSelectNodesComponent, {next: AddAppConfirmComponent, pre: AddAppDefSchemaComponent});
    // TODO :将来还需要设置机器选择页面
    this.configFST.set(AddAppConfirmComponent, {next: null, pre: AddappSelectNodesComponent});
    this.route.queryParams.subscribe((param) => {
      let name = param["name"];
      if (!!name) {
        let url = "/runtime/addapp.ajax";
        this.httpPost(url, `action=schema_action&emethod=get_app&name=${name}`)
          .then((r) => {
            if (!r) {
              return;
            }
            let info = r.bizresult.app;
            let dto = new ConfirmDTO();
            dto.recreate = true;
            dto.stupid = {model: StupidModal.deseriablize(r.bizresult.schema)};
            dto.appform = new AppDesc();
            dto.appform.name = info.projectName.substr(7);
            dto.appform.workflow = info.workflow;
            dto.appform.dptId = info.dptId;
            dto.appform.recept = info.recept;
            if (info.selectableDepartment) {
              dto.appform.dpts = [];
              info.selectableDepartment.forEach((rr: { name: string, value: string }) => {
                let o = new Option();
                o.value = rr.value;
                o.name = rr.name;
                dto.appform.dpts.push(o);
              });
            }
            // let dpt = new Option();
            // dpt.name = "测试部门";
            // dpt.value = "356";
            // dto.appform.dpts = [dpt];
            this.loadComponent(AddappSelectNodesComponent, dto);
          });
      } else {
        this.loadComponent(AddAppFormComponent, null);
      }
    });
    // this.loadComponent(AddAppDefSchemaComponent, dto);
  }

  ngOnDestroy() {
    // clearInterval(this.interval);
  }

  // 通过跳转状态机加载Component
  loadComponent(cpt: Type<any>, dto: any) {
    // var cpt = AddAppFormComponent;
    let componentRef = this.setComponentView(cpt);
    let nextCpt = this.configFST.get(cpt).next;
    let preCpt = this.configFST.get(cpt).pre;

    if (dto) {
      componentRef.instance.dto = dto;
    }

    // console.info({next: nextCpt, pre: preCpt});

    if (nextCpt !== null) {
      componentRef.instance.nextStep.subscribe((e: any) => {
          this.loadComponent(nextCpt, e);
        }
      );
    }

    if (preCpt !== null) {
      componentRef.instance.preStep.subscribe((e: any) => {
          this.loadComponent(preCpt, e);
        }
      );
    }
  }

  private setComponentView(component: Type<any>): ComponentRef<any> {
    let componentFactory = this._componentFactoryResolver.resolveComponentFactory(component);
    //
    // let viewContainerRef = this.indexAddFlow.viewContainerRef;
    // viewContainerRef.clear();
    this.indexAddFlow.clear();
    return this.indexAddFlow.createComponent(componentFactory);
  }


}
