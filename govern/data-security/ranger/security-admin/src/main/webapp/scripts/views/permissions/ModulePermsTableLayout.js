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


define(function(require){
    'use strict';

	var Backbone		= require('backbone');
	var XAEnums 		= require('utils/XAEnums');
	var XALinks 		= require('modules/XALinks');
	var XAGlobals 		= require('utils/XAGlobals');
	var SessionMgr 		= require('mgrs/SessionMgr');
	var XAUtil			= require('utils/XAUtils');
	var localization	= require('utils/XALangSupport');

	var XABackgrid		= require('views/common/XABackgrid');
	var XATableLayout	= require('views/common/XATableLayout');
	var RangerServiceDef	= require('models/RangerServiceDef');
	var UserPermission 		= require('models/UserPermission');
	var ModulePermsTableLayoutTmpl = require('hbs!tmpl/permissions/ModulePermsTableLayout_tmpl');

	require('backgrid-filter');
	require('backgrid-paginator');
	require('bootbox');

	var ModulePermsTableLayout = Backbone.Marionette.Layout.extend(
	/** @lends ModulePermsTableLayout */
	{
		_viewName : 'ModulePermsTableLayout',

		template: ModulePermsTableLayoutTmpl,

		templateHelpers : function(){
			return{
                setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
			}
		},

		breadCrumbs : function(){
			return [XALinks.get('ModulePermissions')];
		},

		/** Layout sub regions */
		regions: {
			'rTableList'	: 'div[data-id="r_table"]',
		},

		/** ui selector cache */
		ui: {
			'btnShowMore' : '[data-id="showMore"]',
			'btnShowLess' : '[data-id="showLess"]',
			'visualSearch' : '.visual_search'
		},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click ' + this.ui.btnShowMore]  = 'onShowMore';
			events['click ' + this.ui.btnShowLess]  = 'onShowLess';

			return events;
		},

		/**
		* intialize a new RangerPolicyTableLayout Layout
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a ModulePermsTableLayout Layout");
			
                _.extend(this, _.pick(options));
                this.urlQueryParams = XAUtil.urlQueryParams();
			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
		},
		initializeModulePerms : function(){
		   this.collection.fetch({
			   cache : false,
		   });

		},
		/** on render callback */
		onRender: function() {
			//this.initializePlugins();
			this.renderTable();
                        this.addVisualSearch();
                        if(_.isUndefined(this.urlQueryParams)) {
                                this.initializeModulePerms();
                        }
		},
		/** all post render plugin initialization */
		initializePlugins: function(){
		},
		renderTable : function(){
			var that = this;
			this.rTableList.show(new XATableLayout({
				columns: this.getColumns(),
				collection: this.collection,
				includeFilter : false,
				gridOpts : {
					row: Backgrid.Row.extend({}),
					emptyText : 'No permissions found!'
				},
			}));
		},

		getColumns : function(){
			var that = this;
			var cols = {
				module : {
					cell : "uri",
					reName : 'module',
					href: function(model){
                                            return '#!/permissions/'+model.id+'/edit';
					},
					label	: localization.tt("lbl.modules"),
					editable: false,
					sortable : false
				},
				groupNameList : {
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					label : localization.tt("lbl.group"),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								return XAUtil.showGroupsOrUsers(rawValue,model,'groups');
							}else{
								return '--';
							}
						}
					}),
					editable : false,
					sortable : false
				},
				//Hack for backgrid plugin doesn't allow to have same column name
				userNameList : {
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					label : localization.tt("lbl.users"),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							return !_.isUndefined(rawValue) ? 
									XAUtil.showGroupsOrUsers(rawValue, model, 'users')
									:  '--';
						}
					}),
					editable : false,
					sortable : false
				},
			};
                        if(SessionMgr.isSystemAdmin()){
			cols['permissions'] = {
				cell :  "html",
				label : localization.tt("lbl.action"),
				formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
					fromRaw: function (rawValue,model) {
						return '<a href="#!/permissions/'+model.id+'/edit" class="btn btn-sm" title="Edit"><i class="fa-fw fa fa-edit fa-fw fa fa-large"></i></a>';
					}
				}),
				editable: false,
				sortable : false

			};
                        }
                        if(SessionMgr.isAuditor()){
                            cols.module.cell = "string";
                        }
			return this.collection.constructor.getTableCols(cols, this.collection);
		},
		onShowMore : function(e){
			var attrName = 'policy-groups-id';
			var id = $(e.currentTarget).attr(attrName);
			if(_.isUndefined(id)){
				id = $(e.currentTarget).attr('policy-users-id');
				attrName = 'policy-users-id';
			}
			var $td = $(e.currentTarget).parents('td');
                        var show = false, shownCnt = 1;
                        $.each($td.find('[data-id="moreSpans"]'), function(i, div){
                                if($(div).is(':hidden') && !show){
                                        $(div).show();
                                        show = true;
                                        return false;
                                }
                                if(!$(div).is(':hidden')){
                                        shownCnt++;
                                }
                        })
                        if($td.find('[data-id="moreSpans"]').length == shownCnt){
                                $td.find('[data-id="showLess"]['+attrName+'="'+id+'"]').show();
                                $td.find('[data-id="showMore"]['+attrName+'="'+id+'"]').hide();
                        }
			$td.find('[data-id="showMore"]['+attrName+'="'+id+'"]').parents('div[data-id="groupsDiv"]').addClass('set-height-groups');
		},
		onShowLess : function(e){
			var attrName = 'policy-groups-id';
			var id = $(e.currentTarget).attr(attrName);
			if(_.isUndefined(id)){
				id = $(e.currentTarget).attr('policy-users-id');
				attrName = 'policy-users-id';
			}
			var $td = $(e.currentTarget).parents('td');
                        $td.find('[data-id="moreSpans"]').hide();
			$td.find('[data-id="showLess"]['+attrName+'="'+id+'"]').hide();
			$td.find('[data-id="showMore"]['+attrName+'="'+id+'"]').show();
			$td.find('[data-id="showMore"]['+attrName+'="'+id+'"]').parents('div[data-id="groupsDiv"]').removeClass('set-height-groups');
		},
		addVisualSearch : function(){
                        var that = this, query = '';
            var searchOpt = ['Module Name','Group Name','User Name'];
            var serverAttrName  = [{text : "Module Name", label :"module", urlLabel : "moduleName"},
                                    {text : "Group Name", label :"groupName", urlLabel : "groupName"},
                                    {text : "User Name", label :"userName", urlLabel : "userName"}
                                ];
            if(!_.isUndefined(this.urlQueryParams)) {
                var urlQueryParams = XAUtil.changeUrlToSearchQuery(this.urlQueryParams);
                _.map(urlQueryParams, function(val , key) {
                    if (_.some(serverAttrName, function(m){return m.urlLabel == key})) {
                        query += '"'+XAUtil.filterKeyForVSQuery(serverAttrName, key)+'":"'+val+'"';
                    }
                });
            }
            var pluginAttr = {
                                placeholder :localization.tt('h.searchForPermissions'),
                                container : this.ui.visualSearch,
                                query     : query,
                                callbacks :  {
                                        valueMatches :function(facet, searchTerm, callback) {
                                                switch (facet) {
                                                }
                                        }
                                }
                        };
			window.vs = XAUtil.addVisualSearch(searchOpt,serverAttrName, this.collection,pluginAttr);
		},
		getActiveStatusNVList : function() {
			var activeStatusList = _.filter(XAEnums.ActiveStatus, function(obj){
				if(obj.label != XAEnums.ActiveStatus.STATUS_DELETED.label)
					return obj;
			});
			
			return _.map(activeStatusList, function(status) { return { 'label': status.label, 'value': status.label.toLowerCase()}; })
		},
		/** on close */
		onClose: function(){
		}

	});

	return ModulePermsTableLayout;
});
