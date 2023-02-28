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

import {AfterViewInit, Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {TISService} from '../common/tis.service';

import {SchemaExpertAppCreateEditComponent, SchemaVisualizingEditComponent} from '../common/schema.expert.create.edit.component';
import {BasicFormComponent} from '../common/basic.form.component';
import {ConfirmDTO, EnginType, StupidModal} from './addapp-pojo';
import {NzModalService} from "ng-zorro-antd/modal";
import {TisResponseResult} from "../common/tis.plugin";
import {DataxDTO} from "./datax.add.component";
import {StepType} from "../common/steps.component";
import {NzTabsCanDeactivateFn} from "ng-zorro-antd/tabs";
// 文档：https://angular.io/docs/ts/latest/guide/forms.html
// schema 高级编辑模式入口：http://tis:8080/runtime/schema_manage.htm?aid=1
// 高级模式: http://tis:8080/runtime/schemaXml.ajax?aid=$aid&event_submit_do_get_xml_content=y&action=schema_action
// 小白模式: http://tis:8080/runtime/schemaManage.ajax?aid=$aid&event_submit_do_get_fields=y&action=schema_action


@Component({
  selector: 'schema-define',
  styles: [`

      [nz-button] {
          margin-right: 8px;
      }

  `
    , `

          .editor-up {
              border-width: 2px 2px 0 2px;
              border-color: #666666;
              border-style: dashed
          }

          .editor-below {
              border-width: 0 2px 2px 2px;
              border-color: #666666;
              border-style: dashed
          }

    `
  ],
  template: `
      <tis-steps [type]="stepInfo.type" [step]="stepInfo.step"></tis-steps>
      <tis-page-header [showBreadcrumb]="false" [result]="result">
          <button nz-button nzType="default" (click)="gotoProfileDefineStep()"><i nz-icon nzType="backward" nzTheme="outline"></i>上一步</button>
          <button nz-button nzType="primary" (click)="createIndexConfirm()"><i nz-icon nzType="forward" nzTheme="outline"></i>下一步</button>
      </tis-page-header>
      <nz-spin [nzSpinning]="formDisabled" nzSize="large">
          <form method="post">

              <nz-tabset [nzAnimated]="false" [nzTabBarExtraContent]="extraTemplate" [nzCanDeactivate]="canDeactivate">
                  <nz-tab [nzTitle]="foolTitleTemplate">
                      <visualizing-schema-editor [engineType]="this.engineType" [bizResult]="stupidModal"></visualizing-schema-editor>
                  </nz-tab>
                  <nz-tab [nzTitle]="expertTitleTemplate">
                      <ng-template nz-tab>
                          <expert-schema-editor [engineType]="this.engineType" [schemaXmlContent]="stupidModal.schemaXmlContent"
                                                (saveSuccess)="expertSchemaEditorSuccess($event)"
                                                (initComplete)="expertSchemaEditorInitComplete($event)"></expert-schema-editor>
                      </ng-template>
                  </nz-tab>
              </nz-tabset>

              <ng-template #extraTemplate>
              </ng-template>

              <ng-template #foolTitleTemplate>
                  <i class="fa fa-meh-o" aria-hidden="true"></i>小白模式
              </ng-template>
              <ng-template #expertTitleTemplate>
                  <i class="fa fa-code" aria-hidden="true"></i>专家模式
              </ng-template>
          </form>
      </nz-spin>
  `
})
export class AddAppDefSchemaComponent extends BasicFormComponent implements OnInit, AfterViewInit {

  @Output('preStep') preStep = new EventEmitter<any>();
  // 下一步 确认页面
  @Output('nextStep') nextStep = new EventEmitter<any>();

  stupidModal: StupidModal;

  stepInfo: { type: StepType, step: number } = {type: StepType.CreateIndex, step: 1};

  engineType: { type: EnginType, payload?: string } = {type: EnginType.Solr};
  _dto: ConfirmDTO;
  _dataxDto: DataxDTO;

  // 第一步中传递过来的提交信息
  @Input() set dto(val: ConfirmDTO | DataxDTO) {
    if (val instanceof ConfirmDTO) {
      // this.engineType = {type: EnginType.Solr};
      this._dto = val;
    } else {
      this.engineType = {type: EnginType.ES, payload: val.dataxPipeName};
      this._dataxDto = val;
      this.stepInfo = {type: val.processModel, step: 3};
    }
  }

  @ViewChild(SchemaExpertAppCreateEditComponent, {static: false}) private schemaExpertEditor: SchemaExpertAppCreateEditComponent;

  @ViewChild(SchemaVisualizingEditComponent, {static: false}) private schemaVisualtEditor: SchemaVisualizingEditComponent;

  // 当前的编辑模式处在什么模式
  expertModel = false;

  constructor(tisService: TISService, modalService: NzModalService) {
    super(tisService, modalService);
  }


  canDeactivate: NzTabsCanDeactivateFn = (fromIndex: number, toIndex: number) => {
    // console.log(fromIndex);
    switch (fromIndex) {
      case 0:
        return this.toggleModel(true)
      case 1:
        return this.toggleModel(false)
      default:
        throw new Error("fromIndex is error:" + fromIndex);
    }
  };

  // 下一步： 到索引创建确认页面
  public createIndexConfirm(): void {
    // 进行页面表单校验
    let dto = this._dto;
    let requestUrl = '/runtime/addapp.ajax?action=schema_action';
    switch (this.engineType.type) {
      case EnginType.Solr:
        break;
      case EnginType.ES: {
        dto = new ConfirmDTO();
        requestUrl = '/coredefine/corenodemanage.ajax?action=datax_action';
        dto.dataxName = this.engineType.payload;
        if (!this.engineType.payload) {
          throw new Error("engineType.payload can not be null");
        }
        break;
      }
    }

    dto.expertModel = this.expertModel;
    if (this.expertModel === true) {
      // 当前处在专家模式状态
      dto.expert = {xml: this.schemaExpertEditor.schemaXmlContent};
    } else {
      // 当前处在傻瓜化模式状态
      dto.stupid = {model: this.schemaVisualtEditor.stupidModel};
    }

    this.jsonPost(
      `${requestUrl}&emethod=goto_${this.engineType.type}_app_create_confirm`
      , (dto)).then((r) => {
      this.processResult(r);
      if (r.success) {
        switch (this.engineType.type) {
          case EnginType.ES: {
            this.nextStep.emit(this._dataxDto);
            break;
          }
          case EnginType.Solr: {
            this.nextStep.emit(dto);
            break;
          }
        }

      } else {
        this.expertSchemaEditorInitComplete(r);
      }
    });

  }

  public expertSchemaEditorInitComplete(e: TisResponseResult): void {
    this.expertModel = e.success;
  }


  // 名称空间是否有错误
  // public fieldNameInputErr(id: number): Observable<{ valid: boolean }> {
  //   const result = {valid: false};
  //   this.stupidModelValidateResult.set(id, result);
  //   return of(result);
  //   // this.stupidModelValidateResult.forEach((i) => {
  //   //   if (i.id === id && i.fieldInputBlank) {
  //   //     result.valid = true;
  //   //   }
  //   // });
  //   // return result.valid;
  // }
  // 切换视图状态
  public toggleModel(_expertModel: boolean): Promise<boolean> {
    // 判断当前状态和想要变化的状态是否一致
    if (this.formDisabled || this.expertModel === _expertModel) {
      return;
    }
    this.formDisabled = true;
    this.clearProcessResult();
    try {
      if (_expertModel === true) {
        return this.schemaVisualtEditor.saveAndMergeXml().then((schemaXmlContent: { success: boolean, schema: string }) => {
          this.stupidModal.schemaXmlContent = schemaXmlContent.schema;
          this.formDisabled = false;
          if (schemaXmlContent.success) {
            this.expertModel = _expertModel;
          }
          return schemaXmlContent.success;
        }, (_) => {
          this.formDisabled = false;
          return false;
        })
      } else {
        // 点击小白模式
        return this.schemaExpertEditor.doSaveContent().then((modal) => {
          this.stupidModal = modal.stupid;
          this.formDisabled = false;
          if (modal.success) {
            this.expertModel = _expertModel;
          }
          return modal.success;
        }, (_) => {
          this.formDisabled = false;
          return false;
        });
      }
    } catch (e) {
      this.formDisabled = false;
      throw e;
    }

  }

  // 接收专家模式下编辑成功之后消息
  public expertSchemaEditorSuccess(result: { bizresult: StupidModal, success: boolean }): void {
    // if (result.success) {
    //   this.setBizResult(result.bizresult);
    //   this.expertModel = false;
    // } else {
    //   this.processResult(result);
    // }
  }


  ngAfterViewInit(): void {
    // console.info(this.dto);
  }

  ngOnInit(): void {
    // FIXME: modify it
    switch (this.engineType.type) {
      case EnginType.ES:


        this.httpPost('/runtime/schemaManage.ajax'
          , "stepType=" + this.stepInfo.type + "&event_submit_do_get_es_tpl_fields=y&action=schema_action&dataxName=" + this._dataxDto.dataxPipeName)
          .then((r) => {
            if (r.success) {
              this.stupidModal = StupidModal.deseriablize(r.bizresult);
            }
          });

        break;
      case EnginType.Solr:
        if (!this._dto.stupid) {
          // 从app定义第一步进入
          let f = this._dto.appform;
          let workflow = f.workflow;
          // let tisTpl = f.tisTpl;
          // workflow = 'union';
          this.jsonPost('/runtime/schemaManage.ajax?event_submit_do_get_tpl_fields=y&action=schema_action', f)
          // this.httpPost('/runtime/schemaManage.ajax'
          //   , 'event_submit_do_get_tpl_fields=y&action=schema_action&wfname='
          //   + workflow)
            .then((r) => {
              if (r.success) {
                return r;
              } else {
                this.processResult(r);
              }
            })
            .then(
              (r: TisResponseResult) => {
                //   this.setBizResult(r.bizresult);
                this.stupidModal = StupidModal.deseriablize(r.bizresult);
                // console.log(this.stupidModal);
                this.setTplAppid(this.stupidModal);
              });
        } else {
          let d: ConfirmDTO = this._dto
          // 是从确认页面返回进入的,傻瓜化模式
          if (d.stupid) {
            this.stupidModal = d.stupid.model;
          }
        }
    }
  }


  private setTplAppid(o: StupidModal): void {
    if (o.tplAppId) {
      this._dto.tplAppId = o.tplAppId;
    }
  }

  public gotoProfileDefineStep(): void {
    switch (this.engineType.type) {
      case EnginType.ES:
        this.preStep.emit(this._dataxDto);
        return;
      case EnginType.Solr:
        this.preStep.emit(this._dto);
        return;
    }
  }
}



