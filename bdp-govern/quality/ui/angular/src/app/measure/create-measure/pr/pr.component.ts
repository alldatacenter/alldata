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
import {Component, OnInit} from "@angular/core";
import {ServiceService} from "../../../service/service.service";
import {ToasterModule, ToasterService, ToasterContainerComponent} from "angular2-toaster";
import {HttpClient} from "@angular/common/http";
import {Router} from "@angular/router";
import {AfterViewChecked, ElementRef} from "@angular/core";
import {Col} from './step1/step1.component'
import * as $ from "jquery";

export class ProfilingStep1 {
  data: any;
  schemaCollection: Col[] = [];
  nodeList: object[] = [];
  selection: Col[];
  dropdownList: object = {};
  currentDB: string;
  currentDBStr: string;
  currentTable: string;
  srcname: string;
  srclocation: string;
  selectedItems: object = {};
}

export class ProfilingStep2 {
  selectedItems: object = {};
}

export class ProfilingStep3 {
  config: object = {
    where: "",
    timezone: "",
    num: 1,
    timetype: "day",
    needpath: false,
    path: ""
  };
  timezone: string = "";
  where: string = "";
  size: string = "1day";
  needpath: boolean = false;
  path: string;
}

export class ProfilingStep4 {
  prName: string = "";
  desc: string;
  type: string = "profiling";
  owner: string = "test";
  noderule: object[] = [];
}

@Component({
  selector: "app-pr",
  templateUrl: "./pr.component.html",
  providers: [ServiceService],
  styleUrls: ["./pr.component.css"]
})
export class PrComponent implements AfterViewChecked, OnInit {
  currentStep = 1;

  step1: ProfilingStep1;
  step2: ProfilingStep2;
  step3: ProfilingStep3;
  step4: ProfilingStep4;

  transrule = [];
  transenumrule = [];
  transnullrule = [];
  transregexrule = [];
  newMeasure = {};
  createResult: any;

  public visible = false;
  public visibleAnimate = false;
  private toasterService: ToasterService;

  next() {
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

  prev() {
    this.currentStep--;
  }

  updateStep1(body: ProfilingStep1) {
    this.step1 = body;
    this.next();
  }

  updateStep2(body: ProfilingStep2) {
    this.step2 = body;
    this.next();
  }

  updateStep3(body: ProfilingStep3) {
    this.step3 = body;
    this.next();
  }

  formValidation = (step) => {
    if (step == undefined) step = this.currentStep;
    if (step == 1) {
      return this.step1.selection && this.step1.selection.length > 0;
    } else if (step == 2) {
      let len = 0;
      let selectedlen = 0;
      for (let key in this.step2.selectedItems) {
        selectedlen++;
        len = this.step2.selectedItems[key].length;
        if (len == 0) {
          return false;
        }
      }
      return this.step1.selection.length == selectedlen;
    } else if (step == 3) {
      return true;
    } else if (step == 4) {
      return /^[a-zA-Z0-9_-]+$/.test(this.step4.prName);
    }
    return false;
  };

  constructor(
    private elementRef: ElementRef,
    toasterService: ToasterService,
    private http: HttpClient,
    private router: Router,
    public serviceService: ServiceService
  ) {
    this.toasterService = toasterService;
  }

  getGrouprule() {
    var selected = {name: ""};
    var value = "";
    var nullvalue = "";
    var nullname = "";
    var enmvalue = ""
    var regexvalue = "";
    var regexname = "";
    var grpname = "";

    for (let key in this.step2.selectedItems) {
      selected.name = key;
      let info = "";
      let otherinfo = "";
      for (let i = 0; i < this.step2.selectedItems[key].length; i++) {
        var originrule = this.step2.selectedItems[key][i].itemName;
        info = info + originrule + ",";

        if (originrule == "Enum Detection Top5 Count") {

          enmvalue = this.transferRule(originrule, selected);
          grpname = `${selected.name}_top5count`;
          this.transenumrule.push(enmvalue);
          this.pushEnmRule(enmvalue, grpname);

        } else if (originrule == "Null Count") {

          nullvalue = this.transferRule(originrule, selected);
          nullname = `${selected.name}_nullcount`;
          this.transnullrule.push(nullvalue);
          this.pushNullRule(nullvalue, nullname);

        } else if (originrule == "Empty Count") {

          nullvalue = this.transferRule(originrule, selected);
          nullname = `${selected.name}_emptycount`;
          this.transnullrule.push(nullvalue);
          this.pushNullRule(nullvalue, nullname);

        } else if (originrule == "Regular Expression Detection Count") {

          selected['regex'] = this.step2.selectedItems[key].regex;
          regexvalue = this.transferRule(originrule, selected);
          regexname = `${selected.name}_regexcount`;
          this.transregexrule.push(regexvalue);
          this.pushRegexRule(regexvalue, regexname);

        } else {

          otherinfo = otherinfo + originrule + ",";
          value = this.transferRule(originrule, selected);
          this.transrule.push(value);

        }
      }

      info = info.substring(0, info.lastIndexOf(","));
      otherinfo = otherinfo.substring(0, otherinfo.lastIndexOf(","));
      this.step4.noderule.push({
        name: key,
        infos: info
      });
    }
    if (this.transrule.length != 0) {
      this.getRule(this.transrule);
    }
  }

  getRule(trans) {
    var rule = "";
    for (let i of trans) {
      rule = rule + i + ",";
    }
    rule = rule.substring(0, rule.lastIndexOf(","));
    this.pushRule(rule);
  }

  pushEnmRule(rule, grpname) {
    this.newMeasure["evaluate.rule"].rules.push({
      "dsl.type": "griffin-dsl",
      "dq.type": "PROFILING",
      rule: rule,
      "out.dataframe.name": grpname,
      "out": [
        {
          "type": "metric",
          "name": grpname,
          "flatten": "array"
        }
      ]
    });
  }

  pushNullRule(rule, nullname) {
    this.newMeasure["evaluate.rule"].rules.push({
      "dsl.type": "griffin-dsl",
      "dq.type": "PROFILING",
      rule: rule,
      "out.dataframe.name": nullname
    });
  }

  pushRegexRule(rule, nullname) {
    this.newMeasure["evaluate.rule"].rules.push({
      "dsl.type": "griffin-dsl",
      "dq.type": "PROFILING",
      rule: rule,
      "out.dataframe.name": nullname
    });
  }

  pushRule(rule) {
    this.newMeasure["evaluate.rule"].rules.push({
      "dsl.type": "griffin-dsl",
      "dq.type": "PROFILING",
      rule: rule,
      name: "profiling"
    });
  }

  transferRule(rule, col) {
    switch (rule) {
      case "Total Count":
        return (
          `count(source.${col.name}) AS \`${col.name}_count\``
        );
      case "Distinct Count":
        return (
          `approx_count_distinct(source.${col.name}) AS \`${col.name}_distcount\``
        );
      case "Null Count":
        return (
          `count(source.${col.name}) AS \`${col.name}_nullcount\` WHERE source.${col.name} IS NULL`
        );
      case "Maximum":
        return (
          `max(source.${col.name}) AS \`${col.name}_max\``
        );
      case "Minimum":
        return (
          `min(source.${col.name}) AS \`${col.name}_min\``
        );
      case "Average":
        return (
          `avg(source.${col.name}) AS \`${col.name}_average\``
        );
      case "Empty Count":
        return (
          `count(source.${col.name}) AS \`${col.name}_emptycount\` WHERE source.${col.name} = ''`
        );
      case "Regular Expression Detection Count":
        return (
          `count(source.${col.name}) AS \`${col.name}_regexcount\` WHERE source.${col.name} RLIKE '^[0-9]{4}$'`
        );
      case "Enum Detection Top5 Count":
        return (
          `source.${col.name} AS ${col.name}, count(*) AS count GROUP BY source.${col.name} ORDER BY count DESC LIMIT 5`
        );
    }
  }

  public hide(): void {
    this.visibleAnimate = false;
    setTimeout(() => (this.visible = false), 300);
    this.transrule = [];
    this.transenumrule = [];
    this.transnullrule = [];
    this.transregexrule = [];
    this.step4.noderule = [];
    $("#save").removeAttr("disabled");
  }

  public onContainerClicked(event: MouseEvent): void {
    if ((<HTMLElement>event.target).classList.contains("modal")) {
      this.hide();
    }
  }

  submit(body: ProfilingStep4) {
    this.step4 = body;

    if (!this.formValidation(this.currentStep)) {
      this.toasterService.pop(
        "error",
        "Error!",
        "Please complete the form in this step before proceeding!"
      );
      return false;
    }

    this.newMeasure = {
      name: this.step4.prName,
      "measure.type": "griffin",
      "dq.type": "PROFILING",
      "rule.description": {
        details: this.step4.noderule
      },
      "process.type": "BATCH",
      owner: this.step4.owner,
      description: this.step4.desc,
      "data.sources": [
        {
          name: "source",
          connector:
            {
              name: this.step1.srcname,
              type: "HIVE",
              version: "1.2",
              "data.unit": this.step3.size,
              "data.time.zone": this.step3.timezone,
              config: {
                database: this.step1.currentDB,
                "table.name": this.step1.currentTable,
                where: this.step3.config['where']
              },
              predicates: [
                {
                  type: "file.exist",
                  config: {
                    "root.path": this.step1.srclocation,
                    path: this.step3.path
                  }
                }
              ]
            }
        }
      ],
      "evaluate.rule": {
        "out.dataframe.name": "profiling",
        rules: []
      }
    };

    this.getGrouprule();
    if (this.step3.size.indexOf("0") == 0) {
      delete this.newMeasure["data.sources"][0]["connector"]["data.unit"];
    }
    if (!this.step3.needpath || this.step3.path == "") {
      delete this.newMeasure["data.sources"][0]["connector"]["predicates"];
    }
    this.visible = true;
    setTimeout(() => (this.visibleAnimate = true), 100);
  }

  save() {
    let addModels = this.serviceService.config.uri.addModels;

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

  ngOnInit() {
    this.step1 = new ProfilingStep1();
    this.step2 = new ProfilingStep2();
    this.step3 = new ProfilingStep3();
    this.step4 = new ProfilingStep4();
  }

  ngAfterViewChecked() {
  }
}
