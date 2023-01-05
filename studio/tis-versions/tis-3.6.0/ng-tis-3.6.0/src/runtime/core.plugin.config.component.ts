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

import {Component} from "@angular/core";
import {TISService} from "../common/tis.service";

import {ActivatedRoute, Router} from "@angular/router";
import {AppFormComponent, CurrentCollection} from "../common/basic.form.component";
import {NzModalService} from "ng-zorro-antd/modal";


@Component({
  styles: [`

      .head-tool-block {
          float: right;
      }

      .plugin-block {
          padding-left: 10px;
          padding-top: 10px;
          clear: both;
      }

      .one-plugin {
          background-color: #ededed;
          padding: 1em;
          margin-top: 5px;
          margin-bottom: 5px;
      }

      .plugin-name {
          font-weight: bold;
          font-size: 25px;
      }

      .head-link {
          color: red;
          font-size: 30px;
      }

  `],
  template: `

      <tis-page-header [showBreadcrumb]="false" title="插件配置" [result]="result" >
      </tis-page-header>

      <ul class="nav nav-tabs">
          <li class="nav-item">
              <a class="nav-link active" href="#">Schema</a>
          </li>
          <li class="nav-item">
              <a class="nav-link" href="#">Core</a>
          </li>
          <li class="nav-item">
              <a class="nav-link" href="#">离线</a>
          </li>
          <li class="nav-item">
              <a class="nav-link" href="#">在线</a>
          </li>
          <li class="nav-item">
              <a class="nav-link" href="#">反馈</a>
          </li>
      </ul>

      <div>
          <div style="margin-top: 10px">
              <div style="float:right">
                  <button type="button" class="btn btn-primary btn-sm" (click)="savePluginConfig()">保存配置</button>
              </div>
          </div>

          <div class="plugin-block" *ngFor="let r of _heteroList">

              <h5>{{r.fullCaption}}</h5>

              <div class="one-plugin" *ngFor="let i of r.allItems">
                  <div class="plugin-header">
                      <div class="head-tool-block">
                          <a class="head-link" href="javascript:void(0);" (click)="deletePlugin(r.items,i)">
                              <i class="fa fa-close fa-lg" aria-hidden="true"></i>
                          </a>

                      </div>
                      <span class="plugin-name">{{i.pluginName}}</span>
                  </div>

                  <form style="clear:both;">
                      <div class="form-group row" *ngFor="let ff of i.fields">
                          <label for="static{{i.displayName}}{{ff.name}}" class="col-sm-2 col-form-label">{{ff.name}}</label>
                          <div class="col-sm-10">
                              <input #ipt type="text" [name]="ff.name" (change)="changeIptField(ff,i)"
                                     [class.is-invalid]="ff.iptInvalid" class="form-control "
                                     id="static{{i.displayName}}{{ff.name}}" [(ngModel)]="ff.value"/>
                              <div *ngIf="ff.iptInvalid" class="invalid-feedback">{{ff.validateMsg}}</div>
                          </div>
                      </div>
                  </form>
              </div>


              <div class="btn-group">
                  <button class="btn btn-secondary btn-sm dropdown-toggle"
                          type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                      添加
                  </button>
                  <div class="dropdown-menu">
                      <a class="dropdown-item" *ngFor="let t of r.descriptors"
                         href="javascript:void(0)" (click)="addPluginClick(r.items,t)">{{t.displayName}}</a>

                  </div>
              </div>

              <hr/>
          </div>
      </div>
      <div style="clear:both"></div>
  `
})
export class CorePluginConfigComponent extends AppFormComponent {

  _heteroList: HeteroList[];

  constructor(tisService: TISService, modalService: NzModalService
    , route: ActivatedRoute, private router: Router) {
    super(tisService, route, modalService);
  }


  protected initialize(app: CurrentCollection): void {

    this.httpPost('/coredefine/coredefine.ajax'
      , 'action=core_action&emethod=get_plugin_config_info')
      .then((r) => {
        if (r.success) {
          this._heteroList = this.clonelist(r.bizresult);
        }
      });
  }

  private clonelist(_heteroList: HeteroList[]): HeteroList[] {

    let hlist: HeteroList[] = [];
    let ary: any[] = _heteroList;

    ary.forEach(function (r) {

      let h: HeteroList = Object.assign(new HeteroList(), r);
      h.items = [];

      r.items.forEach(function (ii: any) {
        h.items.push(Object.assign(new PluginItem(), ii));
      });

      h.descriptors = [];

      r.descriptors.forEach(function (d: any) {
        h.descriptors.push(Object.assign(new PluginDescriptor(), d));
      });

      hlist.push(h);
    });

    return hlist;

  }


  savePluginConfig(): void {

    let postlist = this.clonelist(this._heteroList);

    postlist.forEach(function (rr) {
      rr.items.forEach(function (item) {
        item.clearFields();
      });
    });

    this.jsonPost('/coredefine/coredefine.ajax?action=core_action&emethod=save_plugin_config'
      , JSON.stringify(postlist))
      .then((r) => {

        if (r.success) {

        }

      });
  }

  // 字段发生变化需要对表单进行校验
  changeIptField(ipt: Field
    , item: PluginItem): void {

    ipt.iptInvalid = false;
    // item[ipt.name] = ipt.value;
    let post = Object.assign(new PluginItem(), item);
    post.clearFields();
// item.resetValidating();
    // let r = item.getFieldValidate(ipt.name);
// console.log(post);
    // r.iptInvalid = false;
    this.jsonPost('/coredefine/coredefine.ajax?action=core_action&emethod=validate_plugin_field&keyname=' + ipt.name
      , JSON.stringify(post))
      .then((r) => {

        if (r.success) {
          if (r.bizresult) {
            let b = r.bizresult;
            if ('ERROR' === b.kind) {
              ipt.iptInvalid = true;
              ipt.validateMsg = b.message;
            }
          }
        }
      });

  }

  // 添加plugin按钮点击
  addPluginClick(items: PluginItem[], desc: PluginDescriptor): void {

    // console.log(desc);
    let item = new PluginItem();
    item.pluginName = desc.displayName;
    item.klass = desc.id;

    item.keys = desc.formFieldsDesc;

    items.push(item);

  }

  deletePlugin(items: PluginItem[], del: PluginItem): void {

    for (let i = 0; i < items.length; i++) {
      if (items[i] === del) {
        items.splice(i, 1);
        return;
      }
    }

    //

  }

  // // 显示所有的字段枚举
  // getFieldEnums(descriptors: PluginDescriptor[],klass : string) : FormFieldDescriptor[] {
  //
  //   let desc = descriptors.find(function (r) {
  //    return r.id === klass;
  //   });
  //
  //   return desc.formFieldsDesc;
  // }

  public refresh(): void {
    // console.("refresh");

  }


}

class HeteroList {
  caption: string;
  items: PluginItem[];
  descriptors: PluginDescriptor[];

  // constructor(json:any){
  //  this.deserialize(json,this);
  // }

  // private deserialize(json :any, instance :any) {
  //   for(var prop in json) {
  //     if(!json.hasOwnProperty(prop))
  //       continue;
  //     if(typeof json[prop] === 'object')
  //       this.deserialize(json[prop], instance[prop]);
  //     else
  //       instance[prop] = json[prop];
  //   }
  // }

  get fullCaption(): string {
    return this.caption;
  }

  private injectFieldDesc = false;

  get allItems(): PluginItem[] {

    if (!this.injectFieldDesc) {

      let d = this.descriptors;
      this.items.forEach(function (item) {
        let desc = d.find(function (dd: PluginDescriptor) {
          return dd.id === item.klass;
        });
        if (!desc) {
          throw new Error("item:" + item.klass + " can not find relevant desc");
        }
        item.keys = desc.formFieldsDesc;
      });
      // console.log("this.items:"+ this.items.length);
      this.injectFieldDesc = true;
    }


    return this.items;
  }
}

class PluginItem {
  displayName: string;
  klass: string;
  pluginName: string;

  keys: FormFieldDescriptor[];

  private _fields: Field[];

  get fields(): Field[] {

    if (!this._fields) {
      let ii = this;
      this._fields = [];
      let fs = this._fields;
      if (this.keys) {
        this.keys.forEach(function (key) {
          fs.push(new Field(key.name, false, '', ii));
        });
      }
    }
    return this._fields;
    // return _fields;
    // return [];
    // let ii = this;
    // let flist:Field[] = [];
    // this.keys.forEach(function (key) {
    //   flist.push(new Field(key.name,false,'', ii));
    // });
    // return flist;
  }

  resetValidating() {
    this.fields.forEach(function (f) {
      f.iptInvalid = false;
    });

  }

  clearFields(): void {
    this._fields = null;
    this.keys = null;
  }


}

class Field {
  constructor(public name: string
    , public iptInvalid: boolean, public validateMsg: string, private _item: PluginItem) {

  }

  get value(): string {
    return this._item[this.name];
  }

  set value(val: string) {
    this._item[this.name] = val;
  }

}

class PluginDescriptor {
  displayName: string;

  formFieldsDesc: FormFieldDescriptor[];

  id: string;

}

class FormFieldDescriptor {
  name: string;
  type: string;
}





