/*
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
define([
    "require",
    "backbone",
    "hbs!tmpl/graph/TypeSystemTreeView_tmpl",
    "collection/VLineageList",
    "models/VEntity",
    "LineageHelper",
    "d3",
    "dagreD3",
    "d3-tip",
    "utils/CommonViewFunction",
    "utils/Utils",
    "platform",
    "jquery-ui"
], function(require, Backbone, TypeSystemTreeViewTmpl, VLineageList, VEntity, LineageHelper, d3, dagreD3, d3Tip, CommonViewFunction, Utils, platform) {
    "use strict";

    /** @lends TypeSystemTreeView */
    var TypeSystemTreeView = Backbone.Marionette.LayoutView.extend({
        _viewName: "TypeSystemTreeViewTmpl",

        template: TypeSystemTreeViewTmpl,
        templateHelpers: function() {
            return {
                modalID: this.viewId,
                width: "100%",
                height: "300px"
            };
        },

        /** Layout sub regions */
        regions: {
            RTypeSystemTreeViewPage: "#r_typeSystemTreeViewPage"
        },

        /** ui selector cache */
        ui: {
            typeSystemTreeViewPage: "[data-id='typeSystemTreeViewPage']",
            boxClose: '[data-id="box-close"]',
            nodeDetailTable: '[data-id="nodeDetailTable"]',
            attributeTable: '[data-id="attribute-table"]',
            typeSearch: '[data-id="typeSearch"]',
            filterServiceType: '[data-id="filterServiceType"]',
            onZoomIn: '[data-id="zoom-in"]',
            onZoomOut: '[data-id="zoom-out"]',
            filterBox: ".filter-box",
            searchBox: ".search-box",
            settingBox: '.setting-box',
            filterToggler: '[data-id="filter-toggler"]',
            settingToggler: '[data-id="setting-toggler"]',
            searchToggler: '[data-id="search-toggler"]',
            labelFullName: '[data-id="labelFullName"]',
            reset: '[data-id="reset"]',
            fullscreenToggler: '[data-id="fullScreen-toggler"]',
            noValueToggle: "[data-id='noValueToggle']",
            showOnlyHoverPath: '[data-id="showOnlyHoverPath"]',
            showTooltip: '[data-id="showTooltip"]',
            saveSvg: '[data-id="saveSvg"]',
        },
        /** ui events hash */
        events: function() {
            var events = {};
            events["click " + this.ui.boxClose] = "toggleBoxPanel";
            events["click " + this.ui.onZoomIn] = "onClickZoomIn";
            events["click " + this.ui.onZoomOut] = "onClickZoomOut";
            events["click " + this.ui.filterToggler] = "onClickFilterToggler";
            events["click " + this.ui.settingToggler] = 'onClickSettingToggler';
            events["click " + this.ui.searchToggler] = "onClickSearchToggler";
            events["click " + this.ui.saveSvg] = 'onClickSaveSvg';
            events["click " + this.ui.fullscreenToggler] = "onClickFullscreenToggler";
            events["click " + this.ui.reset] = "onClickReset";
            events["change " + this.ui.labelFullName] = "onClickLabelFullName";
            events["click " + this.ui.noValueToggle] = function() {
                this.showAllProperties = !this.showAllProperties;
                this.ui.noValueToggle.attr("data-original-title", (this.showAllProperties ? "Hide" : "Show") + " empty values");
                Utils.togglePropertyRelationshipTableEmptyValues({
                    "inputType": this.ui.noValueToggle,
                    "tableEl": this.ui.nodeDetailTable
                });
            };
            return events;
        },

        /**
         * @constructs
         */
        initialize: function(options) {
            _.extend(this, _.pick(options, "entityDefCollection"));
            this.labelFullText = false;
        },
        onShow: function() {
            this.$(".fontLoader").show();
            this.initializeGraph();
            this.fetchGraphData();
        },
        onRender: function() {
            var that = this;
            this.$el.on("click", "code li[data-def]", function() {
                if (that.selectedDetailNode) {
                    var dataObj = $(this).data(),
                        defObj = that.selectedDetailNode[dataObj.def],
                        newData = null;
                    if (dataObj.def === "businessAttributes") {
                        newData = defObj[dataObj.attribute];
                    } else {
                        newData = _.filter(defObj, { name: dataObj.attribute });
                    }
                    that.ui.attributeTable.find("pre").html('<code style="max-height: 100%">' + Utils.JSONPrettyPrint(newData, function(val) {
                        return val;
                    }) + '</code>');
                    that.$el.find('[data-id="typeAttrDetailHeader"]').text(dataObj.def);
                    that.ui.nodeDetailTable.hide("slide", { direction: "right" }, 400);
                    that.ui.attributeTable.show("slide", { direction: "left" }, 400);
                    that.$el.find(".typeDetailHeader").hide();
                    that.$el.find(".typeAttrDetailHeader").show()
                }
            });
            this.$el.on("click", "span[data-id='box-back']", function() {
                that.ui.nodeDetailTable.show("slide", { direction: "right" }, 400);
                that.ui.attributeTable.hide("slide", { direction: "left" }, 400);
                that.$el.find(".typeDetailHeader").show();
                that.$el.find(".typeAttrDetailHeader").hide()
            })
        },
        fetchGraphData: function(options) {
            var that = this;
            var entityTypeDef = that.entityDefCollection.fullCollection.toJSON();
            this.$(".fontLoader").show();
            this.$("svg").empty();
            if (that.isDestroyed) {
                return;
            }
            if (entityTypeDef.length) {
                that.generateData($.extend(true, {}, { data: entityTypeDef }, options)).then(function(graphObj) {
                    that.createGraph(options);
                });
            }
        },
        generateData: function(options) {
            return new Promise(
                function(resolve, reject) {
                    try {
                        var that = this,
                            newHashMap = {},
                            styleObj = {
                                fill: "none",
                                stroke: "#ffb203",
                                width: 3
                            },
                            makeNodeData = function(relationObj) {
                                if (relationObj) {
                                    if (relationObj.updatedValues) {
                                        return relationObj;
                                    }
                                    var obj = _.extend(relationObj, {
                                        shape: "img",
                                        updatedValues: true,
                                        label: relationObj.name.trunc(18),
                                        toolTipLabel: relationObj.name,
                                        id: relationObj.guid,
                                        isLineage: true,
                                        isIncomplete: false
                                    });
                                    return obj;
                                }
                            },
                            getStyleObjStr = function(styleObj) {
                                return "fill:" + styleObj.fill + ";stroke:" + styleObj.stroke + ";stroke-width:" + styleObj.width;
                            },
                            setNode = function(guid, obj) {
                                var node = that.LineageHelperRef.getNode(guid);
                                if (!node) {
                                    var nodeData = makeNodeData(obj);
                                    that.LineageHelperRef.setNode(guid, nodeData);
                                    return nodeData;
                                } else {
                                    return node;
                                }
                            },
                            setEdge = function(fromNodeGuid, toNodeGuid) {
                                that.LineageHelperRef.setEdge(fromNodeGuid, toNodeGuid, {
                                    arrowhead: "arrowPoint",
                                    style: getStyleObjStr(styleObj),
                                    styleObj: styleObj
                                });
                            },
                            setGraphData = function(fromEntityId, toEntityId) {
                                setNode(fromEntityId);
                                setNode(toEntityId);
                                setEdge(fromEntityId, toEntityId);
                            };

                        if (options.data) {
                            if (options.filter) {
                                // filter
                                var pendingSuperList = {},
                                    outOfFilterData = {},
                                    doneList = {};

                                var linkParents = function(obj) {
                                    if (obj && obj.superTypes.length) {
                                        _.each(obj.superTypes, function(superType) {
                                            var fromEntityId = obj.guid;
                                            var tempObj = doneList[superType] || outOfFilterData[superType];
                                            if (tempObj) {
                                                if (!doneList[superType]) {
                                                    setNode(tempObj.guid, tempObj);
                                                }
                                                setEdge(tempObj.guid, fromEntityId);
                                                linkParents(tempObj);
                                            } else {
                                                if (pendingSuperList[superType]) {
                                                    pendingSuperList[superType].push(fromEntityId);
                                                } else {
                                                    pendingSuperList[superType] = [fromEntityId];
                                                }
                                            }
                                        });
                                    }
                                }
                                _.each(options.data, function(obj) {
                                    var fromEntityId = obj.guid;
                                    if (pendingSuperList[obj.name]) {
                                        doneList[obj.name] = obj;
                                        setNode(fromEntityId, obj);
                                        _.map(pendingSuperList[obj.name], function(guid) {
                                            setEdge(fromEntityId, guid);
                                        });
                                        delete pendingSuperList[obj.name];
                                        linkParents(obj);
                                    }
                                    if (obj.serviceType === options.filter) {
                                        doneList[obj.name] = obj;
                                        setNode(fromEntityId, obj);
                                        linkParents(obj);
                                    } else if (!doneList[obj.name] && !outOfFilterData[obj.name]) {
                                        outOfFilterData[obj.name] = obj;
                                    }
                                });
                                pendingSuperList = null;
                                doneList = null;
                                outOfFilterData = null;
                            } else {
                                // Without filter
                                var pendingList = {},
                                    doneList = {};

                                _.each(options.data, function(obj) {
                                    var fromEntityId = obj.guid;
                                    doneList[obj.name] = obj;
                                    setNode(fromEntityId, obj);
                                    if (pendingList[obj.name]) {
                                        _.map(pendingList[obj.name], function(guid) {
                                            setEdge(guid, fromEntityId);
                                        });
                                        delete pendingList[obj.name];
                                    }
                                    if (obj.subTypes.length) {
                                        _.each(obj.subTypes, function(subTypes) {
                                            //var subTypesObj = _.find(options.data({ name: superTypes });
                                            //setNode(superTypeObj.attributes.guid, superTypeObj.attributes);
                                            if (doneList[subTypes]) {
                                                setEdge(fromEntityId, doneList[subTypes].guid);
                                            } else {
                                                if (pendingList[subTypes]) {
                                                    pendingList[subTypes].push(fromEntityId);
                                                } else {
                                                    pendingList[subTypes] = [fromEntityId];
                                                }
                                            }
                                        });
                                    }
                                });
                                pendingList = null;
                                doneList = null;
                            }
                        }
                        resolve(this.g);
                    } catch (e) {
                        reject(e);
                    }
                }.bind(this)
            );
        },
        toggleBoxPanel: function(options) {
            var el = options && options.el,
                nodeDetailToggler = options && options.nodeDetailToggler,
                currentTarget = options.currentTarget;
            this.$el.find(".show-box-panel").removeClass("show-box-panel");
            if (el && el.addClass) {
                el.addClass("show-box-panel");
            }
            this.$("circle.node-detail-highlight").removeClass("node-detail-highlight");
        },
        onClickNodeToggler: function(options) {
            this.toggleBoxPanel({ el: this.$(".lineage-node-detail"), nodeDetailToggler: true });
        },
        onClickZoomIn: function() {
            this.LineageHelperRef.zoomIn();
        },
        onClickZoomOut: function() {
            this.LineageHelperRef.zoomOut();
        },
        onClickFilterToggler: function() {
            this.toggleBoxPanel({ el: this.ui.filterBox });
        },
        onClickSettingToggler: function() {
            this.toggleBoxPanel({ el: this.ui.settingBox });
        },
        onClickSearchToggler: function() {
            this.toggleBoxPanel({ el: this.ui.searchBox });
        },
        onClickLabelFullName: function() {
            this.labelFullText = !this.labelFullText;
            this.LineageHelperRef.displayFullName({ bLabelFullText : this.labelFullText });
        },
        onClickReset: function() {
            this.fetchGraphData({ refresh: true });
            this.ui.typeSearch.data({ refresh: true }).val("").trigger("change");
            this.ui.filterServiceType.data({ refresh: true }).val("").trigger("change");
            this.ui.labelFullName.prop("checked", false);
            this.labelFullText = false;
        },
        onClickSaveSvg: function(e, a) {
            this.LineageHelperRef.exportLineage({ downloadFileName: "TypeSystemView" });
        },
        onClickFullscreenToggler: function(e) {
            var icon = $(e.currentTarget).find("i"),
                panel = $(e.target).parents(".tab-pane").first();
            icon.toggleClass("fa-expand fa-compress");
            if (icon.hasClass("fa-expand")) {
                icon.parent("button").attr("data-original-title", "Full Screen");
            } else {
                icon.parent("button").attr("data-original-title", "Default View");
            }
            panel.toggleClass("fullscreen-mode");
            var node = this.$("svg.main").parent()[0].getBoundingClientRect();
            this.LineageHelperRef.updateOptions({
                width: node.width,
                height: node.height
            });
            this.calculateLineageDetailPanelHeight();
        },
        updateDetails: function(data) {
            this.$("[data-id='typeName']").text(Utils.getName(data));

            this.selectedDetailNode = {};
            this.selectedDetailNode.atttributes = data.attributeDefs;
            this.selectedDetailNode.businessAttributes = data.businessAttributeDefs;
            this.selectedDetailNode.relationshipAttributes = data.relationshipAttributeDefs;
            //atttributes
            data["atttributes"] = (data.attributeDefs || []).map(function(obj) {
                return obj.name;
            });
            //businessAttributes
            data["businessAttributes"] = _.keys(data.businessAttributeDefs);
            //relationshipAttributes
            data["relationshipAttributes"] = (data.relationshipAttributeDefs || []).map(function(obj) {
                return obj.name;
            });

            this.ui.nodeDetailTable.html(
                CommonViewFunction.propertyTable({
                    scope: this,
                    guidHyperLink: false,
                    getValue: function(val, key) {
                        if (key && key.toLowerCase().indexOf("time") > 0) {
                            return Utils.formatDate({ date: val });
                        } else {
                            return val;
                        }
                    },
                    getArrayOfStringElement: function(val, key) {
                        var def = null,
                            classname = "json-string";
                        if (key === "atttributes" || key === "businessAttributes" || key === "relationshipAttributes") {
                            def = key;
                            classname += " cursor";
                        }
                        return "<li class='" + classname + "' " + (def ? 'data-def="' + def + '" data-attribute="' + val + '"' : '') + ">" + val + "</li>"
                    },
                    getArrayOfStringFormat: function(valueArray) {
                        return valueArray.join("");
                    },
                    getEmptyString: function(key) {
                        if (key === "subTypes" || key === "superTypes" || key === "atttributes" || key === "relationshipAttributes") {
                            return "[]";
                        }
                        return "N/A";
                    },
                    valueObject: _.omit(data, ["id", "attributeDefs", "relationshipAttributeDefs", "businessAttributeDefs", "isLineage", "isIncomplete", "label", "shape", "toolTipLabel", "updatedValues"]),
                    sortBy: true
                })
            );
        },
        createGraph: function(opt) {
            this.LineageHelperRef.createGraph();
        },
        filterData: function(value) {
            this.LineageHelperRef.refresh();
            this.fetchGraphData({ filter: value });
        },
        initializeGraph: function() {
            //ref - https://bl.ocks.org/seemantk/80613e25e9804934608ac42440562168
            var that = this,
                node = this.$("svg.main").parent()[0].getBoundingClientRect();
            this.$("svg").attr("viewBox", "0 0 " + node.width + " " + node.height);
            this.LineageHelperRef = new LineageHelper.default({
                el: this.$("svg.main")[0],
                legends: false,
                setDataManually: true,
                width: node.width,
                height: node.height,
                zoom: true,
                fitToScreen: true,
                dagreOptions: {
                    rankdir: "tb"
                },
                toolTipTitle: "Type",
                isShowHoverPath: function() { return that.ui.showOnlyHoverPath.prop('checked') },
                isShowTooltip: function() { return that.ui.showTooltip.prop('checked') },
                onNodeClick: function(d) {
                    that.onClickNodeToggler();
                    that.updateDetails(that.LineageHelperRef.getNode(d.clickedData, true));
                    that.calculateLineageDetailPanelHeight();
                },
                beforeRender: function() {
                    that.$(".fontLoader").show();
                },
                afterRender: function() {
                    that.graphOptions = that.LineageHelperRef.getGraphOptions();
                    that.renderTypeFilterSearch();
                    that.$(".fontLoader").hide();
                    return;
                }
            });
        },
        renderTypeFilterSearch: function(data) {
            var that = this;
            var searchStr = "<option></option>",
                filterStr = "<option></option>",
                tempFilteMap = {};
            var nodes = that.LineageHelperRef.getNodes();
            if (!_.isEmpty(nodes)) {
                _.each(nodes, function(obj) {
                    searchStr += '<option value="' + obj.guid + '">' + obj.name + "</option>";
                    if (obj.serviceType && !tempFilteMap[obj.serviceType]) {
                        tempFilteMap[obj.serviceType] = obj.serviceType;
                        filterStr += '<option value="' + obj.serviceType + '">' + obj.serviceType + "</option>";
                    }
                });
            }
            this.ui.typeSearch.html(searchStr);
            if (!this.ui.filterServiceType.data("select2")) {
                this.ui.filterServiceType.html(filterStr);
            }

            this.initilizeTypeFilterSearch();
        },
        initilizeTypeFilterSearch: function() {
            var that = this;
            this.ui.typeSearch
                .select2({
                    closeOnSelect: true,
                    placeholder: "Select Type"
                })
                .on("change.select2", function(e) {
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                    if (!that.ui.typeSearch.data("refresh")) {
                        var selectedNode = $('[data-id="typeSearch"]').val();
                        that.LineageHelperRef.searchNode({ guid: selectedNode });
                    } else {
                        that.ui.typeSearch.data("refresh", false);
                    }
                });
            if (!this.ui.filterServiceType.data("select2")) {
                this.ui.filterServiceType
                    .select2({
                        closeOnSelect: true,
                        placeholder: "Select ServiceType"
                    })
                    .on("change.select2", function(e) {
                        e.stopPropagation();
                        e.stopImmediatePropagation();
                        if (!that.ui.filterServiceType.data("refresh")) {
                            var selectedNode = $('[data-id="filterServiceType"]').val();
                            that.filterData(selectedNode);
                        } else {
                            that.ui.filterServiceType.data("refresh", false);
                        }
                    });
            }
        },
        calculateLineageDetailPanelHeight: function(){
            this.ui.typeSystemTreeViewPage.find('tbody').removeAttr('style');
            var $panel = this.ui.typeSystemTreeViewPage.find('.fix-box'),
                $parentHeight = this.ui.typeSystemTreeViewPage.height() - 48, // 48px is the Panels top from the parent container
                $tBody = $panel.find('tbody'),
                panelHeight = $tBody.height() + 37; //37px is height of Panel Header
                if($parentHeight < panelHeight){
                    panelHeight = $parentHeight;
                }
                $panel.css('height', panelHeight  + 'px');
                $tBody.css('height', '100%');
        }
    });
    return TypeSystemTreeView;
});