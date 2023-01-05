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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {TISService} from '../common/tis.service';
import {TableAddStep} from './table.add.step';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';

import {TabColReflect, TablePojo} from './table.add.component';
import {FormComponent} from "../common/form.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {Descriptor, TisResponseResult} from "../common/tis.plugin";
import {PluginsComponent} from "../common/plugins.component";
import {DataxDTO} from "../base/datax.add.component";
import {ExecModel} from "../base/datax.add.step7.confirm.component";
import {BasicFormComponent} from "../common/basic.form.component";

declare var jQuery: any;


@Component({
  selector: 'tableAddStep1',
  template: `
      <tis-form [spinning]="formDisabled" #form>
          <tis-page-header [showBreadcrumb]="false" [result]="result">
              <input type="hidden" name="event_submit_do_check_table_logic_name_repeat" value="y"/>
              <input type="hidden" name="action" value="offline_datasource_action"/>
              <button nz-button nzType="primary" (click)="createNextStep(form)" [disabled]="!_dto.tablePojo.dbId">下一步</button>
          </tis-page-header>
          <tis-ipt #dbname title="数据库" name="dbname" require>
              <nz-select *ngIf="!updateMode" [name]="dbname.name" [id]="dbname.name" [(ngModel)]="_dto.tablePojo.db">
                  <nz-option *ngFor="let db of dbs" [nzValue]="db" [nzLabel]="db.name"></nz-option>
              </nz-select>
              <input nz-input *ngIf="updateMode" [name]="dbname.name" [id]="dbname.name" disabled [value]="_dto.tablePojo.dbName"/>
          </tis-ipt>

          <!--          <tis-ipt #table *ngIf='tbs.length>0 && !updateMode' title="表名" name="table" require>-->
          <!--              <nz-select [name]="table.name" [id]="table.name" [nzShowSearch]="true" (change)="tabChange()" [(ngModel)]="dto.tableName">-->
          <!--                  <nz-option *ngFor="let t of tbs" [nzValue]="t.value" [nzLabel]="t.name"></nz-option>-->
          <!--              </nz-select>-->
          <!--          </tis-ipt>-->

          <!--          <tis-ipt *ngIf='updateMode' title="表名" name="table" require>-->
          <!--              <input nz-input tis-ipt-prop disabled [value]="dto.tableName"/>-->
          <!--          </tis-ipt>-->
      </tis-form>
  `
})
export class TableAddStep1Component extends TableAddStep implements OnInit {
  switchType = 'single';
  dbs: { name: string, value: string }[] = [];
  tbs: { name: string, value: string }[] = [];

  // _db: TablePojo;
  _dto: DataxDTO;

  @Input() set dto(val: DataxDTO) {
    this._dto = val;
  };

  @Output() processHttpResult: EventEmitter<any> = new EventEmitter();

  public static getReaderDescAndMeta(bcpt: BasicFormComponent, _dto: DataxDTO): Promise<TisResponseResult> {
    return bcpt.jsonPost('/offline/datasource.ajax?event_submit_do_get_ds_relevant_reader_desc=y&action=offline_datasource_action', _dto.tablePojo)
      .then(result => {
        if (result.success) {
          _dto.processMeta = result.bizresult.processMeta;
          let rdescIt: IterableIterator<Descriptor> = PluginsComponent.wrapDescriptors(result.bizresult.readerDesc).values();
          // dto.writerDescriptor = wdescIt.next().value;
          _dto.readerDescriptor = rdescIt.next().value;
          // console.log(dto);
          // this.nextStep.emit(this._dto);
          return result;
        }
      });
  }

  constructor(tisService: TISService, protected router: Router,
              private activateRoute: ActivatedRoute, protected location: Location) {
    super(tisService, router, location);
  }


  get updateMode(): boolean {
    return !this._dto.tablePojo.isAdd;
  }

  // DB名称选择
  // public dbChange(dbid: string) {
  //   this.tbs = [];
  //   // this.tablePojo.tableName = null;
  //   this.tabChange();
  //   this.httpPost('/offline/datasource.ajax'
  //     , `event_submit_do_select_db_change=y&action=offline_datasource_action&dbid=${dbid}`
  //   ).then(result => {
  //     if (result.success) {
  //       this.tbs = result.bizresult;
  //     }
  //   });
  // }

  public tabChange(): void {

  }


  ngOnInit(): void {
    // if (this.dto && this.dto.dbId) {
    // this.dbChange(this.dto.dbId);
    // }

    this.httpPost('/offline/datasource.ajax',
      'event_submit_do_get_usable_db_names=y&action=offline_datasource_action')
      .then(
        result => {
          this.processHttpResult.emit(result);
          if (result.success) {
            this.dbs = result.bizresult;
          }
        });
  }

  changeType(value: string): void {
    // console.log(value);
    this.switchType = value;
  }

  public createNextStep(form: any): void {
    // console.log(this.tablePojo);
    TableAddStep1Component.getReaderDescAndMeta(this, this._dto).then((result) => {
      if (result.success) {
        this.nextStep.emit(this._dto);
      }
    });
    // 校验库名和表名是否存在
    // this.jsonPost('/offline/datasource.ajax?event_submit_do_check_table_logic_name_repeat=y&action=offline_datasource_action', this.dto)
    //   .then(
    //     result => {
    //       if (result.success) {
    //         let biz = result.bizresult;
    //         this.dto.selectSql = biz.sql;
    //         this.dto.cols = [];
    //         if (biz.cols) {
    //           biz.cols.forEach((col: any) => {
    //             let c = Object.assign(new TabColReflect(), col);
    //             this.dto.cols.push(c);
    //           });
    //         }
    //         this.nextStep.emit(this.dto);
    //       } else {
    //         this.processResult(result);
    //         this.processHttpResult.emit(result);
    //       }
    //     }
    //   );
  }


}
