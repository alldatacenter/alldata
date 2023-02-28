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

import {Component, ElementRef, ViewChild} from "@angular/core";
import {TISService} from "../common/tis.service";
import {TriggerDumpComponent} from "./trigger_dump.component";
import {PojoComponent} from "./pojo.component";
import {SnapshotChangeLogComponent} from "./snapshot.change.log";
import {ActivatedRoute, Router} from "@angular/router";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";
import * as dagreD3 from 'dagre-d3';
import * as d3 from 'd3';
import {NzModalService} from "ng-zorro-antd/modal";


// 这个类专门负责router
@Component({
  template: `
      <nz-spin [nzSpinning]="this.formDisabled" [nzDelay]="1000" nzSize="large">
          <tis-page-header [showBreadcrumb]="false" (refesh)="get_view_data()">
          </tis-page-header>
          <nz-row [nzGutter]="16">
              <nz-col [nzSpan]="3">
                  <nz-card class="primary-card">
                      <nz-statistic [nzValue]="(instanceDirDesc.allcount | number)!" [nzTitle]="'总记录数(条)'"></nz-statistic>
                  </nz-card>
              </nz-col>
              <nz-col [nzSpan]="5">
                  <nz-card class="primary-card" [nzExtra]="todayMetricsQueryTemplate">
                      <nz-statistic [nzValue]="(this.todayMetrics?.queryCount | number: '1.0-2')!" [nzTitle]="'当天查询次数'"></nz-statistic>
                  </nz-card>
                  <ng-template #todayMetricsQueryTemplate>
                      <a routerLink="./monitor"><i nz-icon nzType="line-chart" nzTheme="outline"></i></a>
                  </ng-template>
              </nz-col>
              <nz-col [nzSpan]="5">
                  <nz-card class="primary-card" [nzExtra]="todayMetricsUpdateTemplate">
                      <nz-statistic [nzValue]="(this.todayMetrics?.updateCount | number: '1.0-2')!" [nzTitle]="'当天更新次数'"></nz-statistic>
                  </nz-card>
                  <ng-template #todayMetricsUpdateTemplate>
                      <a routerLink="./monitor"><i nz-icon nzType="line-chart" nzTheme="outline"></i></a>
                  </ng-template>
              </nz-col>
              <nz-col [nzSpan]="11">
                  <nz-card nzTitle="副本目录信息" class="primary-card">
                      {{instanceDirDesc.desc}}
                  </nz-card>
              </nz-col>
          </nz-row>
          <br/>
          <nz-row [nzGutter]="16">
              <nz-col [nzSpan]="24">
                  <nz-card [nzTitle]="'节点拓扑'">
                      <nz-row [nzGutter]="16">
                          <nz-col [nzSpan]="2">
                              <ul id="tis-node-enum">
                                  <li style="background-color: #57A957" [ngStyle]="{'background-color': STATE_COLOR.COLOR_Active }">Active</li>
                                  <li style="background-color: #d5dd00">Recovering</li>
                                  <li style="background-color: #c48f00">Down</li>
                                  <li style="background-color: #C43C35">Recovery Failed</li>
                                  <li style="background-color: #e0e0e0">Gone</li>
                              </ul>
                          </nz-col>
                          <nz-col [nzSpan]="22">
                              <svg id="svg-canvas" #svgblock width='100%' height=600></svg>
                          </nz-col>
                      </nz-row>
                  </nz-card>
              </nz-col>
          </nz-row>
      </nz-spin>
  `,
  styles: [`
      .tis-node-label tspan {
          font-size: 10px;
      }

      #tis-node-enum {
          margin: 0px;
          padding-left: 4px;
      }

      #tis-node-enum li {
          display: inline-block;
          padding: 3px;
          list-style: none;
          margin-bottom: 4px;
          border: 1px solid #d9d9d9;
          border-radius: 6px;
          width: 9em;
      }

      .primary-card {
          height: 150px;
      }

      .clusters rect {
          fill: #00ffd0;
          stroke: #999;
          stroke-width: 1.5px;
      }

      text {
          font-weight: 300;
          font-family: "Helvetica Neue", Helvetica, Arial, sans-serf;
          font-size: 14px;
      }

      .node rect {
          stroke: #999;
          fill: #fff;
          stroke-width: 1.5px;
      }

      .edgePath path {
          stroke: #333;
          stroke-width: 1.5px;
      }
  `]
})
export class CorenodemanageComponent extends AppFormComponent {
// http://localhost:8080/coredefine/corenodemanage.ajax?action=core_action&emethod=get_view_data
  app: any;
  config: any;
  instanceDirDesc: any = {allcount: 0};
  todayMetrics: TodayMetrics = {queryCount: 0, updateCount: 0};

  STATE_COLOR = {
    COLOR_Active: '#57A957',
    COLOR_Recovering: '#d5dd00',
    COLOR_Down: '#c48f00',
    COLOR_Recovery_Failed: '#C43C35',
    COLOR_Gone: '#e0e0e0',
  }

  @ViewChild('svgblock', {static: false}) svgblock: ElementRef;

  constructor(tisService: TISService, modalService: NzModalService
    , route: ActivatedRoute, private router: Router) {
    super(tisService, route, modalService);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.get_view_data();
  }

  get_view_data() {
    this.httpPost('/coredefine/corenodemanage.ajax', 'action=core_action&emethod=get_view_data')
      .then((r) => {
        if (r.success) {
          this.app = r.bizresult.app;
          this.config = r.bizresult.config;
          this.instanceDirDesc = r.bizresult.instanceDirDesc;
          this.todayMetrics = r.bizresult.metrics;
          this.paintToplog(this.currentApp, this.createGraph(), r.bizresult.topology);
        }
      });
  }

  protected initialize(app: CurrentCollection): void {

  }

  private paintToplog(app: CurrentCollection, g: any, data: any): void {
    let appname = app.appName;
    g.setNode(appname, {label: appname, style: 'fill: white;stroke-width: 1.5px;stroke: #999'});

    data.shareds.forEach((shard) => {
      g.setNode(shard.name, {label: shard.name, clusterLabelPos: 'top', style: 'fill: #d3d7e8;stroke-width: 1.5px;stroke: #999'});

      shard.replics.forEach((r) => {
        let props = r.properties;
        let fillColor = 'white';
        switch (r.state) {
          case 'ACTIVE':
            fillColor = this.STATE_COLOR.COLOR_Active;
            break;
          case 'DOWN':
            fillColor = this.STATE_COLOR.COLOR_Down;
            break;
          case 'RECOVERING':
            fillColor = this.STATE_COLOR.COLOR_Recovering;
            break;
          case 'RECOVERY_FAILED':
            fillColor = this.STATE_COLOR.COLOR_Recovery_Failed;
            break;
          default:
            throw new Error(`illegal state:${props.state}`);
        }
        let stroke = '';
        if (props.leader) {
          stroke = 'stroke-width: 2px;stroke: black';
        }

        g.setNode(r.name, {label: r.name, class: 'tis-node-label', shape: "ellipse", style: `fill: ${fillColor};cursor: pointer;${stroke}`});
        g.setParent(r.name, shard.name);
        g.setEdge(r.name, appname, {style: 'stroke: #333;stroke-width: 1.5px;fill: none;'});
      });
    });
    g.nodes().forEach(function (v) {
      let node = g.node(v);
      // console.log(node);
      // node.elem.onclick = function () {
      //   console.log("xxxxx");
      // };
      // Round the corners of the nodes
      node.rx = node.ry = 5;
    });
    let render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
    let svg = d3.select(this.svgblock.nativeElement),
      svgGroup = svg.append("g");
// Run the renderer. This is what draws the final graph.
    render(svg.select("g"), g);

    svg.selectAll("g.node").on("click", function (d) {

    });

// Center the graph
    let xCenterOffset = (svg.attr("width") - g.graph().width) / 2;
    // svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
    svgGroup.attr("transform", "translate(" + 100 + ", 20)");
    // console.log(g.graph().height);
    svg.attr("height", g.graph().height + 40);
  }


  private createGraph(): any {

    // Create the input graph
    let g = new dagreD3.graphlib.Graph({compound: true})
      .setGraph({
        nodesep: 10,
        ranksep: 40,
        rankdir: "LR",
        marginx: 10,
        marginy: 20
      })
      .setDefaultEdgeLabel(function () {
        return {};
      });
    return g;
  }

  public jsonString(v: any): string {
    return JSON.stringify(v);
  }

  // 立刻触发全量索引构建
  public triggerFullBuild(): void {

    this.httpPost('/coredefine/corenodemanage.ajax'
      , 'event_submit_do_trigger_fullbuild_task=y&action=core_action')
      .then((d) => {
        if (d.success) {
          if (d.bizresult) {
            let msg: any = [];
            msg.push({
              'content': '全量索引构建已经触发'
              , 'link': {'content': '状态日志', 'href': './buildprogress/' + d.bizresult.taskid}
            });

            this.processResult({success: true, 'msg': msg});
          } else {
            alert("重复触发了");
          }
        } else {
          this.processResult(d);
        }
      });


    // let msg: any = [];
    // msg.push({
    //   'content': '全量索引构建已经触发'
    //   , 'link': {'content': '查看构建状态', 'href': './buildprogress/' + 123}
    // });
    //
    // this.processResultWithTimeout({'success': true, 'msg': msg}, 10000);


  }


// 打开模态对话框
  public openModal(): void {

  }

  // 配置同步到线上
  // public openSyncConfigDialog(): void {
  //   // this.modalService.open(SyncConfigComponent, {size: 'lg'});
  //   this.openLargeDialog(SyncConfigComponent);
  // }

  // 变更历史
  public openSnapshotVerChangeLog(): void {
    // this.modalService.open(SnapshotChangeLogComponent, {windowClass: 'schema-edit-modal'});
    this.openDialog(SnapshotChangeLogComponent, {nzTitle: "版本切换历史"});
  }

  // // 从其他索引拷贝索引配置
  // public openCopyOtherIndexDialog(): void {
  //   // this.modalService.open(CopyOtherCoreComponent, {size: 'lg'});
  //   this.openLargeDialog(CopyOtherCoreComponent);
  // }

  public openTriggerFullDumpDialog(): void {
    // 打开触发全量构建对话框
    //   const modalRef = this.modalService.open(TriggerDumpComponent);
    this.openDialog(TriggerDumpComponent, {nzTitle: "触发全量索引构建"});

  }

  // // 打开Schema编辑页面
  // public openSchemaDialog(snapshotId: number, editable: boolean): void {
  //   let modalRef: NzModalRef<SchemaXmlEditComponent> =
  //     this.openLargeDialog(SchemaXmlEditComponent);
  //   modalRef.getContentComponent().snapshotid = snapshotId;
  //
  // }

  // 打开Pojo编辑页面
  public openPojoDialog(): void {
    // var modalRef: NgbModalRef = this.modalService.open(PojoComponent, {windowClass: 'schema-edit-modal'});

    this.openDialog(PojoComponent, {nzTitle: "POJO"});

  }


  public pushConfigAndEffect(): void {

    this.httpPost('/coredefine/corenodemanage.ajax'
      , 'action=core_action&needReload=true&emethod=update_schema_all_server').then((r) => {
      this.processResult(r);
    });
  }

  public pushConfig(): void {
    this.httpPost('/coredefine/corenodemanage.ajax'
      , 'action=core_action&needReload=false&emethod=update_schema_all_server').then((r) => {
      this.processResult(r);
    });
  }

  // 打开solr编辑页面
  public openSolrConfigDialog(snapshotId: number, editable: boolean): void {
    // let modalRef: NgbModalRef
    //   =  // this.modalService.open(SolrCfgEditComponent, {windowClass: 'schema-edit-modal'});
    // this.openNormalDialog(SolrCfgEditComponent);
    // modalRef.componentInstance.snapshotid = snapshotId;
  }

  public openGlobalParametersDialog() {


  }

  // closeResult: string;
  //
  // public opendialog(content: any): void {
  //
  //   this.modalService.open(content).result.then((result) => {
  //     this.closeResult = `Closed with: ${result}`;
  //   }, (reason) => {
  //     this.closeResult = `Dismissed ${this.getDismissReason(reason)}`;
  //   });
  //   console.info("haha");
  // }
  //
  // private getDismissReason(reason: any): string {
  //   if (reason === ModalDismissReasons.ESC) {
  //     return 'by pressing ESC';
  //   } else if (reason === ModalDismissReasons.BACKDROP_CLICK) {
  //     return 'by clicking on a backdrop';
  //   } else {
  //     return `with: ${reason}`;
  //   }
  // }
  gotoFullBuildHistory() {
    // <a class="dropdown-item" routerLink="./full_build_history"></a>
    this.router.navigate(["/offline/wf/build_history/45"]);
  }


}

interface TodayMetrics {
  // "metrics":{
  //   "queryCount":0,
  //   "updateCount":0
  // },
  queryCount: number;
  updateCount: number;
}

