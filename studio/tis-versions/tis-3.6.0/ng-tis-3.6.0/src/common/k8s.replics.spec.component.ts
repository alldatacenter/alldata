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

import {AfterContentInit, AfterViewInit, Component, Input, OnInit} from "@angular/core";
import {BasicFormComponent} from "./basic.form.component";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {TISService} from "./tis.service";
import {ActivatedRoute} from "@angular/router";
import {NzModalService} from "ng-zorro-antd/modal";
import {Item, ItemPropVal} from "./tis.plugin";


@Component({
  selector: `k8s-replics-spec`,
  template: `
      <tis-form [fieldsErr]="this.errorItem" [labelSpan]="this.labelSpan" [controlerSpan]="this.controlSpan" [formGroup]="specForm">
          <tis-ipt #pods title="Pods" name="pods" require>
              <nz-input-group nzCompact [nzAddOnAfter]="podUnit" style="width:200px">
                  <nz-input-number [nzDisabled]="disabled" name="pods" formControlName="pods" class="input-number"  [nzStep]="1" ></nz-input-number>
              </nz-input-group>
          </tis-ipt>
          <tis-ipt title="CPU" name="cpu" require>
              <div class="resource-spec">
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('cuprequest')}">
                      <nz-input-group nzAddOnBefore="Request" nzCompact [nzAddOnAfter]="cpuRequestTpl">
                          <nz-input-number [nzDisabled]="disabled" name="cuprequest" class="input-number" formControlName="cuprequest" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <ng-template #cpuRequestTpl>
                          <nz-select [nzDisabled]="disabled" class="spec-unit" formControlName="cuprequestunit">
                              <nz-option [nzLabel]="'millicores'" [nzValue]="'m'"></nz-option>
                              <nz-option [nzLabel]="'cores'" [nzValue]="'cores'"></nz-option>
                          </nz-select>
                      </ng-template>
                      <div *ngIf="hasErr('cuprequest')" class="ant-form-item-explain">{{controlErr('cuprequest')}}</div>
                  </div>
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('cuplimit')}">
                      <nz-input-group nzAddOnBefore="Limit" nzCompact [nzAddOnAfter]="cpuLimitTpl">
                          <nz-input-number [nzDisabled]="disabled" formControlName="cuplimit" class="input-number" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <ng-template #cpuLimitTpl>
                          <nz-select [nzDisabled]="disabled" class="spec-unit" formControlName="cuplimitunit">
                              <nz-option [nzLabel]="'millicores'" [nzValue]="'m'"></nz-option>
                              <nz-option [nzLabel]="'cores'" [nzValue]="'cores'"></nz-option>
                          </nz-select>
                      </ng-template>
                      <div *ngIf="hasErr('cuplimit')" class="ant-form-item-explain">{{controlErr('cuplimit')}}</div>
                  </div>
              </div>
          </tis-ipt>
          <tis-ipt title="Memory" name="memory" require>
              <div class="resource-spec">
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('memoryrequest')}">
                      <nz-input-group nzAddOnBefore="Request" nzCompact [nzAddOnAfter]="memoryrequestTpl">
                          <nz-input-number [nzDisabled]="disabled" formControlName="memoryrequest" class="input-number" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <ng-template #memoryrequestTpl>
                          <nz-select [nzDisabled]="disabled" class="spec-unit" formControlName="memoryrequestunit">
                              <nz-option [nzLabel]="'MB'" [nzValue]="'M'"></nz-option>
                              <nz-option [nzLabel]="'GB'" [nzValue]="'G'"></nz-option>
                          </nz-select>
                      </ng-template>
                      <div *ngIf="hasErr('memoryrequest')" class="ant-form-item-explain">{{controlErr('memoryrequest')}}</div>
                  </div>
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('memorylimit')}">
                      <nz-input-group nzAddOnBefore="Limit" nzCompact [nzAddOnAfter]="memorylimitTpl">
                          <nz-input-number [nzDisabled]="disabled" formControlName="memorylimit" class="input-number" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <ng-template #memorylimitTpl>
                          <nz-select [nzDisabled]="disabled" class="spec-unit" formControlName="memorylimitunit">
                              <nz-option [nzLabel]="'MB'" [nzValue]="'M'"></nz-option>
                              <nz-option [nzLabel]="'GB'" [nzValue]="'G'"></nz-option>
                          </nz-select>
                      </ng-template>
                      <div *ngIf="hasErr('memorylimit')" class="ant-form-item-explain">{{controlErr('memorylimit')}}</div>
                  </div>
              </div>
          </tis-ipt>
          <tis-ipt title="弹性扩缩容" name="hpa" require>
              <div >
                  <nz-switch [nzDisabled]="disabled" nzCheckedChildren="开" nzUnCheckedChildren="关" formControlName="supportHpa"></nz-switch>
              </div>
              <div *ngIf="specForm?.get('supportHpa').value" class="resource-spec">
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('cpuAverageUtilization')}">
                      <nz-input-group nzAddOnBefore="CPU平均利用率" [nzAddOnAfter]="'%'" nzCompact>
                          <nz-input-number [nzDisabled]="disabled" name="cpuAverageUtilization" class="input-number" formControlName="cpuAverageUtilization" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <div *ngIf="hasErr('cpuAverageUtilization')" class="ant-form-item-explain">{{controlErr('cpuAverageUtilization')}}</div>
                  </div>
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('minHpaPod')}">
                      <nz-input-group nzAddOnBefore="最小Pods" [nzAddOnAfter]="podUnit" nzCompact>
                          <nz-input-number [nzDisabled]="disabled" name="minHpaPod" class="input-number" formControlName="minHpaPod" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <div *ngIf="hasErr('minHpaPod')" class="ant-form-item-explain">{{controlErr('minHpaPod')}}</div>
                  </div>
                  <div [ngClass]="{'ant-form-item-has-error':hasErr('maxHpaPod')}">
                      <nz-input-group nzAddOnBefore="最大Pods" [nzAddOnAfter]="podUnit" nzCompact>
                          <nz-input-number [nzDisabled]="disabled" name="maxHpaPod" class="input-number" formControlName="maxHpaPod" [nzMin]="1" [nzStep]="1"></nz-input-number>
                      </nz-input-group>
                      <div *ngIf="hasErr('maxHpaPod')" class="ant-form-item-explain">{{controlErr('maxHpaPod')}}</div>
                  </div>
              </div>
              <div *ngIf="specForm?.get('supportHpa').value" style="padding-top: 5px">
                  <nz-alert nzType="info" [nzMessage]="suggestMsg"></nz-alert>
                  <ng-template #suggestMsg>
                      在启用<strong>弹性扩缩容</strong>之前，请先在K8S集群中部署<strong>metrics-server</strong>,不然<strong>弹性扩缩容</strong>无法正常工作，具体请参照<a target="_blank" href="https://github.com/kubernetes-sigs/metrics-server">https://github.com/kubernetes-sigs/metrics-server</a>
                  </ng-template>
              </div>
          </tis-ipt>
      </tis-form>
      <ng-template #podUnit>个</ng-template>
  `
  , styles: [`
        .resource-spec {
            display: flex;
        }

        .resource-spec .spec-unit {
            width: 150px;
        }

        .resource-spec div {
            flex: 1;
            margin-right: 20px;
        }

        .input-number {
            width: 100%;
        }

        .spec-form {
        }
  `]
})
export class K8SReplicsSpecComponent extends BasicFormComponent implements AfterContentInit, AfterViewInit, OnInit {
  specForm: FormGroup;
  @Input()
  labelSpan = 2;
  @Input()
  controlSpan = 20;

  @Input()
  disabled = false;

  @Input()
  errorItem: Item;
  @Input()
  rcSpec: K8SRCSpec;

  constructor(tisService: TISService, modalService: NzModalService, private fb: FormBuilder) {
    super(tisService, modalService);
    this.specForm = this.fb.group({
      supportHpa: [ false, [Validators.required]],
      minHpaPod: [1],
      maxHpaPod: [3],
      cpuAverageUtilization: [10, [Validators.max(100), Validators.min(1)]],
      pods: [1, [Validators.required]],
      cuprequest: [500, [Validators.required]],
      cuprequestunit: ['m', [Validators.required]],
      cuplimitunit: ['cores', [Validators.required]],
      cuplimit: [1, [Validators.required]],
      memoryrequest: [500, [Validators.required]],
      memoryrequestunit: ['M', [Validators.required]],
      memorylimit: [2, [Validators.required]],
      memorylimitunit: ['G', [Validators.required]]
    });
  }

  hasErr(control: string): boolean {
    if (!this.errorItem) {
      return false;
    }
    // @ts-ignore
    let itemPP: ItemPropVal = this.errorItem.vals[control];
    return itemPP && itemPP.hasFeedback;
  }

  controlErr(control: string): string {
    if (!this.errorItem) {
      return '';
    }
    // @ts-ignore
    let itemPP: ItemPropVal = this.errorItem.vals[control];
    if (!itemPP) {
      return '';
    }
    return itemPP.error;
  }

  ngAfterContentInit(): void {
    // this.specForm.get
  }

  public get k8sControllerSpec(): K8SRCSpec {
    return this.specForm.value;
  }

  ngAfterViewInit(): void {
  }

  ngOnInit(): void {

    if (!this.rcSpec) {
      this.rcSpec = {
        'pods': 1,
        supportHpa: false, minHpaPod: 1, maxHpaPod: 3, cpuAverageUtilization: 10,  cuprequest: 500,
        cuprequestunit: 'm',
        cuplimitunit: 'cores',
        cuplimit: 1,
        memoryrequest: 500,
        memoryrequestunit: 'M',
        memorylimit: 2,
        memorylimitunit: 'G'
      };
    }
   // console.log(this.rcSpec);
    this.specForm.setValue(this.rcSpec);
    // this.specForm.setErrors({"cuprequest": "dddddddd"});
    // console.log(this.specForm.errors);
    // console.log(this.specForm.value);
  }
}


export interface K8SRCSpec {
  supportHpa: boolean,
  minHpaPod: number,
  maxHpaPod: number,
  cpuAverageUtilization: number,
  pods: number,
  cuprequest: number,
  cuprequestunit: string,
  cuplimitunit: string,
  cuplimit: number,
  memoryrequest: number,
  memoryrequestunit: string,
  memorylimit: number,
  memorylimitunit: string
}


