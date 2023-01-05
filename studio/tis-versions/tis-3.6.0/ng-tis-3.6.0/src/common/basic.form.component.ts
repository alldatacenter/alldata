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

import {TISService} from './tis.service';
import {ActivatedRoute, Params} from '@angular/router';
import {Component, EventEmitter, Injectable, Input, OnInit, Output, Type} from '@angular/core';
// import JQuery from 'jquery';
// @ts-ignore
import * as NProgress from 'nprogress/nprogress.js';
import 'nprogress/nprogress.css';
import {ModalOptions, NzModalRef, NzModalService} from "ng-zorro-antd/modal";

import {NzNotificationService, NzNotificationRef} from "ng-zorro-antd/notification";
import {SavePluginEvent, TisResponseResult} from "./tis.plugin";
import {Subject} from "rxjs";
import {map} from "rxjs/operators";
import {LogType} from "../runtime/misc/RCDeployment";

import {AppType} from "./application";
import {NzDrawerRef} from "ng-zorro-antd/drawer";

/**
 * Created by baisui on 2017/4/12 0012.
 */
declare var jQuery: any;
const KEY_show_Bread_crumb = "showBreadcrumb";

// declare var NProgress: any;
export class BasicFormComponent {
  result: TisResponseResult;
  // 表单是否禁用

  public formDisabled = false;

  // 取得随机ID
  public static getUUID(): string {
    function s4() {
      return Math.floor((1 + Math.random()) * 0x10000)
        .toString(16)
        .substring(1);
    }

    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
      s4() + '-' + s4() + s4() + s4();
  }

  // // 当前上下文中使用的索引
  // currIndex: CurrentCollection;
  public showBreadCrumb(route: ActivatedRoute): boolean {
    return !!route.snapshot.data[KEY_show_Bread_crumb];
  }

  constructor(protected tisService: TISService, protected modalService?: NzModalService, protected notification?: NzNotificationService) {
  }

  protected confirm(content: string, onOK: () => void): void {
    if (!this.modalService) {
      throw new Error(" have not inject prop 'modalService' ");
    }
    this.modalService.confirm({
      nzTitle: '确认',
      nzContent: content,
      nzOkText: '执行',
      nzCancelText: '取消',
      nzOnOk: onOK
    });
  }

  protected successNotify(msg: string, duration?: number): NzNotificationRef {
    return this.notification.success('成功', msg, {nzDuration: duration > 0 ? duration : 6000});
  }

  protected errNotify(msg: string, duration?: number) {
    this.notification.error('错误', msg, {nzDuration: duration > 0 ? duration : 6000});
  }

  protected infoNotify(msg: string, duration?: number) {
    this.notification.info('信息', msg, {nzDuration: duration > 0 ? duration : 6000});
  }

  private webExecuteCallback = (r: TisResponseResult): TisResponseResult => {
    this.formDisabled = false;
    // console.log("webExecuteCallback")
    NProgress.done();
    return r;
  }

  get appNotAware(): boolean {
    return !this.tisService.currentApp;
  }

  protected clearProcessResult(): void {
    this.result = {success: true, msg: [], errormsg: []};
  }

  public processResult(result: TisResponseResult, callback?: () => void): void {
    return this.processResultWithTimeout(result, 10000, callback);
  }

  // 显示执行结果
  protected processResultWithTimeout(result: TisResponseResult, timeout: number, callback?: () => void): void {
    this.result = result;
    // console.log(result);
    if (timeout > 0) {
      setTimeout(() => {
        this.clearProcessResult();
        if (callback) {
          callback();
        }
      }, timeout);
    }
  }

  protected submitForm(url: string, form: any): void {
    this.formDisabled = true;
    NProgress.start();
    this.clearProcessResult();
    this.tisService.httpPost(url
      , jQuery(form).serialize()).then(result => {
      this.processResult(result);
      this.formDisabled = false;
    });
  }

  public openDialog(component: any, options: ModalOptions<any>): NzModalRef<any> {

    let option: ModalOptions = {
      // nzTitle: title,
      nzWidth: "800px",
      nzContent: component,
      nzFooter: null,
      nzMaskClosable: false
    };
    return this.modalService.create(Object.assign(option, options));
  }

  get currentApp(): CurrentCollection {
    return this.tisService.currentApp;
  }

  // 发送http post请求
  public httpPost(url: string, body: string): Promise<TisResponseResult> {
    this.formDisabled = true;
    NProgress.start();
    this.clearProcessResult();
    return this.tisService.httpPost(url, body).then(this.webExecuteCallback).catch(this.handleError);
  }

  // 发送json表单
  public jsonPost(url: string, body: any, e?: SavePluginEvent): Promise<TisResponseResult> {
    this.formDisabled = true;
    NProgress.start();
    this.clearProcessResult();
    return this.tisService.jsonPost(url, body, e).then(this.webExecuteCallback).catch(this.handleError);
  }

// = (r: TisResponseResult): TisResponseResult => {
  protected handleError = (error: any): Promise<any> => {
    // console.log(error);
    // console.log(this);
    this.formDisabled = false;
    NProgress.done();
    return Promise.reject(error.message || error);
  }

  protected jsonp(url: string): Promise<TisResponseResult> {
    this.formDisabled = true;
    NProgress.start();
    return this.tisService.jsonp(url).then(this.webExecuteCallback).catch(this.handleError);
  }

  public jPost(url: string, o: any): Promise<TisResponseResult> {
    this.formDisabled = true;
    NProgress.start();
    this.clearProcessResult();
    return this.tisService.jPost(url, o).then(this.webExecuteCallback).catch(this.handleError);
  }

}


// 可选项
export class Option {
  constructor(public value: string, public label: string) {
  }
}

export class NodeMeta {


  constructor(public type: string, private img: string
    , private size: number[], public label: string, public compRef: Type<any>) {
  }

  get width(): number {
    return this.size[0];
  }

  get height(): number {
    return this.size[1];
  }

  get imgPath(): string {
    return '/images/icon/' + this.img;
  }
}


export abstract class BasicSidebarDTO {
  protected constructor(public nodeMeta: NodeMeta) {
  }
}


@Component({
  selector: 'sidebar-toolbar',
  styles: [
      ` .sidebar {
          border-bottom: thin solid #999999;
          padding: 0 0 5px 0;
          margin: 0 0 18px 0;
          height: 40px;
      }

      .float-right {
          float: right;
      }
    `
  ],
  template: `
      <div class="sidebar">
          <button *ngIf="!deleteDisabled" nz-button nzType="primary" nzDanger (click)="_deleteNode()">删除</button>
          <div [ngClass]="{'float-right': true}">
              <button *ngIf="!saveDisabled" nz-button nzType="primary" (click)="_saveClick()">保存</button>&nbsp;
              <button nz-button nzType="default" (click)="_closeSidebar($event)">关闭</button>
          </div>
      </div>
      <div style="clear: both"></div>
  `
})
export class SideBarToolBar extends BasicFormComponent {
  @Output() save = new EventEmitter<any>();
  @Input() deleteDisabled = false;
  @Input() saveDisabled = false;
  @Output() delete = new EventEmitter<any>();
  @Output() close = new EventEmitter<any>();

  constructor(tisService: TISService, ngModalService: NzModalService) {
    super(tisService, ngModalService);
  }

  _deleteNode() {
    this.modalService.confirm({
      nzTitle: '<i>请确认是否要删除该节点?</i>',
      nzContent: '<b>删除之后不可恢复</b>',
      nzOnOk: () => {
        this.delete.emit();
      }
    });
  }


  _saveClick() {
    this.save.emit();
  }

  _closeSidebar(event: MouseEvent) {
    this.close.emit(event);
  }

}

@Injectable()
export abstract class BasicSideBar extends BasicFormComponent {
  @Output() saveClick = new EventEmitter<any>();
  // @Output() onClose = new EventEmitter<any>();
  @Input() nodeMeta: NodeMeta;
  @Input() g6Graph: any;
  @Input() parentComponent: IDataFlowMainComponent;

  protected constructor(tisService: TISService, modalService: NzModalService, private drawerRef: NzDrawerRef<BasicSideBar>, notification?: NzNotificationService) {
    super(tisService, modalService, notification);
  }

  _saveClick(): void {
    this.saveClick.emit();
  }

  _closeSidebar(event: MouseEvent): void {
    // this.onClose.emit();
    this.drawerRef.close();
    if (event) {
      event.stopPropagation();
    }
  }

  public abstract initComponent(addComponent: IDataFlowMainComponent, selectNode: BasicSidebarDTO): void;

  public abstract subscribeSaveClick(graph: any, $: any, nodeid: string, addComponent: IDataFlowMainComponent, evt: any): void;
}

export interface IDataFlowMainComponent {
  readonly dumpTabs: Map<string, DumpTable>;
  readonly joinNodeMap: Map<string /*id*/, JoinNode>;

  closePanel(): void;

  getUid(): string;
}

export class WSMessage {
  constructor(public logtype: string, public data?: any) {

  }
}

@Injectable()
export abstract class AppFormComponent extends BasicFormComponent implements OnInit {
  private _getCurrentAppCache = false;

  protected constructor(tisService: TISService, protected route: ActivatedRoute, modalService: NzModalService, notification?: NzNotificationService) {
    super(tisService, modalService, notification);
  }

  // @Input()
  public set getCurrentAppCache(val: boolean) {
    this._getCurrentAppCache = val;
  }

  ngOnInit(): void {
    let queryParams = this.route.snapshot.queryParams;
    let execId = queryParams['execId'];
    this.tisService.execId = execId;
    this.route.params
      .subscribe((params: Params) => {
        // console.log(params['name'] + ",getCurrentAppCache:" + this._getCurrentAppCache);
        // if (this.tisService instanceof AppTISService) {
        let appTisService: TISService = this.tisService;
        if (!this._getCurrentAppCache) {
          let collectionName = params['name'];
          // console.log(collectionName);
          if (!collectionName) {
            appTisService.currentApp = null;
          }
          if (!appTisService.currentApp && collectionName) {
            appTisService.currentApp = new CurrentCollection(0, collectionName);
            // console.log(this.currentApp);
          } else {
            // appTisService.currentApp = null;
          }
        }
        // console.log(appTisService.currentApp);
        this.initialize(appTisService.currentApp);
      });
  }

  protected abstract initialize(app: CurrentCollection): void ;

  protected getWSMsgSubject(logtype: string): Subject<WSMessage> {
    let app = this.currentApp;
    return <Subject<WSMessage>>this.tisService.wsconnect(`ws://${window.location.host}/tjs/download/logfeedback?logtype=${logtype}&collection=${app ? app.name : ''}`)
      .pipe(map((response: MessageEvent) => {
        let json = JSON.parse(response.data);
        // console.log(json);
        if (json.logType && json.logType === "MQ_TAGS_STATUS") {
          return new WSMessage('mq_tags_status', json);
        } else if (json.logType && json.logType === "INCR") {
          return new WSMessage('incr', json);
        } else if (json.logType && json.logType === "INCR_DEPLOY_STATUS_CHANGE") {
          return new WSMessage(LogType.INCR_DEPLOY_STATUS_CHANGE, json);
        } else if (json.logType && json.logType === "DATAX_WORKER_POD_LOG") {
          return new WSMessage(LogType.DATAX_WORKER_POD_LOG, json);
        }
        return null;
      }));
  }
}

export class CurrentCollection {
  constructor(private id: number, public name: string, public appTyp?: AppType) {
  }

  public get appid() {
    return this.id;
  }

  public get appName() {
    return this.name;
  }
}


// sidebar 在与主页面传递的dto对象
export class DumpTable extends BasicSidebarDTO {
  constructor(nodeMeta: NodeMeta, public nodeid: string, public sqlcontent?: string, public dbid?: string, public tabid?: string, public tabname?: string) {
    super(nodeMeta);
  }

  public get cascaderTabId(): string {
    return this.tabid + '%' + this.tabname;
  }
}

/**
 * 记录表是否是索引主表，是否需要监听增量更新信息
 */
export class ERMetaNode extends BasicSidebarDTO {

  public columnTransferList: ColumnTransfer[] = [];
  // 主索引表
  public primaryIndexTab = false;
  // 当primaryIndexTab为true时，primaryIndexColumnName不能为空
  public primaryIndexColumnNames: PrimaryIndexColumnName[] = [new PrimaryIndexColumnName(null, false)];

  // 监听增量变更
  public monitorTrigger = true;
  public sharedKey: string;
  public timeVerColName: string;

  constructor(public dumpnode: DumpTable, public topologyName: string) {
    super(null);
  }
}

export class PrimaryIndexColumnName {
  public delete = false;

  constructor(public name: string, public pk: boolean) {
  }
}

export class ColumnTransfer {
  public checked = false;

  constructor(public colKey: string, public transfer: string, public param: string) {
  }
}

export class ERRuleNode extends BasicSidebarDTO {
  public cardinality: string;
  linkKeyList: LinkKey[] = [];

  constructor(public  rel: { id: string, 'sourceNode': DumpTable, 'targetNode': DumpTable, 'linkrule': { linkKeyList: LinkKey[], cardinality: string } }, public topologyName: string) {
    super(null);
    this.cardinality = rel.linkrule.cardinality;
    this.linkKeyList = rel.linkrule.linkKeyList;
  }

  public get sourceNode(): DumpTable {
    return this.rel.sourceNode;
  }

  public get targetNode(): DumpTable {
    return this.rel.targetNode;
  }
}


export class JoinNode extends BasicSidebarDTO {
  public dependencies: Option[] = [];
  public edgeIds: string[] = [];

  constructor(nodeMeta: NodeMeta, public id?: string, public exportName?: string, public position?: Pos, public sql?: string) {
    super(nodeMeta);
  }

  public addDependency(o: Option): void {
    this.dependencies.push(o);
  }

  public addEdgeId(id: string): void {
    this.edgeIds.push(id);
  }
}


export class NodeMetaConfig {
  constructor(public exportName: string, public id: string, public type: string, public position: Pos
    , public sql: string, public dependencies: NodeMetaDependency[]) {
  }
}


export class Pos {
  constructor(public x: number, public y: number) {

  }
}

export class NodeMetaDependency {
  constructor(public id: string, public tabid: string, public dbid: string
    , public dbName: string, public name: string, public extraSql: string
    , public position?: Pos, public type?: string) {

  }
}

export class LinkKey {
  public checked = false;

  constructor(public parentKey: string, public childKey: string) {
  }
}


