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

import dagreD3 from "dagre-d3";
import { select, selection, event } from "d3-selection";
import { curveBasis } from "d3-shape";
import { LineageUtils, DataUtils } from "./Utils";
import d3Tip from "d3-tip";

import "./styles/style.scss";

export default class LineageHelper {
    constructor(options) {
        this.options = {};
        this._updateOptions(options);
        const { el, manualTrigger = false } = this.options;
        if (el === undefined) {
            throw new Error("LineageHelper requires el propety to render the graph");
        }
        this.initReturnObj = {
            init: (arg) => this.init(arg),
            updateOptions: (options) => this._updateAllOptions(options),
            createGraph: (opt = {}) => this._createGraph(this.options, this.graphOptions, opt),
            clear: (arg) => this.clear(arg),
            refresh: (arg) => this.refresh(arg),
            centerAlign: (arg) => this.centerAlign(arg),
            exportLineage: (arg) => this.exportLineage(arg),
            zoomIn: (arg) => this.zoomIn(arg),
            zoomOut: (arg) => this.zoomOut(arg),
            zoom: (arg) => this.zoom(arg),
            fullScreen: (arg) => this.fullScreen(arg),
            searchNode: (arg) => this.searchNode(arg),
            removeNodeSelection: (arg) => this.removeNodeSelection(arg),
            getGraphOptions: () => this.graphOptions,
            getNode: (guid, actual) => {
                let rObj = null;
                if (actual) {
                    rObj = this.actualData[guid];
                } else {
                    rObj = this.g._nodes[guid];
                }
                if (rObj) {
                    rObj = Object.assign({}, rObj);
                }
                return rObj;
            },
            getNodes: (guid, actual) => {
                let rObj = null;
                if (actual) {
                    rObj = this.actualData;
                } else {
                    rObj = this.g._nodes;
                }
                if (rObj) {
                    rObj = Object.assign({}, rObj);
                }
                return rObj;
            },
            setNode: this._setGraphNode,
            setEdge: this._setGraphEdge
        };
        if (manualTrigger === false) {
            this.init();
        }
        return this.initReturnObj;
    }
    /**
     * [updateAllOptions]
     * @param  {[type]}
     * @return {[type]}
     */
    _updateAllOptions(options) {
        Object.assign(this.options, options);
        var svgRect = this.svg.node().getBoundingClientRect();
        this.graphOptions.width = this.options.width || svgRect.width;
        this.graphOptions.height = this.options.height || svgRect.height;
        const { svg, width, height, guid } = this.graphOptions;
        const { fitToScreen } = this.options;
        svg.select("g").node().removeAttribute("transform");
        svg.attr("viewBox", "0 0 " + width + " " + height).attr("enable-background", "new 0 0 " + width + " " + height);
        this.centerAlign({ fitToScreen, guid });
    }
    /**
     * [updateOptions get the options from user and appedn add it in this,option context]
     * @param  {[Object]} options [lib options from user]
     * @return {[null]}         [null]
     */
    _updateOptions(options) {
        Object.assign(this.options, { filterObj: { isProcessHideCheck: false, isDeletedEntityHideCheck: false } }, options);
    }
    /**
     * [init Start the graph build process]
     * @return {[null]} [null]
     */
    init() {
        const { data = {} } = this.options;
        if (data.baseEntityGuid) {
            this.guid = data.baseEntityGuid;
        }
        // Call the initializeGraph method to initlize dagreD3 graphlib
        this._initializeGraph();
        this._initGraph();
    }
    /**
     * [clear Allows user to clear the graph refrence and dom]
     * @return {[type]} [description]
     */
    clear() {
        if (!this.options.el) {
            this.svg.remove();
            this.svg = null;
        }
        this.g = null;
        this.graphOptions = {};
    }
    /**
     * [centerAlign Allows user to center the lineage position, without rerender]
     * @return {[type]} [description]
     */
    centerAlign(opt = {}) {
        var svgGroupEl = this.svg.select("g"),
            edgePathEl = svgGroupEl.selectAll("g.edgePath");
        LineageUtils.centerNode({
            ...this.graphOptions,
            svgGroupEl,
            edgePathEl,
            ...opt
        });
    }
    /**
     * [zoomIn description]
     * @return {[type]} [description]
     */
    zoomIn(opt = {}) {
        LineageUtils.zoomIn({ ...this.graphOptions, ...opt });
    }
    /**
     * [zoomOut description]
     * @return {[type]} [description]
     */
    zoomOut(opt = {}) {
        LineageUtils.zoomOut({ ...this.graphOptions, ...opt });
    }
    /**
     * [zoom description]
     * @return {[type]} [description]
     */
    zoom(opt = {}) {
        LineageUtils.zoom({ ...this.graphOptions, ...opt });
    }
    /**
     * [refresh Allows user to rerender the lineage]
     * @return {[type]} [description]
     */
    refresh() {
        this.clear();
        this._initializeGraph();
        this._initGraph({ refresh: true });
    }
    /**
     * [removeNodeSelection description]
     * @return {[type]} [description]
     */
    removeNodeSelection() {
        this.svg.selectAll("g.node>circle").classed("node-detail-highlight", false);
    }
    /**
     * [searchNode description]
     * @return {[type]} [description]
     */
    searchNode({ guid, onSearchNode }) {
        this.svg.selectAll(".serach-rect").remove();
        this.centerAlign({
            guid: guid,
            onCenterZoomed: function (opts) {
                const { selectedNodeEl } = opts;
                selectedNodeEl.select(".label").attr("stroke", "#316132");
                selectedNodeEl.select("circle").classed("wobble", true);
                selectedNodeEl
                    .insert("rect", "circle")
                    .attr("class", "serach-rect")
                    .attr("x", -50)
                    .attr("y", -27.5)
                    .attr("width", 100)
                    .attr("height", 55);
                if (onSearchNode && typeof onSearchNode === "function") {
                    onSearchNode(opts);
                }
            }
        });
    }

    /**
     * [exportLineage description]
     * @param  {Object} options [description]
     * @return {[type]}         [description]
     */
    exportLineage(options = {}) {
        let downloadFileName = options.downloadFileName;
        if (downloadFileName === undefined) {
            let node = this.g._nodes[this.guid];
            if (node && node.attributes) {
                downloadFileName = `${node.attributes.qualifiedName || node.attributes.name || "lineage_export"}.png`;
            } else {
                downloadFileName = "export.png";
            }
        }

        LineageUtils.saveSvg({
            ...this.graphOptions,
            downloadFileName: downloadFileName,
            onExportLineage: (opt) => {
                if (options.onExportLineage) {
                    onExportLineage(opt);
                }
            }
        });
    }
    /**
     * [fullScreen description]
     * @param  {Object} options.el }            [description]
     * @return {[type]}            [description]
     */
    fullScreen({ el } = {}) {
        if (el === undefined) {
            throw new Error("LineageHelper requires el propety to apply fullScreen class");
        }
        const fullScreenEl = select(el);
        if (fullScreenEl.classed("fullscreen-mode")) {
            fullScreenEl.classed("fullscreen-mode", false);
            return false;
        } else {
            fullScreenEl.classed("fullscreen-mode", true);
            return true;
        }
    }
    /**
     * [_getValueFromUser description]
     * @param  {[type]} ref [description]
     * @return {[type]}     [description]
     */
    _getValueFromUser(ref) {
        if (ref !== undefined) {
            if (typeof ref === "function") {
                return ref();
            } else {
                return ref;
            }
        }
        return;
    }
    /**
     * [initializeGraph initlize the dagreD3 graphlib]
     * @return {[null]} [null]
     */
    _initializeGraph() {
        const { width = "100%", height = "100%", el } = this.options;

        // Append the svg using d3.
        this.svg = select(el);

        if (!(el instanceof SVGElement)) {
            this.svg.selectAll("*").remove();
            this.svg = this.svg
                .append("svg")
                .attr("xmlns", "http://www.w3.org/2000/svg")
                .attr(" xmlns:xlink", "http://www.w3.org/1999/xlink")
                .attr("version", "1.1")
                .attr("width", width)
                .attr("height", height);
        }
        // initlize the dagreD3 graphlib
        this.g = new dagreD3.graphlib.Graph()
            .setGraph(
                Object.assign(
                    {
                        nodesep: 50,
                        ranksep: 90,
                        rankdir: "LR",
                        marginx: 20,
                        marginy: 20,
                        transition: function transition(selection) {
                            return selection.transition().duration(500);
                        }
                    },
                    this.options.dagreOptions
                )
            )
            .setDefaultEdgeLabel(function () {
                return {};
            });

        // Create graphOptions for common use
        var svgRect = this.svg.node().getBoundingClientRect();
        this.actualData = {};
        this.graphOptions = {
            svg: this.svg,
            g: this.g,
            dagreD3: dagreD3,
            guid: this.guid,
            width: this.options.width || svgRect.width,
            height: this.options.height || svgRect.height
        };
    }
    /**
     * [_initGraph description]
     * @return {[type]} [description]
     */
    _initGraph({ refresh } = {}) {
        if (this.svg) {
            this.svg.select("g").remove();
        }
        let filterObj = this.options.filterObj;
        if (this.options.getFilterObj) {
            let filterObjVal = this.options.getFilterObj();
            if (filterObjVal !== undefined || filterObjVal !== null) {
                if (typeof filterObjVal === "object") {
                    filterObj = filterObjVal;
                } else {
                    throw new Error("getFilterObj expect return type `object`,`null` or `Undefined`");
                }
            }
        }

        if (this.options.setDataManually === true) {
            return;
        } else if (this.options.data === undefined || (this.options.data && this.options.data.relations.length === 0)) {
            if (this.options.beforeRender) {
                this.options.beforeRender();
            }
            this.svg
                .append("text")
                .attr("x", "50%")
                .attr("y", "50%")
                .attr("alignment-baseline", "middle")
                .attr("text-anchor", "middle")
                .text("No lineage data found");
            if (this.options.afterRender) {
                this.options.afterRender();
            }
            return;
        }

        return DataUtils.generateData({
            ...this.options,
            filterObj: filterObj,
            ...this.graphOptions,
            setGraphNode: this._setGraphNode,
            setGraphEdge: this._setGraphEdge
        }).then((graphObj) => {
            this._createGraph(this.options, this.graphOptions, { refresh });
        });
    }

    /**
     * [description]
     * @param  {[type]} guid     [description]
     * @param  {[type]} nodeData [description]
     * @return {[type]}          [description]
     */
    _setGraphNode = (guid, nodeData) => {
        this.actualData[guid] = Object.assign({}, nodeData);
        this.g.setNode(guid, nodeData);
    };

    /**
     * [description]
     * @param  {[type]} fromGuid [description]
     * @param  {[type]} toGuid   [description]
     * @param  {[type]} opts     [description]
     * @return {[type]}          [description]
     */
    _setGraphEdge = (fromGuid, toGuid, opts) => {
        this.g.setEdge(fromGuid, toGuid, {
            curve: curveBasis,
            ...opts
        });
    };
    /**
     * [_createGraph description]
     * @param  {Object}  options.data    [description]
     * @param  {Boolean} isShowTooltip   [description]
     * @param  {Boolean} isShowHoverPath [description]
     * @param  {[type]}  onLabelClick    [description]
     * @param  {[type]}  onPathClick     [description]
     * @param  {[type]}  onNodeClick     }            [description]
     * @param  {[type]}  graphOptions    [description]
     * @return {[type]}                  [description]
     */
    _createGraph(
        {
            data = {},
            imgBasePath,
            isShowTooltip,
            isShowHoverPath,
            onLabelClick,
            onPathClick,
            onNodeClick,
            zoom,
            fitToScreen,
            getToolTipContent,
            toolTipTitle
        },
        graphOptions,
        { refresh }
    ) {
        if (this.options.beforeRender) {
            this.options.beforeRender();
        }
        const that = this,
            { svg, g, width, height } = graphOptions,
            isRankdirToBottom = this.options.dagreOptions && this.options.dagreOptions.rankdir === "tb";

        if (svg instanceof selection === false) {
            throw new Error("svg is not initialized or something went wrong while creatig graph instance");
            return;
        }
        if (g._nodes === undefined || g._nodes.length === 0) {
            svg.html('<text x="50%" y="50%" alignment-baseline="middle" text-anchor="middle">No relations to display</text>');
            return;
        }

        g.nodes().forEach(function (v) {
            var node = g.node(v);
            // Round the corners of the nodes
            if (node) {
                node.rx = node.ry = 5;
            }
        });

        svg.attr("viewBox", "0 0 " + width + " " + height).attr("enable-background", "new 0 0 " + width + " " + height);
        var svgGroupEl = svg.append("g");

        // Append defs
        var defsEl = svg.append("defs");

        // Create the renderer
        var render = new dagreD3.render();
        // Add our custom arrow (a hollow-point)
        render.arrows().arrowPoint = function () {
            return LineageUtils.arrowPointRender(...arguments, { ...graphOptions });
        };
        // Render custom img inside shape
        render.shapes().img = function () {
            return LineageUtils.imgShapeRender(...arguments, {
                ...graphOptions,
                isRankdirToBottom: isRankdirToBottom,
                imgBasePath: that._getValueFromUser(imgBasePath),
                defsEl
            });
        };

        var tooltip = d3Tip()
            .attr("class", "d3-tip")
            .offset([10, 0])
            .html((d) => {
                if (getToolTipContent && typeof getToolTipContent === "function") {
                    return getToolTipContent(d, g.node(d));
                } else {
                    var value = g.node(d);
                    var htmlStr = "";
                    if (toolTipTitle) {
                        htmlStr = "<h5 style='text-align: center;'>" + toolTipTitle + "</h5>";
                    } else if (value.id !== this.guid) {
                        htmlStr = "<h5 style='text-align: center;'>" + (value.isLineage ? "Lineage" : "Impact") + "</h5>";
                    }

                    htmlStr += "<h5 class='text-center'><span style='color:#359f89'>" + value.toolTipLabel + "</span></h5> ";
                    if (value.typeName) {
                        htmlStr += "<h5 class='text-center'><span>(" + value.typeName + ")</span></h5> ";
                    }
                    if (value.queryText) {
                        htmlStr += "<h5>Query: <span style='color:#359f89'>" + value.queryText + "</span></h5> ";
                    }
                    return "<div class='tip-inner-scroll'>" + htmlStr + "</div>";
                }
            });

        svg.call(tooltip);

        // if (platform.name !== "IE") {
        //  this.$(".fontLoader").hide();
        // }

        render(svgGroupEl, g);

        //change text postion
        svgGroupEl
            .selectAll("g.nodes g.label")
            .attr("transform", () => {
                if (isRankdirToBottom) {
                    return "translate(2,-20)";
                }
                return "translate(2,-35)";
            })
            .on("mouseenter", function (d) {
                event.preventDefault();
                select(this).classed("highlight", true);
            })
            .on("mouseleave", function (d) {
                event.preventDefault();
                select(this).classed("highlight", false);
            })
            .on("click", function (d) {
                event.preventDefault();
                if (onLabelClick && typeof onLabelClick === "function") {
                    onLabelClick({ clickedData: d });
                }
                tooltip.hide(d);
            });

        svgGroupEl
            .selectAll("g.nodes g.node circle")
            .on("mouseenter", function (d, index, element) {
                that.activeNode = true;
                var matrix = this.getScreenCTM().translate(+this.getAttribute("cx"), +this.getAttribute("cy"));
                that.svg.selectAll(".node").classed("active", false);
                select(this).classed("active", true);
                if (that._getValueFromUser(isShowTooltip)) {
                    var direction = LineageUtils.getToolTipDirection({ el: this });
                    tooltip.direction(direction).show(d, this);
                }
                if (that._getValueFromUser(isShowHoverPath) === false) {
                    return;
                }
                LineageUtils.onHoverFade({
                    opacity: 0.3,
                    mouseenter: true,
                    hoveredNode: d,
                    ...graphOptions
                });
            })
            .on("mouseleave", function (d) {
                that.activeNode = false;
                var nodeEL = this;
                setTimeout(function (argument) {
                    if (!(that.activeTip || that.activeNode)) {
                        select(nodeEL).classed("active", false);
                        if (that._getValueFromUser(isShowTooltip)) {
                            tooltip.hide(d);
                        }
                    }
                }, 150);
                if (that._getValueFromUser(isShowHoverPath) === false) {
                    return;
                }
                LineageUtils.onHoverFade({
                    mouseenter: false,
                    hoveredNode: d,
                    ...graphOptions
                });
            })
            .on("click", function (d) {
                if (event.defaultPrevented) return; // ignore drag
                event.preventDefault();
                tooltip.hide(d);
                svg.selectAll("g.node>circle").classed("node-detail-highlight", false);
                select(this).classed("node-detail-highlight", true);
                if (onNodeClick && typeof onNodeClick === "function") {
                    onNodeClick({ clickedData: d, el: this });
                }
            });

        // Bind event on edgePath
        var edgePathEl = svgGroupEl.selectAll("g.edgePath");
        edgePathEl.selectAll("path.path").on("click", function (d) {
            if (onPathClick && typeof onPathClick === "function") {
                var pathRelationObj = data.relations.find(function (obj) {
                    if (obj.fromEntityId === d.v && obj.toEntityId === d.w) {
                        return true;
                    }
                });
                onPathClick({ pathRelationObj, clickedData: d });
            }
        });

        // tooltip hover handle to fix node hover conflict
        // select("body").on("mouseover", ".d3-tip", function(el) {
        //  that.activeTip = true;
        // });
        // select("body").on("mouseleave", ".d3-tip", function(el) {
        //  that.activeTip = false;
        //  svg.selectAll(".node").classed("active", false);
        //  //tooltip.hide();
        // });

        // Center the graph
        if (zoom !== false) {
            LineageUtils.centerNode({
                ...graphOptions,
                fitToScreen,
                svgGroupEl,
                edgePathEl
            });
        }

        // if (platform.name === "IE") {
        //  LineageUtils.refreshGraphForIE({
        //      edgeEl: this.$("svg .edgePath")
        //  });
        // }

        LineageUtils.dragNode({
            ...graphOptions,
            edgePathEl
        });

        if (refresh !== true) {
            this._addLegend();
        }

        if (this.options.afterRender) {
            this.options.afterRender();
        }
    }
    _addLegend() {
        if (this.options.legends === false) {
            return;
        }
        var container = select(this.options.legendsEl || this.options.el)
            .insert("div", ":first-child")
            .classed("legends", true);

        let span = container.append("span").style("color", "#fb4200");
        span.append("i").classed("fa fa-circle-o fa-fw", true);
        span.append("span").html("Current Entity");

        span = container.append("span").style("color", "#686868");
        span.append("i").classed("fa fa-hourglass-half fa-fw", true);
        span.append("span").html("In Progress");

        span = container.append("span").style("color", "#df9b00");
        span.append("i").classed("fa fa-long-arrow-right fa-fw", true);
        span.append("span").html("Lineage");

        span = container.append("span").style("color", "#fb4200");
        span.append("i").classed("fa fa-long-arrow-right fa-fw", true);
        span.append("span").html("Impact");
    }
}