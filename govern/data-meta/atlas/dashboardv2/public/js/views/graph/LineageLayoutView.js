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

define(['require',
    'backbone',
    'hbs!tmpl/graph/LineageLayoutView_tmpl',
    'collection/VLineageList',
    'models/VEntity',
    'utils/Utils',
    'LineageHelper',
    'd3',
    'dagreD3',
    'd3-tip',
    'utils/Enums',
    'utils/UrlLinks',
    'utils/Globals',
    'utils/CommonViewFunction',
    'platform',
    'jquery-ui'
], function(require, Backbone, LineageLayoutViewtmpl, VLineageList, VEntity, Utils, LineageHelper, d3, dagreD3, d3Tip, Enums, UrlLinks, Globals, CommonViewFunction, platform) {
    'use strict';

    var LineageLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends LineageLayoutView */
        {
            _viewName: 'LineageLayoutView',

            template: LineageLayoutViewtmpl,
            className: "resizeGraph",

            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                graph: ".graph",
                checkHideProcess: "[data-id='checkHideProcess']",
                checkDeletedEntity: "[data-id='checkDeletedEntity']",
                selectDepth: 'select[data-id="selectDepth"]',
                selectNodeCount: 'select[data-id="selectNodeCount"]',
                filterToggler: '[data-id="filter-toggler"]',
                settingToggler: '[data-id="setting-toggler"]',
                searchToggler: '[data-id="search-toggler"]',
                boxClose: '[data-id="box-close"]',
                lineageFullscreenToggler: '[data-id="fullScreen-toggler"]',
                filterBox: '.filter-box',
                searchBox: '.search-box',
                settingBox: '.setting-box',
                lineageTypeSearch: '[data-id="typeSearch"]',
                searchNode: '[data-id="searchNode"]',
                nodeDetailTable: '[data-id="nodeDetailTable"]',
                showOnlyHoverPath: '[data-id="showOnlyHoverPath"]',
                showTooltip: '[data-id="showTooltip"]',
                saveSvg: '[data-id="saveSvg"]',
                resetLineage: '[data-id="resetLineage"]',
                onZoomIn: '[data-id="zoom-in"]',
                labelFullName: '[data-id="labelFullName"]',
                onZoomOut: '[data-id="zoom-out"]'
            },
            templateHelpers: function() {
                return {
                    width: "100%",
                    height: "100%",
                    compactLineageEnabled: Globals.isLineageOnDemandEnabled
                };
            },
            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.checkHideProcess] = 'onCheckUnwantedEntity';
                events["click " + this.ui.checkDeletedEntity] = 'onCheckUnwantedEntity';
                events['change ' + this.ui.selectDepth] = 'onSelectDepthChange';
                events['change ' + this.ui.selectNodeCount] = 'onSelectNodeCount';
                events["click " + this.ui.filterToggler] = 'onClickFilterToggler';
                events["click " + this.ui.boxClose] = 'toggleBoxPanel';
                events["click " + this.ui.settingToggler] = 'onClickSettingToggler';
                events["click " + this.ui.lineageFullscreenToggler] = 'onClickLineageFullscreenToggler';
                events["click " + this.ui.searchToggler] = 'onClickSearchToggler';
                events["click " + this.ui.saveSvg] = 'onClickSaveSvg';
                events["click " + this.ui.resetLineage] = 'onClickResetLineage';
                events["click " + this.ui.onZoomIn] = 'onClickZoomIn';
                events["click " + this.ui.onZoomOut] = 'onClickZoomOut';
                events["change " + this.ui.labelFullName] = "onClickLabelFullName";
                return events;
            },

            /**
             * intialize a new LineageLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, 'processCheck', 'guid', 'entity', 'entityName', 'entityDefCollection', 'actionCallBack', 'fetchCollection', 'attributeDefs'));
                this.collection = new VLineageList();
                this.typeMap = {};
                this.apiGuid = {};
                this.edgeCall;
                this.filterObj = {
                    isProcessHideCheck: false,
                    isDeletedEntityHideCheck: false,
                    depthCount: Globals.lineageDepth
                };
                this.searchNodeObj = {
                    selectedNode: ''
                }
                this.labelFullText = false;
            },
            onRender: function() {
                var that = this,
                    nodeCountArray = _.uniq([3, 6, Globals.lineageNodeCount]);
                this.initialQueryObj = {};
                this.ui.searchToggler.prop("disabled", true);
                this.$graphButtonsEl = this.$(".graph-button-group button, select[data-id='selectDepth']");
                if (Globals.isLineageOnDemandEnabled) {
                    this.ui.resetLineage.attr("title", "Reset Lineage");
                    this.initialQueryObj[this.guid] = {
                        "direction": "BOTH",
                        "inputRelationsLimit": Globals.lineageNodeCount,
                        "outputRelationsLimit": Globals.lineageNodeCount,
                        "depth": Globals.lineageDepth,
                    }
                }
                this.fetchGraphData({ queryParam: this.initialQueryObj });
                if (this.layoutRendered) {
                    this.layoutRendered();
                }
                if (this.processCheck) {
                    this.hideCheckForProcess();
                }
                if (this.entity.status === "DELETED") {
                    this.hideCheckForDeletedEntity();
                }
                //this.initializeGraph();
                this.ui.selectDepth.select2({
                    data: _.sortBy([3, 6, 9, 12, 15, 18, 21]),
                    tags: true,
                    dropdownCssClass: "number-input",
                    multiple: false
                });
                this.ui.selectNodeCount.select2({
                    data: _.sortBy(nodeCountArray),
                    tags: true,
                    dropdownCssClass: "number-input",
                    multiple: false
                });
                this.ui.selectNodeCount.val(Globals.lineageNodeCount).trigger("change");
            },
            onShow: function() {
                this.$('.fontLoader').show();
                // this.$el.resizable({
                //     handles: ' s',
                //     minHeight: 375,
                //     stop: function(event, ui) {
                //         ui.element.height(($(this).height()));
                //     },
                // });
            },
            onClickLineageFullscreenToggler: function(e) {
                var icon = $(e.currentTarget).find('i'),
                    panel = $(e.target).parents('.tab-pane').first();
                icon.toggleClass('fa-expand fa-compress');
                if (icon.hasClass('fa-expand')) {
                    Globals.isFullScreenView = false;
                    icon.parent('button').attr("data-original-title", "Full Screen");
                } else {
                    icon.parent('button').attr("data-original-title", "Default View");
                }
                panel.toggleClass('fullscreen-mode');
                var node = this.$("svg").parent()[0].getBoundingClientRect();
                this.LineageHelperRef.updateOptions({
                    width: node.width,
                    height: node.height
                });
                this.calculateLineageDetailPanelHeight();
            },
            onCheckUnwantedEntity: function(e) {
                var that = this;
                //this.initializeGraph();
                this.searchNodeObj.selectedNode = "";
                if ($(e.target).data("id") === "checkHideProcess") {
                    this.filterObj.isProcessHideCheck = e.target.checked;
                } else {
                    this.filterObj.isDeletedEntityHideCheck = e.target.checked;
                }
                this.renderLineageTypeSearch(this.data);
                this.LineageHelperRef.refresh({ compactLineageEnabled: Globals.isLineageOnDemandEnabled, filterObj: this.filterObj });
            },
            toggleBoxPanel: function(options) {
                var el = options && options.el,
                    nodeDetailToggler = options && options.nodeDetailToggler,
                    currentTarget = options.currentTarget;
                if (options.nodeDetailToggler) {
                    this.ui.lineageTypeSearch.select2("close");
                }
                this.$el.find('.show-box-panel').removeClass('show-box-panel');
                if (el && el.addClass) {
                    el.addClass('show-box-panel');
                }
                this.$('circle.node-detail-highlight').removeClass("node-detail-highlight");
            },
            toggleLoader: function(element) {
                if ((element).hasClass('fa-camera')) {
                    (element).removeClass('fa-camera').addClass("fa-spin-custom fa-refresh");
                } else {
                    (element).removeClass("fa-spin-custom fa-refresh").addClass('fa-camera');
                }
            },
            toggleDisableState: function(options) {
                var el = options.el,
                    disabled = options.disabled;
                if (el && el.prop) {
                    if (disabled) {
                        el.prop("disabled", disabled);
                    } else {
                        el.prop("disabled", !el.prop("disabled"));
                    }
                }
            },
            onClickNodeToggler: function(options) {
                this.toggleBoxPanel({ el: this.$('.lineage-node-detail'), nodeDetailToggler: true });
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
            onSelectDepthChange: function(e, options) {
                //this.initializeGraph();
                Globals.lineageDepth = parseInt(e.currentTarget.value);
                //legends property is added in queryParam to stop the legend getting added in lineage graph whenever dept is changed. 
                if (!Globals.isLineageOnDemandEnabled) {
                    this.fetchGraphData({ queryParam: { 'depth': Globals.lineageDepth }, 'legends': false });
                }

                if (Globals.isLineageOnDemandEnabled) {
                    this.initialQueryObj[this.guid].depth = Globals.lineageDepth;
                    this.fetchGraphData({ queryParam: this.initialQueryObj, 'legends': false })
                }
            },
            onSelectNodeCount: function(e, options) {
                Globals.lineageNodeCount = parseInt(e.target.value);
                if (Globals.lineageNodeCount === 0) {
                    Utils.notifyWarn({
                        content: 'Value cannot be less than 1'
                    });
                    this.ui.selectNodeCount.val(3).trigger("change");
                }
            },
            onClickResetLineage: function() {
                if (Globals.isLineageOnDemandEnabled) {
                    this.fetchGraphData({ queryParam: this.initialQueryObj, 'legends': false });
                }
                if (!Globals.isLineageOnDemandEnabled) {
                    this.LineageHelperRef.refresh();
                }
                this.searchNodeObj.selectedNode = "";
                this.ui.lineageTypeSearch.data({ refresh: true }).val("").trigger("change");
                this.ui.labelFullName.prop("checked", false);
                this.labelFullText = false;
            },
            onClickSaveSvg: function(e, a) {
                var that = this;
                if (that.lineageRelationshipLength >= 1000) {
                    Utils.notifyInfo({
                        content: "There was an error in downloading lineage: Lineage exceeds display parameters!"
                    });
                    return;
                }
                this.LineageHelperRef.exportLineage();
            },
            onClickZoomIn: function() {
                this.LineageHelperRef.zoomIn();
            },
            onClickZoomOut: function() {
                this.LineageHelperRef.zoomOut();
            },
            onClickLabelFullName: function() {
                this.labelFullText = !this.labelFullText;
                this.LineageHelperRef.displayFullName({ bLabelFullText: this.labelFullText });
            },
            fetchGraphData: function(options) {
                var that = this,
                    queryParam = options && options.queryParam || {};
                this.$('.fontLoader').show();
                this.$('svg>g').hide();
                this.toggleDisableState({
                    "el": that.$graphButtonsEl,
                    disabled: true
                });
                //Create data for displaying just entityNode when no relationships are present.
                var classificationNamesArray = [];
                if (this.entity.classifications) {
                    this.entity.classifications.forEach(function(item) {
                        classificationNamesArray.push(item.typeName);
                    });
                }
                this.currentEntityData = {
                    classificationNames: classificationNamesArray,
                    displayText: that.entity.attributes.name,
                    labels: [],
                    meaningNames: [],
                    meanings: []
                }
                _.extend(this.currentEntityData, _.pick(this.entity, 'attributes', 'guid', 'isIncomplete', 'status', 'typeName'));
                //End
                var dataObj = {
                    compactLineageEnabled: Globals.isLineageOnDemandEnabled,
                    success: function(data) {
                        if (that.isDestroyed) {
                            return;
                        }
                        data["legends"] = options ? options.legends : true;
                        that.lineageOnDemandPayload = data.lineageOnDemandPayload ? data.lineageOnDemandPayload : {};
                        // show only main part of lineage current entity is at bottom, so reverse is done
                        var relationsReverse = data.relations ? data.relations.reverse() : null,
                            lineageMaxRelationCount = 9000;
                        if (relationsReverse.length > lineageMaxRelationCount) {
                            data.relations = relationsReverse.splice(relationsReverse.length - lineageMaxRelationCount, relationsReverse.length - 1);
                            Utils.notifyInfo({
                                content: "Lineage exceeds display parameters and hence only upto 9000 relationships from this lineage can be displayed"
                            });
                        }
                        that.lineageRelationshipLength = data.relations.length;
                        if (_.isEmpty(data.relations)) {
                            if (_.isEmpty(data.guidEntityMap) || !data.guidEntityMap[data.baseEntityGuid]) {
                                data.guidEntityMap[data.baseEntityGuid] = that.currentEntityData;
                            }
                        }
                        that.data = data;
                        var updatedData = that.updateLineageData(data);
                        _.extend(data.guidEntityMap, updatedData.plusBtnsObj);
                        data.relations = data.relations.concat(updatedData.plusBtnRelationsArray);
                        that.createGraph(data);
                        that.renderLineageTypeSearch(data);
                    },
                    cust_error: function(model, response) {
                        that.noLineage();
                    },
                    complete: function() {
                        that.$('.fontLoader').hide();
                        that.$('svg>g').show();
                    }
                };
                Globals.isLineageOnDemandEnabled ? dataObj.data = queryParam : dataObj.queryParam = queryParam;

                this.collection.getLineage(this.guid, dataObj)
            },
            updateLineageData: function(data) {
                var that = this,
                    rawData = data,
                    plusBtnsObj = {},
                    plusBtnRelationsArray = [];
                this.relationsOnDemand = data.relationsOnDemand ? data.relationsOnDemand : null;
                this.lineageOnDemandPayload = data.lineageOnDemandPayload ? data.lineageOnDemandPayload : null;
                if (this.relationsOnDemand) {
                    _.each(this.relationsOnDemand, function(values, nodeId) {
                        if (values.hasMoreInputs) {
                            var btnType = "Input",
                                moreInputBtnObj = that.createExpandButtonObj({ nodeId: nodeId, btnType: btnType });
                            plusBtnsObj[moreInputBtnObj.guid] = moreInputBtnObj;
                            plusBtnRelationsArray.push({ fromEntityId: moreInputBtnObj.guid, toEntityId: nodeId, relationshipId: 'dummy' });
                        }
                        if (values.hasMoreOutputs) {
                            var btnType = "Output",
                                moreOutputBtnObj = that.createExpandButtonObj({ nodeId: nodeId, btnType: btnType });
                            plusBtnsObj[moreOutputBtnObj.guid] = moreOutputBtnObj;
                            plusBtnRelationsArray.push({ fromEntityId: nodeId, toEntityId: moreOutputBtnObj.guid, relationshipId: 'dummy' });
                        }
                    });
                    return {
                        plusBtnsObj: plusBtnsObj,
                        plusBtnRelationsArray: plusBtnRelationsArray
                    }
                }
            },
            generateAddButtonId: function(btnType) {
                return btnType + Math.random().toString(16).slice(2)
            },
            createExpandButtonObj: function(options) {
                var defaultObj = {
                        attributes: {
                            owner: '',
                            createTime: 0,
                            qualifiedName: 'PlusBtn',
                            name: 'PlusBtn',
                            description: ''
                        },
                        isExpandBtn: true,
                        classificationNames: [],
                        displayText: "Expand",
                        isIncomplete: false,
                        labels: [],
                        meaningNames: [],
                        meanings: [],
                        status: "ACTIVE",
                        typeName: "Table"
                    },
                    btnObj = Object.assign({}, defaultObj),
                    btnType = (options.btnType === "Input") ? "more-inputs" : "more-outputs",
                    btnId = this.generateAddButtonId(btnType);
                btnObj.guid = btnId;
                btnObj.parentNodeGuid = options.nodeId;
                btnObj.btnType = options.btnType;
                return btnObj;
            },
            createGraph: function(data) {
                var that = this;
                $('.resizeGraph').css("height", this.$('.svg').height() + "px");

                this.LineageHelperRef = new LineageHelper.default({
                    entityDefCollection: this.entityDefCollection.fullCollection.toJSON(),
                    data: data,
                    el: this.$('.svg')[0],
                    legendsEl: this.$('.legends')[0],
                    legends: data.legends,
                    getFilterObj: function() {
                        return {
                            isProcessHideCheck: that.filterObj.isProcessHideCheck,
                            isDeletedEntityHideCheck: that.filterObj.isDeletedEntityHideCheck
                        }
                    },
                    isShowHoverPath: function() { return that.ui.showOnlyHoverPath.prop('checked') },
                    isShowTooltip: function() { return that.ui.showTooltip.prop('checked') },
                    onPathClick: function(d) {
                        if (d.pathRelationObj) {
                            var relationshipId = d.pathRelationObj.relationshipId;
                            require(['views/graph/PropagationPropertyModal'], function(PropagationPropertyModal) {
                                var view = new PropagationPropertyModal({
                                    edgeInfo: d.pathRelationObj,
                                    relationshipId: relationshipId,
                                    lineageData: data,
                                    apiGuid: that.apiGuid,
                                    detailPageFetchCollection: that.fetchCollection
                                });
                            });
                        }
                    },
                    onNodeClick: function(d) {
                        if (d.clickedData.indexOf("more") >= 0) {
                            that.onExpandNodeClick({ guid: d.clickedData });
                            return;
                        }
                        that.onClickNodeToggler();
                        that.updateRelationshipDetails({ guid: d.clickedData });
                        that.calculateLineageDetailPanelHeight();
                    },
                    onLabelClick: function(d) {
                        var guid = d.clickedData;
                        Globals.isFullScreenView = true;
                        that.ui.lineageTypeSearch.select2("close");
                        if (guid.indexOf("more") >= 0) {
                            return;
                        }
                        if (that.guid == guid) {
                            Utils.notifyInfo({
                                html: true,
                                content: "You are already on " + "<b>" + that.entityName + "</b> detail page."
                            });
                        } else {
                            Utils.setUrl({
                                url: '#!/detailPage/' + guid + '?tabActive=lineage',
                                mergeBrowserUrl: false,
                                trigger: true
                            });
                        }
                    },
                    beforeRender: function() {
                        that.$('.fontLoader').show();
                        that.toggleDisableState({
                            "el": that.$graphButtonsEl,
                            disabled: true
                        });
                    },
                    afterRender: function() {
                        // Remove Loader
                        that.$('.fontLoader').hide();
                        if (data.relations.length) {
                            that.toggleDisableState({
                                "el": that.$graphButtonsEl,
                                disabled: false
                            });
                        }
                    }
                });
            },
            onExpandNodeClick: function(options) {
                var parentNodeData = this.LineageHelperRef.getNode(options.guid);
                this.updateQueryObject(parentNodeData.parentNodeGuid, parentNodeData.btnType);
            },
            updateQueryObject: function(parentId, btnType) {
                var inputLimit = null,
                    outputLimit = null,
                    that = this,
                    queryParam;
                if (_.has(that.lineageOnDemandPayload, parentId)) {
                    _.find(that.lineageOnDemandPayload, function(value, key) {
                        if (key === parentId) {
                            if (btnType === "Input") {
                                value.inputRelationsLimit = value.inputRelationsLimit + Globals.lineageNodeCount;
                                value.outputRelationsLimit = value.outputRelationsLimit;
                            }
                            if (btnType === "Output") {
                                value.inputRelationsLimit = value.inputRelationsLimit;
                                value.outputRelationsLimit = value.outputRelationsLimit + Globals.lineageNodeCount;
                            }
                        }
                    });
                } else {
                    var relationCount = that.validateInputOutputLimit(parentId, btnType);
                    if (btnType === "Input") {
                        inputLimit = relationCount.inputRelationCount + Globals.lineageNodeCount;
                        outputLimit = relationCount.outputRelationCount;
                    }
                    if (btnType === "Output") {
                        inputLimit = relationCount.inputRelationCount;
                        outputLimit = relationCount.outputRelationCount + Globals.lineageNodeCount;
                    }
                    this.lineageOnDemandPayload[parentId] = { direction: "BOTH", inputRelationsLimit: inputLimit, outputRelationsLimit: outputLimit, depth: Globals.lineageDepth };
                }
                this.fetchGraphData({ queryParam: this.lineageOnDemandPayload, 'legends': false });
            },
            validateInputOutputLimit: function(parentId, btnType) {
                var inputRelationCount, outputRelationCount;
                for (var guid in this.relationsOnDemand) {
                    if (parentId === guid && (btnType === "Input" || btnType === "Output")) {
                        inputRelationCount = this.relationsOnDemand[guid].inputRelationsCount ? this.relationsOnDemand[guid].inputRelationsCount : Globals.lineageNodeCount;
                        outputRelationCount = this.relationsOnDemand[guid].outputRelationsCount ? this.relationsOnDemand[guid].outputRelationsCount : Globals.lineageNodeCount;
                    }
                }
                return {
                    inputRelationCount: inputRelationCount,
                    outputRelationCount: outputRelationCount
                };
            },
            noLineage: function() {
                this.$('.fontLoader').hide();
                this.$('.depth-container').hide();
                this.$('svg').html('<text x="50%" y="50%" alignment-baseline="middle" text-anchor="middle">No lineage data found</text>');
                if (this.actionCallBack) {
                    this.actionCallBack();
                }
            },
            hideCheckForProcess: function() {
                this.$('.hideProcessContainer').hide();
            },
            hideCheckForDeletedEntity: function() {
                //This has been added to handle the scenario where hideDeletedEntityCheck should hide form filters when baseEntity is deleted.
                this.$('.hideDeletedContainer').hide();
            },
            renderLineageTypeSearch: function(data) {
                var that = this;
                return new Promise(function(resolve, reject) {
                    try {
                        var typeStr = '<option></option>';
                        if (!_.isEmpty(data)) {
                            _.each(data.guidEntityMap, function(obj, index) {
                                var nodeData = that.LineageHelperRef.getNode(obj.guid);
                                if ((that.filterObj.isProcessHideCheck && obj && obj.isProcess) || (that.filterObj.isDeletedEntityHideCheck && obj && obj.isDeleted) || (Globals.isLineageOnDemandEnabled && obj && _.contains(["Input", "Output"], obj.btnType))) {
                                    return;
                                }
                                typeStr += '<option value="' + obj.guid + '">' + obj.displayText + '</option>';
                            });
                        }
                        that.ui.lineageTypeSearch.html(typeStr);
                        that.initilizelineageTypeSearch();
                        resolve();
                    } catch (e) {
                        console.log(e);
                        reject(e);
                    }
                })
            },
            initilizelineageTypeSearch: function() {
                var that = this;
                this.ui.lineageTypeSearch.select2({
                    closeOnSelect: true,
                    placeholder: 'Select Node'
                }).on('change.select2', function(e) {
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                    if (!that.ui.lineageTypeSearch.data("refresh")) {
                        var selectedNode = $('[data-id="typeSearch"]').val();
                        that.searchNodeObj.selectedNode = selectedNode;
                        that.LineageHelperRef.searchNode({ guid: selectedNode });
                    } else {
                        that.ui.lineageTypeSearch.data("refresh", false);
                    }
                });
                if (this.searchNodeObj.selectedNode) {
                    this.ui.lineageTypeSearch.val(this.searchNodeObj.selectedNode);
                    this.ui.lineageTypeSearch.trigger("change.select2");
                }
            },
            updateRelationshipDetails: function(options) {
                var that = this,
                    guid = options.guid,
                    initialData = that.LineageHelperRef.getNode(guid);
                if (initialData === undefined) {
                    return;
                }
                var typeName = initialData.typeName || guid,
                    attributeDefs = initialData && initialData.entityDef ? initialData.entityDef.attributeDefs : null;
                this.$("[data-id='typeName']").text(typeName);
                this.entityModel = new VEntity({});
                var config = {
                    guid: 'guid',
                    typeName: 'typeName',
                    name: 'name',
                    qualifiedName: 'qualifiedName',
                    owner: 'owner',
                    createTime: 'createTime',
                    status: 'status',
                    classificationNames: 'classifications',
                    meanings: 'term'
                };
                var data = {};
                _.each(config, function(valKey, key) {
                    var val = initialData[key];
                    if (_.isUndefined(val) && initialData.attributes && initialData.attributes[key]) {
                        val = initialData.attributes[key];
                    }
                    if (val) {
                        data[valKey] = val;
                    }
                });
                this.ui.nodeDetailTable.html(CommonViewFunction.propertyTable({
                    "scope": this,
                    "valueObject": data,
                    "attributeDefs": attributeDefs,
                    "sortBy": false
                }));
            },
            calculateLineageDetailPanelHeight: function() {
                var $parentContainer = $('#tab-lineage .resizeGraph'),
                    $panel = $parentContainer.find('.fix-box');
                var $parentHeight = $parentContainer.find('.fix-box, tbody').removeAttr('style').height() - 48, // 48px is the Panels top from the parent container
                    $tBody = $panel.find('tbody'),
                    panelHeight = $tBody.height() + 100;
                if ($parentHeight < panelHeight) {
                    panelHeight = $parentHeight;
                }
                $panel.css('height', panelHeight + 'px');
                $tBody.css('height', '100%');
            }
        });
    return LineageLayoutView;
});