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
	var XAUtil			= require('utils/XAUtils');
	var XABackgrid		= require('views/common/XABackgrid');
	var localization	= require('utils/XALangSupport');
	var SessionMgr  	= require('mgrs/SessionMgr');
	var App				= require('App');

	var VXGroupList		= require('collections/VXGroupList');
	var VXGroup			= require('models/VXGroup');
	var VXUserList		= require('collections/VXUserList');
	var XATableLayout	= require('views/common/XATableLayout');
	var vUserInfo		= require('views/users/UserInfo');
	var VXUser          = require('models/VXUser');
        var VXRoleList		= require('collections/VXRoleList');
        var VXRole			= require('models/VXRole');

	var UsertablelayoutTmpl = require('hbs!tmpl/users/UserTableLayout_tmpl');
	var XAViewUtils		= require('utils/XAViewUtils');

	var UserTableLayout = Backbone.Marionette.Layout.extend(
	/** @lends UserTableLayout */
	{
		_viewName : 'UserTableLayout',
		
    	template: UsertablelayoutTmpl,

		templateHelpers : function(){
			return{
				setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
			}
		},

    	breadCrumbs :[XALinks.get('Users')],
		/** Layout sub regions */
    	regions: {
    		'rTableList' :'div[data-id="r_tableList"]',
    		'rUserDetail'	: '#userDetail'
    	},

    	/** ui selector cache */
    	ui: {
    		tab 		: '.nav-tabs',
    		addNewUser	: '[data-id="addNewUser"]',
    		addNewGroup	: '[data-id="addNewGroup"]',
    		visualSearch: '.visual_search',
    		btnShowMore : '[data-id="showMore"]',
			btnShowLess : '[data-id="showLess"]',
    		btnSave		: '[data-id="save"]',
    		btnShowHide		: '[data-action="showHide"]',
			visibilityDropdown		: '[data-id="visibilityDropdown"]',
			addNewBtnDiv	: '[data-id="addNewBtnDiv"]',
            deleteUser: '[data-id="deleteUserGroup"]',
            showUserList:'[data-js="showUserList"]',
            addNewRoles: '[data-id="addNewRoles"]',
            hideShowVisibility: '[data-id="hideShowVisibility"]',
            syncDetailes : '[data-id="syncDetailes"]',
    	},

		/** ui events hash */
		events: function() {
			var events = {};
			events['click '+this.ui.tab+' li a']  = 'onTabChange';
			events['click ' + this.ui.btnShowMore]  = 'onShowMore';
			events['click ' + this.ui.btnShowLess]  = 'onShowLess';
			events['click ' + this.ui.btnSave]  = 'onSave';
			events['click ' + this.ui.visibilityDropdown+ ' a']  = 'onVisibilityChange';
			events['click ' + this.ui.deleteUser] = 'onDeleteUser';
            events['click ' + this.ui.showUserList] = 'showUserList';
            events['click '+this.ui.syncDetailes] = 'onSyncDetailes';
            return events;
		},

    	/**
		* intialize a new UserTableLayout Layout 
		* @constructs
		*/
		initialize: function(options) {
			console.log("initialized a UserTableLayout Layout");

			_.extend(this, _.pick(options, 'groupList','tab', 'roleList'));
			this.urlQueryParams = XAUtil.urlQueryParams();
			this.showUsers = this.tab == 'usertab' ? true : false;
                        this.showGroups = this.tab == 'grouptab' ? true : false;
			this.chgFlags = [];
			if(_.isUndefined(this.groupList)){
				this.groupList = new VXGroupList();
			}
			if(_.isUndefined(this.roleList)){
				this.roleList = new VXRoleList();
				this.roleList.url = "service/roles/lookup/roles";
			}

			this.bindEvents();
		},

		/** all events binding here */
		bindEvents : function(){
			var that = this;
			
			/*this.listenTo(this.model, "change:foo", this.modelChanged, this);*/
			/*this.listenTo(communicator.vent,'someView:someEvent', this.someEventHandler, this)'*/
		},

		/** on render callback */
		onRender: function() {
			this.initializePlugins();
            this.renderTable();
			this.bindEventToColl(this.collection);
			this.bindEventToColl(this.groupList);
                        this.bindEventToColl(this.roleList);
        },
		bindEventToColl : function(coll){
			if(_.isUndefined(coll)) return;
			this.listenTo(coll, "sync reset", function(){
				this.$el.find('table input[type="checkbox"]').prop('checked',false)
				coll.each(function(model, key, value){
					coll.trigger("backgrid:selected", model, false, true);
					coll.deselect(model);
				}, this);
			}, this);
			this.listenTo(coll, 'model:highlightUserGroupTableRow', function(){
				this.$el.find('.table tr:last').addClass("alert");
				var self = this;
				setTimeout(function () {
					self.$el.find('.table tr:last').removeClass("alert");
				}, 6000);
				XAUtil.scrollToField(this.$el.find('.table tr:last'));
			})
		},
		onTabChange : function(e){
			var that = this;
                        if (!_.isUndefined(e)) {
                                var nav = e.currentTarget.hash === "#users" ? "#!/users/usertab" : (e.currentTarget.hash === "#groups" ?
                                 "#!/users/grouptab" : "#!/users/roletab");
                                App.appRouter.navigate(nav,{trigger: false});
                                this.showUsers = $(e.currentTarget).attr('href') == '#users' ? true : false;
                                this.showGroups = $(e.currentTarget).attr('href') == '#groups' ? true : false;
                this.urlQueryParams = '';
                        }
            this.renderTable();
                        this.chgFlags = [];
			$(this.rUserDetail.el).hide();
		},

        renderTable : function() {
            // this.addVisualSearch();
            if (this.$el.find('.nav-tabs') && this.$el.find('.nav-tabs a').hasClass('active')) {
                this.$el.find('.nav-tabs a').removeClass('active');
            }
            if(this.showUsers){
                this.$el.find('[data-js="users"] a').addClass('active')
                this.renderUserTab();
            } else if(this.showGroups){
                this.$el.find('[data-js="groups"] a').addClass('active')
                this.renderGroupTab();
            } else {
                this.$el.find('[data-js="roles"] a').addClass('active')
                this.renderRoleTab();
            }
        },

		onVisibilityChange : function(e){
			var that = this;
			var status = $(e.currentTarget).attr('data-id') == 'visible' ? true : false;
			var updateReq = {};
			var collection = this.showUsers ? this.collection : this.groupList;

			collection.each(function(m){
				if(m.selected && m.get('isVisible') != status){
				  	m.set('isVisible', status);
					m.toServer();
					updateReq[m.get('id')] = m.get('isVisible');
				}
			});
			if(_.isEmpty(updateReq)){
				if(this.showUsers){
					XAUtil.alertBoxWithTimeSet(localization.tt('msg.plsSelectUserToSetVisibility'));
				}else{
					XAUtil.alertBoxWithTimeSet(localization.tt('msg.plsSelectGroupToSetVisibility'));
				}
				
				return;
			}
			var clearCache = function(coll){
					_.each(Backbone.fetchCache._cache, function(url, val){
		                   var urlStr = coll.url;
		                   if((val.indexOf(urlStr) != -1)){
		                       Backbone.fetchCache.clearItem(val);
		                   }
		                });
					coll.fetch({reset: true, cache:false}).done(function(coll){
	                	coll.each(function(model){
	                		coll.trigger("backgrid:selected", model, false, true);
	    					coll.deselect(model);
	    				}, this);
	                })
			}
			if(this.showUsers){
				collection.setUsersVisibility(updateReq, {
					success : function(){
						that.chgFlags = [];
						clearCache(collection);
					},
					error : function(resp){
						if(!_.isUndefined(resp) && !_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
							XAUtil.notifyError('Error', resp.responseJSON.msgDesc);
						}else{
							XAUtil.notifyError('Error', "Error occurred while updating user");
						}
						collection.trigger('error','',resp)
					},
				});
			} else {
			    collection.setGroupsVisibility(updateReq, {
					success : function(){
						that.chgFlags = [];
						clearCache(collection);
					},
					error : function(resp){
						if(!_.isUndefined(resp) && !_.isUndefined(resp.responseJSON) && !_.isUndefined(resp.responseJSON.msgDesc)){
							XAUtil.notifyError('Error', resp.responseJSON.msgDesc);
						}else{
							XAUtil.notifyError('Error', "Error occurred while updating group");
						}
						collection.trigger('error','',resp)
					},
                });

			}
		},
		renderUserTab : function(){
			var that = this;
                        this.ui.addNewRoles.hide();
                        this.ui.hideShowVisibility.show();
                        if(_.isUndefined(this.collection) || _.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)){
				this.collection = new VXUserList();
                        }
			this.collection.selectNone();
			this.renderUserListTable();
                        _.extend(this.collection.queryParams, XAUtil.getUserDataParams());
                        this.ui.addNewGroup.hide();
                        this.ui.addNewUser.show();
                        this.$('.wrap-header').text('User List');
                        this.addVisualSearch();
            if (_.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)) {
                this.collection.fetch({
				reset: true,
				cache: false
			}).done(function(userList){
				if(App.usersGroupsListing && !_.isEmpty(App.usersGroupsListing) && App.usersGroupsListing.showLastPage){
					if(userList.state.totalRecords > userList.state.pageSize){
						that.collection.getLastPage({
							cache : false,
						}).done(function(m){
							App.usersGroupsListing={};
							(_.last(m.models)).trigger("model:highlightUserGroupTableRow");
						});
					}else{
						_.last(userList.models).trigger("model:highlightUserGroupTableRow");
					}
				}
				that.checkRoleKeyAdmin();
			});
            }
		},
		renderGroupTab : function(){
			var that = this;
            this.ui.addNewRoles.hide();
            this.ui.hideShowVisibility.show();
                        if(_.isUndefined(this.groupList) || _.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)){
				this.groupList = new VXGroupList();
			}
			this.groupList.selectNone();
			this.renderGroupListTable();
                        this.addVisualSearch();
                        this.ui.addNewUser.hide();
                        this.ui.addNewGroup.show();
                        this.$('.wrap-header').text('Group List');
            if (_.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)) {
                this.groupList.fetch({
				reset:true,
				cache: false,
			}).done(function(groupList){
				if(App.usersGroupsListing && !_.isEmpty(App.usersGroupsListing) && App.usersGroupsListing.showLastPage){
					if(groupList.state.totalRecords > groupList.state.pageSize){
						that.groupList.getLastPage({
							cache : false,
						}).done(function(m){
							(_.last(m.models)).trigger("model:highlightUserGroupTableRow");
						});
					}else{
						(_.last(groupList.models)).trigger("model:highlightUserGroupTableRow");
					}
					App.usersGroupsListing={};
				}
				that.checkRoleKeyAdmin();
			});
            }
		},
                renderRoleTab : function(){
                        var that = this;
                        this.ui.hideShowVisibility.hide();
                        if(_.isUndefined(this.roleList) || _.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)){
                                this.roleList = new VXRoleList();
                                this.roleList.url = "service/roles/lookup/roles";
                        }
                        this.ui.addNewUser.hide();
                        this.ui.addNewGroup.hide();
                        this.ui.addNewRoles.show();
                        this.$('.wrap-header').text('Role List');
                        this.renderRoleListTable();
                        this.addVisualSearch();
                        if(_.isUndefined(this.urlQueryParams) || _.isEmpty(this.urlQueryParams)) {
                            this.roleList.fetch({
                                    reset:true,
                                    cache: false,
                            }).done(function(roleList){
                                    if(App.usersGroupsListing && !_.isEmpty(App.usersGroupsListing) && App.usersGroupsListing.showLastPage){
                                            if(roleList.state.totalRecords > roleList.state.pageSize){
                                                    that.roleList.getLastPage({
                                                            cache : false,
                                                    }).done(function(m){
                                                            (_.last(m.models)).trigger("model:highlightUserGroupTableRow");
                                                    });
                                            }else{
                                                    (_.last(roleList.models)).trigger("model:highlightUserGroupTableRow");
                                            }
                                            App.usersGroupsListing={};
                                    }
                            });
                        }
                },
		renderUserListTable : function(){
			var that = this;
			var tableRow = Backgrid.Row.extend({
				render: function () {
					tableRow.__super__.render.apply(this, arguments);
    				if(!this.model.get('isVisible')){
    					this.$el.addClass('tr-inactive');
    				}
    				return this;
				},
			});
			this.rTableList.show(new XATableLayout({
				columns: this.getColumns(),
				collection: this.collection,
				includeFilter : false,
				gridOpts : {
					row: tableRow,
					header : XABackgrid,
					emptyText : 'No users found!'
				}
			}));	

		},

		getColumns : function(){
			var cols = {
				
				select : {
					label : localization.tt("lbl.isVisible"),
					//cell : Backgrid.SelectCell.extend({className: 'cellWidth-1'}),
					cell: "select-row",
				    headerCell: "select-all",
					click : false,
					drag : false,
					editable : false,
					sortable : false
				},
				name : {
					label	: localization.tt("lbl.userName"),
					href: function(model){
                                            return '#!/user/'+ model.id;
					},
					editable:false,
					sortable:false,
                                        sortType: 'toggle',
					cell :'uri'						
				},
				emailAddress : {
					label	: localization.tt("lbl.emailAddress"),
					cell : 'string',
					editable:false,
					sortable:false,
					placeholder : localization.tt("lbl.emailAddress")+'..'
				},
				userRoleList :{
					label	: localization.tt("lbl.role"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue) && rawValue.length > 0){
								var role = rawValue[0];
								return '<span class="badge badge-info">'+XAEnums.UserRoles[role].label+'</span>';
							}
							return '--';
						}
					}),
					click : false,
					drag : false,
					editable:false,
					sortable:false,
				},
				userSource :{
					label	: localization.tt("lbl.userSource"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								if(rawValue == XAEnums.UserSource.XA_PORTAL_USER.value)
									return '<span class="badge badge-success">'+XAEnums.UserTypes.USER_INTERNAL.label+'</span>';
								else
									return '<span class="badge badge-green">'+XAEnums.UserTypes.USER_EXTERNAL.label+'</span>';
							}else
								return '--';
						}
					}),
					click : false,
					drag : false,
					editable:false,
					sortable:false,
				},
				syncSource : {
					label	: localization.tt("lbl.syncSource"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								return '<span class="badge badge-green">'+rawValue+'</span>';
							}else
								return '--';
						}
					}),
					click : false,
					drag : false,
					editable:false,
					sortable:false,
				},
				groupNameList : {
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					label : localization.tt("lbl.groups"),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue,model) {
                                                        if(!_.isUndefined(rawValue) && !_.isEmpty(rawValue)){
                                                                return XAUtil.showMoreLessBtnForGroupsUsersRoles(_.map(rawValue,function(name){return {'modelId': model.id,'entityName': name}}) , 'groups');
							}
							else
							return '--';
						}
					}),
					editable : false,
					sortable : false
				},
				isVisible : {
					label	: localization.tt("lbl.visibility"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								if(rawValue)
									return '<span class="badge badge-success">'+XAEnums.VisibilityStatus.STATUS_VISIBLE.label+'</span>';
								else
									return '<span class="badge badge-green">'+XAEnums.VisibilityStatus.STATUS_HIDDEN.label+'</span>';
							}else
								return '--';
						}
					}),
					editable:false,
					sortable:false
				},
				otherAttributes : {
                    cell    : 'html',
                    label : localization.tt("h.syncDetails"),
                    editable:false,
                    sortable:false,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            if (!_.isUndefined(rawValue)) {
                                return('<button data-id="syncDetailes" data-for="users" title="Sync Details" id="'+ model.get('id') +'" ><i class="fa-fw fa fa-eye"> </i></button>');
                            } else {
                                return '<center> -- </center>'
                            }
                        }
                    }),
                },
			};
                        if(!SessionMgr.isSystemAdmin()){
                            delete cols.select;
                        }
            if(XAUtil.isAuditorOrKMSAuditor(SessionMgr)){
                cols.name.cell = 'string';
            }
			return this.collection.constructor.getTableCols(cols, this.collection);
		},
		
		renderGroupListTable : function(){
			var that = this;
			
			var tableRow = Backgrid.Row.extend({
				render: function () {
    				tableRow.__super__.render.apply(this, arguments);
    				if(!this.model.get('isVisible')){
    					this.$el.addClass('tr-inactive');
    				}
    				return this;
				},
				
			});
			this.rTableList.show(new XATableLayout({
				columns: this.getGroupColumns(),
				collection: this.groupList,
				includeFilter : false,
				gridOpts : {
					row: tableRow,
					header : XABackgrid,
					emptyText : 'No groups found!'
				}
			}));	

		},

		getGroupColumns : function(){
			var cols = {
				
                select : {
					label : localization.tt("lbl.isVisible"),
					cell: "select-row",
				    headerCell: "select-all",
					click : false,
					drag : false,
					editable : false,
					sortable : false
				},
				name : {
					label	: localization.tt("lbl.groupName"),
					href: function(model){
                                            return '#!/group/'+ model.id;
					},
					editable:false,
					sortable:false,
					cell :'uri'
						
				},
				groupSource :{
					label	: localization.tt("lbl.groupSource"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								if(rawValue == XAEnums.GroupSource.XA_PORTAL_GROUP.value)
									return '<span class="badge badge-success">'+XAEnums.GroupTypes.GROUP_INTERNAL.label+'</span>';
								else {
									return '<span class="badge badge-green">'+XAEnums.GroupTypes.GROUP_EXTERNAL.label+'</span>';
								}
							}else {
								return '--';
							}
						}
					}),
					click : false,
					drag : false,
					editable:false,
					sortable:false,
				},
				syncSource : {
					label	: localization.tt("lbl.syncSource"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								return '<span class="badge badge-green">'+rawValue+'</span>';
							}else
								return '--';
						}
					}),
					click : false,
					drag : false,
					editable:false,
					sortable:false,
				},
				isVisible : {
					label	: localization.tt("lbl.visibility"),
					cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
					formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
						fromRaw: function (rawValue, model) {
							if(!_.isUndefined(rawValue)){
								if(rawValue){
									return '<span class="badge badge-success">'+XAEnums.VisibilityStatus.STATUS_VISIBLE.label+'</span>';
								} else {
									return '<span class="badge badge-green">'+XAEnums.VisibilityStatus.STATUS_HIDDEN.label+'</span>';
								}
							}else {
								return '--';
							}
						}
					}),
					editable:false,
					sortable:false
                },
                member	:{
                    label : "Users",
                    click : false,
                    cell  : Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
                    drag  : false,
                    editable  : false,
                    sortable : false,
                    formatter : _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw : function (rawValue,model) {
                            return ('<div align="center"><button class="userViewicon" title = "View Users" data-js="showUserList" data-name="' + model.get('name')
                                + '" data-id="' + model.id + '"<font color="black"><i class="fa-fw fa fa-group"> </i></font></button></div>');
                        }
                    }),
                },
                otherAttributes : {
                    cell    : 'html',
                    label : localization.tt("h.syncDetails"),
                    editable:false,
                    sortable:false,
                    formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                        fromRaw: function (rawValue, model) {
                            if (!_.isUndefined(rawValue)) {
                                return('<button data-id="syncDetailes" data-for="groups" title="Sync Details" id="'+ model.get('id') +'" ><i class="fa-fw fa fa-eye"> </i></button>');
                            } else {
                                return '<center> -- </center>'
                            }
                        }
                    }),
                },
            };
            if(!SessionMgr.isSystemAdmin()){
                delete cols.select;
            }
            if(XAUtil.isAuditorOrKMSAuditor(SessionMgr)){
                cols.name.cell = 'string';
            }
			return this.groupList.constructor.getTableCols(cols, this.groupList);
		},

                renderRoleListTable : function(){
                        var that = this;

                        var tableRow = Backgrid.Row.extend({
                                render: function () {
                                        tableRow.__super__.render.apply(this, arguments);
                                        return this;
                                },

                        });
                        this.rTableList.show(new XATableLayout({
                                columns: this.getRoleColumns(),
                                collection: this.roleList,
                                includeFilter : false,
                                gridOpts : {
                                        row: tableRow,
                                        header : XABackgrid,
                                        emptyText : 'No roles found!'
                                }
                        }));
                },

                getRoleColumns : function(){
                        var cols = {

                                select : {
                                        label : localization.tt("lbl.isVisible"),
                                        cell: "select-row",
                                    headerCell: "select-all",
                                        click : false,
                                        drag : false,
                                        editable : false,
                                        sortable : false
                                },
                                name : {
                                        label	: localization.tt("lbl.roleName"),
                                        href: function(model){
                                                return '#!/roles/'+ model.id;
                                        },
                                        editable:false,
                                        sortable:false,
                                        cell :'uri'
                                },
                                users : {
                                        cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
                                        label : localization.tt("lbl.users"),
                                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                fromRaw: function (rawValue, model) {
                                                        if(!_.isUndefined(rawValue) && rawValue.length != 0){
                                                                return XAUtil.showMoreLessBtnForGroupsUsersRoles(_.map(rawValue,function(m){return {'modelId': model.id,'entityName': m.name}}), 'users');
                                                        }else{
                                                                return '--';
                                                        }
                                                }
                                        }),
                                        editable : false,
                                        sortable : false
                                },
                                groups : {
                                        cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
                                        label : localization.tt("lbl.groups"),
                                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                fromRaw: function (rawValue, model) {
                                                        if(!_.isUndefined(rawValue) && rawValue.length != 0){
								return XAUtil.showMoreLessBtnForGroupsUsersRoles(_.map(rawValue,function(m){return {'modelId': model.id,'entityName': m.name}}), 'groups');
                                                        }else{
                                                                return '--';
                                                        }
                                                }
                                        }),
                                        editable : false,
                                        sortable : false
                                },
                                roles : {
                                        cell	: Backgrid.HtmlCell.extend({className: 'cellWidth-1'}),
                                        label : localization.tt("lbl.roles"),
                                        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                                                fromRaw: function (rawValue, model) {
                                                        if(!_.isUndefined(rawValue) && rawValue.length != 0){
								return XAUtil.showMoreLessBtnForGroupsUsersRoles(_.map(rawValue,function(m){return {'modelId': model.id,'entityName': m.name}}), 'roles');
                                                        }else{
                                                                return '--';
                                                        }
                                                }
                                        }),
                                        editable : false,
                                        sortable : false
                                },
                        };
            if(!SessionMgr.isSystemAdmin()){
                delete cols.select;
            }
            if(XAUtil.isAuditorOrKMSAuditor(SessionMgr)){
                cols.name.cell = 'string';
            }
                        return this.roleList.constructor.getTableCols(cols, this.roleList);
                },

        showUserList :function(e){
            XAUtil.blockUI();
            var that = this , name , msg = "", content = "" , totalRecords, infoMsg = ""; this.copyUserLists = [];
            var anchor = $(e.currentTarget);
            this.groupId = anchor.attr('data-id');
            name = anchor.attr('data-name');
            this.grpUserList = new VXUserList();
            this.grpUserList.url  = "service/xusers/"+ this.groupId +"/users";
            this.grpUserList.setPageSize(100);
            this.grpUserList.fetch({
                async : true,
                cache : false,
                reset : true,
            }).then(function(){
                XAUtil.blockUI('unblock');
                totalRecords = this.state.totalRecords;
                var title =  "<h4>User's List: " + _.escape(name) + "</h4>";
                    _.each(that.grpUserList.models , function(model){
                        msg +='<span class="link-tag userLists span-margin setEllipsis" title="'+ _.escape(model.get('name')) +'"><a href="#!/user/'+ model.id+'">'+ _.escape(model.get('name')) + '</a></span>';
                        that.copyUserLists.push(model.get('name'));
                    });
                    var html = '<div class="row">\
                                    <div class="col-md-12">\
                                        <input type="text" data-id="userInput" placeholder="Search Users" class= "users-list-search">\
                                        <div class="pull-right link-tag copyUsers btn btn-sm" title="Copy All Users Name"><i class="fa-fw fa fa-copy"></i></div>\
                                    </div>\
                                </div>';
                    if(totalRecords > 100){
                        var showAllUserbtn = '<button class="btn btn-sm showMore" data-id="'+ that.groupId +'" data-id="showMore" title="Show All Users">Show All Users</button>'
                        infoMsg = '<div class="alert alert-warning infoWidth">'+localization.tt('msg.showInitialHundredUser')+showAllUserbtn+'</div>'
                    }
                    if(_.isEmpty(msg)){
                            content = localization.tt("msg.noUserFoundText");
                    }else{
                            content = infoMsg + html + '<div class="usernames clearfix">' + msg +'</div>';
                    }
                    var modal = new Backbone.BootstrapModal({
                        animate : true,
                        content	: content,
                        title   : title,
                        okText  : localization.tt("lbl.ok"),
                        allowCancel : true,
                        focusOk : false
                    }).open();
                    modal.$el.find('.cancel').hide();
                    modal.$el.find('.copyUsers').on("click", function(e){
                        XAUtil.copyToClipboard(e , that.copyUserLists);
                    })
                    that.showAllUser(modal, totalRecords, msg);
            })
        },

        showAllUser : function(modal, totalRecords, msg){
            var that = this; that.copyUserLists = [];
            var filterUserLists = _.clone(modal.$content.find('span'));
            _.each(filterUserLists , function(model){
                that.copyUserLists.push(model.innerText);
            });
            modal.$content.find('.showMore').on("click" ,function(){
                modal.$el.find('.modal-body').scrollTop(0);
                modal.$el.find('.modal-body').addClass('pointer-event');
                modal.$el.find('.modal-body').append('<div class="loaderForModal"></div>');
                this.grpUserList = new VXUserList();
                this.grpUserList.url  = "service/xusers/"+ that.groupId +"/users";
                this.grpUserList.setPageSize(totalRecords);
                this.grpUserList.fetch({
                    async : true,
                    cache : false,
                    reset : true,
                }).then(function(){
                    var tag =""; that.copyUserLists = [];
                    modal.$content.find('.showMore').attr('disabled',true);
                    modal.$el.find('.modal-body').removeClass('pointer-event');
                    modal.$el.find('.loaderForModal').remove();
                    _.each(this.models, function(m){
                        tag +='<span class="link-tag userLists span-margin setEllipsis" title="'+ _.escape(m.get('name')) +'" ><a href="#!/user/'+ m.get('id')+'" >'+ _.escape(m.get('name')) + '</a></span>';
                        that.copyUserLists.push(m.get('name'));
                    });
                    modal.$el.find(".usernames").empty();
                    modal.$el.find(".usernames").append(tag);
                    filterUserLists = _.clone(modal.$content.find('span'));
                    msg = tag;
                })
            });
            modal.$el.find('[data-id="userInput"]').on("keyup" ,function(e){
                var input, users= [];
                modal.$el.find('.copyUsers').show()
                var value = e.currentTarget.value;
                users = msg;
                if(!_.isEmpty(value)){
                    that.copyUserLists=[];
                    users = _.filter(filterUserLists, function(v) {
                       if(v.innerText.toLowerCase().indexOf(value.toLowerCase()) > -1){
                           that.copyUserLists.push(v.innerText);
                           return v;
                        }
                    });
                }
                modal.$el.find(".usernames").empty();
                if(_.isEmpty(users)){
                    users = "No users found.";
                    modal.$el.find('.copyUsers').hide()
                }
                modal.$el.find(".usernames").append(users);
                if(_.isEmpty(value)){
                    that.showAllUser(modal, totalRecords, msg);
                }
            })
        },

		onUserGroupDeleteSuccess: function(jsonUsers,collection){
			_.each(jsonUsers.vXStrings,function(ob){
				var model = _.find(collection.models, function(mo){
					if(mo.get('name') === ob.value)
						return mo;
					});
				collection.remove(model.get('id'));
			});
		},

		onDeleteUser: function(e){

			var that = this;
                        var collection = that.showUsers ? that.collection : (that.showGroups ? that.groupList : that.roleList);
			var selArr = [];
			var message = '';
			collection.each(function(obj){
				if(obj.selected){
	                selArr.push({"value" : obj.get('name') , "id" : obj.get('id')});
	            }
            });
			var  vXStrings = [];
			var jsonUsers  = {};
			for(var i in selArr) {
				var itemName = selArr[i].value , itemId = selArr[i].id;
				vXStrings.push({
					"value" : itemName,
					"id" : itemId
				});
			}
			jsonUsers.vXStrings = vXStrings;

			var total_selected = jsonUsers.vXStrings.length;
			if(total_selected == 0){
				if(that.showUsers){
					XAUtil.alertBoxWithTimeSet(localization.tt('msg.noDeleteUserRow'));
                                }else if(that.showGroups){
					XAUtil.alertBoxWithTimeSet(localization.tt('msg.noDeleteGroupRow'));
                                }else {
                                        XAUtil.alertBoxWithTimeSet(localization.tt('msg.noDeleteRoleRow'));
				}
				return;
			}
            if(total_selected == 1) {
                message = 'Are you sure you want to delete '+(that.showUsers ? 'user': (that.showGroups ? 'group' : 'role'))+' \''+ _.escape( jsonUsers.vXStrings[0].value )+'\'?';
			}
			else {
                                message = 'Are you sure you want to delete '+total_selected+' '+(that.showUsers ? 'user': (that.showGroups ? 'group' : 'role'))+'?';
			}
			if(total_selected > 0){
				XAUtil.confirmPopup({
					msg: message,
					callback: function(){
						XAUtil.blockUI();
						if(that.showUsers){
							var model = new VXUser();
                            var count = 0 , notDeletedUserName = "", errorMsgForNotDeletedUsers = "";
                            _.map(jsonUsers.vXStrings , function(m){
                            	model.deleteUsers(m.id,{
                            		success: function(response,options){
                            			count += 1;
                            			that.userCollection(jsonUsers.vXStrings.length, count, notDeletedUserName, errorMsgForNotDeletedUsers)
                            		},
                            		error:function(response,options){
                            			count += 1;
                            			if(response.responseJSON && response.responseJSON.msgDesc){
                            				errorMsgForNotDeletedUsers += _.escape(response.responseJSON.msgDesc) +".<br>"
                            			}else{
                            				notDeletedUserName += _.escape(m.value) + ", ";
                            			}
                            			that.collection.find(function(model){return model.get('name') === m.value}).selected = false
                            			that.userCollection(jsonUsers.vXStrings.length, count, notDeletedUserName, errorMsgForNotDeletedUsers)
                            		}
                            	});
                            });
                        }else if(that.showGroups) {
							var model = new VXGroup();
                            var count = 0, notDeletedGroupName ="", errorMsgForNotDeletedGroups = "";
                            _.map(jsonUsers.vXStrings, function(m){
                            	model.deleteGroups(m.id,{
                            		success: function(response){
                            			count += 1;
                            			that.groupCollection(jsonUsers.vXStrings.length,count,notDeletedGroupName, errorMsgForNotDeletedGroups)
                            		},
                            		error:function(response,options){
                            			count += 1;
                                        if(response.responseJSON && response.responseJSON.msgDesc){
                                            errorMsgForNotDeletedGroups += _.escape(response.responseJSON.msgDesc) +"<br>"
                                        }else{
                                            notDeletedGroupName += _.escape(m.value) + ", ";
                                        }
                            			that.groupList.find(function(model){return model.get('name') === m.value}).selected = false
                            			that.groupCollection(jsonUsers.vXStrings.length,count, notDeletedGroupName, errorMsgForNotDeletedGroups)
                            		}
                            	})
                            });
                                                }else {
                                                        var model = new VXRole();
                            var count = 0, notDeletedRoleName ="", errorMsgForNotDeletedRoles = "";
                            _.map(jsonUsers.vXStrings, function(m){
                                model.deleteRoles(m.id,{
                                        success: function(response){
                                                count += 1;
                                                that.roleCollection(jsonUsers.vXStrings.length,count,notDeletedRoleName, errorMsgForNotDeletedRoles);
                                        },
                                        error:function(response,options){
                                                count += 1;
                                        if(response.responseJSON && response.responseJSON.msgDesc){
                                            errorMsgForNotDeletedRoles += _.escape(response.responseJSON.msgDesc) +"<br>"
                                        }else{
                                            notDeletedRoleName += _.escape(m.value) + ", ";
                                        }
                                                that.roleList.find(function(model){return model.get('name') === m.value}).selected = false
                                                that.roleCollection(jsonUsers.vXStrings.length,count, notDeletedRoleName, errorMsgForNotDeletedRoles)
                                        }
                                })
                            });
						}
					}
				});
			}
		},
        userCollection : function(numberOfUser, count, notDeletedUserName, errorMsgForNotDeletedUsers){
                if(count == numberOfUser){
                        this.collection.getFirstPage({fetch:true});
                        this.collection.selected = {};
                        this.$el.find('table input[type="checkbox"]').prop('checked',false);
                        XAUtil.blockUI('unblock');
                        if(notDeletedUserName === "" && _.isEmpty(errorMsgForNotDeletedUsers)){
                                XAUtil.notifySuccess('Success','User deleted successfully!');
                        }else{
                            var msg = "";
                            if(!_.isEmpty(notDeletedUserName)){
                                msg = 'Error occurred during deleting Users: '+ notDeletedUserName.slice(0 , -2);
                            }
                            XAUtil.notifyError('Error', errorMsgForNotDeletedUsers + msg);
                        }
                }
        },
        groupCollection : function(numberOfGroup, count ,notDeletedGroupName, errorMsgForNotDeletedGroups){
                if(count == numberOfGroup){
                        this.groupList.getFirstPage({fetch:true});
                        this.groupList.selected  = {};
                        this.$el.find('table input[type="checkbox"]').prop('checked',false)
                        XAUtil.blockUI('unblock');
                        if(notDeletedGroupName === "" && _.isEmpty(errorMsgForNotDeletedGroups)){
                                XAUtil.notifySuccess('Success','Group deleted successfully!');
                        } else {
                            var msg = "";
                            if(!_.isEmpty(notDeletedGroupName)){
                                msg = 'Error occurred during deleting Groups: '+ notDeletedGroupName.slice(0 , -2);
                            }
                            XAUtil.notifyError('Error', errorMsgForNotDeletedGroups + msg);
                        }
                }
        },
        roleCollection : function(numberOfRole, count ,notDeletedRoleName, errorMsgForNotDeletedRoles){
                if(count == numberOfRole){
                        this.roleList.getFirstPage({fetch:true});
                        this.roleList.selected  = {};
                        this.$el.find('table input[type="checkbox"]').prop('checked',false)
                        XAUtil.blockUI('unblock');
                        if(notDeletedRoleName === "" && _.isEmpty(errorMsgForNotDeletedRoles)){
                                XAUtil.notifySuccess('Success','Role deleted successfully!');
                        } else {
                            var msg = "";
                            if(!_.isEmpty(notDeletedRoleName)){
                                msg = 'Error occurred during deleting Roles: '+ notDeletedRoleName.slice(0 , -2);
                            }
                            XAUtil.notifyError('Error', errorMsgForNotDeletedRoles + msg);
                        }
                }
        },
		addVisualSearch : function(){
			var that = this;
			var coll,placeholder;
			var searchOpt = [], serverAttrName = [];
			if(this.showUsers){
				placeholder = localization.tt('h.searchForYourUser');	
				coll = this.collection;
				searchOpt = ['User Name','Email Address','Visibility', 'Role','User Source','User Status', 'Sync Source'];//,'Start Date','End Date','Today'];
				var userRoleList = _.map(XAEnums.UserRoles,function(obj,key){return {label:obj.label,value:key};});
                serverAttrName  = [{text : "User Name", label :"name", urlLabel : "userName"},
                                    {text : "Email Address", label :"emailAddress", urlLabel : "emailAddress"},
                                    {text : "Role", label :"userRole", 'multiple' : true, 'optionsArr' : userRoleList, urlLabel : "role"},
                                    {text : "Visibility", label :"isVisible", 'multiple' : true, 'optionsArr' : XAUtil.enumToSelectLabelValuePairs(XAEnums.VisibilityStatus), urlLabel : "visibility"},
                                    {text : "User Source", label :"userSource", 'multiple' : true, 'optionsArr' : XAUtil.enumToSelectLabelValuePairs(XAEnums.UserTypes), urlLabel : "userSource"},
                                    {text : "User Status", label :"status", 'multiple' : true, 'optionsArr' : XAUtil.enumToSelectLabelValuePairs(XAEnums.ActiveStatus), urlLabel : "userStatus"},
                                    {text : "Sync Source", label :"syncSource", urlLabel : "syncSource"}
                                ];
            } else if(this.showGroups){
				placeholder = localization.tt('h.searchForYourGroup');
				coll = this.groupList;
				searchOpt = ['Group Name','Group Source', 'Visibility', 'Sync Source'];//,'Start Date','End Date','Today'];
                serverAttrName  = [{text : "Group Name", label :"name", urlLabel : "groupName"},
                                    {text : "Visibility", label :"isVisible", 'multiple' : true, 'optionsArr' : XAUtil.enumToSelectLabelValuePairs(XAEnums.VisibilityStatus), urlLabel : "visibility"},
                                    {text : "Group Source", label :"groupSource", 'multiple' : true, 'optionsArr' : XAUtil.enumToSelectLabelValuePairs(XAEnums.GroupTypes), urlLabel : "groupSource"},
                                    {text : "Sync Source", label :"syncSource", urlLabel : "syncSource"}];
            } else{
                placeholder = localization.tt('h.searchForYourRole');
                coll = this.roleList;
                searchOpt = ['Role Name','User Name', 'Group Name', /*Role ID*/];//,'Start Date','End Date','Today'];
                serverAttrName  = [{text : "Role Name", label :"roleNamePartial", urlLabel : "roleName"},
                                   {text : "User Name", label :"userNamePartial", urlLabel : "userName"},
                                   {text : "Group Name", label :"groupNamePartial", urlLabel : "groupName"},
                                ];
            }
            var query = '';
            if(!_.isUndefined(this.urlQueryParams) && !_.isEmpty(this.urlQueryParams)) {
                var urlQueryParams = XAUtil.changeUrlToSearchQuery(this.urlQueryParams);
                _.map(urlQueryParams, function(val , key) {
                    if (_.some(serverAttrName, function(m){return m.urlLabel == key})) {
                        query += '"'+XAUtil.filterKeyForVSQuery(serverAttrName, key)+'":"'+val+'"';
                    }
                });
            }
			var pluginAttr = {
				      placeholder :placeholder,
				      container : this.ui.visualSearch,
				      query     : query,
                                      callbacks :  {
				    	  valueMatches :function(facet, searchTerm, callback) {
								switch (facet) {
									case 'Role':
                                        var userRoles ={};
                                        _.map(XAUtil.getUserDataParams().userRoleList, function(obj){
                                                userRoles[obj] = XAEnums.UserRoles[obj];
                                        })
                                        var roles = XAUtil.hackForVSLabelValuePairs(userRoles);
                                        callback(roles);
										break;
									case 'User Source':
										callback(XAUtil.hackForVSLabelValuePairs(XAEnums.UserTypes));
										break;	
									case 'Group Source':
										callback(XAUtil.hackForVSLabelValuePairs(XAEnums.GroupTypes));
										break;		
									case 'Visibility':
										callback(XAUtil.hackForVSLabelValuePairs(XAEnums.VisibilityStatus));
										break;
									case 'User Status':
//										callback(XAUtil.hackForVSLabelValuePairs(XAEnums.ActiveStatus));
										callback(that.getActiveStatusNVList());
										break;
									case 'Sync Source':
										callback( _.map(XAEnums.UserSyncSource, function(obj){ return obj.label; }) );
										break;
									/*case 'Start Date' :
										setTimeout(function () { XAUtil.displayDatepicker(that.ui.visualSearch, callback); }, 0);
										break;
									case 'End Date' :
										setTimeout(function () { XAUtil.displayDatepicker(that.ui.visualSearch, callback); }, 0);
										break;
									case 'Today'	:
										var today = Globalize.format(new Date(),"yyyy/mm/dd");
										callback([today]);
										break;*/
								}     
			            	
							}
				      }
				};
			XAUtil.addVisualSearch(searchOpt,serverAttrName, coll,pluginAttr);
		},
		getActiveStatusNVList : function() {
			var activeStatusList = _.filter(XAEnums.ActiveStatus, function(obj){
				if(obj.label != XAEnums.ActiveStatus.STATUS_DELETED.label)
					return obj;
			});
			return _.map(activeStatusList, function(status) { return { 'label': status.label, 'value': status.label}; })
		},
		onShowMore : function(e){
                        var name = $(e.currentTarget).attr('data-name');
                        var id = $(e.currentTarget).attr('model-'+name+'-id');
                        this.rTableList.$el.find('[model-'+name+'-id="'+id+'"]').show();
                        $('[data-id="showLess"][model-'+name+'-id="'+id+'"]').show();
                        $('[data-id="showMore"][model-'+name+'-id="'+id+'"]').hide();
                        $('[data-id="showMore"][model-'+name+'-id="'+id+'"]').parents('div[data-id="groupsDiv"]').addClass('set-height-groups');
		},
		onShowLess : function(e){
                        var name = $(e.currentTarget).attr('data-name');
                        var id = $(e.currentTarget).attr('model-'+name+'-id');
                        this.rTableList.$el.find('[model-'+name+'-id="'+id+'"]').slice(4).hide();
                        $('[data-id="showLess"][model-'+name+'-id="'+id+'"]').hide();
                        $('[data-id="showMore"][model-'+name+'-id="'+id+'"]').show();
                        $('[data-id="showMore"][model-'+name+'-id="'+id+'"]').parents('div[data-id="groupsDiv"]').removeClass('set-height-groups')
		},
		checkRoleKeyAdmin : function() {
			if(SessionMgr.isKeyAdmin()){
				this.ui.addNewBtnDiv.children().hide()
			}
		},
        onSyncDetailes : function(e){
            var that = this;
            if (e.currentTarget.dataset.for =="users") {
                var syncData = this.collection.models.find(function(m){return m.id == e.currentTarget.id});
            } else {
                var syncData = this.groupList.models.find(function(m){return m.id == e.currentTarget.id});
            }
            var SyncSourceView = Backbone.Marionette.ItemView.extend({
                template : require('hbs!tmpl/reports/UserSyncInfo_tmpl'),
                templateHelpers:function(){
                   return {'syncSourceInfo' : XAViewUtils.syncUsersGroupsDetails(this)};
                },
                initialize: function(){
                },
                onRender: function(){}
            });
            var modal = new Backbone.BootstrapModal({
                animate : true,
                content     : new SyncSourceView({model : syncData}),
                title: localization.tt("h.syncDetails"),
                okText :localization.tt("lbl.ok"),
                allowCancel : true,
                escape : true,
                focusOk : false
            }).open();
            modal.$el.find('.cancel').hide();
        },

		/** all post render plugin initialization */
		initializePlugins: function(){
		},

		/** on close */
		onClose: function(){
			XAUtil.allowNavigation();
            XAUtil.removeUnwantedDomElement();
		}

	});

	return UserTableLayout; 
});
