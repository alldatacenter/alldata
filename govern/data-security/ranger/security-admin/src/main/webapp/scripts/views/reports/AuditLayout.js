/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 
define(function(require) {
	'use strict';

	var Backbone 		= require('backbone');
	var App 			= require('App');

	var XAEnums 		= require('utils/XAEnums');
	var XAGlobals 		= require('utils/XAGlobals');
	var XAUtils			= require('utils/XAUtils');
	var XABackgrid		= require('views/common/XABackgrid');
	var XATableLayout	= require('views/common/XATableLayout');
	var localization	= require('utils/XALangSupport');
	var SessionMgr 		= require('mgrs/SessionMgr');
	var XAViewUtils		= require('utils/XAViewUtils');
	
	var VXAuthSession				= require('collections/VXAuthSessionList');
	var VXTrxLogList   				= require('collections/VXTrxLogList');
	var VXAssetList 				= require('collections/VXAssetList');
	var VXPolicyExportAuditList 	= require('collections/VXPolicyExportAuditList');
	var RangerServiceDefList 		= require('collections/RangerServiceDefList');
	var RangerService				= require('models/RangerService');
	var RangerServiceList			= require('collections/RangerServiceList');
        var VXUserList 				= require('collections/VXUserList');
	var AuditlayoutTmpl 			= require('hbs!tmpl/reports/AuditLayout_tmpl');
	var vOperationDiffDetail		= require('views/reports/OperationDiffDetail');
	var RangerPolicy 				= require('models/RangerPolicy');
	var RangerPolicyRO				= require('views/policies/RangerPolicyRO');
	var vPlugableServiceDiffDetail	= require('views/reports/PlugableServiceDiffDetail');
    var vLoginSessionDetail         = require('views/reports/LoginSessionDetail');
    var RangerZoneBase              = require('model_bases/RangerZoneBase');
    var AuditAccessLogDetail        = require('views/reports/AuditAccessLogDetailView');

	var moment = require('moment');
	require('bootstrap-datepicker');
	require('Backbone.BootstrapModal');
	require('visualsearch');
	require('Backgrid.ColumnManager');
	
	var AuditLayout = Backbone.Marionette.Layout.extend(
	/** @lends AuditLayout */
	{
		_viewName : 'AuditLayout',

		template : AuditlayoutTmpl,
		templateHelpers : function(){
			var repositoryList = _.map(XAEnums.AssetType,function(asset){return {id : asset.value, value :asset.label};});
			
			return {
				repositoryList	: repositoryList,
				currentDate 	: Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"),
				setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
			};
		},
		breadCrumbs : [],

		/** Layout sub regions */
		regions : {
			'rTableList'	: 'div[data-id="r_tableLists"]'
		},

		/** ui selector cache */
		ui : {
			tableList			: 'div[data-id="r_tableLists"]',
			refresh 			: '[data-id="refresh"]',
			resourceName 		: '[data-id = "resourceName"]',
			selectRepo	 		: '[data-id = "selectRepo"]',
			searchBtn 			: '[data-id="searchBtn"]',
			lastUpdateTimeLabel : '[data-id = "lastUpdateTimeLabel"]',
			startDate	 		: '[data-id = "startDate"]',
			endDate	 			: '[data-id = "endDate"]',
			tab 				: '.nav-tabs',
			refreshTable		: '[data-id="refreshTable"]',
			quickFilter			: '[data-id="quickFilter"]',
            visualSearch		: '.visual_search',
            'iconSearchInfo' : '[data-id="searchInfo"]',
            btnShowMore : '[data-id="showMore"]',
                        btnShowLess : '[data-id="showLess"]',
            iconqueryInfo : '[data-name="queryInfo"]',
            hidePopup : '[data-id="hide-popup"]',
            syncDetailes : '[data-id="syncDetailes"]',
            viewSession : '[data-name="viewSession"]',
            excludeServiceUser : '[data-id="excludeServiceUser"]',
            serviceUsersExclude:'[data-id="serviceUsersExclude"]',
            showPageDetail:'[data-id="showPageDetail"]',
            colManager: "[data-id='colManager']",
		},

		/** ui events hash */
		events : function() {
			var events = {};
			events['click ' + this.ui.refresh]         = 'onRefresh';
			events['click ' + this.ui.searchBtn]  	   = 'onSearch';
			events['click '+this.ui.tab+' a']		   = 'onTabChange';
                        events['click ' + this.ui.btnShowMore]  = 'onShowMore';
                        events['click ' + this.ui.btnShowLess]  = 'onShowLess';
            if(this.currentTab == '#bigData'){
                events['click ' + this.ui.hidePopup]  = 'onClickOutSide';
            }
            events['click '+this.ui.syncDetailes] = 'onSyncDetailes';
            events['click ' + this.ui.viewSession]  = 'onViewSession';
            events['click ' + this.ui.serviceUsersExclude]  = 'onExcludeServiceUsers';
            return events;
		},

		/**
		 * intialize a new AuditLayout Layout
		 * @constructs
		 */
		initialize : function(options) {
			console.log("initialized a AuditLayout Layout");

            _.extend(this, _.pick(options, 'accessAuditList','tab'));
            var that = this;
            $('.latestResponse').hide();
			this.bindEvents();
                        this.currentTab = '#'+this.tab.split('?')[0];
			var date = new Date().toString();
			this.timezone = date.replace(/^.*GMT.*\(/, "").replace(/\)$/, "");
			this.initializeServiceDefColl();
            if(_.isUndefined(App.vsHistory)){
                App.vsHistory = {'bigData':[], 'admin':[], 'loginSession':[], 'agent':[],'pluginStatus':[], 'userSync': []};
            }
            //Add url params to vsHistory
           	this.urlQueryParams = XAUtils.urlQueryParams();
            if(!_.isUndefined(this.urlQueryParams)) {
                App.vsHistory[that.tab.split('?')[0]] = [];
                var searchFregment = XAUtils.changeUrlToSearchQuery(decodeURIComponent(this.urlQueryParams));
                if (this.urlQueryParams && _.has(searchFregment, 'excludeServiceUser')) {
                    App.excludeServiceUser = searchFregment.excludeServiceUser == "true";
                }
                _.map (searchFregment, function(val, key) {
                    if (key !== "sortBy" && key !== "sortType" && key !== "sortKey" && key !== "excludeServiceUser") {
                        if (_.isArray(val)) {
                            _.map(val, function (v) {
                                App.vsHistory[that.tab.split('?')[0]].push(new Backbone.Model( {'category': key, 'value' : v}));
                            })
                        } else {
                            App.vsHistory[that.tab.split('?')[0]].push(new Backbone.Model( {'category': key, 'value' : val}));
                        }
                    }
                } )
            }
            //if url params are not present then set a default value in Audit assecc vsHistory
            if(_.isEmpty(App.vsHistory.bigData)){
                var startDateModel = new Backbone.Model({'category':'Start Date', value:Globalize.format(new Date(),"MM/dd/yyyy")});
                App.vsHistory['bigData'].push(startDateModel);
            }
        },

		/** all events binding here */
		bindEvents : function() {
            this.listenTo(this.accessAuditList, "sync", this.showTagsAttributes, this);
            this.listenTo(this.accessAuditList, "sync reset error", this.showPageDetail);
		},

                onClickOutSide: function(){
                        if($('.queryInfo') && this.currentTab == '#bigData'){
                                $('.queryInfo').popover('hide');
                        }
                },

		initializeServiceDefColl : function() {
			this.serviceDefList	= new RangerServiceDefList();
			this.serviceDefList.fetch({ 
				cache : false,
				async:false,
				data :{'pageSource':'Audit'}
			});
            this.serviceList = new RangerServiceList();
            this.serviceList.setPageSize(200)
            this.serviceList.fetch({
                cache : false,
                async:false,
                data :{'pageSource':'Audit'}
            });
		},
		/** on render callback */
		onRender : function() {
			var that = this;
			this.ui.tab.find('[href="'+this.currentTab+'"]').addClass('active');
			if(this.currentTab != '#bigData'){
				this.onTabChange();
			// 	this.ui.tab.find('li[class="active"]').removeClass();
			// 	this.ui.tab.find('[href="'+this.currentTab+'"]').parent().addClass('active');
			} else {
                var sortObj = {};
                if(Backbone.history.fragment.indexOf("?") !== -1) {
                    var sortFragment = Backbone.history.fragment.substring(Backbone.history.fragment.indexOf("?") + 1);
                    sortObj = _.pick(XAUtils.changeUrlToSearchQuery(decodeURIComponent(sortFragment)), 'sortType','sortBy');
                }
                if(!_.isEmpty(sortObj)) {
                    XAUtils.setSorting(this.accessAuditList, sortObj);
                }
				this.renderBigDataTable();
				this.addSearchForBigDataTab();
				XAUtils.resizeableColumn(this, 'resourceType');
			}
			this.showTagsAttributes();

		},
		modifyPluginStatusTableSubcolumns : function(){
			this.$el.find('[data-id="r_tableList"] table thead').prepend('<tr>\
					<th class="renderable pid"></th>\
					<th class="renderable ruser"></th>\
					<th class="renderable ruser"></th>\
					<th class="renderable ruser"></th>\
                                        <th class="renderable ruser"></th>\
					<th class="renderable ruser"></th>\
					<th class="renderable cip" colspan="3">Policy ( Time )<i class="fa-fw fa fa-info-circle m-l-sm" data-id ="policyTimeDetails"></th>\
                    <th class="renderable cip" colspan="3">Tag ( Time )<i class="fa-fw fa fa-info-circle m-l-sm" data-id ="tagPolicyTimeDetails"></th>\
			 	</tr>');
		},
        modifyUserSyncTableSubcolumns : function(){
            this.$el.find('[data-id="r_tableList"] table thead').prepend('<tr>\
                    <th class="renderable ruser"></th>\
                    <th class="renderable ruser"></th>\
                    <th class="renderable cip" colspan="2">Number Of New</th>\
                    <th class="renderable cip" colspan="2">Number Of Modified</th>\
                    <th class="renderable ruser"></th>\
                    <th class="renderable ruser"></th>\
            </tr>');
        },

		onTabChange : function(e){
                        var that = this, tab, sortObj = {};
			tab = !_.isUndefined(e) ? $(e.currentTarget).attr('href') : this.currentTab;
			this.$el.parents('body').find('.datepicker').remove();
            if (!_.isUndefined(e)) {
                    Backbone.history.navigate("!/reports/audit/"+ tab.slice(1), false)
            }
            if(Backbone.history.fragment.indexOf("?") !== -1) {
                var sortFragment = Backbone.history.fragment.substring(Backbone.history.fragment.indexOf("?") + 1);
                sortObj = _.pick(XAUtils.changeUrlToSearchQuery(decodeURIComponent(sortFragment)), 'sortType','sortBy');
                        }
            switch (tab) {
				case "#bigData":
					this.currentTab = '#bigData';
                    //Remove empty search values on tab changes for visual search.
                    App.vsHistory.bigData = XAUtils.removeEmptySearchValue(App.vsHistory.bigData);
					this.ui.visualSearch.show();
					this.ui.visualSearch.parents('.well').show();
					this.renderBigDataTable();
					this.addSearchForBigDataTab();
					XAUtils.resizeableColumn(this, 'resourceType');
                    this.listenTo(this.accessAuditList, "request", that.updateLastRefresh);
                    this.ui.iconSearchInfo.show();
                    this.showTagsAttributes();
                    this.ui.excludeServiceUser.show();
                    this.ui.colManager.show();
					break;
				case "#admin":
					this.currentTab = '#admin';
                    App.vsHistory.admin = XAUtils.removeEmptySearchValue(App.vsHistory.admin);
                    this.trxLogList = new VXTrxLogList();
                    if(!_.isEmpty(sortObj)) {
                        XAUtils.setSorting(this.trxLogList, sortObj);
                    }
                    this.renderAdminTable();
					if(_.isEmpty(App.vsHistory.admin) && _.isUndefined(App.sessionId)){
			     	    this.trxLogList.fetch({
							   cache : false
						});
					}
					this.addSearchForAdminTab();
					this.listenTo(this.trxLogList, "request", that.updateLastRefresh);
					this.listenTo(this.trxLogList, "sync reset", that.showPageDetail);
                    this.ui.iconSearchInfo.hide();
                    $('.popover').remove();
                    this.ui.excludeServiceUser.hide();
                    this.ui.colManager.hide();
					break;
				case "#loginSession":
					this.currentTab = '#loginSession';
                    App.vsHistory.loginSession = XAUtils.removeEmptySearchValue(App.vsHistory.loginSession);
					this.authSessionList = new VXAuthSession();
                    if(!_.isEmpty(sortObj)) {
                        XAUtils.setSorting(this.authSessionList, sortObj);
                    } else {
                        _.extend(this.authSessionList.queryParams,{ 'sortBy'  :  'id' });
					//Setting SortBy as id and sortType as desc = 1
                                        this.authSessionList.setSorting('id',1);
                    }
                                        this.renderLoginSessionTable();
                    if(_.isEmpty(App.vsHistory.loginSession)){
                        this.authSessionList.fetch({
                        	cache:false,
                        });
                    }
					this.addSearchForLoginSessionTab();
					this.listenTo(this.authSessionList, "request", that.updateLastRefresh);
					this.listenTo(this.authSessionList, "sync reset", that.showPageDetail);
                    this.ui.iconSearchInfo.hide();
                    $('.popover').remove();
                    this.ui.excludeServiceUser.hide();
                    this.ui.colManager.hide();
					break;
				case "#agent":
					this.currentTab = '#agent';
                    App.vsHistory.agent = XAUtils.removeEmptySearchValue(App.vsHistory.agent);
					this.policyExportAuditList = new VXPolicyExportAuditList();	
					var params = { priAcctId : 1 };
                    if(!_.isEmpty(sortObj)) {
                        XAUtils.setSorting(this.policyExportAuditList, sortObj);
                    } else {
                        _.extend(this.policyExportAuditList.queryParams,{ 'sortBy'  :  'createDate' });
					this.policyExportAuditList.setSorting('createDate',1);
                    }
                    that.renderAgentTable();
                    if(_.isEmpty(App.vsHistory.agent)){
                    this.policyExportAuditList.fetch({
	                    cache : false,
	                    data :params
                    });
                    }
					this.addSearchForAgentTab();
					this.listenTo(this.policyExportAuditList, "request", that.updateLastRefresh);
					 this.listenTo(this.policyExportAuditList, "sync reset", that.showPageDetail);
                    this.ui.iconSearchInfo.hide();
                    $('.popover').remove();
                    this.ui.excludeServiceUser.hide();
                    this.ui.colManager.hide();
					break;
				case "#pluginStatus":
					 this.currentTab = '#pluginStatus';
                     App.vsHistory.pluginStatus = XAUtils.removeEmptySearchValue(App.vsHistory.pluginStatus);
					 this.ui.visualSearch.show();
					 this.pluginInfoList = new VXPolicyExportAuditList();
                     if(!_.isEmpty(sortObj)){
                        XAUtils.setSorting(this.pluginInfoList, sortObj);
                     }
                     this.renderPluginInfoTable();
					 this.modifyPluginStatusTableSubcolumns();
                     XAUtils.customPopover(this.$el.find('[data-id ="policyTimeDetails"]'),'Policy (Time details)',localization.tt('msg.policyTimeDetails'),'left');
                     XAUtils.customPopover(this.$el.find('[data-id ="tagPolicyTimeDetails"]'),'Tag Policy (Time details)',localization.tt('msg.tagPolicyTimeDetails'),'left');
					 //To use existing collection
					 this.pluginInfoList.url = 'service/plugins/plugins/info';
					 this.pluginInfoList.modelAttrName = 'pluginInfoList';
                                         this.pluginInfoList.switchMode("client",  {fetch:false});
                     if(_.isEmpty(App.vsHistory.pluginStatus)){
                         this.pluginInfoList.fetch({cache : false});
                     }
					 this.addSearchForPluginStatusTab();
					 this.listenTo(this.pluginInfoList, "request", that.updateLastRefresh);
					 this.listenTo(this.pluginInfoList, "sync reset", that.showPageDetail);
					 this.ui.iconSearchInfo.hide();
                     $('.popover').remove();
                     this.ui.excludeServiceUser.hide();
                     this.ui.colManager.hide();
					 break;
                case "#userSync":
                     this.currentTab = '#userSync';
                     this.ui.visualSearch.show();
                     this.userSyncAuditList = new VXUserList();
                     if(!_.isEmpty(sortObj)) {
                        XAUtils.setSorting(this.userSyncAuditList, sortObj);
                     } else {
                        _.extend(this.userSyncAuditList.queryParams,{ 'sortBy'  :  'eventTime' });
                        this.userSyncAuditList.setSorting('eventTime',1);
                     }
                     this.renderUserSyncTable();
                     this.modifyUserSyncTableSubcolumns();
                     //To use existing collection
                     this.userSyncAuditList.url = 'service/assets/ugsyncAudits';
                     this.userSyncAuditList.modelAttrName = 'vxUgsyncAuditInfoList';
                     this.addSearchForUserSyncTab();
                     this.listenTo(this.userSyncAuditList, "request", that.updateLastRefresh);
                     this.listenTo(this.userSyncAuditList, "sync reset", that.showPageDetail);
                     this.ui.iconSearchInfo.hide();
                     this.ui.excludeServiceUser.hide();
                     this.ui.colManager.hide();
                     break;
			}
			var lastUpdateTime = Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt");
			that.ui.lastUpdateTimeLabel.html(lastUpdateTime);
		},
		addSearchForBigDataTab :function(){
            var that = this , query = '';
            var serviceListForRepoType =  this.serviceDefList.map(function(serviceDef){
                return {'label' : serviceDef.get('displayName').toUpperCase(), 'value' : serviceDef.get('id')}
            });
            var serviceNameForRepoName = this.serviceList.map(function(service){
                return {'label' : service.get('displayName'), 'value' : service.get('name')}
            })
            var serviceUser = [{'label' : 'True' , 'value' : true},{'label' : 'False' , 'value' : false}]
            var serverAttrName = [{text : 'Start Date', label :'startDate', urlLabel : 'startDate'},
                                    {text : 'End Date', label :'endDate', urlLabel : 'endDate'},
                                    {text : 'Application', label : 'agentId', urlLabel : 'application'},
                                    {text : 'User', label :'requestUser', 'addMultiple': true, urlLabel : 'user'},
                                    {text : 'Exclude User', label :'excludeUser', 'addMultiple': true, urlLabel : 'excludeUser'},
                                    {text : 'Resource Name',label :'resourcePath', urlLabel : 'resourceName'},
                                    {text : 'Service Name', label :'repoName', urlLabel : 'serviceName', 'optionsArr' : serviceNameForRepoName, 'multiple' : true},
                                    {text : 'Policy ID', label :'policyId', urlLabel : 'policyID'},
                                    {text : 'Service Type',label :'repoType','multiple' : true, 'optionsArr' : serviceListForRepoType, urlLabel : 'serviceType'},
                                    {text : 'Result', label :'accessResult', 'multiple' : true, 'optionsArr' : XAUtils.enumToSelectLabelValuePairs(XAEnums.AccessResult), urlLabel : 'result'},
                                    {text : 'Access Type', label :'accessType', urlLabel : 'accessType'},
                                    {text : 'Access Enforcer',label :'aclEnforcer', urlLabel : 'accessEnforcer'},
                                    {text : 'Client IP',label :'clientIP', urlLabel : 'clientIP'},
                                    {text : 'Tags',label :'tags', urlLabel : 'tags'},
                                    {text : 'Resource Type',label : 'resourceType', urlLabel : 'resourceType'},
                                    {text : 'Cluster Name',label : 'cluster', urlLabel : 'clusterName'},
                                    {text : 'Zone Name',label : 'zoneName', urlLabel : 'zoneName'},
                                    {text : localization.tt("lbl.agentHost"), label :"agentHost", urlLabel : 'agentHost'},
                                    {text : 'Audit ID', label : 'eventId', urlLabel : 'eventId'}
                                   //{text : localization.tt("lbl.permission"), label :'action', urlLabel : 'permission'}
                                ];
            var searchOpt = ['Resource Type','Start Date','End Date','Application','User','Service Name','Service Type','Resource Name','Access Type','Result','Access Enforcer',
            'Client IP','Tags','Cluster Name', 'Zone Name', 'Exclude User', localization.tt("lbl.agentHost"), 'Policy ID', 'Audit ID'];//, localization.tt("lbl.permission")];
                        this.clearVisualSearch(this.accessAuditList, serverAttrName);
                        this.searchInfoArr =[{text :'Access Enforcer', info :localization.tt('msg.accessEnforcer')},
                                            {text :'Access Type' 	, info :localization.tt('msg.accessTypeMsg')},
                                            {text :'Client IP' 		, info :localization.tt('msg.clientIP')},
                                            {text :'Cluster Name'	, info :localization.tt('h.clusterName')},
                                            {text :'Zone Name'      , info :localization.tt('h.zoneName')},
                                            {text :'End Date'       , info :localization.tt('h.endDate')},
                                            {text :'Resource Name' 	, info :localization.tt('msg.resourceName')},
                                            {text :'Resource Type'  , info :localization.tt('msg.resourceTypeMsg')},
                                            {text :'Result'			, info :localization.tt('msg.resultMsg')},
                                            {text :'Service Name' 	, info :localization.tt('h.serviceNameMsg')},
                                            {text :'Service Type' 	, info :localization.tt('h.serviceTypeMsg')},
                                            {text :'Start Date'     , info :localization.tt('h.startDate')},
                                            {text :'User' 			, info :localization.tt('h.userMsg')},
                                            {text :'Exclude User' 	, info :localization.tt('h.userMsg')},
                                            {text :'Application' 	, info :localization.tt('h.application')},
                                            {text :'Tags' 			, info :localization.tt('h.tagsMsg')},
                                            {text : localization.tt("lbl.permission"), info : localization.tt("lbl.permission")},];
                        //initilize info popover
                        XAUtils.searchInfoPopover(this.searchInfoArr , this.ui.iconSearchInfo , 'bottom');
                        //Set query(search filter values in query)
                        if(_.isEmpty(App.vsHistory.bigData)){
                            query = '"Start Date": "'+Globalize.format(new Date(),"MM/dd/yyyy")+'"';
                            App.vsHistory.bigData.push(new Backbone.Model({'category':'Start Date', value:Globalize.format(new Date(),"MM/dd/yyyy")}));
                        }else{
                            _.map(App.vsHistory.bigData, function(a) {
                                query += '"'+XAUtils.filterKeyForVSQuery(serverAttrName, a.get('category'))+'":"'+a.get('value')+'"';
                            });
                        }
			var pluginAttr = {
			      placeholder :localization.tt('h.searchForYourAccessAudit'),
			      container : this.ui.visualSearch,
			      query     : query,
			      supportMultipleItems: true,
			      type		: 'bigData',
			      callbacks :  { 
					valueMatches : function(facet, searchTerm, callback) {
						var auditList = [];
						_.each(XAEnums.ClassTypes, function(obj){
							if((obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_ASSET.value) 
									|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_RESOURCE.value) 
									|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_POLICY.value) 
									|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SERVICE.value)
									|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value) 
									|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.value))
								auditList.push({label :obj.label, value :obj.value+''});
						});
						
						switch (facet) {
							case 'Service Name':
                                                                var serviceNameVal = [];
                                                                that.serviceList.each(function(m){
                                    if(SessionMgr.isKeyAdmin() || SessionMgr.isKMSAuditor()){
                                        if(m.get('type') !== XAEnums.ServiceType.SERVICE_TAG.label){
                                            serviceNameVal.push({ 'label' : m.get('displayName'), 'value' : m.get('displayName')});
                                        }
                                    }else{
                                        if(m.get('type') !== XAEnums.ServiceType.SERVICE_TAG.label && m.get('type') !== XAEnums.ServiceType.Service_KMS.label){
                                            serviceNameVal.push({ 'label' : m.get('displayName'), 'value' : m.get('displayName')});
                                        }
                                    }
								});
								callback(serviceNameVal);
								break;
							case 'Service Type':
								var serviveDefs = [];
								that.serviceDefList.each(function(m){
                                    if(SessionMgr.isKeyAdmin() || SessionMgr.isKMSAuditor()){
                                        if(m.get('name').toUpperCase() != (XAEnums.ServiceType.SERVICE_TAG.label).toUpperCase()){
                                            serviveDefs.push({ 'label' : m.get('displayName').toUpperCase(), 'value' : m.get('displayName').toUpperCase() });
                                        }
                                    }else{
                                        if(m.get('name').toUpperCase() != (XAEnums.ServiceType.SERVICE_TAG.label).toUpperCase() && m.get('name') !== XAEnums.ServiceType.Service_KMS.label){
                                            serviveDefs.push({ 'label' : m.get('displayName').toUpperCase(), 'value' : m.get('displayName').toUpperCase() });
                                        }
                                    }
								});
								callback(serviveDefs);
								break;
							case 'Result':
				                callback(XAUtils.hackForVSLabelValuePairs(XAEnums.AccessResult));
				                break;  
							case 'Start Date' :
								var endDate, models = that.visualSearch.searchQuery.where({category:"End Date"});
								if(models.length > 0){
									var tmpmodel = models[0];
									endDate = tmpmodel.attributes.value;
								} 
								XAUtils.displayDatepicker(that.ui.visualSearch, facet, endDate, callback);
								break;
							case 'End Date' :
								var startDate, models = that.visualSearch.searchQuery.where({category:"Start Date"});
								if(models.length > 0){
									var tmpmodel = models[0];
									startDate = tmpmodel.attributes.value;
								} 
								XAUtils.displayDatepicker(that.ui.visualSearch, facet, startDate, callback);
								break;
							case 'Zone Name' :
								var rangerZoneList = new RangerZoneBase(), zoneList = [];
								rangerZoneList.fetch({
									cache : false,
									async : false,
									url: "service/public/v2/api/zone-headers",
								})
								if (rangerZoneList && rangerZoneList.attributes) {
									_.map(rangerZoneList.attributes,function(m){
										zoneList.push({'label' : m.name, 'value' : m.name});
									});
								}
								zoneList = _.sortBy(zoneList, 'label')
								callback(zoneList);
								break;
						}
					}
                }
			};
            this.accessAuditList.queryParams.excludeServiceUser = App.excludeServiceUser || false;
            this.ui.serviceUsersExclude.prop('checked', App.excludeServiceUser || false);
            this.visualSearch = XAUtils.addVisualSearch(searchOpt,serverAttrName, this.accessAuditList, pluginAttr);
            this.setEventsToFacets(this.visualSearch, App.vsHistory.bigData);
        },
		addSearchForAdminTab : function(){
			var that = this;
			var searchOpt = ["Audit Type", "User", "Actions", "Session ID", "Start Date", "End Date"];
                        var serverAttrName  = [{text : "Audit Type", label :"objectClassType",'multiple' : true, 'optionsArr' : XAUtils.enumToSelectLabelValuePairs(XAEnums.ClassTypes), urlLabel : 'auditType'},
                                    {text : "User", label :"owner", urlLabel : 'user'},
                                    {text :  "Session ID", label :"sessionId", urlLabel : 'sessionId'},
                                    {text : 'Start Date',label :'startDate', urlLabel : 'startDate'},
                                    {text : 'End Date',label :'endDate', urlLabel : 'endDate'},
                                    {text : "Actions", label :"action",'multiple' : true, 'optionsArr' : XAUtils.enumToSelectLabelValuePairs(XAGlobals.ActionType), urlLabel : 'actions'}
                                ];
			
                        var auditList = [],query = '', actionTypeList = [];
			_.each(XAEnums.ClassTypes, function(obj){
				if((obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_POLICY.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SERVICE.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_USER_PROFILE.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.value))
					auditList.push({label :obj.label, value :obj.label+''});
			});
			_.each(XAGlobals.ActionType, function(obj){
				if(obj.label){
					actionTypeList.push({label :obj.label, value :obj.label})
				}
			})
			if(!_.isUndefined(App.sessionId)){
                App.vsHistory.admin = [] ;
				query = '"Session ID": "'+App.sessionId+'"';
                App.vsHistory.admin.push(new Backbone.Model({'category':'Session ID', value:App.sessionId}));
				delete App.sessionId;
            }else{
                _.map(App.vsHistory.admin, function(a) {
                    query += '"'+XAUtils.filterKeyForVSQuery(serverAttrName, a.get('category'))+'":"'+a.get('value')+'"';
                });
			}
			var pluginAttr = {
				      placeholder :localization.tt('h.searchForYourAccessLog'),
				      container : this.ui.visualSearch,
				      query     : query,
                                      type 		: 'admin',
				      callbacks :  { 
				    	  valueMatches :function(facet, searchTerm, callback) {
								switch (facet) {
									case 'Audit Type':
										callback(_.sortBy(auditList, 'label'));
										break;
									case 'Actions':
										callback(actionTypeList);
										break;
									case 'Start Date' :
											var endDate, models = that.visualSearch.searchQuery.where({category:"End Date"});
											if(models.length > 0){
												var tmpmodel = models[0];
												endDate = tmpmodel.attributes.value;
											} 
											XAUtils.displayDatepicker(that.ui.visualSearch, facet, endDate, callback);
											break;
									case 'End Date' :
										var startDate, models = that.visualSearch.searchQuery.where({category:"Start Date"});
										if(models.length > 0){
											var tmpmodel = models[0];
											startDate = tmpmodel.attributes.value;
										} 
										XAUtils.displayDatepicker(that.ui.visualSearch, facet, startDate, callback);
										break;
								}     
			            	
							}
				      }
				};
			
			this.visualSearch = XAUtils.addVisualSearch(searchOpt,serverAttrName, this.trxLogList,pluginAttr);
                        this.setEventsToFacets(this.visualSearch, App.vsHistory.admin);
		},
		addSearchForLoginSessionTab : function(){
                        var that = this , query = '' ;
			var searchOpt = ["Session ID", "Login ID", "Result", "Login Type", "IP", "User Agent", "Start Date","End Date"];
                        var serverAttrName  = [{text : "Session ID", label :"id", urlLabel : 'sessionID'},
                                    {text : "Login ID", label :"loginId", urlLabel : 'loginID'},
                                    {text : "Result", label :"authStatus",'multiple' : true, 'optionsArr' : XAUtils.enumToSelectLabelValuePairs(XAEnums.AuthStatus), urlLabel : 'result'},
                                    {text : "Login Type", label :"authType",'multiple' : true, 'optionsArr' : XAUtils.enumToSelectLabelValuePairs(XAEnums.AuthType), urlLabel : 'loginType'},
                                    {text : "IP", label :"requestIP", urlLabel : 'requestIP'},
                                    {text :"User Agent", label :"requestUserAgent", urlLabel : 'userAgent'},
                                    {text : 'Start Date',label :'startDate', urlLabel : 'startDate'},
                                    {text : 'End Date',label :'endDate', urlLabel : 'endDate'}
                                ];
            _.map(App.vsHistory.loginSession, function(m) {
                query += '"'+XAUtils.filterKeyForVSQuery(serverAttrName, m.get('category'))+'":"'+m.get('value')+'"';
            });
			var pluginAttr = {
				      placeholder :localization.tt('h.searchForYourLoginSession'),
				      container : this.ui.visualSearch,
                                      query     : query,
                                      type 		: 'loginSession',
				      callbacks :  { 
				    	  valueMatches :function(facet, searchTerm, callback) {
								switch (facet) {
									case 'Result':
										var authStatusList = _.filter(XAEnums.AuthStatus, function(obj){
											if(obj.label != XAEnums.AuthStatus.AUTH_STATUS_UNKNOWN.label)
												return obj;
										});
										callback(XAUtils.hackForVSLabelValuePairs(authStatusList));
										break;
									case 'Login Type':
										var authTypeList = _.filter(XAEnums.AuthType, function(obj){
											if(obj.label != XAEnums.AuthType.AUTH_TYPE_UNKNOWN.label)
												return obj;
										});
										callback(XAUtils.hackForVSLabelValuePairs(authTypeList));
										break;	
									case 'Start Date' :
											var endDate, models = that.visualSearch.searchQuery.where({category:"End Date"});
											if(models.length > 0){
												var tmpmodel = models[0];
												endDate = tmpmodel.attributes.value;
											} 
											XAUtils.displayDatepicker(that.ui.visualSearch, facet, endDate, callback);
											break;
									case 'End Date' :
										var startDate, models = that.visualSearch.searchQuery.where({category:"Start Date"});
										if(models.length > 0){
											var tmpmodel = models[0];
											startDate = tmpmodel.attributes.value;
										} 
										XAUtils.displayDatepicker(that.ui.visualSearch, facet, startDate, callback);
										break;
                                                                        }
			            	
							}
				      }
				};
			this.visualSearch = XAUtils.addVisualSearch(searchOpt,serverAttrName, this.authSessionList,pluginAttr);
                        this.setEventsToFacets(this.visualSearch, App.vsHistory.loginSession);
		},
		addSearchForAgentTab : function(){
                        var that = this , query = '';
                        var serviceNameForRepoName = this.serviceList.map(function(service){
                            return {'label' : service.get('displayName'), 'value' : service.get('name')}
                        })
                        var searchOpt = ["Service Name", "Plugin ID", "Plugin IP", "Http Response Code", "Start Date","End Date", "Cluster Name"];
                        var serverAttrName  = [{text : "Plugin ID", label :"agentId", urlLabel : 'pluginID'},
                                                {text : "Plugin IP", label :"clientIP", urlLabel : 'pluginIP'},
                                                {text : "Service Name", label :"repositoryName", urlLabel : 'serviceName','optionsArr' : serviceNameForRepoName, 'multiple' : true},
                                                {text : "Http Response Code", label :"httpRetCode", urlLabel : 'httpResponseCode'},
                                                {text : "Export Date", label :"createDate", urlLabel : 'exportDate'},
                                                {text : 'Start Date',label :'startDate', urlLabel : 'startDate'},
                                                {text : 'End Date',label :'endDate', urlLabel : 'endDate'},
                                                {text : 'Cluster Name',label :'cluster', urlLabel : 'clusterName'}];
                        _.map(App.vsHistory.agent, function(m) {
                            query += '"'+XAUtils.filterKeyForVSQuery(serverAttrName, m.get('category'))+'":"'+m.get('value')+'"';
                        });
			var pluginAttr = {
				      placeholder :localization.tt('h.searchForYourAgent'),
				      container : this.ui.visualSearch,
                                      query     : query,
                                      type		: 'agent',
				      callbacks :  { 
				    	  valueMatches :function(facet, searchTerm, callback) {
								switch (facet) {
								    case 'Service Name':
                                                                                callback(that.serviceList.map(function(model){
                                            return { 'label' : model.get('displayName'), 'value' : model.get('displayName')}
                                        }));
										break;
                                                                    case 'Start Date' :
											var endDate, models = that.visualSearch.searchQuery.where({category:"End Date"});
											if(models.length > 0){
												var tmpmodel = models[0];
												endDate = tmpmodel.attributes.value;
											} 
											XAUtils.displayDatepicker(that.ui.visualSearch, facet, endDate, callback);
											break;
									case 'End Date' :
										var startDate, models = that.visualSearch.searchQuery.where({category:"Start Date"});
										if(models.length > 0){
											var tmpmodel = models[0];
											startDate = tmpmodel.attributes.value;
										} 
										XAUtils.displayDatepicker(that.ui.visualSearch, facet, startDate, callback);
										break;
								}
			            	
							}
				      }
				};
			this.visualSearch = XAUtils.addVisualSearch(searchOpt,serverAttrName, this.policyExportAuditList, pluginAttr);
                        this.setEventsToFacets(this.visualSearch, App.vsHistory.agent);
		},
		addSearchForPluginStatusTab : function(){
                        var that = this , query = '';
                        var serviceNameForRepoName = this.serviceList.map(function(service){
                            return {'label' : service.get('displayName'), 'value' : service.get('name')}
                        });
                        var serviceListForRepoType =  this.serviceDefList.map(function(serviceDef){
                            return {'label' : serviceDef.get('displayName').toUpperCase(), 'value' : serviceDef.get('name')}
                        });
                        var searchOpt = [localization.tt("lbl.serviceName"), localization.tt("lbl.serviceType"),localization.tt("lbl.applicationType"),
                             localization.tt("lbl.agentIp"), localization.tt("lbl.hostName"), localization.tt("lbl.clusterName")];
                        var serverAttrName  = [{text : localization.tt("lbl.serviceName"), label :"serviceName", urlLabel : 'serviceName', 'optionsArr' : serviceNameForRepoName, 'multiple' : true},
                                                {text : localization.tt("lbl.applicationType"), label :"pluginAppType", urlLabel : 'applicationType'},
                                                {text : localization.tt("lbl.agentIp"), label :"pluginIpAddress", urlLabel : 'agentIp'},
                                                {text : localization.tt("lbl.hostName"), label :"pluginHostName", urlLabel : 'hostName'},
                                                {text : localization.tt("lbl.serviceType"), label :"serviceType", urlLabel : 'serviceType', 'optionsArr' : serviceListForRepoType, 'multiple' : true},
                                                {text : localization.tt("lbl.clusterName"),label :'clusterName', urlLabel : 'clusterName'}];
                        _.map(App.vsHistory.pluginStatus, function(m) {
                            query += '"'+XAUtils.filterKeyForVSQuery(serverAttrName, m.get('category'))+'":"'+m.get('value')+'"';
                        });
			var pluginAttr = {
					placeholder    : localization.tt('msg.searchForPluginStatus'),
					container         : this.ui.visualSearch,
                                        query             : query,
                                        type		   : 'pluginStatus',
					callbacks :  { 
				    	  valueMatches :function(facet, searchTerm, callback) {
								switch (facet) {
								    case 'Service Name':
                                                                                callback(that.serviceList.map(function(model){
                                            return { 'label' : model.get('displayName'), 'value' : model.get('displayName')}}
                                        ));
										break;

                                    case 'Service Type':
                                        var serviveType = [];
                                        that.serviceDefList.each(function(m){
                                                serviveType.push({ 'label' : m.get('displayName').toUpperCase() , 'value' : m.get('displayName').toUpperCase() });
                                        });
                                        callback(serviveType);
                                        break;
                                }
                        }
                    }
			}
			this.visualSearch = XAUtils.addVisualSearch(searchOpt, serverAttrName, this.pluginInfoList, pluginAttr);
                        this.setEventsToFacets(this.visualSearch, App.vsHistory.pluginStatus);
                },
                //preserve, remove and change search filter values in App.vsHistory
                setEventsToFacets : function(vs, value){
                        vs.searchQuery.bind('add', function(model){
                                value.push(model) ;
                        });
                        vs.searchQuery.bind('remove', function(model){
                                value = _.filter(value, function(m){
                                    return m.get('category') != model.get('category') || m.get('value') != model.get('value');
                                });
                                App.vsHistory[vs.options.type] = value;
                        });
                        vs.searchQuery.bind('change', function(model){
                                _.each(App.vsHistory[vs.options.type],function(m){
                                    if(m.cid == model.cid) {
                                        m.attributes.value = model.get('value');
                                    } else if (model._previousAttributes) {
                                        if (model._previousAttributes.category === m.attributes.category && model._previousAttributes.value === m.attributes.value ) {
                                            m.attributes.value = model.get('value');
                                        }
                                    }
                                })
                        });
		},
        addSearchForUserSyncTab : function(){
            var that = this , query = '';
            var searchOpt = [localization.tt("lbl.userName"), localization.tt("lbl.syncSource"), localization.tt("lbl.startDate"), localization.tt("lbl.endDate")];
            var serverAttrName  = [{text : localization.tt("lbl.userName"), label :"userName", urlLabel : 'userName'},
                                    {text : localization.tt("lbl.syncSource"), label :"syncSource", urlLabel : 'syncSource'},
                                    {text : 'Start Date',label :'startDate', urlLabel : 'startDate'},
                                    {text : 'End Date',label :'endDate', urlLabel : 'endDate'}];
            if(_.isEmpty(App.vsHistory.userSync)){
                query = '"Start Date": "'+Globalize.format(new Date(),"MM/dd/yyyy")+'"';
                App.vsHistory.userSync.push(new Backbone.Model({'category':'Start Date', value:Globalize.format(new Date(),"MM/dd/yyyy")}));
            }else{
                _.map(App.vsHistory.userSync, function(a) {
                    query += '"'+XAUtils.filterKeyForVSQuery(serverAttrName, a.get('category'))+'":"'+a.get('value')+'"';
                });
            }
            var pluginAttr = {
                placeholder    : localization.tt('msg.searchForUserSync'),
                container         : this.ui.visualSearch,
                query             : query,
                type		   : 'userSync',
                callbacks :  {
                    valueMatches :function(facet, searchTerm, callback) {
                        switch (facet) {
                            case 'Sync Source':
                                    callback( _.map(XAEnums.UserSyncSource, function(obj){ return obj.label; }) );
                                    break;
                            case 'Start Date' :
                                    var endDate, models = that.visualSearch.searchQuery.where({category:"End Date"});
                                    if(models.length > 0){
                                        var tmpmodel = models[0];
                                        endDate = tmpmodel.attributes.value;
                                    }
                                    XAUtils.displayDatepicker(that.ui.visualSearch, facet, endDate, callback);
                                    break;
                            case 'End Date' :
                                    var startDate, models = that.visualSearch.searchQuery.where({category:"Start Date"});
                                    if(models.length > 0){
                                        var tmpmodel = models[0];
                                        startDate = tmpmodel.attributes.value;
                                    }
                                    XAUtils.displayDatepicker(that.ui.visualSearch, facet, startDate, callback);
                                    break;
                        }
                    }
                }
            }
            this.visualSearch = XAUtils.addVisualSearch(searchOpt, serverAttrName, this.userSyncAuditList, pluginAttr);
            this.setEventsToFacets(this.visualSearch, App.vsHistory.userSync);
        },
		renderAdminTable : function(){
			var that = this , self = this;
			
			var TableRow = Backgrid.Row.extend({
				events: {
					'click' : 'onClick'
				},
				initialize : function(){
					var that = this;
					var args = Array.prototype.slice.apply(arguments);
					Backgrid.Row.prototype.initialize.apply(this, args);
					this.listenTo(this.model, 'model:highlightBackgridRow', function(){
						that.$el.addClass("alert");
						$("html, body").animate({scrollTop: that.$el.position().top},"linear");
						setTimeout(function () {
							that.$el.removeClass("alert");
						}, 1500);
					}, this);
				},
				onClick: function (e) {
					var self = this;
					if($(e.target).is('.fa-fw fa fa-edit,.fa-fw fa fa-trash,a,code'))
						return;
					this.$el.parent('tbody').find('tr').removeClass('tr-active');
					this.$el.toggleClass('tr-active');
					
					XAUtils.blockUI();
					var action = self.model.get('action');
					var date = new Date(self.model.get('createDate')).toString();
					var timezone = date.replace(/^.*GMT.*\(/, "").replace(/\)$/, "");
					var objectCreatedDate =  Globalize.format(new Date(self.model.get('createDate')), "MM/dd/yyyy hh:mm:ss tt")+' '+timezone;
					
					var fullTrxLogListForTrxId = new VXTrxLogList();
					fullTrxLogListForTrxId.getFullTrxLogListForTrxId(this.model.get('transactionId'),{
                                                cache : false,
                                                error : function(response , error){
                                                        if (response && response.status === 419 ) {
                                                                XAUtils.defaultErrorHandler(error , response);
                                                        } else {
                                                                XAUtils.showErrorMsg(response.responseJSON.msgDesc);
                                                        }
                                                }
					}).done(function(coll,mm){
						XAUtils.blockUI('unblock');
						fullTrxLogListForTrxId = new VXTrxLogList(coll.vXTrxLogs);
						//diff view to support new plugable service model
						var hasAction = ["EXPORT JSON", "EXPORT EXCEL", "EXPORT CSV", "IMPORT START", "IMPORT END"];
						if(self.model.get('objectClassType') == XAEnums.ClassTypes.CLASS_TYPE_RANGER_POLICY.value && ($.inArray(action,hasAction) >= 0)){
							var view  = that.getExportImportTemplate(fullTrxLogListForTrxId);
						} else if(self.model.get('objectClassType') == XAEnums.ClassTypes.CLASS_TYPE_RANGER_POLICY.value){
							var view = new vPlugableServiceDiffDetail({
								collection : fullTrxLogListForTrxId,
								classType : self.model.get('objectClassType'),
								objectName : self.model.get('objectName'),
								objectId   : self.model.get('objectId'),
								objectCreatedDate : objectCreatedDate,
								userName :self.model.get('owner'),
								action : action,
								repoName : self.model.get('parentObjectName'),
							});
						} else if (self.model.get('objectClassType') == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE.value){
							var view = new vOperationDiffDetail({
								collection : fullTrxLogListForTrxId,
								classType : self.model.get('objectClassType'),
								objectName : self.model.get('objectName'),
								objectId   : self.model.get('objectId'),
								objectCreatedDate : objectCreatedDate,
								userName :self.model.get('owner'),
								action : action
							});
						} else {
							var view = new vOperationDiffDetail({
								collection : fullTrxLogListForTrxId,
								classType : self.model.get('objectClassType'),
								objectName : self.model.get('objectName'),
								objectId   : self.model.get('objectId'),
								objectCreatedDate : objectCreatedDate,
								userName :self.model.get('owner'),
								action : action
							});
						}
						var modal = new Backbone.BootstrapModal({
							animate : true, 
							content		: view,
							title: localization.tt("h.operationDiff")+' : '+action,
							okText :localization.tt("lbl.ok"),
							allowCancel : true,
							escape : true,
							focusOk : false
						}).open();
						//modal.$el.addClass('modal-diff').attr('tabindex',-1);
						modal.$el.find('.cancel').hide();
					});
				}
			});
			this.rTableList.show(new XATableLayout({
				columns: this.getAdminTableColumns(),
				collection:this.trxLogList,
				includeFilter : false,
				gridOpts : {
					row : TableRow,
					header : XABackgrid,
					emptyText : 'No service found!!'
				}
                        }));
            //Trigger backgrid sort event
            XAUtils.backgridSort(this.trxLogList);
		},
		getExportImportTemplate : function(trxLogs){
			var log = trxLogs.models[0],fields = '', values = '', infoJson = {};
			if(log.get("action")=="IMPORT START"){
				return '<center>'+(localization.tt('msg.importingFiles'))+'</center>';
			}
			if(!_.isUndefined(log.get('previousValue')) && !_.isEmpty(log.get('previousValue'))){
				infoJson = JSON.parse(log.get('previousValue'))
				if(_.isUndefined(infoJson) || _.isEmpty(infoJson)){
					return '<h5> No User details found !!</h5>';
				} 
			}
			_.each(infoJson, function(val, key){
				fields +='<li class="change-row">'+key+'</li>';
				if(_.isEmpty(val) && !_.isNumber(val)){
					values +='<li class="change-row">--</li>';
				}else{
					if(key == 'Export time'){
						val = Globalize.format(new Date(val), "MM/dd/yyyy hh:mm:ss tt");
					}
					values +='<li class="change-row">'+val+'</li>'
				}
			});
			return  '<div class="diff-content">\
				<h5> Details :</h5>\
				<div class="diff">\
					<div class="diff-left">\
						<ol class="attr">'+fields+'\
						</ol>\
					</div>\
					<div class="diff-right">\
						<ol class="list-unstyled data">'+values+'\
						</ol>\
					</div>\
				</div>\
			</div>';
		},
		getAdminTableColumns : function(){
			var auditList = [];
			_.each(XAEnums.ClassTypes, function(obj){
				if((obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_ASSET.value) 
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_RESOURCE.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_POLICY.value) 
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SERVICE.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value)
						|| (obj.value == XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.value))
				auditList.push({text :obj.label, id :obj.value});
			});
			var cols = {
				operation : {
					label : localization.tt("lbl.operation"),
					cell: "html",
					click : false,
					drag : false,
					sortable:false,
					editable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							rawValue = model.get('objectClassType');
							var action = model.get('action'), name = _.escape(model.get('objectName')),
								label = XAUtils.enumValueToLabel(XAEnums.ClassTypes,rawValue), html = '',
								hasAction = ["EXPORT JSON", "EXPORT EXCEL", "EXPORT CSV", "IMPORT START", "IMPORT END"];
							if($.inArray(action,hasAction)>=0){
								if(action == "EXPORT JSON" || action == "EXPORT EXCEL" || action == "EXPORT CSV")
                                                                        return 'Exported policies';
								else
                                                                        return action;
							} else{	
								 if(rawValue == XAEnums.ClassTypes.CLASS_TYPE_XA_ASSET.value || rawValue == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SERVICE.value)
								 	 html = 	'Service '+action+'d '+'<b>'+name+'</b>';
								 else if((rawValue == XAEnums.ClassTypes.CLASS_TYPE_XA_RESOURCE.value || rawValue == XAEnums.ClassTypes.CLASS_TYPE_RANGER_POLICY.value))
									 html = 	'Policy '+action+'d '+'<b>'+name+'</b>';
								 else if(rawValue == XAEnums.ClassTypes.CLASS_TYPE_XA_USER.value)
									 html = 	'User '+action+'d '+'<b>'+name+'</b>';
								 else if(rawValue == XAEnums.ClassTypes.CLASS_TYPE_XA_GROUP.value)
									 html = 	'Group '+action+'d '+'<b>'+name+'</b>';
								 else if(rawValue  == XAEnums.ClassTypes.CLASS_TYPE_USER_PROFILE.value)
									 html = 	'User profile '+action+'d '+'<b>'+name+'</b>';
								 else if(rawValue  == XAEnums.ClassTypes.CLASS_TYPE_PASSWORD_CHANGE.value)
									 html = 	'User profile '+action+'d '+'<b>'+name+'</b>';
								 else if(rawValue  == XAEnums.ClassTypes.CLASS_TYPE_RANGER_SECURITY_ZONE.value)
									 html =     'Security Zone '+action+'d '+'<b>'+name+'</b>';
                                                                else if(rawValue  == XAEnums.ClassTypes.CLASS_TYPE_RANGER_ROLE.value)
                                                                         html =     'Role '+action+'d '+'<b>'+name+'</b>';
								 return html;
						    }
						}
					})
				},
				objectClassType : {
					label : localization.tt("lbl.auditType"),
					cell: "string",
					click : false,
					drag : false,
					sortable:false,
					editable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							var action = model.get('action');
							return XAUtils.enumValueToLabel(XAEnums.ClassTypes,rawValue);
						}
					})
				},
				owner : {
					label : localization.tt("lbl.user"),
					cell: "string",
					click : false,
					drag : false,
					sortable:false,
					editable:false
				},
				createDate : {
					label : localization.tt("lbl.date") + '  ( '+this.timezone+' )',
					cell: "String",
					editable:false,
                    sortType: 'toggle',
                    sortable : true,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							return Globalize.format(new Date(model.get('createDate')),  "MM/dd/yyyy hh:mm:ss tt");
						}
					})
				},
				action : {
					label : localization.tt("lbl.actions"),
					cell: "html",
					click : false,
					drag : false,
					sortable:false,
					editable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue) {
							var html = '';
							if(rawValue =='create'){
								html = 	'<label class="badge badge-success capitalize">'+rawValue+'</label>';
							} else if(rawValue == 'update'){
								html = 	'<label class="badge badge-yellow capitalize">'+rawValue+'</label>';
							}else if(rawValue == 'delete'){
								html = 	'<label class="badge badge-danger capitalize">'+rawValue+'</label>';
							}else if(rawValue =='IMPORT START'){
								html = 	'<label class="badge badge-info capitalize">'+rawValue+'</label>';
							}else if(rawValue =='IMPORT END'){
								html = 	'<label class="badge badge-info capitalize">'+rawValue+'</label>';
							}							else {
								rawValue = rawValue.toLowerCase() 
								html = 	'<label class="badge badge-secondary capitalize ">'+rawValue+'</label>';
							}
							return html;
						}
					})
				},
                sessionId : {
                    label : localization.tt("lbl.sessionId"),
                    cell: "html",
                    click : false,
                    drag : false,
                    sortable:false,
                    editable:false,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            var sessionId = model.get('sessionId');
                            if (!_.isUndefined(sessionId)) {
                                return '<a href="javascript:void(0);" data-name ="viewSession" data-id="'+sessionId+'" title="'+sessionId+'">'+sessionId+'</a>';
                            } else {
                                return '';
                            }
                        }
                    })
                }
			};
            if (this.trxLogList.queryParams.sortBy && !_.isEmpty(this.trxLogList.queryParams.sortBy)) {
                XAUtils.backgridSortType(this.trxLogList, cols);
            }
			return this.trxLogList.constructor.getTableCols(cols, this.trxLogList);
		},

		renderBigDataTable : function(){
			var that = this , self = this;
			
			var TableRow = Backgrid.Row.extend({
				events: {
					'click' : 'onClick'
				},
				initialize : function(){
					var that = this;
					var args = Array.prototype.slice.apply(arguments);
					Backgrid.Row.prototype.initialize.apply(this, args);
				},
				onClick: function (e) {
                    var self = this ;
                    if($(e.target).hasClass('policyIdColumn') || $(e.target).closest('td').hasClass("policyIdColumn")) {
                        if($(e.target).is('.fa-external-link'))
                            return;
                        if(this.model.get('repoType')){
                                    var repoType =  this.model.get('repoType');
                                }
                                                var policyId = this.model.get('policyId');
                                                if(policyId == -1){
                                                        return;
                                                }
                                var eventTime = this.model.get('eventTime');

                                var policyVersion = this.model.get('policyVersion');

                                var application = this.model.get('agentId');

                                                var policy = new RangerPolicy({
                                                        id: policyId,
                                                        version:policyVersion
                                                });
                                                var policyVersionList = policy.fetchVersions();
                                                var view = new RangerPolicyRO({
                                                        policy: policy,
                                                        policyVersionList : policyVersionList,
                                    serviceDefList: that.serviceDefList,
                                    eventTime : eventTime,
                                    repoType : repoType
                                                });
                                                var modal = new Backbone.BootstrapModal({
                                                        animate : true,
                                                        content		: view,
                                                        title: localization.tt("h.policyDetails"),
                                                        okText :localization.tt("lbl.ok"),
                                    allowCancel : true,
                                                        escape : true,
                                                        focusOk : false
                                                }).open();
                                modal.$el.find('.cancel').hide();
                                                var policyVerEl = modal.$el.find('.modal-footer').prepend('<div class="policyVer pull-left"></div>').find('.policyVer');
                                                policyVerEl.append('<i id="preVer" class="fa-fw fa fa-chevron-left '+ ((policy.get('version')>1) ? 'active' : '') +'"></i><text>Version '+ policy.get('version') +'</text>').find('#preVer').click(function(e){
                                                        view.previousVer(e);
                                                });
                                                    var policyVerIndexAt = policyVersionList.indexOf(policy.get('version'));
                                                policyVerEl.append('<i id="nextVer" class="fa-fw fa fa-chevron-right '+ (!_.isUndefined(policyVersionList[++policyVerIndexAt])? 'active' : '')+'"></i>').find('#nextVer').click(function(e){
                                                        view.nextVer(e);
                                                });
                    } else {
                        if($(e.target).hasClass('tagsColumn') || $(e.target).closest('td').hasClass("tagsColumn")) {
                                return;
                        }
                        var view = new AuditAccessLogDetail({
                            auditaccessDetail : this.model.attributes,
                        });
                        var url =Backbone.history.location.href.substring(0, Backbone.history.location.href.indexOf('#!'))+'#!/reports/audit/eventlog/'+this.model.get('eventId');
                        var eventUrl = '<a href="'+url+'" target="_blank" title="Show log details in next tab"> <i class="fa-fw fa fa-external-link pull-right"></i> </a>';
                        var modal = new Backbone.BootstrapModal({
                            animate : true,
                            content     : view,
                            title: localization.tt("lbl.auditAccessDetail") + eventUrl,
                            okText :localization.tt("lbl.ok"),
                            allowCancel : true,
                            escape : true,
                        }).open();
                        modal.$el.find('.cancel').hide();
                    }
				}
			});

			this.ui.tableList.removeClass("clickable");
			this.rTableList.show(new XATableLayout({
				columns: this.getColumns(),
				collection: this.accessAuditList,
				includeFilter : false,
				backgridColumnManager: true,
				gridOpts : {
					row: TableRow,
					header : XABackgrid,
					emptyText : 'No Access Audit found!'
				},
				columnManagerOpts: {
                    opts: {
                        // initialColumnsVisible: null,
                        saveState: true,
                        loadStateOnInit: true,
                    },
                    visibilityControlOpts: {
                        buttonTemplate: _.template("<span class='btn btn-sm'>&nbspColumns&nbsp<i class='fa fa-caret-down'></i></span>")
                    },
                    el: this.ui.colManager
                },
			}));
            XAUtils.backgridSort(this.accessAuditList);
		},

		getColumns : function(){
			var that = this;
			var cols = {
					policyId : {
                                                // cell : "html",
                                                cell: Backgrid.HtmlCell.extend({
                                                        className : 'policyIdColumn'
                                                }),
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
								var serviceDef = that.serviceDefList.findWhere({'id' : model.get('repoType')}),
								href = 'javascript:void(0)';
								if(rawValue == -1 || _.isUndefined(serviceDef)){
									return '--';
								}
								return '<a href="'+href+'" title="'+rawValue+'">'+rawValue+'</a>';
							}
						}),
						label	: localization.tt("lbl.policyId"),
						editable: false,
						sortable : true,
						sortType: 'toggle',
					},
                    policyVersion: {
                        label : localization.tt("lbl.policyVersion"),
                        cell: "html",
                        click: false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function (rawValue, model) {
                                rawValue = _.escape(rawValue);
                                    if(_.isUndefined(rawValue) || _.isEmpty(rawValue)) {
                                        return '--'
                                    } else {
                                        return '<span title="'+rawValue+'">'+rawValue+'</span>';
                                    }
                                }
                            }),
                          drag: false,
                          sortable: false,
                          editable: false,
                    },
					eventTime : {
						label : 'Event Time',
						cell: "String",
						editable:false,
						sortable : true,
						direction: "descending",
                        sortType: 'toggle',
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
								return Globalize.format(new Date(rawValue),  "MM/dd/yyyy hh:mm:ss tt");
							}
						})
					},
				    agentId : {
					label : 'Application',
					cell: "String",
					click : false,
					drag : false,
                                        editable:false,
                    sortable : false,
				},
					requestUser : {
						label : 'User',
						cell: "String",
						click : false,
						drag : false,
                                                editable:false,
                        sortable : true,
                        sortType: 'toggle',
					},
					repoName : {
						label : 'Service (Name / Type)',
						cell: "html",
						click : false,
						drag : false,
						sortable:false,
						editable:false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
                                                                return '<div title="'+model.get('repoDisplayName')+'">'+_.escape(model.get('repoDisplayName'))+'</div>\
                                    <div title="'+model.get('serviceTypeDisplayName')+'" style="border-top: 1px solid #ddd;">'+_.escape(model.get('serviceTypeDisplayName'))+'</div>';
							}
						})
					},
					resourceType: {
						label : 'Resource (Name / Type)',
						cell: "html",
						formatter: _.extend({},Backgrid.CellFormatter.prototype,{
							 fromRaw: function(rawValue,model) {
							     return XAViewUtils.resourceTypeFormatter(rawValue, model);
							 }
						}),
						drag: false,
						sortable: true,
						sortType: 'toggle',
						editable: false,
					},
					accessType : {
						label : localization.tt("lbl.accessType"),
						cell: "String",
						drag : false,
						sortable:true,
						sortType: 'toggle',
						editable:false
					},
					action : {
						label : localization.tt("lbl.permission"),
						cell: "html",
						drag : false,
						editable:false,
						sortable : true,
						sortType: 'toggle',
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw : function (rawValue, model) {
								rawValue = _.escape(rawValue);
								if(_.isUndefined(rawValue) || _.isEmpty(rawValue)){
									return '<center>--</center>';
								}else{
									return '<span  class="badge badge-info" title="'+rawValue+'">'+rawValue+'</span>';
								}
							}
						})
					},
					accessResult : {
						label : localization.tt("lbl.result"),
						cell: "html",
						click : false,
						drag : false,
						editable:false,
                        sortable : false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue) {
								var label = '', html = '';
								_.each(_.toArray(XAEnums.AccessResult),function(m){
									if(parseInt(rawValue) == m.value){
										label=  m.label;
										if(m.value == XAEnums.AccessResult.ACCESS_RESULT_ALLOWED.value){
											html = 	'<label class="badge badge-success">'+label+'</label>';
										} else {
											html = 	'<label class="badge badge-danger">'+label+'</label>';
										} 
									}	
								});
								return html;
							}
						})
					},
					aclEnforcer : {
						label :localization.tt("lbl.aclEnforcer"),
						cell: "String",
						drag : false,
						sortable:true,
						sortType: 'toggle',
						editable:false
					},
					agentHost : {
						cell : 'html',
						label : localization.tt("lbl.agentHost"),
						editable : false,
						sortable : false,
						formatter : _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw : function (rawValue, model) {
								rawValue = _.escape(rawValue);
								if(_.isUndefined(rawValue)){
									return '<center>--</center>';
								}else{
									return '<span title="'+rawValue+'">'+rawValue+'</span>';
								}
							}
						}),
					},
					clientIP : {
						label : 'Client IP',
						cell: "string",
						drag : false,
						sortable:true,
						sortType: 'toggle',
						editable:false
					},
                    clusterName : {
                        label : localization.tt("lbl.clusterName"),
                        cell: 'html',
                        click : false,
                        drag : false,
                        sortable:false,
                        editable:false,
                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                fromRaw: function (rawValue, model) {
                                    rawValue = _.escape(rawValue);
                                    if (_.isUndefined(rawValue) || _.isEmpty(rawValue)) {
                                        '--'
                                    } else {
                                        return '<span title="'+rawValue+'">'+rawValue+'</span>';
                                    }
                                }
                        }),
					},
                    zoneName: {
						label : localization.tt("lbl.zoneName"),
						cell: "html",
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                            fromRaw: function (rawValue, model) {
                                rawValue = _.escape(rawValue);
                                if (_.isUndefined(rawValue) || _.isEmpty(rawValue)) {
                                    '--'
                                } else {
                                    return '<span class="badge badge-dark" title="'+rawValue+'">'+rawValue+'</span>';
                                }
                            }
                        }),
						drag: false,
						sortable: true,
						sortType: 'toggle',
						editable: false,
					},

                                        eventCount : {
                                                label : 'Event Count',
						cell: "string",
						click : false,
						drag : false,
						sortable:false,
						editable:false
					},
                                        tags : {
                                                label : 'Tags',
                                                cell: Backgrid.HtmlCell.extend({
                                                        className : 'tagsColumn'
                                                }),
						click : false,
						drag : false,
						sortable:false,
						editable:false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
                                                                if(_.isUndefined(rawValue)){
                                                                        return '<center>--</center>';
                                                                }
                                                                return XAUtils.showAuditLogTags(_.sortBy(JSON.parse(rawValue), 'type'), model);
							}
						}),
					},
            };
            if (this.accessAuditList.queryParams.sortBy && !_.isEmpty(this.accessAuditList.queryParams.sortBy)) {
                XAUtils.backgridSortType(this.accessAuditList, cols);
            }
			return this.accessAuditList.constructor.getTableCols(cols, this.accessAuditList);
		},
		renderLoginSessionTable : function(){
			var that = this;
			this.ui.tableList.removeClass("clickable");
			this.rTableList.show(new XATableLayout({
				columns: this.getLoginSessionColumns(),
				collection:this.authSessionList,
				includeFilter : false,
				gridOpts : {
					row :Backgrid.Row.extend({}),
					header : XABackgrid,
					emptyText : 'No login session found!!'
				}
                        }));
            XAUtils.backgridSort(this.authSessionList);
		},
		getLoginSessionColumns : function(){
			var authStatusList = [],authTypeList = [];
			_.each(XAEnums.AuthStatus, function(obj){
					if(obj.value !=  XAEnums.AuthStatus.AUTH_STATUS_UNKNOWN.value)
						authStatusList.push({text :obj.label, id :obj.value});
			});
			_.each(XAEnums.AuthType, function(obj){
				if(obj.value !=  XAEnums.AuthType.AUTH_TYPE_UNKNOWN.value)
					authTypeList.push({text :obj.label, id :obj.value});
			});

			var cols = {
                id : {
                    label : localization.tt("lbl.sessionId"),
                    cell : "html",
                    editable:false,
                    sortType: 'toggle',
                    sortable : true,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            var sessionId = model.get('id');
                            if (!_.isUndefined(sessionId)) {
                                return '<a href="javascript:void(0);" data-name ="viewSession" data-id="'+sessionId+'" title="'+sessionId+'">'+sessionId+'</a>';
                            } else {
                                return '';
                            }
                        }
                    })
                },
				loginId : {
					label : localization.tt("lbl.loginId"),
					cell: "String",
					sortable:false,
					editable:false
				},
				authStatus : {
					label : localization.tt("lbl.result"),
					cell :"html",
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue) {
							var label = '', html = '';
							_.each(_.toArray(XAEnums.AuthStatus),function(m){
								if(parseInt(rawValue) == m.value){
									label=  m.label;
									if(m.value == 1){
										html = 	'<label class="badge badge-success">'+label+'</label>';
									} else if(m.value == 2){
										html = 	'<label class="badge badge-danger">'+label+'</label>';
									} else {
										html = 	'<label class="label">'+label+'</label>';
									}
								}	
							});
							return html;
						}
					}),
					sortable:false,
					editable:false
				},
				authType: {
					label : localization.tt("lbl.loginType"),
					cell :"string",
					sortable:false,
					editable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue) {
							var label='';
							_.each(_.toArray(XAEnums.AuthType),function(m){
								if(parseInt(rawValue) == m.value){
									label=  m.label;
								}	
							});
							return label;
						}
					})
					
				},
				requestIP : {
					label : localization.tt("lbl.ip"),
					cell: "String",
					sortable:false,
					editable:false,
					placeholder : localization.tt("lbl.ip")
				},
				requestUserAgent : {
					label : localization.tt("lbl.userAgent"),
					cell: "html",
					sortable:false,
					editable:false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model) {
							return _.isUndefined(rawValue) ? '--': 
								'<span title="'+_.escape(rawValue) +'" class="showMore">'+_.escape(rawValue)+'</span>';
						}
					})
				},
				authTime : {
					label : localization.tt("lbl.loginTime")+ '   ( '+this.timezone+' )',
					cell: "String",
					editable:false,
                    sortable : false,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							return Globalize.format(new Date(model.get('authTime')),  "MM/dd/yyyy hh:mm:ss tt");
						}
					})
				}
			};
            if (this.authSessionList.queryParams.sortBy && !_.isEmpty(this.authSessionList.queryParams.sortBy)) {
                XAUtils.backgridSortType(this.authSessionList, cols);
            }
			return this.authSessionList.constructor.getTableCols(cols, this.authSessionList);
		},
		renderAgentTable : function(){
			this.ui.tableList.removeClass("clickable");
			this.rTableList.show(new XATableLayout({
				columns: this.getAgentColumns(),
				collection: this.policyExportAuditList,
				includeFilter : false,
				gridOpts : {
					row : 	Backgrid.Row.extend({}),
					header : XABackgrid,
					emptyText : 'No plugin found!'
				}
                        }));
            XAUtils.backgridSort(this.policyExportAuditList);
		},
		getAgentColumns : function(){
			var cols = {
					createDate : {
						cell : 'string',
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
								return Globalize.format(new Date(model.get('createDate')),  "MM/dd/yyyy hh:mm:ss tt");
							}
						}),
						label : localization.tt('lbl.createDate')+ '   ( '+this.timezone+' )',
						editable:false,
						sortType: 'toggle',
                        sortable : true,
					},
					repositoryName : {
						cell : 'html',
						label	: localization.tt('lbl.serviceName'),
						editable:false,
						sortable:false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
                                                                var repoName = _.escape(model.get('repositoryDisplayName'));
                                                                return '<span title="'+repoName+'">'+repoName+'</span>';
							}
						}),
						
					},
					agentId : {
                                                cell : 'html',
						label	:localization.tt('lbl.agentId'),
						editable:false,
                                                sortable:false,
                                                formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                        fromRaw: function (rawValue, model) {
                                                                        rawValue = _.escape(rawValue);
                                                                        return '<span title="'+rawValue+'">'+rawValue+'</span>';
                                                                     }
                                                    }),
                                    },
					clientIP : {
						cell : 'string',
						label	: localization.tt('lbl.agentIp'),
						editable:false,
						sortable:false
					},
                                        clusterName : {
                                                label : localization.tt("lbl.clusterName"),
                                                cell: 'html',
                                                click : false,
                                                drag : false,
                                                sortable:false,
                                                editable:false,
                                                formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                        fromRaw: function (rawValue, model) {
                                                                rawValue = _.escape(rawValue);
                                                                return '<span title="'+rawValue+'">'+rawValue+'</span>';
                                                        }
                                                }),
                                        },
					httpRetCode : {
						cell : 'html',
						label	: localization.tt('lbl.httpResponseCode'),
						editable:false,
						sortable:false,
						formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
							fromRaw: function (rawValue, model) {
								var html = rawValue;
								return (rawValue > 400) ? '<label class="label btn-danger">'+rawValue+'</label>'
										:	'<label class="label btn-success">'+rawValue+'</label>';
							}
						})
					},
					syncStatus : {
						cell : 'string',
						label	: localization.tt("lbl.status"),
						editable:false,
						sortable:false
					},

					
			};
            if (this.policyExportAuditList.queryParams.sortBy && !_.isEmpty(this.policyExportAuditList.queryParams.sortBy)) {
                XAUtils.backgridSortType(this.policyExportAuditList, cols);
            }
			return this.policyExportAuditList.constructor.getTableCols(cols, this.policyExportAuditList);
		},
		renderPluginInfoTable : function(){
			this.ui.tableList.removeClass("clickable");
                        this.rTableList.show(new XATableLayout({
                                columns: this.getPluginInfoColums(),
                                collection: this.pluginInfoList,
                                includePagination : false,
                                includeFilter : false,
                                gridOpts : {
                                        row : 	Backgrid.Row.extend({}),
                                        header : XABackgrid,
                                                emptyText : 'No plugin status found!'
                                }
                        }));
            XAUtils.backgridSort(this.pluginInfoList);
                },
                getPluginInfoColums : function(){
                        var that = this, cols ={
				serviceName : {
                                        cell 	: 'html',
					label	: localization.tt("lbl.serviceName"),
					editable:false,
                                        sortable:true,
                                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                fromRaw: function (rawValue, model) {
                                                        var repoName = _.escape(model.get('serviceDisplayName'));
                                                        return '<span title="'+repoName+'">'+repoName+'</span>';
                                                }
                                        }),
                                },
                                serviceType : {
                                        cell : 'html',
                                        label   : localization.tt("lbl.serviceType"),
                                        editable:false,
                                        sortable:true,
                                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                fromRaw: function (rawValue, model) {
                                                        if(_.isEmpty(rawValue) || _.isUndefined(rawValue)){
                                                                return '<center>--</center>';
                                                        }
                                                        var repoType = _.escape(model.get('serviceTypeDisplayName'));
                                                        return '<span title="'+repoType+'">'+repoType+'</span>';
                                                }
                                        }),
				},
				appType : {
                                        cell : 'html',
                                        label	: localization.tt("lbl.applicationType"),
					editable:false,
                                        sortable:true,
                                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                fromRaw: function (rawValue, model) {
                                                        rawValue = _.escape(rawValue);
                                                        return '<span title="'+rawValue+'">'+rawValue+'</span>';
                                                }
                                        }),
				},
				hostName : {
					cell : 'html',
					label	: localization.tt("lbl.hostName"),
					editable:false,
                                        sortable:true,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							rawValue = _.escape(rawValue);
                                                        if(_.isEmpty(rawValue) || _.isUndefined(rawValue)){
                                                                return '<center>--</center>';
                                                        }
                            return '<span title="'+rawValue+'">'+rawValue+'</span>';
						}
					}),
				},
				ipAddress : {
					cell 	: 'string',
					label	: localization.tt("lbl.agentIp"),
					editable:false,
                                        sortable:true,
				},
				clusterName : {
					cell : 'html',
					label	: localization.tt("lbl.clusterName"),
					editable:false,
					sortable:true,
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							var clusterName = _.escape(model.get('info')['clusterName']);
							return '<span title="'+clusterName+'">'+clusterName+'</span>';
						}
					}),
				},
				lastPolicyUpdateTime : {
					cell : 'html',
					label : localization.tt("lbl.lastUpdate"),
					editable : false,
					sortable : true,
					sortValue: function (model, sortKey) {
						return model.get('info').lastPolicyUpdateTime;
					},
					formatter : _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(_.isUndefined(model.get('info').lastPolicyUpdateTime)
								|| _.isNull(model.get('info').lastPolicyUpdateTime)){
									return '<center>--</center>';
							}
                                                        return that.setTimeStamp(new Date(parseInt(model.get('info')['lastPolicyUpdateTime'])) , moment);
						}
					})
				},
				policyDownloaded : {
					cell : 'html',
					label	: localization.tt("lbl.download"),
					editable:false,
					sortable:true,
					sortValue: function (model, sortKey) {
						return model.get('info').policyDownloadTime;
					},
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(_.isUndefined(model.get('info').policyDownloadTime)
								|| _.isNull(model.get('info').policyDownloadTime)){
								return '<center>--</center>';
							}
                                                        var downloadDate = new Date(parseInt(model.get('info')['policyDownloadTime']));
                                                        if(!_.isUndefined(model.get('info')['lastPolicyUpdateTime'])){
                                                                var lastUpdateDate = new Date(parseInt(model.get('info')['lastPolicyUpdateTime']));
                                                                if(that.isDateDifferenceMoreThanHr(downloadDate, lastUpdateDate)){
                                                                        if(moment(downloadDate).diff(moment(lastUpdateDate),'minutes') >= -2) {
                                                                                return '<span class="text-warning"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.downloadTimeDelayMsg")+'"></i>'
                                                                                + that.setTimeStamp(downloadDate , moment) +'</span>';
                                                                        } else {
                                                                                return '<span class="text-error"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.downloadTimeDelayMsg")+'"></i>'
                                                                                + that.setTimeStamp(downloadDate , moment)+'</span>';
                                                                        }

                                                                }
                                                        }
                                                        return that.setTimeStamp(new Date(parseInt(model.get('info')['policyDownloadTime'])) , moment);
						}
					})
				},
				policyActive: {
					cell : 'html',
					label	: localization.tt("lbl.active"),
					editable:false,
					sortable:true,
					sortValue: function (model, sortKey) {
						return model.get('info').policyActivationTime;
					},
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(_.isUndefined(model.get('info').policyActivationTime)
                                                                || _.isNull(model.get('info').policyActivationTime)
                                                                || model.get('info').policyActivationTime == 0){
								return '<center>--</center>';
							}
							var activeDate = new Date(parseInt(model.get('info')['policyActivationTime']));
							if(!_.isUndefined(model.get('info')['lastPolicyUpdateTime'])){
								var lastUpdateDate = new Date(parseInt(model.get('info')['lastPolicyUpdateTime']));
								if(that.isDateDifferenceMoreThanHr(activeDate, lastUpdateDate)){
                                                                        if(moment(activeDate).diff(moment(lastUpdateDate),'minutes') >= -2) {
                                                                                return '<span class="text-warning"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.activationTimeDelayMsg")+'"></i>'
                                                                                + that.setTimeStamp(activeDate , moment) +'</span>';
                                                                        } else {
                                                                                return '<span class="text-error"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.activationTimeDelayMsg")+'"></i>'
                                                                                + that.setTimeStamp(activeDate , moment)+'</span>';
                                                                        }

								}
							}
                                                        return that.setTimeStamp(activeDate , moment);
						}
					})
				},
				lastTagUpdateTime : {
					cell 	: 'html',
					label	:localization.tt("lbl.lastUpdate"),
					editable:false,
					sortable:true,
					sortValue: function (model, sortKey) {
						//If lastTagUpdateTime is undefined or not present in respones then set lastTagUpdateTime value to null for sorting purpose.
						if(model.get('info') && _.isUndefined(model.get('info').lastTagUpdateTime)){
							model.get('info').lastTagUpdateTime = null;
						}
						return model.get('info').lastTagUpdateTime;
					},
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(_.isUndefined(model.get('info').lastTagUpdateTime)
								|| _.isNull(model.get('info').lastTagUpdateTime)){
								return '<center>--</center>';
							}
                                                        return that.setTimeStamp(new Date(parseInt(model.get('info')['lastTagUpdateTime'])) , moment);
						}
					})
				},
				tagDownloaded : {
					cell : 'html',
					label : localization.tt("lbl.download"),
					editable : false,
					sortable : true,
					sortValue: function (model, sortKey) {
						return model.get('info').tagDownloadTime;
					},
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(_.isUndefined(model.get('info').tagDownloadTime)
								|| _.isNull(model.get('info').tagDownloadTime)){
								return '<center>--</center>';
							}
                                                        var downloadTagDate = new Date(parseInt(model.get('info')['tagDownloadTime']));
                                                        if(!_.isUndefined(model.get('info')['lastTagUpdateTime'])){
                                                                var lastUpdateDate = new Date(parseInt(model.get('info')['lastTagUpdateTime']));
                                                                if(that.isDateDifferenceMoreThanHr(downloadTagDate, lastUpdateDate)){
                                                                        if(moment(downloadTagDate).diff(moment(lastUpdateDate),'minutes') >= -2) {
                                                                                return '<span class="text-warning"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.downloadTimeDelayMsg")+'"></i>'
                                                                                + that.setTimeStamp(downloadTagDate , moment) +'</span>';
                                                                        } else {
                                                                                return '<span class="text-error"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.downloadTimeDelayMsg")+'"></i>'
                                                                                + that.setTimeStamp(downloadTagDate , moment)+'</span>';
                                                                        }

                                                                }
                                                        }
                                                        return that.setTimeStamp(new Date(parseInt(model.get('info')['tagDownloadTime'])) , moment);

						}
					})
				},
				tagActive : {
					cell : 'html',
					label	: localization.tt("lbl.active"),
					editable:false,
					sortable:true,
					sortValue: function (model, sortKey) {
						return model.get('info').tagActivationTime;
					},
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(_.isUndefined(model.get('info').tagActivationTime)
								|| _.isNull(model.get('info').tagActivationTime)){
								return '<center>--</center>';
							}
							var activeDate = new Date(parseInt(model.get('info')['tagActivationTime']));
								if(!_.isUndefined(model.get('info')['lastTagUpdateTime'])){
									var lastUpdateDate = new Date(parseInt(model.get('info')['lastTagUpdateTime']));
									if(that.isDateDifferenceMoreThanHr(activeDate, lastUpdateDate)){
                                                                                if(moment(activeDate).diff(moment(lastUpdateDate),'minutes') >= -2) {
                                                                                        return '<span class="text-warning"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.activationTimeDelayMsg")+'"></i>'
                                                                                        + that.setTimeStamp(activeDate , moment) +'</span>';
                                                                                } else {
                                                                                        return '<span class="text-error"><i class="fa-fw fa fa-exclamation-circle activePolicyAlert" title="'+localization.tt("msg.activationTimeDelayMsg")+'"></i>'
                                                                                        + that.setTimeStamp(activeDate , moment)+'</span>';
                                                                                }
									}
								}
                                                                return that.setTimeStamp(activeDate , moment);

						}
					})
				},
			}
            if (this.pluginInfoList.queryParams.sortBy && !_.isEmpty(this.pluginInfoList.queryParams.sortBy)) {
                XAUtils.backgridSortType(this.pluginInfoList, cols);
            }
			return this.pluginInfoList.constructor.getTableCols(cols, this.pluginInfoList);
		},
        renderUserSyncTable : function(){
            this.$el.addClass("user-sync-table");
            this.ui.tableList.removeClass("clickable");
            this.rTableList.show(new XATableLayout({
                columns: this.getUserSyncColums(),
                collection: this.userSyncAuditList,
                includeFilter : false,
                gridOpts : {
                    row : Backgrid.Row.extend({}),
                    header : XABackgrid,
                    emptyText : 'No user sync audit found!'
                }
            }));
            XAUtils.backgridSort(this.userSyncAuditList);
        },
        getUserSyncColums : function(){
            var cols ={
                userName : {
                    cell 	: 'string',
                    label	: localization.tt("lbl.userName"),
                    editable:false,
                    sortable:false,
                },
                syncSource : {
                    cell 	: 'html',
                    label	: localization.tt("lbl.syncSource"),
                    editable:false,
                    sortable:false,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            var label = rawValue == "Unix" ? 'success' : (rawValue == "File" ? 'info' : 'yellow');
                            return '<center><label class="badge badge-'+label+'">'+_.escape(rawValue)+'</label></center>';
                        }
                    }),
                },
                noOfNewUsers : {
                    cell 	: 'string',
                    label	: localization.tt("h.users"),
                    editable:false,
                    sortable:false,
                },
                noOfNewGroups : {
                    cell 	: 'string',
                    label	: localization.tt("h.groups"),
                    editable:false,
                    sortable:false,
                },
                noOfModifiedUsers : {
                    cell    : 'string',
                    label   : localization.tt("h.users"),
                    editable:false,
                    sortable:false,
                },
                noOfModifiedGroups : {
                    cell    : 'string',
                    label   : localization.tt("h.groups"),
                    editable:false,
                    sortable:false,
                },
                eventTime : {
                    label : 'Event Time',
                    cell: "String",
                    click : false,
                    drag : false,
                    editable:false,
                    sortable : true,
                    sortType: 'toggle',
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            return Globalize.format(new Date(rawValue),  "MM/dd/yyyy hh:mm:ss tt");
                        }
                    })
                },
                syncSourceDetail : {
                    cell    : 'html',
                    label : localization.tt("h.syncDetails"),
                    editable:false,
                    sortable:false,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            return('<button data-id="syncDetailes" title="Sync Details" id="'+ model.get('id') +'" ><i class="fa-fw fa fa-eye"> </i></button>');
                        }
                    }),
                }
            }
            if (this.userSyncAuditList.queryParams.sortBy && !_.isEmpty(this.userSyncAuditList.queryParams.sortBy)) {
                XAUtils.backgridSortType(this.userSyncAuditList, cols);
            }
            return this.userSyncAuditList.constructor.getTableCols(cols, this.userSyncAuditList);

        },
        onSyncDetailes : function(e){
            XAViewUtils.syncSourceDetail(e , this);
        },

        onViewSession : function(e) {
            var authSessionList = new VXAuthSession();
            var sessionId = $(e.currentTarget).data('id');
            authSessionList.fetch({
                data : {
                    id : sessionId
                }
            }).done(function() {
                var view = new vLoginSessionDetail({
                    model : authSessionList.first()
                });
                var modal = new Backbone.BootstrapModal({
                    animate : true,
                    content : view,
                    title : localization.tt("lbl.sessionDetail"),
                    okText : localization.tt("lbl.ok"),
                    allowCancel : true,
                    escape : true,
                    focusOk : false
                }).open();
                //modal.$el.addClass('modal-diff').attr('tabindex', -1);
                modal.$el.find('.cancel').hide();
            });
        },

        isDateDifferenceMoreThanHr : function(date1, date2){
                var diff = (date1 - date2) / 36e5;
                return diff < 0 ? true : false;
        },

        //Time stamp
        setTimeStamp : function(time, moment) {
            return '<span title="'+Globalize.format(time,  "MM/dd/yyyy hh:mm:ss tt")+'">'+Globalize.format(time,  "MM/dd/yyyy hh:mm:ss tt")
            + '<div class="text-muted"><small>'+moment(time).fromNow()+'</small></div></span>'
        },

		onRefresh : function(){
			var that =this, coll,params = {};
			var lastUpdateTime = Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt");
			that.ui.lastUpdateTimeLabel.html(lastUpdateTime);
			switch (this.currentTab) {
			case "#bigData":
				coll = this.accessAuditList;
				break;
			case "#admin":
				coll = this.trxLogList;
				break;
			case "#loginSession":
				coll = this.authSessionList;
				break;
			case "#agent":
				//TODO
				params = { 'priAcctId' : 1 };
				coll = this.policyExportAuditList;
				break;
			case "#pluginStatus":
                                this.pluginInfoList.switchMode("client", {fetch:false});
				coll = this.pluginInfoList;
				break;
                        case "#userSync":
                                coll = this.userSyncAuditList;
                                break;
			}
			coll.fetch({
				reset : true,
				data  : params,
				success: function(){
					that.ui.lastUpdateTimeLabel.html(lastUpdateTime);
				},
				error : function(){
					that.ui.lastUpdateTimeLabel.html(lastUpdateTime);
				}
				
			});
		},
		onSearch : function(e){
			this.setCurrentTabCollection();
			var resourceName= this.ui.resourceName.val(),
				startDate 	= this.ui.startDate.val(),
				endDate  	= this.ui.endDate.val(),
				params = { 'startDate' : startDate , 'endDate' : endDate };
			//After every search we need goto to first page
			$.extend(this.collection.queryParams,params);
			//collection.fetch({data: data, reset: true});

			this.collection.state.currentPage = this.collection.state.firstPage;
			this.collection.fetch({
				reset : true,
				cache : false,
			});
		},
		setCurrentTabCollection : function(){
			switch (this.currentTab) {
				case "#admin":
					this.collection = this.trxLogList;
					break;
				case "#bigData":
					this.collection = this.accessAuditList;
					break;
				case "#loginSession":
					this.collection = this.authSessionList;
					break;
				case "#agent":
					this.collection = this.policyExportAuditList;
					break;
				case "#pluginStatus":
                                        this.pluginInfoList.switchMode("client",  {fetch:false});
					this.collection = this.pluginInfoList;
					break;
                                case "#userSync":
                                        this.collection = this.userSyncAuditList;
                                        break;
			}
		},
		clearVisualSearch : function(collection, serverAttrNameList) {
			_.each(serverAttrNameList, function(obj) {
				if (!_.isUndefined(collection.queryParams[obj.label]))
					delete collection.queryParams[obj.label];
			});
		},
		updateLastRefresh : function(){
			this.ui.lastUpdateTimeLabel.html(Globalize.format(new Date(),  "MM/dd/yyyy hh:mm:ss tt"));
		},
                showTagsAttributes : function(reset){
                        var that = this;
                        this.accessAuditList.each(function(model){
                                if(!_.isUndefined(model.get('tags'))){
                                        var tagData = JSON.parse(model.get('tags'));
                                        _.each(_.sortBy(tagData, 'type'), function(e, i){
                                                var $table = $('<div class="popover-heading" data-id="'+model.id+'">Attribute Details</div><table class="table table-bordered table-condensed tag-attr-table"  style="margin-left:-5px;"><tbody></tr><tr><td>Name</td><td>Value</td></tr></tbody></table>'),
                                                $tbody = $table.find('tbody');
                                                var $tr = '';
                                                 _.each(e.attributes, function(val, key){
                                                         $tr += ('<tr><td>'+_.escape(key)+'</td><td>'+ _.escape(val) +'</td></tr>');
                                                });
                                                $tbody.append($($tr));
                                                that.$el.find('table a[data-name="tags"][data-id="'+model.id+''+i+'"]').popover({
                                                        content: $table,
                                                        html: true,
                                                        trigger: 'click',
                                                        placement: 'bottom',
                                                        container: 'body',
                                                }).on("click", function(e){
                                                        e.stopPropagation();
                                                        $('[data-name="tags"]').not(this).popover('hide');
                                                        $('.popover').find(".popover-content").addClass("tag-attr-popover");

                                                }).on("focusout", function(e){
                                                        $(e.target).popover('hide');
                                                });


                                        });
                                }
                                XAViewUtils.showQueryPopup(model, that);
                        });
                },
                onShowMore : function(e){
                        var id = $(e.currentTarget).attr('audit-log-id');
                        this.rTableList.$el.find('[audit-log-id="'+id+'"]').show();
                        $('[data-id="showLess"][audit-log-id="'+id+'"]').show();
                        $('[data-id="showMore"][audit-log-id="'+id+'"]').hide();
                        $('[data-id="showMore"][audit-log-id="'+id+'"]').parents('div[data-id="tagDiv"]').addClass('set-height-groups');
                },
                onShowLess : function(e){
                        var id = $(e.currentTarget).attr('audit-log-id');
                        this.rTableList.$el.find('[audit-log-id="'+id+'"]').slice(4).hide();
                        $('[data-id="showLess"][audit-log-id="'+id+'"]').hide();
                        $('[data-id="showMore"][audit-log-id="'+id+'"]').show();
                        $('[data-id="showMore"][audit-log-id="'+id+'"]').parents('div[data-id="tagDiv"]').removeClass('set-height-groups')
                },

        onExcludeServiceUsers : function(e) {
            if(this.currentTab === "#bigData"){
                this.accessAuditList.queryParams.excludeServiceUser = e.currentTarget.checked;
                App.excludeServiceUser = e.currentTarget.checked;
                this.accessAuditList.state.currentPage = this.accessAuditList.state.firstPage;
                XAUtils.blockUI();
                var urlParams = XAUtils.changeUrlToSearchQuery(decodeURIComponent(XAUtils.urlQueryParams()))
                urlParams['excludeServiceUser'] = e.currentTarget.checked;
                XAUtils.changeParamToUrlFragment(urlParams);
                this.accessAuditList.fetch({
                    reset : true,
                    cache : false,
                    error : function(coll, response, options) {
                        XAUtils.blockUI('unblock');
                        if(response && response.responseJSON && response.responseJSON.msgDesc){
                            XAUtils.notifyError('Error', response.responseJSON.msgDesc);
                        }else{
                            XAUtils.notifyError('Error', localization.tt('msg.errorLoadingAuditLogs'));
                        }

                    },
                    success: function(){
                       XAUtils.blockUI('unblock');
                    },
                });
            }
        },
        showPageDetail : function(collection){
           // this.ui.btnShowPageDetails.removeClass('hide');
            if(collection.models.length > 0){
                var startIndex = collection.state.currentPage * collection.state.pageSize + 1,
                totalRecords = collection.state.totalRecords,
                endIndex = Math.min(startIndex + collection.state.pageSize - 1 , totalRecords);
                this.ui.showPageDetail.html((startIndex) +' to '+ (endIndex) +' of '+ totalRecords);
            }else{
                this.ui.showPageDetail.html(0);
            }
        },


		/** on close */
		onClose : function() {
			clearInterval(this.timerId);
			clearInterval(this.clearTimeUpdateInterval);
            XAUtils.removeUnwantedDomElement();
            $('.latestResponse').show();
		}
	});

	return AuditLayout;
});
