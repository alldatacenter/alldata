/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
import {Component, OnInit, Input, Output, EventEmitter, ViewChild} from "@angular/core";
import {ServiceService} from "../../../../service/service.service";
import {TREE_ACTIONS, ITreeOptions, TreeComponent} from "angular-tree-component";
import {HttpClient, HttpParams} from "@angular/common/http";
import {AfterViewChecked, ElementRef} from "@angular/core";
import {ProfilingStep1} from "../pr.component";
import {ITreeNode} from "angular-tree-component/dist/defs/api";

export class node {
  name: string;
  id: number;
  children: object[];
  isExpanded: boolean;
  cols: Col[];
  parent: string;
  location: string;
}

export class Rule {
  type: string;
}

export class Col {
  name: string;
  type: string;
  comment: string;
  selected: boolean;
  isNum: boolean;
  isExpanded: boolean;
  groupby: string;
  RE: string;
  rules: any;
  newRules: Rule[];

  constructor(name: string, type: string, comment: string, selected: boolean) {
    this.name = name;
    this.type = type;
    this.comment = comment;
    this.selected = false;
    this.isExpanded = false;
    this.groupby = "";
    this.rules = [];
    this.RE = "";
    this.newRules = [];

    var patt = new RegExp("int|double|float/i");
    if (patt.test(this.type)) {
      this.isNum = true;
    }
  }
}

@Component({
  selector: "app-pr-step-1",
  templateUrl: "./step1.component.html",
  providers: [ServiceService],
  styleUrls: ["./step1.component.css"]
})

export class PrStep1Component implements AfterViewChecked, OnInit {
  selectedAll = false;

  @Input() step1: ProfilingStep1;
  @Output() nextStep: EventEmitter<Object> = new EventEmitter<Object>();

  @ViewChild(TreeComponent)
  private tree: TreeComponent;

  options: ITreeOptions = {
    displayField: "name",
    isExpandedField: "expanded",
    idField: "id",
    actionMapping: {
      mouse: {
        click: (tree, node, $event) => {
          if (node.hasChildren) {
            this.step1.currentDB = node.data.name;
            this.step1.currentDBStr = this.step1.currentDB + ".";
            this.step1.currentTable = "";
            this.step1.schemaCollection = [];
            this.selectedAll = false;
            TREE_ACTIONS.TOGGLE_EXPANDED(tree, node, $event);
          } else if (node.data.cols !== undefined) {
            this.onTableNodeClick(node);
          }
        }
      }
    },
    animateExpand: true,
    animateSpeed: 30,
    animateAcceleration: 1.2
  };

  public visible = false;
  public visibleAnimate = false;

  toggleSelection(row) {
    row.selected = !row.selected;
    var idx = this.step1.selection.indexOf(row);
    // is currently selected
    if (idx > -1) {
      this.step1.selection.splice(idx, 1);
      this.selectedAll = false;
      for (let key in this.step1.selectedItems) {
        if (key === row.name) {
          delete this.step1.selectedItems[key];
        }
      }
    } else {
      // is newly selected
      this.step1.selection.push(row);
    }
    if (this.step1.selection.length == 3) {
      this.selectedAll = true;
    } else {
      this.selectedAll = false;
    }
    this.setDropdownList();
  }

  setDropdownList() {
    if (this.step1.selection) {
      for (let item of this.step1.selection) {
        if (item.isNum == true) {
          this.step1.dropdownList[item.name] = [
            {id: 1, itemName: "Null Count", category: "Simple Statistics"},
            {id: 2, itemName: "Distinct Count", category: "Simple Statistics"},
            {id: 3, itemName: "Total Count", category: "Summary Statistics"},
            {id: 4, itemName: "Maximum", category: "Summary Statistics"},
            {id: 5, itemName: "Minimum", category: "Summary Statistics"},
            {id: 6, itemName: "Average", category: "Summary Statistics"},
            // {"id":7,"itemName":"Median","category": "Summary Statistics"},
            // {"id":8,"itemName":"Rule Detection Count","category": "Advanced Statistics"},
            {id: 9, itemName: "Enum Detection Top5 Count", category: "Advanced Statistics"}
          ];
        } else {
          this.step1.dropdownList[item.name] = [
            {id: 1, itemName: "Null Count", category: "Simple Statistics"},
            {id: 2, itemName: "Distinct Count", category: "Simple Statistics"},
            {id: 3, itemName: "Total Count", category: "Summary Statistics"},
            // {"id":8,"itemName":"Rule Detection Count","category": "Advanced Statistics"},
            {id: 9, itemName: "Enum Detection Top5 Count", category: "Advanced Statistics"},
            {id: 10, itemName: "Regular Expression Detection Count", regex: "", category: "Advanced Statistics"},
            {id: 11, itemName: "Empty Count", category: "Simple Statistics"}
          ];
        }
      }
    }
  }

  toggleAll() {
    this.selectedAll = !this.selectedAll;
    this.step1.selection = [];

    for (var i = 0; i < this.step1.schemaCollection.length; i++) {
      this.step1.schemaCollection[i].selected = this.selectedAll;
      if (this.selectedAll) {
        this.step1.selection.push(this.step1.schemaCollection[i]);
      }
    }
    this.setDropdownList();
  }

  constructor(
    private elementRef: ElementRef,
    private http: HttpClient,
    public serviceService: ServiceService
  ) {
  }

  ngOnInit() {
    if (this.step1.nodeList.length !== 0) return;
    let getTableNames = this.serviceService.config.uri.dbtablenames;
    
    this.http.get(getTableNames).subscribe((databases) => {
      this.step1.nodeList = new Array();
      let i = 1;
      for (let dbName in databases) {
        if (!databases.hasOwnProperty(dbName)) {
          continue;
        }
        let dbNode = new node();
        dbNode.name = dbName;
        dbNode.id = i++;
        dbNode.isExpanded = false;
        dbNode.children = new Array();
        for (let tableName of databases[dbName]) {
          let tableNode = new node();
          tableNode.name = tableName;
          dbNode.children.push(tableNode);
          tableNode.isExpanded = true;
          tableNode.location = null;
          tableNode.parent = dbName;
          tableNode.cols = null;
        }
        this.step1.nodeList.push(dbNode);
      }
      if (i >= 10) {
        this.options.animateExpand = false;
      }
      this.tree.treeModel.update();
    });
  }

  onTableNodeClick(treeNode: ITreeNode) {
    let node: node = treeNode.data;
    if (node.cols == null) {
      let getTable = this.serviceService.config.uri.dbtable;
      let dbName = node.parent;
      let tableName = node.name;
      let params = new HttpParams({fromString: "db="+dbName+"&table="+tableName});
      this.http.get(getTable, {params: params}).subscribe(data => {
        node.location = data["sd"]["location"];
        node.cols = Array<Col>();
        for (let j = 0; j < data["sd"]["cols"].length; j++) {
          let new_col = new Col(
            data["sd"]["cols"][j].name,
            data["sd"]["cols"][j].type,
            data["sd"]["cols"][j].comment,
            false
          );
          node.cols.push(new_col);
        }
        this.setCurrentTable(treeNode);
      })
    } else {
      this.setCurrentTable(treeNode);
    }
  }

  onSearch(query) {
    this.tree.treeModel.filterNodes((node) => {
      let name;
      if (node.data.children !== undefined) {
        name = node.data.name + ".";
      } else {
        name = node.parent.data.name + "." + node.data.name;
      }
      return name.indexOf(query) >= 0;
    });
  }

  setCurrentTable(treeNode: ITreeNode) {
    let node: node = treeNode.data;
    this.step1.currentTable = node.name;
    this.step1.currentDB = treeNode.parent.data.name;
    this.step1.currentDBStr = this.step1.currentDB + ".";
    this.step1.schemaCollection = node.cols;
    this.step1.srcname = "source" + new Date().getTime();
    this.step1.srclocation = node.location;
    this.selectedAll = false;
    this.step1.selection = [];
    for (let row of this.step1.schemaCollection) {
      row.selected = false;
    }
  }

  nextChildStep() {
    this.nextStep.emit(this.step1);
  }

  ngAfterViewChecked() {
  }
}
