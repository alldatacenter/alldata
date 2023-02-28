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

import {AfterContentInit, AfterViewInit, Component, OnInit} from '@angular/core';
import {BasicSideBar, BasicSidebarDTO, DumpTable, IDataFlowMainComponent, JoinNode, NodeMeta, Option} from '../common/basic.form.component';
import {TISService} from '../common/tis.service';


import {WorkflowAddComponent} from "./workflow.add.component";
import {NzModalService} from "ng-zorro-antd/modal";
import {Item} from "../common/tis.plugin";
import {NzDrawerRef} from "ng-zorro-antd/drawer";


@Component({
  template: `

      <div>

          <sidebar-toolbar (close)="_closeSidebar($event)"
                           (save)="_saveClick()" (delete)="_deleteNode()"></sidebar-toolbar>
          <tis-form [fieldsErr]="errorItem" formLayout="vertical">
              <tis-page-header [showBreadcrumb]="false" [result]="result">
              </tis-page-header>
              <tis-ipt #nodeName title="名称" name="exportName" require="true">
                  <input required type="text" [id]="nodeName.name" nz-input [(ngModel)]="joinNodeForm.nodeName" [name]="nodeName.name"/>
              </tis-ipt>
              <tis-ipt #dptNodes title="依赖节点" name="dependencies" require="true">
                  <nz-select [name]="dptNodes.name" nzMode="tags" style="width: 100%;" nzPlaceHolder="请选择"
                             [(ngModel)]="joinNodeForm.dependenciseTabIds">
                      <nz-option *ngFor="let option of listOfOption"
                                 [nzLabel]="option.label"
                                 [nzValue]="option.value"></nz-option>
                  </nz-select>
              </tis-ipt>
              <tis-ipt #sql title="SQL" name="sql" require="true">
                  <tis-codemirror class="ant-input" [name]="sql.name" [size]="{width:null,height:600}"
                                  [(ngModel)]="joinNodeForm.joinSql"></tis-codemirror>
              </tis-ipt>
          </tis-form>
      </div>

  `,

  styles: [
      `
          .item-head {
              margin: 20px 0px 0px 0px;
          }

          #sqleditorBlock {
              width: 100%;
          }

          .clear {
              clear: both;
          }
    `]
})
// JOIN 节点设置
export class WorkflowAddJoinComponent
  extends BasicSideBar implements OnInit, AfterContentInit, AfterViewInit {
  errorItem: Item = Item.create([]);
  joinNodeForm: JoinNodeForm;

  // @ViewChild('sqleditor', {static: false}) sqleditor: ElementRef;
  listOfOption: Array<Option> = [];

  constructor(tisService: TISService, modalService: NzModalService, drawerRef: NzDrawerRef<BasicSideBar>) {
    super(tisService, modalService, drawerRef);
  }

  initComponent(_: IDataFlowMainComponent, selectNode: BasicSidebarDTO): void {

    // @ts-ignore
    let dto: JoinNode = selectNode;
    this.joinNodeForm = new JoinNodeForm(this.nodeMeta, dto, this);

    this.refeshDependencyOption();
  }

  private refeshDependencyOption(): void {
    this.listOfOption = [];
    this.parentComponent.dumpTabs.forEach((t: DumpTable, key: string) => {
      this.listOfOption.push(new Option(key, t.tabname));
    });
    this.parentComponent.joinNodeMap.forEach((t: JoinNode, key: string) => {
      // 需要将本节点过滤
      if (this.joinNodeForm.dto.id === key) {
        return;
      }
      this.listOfOption.push(new Option(key, t.exportName));
    });
  }

  // 删除节点
  _deleteNode() {
    // console.log(this.joinNodeForm.dto);
    let id = this.joinNodeForm.dto.id;
    let node = this.g6Graph.findById(id);
    //  console.log(node);

    this.g6Graph.removeItem(node);
    this.parentComponent.joinNodeMap.delete(id);
    this.refeshDependencyOption();
    this._closeSidebar(null);
  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {
  }

  ngAfterContentInit(): void {
  }

  _saveClick(): void {
    // console.log(this.joinNodeForm.dto);
    let url = '/offline/datasource.ajax?action=offline_datasource_action&emethod=validateWorkflowAddJoinComponentForm'
    this.jsonPost(url, this.joinNodeForm.dto).then((r) => {
        this.processResult(r);
        if (r.success) {
          this.saveClick.emit(this.joinNodeForm);
        } else {
          this.errorItem = Item.processFieldsErr(r);
        }
      }
    );
  }

  // 点击保存之后处理逻辑
  public subscribeSaveClick(graph: any, $: any /*jquery*/, nodeid: string, addComponent: IDataFlowMainComponent, evt: JoinNodeForm): void {
    let old = graph.findById(nodeid);

    let nmodel = {'label': evt.nodeName, 'nodeMeta': this.joinNodeForm.dto};
    let jn = this.joinNodeForm.dto;
    // ▼▼▼ 将原先的节点全部删除
    jn.edgeIds.forEach((id) => {
      const edge = graph.findById(id);
      // console.log({'source': edge.getSource(), 'target': edge.getTarget()});
      graph.remove(edge);
    });
    jn.edgeIds = [];
    // ▲▲▲
    // console.log({'nodeid': nodeid, 'old': old, 'dependency': evt.dependenciseTabIds});
    // 将新的节点添加上
    evt.dependenciseTabIds.forEach((targetNodeid: any) => {
      let edgeid = addComponent.getUid();
      // console.log({'nodeid': nodeid, 'old': old, 'target': targetNodeid, 'newedgeid': edgeid});
      graph.addItem('edge', {
        'id': edgeid,
        source: nodeid,
        target: targetNodeid,
        style: {
          endArrow: true
        }
      });
      jn.addEdgeId(edgeid);
    });
    addComponent.joinNodeMap.set(nodeid, this.joinNodeForm.dto);

    // 更新label值
    graph.updateItem(old, nmodel);
    addComponent.closePanel();

  }


}

class JoinNodeForm extends BasicSidebarDTO {
  private _dependenciseTabIds: string[];

  // private dto: JoinNode;

  constructor(nodeMeta: NodeMeta, public dto: JoinNode,
              private joinComponent: WorkflowAddJoinComponent
  ) {
    super(nodeMeta);
    this._dependenciseTabIds = dto.dependencies.map((r) => r.value);
  }

  public get nodeName(): string {
    return this.dto.exportName;
  }

  public set nodeName(val: string) {
    this.dto.exportName = val;
  }

  // public get dependencyNodes(): Option[] {
  //   return this.dto.dependencies;
  // }
  //
  // public set dependencyNodes(val: Option[]) {
  //   this.dto.dependencies = val;
  // }

  public set joinSql(val: string) {
    this.dto.sql = val;
  }

  public get joinSql(): string {
    return this.dto.sql;
  }


  public get dependenciseTabIds(): string[] {
    return this._dependenciseTabIds;
  }

  public set dependenciseTabIds(select: string[]) {
    console.log({'select': select});
    this._dependenciseTabIds = select;

    {
      let opts: Option[] = [];
      select.map((id) => {
        // public value: string, public label: string
        let find = this.joinComponent.listOfOption.find((r) => r.value === id);
        if (find) {
          opts.push(new Option(id, find.label));
        } else {
          // throw new Error(`option id ${id} can not find relevant Option Object`);
        }
        console.log({'findid': id, 'finded': find});
      });

      this.dto.dependencies = opts;
    }
  }
}





