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

	var VXAssetBase	= require('model_bases/VXAssetBase');
	var XAUtils		= require('utils/XAUtils');
	var XAEnums		= require('utils/XAEnums');
	var localization= require('utils/XALangSupport');

	var VXAsset = VXAssetBase.extend(
	/** @lends VXAsset.prototype */
	{
		/**
		 * VXAsset initialize method
		 * @augments VXAssetBase
		 * @constructs
		 */
		initialize: function() {
			this.modelName = 'VXAsset';
			this.bindErrorEvents();
		},
		/**
		 * @function schema
		 * This method is meant to be used by UI,
		 * by default we will remove the unrequired attributes from serverSchema
		 */

		schemaBase : function(){
			var attrs = _.omit(this.serverSchema, 'id', 'createDate', 'updateDate', "version",
					"displayOption", "permList", "forUserId", "status", "priGrpId",
					"updatedBy","isSystem");

			_.each(attrs, function(o){
				o.type = 'Hidden';
			});

			// Overwrite your schema definition here
			return _.extend(attrs,{
				name : {
					type		: 'Text',
					title		: 'Repository Name *',
					validators	: ['required'],
					editorAttrs 	:{ maxlength: 255},
				},
				description : {
					type		: 'TextArea',
					title		: 'Description',
					validators	: []
				},
				activeStatus : {
			        type: 'Radio',
					options : function(callback, editor){
						var activeStatus = _.filter(XAEnums.ActiveStatus,function(m){return m.label != 'Deleted'});
						var nvPairs = XAUtils.enumToSelectPairs(activeStatus);
						callback(_.sortBy(nvPairs, function(n){ return !n.val; }));
					}
				},
				assetType : {
					type : 'Select',
					options : function(callback, editor){
						var assetTypes = _.filter(XAEnums.AssetType,function(m){return m.label != 'Unknown'});
						var nvPairs = XAUtils.enumToSelectPairs(assetTypes);
						callback(nvPairs);
					},
					title : localization.tt('lbl.assetType'),
					editorAttrs:{'disabled' : true}
				}

			});
		},

		/** This models toString() */
		toString : function(){
			return this.get('name');
		},
		propertiesNameMap : {
			userName : "username",
			passwordKeytabfile : "password",
			fsDefaultName : "fs.default.name",
			authorization : "hadoop.security.authorization",
			authentication : "hadoop.security.authentication",
			auth_to_local : "hadoop.security.auth_to_local",
			datanode : "dfs.datanode.kerberos.principal",
			namenode : "dfs.namenode.kerberos.principal",
			secNamenode : "dfs.secondary.namenode.kerberos.principal",
			hadoopRpcProtection : "hadoop.rpc.protection",
			//hive
			driverClassName : "jdbc.driverClassName",
			url	: "jdbc.url",
			
			masterKerberos     		: 'hbase.master.kerberos.principal',
			rpcEngine 				: 'hbase.rpc.engine',
			rpcProtection	 		: 'hbase.rpc.protection',
			securityAuthentication  : 'hbase.security.authentication',
			zookeeperProperty 		: 'hbase.zookeeper.property.clientPort',
			zookeeperQuorum 		: 'hbase.zookeeper.quorum',
			//hbase
			zookeeperZnodeParent	: 'zookeeper.znode.parent',
			//knox
			knoxUrl					:'knox.url',
			//Storm
			nimbusUrl				:'nimbus.url',
			
			commonnameforcertificate: 'commonNameForCertificate'
		}

	}, {
		// static class members
	});

    return VXAsset;
	
});


