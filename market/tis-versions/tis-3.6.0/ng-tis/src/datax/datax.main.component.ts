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

import {ActivatedRoute, Router} from "@angular/router";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";
import {NzModalRef, NzModalService} from "ng-zorro-antd/modal";
import {ChartDataSets, ChartOptions} from "chart.js";


// 这个类专门负责router
@Component({
  template: `

      <nz-spin [nzSpinning]="this.formDisabled" [nzDelay]="1000" nzSize="large">
          <tis-page-header [showBreadcrumb]="false" (refesh)="get_view_data()">
          </tis-page-header>
          <nz-row [nzGutter]="16">
              <nz-col [nzSpan]="4">
                  <nz-card [nzTitle]="'统计'">
                      <div style="" class="resource-spec">
                          <nz-statistic
                                  [nzValue]="(allStatis.successCount | number: '1.')!"
                                  [nzTitle]="'成功'"
                                  [nzPrefix]="prefixTplSuccess"
                                  [nzSuffix]="'次'"
                                  [nzValueStyle]="{ color: '#3F8600' }"
                          >
                          </nz-statistic>
                          <ng-template #prefixTplSuccess><i nz-icon nzType="check" nzTheme="outline"></i></ng-template>
                          <nz-statistic
                                  [nzValue]="(allStatis.errCount | number: '1.')!"
                                  [nzTitle]="'失败'"
                                  [nzPrefix]="prefixTplFaild"
                                  [nzSuffix]="'次'"
                                  [nzValueStyle]="{ color: '#CF1322' }"
                          >
                          </nz-statistic>
                          <ng-template #prefixTplFaild><i nz-icon nzType="close" nzTheme="outline"></i></ng-template>
                      </div>
                  </nz-card>
              </nz-col>
              <nz-col [nzSpan]="20">
                  <nz-card [nzTitle]="'执行(成功/失败)统计'" [nzExtra]="extraTemplate">
                      <div style="height: 250px">
                          <canvas *ngIf="barChartData.length>0" baseChart [datasets]="barChartData" [labels]="barChartLabels"
                                  [options]="barChartOptions" [legend]="false" [chartType]="'bar'">
                          </canvas>
                      </div>
                  </nz-card>
                  <ng-template #extraTemplate>
                      <!--                      <button nz-button (click)="get_view_data()"><i nz-icon nzType="redo" nzTheme="outline"></i></button>-->
                  </ng-template>
              </nz-col>
          </nz-row>
      </nz-spin>

      <!--      <full-build-history [dataxProcess]="true"></full-build-history>-->

  `,
  styles: [`

      .resource-spec {
          display: flex;
          height: 250px
      }

      .resource-spec nz-statistic {
          flex: 1;
          margin-right: 20px;
      }

      .tis-node-label tspan {
          font-size: 10px;
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
export class DataxMainComponent extends AppFormComponent {
// http://localhost:8080/coredefine/corenodemanage.ajax?action=core_action&emethod=get_view_data

  allStatis: { errCount: number, successCount: number } = {errCount: 0, successCount: 0};

  public barChartData: ChartDataSets[] = [
    // {data: [], label: 'updateCount'}
    // {backgroundColor: '#95e4fa', data: []},
  ];
  barChartLabels: Array<any> = [];
  barChartOptions: ChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    // aspectRatio: 1.7,
    scales: {
      yAxes: [{
        ticks: {
          min: 0
        }
      }]
    }
  };

  constructor(tisService: TISService, modalService: NzModalService
    , route: ActivatedRoute, private router: Router) {
    super(tisService, route, modalService);
  }


  /**
   * 统计最近8天的执行统计
   */
  reload_exec_statistics(): void {

    let url = '/coredefine/corenodemanage.ajax';
    this.httpPost(url, 'action=datax_action&emethod=get_exec_statistics').then((data) => {
      if (data.success) {
        // let rows = [{timeLab: "5/12", successCount: 1, errCount: 1}, {timeLab: "5/13", successCount: 3, errCount: 1}]; // data.bizresult;
        this.allStatis = data.bizresult.statis;
        let rows = data.bizresult.data;
        let successData: ChartDataSets = {label: "success", backgroundColor: '#398400', data: []};
        //  successData.data = [];
        let faildData: ChartDataSets = {label: "faild", backgroundColor: '#af0f16', data: []};
        // faildData.data = [];
        let labels: Array<any> = [];
        this.barChartLabels = [];
        rows.forEach((r: any) => {
          //
          successData.data.push(r.successCount);
          //
          faildData.data.push(r.errCount);
          labels.push(r.timeLab);
        });
        // console.log([successData, faildData]);
        this.barChartData = [successData, faildData];
        this.barChartLabels = labels;
      }
    });


  }

  ngOnInit(): void {
    super.ngOnInit();
  }

  protected initialize(app: CurrentCollection): void {
    this.reload_exec_statistics();
  }


  get_view_data() {
    this.reload_exec_statistics();
  }
}


