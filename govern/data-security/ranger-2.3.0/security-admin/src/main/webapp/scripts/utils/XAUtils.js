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

	var XAEnums = require('utils/XAEnums');
	var localization = require('utils/XALangSupport');
	var XAUtils = {};
	var notify = require('bootstrap-notify');
	var bootbox = require('bootbox');
    var moment = require('moment');

	// ///////////////////////////////////////////////////////
	// Enum utility methods
	// //////////////////////////////////////////////////////
	/**
	 * Get enum for the enumId
	 * 
	 * @param {integer}
	 *            enumId - The enumId
	 */
	XAUtils.getEnum = function(enumId) {
		if (!enumId || enumId.length < 1) {
			return "";
		}
		// check if the enums are loaded
		if (!XAEnums[enumId]) {
			return "";
		}
		return XAEnums[enumId];
	};

	/**
	 * Get enum by Enum and value
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 * @param {integer}
	 *            value - The value
	 */
	XAUtils.enumElementByValue = function(myEnum, value) {
		var element = _.detect(myEnum, function(element) {
			return element.value == value;
		});
		return element;
	};

	/**
	 * Get enum by Enum and name, value
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 * @param {string}
	 *            propertyName - The name of key
	 * @param {integer}
	 *            propertyValue - The value
	 */
	XAUtils.enumElementByPropertyNameValue = function(myEnum, propertyName,
			propertyValue) {
		for ( var element in myEnum) {
			if (myEnum[element][propertyName] == propertyValue) {
				return myEnum[element];
			}
		}
		return null;
	};

	/**
	 * Get enum value for given enum label
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 * @param {string}
	 *            label - The label to search for in the Enum
	 */
	XAUtils.enumLabelToValue = function(myEnum, label) {
		var element = _.detect(myEnum, function(element) {
			return element.label == label;
		});
		return (typeof element === "undefined") ? "--" : element.value;
	};

	/**
	 * Get enum label for given enum value
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 * @param {integer}
	 *            value - The value
	 */
	XAUtils.enumValueToLabel = function(myEnum, value) {
		var element = _.detect(myEnum, function(element) {
			return element.value == value;
		});
		return (typeof element === "undefined") ? "--" : element.label;
	};

	/**
	 * Get enum label tt string for given Enum value
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 * @param {integer}
	 *            value - The value
	 */
	XAUtils.enumValueToLabeltt = function(myEnum, value) {
		var element = _.detect(myEnum, function(element) {
			return element.value == value;
		});
		return (typeof element === "undefined") ? "--" : element.tt;
	};

	/**
	 * Get NVpairs for given Enum to be used in Select
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 */
	XAUtils.enumToSelectPairs = function(myEnum) {
		return _.map(myEnum, function(o) {
			return {
				val : o.value,
				label : o.label
			};
		});
	};

	/**
	 * Get NVpairs for given Enum
	 * 
	 * @param {Object}
	 *            myEnum - The enum
	 */
	XAUtils.enumNVPairs = function(myEnum) {
		var nvPairs = {
			' ' : '--Select--'
		};

		for ( var name in myEnum) {
			nvPairs[myEnum[name].value] = myEnum[name].label;
		}

		return nvPairs;
	};

	/**
	 * Get array NV pairs for given Array
	 * 
	 * @param {Array}
	 *            myArray - The eArraynum
	 */
	XAUtils.arrayNVPairs = function(myArray) {
		var nvPairs = {
			' ' : '--Select--'
		};
		_.each(myArray, function(val) {
			nvPairs[val] = val;
		});
		return nvPairs;
	};
   //	Search Info it give popover box
   XAUtils.searchInfoPopover = function(myArray, $infoEle, placement){
        var msg = "<span> Wildcard searches ( for example using * or ? ) are not currently supported.</span>";
		myArray.map(function(m){
                   msg += '<div><span><b>'+m.text+' : </b></span><span>'+m.info+'</span></div>'
                });
        $infoEle.popover({ trigger: "manual" , html: true, animation:false, content: msg, container:'body', placement: placement})
                           .on("mouseenter", function () {
                               var _this = this;
                               $(this).popover("show");
                               $(".popover").on("mouseleave", function () {
                                   $(_this).popover('hide');
                               });
                           }).on("mouseleave", function () {
                               var _this = this;
                               setTimeout(function () {
                                   if (!$(".popover:hover").length) {
                                       $(_this).popover("hide");
                                   }
                               }, 300);
               });
   };


	/**
	 * Notify Info the given title / text
	 * 
	 * @param {string}
	 *            text - The text
	 * @param {string}
	 *            type - The type
	 * @param {object}
	 *            text - Plugin options
	 */
	XAUtils.notifyInfo = function(type, text, options) {
		$.notify({
			icon: 'fa-fw fa fa-exclamation-circle',
			title: '<strong>Info!</strong>',
			message: text
		});
	};

	/**
	 * Notify Info the given title / text
	 * 
	 * @param {string}
	 *            text - The text
	 * @param {string}
	 *            type - The type
	 * @param {object}
	 *            text - Plugin options
	 */
	XAUtils.notifyError = function(type, text, options) {
		$.notify({
			icon: 'fa-fw fa fa-exclamation-triangle',
			title: '<strong>Error!</strong>',
			message: text
		},{
			type: 'danger',
		});
	};

	/**
	 * Notify Info the given title / text
	 * 
	 * @param {string}
	 *            text - The text
	 * @param {string}
	 *            type - The type
	 * @param {object}
	 *            text - Plugin options
	 */
	XAUtils.notifySuccess = function(type, text, options) {
		$.notify({
			icon: 'fa-fw fa fa-check-circle',
			title: '<strong>Success!</strong>',
			message: text
		},{
			type: 'success'
		});
	};

	/**
	 * Convert new line to <br />
	 * 
	 * @param {string}
	 *            str - the string to convert
	 */
	XAUtils.nl2br = function(str) {
		if (!str)
			return '';
		return str.replace(/\n/g, '<br/>').replace(/[\r\t]/g, " ");
	};

	/**
	 * Convert <br />
	 * to new line
	 * 
	 * @param {string}
	 *            str - the string to convert
	 */
	XAUtils.br2nl = function(str) {
		if (!str)
			return '';
		return str.replace(/\<br(\s*\/|)\>/gi, '\n');
	};

	/**
	 * Escape html chars
	 * 
	 * @param {string}
	 *            str - the html string to escape
	 */
	XAUtils.escapeHtmlChar = function(str) {
		if (!str)
			return '';
		str = str.replace(/&/g, "&amp;");
		str = str.replace(/>/g, "&gt;");
		str = str.replace(/</g, "&lt;");
		str = str.replace(/\"/g, "&quot;");
		str = str.replace(/'/g, "&#039;");
		return str;
	};

	/**
	 * nl2br and Escape html chars
	 * 
	 * @param {string}
	 *            str - the html string
	 */
	XAUtils.nl2brAndEscapeHtmlChar = function(str) {

		if (!str)
			return '';
		var escapedStr = escapeHtmlChar(str);
		var finalStr = nl2br(str);
		return finalStr;
	};

	/**
	 * prevent navigation with msg and call callback
	 * 
	 * @param {String}
	 *            msg - The msg to show
	 * @param {function}
	 *            callback - The callback to call
	 */
	XAUtils.preventNavigation = function(msg, $form) {
		window._preventNavigation = true;
		window._preventNavigationMsg = msg;
		$("body a, i[class^='fa-fw fa fa-']").on("click.blockNavigation", function(e) {
			XAUtils.preventNavigationHandler.call(this, e, msg, $form);
		});
	};

	/**
	 * remove the block of preventNavigation
	 */
	XAUtils.allowNavigation = function() {
		window._preventNavigation = false;
		window._preventNavigationMsg = undefined;
		$("body a, i[class^='fa-fw fa fa-']").off('click.blockNavigation');
	};

	XAUtils.preventNavigationHandler = function(e, msg, $form) {
		var formChanged = false;
		var target = this;
		if (!_.isUndefined($form))
			formChanged = $form.find('.dirtyField').length > 0 ? true : false;
		if (!$(e.currentTarget).hasClass("_allowNav") && formChanged) {

			e.preventDefault();
			e.stopImmediatePropagation();
			bootbox.dialog(
			{
				message: msg,
				buttons: {
				    noclose: {
				        "label" : localization.tt('btn.stayOnPage'),
						"className" : "btn-success btn-sm",
						"callback" : function() {
						}
				    },
				    cancel: {
						"label" : localization.tt('btn.leavePage'),
						"className" : "btn-danger btn-sm",
						"callback" : function() {
							XAUtils.allowNavigation();
							target.click();
						}
					}
				}
			});
			return false;
		}
	};

	/*
	 * icon Info
	 */
	XAUtils.errorsInfoPopover = function(filed, msg) {
		filed.popover({
			content : '<span class="popoverTextMsg" >'+msg+'</span>',
			html    : true,
			trigger : 'hover',
			placement : 'right',
			container : 'body'

		 })
	};
	
	/**
	 * Bootbox wrapper for alert
	 * 
	 * @param {Object}
	 *            params - The params
	 */
	XAUtils.alertPopup = function(params) {
		bootbox.hideAll();
		if (params.callback == undefined) {
			bootbox.alert(params.msg);
		} else {
			bootbox.alert(params.msg, params.callback);
		}
	};
     
	//Alert box with time set 
	XAUtils.alertBoxWithTimeSet = function(msg) {
		var alert = bootbox.alert(msg);
    	return(setTimeout(function(){alert.modal('hide'); }, 4000));
	}
	
	/**
	 * Bootbox wrapper for confirm
	 * 
	 * @param {Object}
	 *            params - The params
	 */
	XAUtils.confirmPopup = function(params) {
		bootbox.hideAll();
		bootbox.confirm(params.msg, function(result) {
			if (result) {
				params.callback();
			}
		});
	};

	XAUtils.filterResultByIds = function(results, selectedVals) {
		return _.filter(results, function(obj) {
			if ($.inArray(obj.id, selectedVals) < 0)
				return obj;

		});
	};
	XAUtils.filterResultByText = function(results, selectedVals) {
		return _.filter(results, function(obj) {
			if ($.inArray(obj.text, selectedVals) < 0)
				return obj;

		});
	};
	XAUtils.scrollToField = function(field) {
		$("html, body").animate({
			scrollTop : field.position().top - 80
		}, 1100, function() {
			field.focus();
		});
	};
	XAUtils.blockUI = function(options) {
		var Opt = {
			autoUnblock : false,
			clickUnblock : false,
			bgPath : 'images/',
			content : '<img src="images/blockLoading.gif" > Please wait..',
			css : {}
		};
		options = _.isUndefined(options) ? Opt : options;
		$.msg(options);
	};
        XAUtils.showMoreLessBtnForGroupsUsersRoles = function(rawValue , type) {
		var showMoreLess = false, id;
		if (_.isArray(rawValue))
			rawValue = new Backbone.Collection(rawValue);
		if (!_.isUndefined(rawValue) && rawValue.models.length > 0) {
			var groupArr = _.uniq(_.compact(_.map(rawValue.models, function(m,
					i) {
                                if (m.has('entityName'))
                                        return _.escape(m.get('entityName'));
			})));
			if (groupArr.length > 0) {
				if (rawValue.first().has('resourceId'))
					id = rawValue.first().get('resourceId');
				else
                                        id = rawValue.first().get('modelId');
			}
			var newGroupArr = _.map(groupArr, function(name, i) {
				if (i >= 4)
                                        return '<span class="badge badge-info float-left-margin-2" data-name='+type+' model-'+ type +'-id="'
							+ id + '" style="display:none;">' + name
							+ '</span>';
				else if (i == 3 && groupArr.length > 4) {
					showMoreLess = true;
                                        return '<span class="badge badge-info float-left-margin-2" data-name='+type+' model-'+ type +'-id="'
							+ id + '">' + name + '</span>';
				} else
                                        return '<span class="badge badge-info float-left-margin-2" data-name='+type+' model-'+ type +'-id="'
							+ id + '">' + name + '</span>';
			});
			if (showMoreLess) {
				newGroupArr
                                                .push('<span class="float-left-margin-2"><a href="javascript:void(0);" data-id="showMore" class="" data-name='+type+' model-'+ type +'-id="'
								+ id
                                                                + '"><code style=""> + More..</code></a></span><span class="float-left-margin-2"><a href="javascript:void(0);" data-id="showLess" class="" data-name='+type+' model-'+ type +'-id="'
								+ id
								+ '" style="display:none;"><code> - Less..</code></a></span>');
			}
			newGroupArr.unshift('<div data-id="groupsDiv">');
			newGroupArr.push('</div>');
			return newGroupArr.length ? newGroupArr.join(' ') : '--';
		} else
			return '--';
	};
        XAUtils.showGroupsOrUsersForPolicy = function(rawValue, model, showType, rangerServiceDefModel) {
		var showMoreLess = false, groupArr = [], items = [];
		var itemList = ['policyItems','allowExceptions','denyPolicyItems','denyExceptions','dataMaskPolicyItems','rowFilterPolicyItems']
		if(!_.isUndefined(rangerServiceDefModel)){
			if(!this.showAllPolicyItems(rangerServiceDefModel, model)){
				itemList = _.difference(itemList, ["allowExceptions", "denyPolicyItems", "denyExceptions"]);
			}
		}
		itemList = this.isAccessPolicy(model.get('policyType')) ? _.difference(itemList, ["dataMaskPolicyItems", "rowFilterPolicyItems"])
				: this.isMaskingPolicy(model.get('policyType')) ? _.difference(itemList, ["rowFilterPolicyItems"])
				: this.isRowFilterPolicy(model.get('policyType')) ? _.difference(itemList, ["dataMaskPolicyItems"]) : itemList; 
						
                var type = _.isUndefined(showType) || (showType == 'groups') ? 'groups' : ((showType == 'users') ? 'users' : 'roles');
		_.each(itemList, function(item){
		    if(!_.isUndefined(model.get(item)) && !_.isEmpty(model.get(item))) {
		    	items =_.union(items,  model.get(item))
		    }
		});
		_.each(items, function(perm) {
			groupArr = _.union(groupArr, perm[type])
		});
		if (_.isEmpty(items) || _.isEmpty(groupArr))
			return '--';
		var newGroupArr = _.map(groupArr, function(name, i) {
			if (i >= 4) {
				return '<span class="badge badge-info float-left-margin-2" policy-' + type
						+ '-id="' + model.id + '" style="display:none;">'
						+ _.escape(name) + '</span>';
			} else if (i == 3 && groupArr.length > 4) {
				showMoreLess = true;
				return '<span class="badge badge-info float-left-margin-2" policy-' + type
						+ '-id="' + model.id + '">' + _.escape(name) + '</span>';
			} else {
				return '<span class="badge badge-info float-left-margin-2" policy-' + type
						+ '-id="' + model.id + '">' + _.escape(name) + '</span>';
			}
		});
		if (showMoreLess) {
			newGroupArr
					.push('<span class="pull-left float-left-margin-2"><a href="javascript:void(0);" data-id="showMore" class="" policy-'
							+ type
							+ '-id="'
							+ model.id
							+ '"><code style=""> + More..</code></a></span><span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLess" class="" policy-'
							+ type
							+ '-id="'
							+ model.id
							+ '" style="display:none;"><code> - Less..</code></a></span>');
		}
		newGroupArr.unshift('<div data-id="groupsDiv">');
		newGroupArr.push('</div>');
		return newGroupArr.length ? newGroupArr.join(' ') : '--';

	};

	XAUtils.showGroupsOrUsers = function(rawValue, model, userOrGroups) {
                var showMoreLess = false, objArr, lastShowMoreCnt = 1, j = 1, listShownCnt = 1000;
		if (!_.isArray(rawValue) && rawValue.length == 0)
			return '--';
               // objArr = (userOrGroups == 'groups') ? _.pluck(rawValue, 'groupName') : _.pluck(rawValue, 'userName');
               objArr = rawValue;
		var newObjArr = _.map(objArr, function(name, i) {
			if (i >= 4) {
                                var eleStr = '', span = '<span class="badge badge-info float-left-margin-2" policy-' + userOrGroups
                                        + '-id="' + model.id +'">'
                                        +  _.escape(name) + '</span>';
                                if( (i + listShownCnt ) === (listShownCnt*j) + 4){
                                        eleStr = '<div data-id="moreSpans" style="display:none;">'+span;
                                        if(i == objArr.length - 1){
                                                eleStr += '</div>';
                                        }
                                        lastShowMoreCnt = ( listShownCnt*j) + 4;
                                        j++;
                                }else if(i === lastShowMoreCnt - 1 || i == objArr.length - 1){
                                        eleStr = span + '</div>';

                                }else{
                                        eleStr = span;
                                }
                                return eleStr;
			} else if (i == 3 && objArr.length > 4) {
				showMoreLess = true;
				return '<span class="badge badge-info float-left-margin-2" policy-' + userOrGroups
                                                + '-id="' + model.id + '">' +  _.escape(name) + '</span>';
			} else {
				return '<span class="badge badge-info float-left-margin-2" policy-' + userOrGroups
                                                + '-id="' + model.id + '">' +  _.escape(name) + '</span>';
			}
		});
		if (showMoreLess) {
			newObjArr
					.push('<span class="pull-left float-left-margin-2"><a href="javascript:void(0);" data-id="showMore" class="" policy-'
							+ userOrGroups
							+ '-id="'
							+ model.id
							+ '"><code style=""> + More..</code></a></span><span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLess" class="" policy-'
							+ userOrGroups
							+ '-id="'
							+ model.id
							+ '" style="display:none;"><code> - Less..</code></a></span>');
		}
		newObjArr.unshift('<div data-id="groupsDiv">');
		newObjArr.push('</div>');
		return newObjArr.length ? newObjArr.join(' ') : '--';
	};

	XAUtils.defaultErrorHandler = function(model, error) {
		var App = require('App');
		var vError = require('views/common/ErrorView');
		if(!_.isUndefined(model) && !_.isUndefined(model.modelName) 
				&&  model.modelName == XAEnums.ClassTypes.CLASS_TYPE_XA_ACCESS_AUDIT.modelName
				&& error.status !== 419){
			return;
		}
		if (error.status == 404) {
			App.rContent.show(new vError({
				status : error.status
			}));
		} else if (error.status == 401) {
			App.rContent.show(new vError({
				status : error.status
			}));
		} else if (error.status == 419) {
			if(!_.isNull(error.getResponseHeader("X-Rngr-Redirect-Url"))) {
				XAUtils.notifyError('error', 'Session Timeout')
				setTimeout( function(){
					window.location = error.getResponseHeader("X-Rngr-Redirect-Url");
				}, 4000);
			} else {
				window.location = 'login.jsp?sessionTimeout=true';
			}
		}
	};
	XAUtils.select2Focus = function(event) {
		if (/^select2-focus/.test(event.type)) {
			$(this).select2('open');
		}
	};
	XAUtils.makeCollForGroupPermission = function(model, listName) {
		var XAEnums = require('utils/XAEnums');
		var formInputColl = new Backbone.Collection();
		var that = this;
		// permMapList = [ {id: 18, groupId : 1, permType :5}, {id: 18, groupId
		// : 1, permType :4}, {id: 18, groupId : 2, permType :5} ]
		// [1] => [ {id: 18, groupId : 1, permType :5}, {id: 18, groupId : 1,
		// permType :4} ]
		// [2] => [ {id: 18, groupId : 2, permType :5} ]
		if (!model.isNew()) {
			if (!_.isUndefined(model.get(listName))) {
				var policyItems = model.get(listName);
				// var groupPolicyItems =
				// _.filter(policyItems,function(m){if(!_.isEmpty(m.groups))
				// return m;});
				_.each(policyItems, function(obj) {
                                        var groupNames = null, userNames = null, roleNames = null;
					if (!_.isEmpty(obj.groups))
						groupNames = obj.groups;
					if (!_.isEmpty(obj.users))
						userNames = obj.users;
                                        if (!_.isEmpty(obj.roles))
                                                roleNames = obj.roles;
					var m = new Backbone.Model({
						groupName : groupNames,
						userName : userNames,
						accesses : obj.accesses,
						conditions : obj.conditions,
						delegateAdmin : obj.delegateAdmin,
						editMode : true,
                                                roleName : roleNames,
					});
					if(that.isMaskingPolicy(model.get('policyType'))){
						m.set('dataMaskInfo', obj.dataMaskInfo)
					}
					if(that.isRowFilterPolicy(model.get('policyType'))){
						m.set('rowFilterInfo', obj.rowFilterInfo)
					}
					formInputColl.add(m);

				});
			}
		}
		return formInputColl;
	};

	XAUtils.makeCollForUserPermission = function(model, listName) {
		var XAEnums = require('utils/XAEnums');
		var coll = new Backbone.Collection();
		// permMapList = [ {id: 18, groupId : 1, permType :5}, {id: 18, groupId
		// : 1, permType :4}, {id: 18, groupId : 2, permType :5} ]
		// [1] => [ {id: 18, groupId : 1, permType :5}, {id: 18, groupId : 1,
		// permType :4} ]
		// [2] => [ {id: 18, groupId : 2, permType :5} ]
		if (!model.isNew()) {
			if (!_.isUndefined(model.get(listName))) {
				var policyItems = model.get(listName);
				var userPolicyItems = _.filter(policyItems, function(m) {
					if (!_.isEmpty(m.users))
						return m;
				});
				_.each(userPolicyItems, function(obj) {
					var m = new Backbone.Model({
						// userId : groupIds.join(','),
						userName : obj.users.join(','),
						// ipAddress : values[0].ipAddress,
						editMode : true,
						accesses : obj.accesses,
						conditions : obj.conditions
					});
					coll.add(m);

				});
			}
		}
		return coll;
	};
	XAUtils.checkDirtyField = function(arg1, arg2, $elem) {
		if (_.isEqual(arg1, arg2)) {
			$elem.removeClass('dirtyField');
		} else {
			$elem.addClass('dirtyField');
		}
	};
	XAUtils.checkDirtyFieldForToggle = function($el) {
		if ($el.hasClass('dirtyField')) {
			$el.removeClass('dirtyField');
		} else {
			$el.addClass('dirtyField');
		}
	};
	XAUtils.checkDirtyFieldForSelect2 = function($el, dirtyFieldValue, that) {
		if ($el.hasClass('dirtyField')
				&& _.isEqual($el.val(), dirtyFieldValue.toString())) {
			$el.removeClass('dirtyField');
		} else if (!$el.hasClass('dirtyField')) {
			$el.addClass('dirtyField');
			dirtyFieldValue = !_.isUndefined(that.value.values) ? that.value.values
					: '';
		}
		return dirtyFieldValue;
	};
	XAUtils.enumToSelectLabelValuePairs = function(myEnum) {
		return _.map(myEnum, function(o) {
			return {
				label : o.label,
				value : o.value + ''
			// category :'DHSS',
			};
		});
	};
	XAUtils.hackForVSLabelValuePairs = function(myEnum) {
		return _.map(myEnum, function(o) {
			return {
				label : o.label,
				value : o.label + ''
			// category :'DHSS',
			};
		});
	};
	XAUtils.addVisualSearch = function(searchOpt, serverAttrName, collection,
			pluginAttr) {
		var visualSearch, that = this;
		var supportMultipleItems = pluginAttr.supportMultipleItems || false;
                var multipleFacet = serverAttrName.filter(function(elem) {
			return elem['addMultiple'];
		}).map(function(elem) {
			return elem.text;
		});
		var search = function(searchCollection, collection) {
                        var params = {}, urlParams = {};
			if($('.popover')){
					$('.popover').remove();
			}
			searchCollection.each(function(m) {
                //For url params
                if(_.has(urlParams, m.get('category'))) {
                        var oldValue = urlParams[m.get('category')], newValue = m.get('value');
                        if (Array.isArray(oldValue)) {
                                // if it's a list, append to the end
                                oldValue.push(newValue);
                        } else {
                                // convert to a list
                                urlParams[m.get('category')] = [oldValue, newValue];
                        }
                } else {
                        urlParams[m.get('category')] = m.get('value')
                }
                var serverParamName = _.findWhere(serverAttrName, {
                        text : m.attributes.category
                });
                var extraParam = {};
                if (serverParamName && _.has(serverParamName, 'multiple') && serverParamName.multiple) {
                        extraParam[serverParamName.label] = XAUtils.enumLabelToValue(serverParamName.optionsArr, m.get('value'));
                        $.extend(params, extraParam);
                } else {
					if (!_.isUndefined(serverParamName)) {
						var oldValue = params[serverParamName.label];
						var newValue = m.get('value');
						if (oldValue && serverParamName.addMultiple) {
							// if a value is already there
							if (Array.isArray(oldValue)) {
								// if it's a list, append to the end
								oldValue.push(newValue);
							} else {
								// convert to a list
								params[serverParamName.label] = [oldValue, newValue];
							}
						} else {
							params[serverParamName.label] = newValue;
						}
					}
				}
			});
			collection.queryParams = $.extend(collection.queryParams, params);
			collection.state.currentPage = collection.state.firstPage;
            //Add urlLabel to URL
            var urlLabelParam = {};
            _.map(urlParams, function(attr, key) {
                _.filter(serverAttrName, function(val) {
                    if(val.text === key) {
                        return urlLabelParam[val.urlLabel] = attr
                    }
                })
            })

            if(!_.contains(["vXUsers","vXGroups","roles","vXModuleDef"], collection.modelAttrName)) {
                //set sortBy value to url
                if(!_.isUndefined(collection.queryParams) && collection.queryParams.sortBy && !_.isNull(collection.queryParams.sortBy)) {
                    var sortparams = _.pick(collection.queryParams, 'sortBy');
                    collection.state.order == 1 ? sortparams['sortType'] = "descending" : sortparams['sortType'] = "ascending";
                    urlLabelParam = _.extend(urlLabelParam, sortparams)
                }
                //set sortKey value to url
                if(!_.isUndefined(collection.state) && collection.state.sortKey && !_.isNull(collection.state.sortKey) && !_.contains(urlLabelParam, 'sortBy')) {
                    var sortparams = _.pick(collection.state, 'sortKey');
                    collection.state.order == 1 ? sortparams['sortType'] = "descending" : sortparams['sortType'] = "ascending";
                    urlLabelParam = _.extend(urlLabelParam, sortparams)
                }
                //set excludeServiceUser value to url
                if(!_.isUndefined(collection.queryParams) && _.has(collection.queryParams, 'excludeServiceUser')) {
                    var sortparams = _.pick(collection.queryParams, 'excludeServiceUser');
                    urlLabelParam = _.extend(urlLabelParam, sortparams)
                }
            }
            XAUtils.changeParamToUrlFragment(urlLabelParam, collection.modelName);
			collection.fetch({
				reset : true,
				cache : false,
				traditional: supportMultipleItems, // for sending multiple values without []
				error : function(coll, response, options) {
					that.blockUI('unblock');
                                        if(response && response.responseJSON && response.responseJSON.msgDesc){
                                                that.notifyError('Error', response.responseJSON.msgDesc);
                                        }else{
                                                that.notifyError('Error', localization.tt('msg.errorLoadingAuditLogs'));
					}

				}
			});
		};

		var callbackCommon = {
			search : function(query, searchCollection) {
				collection.VSQuery = query;
				search(searchCollection, collection);
			},
			clearSearch : function(callback) {
                                //Remove search history when click on clear search
                                if(!_.isUndefined(pluginAttr.type)){
                                        var App = require('App');
                                        App.vsHistory[pluginAttr.type] = [];
                                }
				_.each(serverAttrName, function(attr) {
					delete collection.queryParams[attr.label];
				});
				callback();
			},
			facetMatches : function(callback) {
				// console.log(visualSearch);
				var searchOptTemp = $.extend(true, [], searchOpt);
				visualSearch.searchQuery.each(function(m) {
					var cat = m.get('category');
					if ($.inArray(cat, searchOptTemp) >= 0 && $.inArray(cat, multipleFacet) < 0) {
						searchOptTemp.splice($.inArray(cat,
								searchOptTemp), 1);
					}
				});
				// visualSearch.options.readOnly = searchOptTemp.length <= 0 ?
				// true : false;
				callback(searchOptTemp, {
					preserveOrder : false
				});
			},
			removedFacet : function(removedFacet, searchCollection, indexObj) {
				// console.log(removedFacet);

				var removedFacetSeverName = _.findWhere(serverAttrName, {
					text : removedFacet.get('category')
				});
				if (!_.isUndefined(removedFacetSeverName)) {
					var queryValue = collection.queryParams[removedFacetSeverName.label];
					if ($.inArray(removedFacetSeverName.text, multipleFacet) && Array.isArray(queryValue)) {
						var idx = queryValue.indexOf(removedFacet.get("value"));
						if (idx != -1) {
							queryValue.splice(idx);
						}
					} else {
						delete collection.queryParams[removedFacetSeverName.label];
					}
					collection.state.currentPage = collection.state.firstPage;
				}
				// TODO Added for Demo to remove datapicker popups
				if (!_.isUndefined(visualSearch.searchBox.$el))
					visualSearch.searchBox.$el.parents('body').find(
							'.datepicker').remove();
			}
		// we can also add focus, blur events callback here..
		};
		pluginAttr.callbacks = $.extend(callbackCommon, pluginAttr.callbacks);
		// Initializing VisualSearch Plugin....
		visualSearch = VS.init($.extend(pluginAttr, {
			remainder : false
		}));

		if (visualSearch.searchQuery.length > 0) // For On Load Visual Search
			search(visualSearch.searchQuery, collection);

		return visualSearch;
	};

	XAUtils.displayDatepicker = function($el, facet, $date, callback) {
                var input = $el.find('.search_facet.is_editing input.search_facet_input');
                //disabling user enter value in date
                input.keypress(function(event) {
                        event.preventDefault();
                });
		$el.parents('body').find('.datepicker').hide();
		input.datepicker({
			autoclose : true,
		}).on('changeDate', function(ev) {
			callback(ev.date);
			input.datepicker("hide");
			var e = jQuery.Event("keydown");
			e.which = 13; // Enter
			$(this).trigger(e);
		});
		if (!_.isUndefined($date)) {
			if (facet == 'Start Date') {
				input.datepicker('setEndDate', $date);
			} else {
				input.datepicker('setStartDate', $date);
			}
		}
		input.datepicker('show');
		input.on('blur', function(e) {
			input.datepicker("hide");
			// $('.datepicker').remove();

		});
		// input.attr("readonly", "readonly");
		input.on('keydown', function(e) {
			if (e.which == 9 && e.shiftKey) {
				input.datepicker('setValue', new Date());
				input.trigger('change');
				input.datepicker("hide");
			}
			if (e.which == 13) {
				var e1 = jQuery.Event("keypress");
				e1.which = 13; // Enter
				$(this).trigger(e1);

			}
		});
		return input;
	};
	XAUtils.getPerms = function(policyType) {
		var permArr = [];
		switch (policyType) {
		case XAEnums.AssetType.ASSET_HDFS.value:
			permArr = [ 'XA_PERM_TYPE_READ', 'XA_PERM_TYPE_WRITE',
					'XA_PERM_TYPE_EXECUTE', 'XA_PERM_TYPE_ADMIN' ];
			break;
		case XAEnums.AssetType.ASSET_HIVE.value:
			permArr = [ 'XA_PERM_TYPE_SELECT', 'XA_PERM_TYPE_UPDATE',
					'XA_PERM_TYPE_CREATE', 'XA_PERM_TYPE_DROP',
					'XA_PERM_TYPE_ALTER', 'XA_PERM_TYPE_INDEX',
					'XA_PERM_TYPE_LOCK', 'XA_PERM_TYPE_ALL',
					'XA_PERM_TYPE_ADMIN' ];
			break;
		case XAEnums.AssetType.ASSET_HBASE.value:
			permArr = [ 'XA_PERM_TYPE_READ', 'XA_PERM_TYPE_WRITE',
					'XA_PERM_TYPE_CREATE', 'XA_PERM_TYPE_ADMIN' ];
			break;
		case XAEnums.AssetType.ASSET_KNOX.value:
			permArr = [ 'XA_PERM_TYPE_ALLOW', 'XA_PERM_TYPE_ADMIN' ];
			break;
		case XAEnums.AssetType.ASSET_STORM.value:
			permArr = [ 'XA_PERM_TYPE_ADMIN' ];
			/*
			 * permArr =
			 * ['XA_PERM_TYPE_SUBMIT_TOPOLOGY','XA_PERM_TYPE_FILE_UPLOAD','XA_PERM_TYPE_GET_NIMBUS',
			 * 'XA_PERM_TYPE_GET_CLUSTER_INFO','XA_PERM_TYPE_FILE_DOWNLOAD','XA_PERM_TYPE_KILL_TOPOLOGY',
			 * 'XA_PERM_TYPE_REBALANCE','XA_PERM_TYPE_ACTIVATE','XA_PERM_TYPE_DEACTIVATE','XA_PERM_TYPE_GET_TOPOLOGY_CONF',
			 * 'XA_PERM_TYPE_GET_TOPOLOGY','XA_PERM_TYPE_GET_USER_TOPOLOGY','XA_PERM_TYPE_GET_TOPOLOGY_INFO','XA_PERM_TYPE_UPLOAD_NEW_CREDENTIAL' ];
			 */
			break;
		}
		return permArr;
	};
	XAUtils.getPermHeaders = function(policyType, isGroup) {
		if (_.isUndefined(isGroup))
			isGroup = true;
		var permHeaders = isGroup ? [ localization.tt('lbl.selectGroup') ]
				: [ localization.tt('lbl.selectUser') ];

		switch (policyType) {
		case XAEnums.AssetType.ASSET_HDFS.value:
			permHeaders.push(localization.tt('lbl.read'), localization
					.tt('lbl.write'), localization.tt('lbl.execute'),
					localization.tt('lbl.admin'), '');
			break;
		case XAEnums.AssetType.ASSET_HIVE.value:
			permHeaders.push(localization.tt('lbl.select'), localization
					.tt('lbl.update'), localization.tt('lbl.create'),
					localization.tt('lbl.drop'), localization.tt('lbl.alter'),
					localization.tt('lbl.index'), localization.tt('lbl.lock'),
					localization.tt('lbl.all'), localization.tt('lbl.admin'),
					'');
			break;
		case XAEnums.AssetType.ASSET_HBASE.value:
			permHeaders.push(localization.tt('lbl.read'), localization
					.tt('lbl.write'), localization.tt('lbl.create'),
					localization.tt('lbl.admin'), '');
			break;
		case XAEnums.AssetType.ASSET_KNOX.value:
			permHeaders.push(localization.tt('lbl.ipAddress'), localization
					.tt('lbl.allow'), localization.tt('lbl.admin'), '');
			break;
		case XAEnums.AssetType.ASSET_STORM.value:
			permHeaders.push(localization.tt('lbl.actions'), localization
					.tt('lbl.admin'), '');
			break;
		}
		return permHeaders;
	};
	XAUtils.getStormActions = function() {
		return [ 'XA_PERM_TYPE_SUBMIT_TOPOLOGY', 'XA_PERM_TYPE_FILE_UPLOAD',
				'XA_PERM_TYPE_GET_NIMBUS', 'XA_PERM_TYPE_GET_CLUSTER_INFO',
				'XA_PERM_TYPE_FILE_DOWNLOAD', 'XA_PERM_TYPE_KILL_TOPOLOGY',
				'XA_PERM_TYPE_REBALANCE', 'XA_PERM_TYPE_ACTIVATE',
				'XA_PERM_TYPE_DEACTIVATE', 'XA_PERM_TYPE_GET_TOPOLOGY_CONF',
				'XA_PERM_TYPE_GET_TOPOLOGY', 'XA_PERM_TYPE_GET_USER_TOPOLOGY',
				'XA_PERM_TYPE_GET_TOPOLOGY_INFO',
				'XA_PERM_TYPE_UPLOAD_NEW_CREDENTIAL' ];
	};

	XAUtils.highlightDisabledPolicy = function(that) {
		var $el = that.rTableList.$el;
		var timerId = setInterval(function() {
			if ($el.find('tr td:last').text() != "No Policies found!") {
				_.each($el.find('tr td').find('.label-important'), function(a,
						b) {
					if ($(a).html() == "Disabled")
						console.log(that.$(a).parents('tr').addClass(
								'disable-policy'))
				}, that);
				clearInterval(timerId);
			}
			console.log('highlight disabled policy..');
		}, 5);
	};
	XAUtils.showAlerForDisabledPolicy = function(that) {
		if (!_.isUndefined(that.model.get('resourceStatus'))
				&& that.model.get('resourceStatus') == XAEnums.ActiveStatus.STATUS_DISABLED.value) {
			that.ui.policyDisabledAlert.show();
			that.$(that.rForm.el).addClass("policy-disabled");
		} else {
			that.ui.policyDisabledAlert.hide();
			that.$(that.rForm.el).removeClass("policy-disabled");
		}
	};
	XAUtils.customXEditableForPolicyCond = function(template,selectionList) {
		// $.fn.editable.defaults.mode = 'list-inline';

		var PolicyConditions = function(options) {
			this.init('policyConditions', options, PolicyConditions.defaults);
		};

		// inherit from Abstract input
		$.fn.editableutils.inherit(PolicyConditions,
				$.fn.editabletypes.abstractinput);

		$.extend(PolicyConditions.prototype, {
			render : function() {
				this.$input = this.$tpl.find('input, textarea');
				var pluginOpts = {
					tags : true,
					width : '220px',
					multiple : true,
					minimumInputLength : 1,
					tokenSeparators : [ ",", ";" ],
				}
				_.each(this.$input, function(elem,index){
					if($(elem).is('input')){
						pluginOpts.maximumSelectionSize = selectionList[index];
						$(elem).select2(pluginOpts);
				    }	
				})
						
			},

			value2str : function(value) {
				var str = '';
				if (value) {
					for ( var k in value) {
						str = str + k + ':' + value[k].toString() + ';';
					}
				}
				return str;
			},

			value2input : function(value) {
				_.each(value, function(val, name) {
					var elem = this.$input.filter('[name=' + name + ']');
					if((elem).is('input')){
						elem.select2('val',
								value[name]);
					}else{
						elem.val(value[name])
					}
					
				}, this);
			},

			input2value : function() {
				var obj = {};
				_.each(this.$input, function(input) {
					var name = input.name;
					if($(input).is('input')){
						var val = this.$input.filter('[name="' + name + '"]').select2('val');
					}else{
						var val = $(input).val();
					}
					obj[name] = val;
				}, this);

				return obj;
			},
			activate : function() {
				this.$input.first().focus()
			},
		});

		PolicyConditions.defaults = $.extend({},
				$.fn.editabletypes.abstractinput.defaults, {
					tpl : template,

					inputclass : ''
				});
		$.fn.editabletypes.policyConditions = PolicyConditions;
	};
	XAUtils.capitaliseFirstLetter = function(string) {
		return string.charAt(0).toUpperCase() + string.slice(1);
	};
	XAUtils.lowerCaseFirstLetter = function(string) {
		return string.charAt(0).toLowerCase() + string.slice(1);
	};
	XAUtils.getServicePoliciesURL = function(serviceId) {
		return "service/plugins/policies/service/" + serviceId;
	};
	XAUtils.getRangerServiceDef = function(name) {
		return "service/plugins/definitions/name/" + name;
	};
	XAUtils.getZonesURL = function(zoneId) {
		return "service/zones/" + zoneId;
	};
	XAUtils.filterAllowedActions = function(controller) {
		var SessionMgr = require('mgrs/SessionMgr');
			var XAGlobals = require('utils/XAGlobals');
			var vError = require('views/common/ErrorView');
			var App = require('App');
			var that = this;
			var checksso = 'false';
			var url = 'service/plugins/checksso';
			$.ajax({
				url : url,
				async : false,
				type : 'GET',
				headers : {
					"cache-control" : "no-cache"
				},
				success : function(resp) {
					checksso = resp;
				},
				error : function(jqXHR, textStatus, err ) {			
					console.log("Error in service/plugins/checksso REST call" + jqXHR.status);
					checksso = jqXHR.status;
				}
			});
			var vXPortalUser = SessionMgr.getUserProfile();
			if(_.isEmpty(vXPortalUser.attributes)){
				if(!_.isUndefined(checksso)){
					if(checksso == '404' || checksso == 'true'){
						App.rContent.show(new vError({
							 status : 204
						}));
						return;
					}else{
						return controller;
					}
				} else {
					return controller;
				}				
			}
			
			var denyControllerActions = [], denyModulesObj = [];
			var userModuleNames = _.pluck(vXPortalUser.get('userPermList'),'moduleName');
			//add by default permission module to admin user
                        if (XAUtils.isAuditorOrSystemAdmin(SessionMgr)){
				userModuleNames.push('Permissions')
			}
			var groupModuleNames = _.pluck(vXPortalUser.get('groupPermissions'), 'moduleName'),
			moduleNames = _.union(userModuleNames, groupModuleNames),
			tagBasedPolicyStr = 'Tag Based Policies', resourceBasedPolicyStr = 'Resource Based Policies';
			
			_.each(XAGlobals.ListOfModuleActions,function(val,key){
				if(!_.isArray(val)){
					_.each(val,function(val1,key1){
						if($.inArray(key1,moduleNames) < 0 ){
							//we are using same controller actions for resource and tag based service and policies creation/updation/listing page
							if( key1 == tagBasedPolicyStr && $.inArray(resourceBasedPolicyStr, moduleNames) >= 0 
									|| key1 == resourceBasedPolicyStr && $.inArray(tagBasedPolicyStr, moduleNames) >= 0){
								return;
							}
							denyModulesObj = val1.concat(denyModulesObj)
						}
					});
				}else{
					if($.inArray(key,moduleNames) < 0){
						denyModulesObj = val.concat(denyModulesObj)
					}
				}
			});
			if (!_.isEmpty(denyModulesObj)) {
				denyControllerActions.push(_.values(denyModulesObj));
				denyControllerActions = _.flatten(denyControllerActions);
			}

			if (!_.isEmpty(denyControllerActions)) {
				_.each(denyControllerActions, function(routeMethodName) {
					if (!_.isUndefined(controller[routeMethodName])) {
						controller[routeMethodName] = function() {
							that.defaultErrorHandler(undefined, {
								'status' : 401
							});
						};
					}
				});
			}
		return controller;
	};
	XAUtils.getRangerServiceByName = function(name) {
		return "service/plugins/services/name/" + name;
	};
	XAUtils.setLocationHash = function(userModuleNames) {
		var XALinks     = require('modules/XALinks');
		var SessionMgr  = require('mgrs/SessionMgr');
		if (_.contains(userModuleNames, XAEnums.MenuPermissions.XA_RESOURCE_BASED_POLICIES.label)){
			   location.hash = XALinks.get('ServiceManager').href;
		   }else if(_.contains(userModuleNames,XAEnums.MenuPermissions.XA_USER_GROUPS.label)){
		       location.hash = XALinks.get('Users').href;
		   }else if(_.contains(userModuleNames, XAEnums.MenuPermissions.XA_REPORTS.label)){
		       location.hash = XALinks.get('UserAccessReport').href;
		   }else if(_.contains(userModuleNames, XAEnums.MenuPermissions.XA_AUDITS.label)){
		       location.hash = XALinks.get('AuditReport').href +'/bigData';
		   }else if(SessionMgr.isSystemAdmin()){
			   location.hash = XALinks.get('ModulePermissions').href;
		   }else{
				//If a user doesnot has access to any tab - taking user to by default Profile page.
			   location.hash = XALinks.get('UserProfile').href;
		   }
	};
	XAUtils.getUserDataParams = function(){
		var SessionMgr  = require('mgrs/SessionMgr');
		var userRoleList = []
		_.each(XAEnums.UserRoles,function(val, key){
            if(SessionMgr.isKeyAdmin() && XAEnums.UserRoles.ROLE_SYS_ADMIN.value != val.value
                && XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.value != val.value){
				userRoleList.push(key)
            }else if(SessionMgr.isSystemAdmin() && XAEnums.UserRoles.ROLE_KEY_ADMIN.value != val.value
                && XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.value != val.value){
				userRoleList.push(key)
			}else if(SessionMgr.isUser() && XAEnums.UserRoles.ROLE_USER.value == val.value){
				userRoleList.push(key)
            }else if(SessionMgr.isAuditor() && XAEnums.UserRoles.ROLE_KEY_ADMIN.value != val.value
                && XAEnums.UserRoles.ROLE_KEY_ADMIN_AUDITOR.value != val.value){
                userRoleList.push(key)
            }else if(SessionMgr.isKMSAuditor() && XAEnums.UserRoles.ROLE_SYS_ADMIN.value != val.value
                && XAEnums.UserRoles.ROLE_ADMIN_AUDITOR.value != val.value){
                userRoleList.push(key)
			}
		})
		return {'userRoleList' : userRoleList };	};
	XAUtils.showErrorMsg = function(respMsg){
		var respArr = respMsg.split(/\([0-9]*\)/);
		respArr = respArr.filter(function(str){ return str; });
		_.each(respArr, function(str){
			var validationMsg = str.split(','), erroCodeMsg = '';
			//get code from string 
			if(!_.isUndefined(validationMsg[0]) && validationMsg[0].indexOf("error code") != -1){
				var tmp = validationMsg[0].split('error code');
				var code = tmp[ tmp.length - 1 ];
				
				erroCodeMsg = 'Error Code : '+ code.match(/\d/g).join('');
				}
			var reason = str.lastIndexOf("reason") != -1 ? (str.substring(str.lastIndexOf("reason")+7, str.indexOf("field[")-3 ))
					: str;
                        erroCodeMsg = erroCodeMsg != "" ? erroCodeMsg +"   " : "";
			var erroMsg = erroCodeMsg +""+ XAUtils.capitaliseFirstLetter(reason);
			return XAUtils.notifyError('Error', erroMsg);
		});
	};
	XAUtils.isSinglevValueInput = function(obj){
		//single value support
		var singleValue = false;
		if(!_.isUndefined(obj.uiHint) && !_.isEmpty(obj.uiHint)){
			var UIHint = JSON.parse(obj.uiHint);
			if(!_.isUndefined(UIHint.singleValue))
				singleValue = UIHint.singleValue;
		}
		return singleValue;
	};
	XAUtils.hideIfNull = function(obj, form){
		//resorces hide show
		var hideIfNull = false;
		if(!_.isEmpty(obj.uiHint)){
			var UIHint = JSON.parse(obj.uiHint);
			if(!_.isUndefined(form.model.get('resources')) && !_.isEmpty(form.model.get('resources')) &&
					_.has(form.model.get('resources'), obj.name)){
				 hideIfNull = false;
			}else{
				if(!_.isUndefined(UIHint.hideIfNull) && ! obj.mandatory){
					hideIfNull = UIHint.hideIfNull;
				}
			}
		}
		return hideIfNull;
	};
	XAUtils.getBaseUrl = function (){
		if(!window.location.origin){
			window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
		}
		return window.location.origin
		+ window.location.pathname.substring(window.location.pathname
				.lastIndexOf('/') + 1, 0);
	};
	
	XAUtils.isMaskingPolicy = function(type){
		return type == XAEnums.RangerPolicyType.RANGER_MASKING_POLICY_TYPE.value ? true : false;
	};
	XAUtils.isRenderMasking = function(dataMaskDef){
		return (!_.isUndefined(dataMaskDef) && !_.isUndefined(dataMaskDef.maskTypes) 
			&& dataMaskDef.maskTypes.length > 0) ? true : false; 
	};
	XAUtils.isAccessPolicy = function(type){
		return type == XAEnums.RangerPolicyType.RANGER_ACCESS_POLICY_TYPE.value ? true : false;
	};
	XAUtils.isRowFilterPolicy = function(type){
		return type == XAEnums.RangerPolicyType.RANGER_ROW_FILTER_POLICY_TYPE.value ? true : false;
	};
	XAUtils.isRenderRowFilter = function(rowFilterDef){
		return (!_.isUndefined(rowFilterDef) && !_.isUndefined(rowFilterDef.resources) 
			&& rowFilterDef.resources.length > 0) ? true : false; 
	};
	XAUtils.showAllPolicyItems = function(rangerServiceDefModel, model){
		var enableDenyAndExceptionsInPolicies = false,serviceDefOptions = rangerServiceDefModel.get('options');
		if((!_.isUndefined(serviceDefOptions) && !_.isUndefined(serviceDefOptions.enableDenyAndExceptionsInPolicies))){
			enableDenyAndExceptionsInPolicies = this.isAccessPolicy(model.get('policyType')) && $.parseJSON(serviceDefOptions.enableDenyAndExceptionsInPolicies);
		} else {
			if(rangerServiceDefModel.get('name') == XAEnums.ServiceType.SERVICE_TAG.label){
				enableDenyAndExceptionsInPolicies = true;
			}		
		}
		return enableDenyAndExceptionsInPolicies;
	};
	XAUtils.isEmptyObjectResourceVal = function (obj) {
		return !_.isUndefined(obj['resources']) && !_.isEmpty(obj['resources'])
		 		&& !_.isNull(obj['resources']) ? false : true;
	};
    XAUtils.removeEmptySearchValue = function(arr) {
            return  _.reject(arr,function(m){
                    return (m.get('value')=="");
            });
    };
        XAUtils.showAuditLogTags = function(rawValue, model) {
                var showMoreLess = false, id = model.id, tagLabels = '';
                var tagNames = _.pluck(rawValue, 'type');
                if (!_.isUndefined(rawValue)) {
                        var tagArr = _.map(rawValue, function(tag, i) {
                                if(tag.attributes && !_.isEmpty(tag.attributes)){
                                        tagLabels = '<a href="javascript:void(0)" data-name="tags" data-id="'+model.id+''+i+'" class="tagsColumn">'+tag.type+'</a>';
                                }else{
                                        tagLabels = tag.type;
                                }
                                tagLabels += (i+1 < rawValue.length) ? '&nbsp;,&nbsp;' : '';
                                if (i >= 4){
                                        return '<span class="float-left-margin-1" audit-log-id="'
                                        + id + '" style="display:none;">' + tagLabels
                                        + '</span>';
                                }
                                else{
                                        if (i == 3 && rawValue.length > 4) {
                                                showMoreLess = true;
                                                return '<span class="float-left-margin-1" audit-log-id="'
                                                                + id + '">' + tagLabels + '</span>';
                                        } else{
                                                return '<span class="float-left-margin-1" audit-log-id="'
                                                + id + '">' + tagLabels + '</span>';
                                        }
                                }
                        });
                        if (showMoreLess) {
                                tagArr.push('<span class="float-left-margin-1"><a href="javascript:void(0);" data-id="showMore" class="" audit-log-id="'
                                                                + id
                                                                + '"><code style=""> + More..</code></a></span><span class="float-left-margin-1"><a href="javascript:void(0);" data-id="showLess" class="" audit-log-id="'
                                                                + id
                                                                + '" style="display:none;"><code> - Less..</code></a></span>');
                        }
                        tagArr.unshift('<div data-id="tagDiv" class="popovertag">');
                        tagArr.push('</div>');
                        return tagArr.length ? tagArr.join(' ') : '--';
                } else{
                        return '--';
                }
        };
        XAUtils.isTagBasedDef = function(def){
        	return def.get('name') == XAEnums.ServiceType.SERVICE_TAG.label ? true : false;
        };
    XAUtils.policyTypeResources = function(obj , policyType){
    	if(XAUtils.isAccessPolicy(policyType)){
    		return obj.get('resources');
    	}else{
    		if(XAUtils.isMaskingPolicy(policyType)){
    			return obj.get('dataMaskDef').resources;
    		}else{
    			return obj.get('rowFilterDef').resources;
    		}
    	}
    };
    XAUtils.showMoreAndLessButton = function(rawValue, model){
        var showMoreLess = false;
        var newLabelArr = _.map(rawValue, function(name, i) {
            if (i >= 4) {
                return '<span class="badge badge-info float-left-margin-2 shorten-label" title="'+ _.escape(name) +'" policy-label-id ="'+ model.id +'" style="display:none;">'+ _.escape(name) + '</span>';
            } else if (i == 3 && rawValue.length > 4) {
                showMoreLess = true;
                return '<span class="badge badge-info float-left-margin-2 shorten-label" title="'+ _.escape(name) +'" policy-label-id ="'+ model.id +'">' + _.escape(name) + '</span>';
            } else {
                return '<span class="badge badge-info float-left-margin-2 shorten-label" title="'+ _.escape(name) +'" policy-label-id ="'+ model.id +'">' + _.escape(name) + '</span>';
            }
        });
        if (showMoreLess) {
            newLabelArr.push('<span class="pull-left float-left-margin-2"><a href="javascript:void(0);" data-id="showMore" policy-label-id ="'+ model.id +'"><code style=""> + More..</code></a></span>\
                    <span class="pull-left float-left-margin-2" ><a href="javascript:void(0);" data-id="showLess" style="display:none;" policy-label-id ="'+ model.id +'"><code> - Less..</code></a></span>');
        }
        newLabelArr.unshift('<div data-id="groupsDiv">');
        newLabelArr.push('</div>');
        return newLabelArr.length ? newLabelArr.join(' ') : '--';
    };
    XAUtils.isAuditorOrSystemAdmin = function(SessionMgr){
        return (SessionMgr.isAuditor() || SessionMgr.isSystemAdmin()) ? true : false ;
    };
    XAUtils.isAuditorOrKMSAuditor = function(SessionMgr){
        return (SessionMgr.isAuditor() || SessionMgr.isKMSAuditor()) ? true : false ;
    };
    XAUtils.isPolicyExpierd = function(model){
        var moment = require('moment');
        var momontTz = require('momentTz');
        return !_.some(model.get('validitySchedules') , function(m){
            if(!m.endTime){
                return true;
            } else if(_.isEmpty(m.timeZone)){
                return new Date().valueOf() > new Date(m.endTime).valueOf() ? false : true;
            }else{
                return new Date(moment.tz(m.timeZone).format('MM/DD/YYYY HH:mm:ss')).valueOf() >
                new Date(m.endTime).valueOf() ? false : true;
            }
        });
    };
    XAUtils.copyToClipboard = function(e , copyText ){
        var input = document.createElement('input');
        if(_.isArray(copyText)){
            input.setAttribute('value', copyText.join(' | '));
        }else{
            input.setAttribute('value', copyText);
        }
        document.body.appendChild(input);
        input.select();
        document.execCommand('copy');
        document.body.removeChild(input);
        e.currentTarget.title="Copied!";
    };
    //If view is closed, closed all new DOM element that added in DOM like popup, modal, date-selector and select-list etc.
    XAUtils.removeUnwantedDomElement = function(){
        $('.modal').remove();
        $('.modal-backdrop').remove();
        $('#select2-drop').select2('close');
        $('.datepicker').remove();
        $('.popover').remove();
        $('.datetimepicker').remove();
        $('body').removeClass('modal-open')
    };
    //select2 option
    XAUtils.select2OptionForUserCreateChoice = function(){
        var opts = {
                multiple: true,
                data:[],
                closeOnSelect : true,
                width :'220px',
                allowClear: true,
                tokenSeparators: [" "],
                minimumInputLength: 1,
                initSelection : function (element, callback) {
                    var data = [];
                    //to set single select value
                    if(!_.isUndefined(opts.singleValueInput) && opts.singleValueInput){
                        callback({ id : element.val(), text : element.val() });
                        return;
                    }
                    //this is form multi-select value
                    if(_.isArray(JSON.parse(element.val()))) {
                        $(JSON.parse(element.val())).each(function () {
                            data.push({id: this, text: this});
                        })
                    }
                    callback(data);
                },
                createSearchChoice: function(term, data) {
                    term = _.escape(term);
                    if ($(data).filter(function() {
                        return this.text.localeCompare(term) === 0;
                    }).length === 0) {
                        if($.inArray(term, this.val()) >= 0){
                            return null;
                        }else{
                            return {
                                id : "<b><i class='text-muted-select2'>Create</i></b> " + term,
                                text: term
                            };
                        }
                    }
                },
                formatResult : function(result){
                    return result.id;
                },
                formatSelection : function(result){
                    return result.text;
                },
            };

        return opts;
    }

    //get policy conditions details
    XAUtils.getPolicyConditionDetails = function(policyCondtions, serviceDef){
        var condtionsDetails = [];
        _.each(policyCondtions, function(val){
        var conditionsVal = serviceDef.get('policyConditions').find(function(m){return m.name == val.type});
            condtionsDetails.push({'name' : conditionsVal.label, 'values' : val.values});
        })
        return condtionsDetails;
    }

    //get list of all tag base services
    XAUtils.getTagBaseServices = function(){
        return {
            closeOnSelect : true,
            placeholder : 'Select Tag Services',
            width :'600px',
            allowClear: true,
            multiple: true,
            tokenSeparators: ["," , " "],
            initSelection : function (element, callback) {
                var tags = [];
                _.each(element.val().split(','), function(name) {
                    tags.push({
                        'id': _.escape(name),
                        'text': _.escape(name)
                    });
                });
                callback(tags)
            },
            ajax: {
                url: "service/plugins/services",
                dataType: 'json',
                data: function (term, page) {
                    return { serviceNamePartial : term, serviceType : 'tag' };
                },
                results: function (data, page) {
                    var results = [];
                    if(data.resultSize != "0"){
                        results = data.services.map(function(m, i){ return {id : _.escape(m.name), text: _.escape(m.name) };    });
                        return {results : results};
                    }
                    return {results : results};
                },
                transport : function (options) {
                                            $.ajax(options).fail(function(respones) {
                        XAUtils.defaultErrorHandler('error',respones);
                        this.success({
                            resultSize : 0
                        });
                    });
                }
            },
            formatResult : function(result){
                return result.text;
            },
            formatSelection : function(result){
                return result.text;
            },
            formatNoMatches: function(result){
                return 'No tag service found.';
            }
        };
    }

    //Custom popover
    XAUtils.customPopover = function($element, title, msg, placement){
        $element.popover({
            trigger: "manual",
            title:title,
            html: true,
            animation:false,
            content: msg,
            container:'body',
            placement: placement,
        }).on("mouseenter", function () {
            var _this = this;
            $(this).popover("show");
            $(".popover").on("mouseleave", function () {
                $(_this).popover('hide');
            });
        }).on("mouseleave", function () {
            var _this = this;
            setTimeout(function () {
                if (!$(".popover:hover").length) {
                    $(_this).popover("hide");
                }
            }, 300);
        });
    }

    XAUtils.getUsersGroupsList = function($select, domElement, width, auditFilter){
        var that = domElement,
            tags = [],
            placeholder = $select === 'users' ? "Select User" : $select === 'groups' ? "Select Group" : "Select Role",
            searchUrl = $select === 'users' ? "service/xusers/lookup/users" : $select === 'groups' ? "service/xusers/lookup/groups"
                : "service/roles/roles";
            if(that.model && !_.isEmpty(that.model.get($select))){
                _.map (that.model.get($select) , function(name){
                    tags.push({
	                    'id': _.escape(name),
	                    'text': _.escape(name)
	                });
                })
            }

        return {
            closeOnSelect : true,
            placeholder   : placeholder,
            tags : true,
            width : width,
            initSelection: function(element, callback) {
                callback(tags);
            },
            ajax: {
                url: searchUrl,
                dataType: 'json',
                data: function(term, page) {
                    if($select === 'roles') {
                        return {
                            roleNamePartial: term
                        }
                    } else {
                        return {
                            name: term,
                            isVisible : XAEnums.VisibilityStatus.STATUS_VISIBLE.value,
                        }
                    }
                },
                results: function(data, page) {
                    var results = [],
                        selectedVals = [];
                    //Get selected values of groups/users dropdown
                    if (data.totalCount != "0") {
                        //remove users {USER} and {OWNER}
                        if ($select == 'users' || $select == 'groups') {
                            if (_.isUndefined(auditFilter)) {
                                data.vXStrings = _.reject(data.vXStrings, function(m){return (m.value == '{USER}' || m.value == '{OWNER}')})
                            }
                            results = data.vXStrings.map(function(m) {
                                return {
                                    id: _.escape(m.value),
                                    text: _.escape(m.value)
                                };
                            });
                        } else {
                            if(that.model && !_.isEmpty(that.model.get('name'))){
                                data.roles = _.reject(data.roles, function(m){
                                    return (m.name == that.model.get('name'))
                                })
                            }
                            results = data.roles.map(function(m){
                                return {
                                    id : _.escape(m.name),
                                    text: _.escape(m.name)
                                };
                            });
                        }
                        //remove selected values
                        if(that.collection && that.collection.models){
                            _.filter(that.collection.models, function(model){
                                if(model && !_.isUndefined(model.get('name'))){
                                    selectedVals.push(model.get('name'));
                                }
                            })
                        }
                        if (!_.isEmpty(selectedVals)) {
                            results = XAUtils.filterResultByText(results, selectedVals);
                        }
                        return {
                            results: results
                        };
                    }
                    return {
                        results: results
                    };
                },
                transport: function(options) {
                    $.ajax(options).fail(function(respones) {
                        XAUtils.defaultErrorHandler('error', respones);
                        this.success({
                            resultSize: 0
                        });
                    });
                }
            },
            formatResult: function(result) {
                return result.text;
            },
            formatSelection: function(result) {
                return result.text;
            },
            formatNoMatches: function(result) {
                return $select === 'users' ? "No user found." : $select === 'groups' ? "No group found." : "No role found.";
            }

        }
    }

    //string contain escape character or not
    XAUtils.checkForEscapeCharacter = function(policyName){
        var escapeCharacter = ["&amp;", "&lt;", "&gt;", "&quot;", "&#96;", "&#x27;"];
        return _.some(escapeCharacter, function(m){
            return policyName.includes(m);
        });
    }

    //remove sort caret on grids
    XAUtils.backgridSort = function(col){
        if(!_.isUndefined(col.queryParams) && col.queryParams.sortBy && !_.isNull(col.queryParams.sortBy)) {
                var sortparams = _.pick(col.queryParams, 'sortBy');
            col.state.order == 1 ? sortparams['sortType'] = "descending" : sortparams['sortType'] = "ascending";
            XAUtils.changeParamToUrlFragment(sortparams);
        }
        col.on('backgrid:sort', function(model) {
            // No ids so identify model with CID
            var cid = model.cid, urlObj = {};
            var filtered = model.collection.filter(function(model) {
                return model.cid !== cid;
            });
            _.each(filtered, function(model) {
               model.set('direction', null);
            });
            if(Backbone.history.fragment.indexOf("?") !== -1) {
                var urlFragment = Backbone.history.fragment.substring(Backbone.history.fragment.indexOf("?") + 1);
                urlObj = XAUtils.changeUrlToSearchQuery(decodeURIComponent(urlFragment));
            }
            urlObj['sortBy'] = model.get('name');
            if(_.isNull(model.get('direction'))) {
                delete urlObj.sortType;
                delete urlObj.sortBy;
            } else {
                urlObj['sortType'] = model.get('direction');
            }
            XAUtils.changeParamToUrlFragment(urlObj);
        });
    }

    //Scroll up for roles create page
    XAUtils.scrollToRolesField = function(field) {
        $("html, body").animate({
            scrollTop : field.position().top - 150
        }, 1100, function() {
            field.focus();
        });
    };

    //Get service details By Service name
    XAUtils.getServiceByName = function(name) {
        return "service/plugins/services/name/" + name
    };

    //Add visual search query parameter to URL
    XAUtils.changeParamToUrlFragment = function(obj, modelName) {
	var App = require('App');
        var baseUrlFregment = Backbone.history.fragment.split('?')[0],
        str = [];
        for (var p in obj) {
            if (obj.hasOwnProperty(p)) {
                if(_.isArray(obj[p])) {
                    _.each(obj[p], function(val) {
                        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(val));
                    })
                } else {
                    str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                }
            }
        }
        if(App.vZone && App.vZone.vZoneName && !_.isEmpty(App.vZone.vZoneName) && !obj.hasOwnProperty("securityZone") &&
            modelName && (modelName === "RangerServiceDef" || modelName === "RangerPolicy")) {
		str.push(encodeURIComponent("securityZone")+"="+ encodeURIComponent(App.vZone.vZoneName));
        }
        if( _.isEmpty(str)) {
            Backbone.history.navigate(baseUrlFregment , false);
        } else {
            Backbone.history.navigate(baseUrlFregment+"?"+str.join("&") , false);
        }
    }

    //convert URL to object params
    XAUtils.changeUrlToSearchQuery = function(query) {
        var query_string = {};
        var vars = query.split("&");
        for (var i=0;i<vars.length;i++) {
            var pair = vars[i].split("=");
            pair[0] = decodeURIComponent(pair[0]);
            pair[1] = decodeURIComponent(pair[1]);
            // If first entry with this name
            if (typeof query_string[pair[0]] === "undefined") {
                query_string[pair[0]] = pair[1];
                // If second entry with this name
            } else if (typeof query_string[pair[0]] === "string") {
                var arr = [ query_string[pair[0]], pair[1] ];
                query_string[pair[0]] = arr;
                // If third or later entry with this name
            } else {
                query_string[pair[0]].push(pair[1]);
            }
        }
        return query_string;
    }

    //Return key from serverAttrName for vsSearch
    XAUtils.filterKeyForVSQuery = function(list, key) {
        var value = _.filter(list, function(m) {
            return m.urlLabel === key
        })
        if(_.isEmpty(value) || _.isUndefined(value)) {
            value = _.filter(list, function(m) {
                return m.text === key
            })
        }
       return value[0].text
    }

    //convert string to Camel Case
    XAUtils.stringToCamelCase = function(str) {
        return str.replace(/(?:^\w|[A-Z]|\b\w)/g, function(word, index) {
            return index == 0 ? word.toLowerCase() : word.toUpperCase();
        }).replace(/\s+/g, '');
    }

    //Set backgrid table sorting direction
    XAUtils.backgridSortType = function(collection, column) {
        _.filter(column, function(val, key){
            if(key == collection.queryParams.sortBy) {
                val['direction'] =  collection.state.order == 1 ? "descending" : "ascending"
            }
        })
    }

    //Set default sort by and sort order in collection
    XAUtils.setSorting = function(collectin, sortParams) {
        _.extend(collectin.queryParams,{ 'sortBy'  :  sortParams.sortBy });
        if(sortParams.sortType) {
            sortParams.sortType == "ascending" ? collectin.setSorting(sortParams.sortBy,-1) : collectin.setSorting(sortParams.sortBy,1);
        }
    }

    //Separate query parameters string from URL hash
    XAUtils.urlQueryParams = function() {
    	var urlHash = Backbone.history.location.hash;
    	return urlHash.indexOf("?") !== -1 ? urlHash.substring(urlHash.indexOf("?") + 1) : undefined;
    }

    XAUtils.resizeableColumn = function (self, columnName) {
        self.rTableList.$el.find('.'+columnName).resizable({
            maxHeight : 20,
            stop: function (event, ui) {
                ui.element.css('min-width', ui.size.width);
                localStorage.setItem(columnName+'ColWidth', ui.size.width);
            }
        });
        if(localStorage.getItem(columnName+'ColWidth') !== null) {
            self.rTableList.$el.find('.'+columnName).css('min-width', +localStorage.getItem(columnName+'ColWidth'));
        }
    }

    XAUtils.setIdealActivityTime = function() {
        var App = require('App');
        var INACTIVITY_TIME_OUT = 900;
        if (App.userProfile && App.userProfile.get('configProperties') && App.userProfile.get('configProperties').inactivityTimeout) {
            INACTIVITY_TIME_OUT = parseInt(App.userProfile.get('configProperties').inactivityTimeout);
        }
        INACTIVITY_TIME_OUT *= 1000;
        localStorage.setItem('idealTimeOut', moment().add(INACTIVITY_TIME_OUT, 'milliseconds').valueOf());
        localStorage.setItem('idleTimerLoggedOut', false);
        XAUtils.setIdealActivityTime = function () {
            localStorage.setItem('idealTimeOut', moment().add(INACTIVITY_TIME_OUT, 'milliseconds').valueOf());
            var isLoggedOut = localStorage.getItem('idleTimerLoggedOut') == "true";
            if (isLoggedOut) {
                localStorage.setItem('idleTimerLoggedOut', false);
            }
            XAUtils.startIdealActivityInterval()
        }
    };

    XAUtils.startIdealActivityInterval = function () {
        clearInterval(XAUtils.activityIntervalID)
        XAUtils.activityIntervalID = setInterval(function() {
            var idealTimeVal = parseInt(localStorage.getItem('idealTimeOut'));
            if(moment().isAfter(moment(idealTimeVal))) {
                clearInterval(XAUtils.activityIntervalID)
                var isLoggedOut = localStorage.getItem('idleTimerLoggedOut') == "true";
                if(isLoggedOut) {
                    localStorage.setItem('idleTimerLoggedOut', 'false');
                    XAUtils.idealActivityLogout();
                } else {
                    XAUtils.idelTimePopup();
                }
            }
        }, 2000);
    };

    XAUtils.idelTimePopup = function() {
        var timeLeft = 15;
        var $elem = '<div id="Timer"></div>';

        function countdown() {
            if (timeLeft == 0) {
                clearTimeout(timerId);
                localStorage.setItem('idleTimerLoggedOut', 'false');
                XAUtils.idealActivityLogout();
            } else {
                var isLoggedOut = localStorage.getItem('idleTimerLoggedOut') == "true";
                if(isLoggedOut) {
                    clearTimeout(timerId);
                    localStorage.setItem('idleTimerLoggedOut', 'false');
                    XAUtils.idealActivityLogout();
                } else {
                    $.find('#Timer')[0].innerHTML ='Time left : '+ timeLeft + ' seconds remaining';
                    timeLeft--;
                }
            }
        }
        bootbox.dialog({
            title: 'Session Expiration Warning',
            message: '<span class="inline-block">' + localization.tt('dialogMsg.idelTimeOutMsg') +'<br>'+ $elem + '</span>',
            closeButton: false,
            buttons: {
                noclose: {
                    "label" : localization.tt('btn.stayLoggdedIn'),
                    "className" : "btn-success btn-sm",
                    "callback" : function() {
                        clearTimeout(timerId);
                        XAUtils.setIdealActivityTime()
                    }
                },
                cancel: {
                    "label" : localization.tt('btn.logOutNow'),
                    "className" : "btn-danger btn-sm",
                    "callback" : function() {
                        localStorage.setItem('idleTimerLoggedOut', 'false');
                        XAUtils.idealActivityLogout();
                    }
                }
            }
        });

        var timerId = setInterval(countdown, 1000);

        return false;
    };

    XAUtils.idealActivityLogout = function () {
        var App = require('App');
        // localStorage.setItem('idleTimerLoggedOut', true);
        if(localStorage.getItem('idleTimerLoggedOut') == "false") {
        	localStorage.setItem('idleTimerLoggedOut', true);
        	App.rTopProfileBar.currentView.checkKnoxSSO();
        }
    };

	return XAUtils;
});