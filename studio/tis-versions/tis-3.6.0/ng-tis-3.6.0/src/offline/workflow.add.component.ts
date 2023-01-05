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

import {AfterContentInit, AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, ComponentFactoryResolver, ComponentRef, ElementRef, OnInit, Type, ViewChild} from '@angular/core';

import {BasicFormComponent, BasicSideBar, BasicSidebarDTO, DumpTable, ERMetaNode, ERRuleNode, IDataFlowMainComponent, JoinNode, LinkKey, NodeMeta, NodeMetaConfig, NodeMetaDependency, Option} from '../common/basic.form.component';

import {TISService} from '../common/tis.service';

import {BasicWFComponent, Dataflow, WorkflowComponent} from './workflow.component';
//  @ts-ignore
import {Draggable} from '@shopify/draggable';
import * as G6 from '@antv/g6';
// @ts-ignore
// import {Grid} from '@antv/plugins';
import {Grid} from '@antv/g6/build/plugins.js';
// @ts-ignore
import * as dagre from 'dagre';
// import * as graphlib from 'graphlib';
// @ts-ignore
import * as $ from 'jquery';
import {AddAppFlowDirective} from "../base/addapp.directive";
import {WorkflowAddDbtableSetterComponent} from "./workflow.add.dbtable.setter.component";
import {WorkflowAddJoinComponent} from "./workflow.add.join.component";
import {WorkflowAddNestComponent} from "./workflow.add.nest.component";
import {WorkflowAddUnionComponent} from "./workflow.add.union.component";
import {ActivatedRoute, Router} from "@angular/router";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";

import {NzNotificationService} from 'ng-zorro-antd/notification';
import {WorkflowAddErCardinalityComponent} from "./workflow.add.er.cardinality.component";
import {WorkflowERComponent} from "./workflow.er.component";
import {WorkflowAddErMetaComponent} from "./workflow.add.er.meta.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {NzDrawerService} from "ng-zorro-antd/drawer";
import {TerminalComponent} from "../common/terminal.component";
// import {} from 'ng-sidebar';
// import {Droppable} from '@shopify/draggable';
// @ts-ignore
// import { Graph } from '@antv/g6';


export const TYPE_DUMP_TABLE = 'table';
// console.log(Draggable)
// console.log(G6);

//
@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  template: `

      <!--      <nz-drawer-->
      <!--              [nzBodyStyle]="{ height: 'calc(100% - 20px)', overflow: 'auto', 'padding-bottom': '20px' }"-->
      <!--              [nzMaskClosable]="false"-->
      <!--              [nzClosable]="false"-->
      <!--              [nzWidth]="900"-->
      <!--              [nzVisible]="_opened"-->
      <!--              (nzOnClose)="_toggleSidebar()"-->
      <!--      >-->
      <!--          <ng-container *nzDrawerContent>-->
      <!--              <ng-h tis-index-add-flow></ng-h>-->
      <!--          </ng-container>-->
      <!--      </nz-drawer>-->

      <tis-page-header [breadcrumb]="['数据流','/offline/wf']" [title]="pageTitle" [result]="result"></tis-page-header>
      <div>
          <div id="processon_designer" class="processon-designer">
              <nz-spin [nzSpinning]="formDisabled" nzSize="large">
                  <nz-tabset [nzTabBarExtraContent]="extraTemplate" [(nzSelectedIndex)]="tabSelectedIndex">
                      <nz-tab nzTitle="DF">
                          <div class="designer_body clear">
                              <div class="body_vtoolsbar">
                                  <ul #draggableblock class="processon-widget list-unstyled">
                                      <li *ngFor="let n of _nodeTypesAry">
                                          <img [width]="n.width" [height]="n.height" [src]="n.imgPath" [attr.type]="n.type"/>
                                          <label>{{n.label}}</label>
                                      </li>
                                  </ul>
                              </div>
                              <div class="body_container">
                                  <div id="body_layout" class="processon-booth">
                                      <div id="canvas_container"></div>
                                  </div>
                              </div>
                          </div>
                      </nz-tab>
                      <nz-tab *ngIf="!!topologyName" nzTitle="ER">
                          <ng-template nz-tab>
                              <offline-er #erComponent (edgeClick)="erNodesEdgeClick($event)" [topologyName]="topologyName"
                                          [_nodeTypes]="_nodeTypes" (nodeClick)="erNodesNodeClick($event)"></offline-er>
                          </ng-template>
                      </nz-tab>
                  </nz-tabset>
                  <ng-template #extraTemplate>

                      <button *ngIf="this.workflow" nz-button nz-dropdown [nzDropdownMenu]="menu"
                              nzPlacement="bottomLeft">操作 <i nz-icon nzType="down"></i></button>
                      <nz-dropdown-menu #menu="nzDropdownMenu">
                          <ul nz-menu>
                              <li nz-menu-item (click)="executeWorkflow(this.workflow)"><i nz-icon nzType="play-circle" nzTheme="outline"></i>构建</li>
                              <li nz-menu-item (click)="buildHistory(this.workflow)"><i nz-icon nzType="snippets" nzTheme="outline"></i>构建历史</li>
                          </ul>
                      </nz-dropdown-menu> &nbsp;
                      <button *ngIf="this.tabSelectedIndex === 1" nz-button (click)="syncTabs()"><i nz-icon nzType="sync" nzTheme="outline"></i>同步数据表</button>
                      &nbsp;
                      <button nz-button nzType="primary" (click)="saveTopology()"><i nz-icon nzType="save" nzTheme="outline"></i>保存</button>
                  </ng-template>


              </nz-spin>
          </div>
      </div>

      <nz-modal [(nzVisible)]="isSaveTopologyDialogVisible" nzTitle="设置数据流名称"
                (nzOnCancel)="handleSaveTopologyDialogCancel($event)" (nzOnOk)="handleSaveTopologyDialogOk()">
          <form nz-form [formGroup]="validateSaveTopologyDialogForm" (ngSubmit)="submitSaveTopologyDialogForm()">
              <nz-form-item>
                  <nz-form-label [nzSm]="6" [nzXs]="24" nzRequired nzFor="topologyName">名称</nz-form-label>
                  <nz-form-control [nzSm]="14" [nzXs]="24" nzErrorTip="请输入合法的数据流名称">
                      <input nz-input formControlName="topologyName" id="topologyName"/>
                  </nz-form-control>
              </nz-form-item>
          </form>
      </nz-modal>
  `,
  styles: [
      `
          @CHARSET "UTF-8";
          .clear {
              clear: both;
          }

          .ant-drawer-body {
              padding: 0px;
          }

          .list-unstyled {
              padding-left: 10px;
          }

          .list-unstyled li {
              display: inline-block;
              cursor: pointer;
              margin-top: 20px;
              text-align: center;
              widht: 60px;
          }

          .list-unstyled li label {
              display: block;
          }


          .control-bar {
              margin: 10px 10px 10px 0px;
          }

          /*============统一配置==================*/

          .designer_body {
              position: relative;
              min-height: 300px;
          }

          .body_vtoolsbar {
              position: absolute;
              width: 79px;
          }

          .body_container {
              margin-left: 80px;
              border-left: 1px solid #CBCCCC;
          }

          #body_layout {
              overflow: scroll;
              position: relative;
              z-index: 0;
          }
    `]
})
export class WorkflowAddComponent extends BasicWFComponent
  implements IDataFlowMainComponent, OnInit, AfterContentInit, AfterViewInit {

  _nodeTypes: Map<string /*type*/, NodeMeta> = new Map();
  public _nodeTypesAry: NodeMeta[] = [
    new NodeMeta(TYPE_DUMP_TABLE, 'table.svg', [40, 40], '数据表', WorkflowAddDbtableSetterComponent)
    , new NodeMeta('join', 'cog.svg', [40, 40], 'JOIN', WorkflowAddJoinComponent)
    , new NodeMeta('union', 'union.svg', [40, 40], "UNION", WorkflowAddUnionComponent)
    , new NodeMeta('nest', 'nestable.svg', [35, 35], 'NEST', WorkflowAddNestComponent)
  ];
  // tables: Table[];
  parent: WorkflowComponent;
  // workflow: any;
  isAdd = true;
  // title: string = '创建';
 // public _opened = false;

  isSaveTopologyDialogVisible = false;

  validateSaveTopologyDialogForm: FormGroup;
  topologyName: string;
  workflow: Dataflow;
  // erNodes: ERRules;
  // g6graph
  private graph: any;

  @ViewChild(AddAppFlowDirective, {static: true}) sidebarComponent: AddAppFlowDirective;
  dumpTabs: Map<string, DumpTable> = new Map();
  joinNodeMap: Map<string /*id*/, JoinNode> = new Map();

  nzOptions: any[] | null = null;


  @ViewChild('sqleditor', {static: false}) sqleditor: ElementRef;
  @ViewChild('draggableblock', {static: false}) draggableblock: ElementRef;

  @ViewChild(WorkflowERComponent, {static: false}) erRuleComponent: WorkflowERComponent;
  tabSelectedIndex = 0;

  public static addItem2UI(id: string, x: number, y: number, meta: NodeMeta, nmeta: BasicSidebarDTO): any {

    let model = {
      id: id,
      'x': x,
      'y': y,
      shape: 'image',
      img: meta.imgPath,
      size: [meta.width, meta.height],
      style: {
        'cursor': 'pointer'
      },
      'nodeMeta': nmeta
    };
    return model;
  }


  get pageTitle(): string {
    return (this.isAdd ? "添加数据流" : (this.topologyName));
  }

  public static addEdge(data: { edges: any[] }, id: string, sourceId: string, targetId: string, style?: any): void {

    style = !style ? {
      endArrow: true
    } : style;

    data.edges.push({
      'id': id,
      'source': sourceId,
      'target': targetId,
      'style': style
    });
  }


  constructor(tisService: TISService, //
              private _componentFactoryResolver: ComponentFactoryResolver,
              router: Router,
              route: ActivatedRoute,
              private fb: FormBuilder,
              notification: NzNotificationService,
              private cdr: ChangeDetectorRef,
              private modal: NzModalService,
              private drawerService: NzDrawerService) {
    super(tisService, modal, router, route, notification);
    // this.formDisabled = true;
    // this.workflow = new Workflow();
    //  this.cdr.detach();
//    console.log( Object.keys(graphlib));
    // this.setSizebarView(WorkflowAddDbtableSetterComponent);
    this._nodeTypesAry.forEach((n) => {
      this._nodeTypes.set(n.type, n);
    });
  }

  closePanel(): void {
   // this._opened = true;
  }

  // 保存图形
  saveTopology() {
    if (this.erRuleComponent) {
      let g6 = this.erRuleComponent.g6Graph;
      let dumpNodes: any[] = [];
      g6._cfg.nodes.forEach((r: any) => {
        // evt.item.ermeta
        // console.log(r.ermeta);
        let dumpNode = null;
        // if (r.ermeta) {
        //   dumpNode = {'node': r._cfg.model, 'ermeta': r.ermeta};
        // } else {
        dumpNode = {'node': r._cfg.model};
        // }
        dumpNodes.push(dumpNode);
      });
      let deges: any[] = [];
      g6._cfg.edges.forEach((r: any) => {
        // console.log(r._cfg.model);
        deges.push({'id': r._cfg.model.id, 'sourceNode': r._cfg.model.sourceNode._cfg.model, 'targetNode': r._cfg.model.targetNode._cfg.model, 'linkrule': r.linkrule});
      });

      let postData = {'nodes': dumpNodes, 'edges': deges, 'topologyName': this.topologyName};
      // postData = $.extend(postData, {});
      // console.log(postData);
      this.jsonPost(`/offline/datasource.ajax?emethod=save_er_rule&action=offline_datasource_action`
        , postData)
        .then(result => {
          if (result.success) {
            this.router.navigate(['/offline/wf']);
            this.notification.create('success', '成功', result.msg[0]);
          }
        });
    } else {

      let j = this.graph.save();
      if (j.nodes.length < 1) {
        this.notification.create('error', '错误', '请选择节点');
        return;
      }
      if (this.isAdd) {
        // 打开输入名称对话框
        this.isSaveTopologyDialogVisible = true;
      } else {
        this.applyTopology2Server(() => {
        });
      }
    }
  }

  handleSaveTopologyDialogCancel(evt: any) {
    this.isSaveTopologyDialogVisible = false;
  }

  submitSaveTopologyDialogForm() {
    for (const i in this.validateSaveTopologyDialogForm.controls) {
      this.validateSaveTopologyDialogForm.controls[i].markAsDirty();
      this.validateSaveTopologyDialogForm.controls[i].updateValueAndValidity();
    }
  }

  handleSaveTopologyDialogOk() {
    this.submitSaveTopologyDialogForm();
    if (!this.validateSaveTopologyDialogForm.valid) {
      return;
    }
    this.applyTopology2Server((topologyName) => {
      this.topologyName = topologyName;
    });
  }

  private applyTopology2Server(saveSuccessCallback: (topologyName) => void): void {
    // 关闭对话框
    this.isSaveTopologyDialogVisible = false;
    let j = this.graph.save();
    // console.log(j);
    // console.log(`topologyName:${this.topologyName}`);
    j = $.extend(j, this.validateSaveTopologyDialogForm.getRawValue());
    if (!this.isAdd) {
      j.topologyName = this.topologyName;
    }
    this.jsonPost(`/offline/datasource.ajax?emethod=${this.isAdd ? 'save' : 'update'}_topology&action=offline_datasource_action`, j)
      .then(result => {
        if (result.success) {
          saveSuccessCallback(j.topologyName);
          let biz = result.bizresult;
          if (!biz.erExist) {
            // 跳转到ER编辑Tab
            this.modal.confirm({
              nzTitle: '尚未定义ER关系，是否现在定义?',
              nzOnOk: () => {
                this.tabSelectedIndex = 1;
              },
              nzOnCancel: () => {
                this.notifyAndRedirect(result);
              }
            });
            return;
          }
          if (!biz.erPrimaryTabSet) {
            this.modal.confirm({
              nzTitle: '尚未定义DF的主表，是否现在定义主表?',
              nzOnOk: () => {
                this.tabSelectedIndex = 1;
              },
              nzOnCancel: () => {
                this.notifyAndRedirect(result);
              }
            });
            return;
          }
          this.notifyAndRedirect(result);
        }
      });
  }

  private notifyAndRedirect(result) {
    this.router.navigate(['/offline/wf']);
    this.notification.create('success', '成功', result.msg[0]);
  }

  ngOnInit(): void {

    this.validateSaveTopologyDialogForm = this.fb.group({
      topologyName: [null, [Validators.required]]
    });

    // let f: Observable<string> = this.route.fragment;
    // f.subscribe((frag) => {
    //   this.tabSelectedIndex = (frag === 'er') ? 1 : 0;
    // })

  }

  private drawNodes(g6graph: any, nmetas: NodeMetaConfig[], dumpNodes: NodeMetaDependency[]): void {
    // console.log(nmetas);
    // example:
    // let data = {
    //   nodes: [{
    //     id: 'node1',
    //     x: 100,
    //     y: 200
    //   }, {
    //     id: 'node2',
    //     x: 300,
    //     y: 200
    //   }],
    //   edges: [{
    //     source: 'node1',
    //     target: 'node2'
    //   }]
    // };

    let data: { nodes: any[], edges: any[] } = {
      nodes: [],
      edges: []
    };

    // let x = 0
    let dumpMode = null;
    let processNodeMode = null;
    // dump 节点
    let tabNodeMeta = this.getNodeMeta(TYPE_DUMP_TABLE);
    dumpNodes.forEach((d) => {
      let tabNode = new DumpTable(tabNodeMeta, d.id, d.extraSql, d.dbid, d.tabid, d.name);
      dumpMode = WorkflowAddComponent.addItem2UI(d.id, d.position.x, d.position.y, tabNodeMeta, tabNode);
      dumpMode.label = d.name;

      dumpMode.nodeMeta = tabNode;

      this.dumpTabs.set(d.id, dumpMode.nodeMeta);

      data.nodes.push(dumpMode);
    });


    let n: NodeMetaConfig;
    let meta: NodeMeta;
    // let pos = new Pos(100, 200);
    // JOIN 节点处理
    for (let i = 0; i < nmetas.length; i++) {
      n = nmetas[i];
      meta = this.getNodeMeta(n.type);
      processNodeMode = WorkflowAddComponent.addItem2UI(n.id, n.position.x, n.position.y, meta, new JoinNode(meta, n.id));
      processNodeMode.label = n.exportName;

      //  nodeMeta: NodeMeta, public exportName?: string, public id?: string, public position?: Pos, public sql?: string
      let m = new JoinNode(this.getNodeMeta(n.type), n.id, n.exportName, n.position, n.sql);
      this.joinNodeMap.set(n.id, m);
      processNodeMode.nodeMeta = m;
      data.nodes.push(processNodeMode);
      n.dependencies.forEach((target) => {
        let uid = this.getUid();
        m.addEdgeId(uid);
        m.addDependency(new Option(target.id, target.name));
        // data.edges.push({
        //   'id': uid,
        //   'source': n.id,
        //   'target': target.id,
        //   'style': {
        //     endArrow: true
        //   }
        // });
        WorkflowAddComponent.addEdge(data, uid, n.id, target.id);
      });
    }

    g6graph.data(data);
    // data.nodes = nodes;
    g6graph.render();
  }


  private getNodeMeta(type: string): NodeMeta {
    let meta = this._nodeTypes.get(type);
    if (meta === undefined) {
      throw new Error(`nodetype:${type} is illegal`);
    }
    return meta;
  }

  // 取得随机ID
  public getUid(): string {
    return BasicFormComponent.getUUID();
  }


// 自动排列
  autoArrangement(): void {
    const data = this.graph.save();
    // https://github.com/dagrejs/dagre/wiki
    const g = new dagre.graphlib.Graph();
    // 设置边上的标签
    g.setDefaultEdgeLabel(function () {
      return {};
    });
    // 设置布局方式
    g.setGraph({'rankdir': 'BT', 'align': 'UR'});
    // 设置节点id与尺寸
    data.nodes.forEach((node: any) => {
      // console.log(node);
      g.setNode(node.id, {width: node.size[0], height: node.size[1]});
    });
    // 设置边的起始节点和终止节点
    data.edges.forEach((edge: any) => {
      g.setEdge(edge.source, edge.target);
    });
    // 执行布局
    dagre.layout(g);

    // 然后再设置
    let coord;
    g.nodes().forEach((node: any, i: number) => {
      coord = g.node(node);
      // 设置节点的位置信息
      data.nodes[i].x = coord.x;
      data.nodes[i].y = coord.y;
    });
    g.edges().forEach((edge: any, i: number) => {
      //   coord = g.edge(edge);
      // 设置边的起点
      //   data.edges[i].startPoint = coord.points[0];
      // 设置边的终点
      //   data.edges[i].endPoint = coord.points[coord.points.length - 1];
      // 设置边的控制点
      //   data.edges[i].controlPoints = coord.points.slice(1, coord.points.length - 1);
    });
    this.graph.data(data);
    this.graph.render();

  }

  /**
   * [getRelativePos 鼠标位置相对于一个dom的相对坐标，以dom的左上点为坐标原点]
   * @param  {number} pageX 鼠标位置x
   * @param  {number} pageY 鼠标位置y
   * @param  {dom} dom jQuery dom元素，相对的坐标系元素
   * @return {object} {x:x,y:y} 返回相对dom元素的坐标对象
   */
  private getRelativePos(pageX: number, pageY: number, dom: any
    , clientPos: { x: number, y: number, nodemeta: NodeMeta }): { x: number, y: number, nodemeta: NodeMeta } {
    let doff = dom.offset();
    if (doff == null) {
      doff = {left: 0, top: 0};
    }

    clientPos.x = pageX - doff.left + dom.scrollLeft();
    clientPos.y = pageY - doff.top + dom.scrollTop();

    return clientPos;
  }


  _toggleSidebar(): void {
   // this._opened = !this._opened;
  }

  ngAfterViewInit(): void {

    let wd = $(window);

    wd.bind('resize.designer', (evt: any) => {
        let height = wd.height() - $("#tis-navbar").outerHeight()
          - $(".designer_htoolsbar").outerHeight() - 40;

        let width = wd.width() - $('.body_vtoolsbar').outerWidth() - 1;
        $("#body_layout").height(height);
        $("#body_layout").width(width);

        $('body').height($(window).height());
      }
    );

    setTimeout(() => {
      $(window).trigger("resize.designer");
    });

    // let sqlmirror = fromTextArea(this.sqleditor.nativeElement, this.sqleditorOption);

    // sqlmirror.setValue("select * from mytable;");

    const grid = new Grid({
      cell: 20,
      type: 'dot'
    });
    let canvas_container = $('#canvas_container')[0];
    this.graph = new G6.Graph({
      container: canvas_container,
      plugins: [grid],
      width: 3000,
      height: 1500,
      modes: {
        'click': ['click-select'],
        'default': ['drag-node']
      }
    }); // 'drag-canvas'
    // this.graph.setMode('drag');
    // graph.data({
    //   nodes: [{
    //     x: 100,
    //     y: 100,
    //     shape: 'circle',
    //     label: 'circle',
    //   }]
    // });
    // graph.render();
    let params = this.route.snapshot.params;
    // 有参数即为更新模式
    this.topologyName = params['name'];
    if (this.topologyName !== undefined) {
      // this.isAdd = false;
      // setTimeout(() => {
      this.isAdd = false;

      // });
      let action = `emethod=get_workflow_topology&action=offline_datasource_action&topology=${this.topologyName}`;
      this.httpPost('/offline/datasource.ajax', action)
        .then(result => {
          if (result.success) {
            // 执行逻辑表
            let nmetas: NodeMetaConfig[] = result.bizresult.nodeMetas;
            // 数据库源表
            let dumpNode: NodeMetaDependency[] = result.bizresult.dumpNodes;

            this.workflow = new Dataflow();
            let profile = result.bizresult.profile;
            this.workflow.id = profile.dataflowId;
            this.workflow.name = profile.name;
            this.drawNodes(this.graph, nmetas, dumpNode);
          }
        });
    }


// jQuery.select("");

// @ts-ignore
    const draggable = new Draggable(this.draggableblock.nativeElement,
      {
        draggable: 'img',
        // mirror: {appendTo: document.getElementById('body_layout')}
      });

    let clientPos: { x: number, y: number, nodemeta?: NodeMeta } = {x: 0, y: 0};

    draggable.on('drag:start', (evt: any) => {
      // @ts-ignore
      let type = $(evt.source).attr('type');
      clientPos.nodemeta = this._nodeTypes.get(type);
    });
    let bodyLayout = $('#body_layout');
    draggable.on('drag:move', (evt: any) => {
      // @ts-ignore
      clientPos = this.getRelativePos(evt.sensorEvent.clientX, evt.sensorEvent.clientY, bodyLayout, clientPos);
    });

    draggable.on('drag:stop', (evt: any) => {
      const nodeid = this.getUid();
      let nm: NodeMeta = clientPos.nodemeta;
      // console.log(nm.type);
      let sidebarDTO: BasicSidebarDTO;
      switch (nm.type) {
        case TYPE_DUMP_TABLE:
          sidebarDTO = new DumpTable(nm, nodeid);
          break;
        case 'join':
          sidebarDTO = new JoinNode(nm, nodeid);
          break;
        case 'union':
        case 'nest':
          this.infoNotify('该类型节点还未开放，敬请期待');
          return;
        default:
          throw new Error(`invalid type ${clientPos.nodemeta.type},nm.type:${nm.type}`);
      }

      this.graph.addItem('node', WorkflowAddComponent.addItem2UI(nodeid, clientPos.x, clientPos.y, clientPos.nodemeta, sidebarDTO));
      this.openSideBar(this.graph, nodeid, clientPos.nodemeta.compRef, sidebarDTO);
      // this._opened = true;
    });

    this.graph.on('node:click', (evt: any) => {

      let nodeinfo = evt.item._cfg;

      let nmeta: BasicSidebarDTO = nodeinfo.model.nodeMeta;
      clientPos.nodemeta = this.getNodeMeta(nmeta.nodeMeta.type);
      this.openSideBar(this.graph, nodeinfo.id, clientPos.nodemeta.compRef, nmeta);
      // this._opened = true;
    });
    this.cdr.detectChanges();
  }

  // selectNode 用于在update流程下传输selectNode对象
  private openSideBar(g6graph: any, nodeid: any, component: Type<any>, nmeta: BasicSidebarDTO): void {
    // console.log(component);
    let contentParams: any = {"parentComponent": this, "g6Graph": g6graph};
    if (nmeta) {
      contentParams.nodeMeta = nmeta.nodeMeta;
    }
    const drawerRef = this.drawerService.create<BasicSideBar, {}, {}>({
      nzWidth: "40%",
      nzPlacement: "right",
      nzTitle: '',
      nzClosable: false,
      nzContent: component,
     // nzWrapClassName: 'get-gen-cfg-file',
      nzContentParams: contentParams
    });
    drawerRef.afterOpen.subscribe(() => {
      let sideBar = drawerRef.getContentComponent();
      sideBar.initComponent(this, nmeta);

      sideBar.saveClick.subscribe((d: any) => {
        sideBar.subscribeSaveClick(g6graph, $, nodeid, this, d);
      });

      // 关闭对话框
      // sideBar.onClose.subscribe(() => {
      //   drawerRef.close();
      //   // this._opened = false;
      // });
    });
    // let sideBar = drawerRef.getContentComponent();
    // sideBar.parentComponent = this;
    // if (nmeta) {
    //   sideBar.nodeMeta = nmeta.nodeMeta;
    // }
    // sideBar.g6Graph = g6graph;


  }


  // private setSizebarView(component: Type<any>): ComponentRef<any> {
  //   let componentFactory = this._componentFactoryResolver.resolveComponentFactory(component);
  //   let sidebarRef = this.sidebarComponent.viewContainerRef;
  //   sidebarRef.clear();
  //
  //   let cref: ComponentRef<any> = sidebarRef.createComponent(componentFactory);
  //   return cref;
  // }


  ngAfterContentInit(): void {
  }

  addWorkflow(form: any): void {
    // // console.log(jQuery(form).serialize());
    // let action = jQuery(form).serialize();
    // // console.log(this.isAdd);
    // if (this.isAdd) {
    //   // 添加工作流
    //   this.httpPost('/offline/datasource.ajax', action)
    //     .then(result => {
    //       console.log(result);
    //       if (result.success) {
    //         this.parent.processResultPublic(result);
    //         this.parent.initWorkflows(result.bizresult);
    //         this.activeModal.close();
    //       } else {
    //         this.processResult(result);
    //       }
    //     });
    // } else {
    //   // 编辑工作流
    //   action = action.replace('event_submit_do_add_workflow', 'event_submit_do_edit_workflow');
    //   this.httpPost('/offline/datasource.ajax', action)
    //     .then(result => {
    //       // console.log(result);
    //       if (result.success) {
    //         this.activeModal.close();
    //       } else {
    //         this.processResult(result);
    //       }
    //     });
    // }

  }

// , 'id': edgeInfo.id
// , 'sourceNode': edgeInfo.sourceNode._cfg.model.nodeMeta
// , 'targetNode': edgeInfo.targetNode._cfg.model.nodeMeta
  erNodesEdgeClick(e: { 'g6': any, id: string, 'sourceNode': DumpTable, 'targetNode': DumpTable, 'linkrule': { linkKeyList: LinkKey[], cardinality: string } }) {
    // trigger from WorkflowERComponent
    // console.log(e);

    let nodeid = e.id;

    let emeta = new ERRuleNode(e, this.topologyName);

    this.openSideBar(e.g6, nodeid, WorkflowAddErCardinalityComponent, emeta);

    // this.setSizebarView(WorkflowAddErCardinalityComponent);
   // this._opened = true;

  }

  erNodesNodeClick(e: { 'g6': any, 'dumpnode': DumpTable /**点击的nodeid*/, 'ermeta': ERMetaNode }) {
    // 'dumpnode': nodeInfo.nodeMeta
    let dumpNode = e.dumpnode;
    if (!dumpNode) {
      throw new Error(`dumpNode can not be null`);
    }
    let emeta: ERMetaNode = e.ermeta;
    if (!emeta) {
      emeta = new ERMetaNode(dumpNode, this.topologyName);
    }
    // console.log(e.ermeta);
    // let emeta = new ERMetaNode(dumpNode, this.topologyName);
    this.openSideBar(e.g6, dumpNode.nodeid, WorkflowAddErMetaComponent, emeta);
   // this._opened = true;
  }

  syncTabs() {
    this.erRuleComponent.erTabSelect(true);
  }
}


export class Workflow {
  private _name: string;
  private _task: string;
  private _dependTableIds: number[];

  constructor() {
  }

  get name(): string {
    return this._name;
  }

  set name(value: string) {
    this._name = value;
  }

  get task(): string {
    return this._task;
  }

  set task(value: string) {
    this._task = value;
  }

  get dependTableIds(): number[] {
    return this._dependTableIds;
  }

  set dependTableIds(value: number[]) {
    this._dependTableIds = value;
  }
}
