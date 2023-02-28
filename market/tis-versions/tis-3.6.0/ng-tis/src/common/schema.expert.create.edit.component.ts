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

import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {BasicEditComponent} from '../corecfg/basic.edit.component';
import {TISService} from './tis.service';

import {EnginType, FieldErrorInfo, SchemaField, SchemaFieldType, SchemaFieldTypeTokensType, StupidModal} from "../base/addapp-pojo";
import {ActivatedRoute} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {TisResponseResult} from "./tis.plugin";


// 创建索引流程切换到专家模式框
@Component({
  selector: 'expert-schema-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <tis-page-header [showBreadcrumb]="false">
          <button *ngIf="showSaveControl" nz-button nzType="primary"
                  name="event_submit_do_save_content" (click)="doSaveContent()">保 存
          </button>
      </tis-page-header>
      <form id="contentform" method="post">
          <div id="res_param"></div>
          <textarea name="memo" id="memo" style="display:none;" cols="16"></textarea>
          <input type="hidden" name="event_submit_{{actionExecute}}" value="y"/>
          <input type="hidden" name="action" value="{{actionName}}"/>
          <input type="hidden" *ngFor="let p of editFormAppendParam" name="{{p.name}}" value="{{p.val}}"/>
          <input type="hidden" name="snapshot" [(ngModel)]="snid"/>
          <input type="hidden" name="filename" value="schema"/>
          <div id="tis-schema-codemirror">
              <tis-codemirror name="schemaContent" [size]="{width:'100%',height:'95%'}" [(ngModel)]="_schemaXmlContent" [config]="codeMirrirOpts"></tis-codemirror>
          </div>
      </form>
  `,
  styles: [
      `
          #tis-schema-codemirror {
              height: 800px;
          }
    `
    ,

  ]
})
export class SchemaExpertAppCreateEditComponent extends BasicEditComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() engineType: { type: EnginType, payload?: any } = {type: EnginType.Solr};
  @Output() saveSuccess = new EventEmitter<any>();
  @Output() initComplete = new EventEmitter<{ success: boolean, bizresult: any }>();
  formAppendParam: { name: string, val: string }[] = [];

  _schemaXmlContent: string;

  constructor(tisService: TISService, modalService: NzModalService, route: ActivatedRoute) {
    super(tisService, modalService, route);
  }

  ngOnDestroy(): void {
  }

  public get schemaXmlContent(): string {
    return this._schemaXmlContent;
  }

  // 设置Component内容
  @Input()
  public set schemaXmlContent(val: string) {
    if (!val) {
      return;
    }
    this._schemaXmlContent = val;
  }

  ngOnInit(): void {
    this.showSaveControl = false;
  }

  protected afterCodeSet(cfgContent: string): void {
    // this.initComplete.emit({success: true, bizresult: {cfgContent: cfgContent}});
  }

  public get editFormAppendParam(): { name: string, val: string }[] {
    return this.formAppendParam;
  }

  // 此方法会在父组件中调用
  public doSaveContent(): Promise<{ success: boolean, stupid: StupidModal }> {
    // super.doSaveContent(this.editform.nativeElement);
    let postBody: any = {content: this._schemaXmlContent};

    switch (this.engineType.type) {
      case EnginType.Solr:
        break;
      case EnginType.ES: {
        postBody.dataxName = this.engineType.payload;
        break;
      }
    }
    return this.jsonPost(`/runtime/schema.ajax?action=schema_action&emethod=toggle_${this.engineType.type}_stupid_model`
      , postBody)
      .then((r: TisResponseResult) => {
        return {success: r.success, stupid: StupidModal.deseriablize(r.bizresult)};
      });

  }

  public get actionExecute(): string {
    return 'do_toggle_stupid_model';
  }

  public get actionName(): string {
    return 'schema_action';
  }


  ngAfterViewInit(): void {
    // console.info(this.editform);
    // console.info(this.code);
  }


  protected getResType(): string {
    return 'schema.xml';
  }

  // 成功保存文本内容之后
  protected afterSaveContent(result: any): void {
    this.saveSuccess.emit(result);
  }
}


@Component({
  selector: 'visualizing-schema-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <nz-table #fieldlist
                [nzScroll]="scrollConfig"
                [nzData]="fields" [nzShowPagination]="false" [nzFrontPagination]="false" [nzSize]="'small'">
          <thead>
          <tr>
              <th nzWidth="80px">#</th>
              <th nzWidth="60px"></th>
              <th>
                  <span nz-tooltip nzTooltipTitle="对应数据源中的列名" nzTooltipPlacement="top">字段名称</span>
              </th>
              <th nzWidth="400px"><span data-placement="top"
                                        data-toggle="tooltip">字段类型</span></th>
              <th>可查询</th>
              <th>存储</th>
              <th><span>可排序</span>
              </th>
              <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <ng-container *ngFor="let f of fieldlist.data let i = index">
              <tr [class.editor-up]="f.editorOpen" class="editable-row">
                  <td nzShowExpand (nzExpandChange)="editorOpenClick(f)">
                      <span>{{f.index}}</span>
                      <!--
                      <button nz-button nzType="link" (click)="editorOpenClick(f)">
                          <i [class.fa-angle-double-down]="!f.editorOpen" [class.fa-angle-double-up]="f.editorOpen" class="fa"
                             aria-hidden="true"></i></button>
                    -->
                  </td>
                  <td>
                      <i *ngIf="f.uniqueKey" class="fa fa-key" aria-hidden="true"></i>
                      <i *ngIf="f.sharedKey" class="fa fa-share-alt" aria-hidden="true"></i>
                  </td>
                  <td [class.has-danger]="f.errInfo.fieldNameError">
                      <div>
                          <div class="editable-cell" *ngIf="this.editField !== f && f.name !== null && f.name !== undefined; else editTpl">
                              <div class="editable-cell-value-wrap" (click)="startEdit(f, $event)">
                                  {{f.name}}
                              </div>
                          </div>
                          <ng-template #editTpl>
                              <input nz-input
                                     placeholder="请输入字段名称" (focus)="fieldEditGetFocus(f)" [(ngModel)]="f.name" name="{{'name'+f.id}}"/>
                          </ng-template>
                      </div>
                  </td>
                  <td [class.has-danger]="f.errInfo.fieldTypeError">
                      <nz-select class="type-select" [(ngModel)]="f.fieldtype" nzPlaceHolder="请选择" [nzDropdownMatchSelectWidth]="true" (ngModelChange)="fieldTypeChange(f)">
                          <nz-option [nzValue]="pp.name" [nzLabel]="pp.name" *ngFor="let pp of ftypes"></nz-option>
                      </nz-select>
                      <nz-select *ngIf="f.split" class="type-select" [(ngModel)]="f.tokenizerType" nzAllowClear nzPlaceHolder="选择" [nzDropdownMatchSelectWidth]="true" (ngModelChange)="fieldTypeChange(f)">
                          <nz-option [nzValue]="t.key" [nzLabel]="t.value" *ngFor="let t of getAnalyzer(f.fieldtype)"></nz-option>
                      </nz-select>
                  </td>
                  <td [class.has-danger-left]="f.errInfo.fieldPropRequiredError">
                      <label nz-checkbox [(ngModel)]="f.indexed" [ngModelOptions]="{standalone: true}"></label>
                  </td>
                  <td [class.has-danger-center]="f.errInfo.fieldPropRequiredError">
                      <label nz-checkbox [(ngModel)]="f.stored" [ngModelOptions]="{standalone: true}"></label>
                  </td>
                  <td [class.has-danger-right]="f.errInfo.fieldPropRequiredError">
                      <label nz-checkbox [(ngModel)]="f.docval" [ngModelOptions]="{standalone: true}"></label>
                  </td>
                  <td>
                      <button nz-button nzType="link" (click)="deleteColumn(f)">
                          <i class="fa fa-trash-o"
                             aria-hidden="true"></i></button>
                  </td>
              </tr>
              <tr [nzExpand]="f.editorOpen">
                  <td colspan="7">
                      <!--
                      <button nz-button nzType="link" (click)="f.editorOpen=false"><i nz-icon nzType="close-circle" nzTheme="outline"></i></button>
                      -->
                      &nbsp;&nbsp;&nbsp;&nbsp;
                      <nz-button-group>
                          <button nz-button nzSize="small" (click)="columnMove(i,false)">
                              <i class="fa fa-arrow-up" aria-hidden="true"></i>
                          </button>
                          <button nz-button nzSize="small" (click)="columnMove(i,true)">
                              <i class="fa fa-arrow-down" aria-hidden="true"></i>
                          </button>
                          <button nz-button nzSize="small" (click)="downAddColumn(i,f)">
                              添加
                          </button>
                      </nz-button-group>
                      &nbsp;
                      <label nz-checkbox [(ngModel)]="f.uniqueKey" (ngModelChange)="pkSelectChange($event,f)">设置为主键</label>
                      <label nz-checkbox [(ngModel)]="f.sharedKey" (ngModelChange)="sharedKeySelectChange($event,f)">设置为分组键</label>
                      <label nz-checkbox [(ngModel)]="f.multiValue">多值</label>

                  </td>
              </tr>
          </ng-container>
          </tbody>
      </nz-table>
  `,
  styles: [
      `
          .has-danger {
              border: 2px solid #ff6f70;
              border-radius: 3px;
          }

          .has-danger-left {
              border-left: 2px solid #ff6f70;
              border-top: 2px solid #ff6f70;
              border-bottom: 2px solid #ff6f70;
              border-radius: 3px;
          }

          .has-danger-right {
              border-right: 2px solid #ff6f70;
              border-top: 2px solid #ff6f70;
              border-bottom: 2px solid #ff6f70;
              border-radius: 3px;
          }

          .has-danger-center {
              border-top: 2px solid #ff6f70;
              border-bottom: 2px solid #ff6f70;
              border-radius: 3px;
          }

          .type-select {
              width: 40%;
              margin-right: 4px;
          }

          .editable-cell {
              position: relative;
          }

          .editable-cell-value-wrap {
              padding: 5px 12px;
              cursor: pointer;
          }

          .editable-row:hover .editable-cell-value-wrap {
              border: 1px solid #d9d9d9;
              border-radius: 4px;
              padding: 4px 11px;
          }
    `
  ]
})
export class SchemaVisualizingEditComponent extends BasicEditComponent implements OnInit, AfterViewInit {

  @Input() engineType: { type: EnginType, payload?: string } = {type: EnginType.Solr};
  widthConfig = ['50px', '250px', '200px', '100px', '100px', '100px', '200px'];
  scrollConfig = {x: '1150px', y: '700px'};
  private fieldtypesArray: Array<SchemaFieldType>;
  private fieldtypes: Map<string, SchemaFieldType> = new Map();
  fields: SchemaField[];
  uniqueKey: string;
  shareKey: string;
  schemaXmlContent: string;

  editField: SchemaField | null;

  constructor(tisService: TISService, modalService: NzModalService, route: ActivatedRoute, private cd: ChangeDetectorRef) {
    super(tisService, modalService, route);
  }

  startEdit(f: SchemaField, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.editField = f;
  }

  /**
   * 将可视化内容同步到SchemaXml上
   */
  public saveAndMergeXml(): Promise<{ success: boolean, schema: string }> {
    let model = this.stupidModel;
    if (this.engineType.payload) {
      model.dataxName = this.engineType.payload;
    }

    return this.jsonPost(`/runtime/schema.ajax?action=schema_action&emethod=toggle_${this.engineType.type}_expert_model`
      , model).then(
      (r: { success: boolean, bizresult: { inputDisabled: string[], schema: string } }) => {
        // console.log(r.success);
        // 处理执行结果
        if (!r.success) {
          let biz: any = r.bizresult;
          let errs: Array<FieldErrorInfo> = biz;
          errs.forEach((err) => {
            model.markFieldErr(err);
          });
          this.fields = [...model.fields];
          this.cd.detectChanges();
        }
        //  this.initComplete.emit(r);
        return {success: r.success, schema: r.bizresult.schema};
      });
  }

  get stupidModel(): StupidModal {
    let p = new StupidModal();
    p.fields = this.fields;
    p.fieldtypes = this.fieldtypesArray;
    p.uniqueKey = this.uniqueKey;
    p.shareKey = this.shareKey;
    p.schemaXmlContent = this.schemaXmlContent;
    return p;
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
    // console.log("ngAfterViewInit");
  }

  protected afterSaveContent(bizresult: any): void {
  }

  protected getResType(): string {
    return 'schema.xml';
  }

  @Input()
  set bizResult(bizresult: StupidModal) {
    if (!bizresult) {
      return;
    }
    bizresult.fieldtypes.forEach((i) => {
      this.fieldtypes.set(i.name, i);
    });
    this.fieldtypesArray = bizresult.fieldtypes;
    this.fields = bizresult.fields;
    this.uniqueKey = bizresult.uniqueKey;
    this.shareKey = bizresult.shareKey;
    this.schemaXmlContent = bizresult.schemaXmlContent;
  }

  // 分区建checkbox点击
  public sharedKeySelectChange(e: any, f: SchemaField): void {
    if (f.sharedKey) {
      this.fields.forEach((t) => {
        t.sharedKey = false;
      });
      f.sharedKey = true;
    }
    // f.sharedKey = e.target.checked;
  }

  public editorOpenClick(f: SchemaField): void {

    // if (this.formDisabled) {
    //   return;
    // }

    if (!f.editorOpen) {
      this.fields.forEach((t) => {
        t.editorOpen = false;
      });
    }
    f.editorOpen = !f.editorOpen;

  }

// 主键选择checkbox change事件
  public pkSelectChange(e: any, f: SchemaField): void {
    if (f.uniqueKey) {
      this.fields.forEach((t) => {
        t.uniqueKey = false;
      });
      f.uniqueKey = true;
    }
    // 设置为主键之后，不需要分词，和trie分词
    // f.split = false;
    f.tokenizerType = 'string';
    // f.rangequery = false;
    f.indexed = true;
    f.stored = true;
  }

  // 是否支持区间查询
  public rangeChange(input: any, f: { range: boolean }) {
    f.range = input.checked;
  }

  // 是否存储，是否docvalue选择
  // public storedCheckChange(input: any, f: { stored: boolean }) {
  //   f.stored = input.checked;
  // }
  //
  // public docvalCheckChange(input: any, f: { docval: boolean }) {
  //   f.docval = input.checked;
  // }
  //
  // public indexedCheckChange(input: any, f: { indexed: boolean }) {
  //   f.indexed = input.checked;
  // }

  public get ftypes(): any[] {
    return this.fieldtypesArray;
  }

  public getAnalyzer(fieldtype: string): Array<SchemaFieldTypeTokensType> {
    return this.getSchemaFieldType(fieldtype).tokensType;
  }


  // 交换两列的上下位置关系
  public columnMove(colItemIndex: number, downforward: boolean): void {
    // const colItemIndex = this.fields.indexOf(f);
    const length = this.fields.length;
    let tmp: SchemaField;
    let tmpIndex;

    let offset = downforward ? 1 : -1;

    if ((downforward && colItemIndex + offset < length) || (!downforward && colItemIndex + offset >= 0)) {
      tmp = this.fields[colItemIndex];
      this.fields[colItemIndex] = this.fields[colItemIndex + offset];
      this.fields[colItemIndex + offset] = tmp;
      tmpIndex = this.fields[colItemIndex].index;
      this.fields[colItemIndex].index = this.fields[colItemIndex + offset].index;
      this.fields[colItemIndex + offset].index = tmpIndex;
    }
    this.fields = [...this.fields];
  }

  // 分区建checkbox点击
  // public multiValSelectChange(e: any, f: any): void {
  //   f.multiValue = e.target.checked;
  // }

  // 删除一个列,从array中删除一个item
  public deleteColumn(f: any): void {

    if (this.formDisabled) {
      // 当表单不可用时，直接退出
      return;
    }

    // const indexOf = 0;
    let removeFind = false;
    for (let i = 0; i < this.fields.length; i++) {
      if (!removeFind && f.name !== this.fields[i].name) {
        continue;
      }
      removeFind = true;
      if (i + 1 < this.fields.length) {
        this.fields[i] = this.fields[i + 1];
        this.fields[i].index--;
      }
    }
    if (removeFind) {
      this.fields.pop();
    }

  }

  // 向下添加新列
  public downAddColumn(index: number, f: SchemaField) {

    const flength = this.fields.length;

    let field = new SchemaField();
    field.name = null;
    field.id = flength;
    field.index = f.index + 1;
    field.inputDisabled = false;
    // field.fieldtype = 'string';
    field.tokenizerType = '-1';
    field.split = false;
    this.fields.splice(index + 1, 0, field);
    for (let sIndex = index + 2; sIndex < this.fields.length; sIndex++) {
      this.fields[sIndex].index++;
    }
    this.fields = [...this.fields];

    // console.log(index + "," + f + ",flength" + flength + ",lastlength:" + this.fields.length);
    // let insertIndex = 0;
    //
    // for (let i = 0; i < flength; i++) {
    //   if (f.id > this.fields[i].id && f.id >= this.fields[i + 1].id) {
    //     continue;
    //   } else {
    //     insertIndex = i;
    //     break;
    //   }
    // }
    // insertIndex++;
    // this.fields.push(new SchemaField());
    // for (let i = flength; i >= 0; i--) {
    //   if (insertIndex === i) {
    //     let field = new SchemaField();
    //     field.id = flength;
    //     field.index = i + 1;
    //     field.inputDisabled = false;
    //     field.fieldtype = 'string';
    //     field.tokenizerType = '-1';
    //     field.split = true;
    //     this.fields[i] = field;
    //     // {
    //     //   id: flength, index: i + 1, inputDisabled: false
    //     //   , fieldtype: 'string', tokenizerType: '-1', split: true
    //     // };
    //     break;
    //   } else {
    //     this.fields[i] = this.fields[i - 1];
    //     this.fields[i].index = i + 1;
    //   }
    // }
  }

  fieldEditGetFocus(f: SchemaField) {
    this.editField = f;
  }

  // 用户点击切换切换字段类型
  public fieldTypeChange(f: SchemaField): void {


    const type: SchemaFieldType = this.getSchemaFieldType(f.fieldtype);
    f.split = type.split;

    // this.fields.forEach((i) => {
    //   i.nodeName = 'dddd';
    // });
  }

  private getSchemaFieldType(ft: string): SchemaFieldType {
    const type: SchemaFieldType = this.fieldtypes.get(ft);
    // console.log(this.fieldtypes);
    // f.rangequery = type.rangeAware;
    // f.split = type.split;
    // f.rangequery = type.rangeAware;
    // f.range = false;
    if (!type) {
      throw new Error(`fieldType:${ft} can not get relevant type in local fieldTypes`);
    }
    return type;
  }

}
