/*
*    Licensed to the Apache Software Foundation (ASF) under one or more
*    contributor license agreements.  See the NOTICE file distributed with
*    this work for additional information regarding copyright ownership.
*    The ASF licenses this file to You under the Apache License, Version 2.0
*    (the "License"); you may not use this file except in compliance with
*    the License.  You may obtain a copy of the License at
*
*        http://www.apache.org/licenses/LICENSE-2.0
*
*    Unless required by applicable law or agreed to in writing, software
*    distributed under the License is distributed on an "AS IS" BASIS,
*    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*    See the License for the specific language governing permissions and
*    limitations under the License.
*/

import Ember from 'ember';
import CommonUtils from "../utils/common-utils";
import CustomMappingHandler from "../domain/custom-mapping-handler";
import {MappingMixin,ConfigurationMapper,PrepareMapper} from "../domain/mapping-utils";
var ActionJobHandler=Ember.Object.extend(MappingMixin,{
  type:"actionJob",
  context : {},
  configurationMapper:ConfigurationMapper.create({}),
  prepareMapper:PrepareMapper.create({}),
  setContext(context){
    this.context = context;
  },
  getContext(){
    return this.context;
  },
  handle(nodeDomain,nodeObj,nodeName){
    var actionObj={};
    nodeObj[this.get("actionType")]=actionObj;
    if (this.get("nameSpace")){
      var schemaVersion=this.schemaVersions.actionVersions.get(this.get("actionType"));
      if (this.get("nameSpace")){
        var schema=this.get("nameSpace");
        if (schemaVersion){
          schema=CommonUtils.extractSchema(schema)+":"+schemaVersion;
        }
        nodeObj[this.get("actionType")]["_xmlns"]=schema;
      }
    }
    var customMapping = CustomMappingHandler.getMapping(nodeName);
    var mapping = this.mapping.copy();
    if(customMapping){
      Object.keys(customMapping).forEach((customProp)=>{
        var index = mapping.indexOf(mapping.findBy('xml',customMapping[customProp].prevSibling));
        mapping.insertAt(index+1, {xml:customProp,domain:customProp});
      }.bind(this));
    }
    this.handleMapping(nodeDomain,actionObj,mapping,nodeName);
  },
  /* jshint unused:vars */
  validate(nodeDomain){
    //overwrite in implmentations and return array of errors object.
  },
  handleImport(actionNode,json){
    this.handleImportMapping(actionNode,json,this.mapping);
    var customMapping = {};
    var x2js = new X2JS();
    var actionXml = x2js.json2xml({action:json});
    Object.keys(json).forEach((propKey)=>{
      if(!this.mapping.findBy('xml', propKey) && propKey !== '_xmlns'){
        var eltInXml = actionXml.getElementsByTagName(propKey)[0];
        var previousSiblingXml = eltInXml.previousSibling;
        var prevSibling = previousSiblingXml ? Object.keys(x2js.xml_str2json(new XMLSerializer().serializeToString(previousSiblingXml)))[0] : undefined;
        customMapping[propKey] = {
          prevSibling : prevSibling,
          mapping : {xml:propKey,domain:propKey}
        };
      }
    });
    if(!Ember.isEmpty(Object.keys(customMapping))){
      actionNode.customMapping = customMapping;
    }
  }
});
var JavaActionJobHandler=ActionJobHandler.extend({
  actionType:"java",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"main-class",domain:"mainClass",mandatory:true},
      {xml:"java-opts",domain:"javaOpts"},
      {xml:"java-opt",domain:"javaOpt",occurs:"many", domainProperty:"value"},
      {xml:"arg",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"},
      {xml:"capture-output",domain:"captureOutput",ignoreValue:true}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});
var PigActionJobHandler=ActionJobHandler.extend({
  actionType:"pig",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"script",domain:"script",mandatory:true},
      {xml:"param",domain:"param",domainProperty:"value",occurs:"many"},
      {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  }
});
var HiveActionJobHandler=ActionJobHandler.extend({
  actionType:"hive",
  nameSpace:"uri:oozie:hive-action:0.6",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"script",domain:"script"},
      {xml:"query",domain:"query"},
      {xml:"param",domain:"params",domainProperty:"value",occurs:"many"},
      {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },
  validate(nodeDomain){
    if (Ember.isBlank(nodeDomain.script) && Ember.isBlank(nodeDomain.query)){
      return [{message : "Either script or query to be set."}];
    }
  }
});
var Hive2ActionJobHandler=ActionJobHandler.extend({
  actionType:"hive2",
  nameSpace:"uri:oozie:hive2-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"jdbc-url",domain:"jdbc-url",mandatory:true},
      {xml:"password",domain:"password"},
      {xml:"script",domain:"script"},
      {xml:"query",domain:"query"},
      {xml:"param",domain:"params",domainProperty:"value",occurs:"many"},
      {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },
  validate(nodeDomain){
    if (Ember.isBlank(nodeDomain.script) && Ember.isBlank(nodeDomain.query)){
      return [{message : "Either script or query to be set."}];
    }
  }
});

var SqoopActionJobHandler=ActionJobHandler.extend({
  actionType:"sqoop",
  nameSpace:"uri:oozie:sqoop-action:0.4",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"command",domain:"command"},
      {xml:"arg",domain:"arg",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },
  validate(nodeDomain){
    if (Ember.isBlank(nodeDomain.command) && (!nodeDomain.arg || nodeDomain.arg.length < 1)){
      return [{message : "Either command or arguments have to be set."}];
    }
  }
});
var ShellActionJobHandler=ActionJobHandler.extend({
  actionType:"shell",
  nameSpace:"uri:oozie:shell-action:0.3",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"exec",domain:"exec",mandatory:true},
      {xml:"argument",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"env-var",domain:"envVar",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"},
      {xml:"capture-output",domain:"captureOutput",ignoreValue:true}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});
var SparkActionJobHandler=ActionJobHandler.extend({
  actionType:"spark",
  nameSpace:"uri:oozie:spark-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"master",domain:"master",mandatory:true,displayName:"Runs On"},
      {xml:"mode",domain:"mode"},
      {xml:"name",domain:"sparkName",mandatory:true},
      {xml:"class",domain:"class"},
      {xml:"jar",domain:"jar",mandatory:true,displayName:"Application"},
      {xml:"spark-opts",domain:"sparkOpts"},
      {xml:"arg",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },
  handleImport(actionNode,json){
    if(json.master === "yarn-client") {
           json.master = "yarn-cluster";
         }
    this._super(actionNode,json);
  }
});
var SubWFActionJobHandler=ActionJobHandler.extend({
  actionType:"sub-workflow",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"app-path",domain:"appPath",mandatory:true},
      {xml:"propagate-configuration",domain:"propagate-configuration", ignoreValue:true},
      {xml:"configuration",customHandler:this.configurationMapper}
    ];
  }
});
var DistCpJobHandler=ActionJobHandler.extend({
  actionType:"distcp",
  nameSpace:"uri:oozie:distcp-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.handlePrepare},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"java-opts",domain:"javaOpts"},
      {xml:"arg",domain:"args",occurs:"many",domainProperty:"value"},
    ];
  },

});

var SshActionJobHandler=ActionJobHandler.extend({
  actionType:"ssh",
  nameSpace:"uri:oozie:ssh-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"host",domain:"host"},
      {xml:"command",domain:"command"},
      {xml:"args",domain:"args",occurs:"many",domainProperty:"value"},
      {xml:"arg",domain:"arg",occurs:"many",domainProperty:"value"},
      {xml:"capture-output",domain:"captureOutput",ignoreValue:true}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});

var EmailActionJobHandler=ActionJobHandler.extend({
  actionType:"email",
  nameSpace:"uri:oozie:email-action:0.2",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"to",domain:"to",mandatory:true},
      {xml:"cc",domain:"cc"},
      {xml:"bcc",domain:"bcc"},
      {xml:"subject",domain:"subject",mandatory:true},
      {xml:"body",domain:"body",mandatory:true},
      {xml:"content_type",domain:"content_type"},
      {xml:"attachment",domain:"attachment"}

    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});


var MapRedActionJobHandler=ActionJobHandler.extend({
  actionType:"map-reduce",
  mapping:null,
  init(){
    this.mapping=[
      {xml:"job-tracker",domain:"jobTracker"},
      {xml:"name-node",domain:"nameNode"},
      {xml:"prepare",customHandler:this.prepareMapper},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"config-class", domain:"config-class"},
      {xml:"configuration",customHandler:this.configurationMapper},
      {xml:"file",domain:"files",occurs:"many",domainProperty:"value"},
      {xml:"archive",domain:"archives",occurs:"many",domainProperty:"value"}
    ];
  },

  handleImport(actionNode,json){
    this._super(actionNode,json);
  }
});

var CustomActionJobHandler=ActionJobHandler.extend({
  actionType:'',
  mapping:null,
  init(){
    this.mapping=[];
  },
  handleImport(actionNode,json){
    actionNode.set('domain', json);
  },
  handle(nodeDomain,nodeObj,nodeName){
    var customDomain = {};
    Object.keys(nodeDomain).forEach(key =>{
      if(key !== 'slaInfo' && key !== 'slaEnabled' && key!=='credentials'){
        customDomain[key] = nodeDomain[key];
      }
    });
    nodeObj[this.get("actionType")] = customDomain;
  }
});
var FSActionJobHandler=ActionJobHandler.extend({
  actionType:"fs",
  mapping:null,
  x2js : new X2JS({escapeMode:false}),
  init(){
    this.mapping=[
      {xml:"name-node",domain:"nameNode"},
      {xml:"job-xml",domain:"jobXml",occurs:"many",domainProperty:"value"},
      {xml:"configuration", customHandler:this.configurationMapper},
      {xml:"delete"},
      {xml:"mkdir"},
      {xml:"move"},
	    {xml:"chmod"},
      {xml:"touchz"},
      {xml:"chgrp"}
    ];
  },
  handle(nodeDomain,nodeObj,nodeName){
    this._super(nodeDomain,nodeObj,nodeName);
    if (!nodeDomain.fsOps){
      return;
    }
    var $root = Ember.$('<XMLDocument />');
    nodeDomain.fsOps.forEach(function(fsop){
      switch (fsop.type) {
        case 'delete':
          $root.append(Ember.$('<delete/>').attr("path",fsop.path));
          break;
        case 'mkdir':
          $root.append(Ember.$('<mkdir/>').attr("path",fsop.path));
          break;
        case 'move':
          $root.append(Ember.$('<move/>').attr("source",fsop.source).attr("target",fsop.target));
          break;
        case 'touchz':
          $root.append(Ember.$('<touchz/>').attr("path",fsop.path));
          break;
        case 'chmod':
          var permissions, ownerPerm = 0, groupPerm = 0, othersPerm = 0, dirFiles = fsop.dirfiles;

          if(fsop){
            if(fsop.oread){
              ownerPerm = 1;
            }
            if(fsop.owrite){
              ownerPerm = ownerPerm + 2;
            }
            if(fsop.oexecute){
              ownerPerm = ownerPerm + 4;
            }
            if(fsop.gread){
              groupPerm = 1;
            }
            if(fsop.gwrite){
              groupPerm = groupPerm + 2;
            }
            if(fsop.gexecute){
              groupPerm = groupPerm + 4;
            }
            if(fsop.rread){
              othersPerm = 1;
            }
            if(fsop.rwrite){
              othersPerm = othersPerm + 2;
            }
            if(fsop.rexecute){
              othersPerm = othersPerm + 4;
            }
          }
          permissions = ownerPerm+""+groupPerm+""+othersPerm;
          if(dirFiles === undefined){
            dirFiles = false;
          }
          var conf={"_path":fsop.path,"_permissions":permissions,"_dir-files":dirFiles};
          var chmodElem=Ember.$('<chmod/>').attr("path",fsop.path).attr("permissions",permissions).attr("dir-files",dirFiles);
          if (fsop.recursive){
            chmodElem.append('<recursive/>');
          }
          $root.append(chmodElem);
          break;
        case 'chgrp':
          var dirFiles = fsop.dirfiles;
          if(dirFiles === undefined){
            dirFiles = false;
          }
          var chGrpElem=Ember.$('<chgrp/>').attr("path",fsop.path).attr("group",fsop.group).attr("dir-files",dirFiles);
          if (fsop.recursive){
            chGrpElem.append('<recursive/>');
          }
          $root.append(chGrpElem);
          break;
        default:
      }
    });
    if (nodeObj.fs){
        nodeObj['fs']=this.x2js.json2xml_str(nodeObj.fs)+$root.html();
    }else{
        nodeObj['fs']=$root.html();
    }

    console.log("generated root=",$root)
  },
  handleImport(actionNode,json,xmlDoc){
    this._super(actionNode,json);
    var commandKeys=["delete","mkdir","move","chmod","touchz","chgrp"];
    var fsConfigs=xmlDoc[0].children[0].children;
    var fsOps=actionNode.domain.fsOps=[];
    for (var idx=0,len=fsConfigs.length;idx<len;idx++){
      var fsCofig=fsConfigs[idx];
      var tag=fsCofig.tagName;
      var fsConf={};
      fsOps.push(fsConf);
      fsConf.type=tag;
      var settings=fsConf;
      switch (tag) {
        case 'delete':
          settings.path=fsCofig.getAttribute("path");
          settings.skipTrash=fsCofig.getAttribute("skip-trash");
          break;
        case 'mkdir':
          settings.path=fsCofig.getAttribute("path");
          break;
        case 'touchz':
          settings.path=fsCofig.getAttribute("path");
          break;
        case 'move':
          settings.source=fsCofig.getAttribute("source");
          settings.target=fsCofig.getAttribute("target");
          break;
        case 'chmod':
          settings.path=fsCofig.getAttribute("path");
          settings.permissions=fsCofig.getAttribute("permissions");
          settings.dirfiles==fsCofig.getAttribute("dir-files");
          settings.recursive ==fsCofig.children.length===1 && fsCofig.children[0].tagName==="recursive";
          var perm = settings.permissions.toString();
          if(isNaN(perm)){
            var permList = {"-":0,"r":1,"w":2,"x":4}, permissionNumFormat = "", permTokenNum = 0, tempArr = [1,4,7];
            for(let p=0; p<tempArr.length; p++){
                var permToken = perm.slice(tempArr[p],tempArr[p]+3);
                for(let q=0; q<permToken.length; q++){
                  var tok = permList[permToken.slice(q,q+1)]
                  permTokenNum = permTokenNum + tok;
                }
                permissionNumFormat = permissionNumFormat +""+ permTokenNum;
                permTokenNum = 0;
            }
            perm = permissionNumFormat;
          }
          for(var i=0; i< perm.length; i++){
            var keyField;
            if(i===0){
              keyField = "o";
            }else if(i===1){
              keyField = "g";
            }else if(i===2){
              keyField = "r";
            }
            if(perm.slice(i,i+1) === "0"){
              settings[keyField+"read"] = 0;
              settings[keyField+"write"] = 0;
              settings[keyField+"execute"] = 0;
            }else if(perm.slice(i,i+1) === "1"){
              settings[keyField+"read"] = 1;
              settings[keyField+"write"] = 0;
              settings[keyField+"execute"] = 0;
            }else if(perm.slice(i,i+1) === "2"){
              settings[keyField+"read"] = 0;
              settings[keyField+"write"] = 2;
              settings[keyField+"execute"] = 0;
            }else if (perm.slice(i,i+1) === "3"){
              settings[keyField+"read"] = 1;
              settings[keyField+"write"] = 2;
              settings[keyField+"execute"] = 0;
            }else if (perm.slice(i,i+1) === "4"){
              settings[keyField+"read"] = 0;
              settings[keyField+"write"] = 0;
              settings[keyField+"execute"] = 4;
            }else if (perm.slice(i,i+1) === "5"){
              settings[keyField+"read"] = 1;
              settings[keyField+"write"] = 0;
              settings[keyField+"execute"] = 4;
            }else if (perm.slice(i,i+1) === "6"){
              settings[keyField+"read"] = 0;
              settings[keyField+"write"] = 2;
              settings[keyField+"execute"] = 4;
            }else if (perm.slice(i,i+1) === "7"){
              settings[keyField+"read"] = 1;
              settings[keyField+"write"] = 2;
              settings[keyField+"execute"] = 4;
            }
          }
          break;
        case 'chgrp':
          settings.path=fsCofig.getAttribute("path");
          settings.group=fsCofig.getAttribute("group");
          settings.dirfiles==fsCofig.getAttribute("dir-files");
          settings.recursive ==fsCofig.children.length===1 && fsCofig.children[0].tagName==="recursive";
          break;
        default:
          break;
      }
    }
  }
});
export{ActionJobHandler,JavaActionJobHandler,PigActionJobHandler,HiveActionJobHandler,SqoopActionJobHandler,ShellActionJobHandler, EmailActionJobHandler,SparkActionJobHandler,MapRedActionJobHandler, Hive2ActionJobHandler, SubWFActionJobHandler, DistCpJobHandler, SshActionJobHandler, FSActionJobHandler, CustomActionJobHandler};
