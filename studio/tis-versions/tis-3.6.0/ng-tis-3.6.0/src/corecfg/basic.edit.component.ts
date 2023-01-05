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

import {Injectable, Input, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {TISService} from '../common/tis.service';
// import {ScriptService} from '../service/script.service';
import {BasicFormComponent} from '../common/basic.form.component';
import {EditorConfiguration} from "codemirror";
import {ActivatedRoute, Params, Router} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {AbstractControl, FormBuilder, FormGroup, Validators} from "@angular/forms";

declare var CodeMirror: any;
declare var document: any;
declare var jQuery: any;


// import {Observable} from 'rxjs/Observable';

@Injectable()
export abstract class BasicEditComponent extends BasicFormComponent implements OnInit {

  showSaveControl = true;

  public model: SchemaFormData = new SchemaFormData();
  // protected code: ElementRef;
  // protected codeMirror: any;
  // schemaContent: string;
  snid: number;

  // private resSnapshotid = new Subject<number>();

  constructor(tisService: TISService, modalService: NzModalService, protected route: ActivatedRoute, notification?: NzNotificationService) {
    super(tisService, modalService, notification);
  }

  @Input() set snapshotid(val: number) {
    this.snid = val;
    // this.resSnapshotid.next(val);
  }


  ngOnInit(): void {

    this.route.params
      .subscribe((params: Params) => {
        let snapshotid = params['snapshotid']
        if (!snapshotid) {
          throw new Error("can not find rout param restype");
        }
        // tslint:disable-next-line:radix
        this.snid = parseInt(snapshotid);
        this.httpPost('/runtime/jarcontent/schema.ajax'
          , 'action=' + this.executeAction + '&snapshot='
          + snapshotid + '&editable=false&restype='
          + this.getResType(params) + '&event_submit_' + this.getExecuteMethod() + '=y')
          .then(result => result.success ? result.bizresult : super.processResult(result))
          .then(result => this.processConfigResult(result));
      });
  }

  protected getResType(params: Params): string {
    let resType = params['restype']
    if (!resType) {
      throw new Error("can not find rout param restype");
    }
    if ('schema' === resType) {
      return 'schema.xml';
    } else if ('config' === resType) {
      return 'solrconfig.xml';
    } else {
      throw new Error(`restype:${resType} is illegal`);
    }
  }

  protected get executeAction(): string {
    return "save_file_content_action";
  }

  protected getExecuteMethod(): string {
    return 'do_get_config';
  }

  get codeMirrirOpts(): EditorConfiguration {
    return {
     // mode: {name: 'xml', alignCDATA: true},
      mode: {name: 'application/ld+json', alignCDATA: true},
      lineNumbers: true
    };
  }

  // 保存提交内容
  public doSaveContent(): void {
    // this.result = null;
    //
    // this.tisService.httpPost('/runtime/jarcontent/schema.ajax'
    //   , (jQuery(exitform)).serialize()).then(result => {
    //
    //   this.processResult(result);
    //   // if (result.success) {
    //   //   this.afterSaveContent(result.bizresult);
    //   // } else {
    //   //
    //   // }
    //   this.afterSaveContent(result);
    // });
  }


  public get editFormAppendParam(): { name: string, val: string }[] {
    return [];
  }

  protected abstract afterSaveContent(bizresult: any): void;

  protected processConfigResult(conf: any): void {
    this.initialView(conf);
  }

  // incrCount: number = 1;

  // public updateContent(): void {
  //   this.codeMirror.setValue(this.code.nativeElement.value);
  // }

  protected afterCodeSet(cfgContent: string): void {
  }

  private setCodeMirrorContent(cfgContent: string, initTime: boolean): void {
  }

  initialView(cfgContent: string): void {
    this.model.content = cfgContent;
  }
}


@Injectable()
export abstract class AbstractSchemaEditComponent extends BasicEditComponent {

  pageTitle: string;
  memoForm: FormGroup;

  @ViewChild('memoblock', {read: TemplateRef, static: true}) memoblock: TemplateRef<any>;

  constructor(protected fb: FormBuilder,
              tisService: TISService, nzmodalService: NzModalService
    , protected router: Router, route: ActivatedRoute, notification: NzNotificationService) {
    super(tisService, nzmodalService, route, notification);
  }

  ngOnInit(): void {
    super.ngOnInit();

    this.memoForm = this.fb.group({
      memo: [null, [Validators.required, Validators.minLength(5)]],
    });
  }

  submitForm(): boolean {
    for (const i in this.memoForm.controls) {

      let control: AbstractControl = this.memoForm.controls[i];
      control.markAsDirty();
      control.updateValueAndValidity();
      // TODO 最小文本长度的校验一时半会想不明白怎么搞
      // console.error(control.errors);
    }
    return this.memoForm.valid;
  }

  public doSaveContent(): void {

    this.model.filename = this.pageTitle;
    this.model.snapshotid = this.snid;

    this.modalService.confirm({
      nzTitle: '日志',
      nzContent: this.memoblock,
      nzOkText: '提交',
      nzCancelText: '取消',
      nzOnOk: () => {
        if (!this.submitForm()) {
          return false;
        }
        this.startSubmitRemoteServer()
      }
    });
  }


  protected getResType(params: Params): string {
    this.pageTitle = super.getResType(params);
    return this.pageTitle;
  }

  protected afterSaveContent(result: any): void {

    if (result.success) {
    }
  }

  protected abstract startSubmitRemoteServer();
}

export class SchemaFormData {
  snapshotid: number;
  content: string;
  filename: string;
  memo: string;
}



