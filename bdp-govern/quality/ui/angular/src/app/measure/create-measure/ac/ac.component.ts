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
import {Component, OnInit, AfterViewChecked, ViewChild} from "@angular/core";
import {FormControl} from "@angular/forms";
import {FormsModule, Validator} from "@angular/forms";
import {ServiceService} from "../../../service/service.service";
import {TREE_ACTIONS, KEYS, IActionMapping, ITreeOptions, TreeComponent} from "angular-tree-component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {ToasterModule, ToasterService} from "angular2-toaster";
import * as $ from "jquery";
import {Router} from "@angular/router";
import {HttpClient, HttpParams} from "@angular/common/http";
import {ITreeNode} from "angular-tree-component/dist/defs/api";

class node {
  name: string;
  id: number;
  children: object[];
  isExpanded: boolean;
  cols: Col[];
  parent: string;
  location: string;
}

class Col {
  name: string;
  type: string;
  comment: string;
  selected: boolean;

  constructor(name: string, type: string, comment: string, selected: boolean) {
    this.name = name;
    this.type = type;
    this.comment = comment;
    this.selected = false;
  }

  getSelected() {
    return this.selected;
  }

  setSelected(selected) {
    this.selected = selected;
  }
}

@Component({
  selector: "app-ac",
  templateUrl: "./ac.component.html",
  providers: [ServiceService],
  styleUrls: ["./ac.component.css"]
})
export class AcComponent implements OnInit, AfterViewChecked {
  defaultValue: string;
  currentStep = 1;
  desc: string;
  selection = [];
  selectedAll = false;
  selectedAllTarget = false;
  selectionTarget = [];
  map = [];
  mappings = [];
  matches = [];
  dataAsset = "";
  rules = "";
  currentDB = "";
  currentTable = "";
  currentDBTarget = "";
  currentTableTarget = "";
  schemaCollection: Col[];
  schemaCollectionTarget: Col[];
  matchFunctions = ["=", "!=", ">", ">=", "<", "<="];
  data: any;
  currentDBTargetStr: string;
  currentDBstr: string;
  srcconfig = {
    where: "",
    timezone: "",
    num: 1,
    timetype: "day",
    needpath: false,
    path: ""
  };
  tgtconfig = {
    where: "",
    timezone: "",
    num: 1,
    timetype: "day",
    needpath: false,
    path: ""
  };
  srcdata = {
    database: "",
    table: "",
    selection: []
  };
  tgtdata = {
    database: "",
    table: "",
    selection: []
  };
  src_where: string;
  tgt_where: string;
  src_size: string;
  tgt_size: string;
  src_path: string;
  tgt_path: string;
  src_name: string;
  tgt_name: string;
  src_location: string;
  tgt_location: string;
  src_timezone: string;
  tgt_timezone: string;
  src_needpath: boolean;
  tgt_needpath: boolean;

  measureTypes = [
    "accuracy",
    "validity",
    "anomaly detection",
    "publish metrics"
  ];
  type = "accuracy";
  newMeasure = {
    name: "",
    "measure.type": "griffin",
    "dq.type": "ACCURACY",
    "process.type": "BATCH",
    owner: "",
    description: "",
    // "group":[],
    "data.sources": [
      {
        name: "source",
        connector:
          {
            name: "",
            type: "HIVE",
            version: "1.2",
            "data.unit": "",
            "data.time.zone": "",
            config: {
              database: "",
              "table.name": "",
              where: ""
            },
            predicates: [
              {
                type: "file.exist",
                config: {
                  "root.path": '',
                  path: ""
                }
              }
            ]
          }
      },
      {
        name: "target",
        connector:
          {
            name: "",
            type: "HIVE",
            version: "1.2",
            "data.unit": "",
            "data.time.zone": "",
            config: {
              database: "",
              "table.name": "",
              where: ""
            },
            predicates: [
              {
                type: "file.exist",
                config: {
                  "root.path": '',
                  path: ""
                }
              }
            ]
          }
      }
    ],

    "evaluate.rule": {
      rules: [
        {
          "dsl.type": "griffin-dsl",
          "dq.type": "ACCURACY",
          "out.dataframe.name": "accuracy",
          rule: ""
          // "details": {
          //   "source": "source",
          //   "target": "target",
          //   "miss.records": {
          //     "name": "miss.records",
          //     "persist.type": "record"
          //   },
          //   "accuracy": {
          //     "name": "accu",
          //     "persist.type": "metric"
          //   },
          //   "miss": "miss",
          //   "total": "total",
          //   "matched": "matched"
          // }
        }
      ]
    }
  };
  name: "";
  // evaluate.rule:any;
  // desc:'';
  // grp:'';
  owner = "test";
  createResult: any;

  private toasterService: ToasterService;
  public visible = false;
  public visibleAnimate = false;

  @ViewChild("srcTree")
  private tree: TreeComponent;

  @ViewChild("tgtTree")
  private tgtTree: TreeComponent;

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => (this.visible = false), 300);
    $("#save").removeAttr("disabled");
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains("modal")) {
      this.hide();
    }
  }

  addMapping(x, i) {
    this.mappings[i] = x;
  }

  toggleSelection(row) {
    row.selected = !row.selected;
    var idx = this.selection.indexOf(row.name);
    // is currently selected
    if (idx > -1) {
      this.selection.splice(idx, 1);
      this.selectedAll = false;
    } else {
      // is newly selected
      this.selection.push(row.name);
    }
    if (this.selection.length == 3) {
      this.selectedAll = true;
    } else {
      this.selectedAll = false;
    }
  }

  toggleSelectionTarget(row) {
    row.selected = !row.selected;
    var idx = this.selectionTarget.indexOf(row.name);
    // is currently selected
    if (idx > -1) {
      this.selectionTarget.splice(idx, 1);
      this.selectedAllTarget = false;
    } else {
      // is newly selected
      this.selectionTarget.push(row.name);
    }
    if (this.selectionTarget.length == 3) {
      this.selectedAllTarget = true;
    } else {
      this.selectedAllTarget = false;
    }
    let l = this.selectionTarget.length;
    for (let i = 0; i < l; i++) {
      this.matches[i] = "=";
      // this.mappings[i] = this.currentDB + '.' + this.currentTable + '.' + row.name;
    }
  }

  toggleAll() {
    this.selectedAll = !this.selectedAll;
    this.selection = [];
    for (var i = 0; i < this.schemaCollection.length; i++) {
      this.schemaCollection[i].selected = this.selectedAll;
      if (this.selectedAll) {
        this.selection.push(this.schemaCollection[i].name);
        this.matches[i] = "=";
      }
    }
  }

  toggleAllTarget() {
    this.selectedAllTarget = !this.selectedAllTarget;
    this.selectionTarget = [];
    for (var i = 0; i < this.schemaCollectionTarget.length; i++) {
      this.schemaCollectionTarget[i].selected = this.selectedAllTarget;
      if (this.selectedAllTarget) {
        this.selectionTarget.push(this.schemaCollectionTarget[i].name);
      }
    }
  }

  next(form) {
    if (this.formValidation(this.currentStep)) {
      this.currentStep++;
    } else {
      this.toasterService.pop(
        "error",
        "Error!",
        "Please select at least one attribute!"
      );
      return false;
    }
  }

  formValidation = function (step) {
    if (step == undefined) {
      step = this.currentStep;
    }
    if (step == 1) {
      return this.selection && this.selection.length > 0;
    } else if (step == 2) {
      return this.selectionTarget && this.selectionTarget.length > 0; //at least one target is selected
      // && !((this.currentTable.name == this.currentTableTarget.name)&&(this.currentDB.name == this.currentDBTarget.name));//target and source should be different
    } else if (step == 3) {
      return (
        this.selectionTarget &&
        this.selectionTarget.length == this.mappings.length &&
        this.mappings.indexOf("") == -1
      );
    } else if (step == 4) {
      return true;
    } else if (step == 5) {
    }
    return false;
  };

  prev(form) {
    this.currentStep--;
  }

  goTo(i) {
    this.currentStep = i;
  }

  submit(form) {
    // form.$setPristine();
    // this.finalgrp = [];
    if (!form.valid) {
      this.toasterService.pop(
        "error",
        "Error!",
        "please complete the form in this step before proceeding"
      );
      return false;
    }
    // for(let i=0;i<this.grp.length;i++){
    //   this.finalgrp.push(this.grp[i].value);
    // }
    // this.showgrp = this.finalgrp.join(",");
    var rule = "";
    this.newMeasure = {
      name: this.name,
      "measure.type": "griffin",
      "dq.type": "ACCURACY",
      "process.type": "BATCH",
      owner: this.owner,
      description: this.desc,
      // "group":this.finalgrp,
      "data.sources": [
        {
          name: "source",
          connector:
            {
              name: this.src_name,
              type: "HIVE",
              version: "1.2",
              "data.unit": this.src_size,
              "data.time.zone": this.src_timezone,
              config: {
                database: this.currentDB,
                "table.name": this.currentTable,
                where: this.src_where
              },
              predicates: [
                {
                  type: "file.exist",
                  config: {
                    "root.path": this.src_location,
                    path: this.src_path
                  }
                }
              ]
            }
        },
        {
          name: "target",
          connector:
            {
              name: this.tgt_name,
              type: "HIVE",
              version: "1.2",
              "data.unit": this.tgt_size,
              "data.time.zone": this.tgt_timezone,
              config: {
                database: this.currentDBTarget,
                "table.name": this.currentTableTarget,
                where: this.tgt_where
              },
              predicates: [
                {
                  type: "file.exist",
                  config: {
                    "root.path": this.tgt_location,
                    path: this.tgt_path
                  }
                }
              ]
            }
        }
      ],
      "evaluate.rule": {
        rules: [
          {
            "dsl.type": "griffin-dsl",
            "dq.type": "ACCURACY",
            "out.dataframe.name": "accuracy",
            rule: ""
            // "details": {
            //   "source": "source",
            //   "target": "target",
            //   "miss.records": {
            //     "name": "miss.records",
            //     "persist.type": "record"
            //   },
            //   "accuracy": {
            //     "name": "accu",
            //     "persist.type": "metric"
            //   },
            //   "miss": "miss",
            //   "total": "total",
            //   "matched": "matched"
            // }
          }
        ]
      }
    };
    if (this.src_size.indexOf("0") == 0) {
      this.deleteUnit(0);
    }
    if (this.tgt_size.indexOf("0") == 0) {
      this.deleteUnit(1);
    }
    if (!this.src_needpath || this.src_path == "") {
      this.deletePredicates(0);
    }
    if (!this.tgt_needpath || this.tgt_path == "") {
      this.deletePredicates(1);
    }
    var mappingRule = function (src, tgt, matches) {
      var rules;
      rules = "source." + src + matches + "target." + tgt;
      return rules;
    };
    var self = this;
    var rules = this.mappings.map(function (item, i) {
      return mappingRule(item, self.selectionTarget[i], self.matches[i]);
    });
    rule = rules.join(" AND ");
    this.rules = rule;
    this.newMeasure["evaluate.rule"].rules[0].rule = rule;
    this.visible = true;
    setTimeout(() => (this.visibleAnimate = true), 100);
  }

  deleteUnit(index) {
    delete this.newMeasure["data.sources"][index]["connector"]["data.unit"];
  }

  deletePredicates(index) {
    delete this.newMeasure["data.sources"][index]["connector"]["predicates"];
  }

  save() {
    var addModels = this.serviceService.config.uri.addModels;
    $("#save").attr("disabled", "true");
    this.http.post(addModels, this.newMeasure).subscribe(
      data => {
        this.createResult = data;
        this.hide();
        this.router.navigate(["/measures"]);
      },
      err => {
        let response = JSON.parse(err.error);
        if (response.code === '40901') {
          this.toasterService.pop("error", "Error!", "Measure name already exists!");
        } else {
          this.toasterService.pop("error", "Error!", "Error when creating measure");
        }
        console.log("Error when creating measure");
      }
    );
  }

  options: ITreeOptions = {
    displayField: "name",
    isExpandedField: "expanded",
    idField: "id",
    actionMapping: {
      mouse: {
        click: (tree, node, $event) => {
          if (node.hasChildren) {
            this.currentDB = node.data.name;
            this.currentDBstr = this.currentDB + ".";
            this.currentTable = "";
            this.selectedAll = false;
            this.schemaCollection = [];
            TREE_ACTIONS.TOGGLE_EXPANDED(tree, node, $event);
          } else if (node.data.cols !== undefined) {
            this.onTableNodeClick(node, this.setSrcTable.bind(this));
          }
        }
      }
    },
    animateExpand: true,
    animateSpeed: 30,
    animateAcceleration: 1.2
  };

  targetOptions: ITreeOptions = {
    displayField: "name",
    isExpandedField: "expanded",
    idField: "id",
    actionMapping: {
      mouse: {
        click: (tree, node, $event) => {
          if (node.hasChildren) {
            this.currentDBTarget = node.data.name;
            this.currentDBTargetStr = this.currentDBTarget + ".";
            this.currentTableTarget = "";
            this.selectedAllTarget = false;
            this.selectionTarget = [];
            this.schemaCollectionTarget = [];
            TREE_ACTIONS.TOGGLE_EXPANDED(tree, node, $event);
          } else if (node.data.cols !== undefined) {
            this.onTableNodeClick(node, this.setTargetTable.bind(this));
          }
        }
      }
    },
    animateExpand: true,
    animateSpeed: 30,
    animateAcceleration: 1.2
  };

  nodeList: object[];
  nodeListTarget: object[];

  constructor(
    toasterService: ToasterService,
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
    this.toasterService = toasterService;
  }

  onResize(event) {
    this.resizeWindow();
  }

  srcAttr(evt) {
    this.srcdata = evt;
    this.currentDB = evt.database;
    this.currentTable = evt.table;
    this.selection = evt.selection;
  }

  tgtAttr(evt) {
    this.tgtdata = evt;
    this.currentDBTarget = evt.database;
    this.currentTableTarget = evt.table;
    this.selectionTarget = evt.selection;
  }

  getSrc(evt) {
    this.srcconfig = evt;
    this.src_timezone = evt.timezone;
    this.src_where = evt.where;
    this.src_size = evt.num + evt.timetype;
    this.src_needpath = evt.needpath;
    this.src_path = evt.path;
  }

  getTgt(evt) {
    this.tgtconfig = evt;
    this.tgt_timezone = evt.timezone;
    this.tgt_where = evt.where;
    this.tgt_size = evt.num + evt.timetype;
    this.tgt_needpath = evt.needpath;
    this.tgt_path = evt.path;
  }

  resizeWindow() {
    var stepSelection = ".formStep[id=step-" + this.currentStep + "]";
    $(stepSelection).css({
      height: window.innerHeight - $(stepSelection).offset().top
    });
    $("fieldset").height(
      $(stepSelection).height() -
      $(stepSelection + ">.stepDesc").height() -
      $(".btn-container").height() -
      130
    );
    $(".y-scrollable").css({
      // 'max-height': $('fieldset').height()- $('.add-dataset').outerHeight()
      height: $("fieldset").height()
    });
  }

  ngOnInit() {
    let getTableNames = this.serviceService.config.uri.dbtablenames;

    this.http.get(getTableNames).subscribe((databases) => {
      this.nodeList = new Array();
      this.nodeListTarget = this.nodeList;  // share same model instead of copying(?)
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
        this.nodeList.push(dbNode);
      }
      if (i >= 10) {
        this.options.animateExpand = false;
        this.targetOptions.animateExpand = false;
      }
      this.updateTrees();
    });
    this.src_size = "1day";
    this.tgt_size = "1day";
    this.src_timezone = this.srcconfig.timezone;
    this.tgt_timezone = this.tgtconfig.timezone;
  }

  updateTrees() {
    if (this.currentStep == 1) {
      this.tree.treeModel.update();
    } else if (this.currentStep == 2) {
      this.tgtTree.treeModel.update();
    }
  }

  onTableNodeClick(treeNode: ITreeNode, callback) {
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
        callback(treeNode);
      })
    } else {
      callback(treeNode);
    }
  }

  setSrcTable(node: ITreeNode) {
    this.currentTable = node.data.name;
    this.currentDB = node.data.parent;
    this.schemaCollection = node.data.cols;
    this.src_location = node.data.location;
    this.src_name = "source" + new Date().getTime();
    this.selectedAll = false;
    this.selection = [];
    for (let row of this.schemaCollection) {
      row.selected = false;
    }
  }

  setTargetTable(node: ITreeNode) {
    this.currentTableTarget = node.data.name;
    this.currentDBTarget = node.data.parent;
    this.schemaCollectionTarget = node.data.cols;
    this.tgt_location = node.data.location;
    this.tgt_name = "target" + new Date().getTime();
    this.selectedAllTarget = false;
    this.selectionTarget = [];
    for (let row of this.schemaCollectionTarget) {
      row.selected = false;
    }
  }

  onKeyPress(event, query, tree) {
    if (event.keyCode == 13) {
      event.preventDefault();
      this.onSearch(query, tree);
    }
  }

  onSearch(query, tree) {
    tree.treeModel.filterNodes((node) => {
      let name;
      if (node.data.children !== undefined) {
        name = node.data.name + ".";
      } else {
        name = node.parent.data.name + "." + node.data.name;
      }
      return name.indexOf(query) >= 0;
    });
  }

  ngAfterViewChecked() {
    this.resizeWindow();
  }
}
