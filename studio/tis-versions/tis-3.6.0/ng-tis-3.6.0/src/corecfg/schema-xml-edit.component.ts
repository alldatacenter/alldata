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

import {Component, OnInit, Input, ViewChild, ViewContainerRef, ContentChild, TemplateRef} from '@angular/core';
import {TISService} from '../common/tis.service';

// import {ScriptService} from '../service/script.service';
import {AbstractSchemaEditComponent, BasicEditComponent} from './basic.edit.component';
import {EditorConfiguration} from "codemirror";
import {FieldErrorInfo, StupidModal} from "../base/addapp-pojo";
import {ActivatedRoute, Params, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {AbstractControl, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SchemaVisualizingEditComponent} from "../common/schema.expert.create.edit.component";

export declare type TisResType = 'schema.xml' | 'solrconfig.xml';

// Schema编辑
@Component({
  // templateUrl: '/runtime/jarcontent/schema.htm',
  template: `
      <tis-page-header [breadcrumb]="['配置变更','../../../snapshotset']" [showBreadcrumb]="true" [showBreadcrumbRoot]="false" [result]="result" [title]="pageTitle">
          <button nz-button nzType="primary"
                  name="event_submit_do_save_content" (click)="doSaveContent()">
              <i nz-icon nzType="save" nzTheme="outline"></i>保存
          </button>
      </tis-page-header>
      <form id="contentform" method="post" #editform>
          <div id="res_param"></div>
          <textarea name="memo" id="memo" style="display:none;" cols="16"></textarea>
          <div id="config-content">
              <input type="hidden" *ngFor="let p of editFormAppendParam" name="{{p.name}}" value="{{p.val}}"/>
              <input type="hidden" name="snapshot" [(ngModel)]="snid"/>
              <input type="hidden" name="filename" value="schema"/>
              <tis-codemirror name="schemaContent" [(ngModel)]="model.content" [config]="codeMirrirOpts" [size]="{width:'100%',height:'100%'}"></tis-codemirror>
          </div>
      </form>
      <ng-template #memoblock>
          <form nz-form [formGroup]="memoForm">
              <nz-form-item>
                  <nz-form-control [nzSpan]="20" nzErrorTip="最小长度5个字符">
                      <textarea name="memo" rows="4" style="width: 100%" nz-input formControlName="memo" [(ngModel)]="this.model.memo"></textarea>
                  </nz-form-control>
              </nz-form-item>
          </form>
      </ng-template>
  `,
  styles: [
      `
          #config-content {
              height: 800px;
          }`
  ]
  // 不知道为啥西面这个加style的方法不行
  // styles:['.schema-edit-modal .modal-dialog {max-width:1200px;}']
})
export class SchemaXmlEditComponent extends AbstractSchemaEditComponent {
  // private resType: TisResType = 'schema.xml';
  // @ContentChild(TemplateRef, {static: false})
  // contentTempate: TemplateRef<any>;
  constructor(fb: FormBuilder,
              tisService: TISService, nzmodalService: NzModalService
    , router: Router, route: ActivatedRoute, notification: NzNotificationService) {
    super(fb, tisService, nzmodalService, router, route, notification);
  }

  protected startSubmitRemoteServer() {

    this.jPost('/runtime/jarcontent/schema.ajax?action=save_file_content_action&event_submit_do_save_content=y'
      , this.model).then(result => {
      if (result.success) {
        this.router.navigate(['../../../snapshotset'], {relativeTo: this.route});
        this.notification.create('success', '成功', result.msg[0]);
      }
    });
  }


  // public doSaveContent(): void {
  //
  //   this.model.filename = this.pageTitle;
  //   this.model.snapshotid = this.snid;
  //
  //   this.modalService.confirm({
  //     nzTitle: '日志',
  //     nzContent: this.memoblock,
  //     nzOkText: '提交',
  //     nzCancelText: '取消',
  //     nzOnOk: () => {
  //       if (!this.submitForm()) {
  //         return false;
  //       }
  //       this.jPost('/runtime/jarcontent/schema.ajax?action=save_file_content_action&event_submit_do_save_content=y'
  //         , this.model).then(result => {
  //         if (result.success) {
  //           this.router.navigate(['../../../snapshotset'], {relativeTo: this.route});
  //           this.notification.create('success', '成功', result.msg[0]);
  //         }
  //       });
  //
  //     }
  //   });
  // }


  // protected getResType(params: Params): string {
  //   this.pageTitle = super.getResType(params);
  //   return this.pageTitle;
  // }

  get codeMirrirOpts(): EditorConfiguration {
    return {
      mode: {name: 'xml', alignCDATA: true},
      lineNumbers: true
    };
  }

  // protected afterSaveContent(result: any): void {
  //
  //   if (result.success) {
  //
  //     // if (result.success) {
  //     //   this.afterSaveContent(result.bizresult);
  //     // } else {
  //     //
  //     // }
  //     // setTimeout(() => {
  //     //   this.activeModal.close(result.bizresult.snapshot);
  //     // }, 1000);
  //   }
  // }
}


@Component({
  // templateUrl: '/runtime/jarcontent/schema.htm',
  template: `

      <tis-page-header [breadcrumb]="['配置变更','../../snapshotset']" [showBreadcrumb]="true" [showBreadcrumbRoot]="false" [result]="result" [title]="'Schema编辑'">
          <button nz-button nzType="primary"
                  name="event_submit_do_save_content" (click)="doSaveContent()">
              <i nz-icon nzType="save" nzTheme="outline"></i>保存
          </button>
      </tis-page-header>
      <nz-spin [nzSpinning]="this.formDisabled" nzSize="large">
          <visualizing-schema-editor [bizResult]="stupidModal"></visualizing-schema-editor>
      </nz-spin>
      <ng-template #memoblock>
          <form nz-form [formGroup]="memoForm">
              <nz-form-item>
                  <nz-form-control [nzSpan]="20" nzErrorTip="最小长度5个字符">
                      <textarea name="memo" rows="4" style="width: 100%" nz-input formControlName="memo" [(ngModel)]="this.model.memo"></textarea>
                  </nz-form-control>
              </nz-form-item>
          </form>
      </ng-template>
  `,
  styles: [
      ` #config-content {
          height: 800px;
      }`
  ]
  // 不知道为啥西面这个加style的方法不行
  // styles:['.schema-edit-modal .modal-dialog {max-width:1200px;}']
})
export class SchemaEditVisualizingModelComponent extends AbstractSchemaEditComponent {
  resType: TisResType = 'schema.xml';
  stupidModal: StupidModal = new StupidModal();
  @ViewChild(SchemaVisualizingEditComponent, {static: false}) private schemaVisualtEditor: SchemaVisualizingEditComponent;

  constructor(fb: FormBuilder,
              tisService: TISService, nzmodalService: NzModalService
    , router: Router, route: ActivatedRoute, notification: NzNotificationService) {
    super(fb, tisService, nzmodalService, router, route, notification);
  }

  protected startSubmitRemoteServer() {
    // console.log(this.stupidModal);
    let postModel = this.schemaVisualtEditor.stupidModel;
    this.jsonPost('/runtime/jarcontent/schema.ajax?action=schema_action&event_submit_do_save_by_expert_model=y'
      , {'visualizingForm': postModel, 'meta': this.model}).then(result => {
     // console.log(result);
      if (result.success) {
        this.router.navigate(['../../snapshotset'], {relativeTo: this.route});
        // this.notification.create('success', '成功', result.msg[0]);
       // this.successNotify(result.msg[0])
      } else {
        let errs: Array<FieldErrorInfo> = result.bizresult;
       // console.log(result.success);
        errs.forEach((err) => {
          postModel.markFieldErr(err);
        });
        this.stupidModal = postModel;
      }
    });
  }


  protected getResType(params: Params): string {
    return this.resType;
  }

  get executeAction(): string {
    return 'schema_action';
  }

  protected getExecuteMethod(): string {
    return 'do_get_fields_by_snapshot_id';
  }

  protected processConfigResult(conf: any): void {
    // console.log(conf);
    this.stupidModal = StupidModal.deseriablize(conf);
  }

  protected afterSaveContent(result: any): void {
  }
}



