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
import Constants from '../utils/constants';
export default Ember.Component.extend({
  workspaceManager : Ember.inject.service('workspace-manager'),
  userInfo : Ember.inject.service('user-info'),
  xmlAppPath : null,
  appPath : null,
  type : 'wf',
  tabId : 0,
  store:Ember.inject.service(),
  //isProjectManagerEnabled : Constants.isProjectManagerEnabled,
  hasMultitabSupport : true,
  tabCounter : new Map(),
  tabs : Ember.A([]),
  currentIndex : Ember.computed('tabs.[]', function() {
    return this.get('tabs').length > 0 ? this.get('tabs').length - 1 : 0;
  }),
  tabsObserver : function(){
    this.get('workspaceManager').saveTabs(this.get('tabs'));
    this.tabManager();
  },
  initialize : function(){
    if (Constants.isProjectManagerEnabled) {
      this.set("isProjectManagerEnabled", "true");
    } else {
      this.set("isProjectManagerEnabled", "false");
    }
    this.get('tabCounter').set('wf', 0);
    this.get('tabCounter').set('coord', 0);
    this.get('tabCounter').set('bundle', 0);
    var tabsData = this.get('workspaceManager').restoreTabs();
    tabsData.promise.then(function(tabs){
      if(tabs){
        this.set('tabs', tabs);
      }
      this.get('tabs').forEach((tab)=>{
        this.get('tabCounter').set(tab.type, (this.get('tabCounter').get(tab.type)) + 1);
      }, this);
      Ember.addObserver(this, 'tabs.[]',this, this.tabsObserver);
      Ember.addObserver(this, 'tabs.@each.name',this, this.tabsObserver);
      Ember.addObserver(this, 'tabs.@each.filePath',this, this.tabsObserver);
    }.bind(this)).catch(function(data){
      Ember.addObserver(this, 'tabs.[]',this, this.tabsObserver);
      Ember.addObserver(this, 'tabs.@each.name',this, this.tabsObserver);
      Ember.addObserver(this, 'tabs.@each.filePath',this, this.tabsObserver);
    });
    Ember.getOwner(this).lookup('route:design').on('openNewTab', function(path, type, isImportedFromDesigner, configuration){
      if(type === 'COORDINATOR'){
        this.createNewTab('coord', path);
      }else if(type === 'BUNDLE'){
        this.createNewTab('bundle', path);
      }else{
        this.createNewTab('wf', path, isImportedFromDesigner, configuration);
      }
    }.bind(this));
  }.on('init'),
  elementsInserted : function(){
    this.tabManager();
    if(this.get('tabs') && this.get('tabs').length > 0){
      var lastActiveTabId = this.get('workspaceManager').getLastActiveTab();
      var activeTab = this.get('tabs').findBy('id', lastActiveTabId);
      if(!activeTab){
        activeTab = this.get('tabs').objectAt(this.get('tabs').length - 1);
      }
      if(activeTab.type === 'dashboard'){
        this.createOrshowDashboard();
      }else{
        this.$('.nav-tabs a[href="#' + activeTab.id + '"]').tab('show');
      }
    }else{
      this.createOrshowDashboard();
    }
  }.on('didInsertElement'),
  onDestroy : function(){
    Ember.removeObserver(this, 'tabs.[]',this, this.tabsObserver);
    Ember.removeObserver(this, 'tabs.@each.name',this, this.tabsObserver);
    Ember.removeObserver(this, 'tabs.@each.filePath',this, this.tabsObserver);
    this.get('tabs').clear();
  }.on('willDestroyElement'),
  tabManager(){
    Ember.run.later(()=>{
      this.$('.nav-tabs a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
      var id = this.$(e.target).attr('href').slice(1);
      this.get('workspaceManager').setLastActiveTab(id);
      var tab = this.get('tabs').findBy('id', id);
      if(tab.type === 'dashboard'){
        this.sendAction('showDashboard');
      } else if (tab.type === 'Projects') {
        this.createOrShowProjManager();
      }
      else{
        this.sendAction('hideDashboard');
      }
      }.bind(this));
    }, 1000);
  },
  setWFConfigProperties(tab ,isImportedFromDesigner, configuration){
    if(isImportedFromDesigner) {
      tab.isImportedFromDesigner = true;
      tab.configuration = configuration;
    }
    return tab;
  },
  createNewTab : function(type, path, isImportedFromDesigner, configuration){
    var existingTab = this.get('tabs').findBy("filePath", path);
    if(existingTab && path){
      existingTab = this.setWFConfigProperties(existingTab, isImportedFromDesigner, configuration);
      this.$('.nav-tabs a[href="#' + existingTab.id + '"]').tab("show");
      return;
    }
    var tab = {
      type : type,
      id : this.generateTabId(),
      name : this.getDisplayName(type)+this.getTabId(type)
    };
    if(path){
      tab.path = path;
    }
    tab = this.setWFConfigProperties(tab, isImportedFromDesigner, configuration);
    this.$('.nav-tabs li').removeClass('active');
    this.$('.tab-content .tab-pane').removeClass('active');
    this.get('tabs').pushObject(tab);
    this.set('isNew', true);
    this.sendAction("hideDashboard");
    this.tabManager();
  },
  getDisplayName(type){
    if(type === 'wf'){
      return "Workflow";
    }else if(type === 'coord'){
      return "Coordinator";
    }else{
      return "Bundle";
    }
  },
  getTabId(type){
    var count = this.get('tabCounter').get(type);
    this.get('tabCounter').set(type, ++count);
    return count;
  },
  createOrShowProjManager(){
    var projectsTab = this.get('tabs').findBy('type', 'Projects');
    if(projectsTab && projectsTab.type === 'Projects'){
      this.$('.nav-tabs a[href="#' + projectsTab.id + '"]').tab('show');
    }else{
      var tab = {
        type : 'Projects',
        id : this.generateTabId(),
        name : 'Projects'
      };
      this.$('.nav-tabs li').removeClass('active');
      this.$('.tab-content .tab-pane').removeClass('active');
      this.get('tabs').pushObject(tab);
      this.$('.nav-tabs a[href="#' + tab.id + '"]').tab('show');
    }
    this.sendAction('showProjManager');
    return;
  },
  createOrshowDashboard(){
    var dashboardTab = this.get('tabs').findBy('type', 'dashboard');
    if(dashboardTab && dashboardTab.type === 'dashboard'){
      this.$('.nav-tabs a[href="#' + dashboardTab.id + '"]').tab('show');
    }else{
      var tab = {
        type : 'dashboard',
        id : this.generateTabId(),
        name : 'Dashboard'
      };
      this.$('.nav-tabs li').removeClass('active');
      this.$('.tab-content .tab-pane').removeClass('active');
      this.get('tabs').pushObject(tab);
      this.$('.nav-tabs a[href="#' + tab.id + '"]').tab('show');
    }
    this.sendAction('showDashboard');
  },
  generateTabId(){
    return 'tab-'+ Math.ceil(Math.random() * 100000);
  },
  sortProp: ['updatedAt:desc'],
  recentFilesSorted: Ember.computed.sort("recentFiles", "sortProp"),
  projList: Ember.computed("recentFilesSorted", function() {
     return this.get("recentFilesSorted").slice(0, 10);
  }),
  actions : {
    deleteWorkflowJob(){
      this.sendAction("deleteWorkflowJob");
    },
    showTopRecentList(){
      var deferred = Ember.RSVP.defer();
      this.sendAction('getAllRecentWorks', deferred);
      deferred.promise.then((data)=>{
        this.set("recentFiles", data);
      }).catch((e)=>{
        console.error(e);
      })
    },
    editWorkflow(path, type){
      this.sendAction('editWorkflow', path, type);
      this.send('hideProjectManagerList');
    },
    showProjectManagerList(){
      var deferred = Ember.RSVP.defer();
      this.sendAction('getAllRecentWorks', deferred);
      deferred.promise.then((data)=>{
        this.set("recentFiles", data);
        this.set("isProjManagerVisible", true);
      }).catch((e)=>{
        console.error(e);
      })
    },
    hideProjectManagerList(){
      this.set("isProjManagerVisible", false);
    },
    register(tabInfo, context){
      var tab = this.get('tabs').findBy('id', tabInfo.id);
      Ember.set(tab, 'context', context);
    },
    show(type){
      this.sendAction('hideDashboard');
      if(this.get('hasMultitabSupport')){
        this.createNewTab(type);
      }else{
        var tab = this.get('tabs').findBy('type', type);
        if(!tab){
          this.createNewTab(type);
        }else{
          this.$('.nav-tabs a[href="#' + tab.id + '"]').tab('show');
        }
      }
    },
    showDashboard(){
      this.createOrshowDashboard();
    },
    showProjectManager(){
      this.createOrShowProjManager();
    },
    showWarning(index){
      this.set('showingWarning', false);
      var tab = this.get('tabs').objectAt(index);
      this.set('indexToClose', index);
      if(tab && tab.type ==='dashboard'){
        this.send('closeTab');
        return;
      }
      this.set('showingWarning', true);
      Ember.run.later(()=>{
        this.$('#ConfirmDialog').modal('show');
      });
    },
    closeTab(){
      this.set('showingWarning', false);
      var index = this.get('indexToClose');
      if(index < this.get('tabs').length - 1){
        var previousTab = this.get('tabs').objectAt(index + 1);
        this.$('.nav-tabs a[href="#'+ previousTab.id + '"]').tab('show');
      }
      this.get('workspaceManager').deleteWorkInProgress(this.get('tabs').objectAt(index).id);
      this.get('tabs').removeAt(index);
      Ember.run.later(()=>{
        var type = this.$('.nav-tabs').find('.active').attr('data-type');
        if(type === 'dashboard'){
          this.createOrshowDashboard();
        }
      }.bind(this));
    },
    openTab(type, path){
      if(this.get('hasMultitabSupport')){
        this.createNewTab(type, path);
      }else{
        var tab = this.get('tabs').findBy('type', type);
        if(!tab){
          this.createNewTab(type, path);
        }else{
          Ember.set(tab,'path', path);
          this.$('.nav-tabs a[href="#' + tab.id + '"]').tab('show');
        }
      }
    },
    changeTabName(tabInfo, name){
      var tab = this.get('tabs').findBy('id', tabInfo.id);
      Ember.set(tab, 'name', name);
    },
    changeFilePath(tabInfo, path){
      var tab = this.get('tabs').findBy('id', tabInfo.id);
      Ember.set(tab, 'filePath', path);
    },
    interceptShow(tab){
      if(tab.type === 'wf' && tab.context){
        CommonUtils.setTestContext(tab.context);
        tab.context.resize();
        tab.context.centerGraph();
      }else if(tab.type === 'dashboard'){
        this.sendAction('showDashboard');
      }
    },
    showAssetManager(value) {
      this.set('showingAssetManager', value);
    }
  }
});
