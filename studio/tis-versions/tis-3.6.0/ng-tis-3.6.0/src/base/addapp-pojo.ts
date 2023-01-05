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

// 小白模式pojo
export class StupidModal {
  dataxName: string;
  fieldtypes: Array<SchemaFieldType> = [];
  schemaXmlContent: string;
  fields: Array<SchemaField> = [];
  uniqueKey: string;
  shareKey: string;
  tplAppId: number;

  public static deseriablize(r: StupidModal): StupidModal {
    let stupidModal = Object.assign(new StupidModal(), r);
    stupidModal.schemaXmlContent = r.schemaXmlContent;
    stupidModal.fieldtypes = [];
    stupidModal.fields = [];
    r.fields.forEach((ff: any) => {
      stupidModal.fields.push(Object.assign(new SchemaField(), ff));
    });
    r.fieldtypes.forEach((type: any) => {
      let nt: SchemaFieldType = Object.assign(new SchemaFieldType(), type);
      nt.tokensType = [];
      if (type.tokensType) {
        type.tokensType.forEach((token: any) => {
          nt.tokensType.push(Object.assign(new SchemaFieldTypeTokensType(), token));
        });
      }
      stupidModal.fieldtypes.push(nt);
    });
    return stupidModal;
  }

  public markFieldErr(err: FieldErrorInfo): void {
    let field = this.fields.find((f) => f.id === err.id);
    if (field) {
      field.errInfo = err;
    }
  }


}

export class SchemaFieldTypeTokensType {
  value: string;
  key: string;
}

export class SchemaFieldType {
  split = false;
  name: string;
  tokensType: Array<SchemaFieldTypeTokensType> = [];
}

export class SchemaField {
  sharedKey = false;
  indexed = false;
  docval = true;
  uniqueKey = false;
  index: number;
  fieldtype: string;
  multiValue = false;
  required = false;
  inputDisabled = true;
  split = false;
  stored = true;
  name: string;
  id: number;
  tokenizerType: string;

  errInfo: FieldErrorInfo = {};

  _editorOpen = false;
  get editorOpen(): boolean {
    return this._editorOpen;
  }

  set editorOpen(val: boolean) {
    this._editorOpen = val;
  }
}

export interface FieldErrorInfo {
  id?: number;
  fieldNameError?: boolean;
  fieldTypeError?: boolean;
  fieldPropRequiredError?: boolean;
}

export enum EnginType {
  Solr = 'solr',
  ES = 'es'
}

// 从schema编辑页面跳转到确认页面使用的包装对象
export class ConfirmDTO {
  dataxName: string;
// {appform: {tisTpl: any, workflow: any}, expertModel: boolean, expert: {xml: string}, stupid: {model:StupidModal}}
  // 使用的模板索引的appid
  tplAppId: number;
  appform: AppDesc;
  expertModel: boolean;
  expert: { xml: string };
  stupid: { model: StupidModal };

  // 当上一次索引已经创建，经过删除之后需要重新创建
  recreate = false;

  // 日常环境中使用的候选服务器
  coreNode: CoreNodeCandidate = new CoreNodeCandidate();
}

export class CoreNodeCandidate {
  shardCount = 1;
  replicaCount = 1;
  // "hostName":"10.1.5.19",
  // "luceneSpecVersion":5.3,
  // "nodeName":"10.1.5.19:8080_solr",
  // "solrCoreCount":11
  hosts: Array<{ hostName: string }> = [];
}

// 可选项
export class Option {
  name: string;
  value: string;
}

// 第一步提交的基本信息包装类
export class AppDesc {

  dsType: string;
  name: string;
  // tisTpl: string;
  workflow: string;
  dptId: string;
  recept: string;
  tabCascadervalues: any = {};

  // 部门列表
  dpts: Option[];

  public get checkedDptName(): string {

    if (!this.dpts) {
      throw new Error('dpts of AppDesc can not be null');
    }

    let o: Option = this.dpts.find((v) => {
      return v.value === this.dptId;
    });

    if (o) {
      return o.name;
    } else {
      return '';
    }

  }

}
