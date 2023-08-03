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

	var Backbone			= require('backbone');
	var XALinks 			= require('modules/XALinks');
	var ServicemanagerlayoutTmpl = require('hbs!tmpl/common/ServiceManagerLayout_tmpl');
	var RangerServiceViewDetail = require('views/service/RangerServiceViewDetail');
	var XAUtil              = require('utils/XAUtils');
    var App    =require('App');
	return Backbone.Marionette.Layout.extend(
	/** @lends Servicemanagerlayout */
	{
		_viewName : name,
		
    	template: ServicemanagerlayoutTmpl,

		templateHelpers: function(){
			return {
				setOldUi : localStorage.getItem('setOldUI') == "true" ? true : false,
			}
		},

    	breadCrumbs :function(){
            if(this.type == "tag"){
                if(App.vZone && App.vZone.vZoneName && !_.isEmpty(App.vZone.vZoneName)){
                    return [XALinks.get('TagBasedServiceManager', App.vZone.vZoneName)];
                }else{
                    return [XALinks.get('TagBasedServiceManager')];
                }
            }else{
                if(App.vZone && App.vZone.vZoneName && !_.isEmpty(App.vZone.vZoneName)){
                    return [XALinks.get('ServiceManager', App.vZone.vZoneName)];
                }else{
                    return [XALinks.get('ServiceManager')];
                }
            }
        },

		/** Layout sub regions */
    	regions: {},

    	/** ui selector cache */
    	ui: {},

		/** ui events hash */
		events : function(){},
    	/**
		* intialize a new Servicemanagerlayout Layout 
		* @constructs
		*/
		initialize: function(options) {},

		/** all events binding here */
		bindEvents : function(){},

		/** on render callback */
		onRender: function() {},
		
		/** all post render plugin initialization */
		initializePlugins: function(){},
		
		/** on close */
		onClose: function(){
            XAUtil.removeUnwantedDomElement();
		}

	});
});