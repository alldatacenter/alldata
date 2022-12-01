/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Enums from "../Enums";
import { curveBasis } from "d3-shape";

const DataUtils = {
	/**
	 * [getBaseUrl description]
	 * @param  {[type]} url [description]
	 * @return {[type]}     [description]
	 */
	getBaseUrl: function(url) {
		return url.replace(/\/[\w-]+.(jsp|html)|\/+$/gi, "");
	},
	/**
	 * [getEntityIconPath description]
	 * @param  {[type]} options.entityData [description]
	 * @param  {Object} options.errorUrl   }            [description]
	 * @return {[type]}                    [description]
	 */
	getEntityIconPath: function({ entityData, errorUrl } = {}) {
		var serviceType,
			status,
			typeName,
			iconBasePath = this.getBaseUrl(window.location.pathname) + Globals.entityImgPath;
		if (entityData) {
			typeName = entityData.typeName;
			serviceType = entityData && entityData.serviceType;
			status = entityData && entityData.status;
		}

		function getImgPath(imageName) {
			return iconBasePath + (Enums.entityStateReadOnly[status] ? "disabled/" + imageName : imageName);
		}

		function getDefaultImgPath() {
			if (entityData.isProcess) {
				if (Enums.entityStateReadOnly[status]) {
					return iconBasePath + "disabled/process.png";
				} else {
					return iconBasePath + "process.png";
				}
			} else {
				if (Enums.entityStateReadOnly[status]) {
					return iconBasePath + "disabled/table.png";
				} else {
					return iconBasePath + "table.png";
				}
			}
		}

		if (entityData) {
			if (errorUrl) {
				var isErrorInTypeName = errorUrl && errorUrl.match("entity-icon/" + typeName + ".png|disabled/" + typeName + ".png") ? true : false;
				if (serviceType && isErrorInTypeName) {
					var imageName = serviceType + ".png";
					return getImgPath(imageName);
				} else {
					return getDefaultImgPath();
				}
			} else if (entityData.typeName) {
				var imageName = entityData.typeName + ".png";
				return getImgPath(imageName);
			} else {
				return getDefaultImgPath();
			}
		}
	},
	/**
	 * [isProcess description]
	 * @param  {[type]}  options.typeName   [description]
	 * @param  {[type]}  options.superTypes [description]
	 * @param  {[type]}  options.entityDef  [description]
	 * @return {Boolean}                    [description]
	 */
	isProcess: function({ typeName, superTypes, entityDef }) {
		if (typeName == "Process") {
			return true;
		}
		return superTypes.indexOf("Process") > -1;
	},
	/**
	 * [isDeleted description]
	 * @param  {[type]}  node [description]
	 * @return {Boolean}      [description]
	 */
	isDeleted: function(node) {
		if (node === undefined) {
			return;
		}
		return Enums.entityStateReadOnly[node.status];
	},
	isNodeToBeUpdated: function(node, filterObj) {
		var isProcessHideCheck = filterObj.isProcessHideCheck,
			isDeletedEntityHideCheck = filterObj.isDeletedEntityHideCheck;
		var returnObj = {
			isProcess: isProcessHideCheck && node.isProcess,
			isDeleted: isDeletedEntityHideCheck && node.isDeleted
		};
		returnObj["update"] = returnObj.isProcess || returnObj.isDeleted;
		return returnObj;
	},
	/**
	 * [getServiceType description]
	 * @param  {[type]} options.typeName  [description]
	 * @param  {[type]} options.entityDef [description]
	 * @return {[type]}                   [description]
	 */
	getServiceType: function({ typeName, entityDef }) {
		var serviceType = null;
		if (typeName) {
			if (entityDef) {
				serviceType = entityDef.serviceType || null;
			}
		}
		return serviceType;
	},
	/**
	 * [getEntityDef description]
	 * @param  {[type]} options.typeName            [description]
	 * @param  {[type]} options.entityDefCollection [description]
	 * @return {[type]}                             [description]
	 */
	getEntityDef: function({ typeName, entityDefCollection }) {
		var entityDef = null;
		if (typeName) {
			entityDef = entityDefCollection.find(function(obj) {
				return obj.name == typeName;
			});
		}
		return entityDef;
	},
	/**
	 * [getNestedSuperTypes description]
	 * @param  {[type]} options.entityDef           [description]
	 * @param  {[type]} options.entityDefCollection [description]
	 * @return {[type]}                             [description]
	 */
	getNestedSuperTypes: function({ entityDef, entityDefCollection }) {
		var data = entityDef,
			collection = entityDefCollection,
			superTypes = new Set();

		var getData = function(data, collection) {
			if (data) {
				if (data.superTypes && data.superTypes.length) {
					data.superTypes.forEach(function(superTypeName) {
						superTypes.add(superTypeName);
						var collectionData = collection.find(function(obj) {
							obj.name === superTypeName;
						});
						if (collectionData) {
							getData(collectionData, collection);
						}
					});
				}
			}
		};
		getData(data, collection);
		return Array.from(superTypes);
	},
	generateData: function({ data = {}, filterObj, entityDefCollection, g, guid, setGraphEdge, setGraphNode }) {
		return new Promise((resolve, reject) => {
			try {
				var relations = data.relations || {},
					guidEntityMap = data.guidEntityMap || {},
					isHideFilterOn = filterObj.isProcessHideCheck || filterObj.isDeletedEntityHideCheck,
					newHashMap = {},
					styleObj = {
						fill: "none",
						stroke: "#ffb203",
						width: 3
					},
					makeNodeData = (relationObj) => {
						if (relationObj) {
							if (relationObj.updatedValues) {
								return relationObj;
							}
							var obj = Object.assign(relationObj, {
								shape: "img",
								updatedValues: true,
								label: relationObj.displayText.trunc(18),
								toolTipLabel: relationObj.displayText,
								id: relationObj.guid,
								isLineage: true,
								isIncomplete: relationObj.isIncomplete,
								entityDef: this.getEntityDef({ typeName: relationObj.typeName, entityDefCollection })
							});
							obj["serviceType"] = this.getServiceType(obj);
							obj["superTypes"] = this.getNestedSuperTypes({
								...obj,
								entityDefCollection: entityDefCollection
							});
							obj["isProcess"] = this.isProcess(obj);
							obj["isDeleted"] = this.isDeleted(obj);
							return obj;
						}
					},
					crateLineageRelationshipHashMap = function({ relations } = {}) {
						var newHashMap = {};
						relations.forEach(function(obj) {
							if (newHashMap[obj.fromEntityId]) {
								newHashMap[obj.fromEntityId].push(obj.toEntityId);
							} else {
								newHashMap[obj.fromEntityId] = [obj.toEntityId];
							}
						});
						return newHashMap;
					},
					getStyleObjStr = function(styleObj) {
						return "fill:" + styleObj.fill + ";stroke:" + styleObj.stroke + ";stroke-width:" + styleObj.width;
					},
					getNewToNodeRelationship = (toNodeGuid, filterObj) => {
						if (toNodeGuid && relationshipMap[toNodeGuid]) {
							var newRelationship = [];
							relationshipMap[toNodeGuid].forEach((guid) => {
								var nodeToBeUpdated = this.isNodeToBeUpdated(makeNodeData(guidEntityMap[guid]), filterObj);
								if (nodeToBeUpdated.update) {
									var newRelation = getNewToNodeRelationship(guid, filterObj);
									if (newRelation) {
										newRelationship = newRelationship.concat(newRelation);
									}
								} else {
									newRelationship.push(guid);
								}
							});
							return newRelationship;
						} else {
							return null;
						}
					},
					getToNodeRelation = (toNodes, fromNodeToBeUpdated, filterObj) => {
						var toNodeRelationship = [];
						toNodes.forEach((toNodeGuid) => {
							var toNodeToBeUpdated = this.isNodeToBeUpdated(makeNodeData(guidEntityMap[toNodeGuid]), filterObj);
							if (toNodeToBeUpdated.update) {
								// To node need to updated
								if (pendingFromRelationship[toNodeGuid]) {
									toNodeRelationship = toNodeRelationship.concat(pendingFromRelationship[toNodeGuid]);
								} else {
									var newToNodeRelationship = getNewToNodeRelationship(toNodeGuid, filterObj);
									if (newToNodeRelationship) {
										toNodeRelationship = toNodeRelationship.concat(newToNodeRelationship);
									}
								}
							} else {
								//when bothe node not to be updated.
								toNodeRelationship.push(toNodeGuid);
							}
						});
						return toNodeRelationship;
					},
					setNode = (guid) => {
						if (!g._nodes[guid]) {
							var nodeData = makeNodeData(guidEntityMap[guid]);
							setGraphNode(guid, nodeData);
							return nodeData;
						} else {
							return g._nodes[guid];
						}
					},
					setEdge = function(fromNodeGuid, toNodeGuid, opt = {}) {
						setGraphEdge(fromNodeGuid, toNodeGuid, {
							arrowhead: "arrowPoint",
							curve: curveBasis,
							style: getStyleObjStr(styleObj),
							styleObj: styleObj,
							...opt
						});
					},
					setGraphData = function(fromEntityId, toEntityId) {
						setNode(fromEntityId);
						setNode(toEntityId);
						setEdge(fromEntityId, toEntityId);
					},
					pendingFromRelationship = {};
				if (isHideFilterOn) {
					var relationshipMap = crateLineageRelationshipHashMap(data);
					Object.keys(relationshipMap).forEach((fromNodeGuid) => {
						var toNodes = relationshipMap[fromNodeGuid],
							fromNodeToBeUpdated = this.isNodeToBeUpdated(makeNodeData(guidEntityMap[fromNodeGuid]), filterObj),
							toNodeList = getToNodeRelation(toNodes, fromNodeToBeUpdated, filterObj);
						if (fromNodeToBeUpdated.update) {
							if (pendingFromRelationship[fromNodeGuid]) {
								pendingFromRelationship[fromNodeGuid] = pendingFromRelationship[fromNodeGuid].concat(toNodeList);
							} else {
								pendingFromRelationship[fromNodeGuid] = toNodeList;
							}
						} else {
							toNodeList.forEach(function(toNodeGuid) {
								setGraphData(fromNodeGuid, toNodeGuid);
							});
						}
					});
				} else {
					relations.forEach(function(obj) {
						setGraphData(obj.fromEntityId, obj.toEntityId);
					});
				}
				if (g._nodes[guid]) {
					if (g._nodes[guid]) {
						g._nodes[guid]["isLineage"] = false;
					}
					this.findImpactNodeAndUpdateData({
						guid,
						g,
						setEdge,
						getStyleObjStr
					});
				}
				resolve(g);
			} catch (e) {
				reject(e);
			}
		});
	},
	findImpactNodeAndUpdateData: function({ guid, getStyleObjStr, g, setEdge }) {
		var that = this,
			traversedMap = {},
			styleObj = {
				fill: "none",
				stroke: "#fb4200",
				width: 3
			},
			traversed = function(toNodeList = {}, fromNodeGuid) {
				let toNodeKeyList = Object.keys(toNodeList);
				if (toNodeKeyList.length) {
					if (!traversedMap[fromNodeGuid]) {
						traversedMap[fromNodeGuid] = true;
						toNodeKeyList.forEach(function(toNodeGuid) {
							if (g._nodes[toNodeGuid]) {
								g._nodes[toNodeGuid]["isLineage"] = false;
							}
							setEdge(fromNodeGuid, toNodeGuid, {
								style: getStyleObjStr(styleObj),
								styleObj: styleObj
							});
							traversed(g._sucs[toNodeGuid], toNodeGuid);
						});
					}
				}
			};
		traversed(g._sucs[guid], guid);
	}
};
export default DataUtils;