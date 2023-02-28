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

import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Input, OnInit, ViewChild} from "@angular/core";
import {TISService} from "../common/tis.service";
import {AppFormComponent, BasicFormComponent, CurrentCollection} from "../common/basic.form.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzDrawerRef, NzDrawerService} from "ng-zorro-antd/drawer";
import {NzNotificationService} from "ng-zorro-antd/notification";
import {HeteroList, Item, PluginSaveResponse} from "../common/tis.plugin";
import {BasicDataXAddComponent} from "./datax.add.base";
import {ActivatedRoute, Router} from "@angular/router";
import {StepType} from "../common/steps.component";
import {DataxDTO} from "./datax.add.component";
import {CodemirrorComponent} from "../common/codemirror.component";

export enum ExecModel {
  Create, Reader
}


// 文档：https://angular.io/docs/ts/latest/guide/forms.html
@Component({
  selector: "datax-config",
  template: `
      <tis-steps *ngIf="createModel" [type]="stepType" [step]="offsetStep(4)"></tis-steps>
      <!--      <tis-page-header [showBreadcrumb]="false" [result]="result">-->
      <!--          <tis-header-tool>-->
      <!--              <button nz-button nzType="default">上一步</button>&nbsp;<button nz-button nzType="primary" (click)="createStepNext()">创建</button>-->
      <!--          </tis-header-tool>-->
      <!--      </tis-page-header>-->
      <nz-spin [nzSpinning]="this.formDisabled">
          <ng-container [ngSwitch]="createModel">
              <tis-steps-tools-bar *ngSwitchCase="true" (cancel)="cancel()" [goBackBtnShow]="_offsetStep>0" (goBack)="goback()">
                  <final-exec-controller [ngSwitch]="createDataXStep">
                      <button *ngSwitchCase="true" nz-button nzType="primary" (click)="createStepNext()"><i nz-icon nzType="rocket" nzTheme="outline"></i>创建</button>
                      <button *ngSwitchCase="false" nz-button nzType="primary" (click)="updateStepNext()"><i nz-icon nzType="rocket" nzTheme="outline"></i>更新</button>
                  </final-exec-controller>
              </tis-steps-tools-bar>
              <div *ngSwitchCase="false" class="fix-foot">

                  <ng-container [ngSwitch]="inUpdate">
                      <button nz-button *ngSwitchCase="false" (click)="startUpdate()"><i nz-icon nzType="edit" nzTheme="outline"></i>编辑基本信息</button>&nbsp;
                      <button nz-button *ngSwitchCase="false" (click)="startEditReader()"><i nz-icon nzType="edit" nzTheme="outline"></i>Reader</button>&nbsp;
                      <button nz-button *ngSwitchCase="false" (click)="startEditWriter()"><i nz-icon nzType="edit" nzTheme="outline"></i>Writer</button>&nbsp;
                      <button nz-button *ngSwitchCase="true" nzType="primary" nzDanger (click)="inUpdate = false"><i nz-icon nzType="edit" nzTheme="outline"></i>取消编辑</button>
                  </ng-container>
                  &nbsp;
                  <button nz-button [disabled]="inUpdate" nzType="primary" (click)="reGenerate()"></button>
                  <button nz-button nz-dropdown nzType="primary" [nzDropdownMenu]="dataxScriptfiles">
                      生成脚本文件
                      <i nz-icon nzType="down"></i>
                  </button>
                  <nz-dropdown-menu #dataxScriptfiles="nzDropdownMenu">
                      <ul nz-menu>
                          <li nz-menu-item (click)="reGenerate()">DataX配置文件</li>
                          <li nz-menu-item (click)="reGenerateSqlDDL()">SQL DDL</li>
                      </ul>
                  </nz-dropdown-menu>
              </div>
          </ng-container>
          <h3>DataX脚本文件</h3>
          <ul class="item-block child-block script-block">
              <li *ngFor="let f of genCfgFileList">
                  <button (click)="viewDataXCfg(f)" nz-button nzType="link" nzSize="large"><i nz-icon nzType="file-text" nzTheme="outline"></i>{{f}}</button>
              </li>
              <i style="color:#777777;font-size: 10px">生成时间：{{lastestGenFileTime | date : "yyyy/MM/dd HH:mm:ss"}}</i>
          </ul>
          <ng-container *ngIf="createDDLFileList.length > 0">
              <h3>Create Table DDL</h3>

              <ul class="child-block item-block script-block">
                  <li *ngFor="let f of createDDLFileList">
                      <button (click)="viewCreateDDLFile(f)" nz-button nzType="link" nzSize="large"><i nz-icon nzType="console-sql" nzTheme="outline"></i>{{f}}</button>
                  </li>
                  <i style="color:#777777;font-size: 10px">生成时间：{{lastestGenFileTime | date : "yyyy/MM/dd HH:mm:ss"}}</i>
              </ul>
          </ng-container>
          <h3>基本信息</h3>
          <div class="item-block">
              <tis-plugins (afterSave)="afterPluginSave($event)" [errorsPageShow]="false"
                           [formControlSpan]="20" [shallInitializePluginItems]="false" [showSaveButton]="inUpdate" [disabled]="!inUpdate"
                           [plugins]="[{name: 'appSource', require: true, extraParam: pluginExtraParam}]"></tis-plugins>
          </div>
          <h3>Reader</h3>
          <div class="item-block">
              <tis-plugins (afterSave)="afterPluginSave($event)" [errorsPageShow]="false"
                           [formControlSpan]="20" [shallInitializePluginItems]="false" [showSaveButton]="false" [disabled]="true"
                           [plugins]="[{name: 'dataxReader', require: true, extraParam: pluginExtraParam}]"></tis-plugins>
              <!--
                            <datax-reader-table-select *ngIf="dto.processMeta && dto.processMeta.readerRDBMS" [execModel]="readModel" [dtoooo]="dto" [inReadonly]="true"></datax-reader-table-select>
              -->
          </div>
          <h3>Writer</h3>
          <div class="item-block">
              <tis-plugins (afterSave)="afterPluginSave($event)" [showExtensionPoint]="{open:false}" [errorsPageShow]="false"
                           [formControlSpan]="20" [shallInitializePluginItems]="false" [showSaveButton]="false" [disabled]="true"
                           [plugins]="[{name: 'dataxWriter', require: true, extraParam: pluginExtraParam}]"></tis-plugins>
          </div>
      </nz-spin>
  `
  , styles: [
      `     .script-block {
            max-height: 200px;
            overflow-y: auto
        }

        .fix-foot {
            z-index: 100;
            padding: 6px;
            background-color: #f1f1f1;
            height: 40px;
            text-align: center;
            position: fixed;
            bottom: 0px;
            width: 100%;
        }

        .child-block {
            list-style-type: none;
        }

        .child-block li {
            display: inline-block;
            width: 20%;
            padding-right: 8px;
        }

        .editable-cell {
            position: relative;
            padding: 5px 12px;
            cursor: pointer;
        }

        .editable-row:hover .editable-cell {
            border: 1px solid #d9d9d9;
            border-radius: 4px;
            padding: 4px 11px;
        }



    `
  ]
})
export class DataxAddStep7Component extends BasicDataXAddComponent implements OnInit, AfterViewInit {
  errorItem: Item = Item.create([]);
  pluginExtraParam: string;
  @Input()
  execModel: ExecModel = ExecModel.Create;
  inUpdate = false;
  genCfgFileList: Array<string> = [];
  createDDLFileList: Array<string> = [];
  lastestGenFileTime: number;

  readModel = ExecModel.Reader;

  @Input()
  set dtoooo(dto: DataxDTO) {
    this.dto = dto;
  }

  constructor(tisService: TISService, modalService: NzModalService, private drawerService: NzDrawerService, r: Router, route: ActivatedRoute, notification: NzNotificationService) {
    super(tisService, modalService, r, route, notification);
  }

  get createModel(): boolean {
    return this.execModel === ExecModel.Create;
  }

  get createDataXStep(): boolean {
    return this.stepType === StepType.CreateDatax
  }


  ngOnInit(): void {
    if (!this.dto) {
      throw new Error("dto can not be null");
    }
    this.pluginExtraParam = `update_${!this.createModel},justGetItemRelevant_true,dataxName_${this.dto.dataxPipeName}`;
    // console.log(this.pluginExtraParam);
    super.ngOnInit();
  }

  protected initialize(app: CurrentCollection): void {
    this.generate_datax_cfgs((this.execModel === ExecModel.Reader));
  }

  private generate_datax_cfgs(getExist: boolean): Promise<GenerateCfgs> {
    let url = '/coredefine/corenodemanage.ajax';
    return this.httpPost(url, 'action=datax_action&emethod=generate_datax_cfgs&dataxName='
      + this.dto.dataxPipeName + "&getExist=" + (getExist)).then((r) => {
      if (r.success) {
        let cfgs: GenerateCfgs = r.bizresult;
        this.genCfgFileList = cfgs.dataxFiles;
        this.createDDLFileList = cfgs.createDDLFiles;
        this.lastestGenFileTime = cfgs.genTime;
        return r.bizresult;
      }
    });
  }

  ngAfterViewInit(): void {
  }


  /**
   * 更新DataX配置
   */
  updateStepNext() {
    this.jsonPost("/coredefine/corenodemanage.ajax?action=datax_action&emethod=update_datax&dataxName=" + this.dto.dataxPipeName
      , this.dto.profile)
      .then((r) => {
        this.processResult(r);
        if (r.success) {
          // console.log(dto);
          // this.nextStep.emit(this.dto);
          this.r.navigate(["../config"], {relativeTo: this.route});
        }
      });
  }

  // 执行下一步
  public createStepNext(): void {
    this.jsonPost("/coredefine/corenodemanage.ajax?action=datax_action&emethod=create_datax&dataxName=" + this.dto.dataxPipeName
      , this.dto.profile)
      .then((r) => {
        this.processResult(r);
        if (r.success) {
          // console.log(dto);
          // this.nextStep.emit(this.dto);
          // this.successNotify(`DataX 实例:'${this.dto.dataxPipeName}'已创建成功`, 2000)
          setTimeout(() => {
            this.r.navigate(["/x", this.dto.dataxPipeName], {relativeTo: this.route});
          }, 1000);

          // onClose.subscribe(() => {
          //
          // })
        } else {
          this.errorItem = Item.processFieldsErr(r);
        }
      });
  }

  viewDataXCfg(fileName: string) {
    this.viewGenFile(fileName, {'formatMode': 'application/ld+json', 'titlePrefix': 'DataX Config File ', 'type': GenCfgFileType.datax});
  }

  viewCreateDDLFile(createDDLName: string) {
    this.viewGenFile(createDDLName
      , {'formatMode': 'text/x-sql', 'titlePrefix': 'Table create DDL file For Writer ', 'type': GenCfgFileType.createDDL, 'editable': true, updateMethod: "save_table_create_ddl"});
  }

  private viewGenFile(fileName: string, opt: GenCfgFileOpt) {

    this.httpPost("/coredefine/corenodemanage.ajax"
      , "action=datax_action&emethod=get_gen_cfg_file&dataxName=" + this.dto.dataxPipeName + "&fileName=" + fileName + "&fileType=" + opt.type)
      .then((r) => {
        this.processResult(r);
        if (r.success) {
          const drawerRef = this.drawerService.create<ViewGenerateCfgComponent, {}, {}>({
            // 此处宽度不能用百分比，不然内部的codemirror显示会有问题
            nzWidth: "800px",
            // nzHeight: "80%",
            nzPlacement: "right",
            nzTitle: `${opt.titlePrefix} '${fileName}' `,
            nzContent: ViewGenerateCfgComponent,
            nzWrapClassName: 'get-gen-cfg-file',
            nzContentParams: {fileMeta: Object.assign({fileName: fileName, dataxName: this.dto.dataxPipeName}, r.bizresult), formatMode: opt.formatMode, editMeta: opt}
          });
        }
      });
  }


  reGenerate() {
    this.generate_datax_cfgs(false).then((r: GenerateCfgs) => {
      // title: string, content: string, options?: NzNotificationDataOptions
      this.tisService.notification.success("成功", `最新生成${r.dataxFiles.length}个DataX配置文件`);
    });
  }

  reGenerateSqlDDL() {
    let url = '/coredefine/corenodemanage.ajax';
    return this.httpPost(url, 'action=datax_action&emethod=regenerate_sql_ddl_cfgs&dataxName=' + this.dto.dataxPipeName)
      .then((r) => {
      if (r.success) {
        let cfgs: GenerateCfgs = r.bizresult;
        this.createDDLFileList = cfgs.createDDLFiles;
        this.lastestGenFileTime = cfgs.genTime;
        this.tisService.notification.success("成功", `最新生成${cfgs.createDDLFiles.length}个Create Table DDL文件`);
      }
    });
  }

  startUpdate() {
    this.inUpdate = true;
  }

  afterPluginSave(e: PluginSaveResponse) {
    if (e.saveSuccess) {
      this.inUpdate = false;
    }
  }

  startEditReader() {
    this.startDataXEdit("reader");
  }

  private startDataXEdit(execType: "reader" | "writer") {
    let execId = BasicFormComponent.getUUID();
    if (!execId) {
      throw new Error("in valid execId");
    }
    this.httpPost("/coredefine/corenodemanage.ajax"
      , "action=datax_action&emethod=create_update_process&execId=" + execId)
      .then((r) => {
        if (r.success) {
          this.r.navigate(['../update'], {relativeTo: this.route, fragment: execType, queryParams: {"execId": r.bizresult}});
        }
      });
  }

  startEditWriter() {

    this.startDataXEdit("writer");
  }


}

enum GenCfgFileType {
  datax = 'datax',
  createDDL = 'createTableDDL'
}

interface GenCfgFileOpt {
  titlePrefix: string;
  formatMode: string;
  type: GenCfgFileType;
  editable?: true;
  updateMethod?: string;
}

class GenerateCfgs {
  dataxFiles: Array<string>;
  createDDLFiles: Array<string>;
  genTime: number;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <nz-page-header *ngIf="editMeta.editable" class="recent-using-tool" [nzGhost]="false">
          <nz-page-header-extra>
              <button nz-button [disabled]="this.formDisabled" nzType="primary" (click)="saveContent()"><i nz-icon nzType="save" nzTheme="outline"></i>保存</button>
          </nz-page-header-extra>
      </nz-page-header>
      <div class="item-block" style="height: 90%;">
          <tis-codemirror #codemirror [name]="'script'" [config]="{mode: formatMode ,lineNumbers: true}" [size]="{width:null,height:'100%'}" [(ngModel)]="fileMeta.content"></tis-codemirror>
      </div>
  `
  , styles: [`
  `]
})
export class ViewGenerateCfgComponent extends AppFormComponent implements AfterViewInit {

  @ViewChild('codemirror', {static: false}) codeMirror: CodemirrorComponent;
  @Input()
  fileMeta: { content?: string, fileName?: string } = {};

  @Input()
  formatMode: string;

  @Input()
  editMeta: EditMeta = {};

  constructor(private drawerRef: NzDrawerRef<{ hetero: HeteroList }>, tisService: TISService, route: ActivatedRoute, modalService: NzModalService, notification: NzNotificationService, private cd: ChangeDetectorRef) {
    super(tisService, route, modalService, notification);
    this.getCurrentAppCache = true;
    // this.cd.detach()
  }

  ngAfterViewInit(): void {
    // setTimeout(() => {
    //   this.codeMirror.save();
    // }, 2000);
  }


  ngOnInit(): void {
    super.ngOnInit();
    //  this.cd.detectChanges();
  }

  protected initialize(app: CurrentCollection): void {

  }

  close(): void {
    this.drawerRef.close();
  }

  saveContent() {
    let post = this.fileMeta;
    if (!this.editMeta.editable) {
      throw new Error("must be editable");
    }
    this.jsonPost("/coredefine/corenodemanage.ajax?event_submit_do_" + this.editMeta.updateMethod + "=y&action=datax_action"
      , post).then((result) => {
      if (result.success) {
        this.drawerRef.close();
      }
    });
  }
}


interface ITableAlias {
  from: string;
  to: string;
}

interface EditMeta {
  editable?: boolean;
  updateMethod?: string;
}
