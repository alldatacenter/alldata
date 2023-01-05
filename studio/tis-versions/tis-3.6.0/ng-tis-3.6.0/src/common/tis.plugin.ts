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

// import {EventEmitter} from "@angular/core";
import {BasicFormComponent} from "./basic.form.component";
import {NzSelectModeType} from "ng-zorro-antd/select";

export const CONST_FORM_LAYOUT_VERTICAL = 3;

export const KEY_OPTIONS_ENUM = "enum";
export declare type PluginName = 'mq' | 'k8s-config' | 'fs' | 'datasource' | 'dataxReader' | 'params-cfg' | 'appSource' | 'dataxWriter' | 'datax-worker';
export declare type PluginMeta = {
  skipSubformDescNullError?: boolean;
  name: PluginName, require: boolean, extraParam?: string
  // 服务端对目标Item的desc进行过滤
  , descFilter?: (desc: Descriptor) => boolean
};
export declare type PluginType = PluginName | PluginMeta;

export const TYPE_ENUM = 5;
export const TYPE_PLUGIN_SELECTION = 6;
export const TYPE_PLUGIN_MULTI_SELECTION = 8;
export const KEY_DEFAULT_VALUE = 'dftVal';

// 某一插件某一属性行
export class ItemPropVal {
  key: string;
  type: number;
  options: Array<ValOption>;
  required: boolean;
  // 如果考到通用性的化这里应该是数组类型，现在考虑到简单实现，线默认用一个单独的
  descVal: DescribleVal;
  advance: boolean;
  error: string;
  public _eprops: { string: any };
  private dftVal: any;
  placeholder: string;
  _primaryVal: any = undefined;
  // 是否是主键
  pk: boolean;
  has_set_primaryVal = false;
  disabled = false;


  constructor(public updateModel = false) {
  }

  set eprops(vals: { String: any }) {
    // @ts-ignore
    this._eprops = vals || {};
    this.dftVal = this._eprops[KEY_DEFAULT_VALUE];
    this.placeholder = this._eprops['placeholder'] || '';
  }


  public setPropValEnums(cols: Array<{ name: string, value: string }>, colItemChecked?: (optVal) => boolean) {
    // console.log([cols, colItemChecked]);
    if (!colItemChecked) {
      colItemChecked = (_) => true;
    }
    let enums: Array<OptionEnum> = [];
    cols.forEach((s) => {
      enums.push({label: s.name, val: s.value, checked: colItemChecked(s.value)})
    });
    this.setEProp(KEY_OPTIONS_ENUM, enums);
  }


  get label(): string {
    let label = this._eprops['label'];
    return label ? label : this.key;
  }

  /**
   * 当
   */
  get enumMode(): NzSelectModeType {
    return this.getEProp('enumMode') || 'default';
  }

  public getEProp(key: string): any {
    return this._eprops[key];
  }

  public setEProp(key: string, val: any): void {
    this._eprops[key] = val;
  }

  get hasFeedback(): boolean {
    return !(!this.error);
  }

  get validateStatus(): string {
    return this.hasFeedback ? 'error' : '';
  }

  set primary(val: any) {
    this._primaryVal = val;
  }

  get primary(): any {
    // console.log(this);
    if (!this.updateModel && !this.has_set_primaryVal && this.dftVal !== undefined) {
      // 新增模式下
      this._primaryVal = this.dftVal;
      this.has_set_primaryVal = true;
    }
    if (this._primaryVal === undefined) {
      this._primaryVal = (this.type === TYPE_ENUM && this.enumMode === 'multiple') ? [] : '';
    }
    return this._primaryVal;
    // return this.updateModel ? this._primaryVal : this.dftVal;
  }

  get primaryVal(): boolean {
    return !(this.descVal);
  }
}

export class Descriptor {
  // 表单内嵌深度，深度到达一定深度，表单的布局需要调整一下
  formLevel: number;
  impl: string;
  implUrl: string;
  containAdvance: boolean;
  displayName: string;
  extendPoint: string;
  attrs: AttrDesc[];
  extractProps: { string: any };
  veriflable: boolean;
  pkField: string;
  // subform relevant

  subFormMeta: {
    behaviorMeta: any,
    fieldName: string,
    idList: Array<string>
    id?: string,
  }
  subForm: boolean;

  /**
   *
   * @param h
   * @param des
   * @param updateModel 是否是更新模式，在更新模式下，插件的默认值不能设置到控件上去
   */
  public static addNewItem(h: HeteroList, des: Descriptor, updateModel: boolean
    , itemPropSetter: (key: string, propVal: ItemPropVal) => ItemPropVal): void {
    let nItem = new Item(des);
    nItem.displayName = des.displayName;
    nItem.implUrl = des.implUrl;
    // nItem.containAdvance = des.containAdvance;
    des.attrs.forEach((attr) => {
      nItem.vals[attr.key] = itemPropSetter(attr.key, attr.addNewEmptyItemProp(updateModel));
    });
    let nitems: Item[] = [];
    h.items.forEach((r) => {
      nitems.push(r);
    });
    // console.log(nItem);
    nitems.push(nItem);
    h.items = nitems;
  }


  public static addNewItemByDescs(h: HeteroList, decs: Array<Descriptor>, updateModel: boolean
    , itemPropSetter: (key: string, propVal: ItemPropVal) => ItemPropVal): void {
    let des: Descriptor;
    // let nitems: Item[] = [];
    for (let index = 0; index < decs.length; index++) {
      des = decs[index];
      Descriptor.addNewItem(h, des, updateModel, itemPropSetter);
    }
  }
}

export interface TisResponseResult {
  bizresult?: any;
  success: boolean;
  errormsg?: string[];
  action_error_page_show?: boolean;
  msg?: Array<any>;
  errorfields?: Array<Array<Array<IFieldError>>>;
}

/**
 * 对应一个plugin的输入项
 */
export class Item {
  impl = '';
  implUrl: string;
  //  vals: Map<string /**key*/, string | DescribleVal> = new Map();
  // vals: Map<string /**key*/, ItemPropVal> = new Map();
  // 后一种类型支持subform的类型
  /**
   * subform format:
   * <pre>
   *   vals:{
   *     tableName:[
   *      {
   *       impl:""
   *       vals:{ k1:v1,k2:v2,k3:v3}
   *      },{},{}
   *     ]
   *   }
   *
   * </pre>
   */
  public vals: { [key: string]: ItemPropVal }
    | { [key: string]: { [key: string]: ItemPropVal } }
    | { [key: string]: Array<Item> } = {};
  displayName = '';
  private _propVals: ItemPropVal[];

  /**
   * 表单中有高级字段，是否显示全部？
   */
  public showAllField = false;

  // containAdvance = false;

  /**
   * 字段中是否包含高级字段（可以隐藏）
   */
  public get containAdvanceField(): boolean {
    return this.dspt.containAdvance;
  }

  /**
   * 创建一个新的Item
   *
   * @param fieldNames
   */
  public static create(fieldNames: string[]): Item {
    let item = new Item(null);
    fieldNames.forEach((fname) => {
      item.vals[fname] = new ItemPropVal();
    });
    return item;
  }

  public static processErrorField(errorFields: Array<Array<IFieldError>>, items: Item[]) {
    let item: Item = null;
    let fieldsErrs: Array<IFieldError> = null;

    if (errorFields) {
      for (let index = 0; index < errorFields.length; index++) {
        fieldsErrs = errorFields[index];
        item = items[index];
        let itemProp: ItemPropVal;
        fieldsErrs.forEach((fieldErr) => {
          let ip = item.vals[fieldErr.name];
          if (ip instanceof ItemPropVal) {
            itemProp = ip;
            itemProp.error = fieldErr.content;

            if (!itemProp.primaryVal) {
              if (fieldErr.errorfields.length !== 1) {
                throw new Error(`errorfields length ${fieldErr.errorfields.length} shall be 1`);
              }
              Item.processErrorField(fieldErr.errorfields, [itemProp.descVal]);
            }
          } else {
            throw new Error("illegal type");
          }
        });
      }
    }
  }

  public static processFieldsErr(result: TisResponseResult): Item {
    let errFields = result.errorfields;
    if (errFields && errFields.length > 0) {
      let pluginsErr = errFields[0];
      if (pluginsErr.length > 0) {
        let pluginErr: Array<IFieldError> = pluginsErr[0];
        let errKeys = pluginErr.map((r) => r.name);
        let item: Item = Item.create(errKeys);
        Item.processErrorField(pluginsErr, [item]);
        return item;
      }
    }
    return Item.create([]);
  }

  public static wrapItemPropVal(v: any, at: AttrDesc): ItemPropVal {
    if (v === undefined || v === null) {
      return;
    }
    let newVal: ItemPropVal = at.addNewEmptyItemProp(true);
    if (at.describable) {
      let d = at.descriptors.get(v.impl);
      if (!d) {
        //
        throw new Error(`impl:${v.impl} can not find relevant descriptor`);
      }
      let ii: Item = Object.assign(new Item(d), v);
      ii.wrapItemVals();
      // console.log(ii);
      newVal.descVal = at.createDescribleVal(ii);
    } else {
      if (at.isMultiSelectableType) {
        if (!Array.isArray(v)) {
          // console.log(v);
          throw new Error("expect val type is array but is not");
        }
        // console.log([at, v, at.eprops[KEY_OPTIONS_ENUM]]);
        if (!at.eprops) {
          // console.log(at);
          throw new Error("at.eprops can not be null");
        }
        let selectableCol: Array<{ val: string, label: string }> = at.eprops[KEY_OPTIONS_ENUM];
        if (!selectableCol) {
          throw new Error("selectableCol can not be null");
        }
        let cols: Array<{ name: string, value: string }> = null;
        if (selectableCol.length < 1) {
          cols = v.map((r) => {
            return {name: r, value: r}
          });
          newVal.setPropValEnums(cols, (_) => true);
        } else {
          cols = selectableCol.map((c) => {
            return {"name": c.label, "value": c.val}
          });
          newVal.setPropValEnums(cols, (sval) => {
            return !!v.find((optVal) => optVal === sval);
          });
        }
        // console.log([selectableCol, cols]);

      } else {
        newVal._primaryVal = v;
      }
      // newVal.pk = (at.key === this.dspt.pkField);
    }
    return newVal;
  }

  constructor(public dspt: Descriptor, public updateModel = false) {
    if (dspt) {
      this.impl = dspt.impl;
    }
  }

  public get implVal() {
    if (!this.updateModel) {

    }
    return '';
  }

  public wrapItemVals(): void {
    let newVals = {};
    let ovals: any /**map*/ = this.vals;
    let newVal: ItemPropVal;
    // console.log(this.dspt.attrs);
    this.dspt.attrs.forEach((at) => {
      let v = ovals[at.key];
      // console.log([at.key, v]);
      newVal = Item.wrapItemPropVal(v, at);
      if (newVal) {
        newVals[at.key] = (newVal);
      }
    });
    this.vals = newVals;
  }

  public clearPropVals(dspClear = true): void {
    delete this._propVals;
    if (dspClear) {
      this.dspt = null;
    }
  }

  public get propVals(): ItemPropVal[] {
    if (this._propVals) {
      return this._propVals;
    }
    if (!this.dspt) {
      this._propVals = [];
      return this._propVals;
    }
    this._propVals = [];
    this.dspt.attrs.forEach((attr /**AttrDesc*/) => {
      let ip: ItemPropVal | { [key: string]: ItemPropVal } | Array<Item> = this.vals[attr.key];
      if (!ip) {
        // throw new Error(`attrKey:${attr.key} can not find relevant itemProp`);
        ip = attr.addNewEmptyItemProp(this.updateModel);
        this.vals[attr.key] = ip;
      }
      // console.log(ip);
      if (ip instanceof ItemPropVal) {
        this._propVals.push(ip);
      } else {
        throw new Error("illegal ip type");
      }
    });
    return this._propVals;
  }
}

export class DescribleVal
  extends Item {
  // impl: string;
  // displayName: string;
  // vals: string[] | DescribleVal[];
  descriptors: Map<string /* impl */, Descriptor> = new Map();
}

export class AttrDesc {
  key: string;
  ord: number;
  // 是否是主键
  pk: boolean;
  advance: boolean;
  /**
   * 当describable为true时descriptors 应该有内容
   * */
  descriptors: Map<string /*impl*/, Descriptor>;
  describable: boolean;
  type: number;
  options: Array<ValOption>;
  required: boolean;
  eprops: { String: any };

  // MULTI_SELECTABLE
  public get isMultiSelectableType(): boolean {
    return this.type === TYPE_PLUGIN_MULTI_SELECTION;
  }

  /**
   *
   * @param updateModel 是否是更新模式，在更新模式下，插件的默认值不能设置到控件上去
   */
  public addNewEmptyItemProp(updateModel: boolean): ItemPropVal {
    let desVal = new ItemPropVal(updateModel);
    desVal.key = this.key;
    desVal.pk = this.pk;
    desVal.advance = this.advance;
    desVal.eprops = Object.assign({}, this.eprops);
    desVal.required = this.required;
    desVal.type = this.type;
    // 当type为6时，options应该有内容
    desVal.options = this.options;
    if (this.describable) {
      desVal.descVal = this.createDescribleVal(new Item(null, updateModel));
      if (this.eprops) {
        let displayName = this.eprops[KEY_DEFAULT_VALUE];
        // displayName
        if (!updateModel && displayName) {
          // 在新建时候
          for (let e of desVal.descVal.descriptors.values()) {
            if (displayName === e.displayName) {
              desVal.descVal.impl = e.impl;
              desVal.descVal.dspt = e;
              break;
            }
          }
        }
      }
    }
    return desVal;
  }

  public createDescribleVal(v: Item): DescribleVal {
    let descVal = new DescribleVal(v.dspt, v.updateModel);
    descVal.displayName = v.displayName;
    // descVal.containAdvance = v.containAdvance;
    // descVal.impl = v.impl;
    descVal.vals = v.vals;
    this.descriptors.forEach((entry) => {
      descVal.descriptors.set(entry.impl, entry);
    });
    return descVal;
  }
}


/*HeteroList*/
export class HeteroList {
  descriptors: Map<string /* impl */, Descriptor> = new Map();
  private _descriptorList: Array<Descriptor>;

  public get descriptorList(): Array<Descriptor> {
    if (!this._descriptorList) {
      this._descriptorList = Array.from(this.descriptors.values());
    }
    return this._descriptorList;
  }


  identityId: string;
  // item 可选数量
  cardinality: string;
  caption: string;
  extensionPoint: string;
  extensionPointUrl: string;
  items: Item[] = [];

  pluginCategory: PluginType;

  public get identity(): string {
    return this.extensionPoint.replace(/\./g, '-');
  }

  public updateDescriptor(newDescriptors: Map<string /* impl */, Descriptor>): void {
    this.descriptors = newDescriptors;
    this._descriptorList = undefined;
  }

  public get addItemDisabled(): boolean {
    return (this.cardinality === '1' && this.items.length > 0);
  }
}

export class PluginSaveResponse {
  constructor(public  saveSuccess: boolean, public formDisabled: boolean, private bizResult?: any) {

  }

  public hasBiz(): boolean {
    return !!this.bizResult;
  }

  public biz(): any {
    return this.bizResult;
  }
}

export interface IFieldError {
  name: string;
  content?: string;
  errorfields?: Array<Array<IFieldError>>
}

export class ValOption {
  public impl: string;
  public name: string;
}

export interface OptionEnum {
  // {label: s.name, val: s.value, checked: colItemChecked(s.value)}
  label: string;
  val: string;
  checked: boolean;
}

export class SavePluginEvent {
  // savePlugin: EventEmitter<{ ?: boolean, ?: boolean }>;
  public verifyConfig = false;
  public notShowBizMsg = false;
  // 顺带要在服务端执行一段脚本
  // namespace:corename:method
  public serverForward;
  public basicModule: BasicFormComponent;
}


