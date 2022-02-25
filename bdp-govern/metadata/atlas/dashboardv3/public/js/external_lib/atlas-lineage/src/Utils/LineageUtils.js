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

import { select, event } from "d3-selection";
import { zoom, zoomIdentity } from "d3-zoom";
import { drag } from "d3-drag";
import { line, curveBasis } from "d3-shape";

import platform from "platform";

import Enums from "../Enums";

const LineageUtils = {
    /**
     * [nodeArrowDistance variable use to define the distance between arrow and node]
     * @type {Number}
     */
    nodeArrowDistance: 24,
    refreshGraphForSafari: function (options) {
        var edgePathEl = options.edgeEl,
            IEGraphRenderDone = 0;
        edgePathEl.each(function (argument) {
            var eleRef = this,
                childNode = $(this).find("pattern");
            setTimeout(function (argument) {
                $(eleRef).find("defs").append(childNode);
            }, 500);
        });
    },
    refreshGraphForIE: function ({ edgePathEl }) {
        var IEGraphRenderDone = 0;
        edgePathEl.each(function (argument) {
            var childNode = $(this).find("marker");
            $(this).find("marker").remove();
            var eleRef = this;
            ++IEGraphRenderDone;
            setTimeout(function (argument) {
                $(eleRef).find("defs").append(childNode);
                --IEGraphRenderDone;
                if (IEGraphRenderDone === 0) {
                    this.$(".fontLoader").hide();
                    this.$("svg").fadeTo(1000, 1);
                }
            }, 1000);
        });
    },
    /**
     * [dragNode description]
     * @param  {[type]} options.g          [description]
     * @param  {[type]} options.svg        [description]
     * @param  {[type]} options.guid       [description]
     * @param  {[type]} options.edgePathEl [description]
     * @return {[type]}                    [description]
     */
    dragNode: function ({ g, svg, guid, edgePathEl }) {
        var dragHelper = {
            dragmove: function (el, d) {
                var node = select(el),
                    selectedNode = g.node(d),
                    prevX = selectedNode.x,
                    prevY = selectedNode.y;

                selectedNode.x += event.dx;
                selectedNode.y += event.dy;
                node.attr("transform", "translate(" + selectedNode.x + "," + selectedNode.y + ")");

                var dx = selectedNode.x - prevX,
                    dy = selectedNode.y - prevY;

                g.edges().forEach((e) => {
                    if (e.v == d || e.w == d) {
                        var edge = g.edge(e.v, e.w);
                        this.translateEdge(edge, dx, dy);
                        select(edge.elem).select("path").attr("d", this.calcPoints(e));
                    }
                });
                //LineageUtils.refreshGraphForIE({ edgePathEl: edgePathEl });
            },
            translateEdge: function (e, dx, dy) {
                e.points.forEach(function (p) {
                    p.x = p.x + dx;
                    p.y = p.y + dy;
                });
            },
            calcPoints: function (e) {
                var edge = g.edge(e.v, e.w),
                    tail = g.node(e.v),
                    head = g.node(e.w),
                    points = edge.points.slice(1, edge.points.length - 1),
                    afterslice = edge.points.slice(1, edge.points.length - 1);
                points.unshift(this.intersectRect(tail, points[0]));
                points.push(this.intersectRect(head, points[points.length - 1]));
                return line()
                    .x(function (d) {
                        return d.x;
                    })
                    .y(function (d) {
                        return d.y;
                    })
                    .curve(curveBasis)(points);
            },
            intersectRect: (node, point) => {
                var x = node.x,
                    y = node.y,
                    dx = point.x - x,
                    dy = point.y - y,
                    nodeDistance = guid ? this.nodeArrowDistance + 3 : this.nodeArrowDistance,
                    w = nodeDistance,
                    h = nodeDistance,
                    sx = 0,
                    sy = 0;

                if (Math.abs(dy) * w > Math.abs(dx) * h) {
                    // Intersection is top or bottom of rect.
                    if (dy < 0) {
                        h = -h;
                    }
                    sx = dy === 0 ? 0 : (h * dx) / dy;
                    sy = h;
                } else {
                    // Intersection is left or right of rect.
                    if (dx < 0) {
                        w = -w;
                    }
                    sx = w;
                    sy = dx === 0 ? 0 : (w * dy) / dx;
                }
                return {
                    x: x + sx,
                    y: y + sy
                };
            }
        };
        var dragNodeHandler = drag().on("drag", function (d) {
                dragHelper.dragmove.call(dragHelper, this, d);
            }),
            dragEdgePathHandler = drag().on("drag", function (d) {
                dragHelper.translateEdge(g.edge(d.v, d.w), event.dx, event.dy);
                var edgeObj = g.edge(d.v, d.w);
                select(edgeObj.elem).select("path").attr("d", dragHelper.calcPoints(d));
            });

        dragNodeHandler(svg.selectAll("g.node"));
        dragEdgePathHandler(svg.selectAll("g.edgePath"));
    },
    zoomIn: function ({ svg, scaleFactor = 1.3 }) {
        this.d3Zoom.scaleBy(svg.transition().duration(750), scaleFactor);
    },
    zoomOut: function ({ svg, scaleFactor = 0.8 }) {
        this.d3Zoom.scaleBy(svg.transition().duration(750), scaleFactor);
    },
    zoom: function ({ svg, xa, ya, scale }) {
        svg.transition().duration(750).call(this.d3Zoom.transform, zoomIdentity.translate(xa, ya).scale(scale));
    },
    fitToScreen: function ({ svg }) {
        var node = svg.node();
        var bounds = node.getBBox();

        var parent = node.parentElement,
            fullWidth = parent.clientWidth,
            fullHeight = parent.clientHeight;

        var width = bounds.width,
            height = bounds.height;
        var midX = bounds.x + width / 2,
            midY = bounds.y + height / 2;

        var scale = (scale || 0.95) / Math.max(width / fullWidth, height / fullHeight),
            xa = fullWidth / 2 - scale * midX,
            ya = fullHeight / 2 - scale * midY;
        this.zoom({ svg, xa, ya, scale });
    },
    /**
     * [centerNode description]
     * @param  {[type]} options.guid           [description]
     * @param  {[type]} options.g              [description]
     * @param  {[type]} options.svg            [description]
     * @param  {[type]} options.svgGroupEl     [description]
     * @param  {[type]} options.edgePathEl     [description]
     * @param  {[type]} options.width          [description]
     * @param  {[type]} options.height         [description]
     * @param  {[type]} options.onCenterZoomed [description]
     * @return {[type]}                        [description]
     */
    centerNode: function ({ guid, g, svg, svgGroupEl, edgePathEl, width, height, fitToScreen, onCenterZoomed }) {
        this.d3Zoom = zoom();
        svg.call(this.d3Zoom).on("dblclick.zoom", null);

        // restrict events

        let selectedNodeEl = svg.selectAll("g.nodes>g[id='" + guid + "']"),
            zoomListener = this.d3Zoom.scaleExtent([0.01, 50]).on("zoom", function () {
                svgGroupEl.attr("transform", event.transform);
            }),
            x = null,
            y = null,
            scale = 1.2;
        if (selectedNodeEl.empty()) {
            if (fitToScreen) {
                this.fitToScreen({ svg });
                return;
            } else {
                x = g.graph().width / 2;
                y = g.graph().height / 2;
            }
        } else {
            var matrix = selectedNodeEl
                .attr("transform")
                .replace(/[^0-9\-.,]/g, "")
                .split(",");
            // if (platform.name === "IE" || platform.name === "Microsoft Edge") {
            //     var matrix = selectedNode
            //         .attr("transform")
            //         .replace(/[a-z\()]/g, "")
            //         .split(" ");
            // }
            x = matrix[0];
            y = matrix[1];
        }

        var xa = -(x * scale - width / 2),
            ya = -(y * scale - height / 2);
        this.zoom({ svg, xa, ya, scale });
        svg.transition().duration(750).call(this.d3Zoom.transform, zoomIdentity.translate(xa, ya).scale(scale));

        if (onCenterZoomed) {
            onCenterZoomed({ newScale: scale, newTranslate: [xa, ya], d3Zoom: this.d3Zoom, selectedNodeEl });
        }
        // if (platform.name === "IE") {
        //     LineageUtils.refreshGraphForIE({ edgePathEl: edgePathEl });
        // }
    },
    /**
     * [getToolTipDirection description]
     * @param  {[type]} options.el [description]
     * @return {[type]}            [description]
     */
    getToolTipDirection: function ({ el }) {
        var width = select("body").node().getBoundingClientRect().width,
            currentELWidth = select(el).node().getBoundingClientRect(),
            direction = "e";
        if (width - currentELWidth.left < 330) {
            direction = width - currentELWidth.left < 330 && currentELWidth.top < 400 ? "sw" : "w";
            if (width - currentELWidth.left < 330 && currentELWidth.top > 600) {
                direction = "nw";
            }
        } else if (currentELWidth.top > 600) {
            direction = width - currentELWidth.left < 330 && currentELWidth.top > 600 ? "nw" : "n";
            if (currentELWidth.left < 50) {
                direction = "ne";
            }
        } else if (currentELWidth.top < 400) {
            direction = currentELWidth.left < 50 ? "se" : "s";
        }
        return direction;
    },
    /**
     * [onHoverFade description]
     * @param  {[type]} options.svg              [description]
     * @param  {[type]} options.g                [description]
     * @param  {[type]} options.mouseenter       [description]
     * @param  {[type]} options.opacity          [description]
     * @param  {[type]} options.nodesToHighlight [description]
     * @param  {[type]} options.hoveredNode      [description]
     * @return {[type]}                          [description]
     */
    onHoverFade: function ({ svg, g, mouseenter, nodesToHighlight, hoveredNode }) {
        var node = svg.selectAll(".node"),
            path = svg.selectAll(".edgePath"),
            isConnected = function (a, b, o) {
                if (a === o || (b && b.length && b.indexOf(o) != -1)) {
                    return true;
                }
            };
        if (mouseenter) {
            svg.classed("hover", true);
            var nextNode = g.successors(hoveredNode),
                previousNode = g.predecessors(hoveredNode),
                nodesToHighlight = nextNode.concat(previousNode);
            node.classed("hover-active-node", function (currentNode, i, nodes) {
                if (isConnected(hoveredNode, nodesToHighlight, currentNode)) {
                    return true;
                } else {
                    return false;
                }
            });
            path.classed("hover-active-path", function (c) {
                var _thisOpacity = c.v === hoveredNode || c.w === hoveredNode ? 1 : 0;
                if (_thisOpacity) {
                    return true;
                } else {
                    return false;
                }
            });
        } else {
            svg.classed("hover", false);
            node.classed("hover-active-node", false);
            path.classed("hover-active-path", false);
        }
    },
    /**
     * [getBaseUrl description]
     * @param  {[type]} path [description]
     * @return {[type]}      [description]
     */
    getBaseUrl: function (url = window.location.pathname) {
        return url.replace(/\/[\w-]+.(jsp|html)|\/+$/gi, "");
    },
    getEntityIconPath: function ({ entityData, errorUrl, imgBasePath }) {
        var iconBasePath = this.getBaseUrl() + (imgBasePath || "/img/entity-icon/");
        if (entityData) {
            let { typeName, serviceType, status, isProcess } = entityData;

            function getImgPath(imageName) {
                return iconBasePath + (Enums.entityStateReadOnly[status] ? "disabled/" + imageName : imageName);
            }

            function getDefaultImgPath() {
                if (isProcess) {
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

            if (errorUrl) {
                // Check if the default img path has error, if yes then stop recursion.
                if (errorUrl.indexOf("table.png") > -1 || errorUrl.indexOf("process.png") > -1) {
                    return null;
                }
                var isErrorInTypeName = errorUrl && errorUrl.match("entity-icon/" + typeName + ".png|disabled/" + typeName + ".png") ? true : false;
                if (serviceType && isErrorInTypeName) {
                    var imageName = serviceType + ".png";
                    return getImgPath(imageName);
                } else {
                    return getDefaultImgPath();
                }
            } else if (typeName) {
                var imageName = typeName + ".png";
                return getImgPath(imageName);
            } else if (serviceType) {
                var imageName = serviceType + ".png";
                return getImgPath(imageName);
            } else {
                return getDefaultImgPath();
            }
        }
    },
    base64Encode: function (file, callback) {
        const reader = new FileReader();
        reader.addEventListener("load", () => callback(reader.result));
        reader.readAsDataURL(file);
    },
    imgShapeRender: function (parent, bbox, node, { dagreD3, defsEl, imgBasePath, guid, isRankdirToBottom }) {
        var that = this,
            viewGuid = guid,
            imageIconPath = this.getEntityIconPath({ entityData: node, imgBasePath }),
            imgName = imageIconPath.split("/").pop();
        if (this.imageObject === undefined) {
            this.imageObject = {};
        }
        if (node.isDeleted) {
            imgName = "deleted_" + imgName;
        }
        if (node.id == viewGuid) {
            var currentNode = true;
        }
        var shapeSvg = parent
            .append("circle")
            .attr("fill", "url(#img_" + encodeURI(imgName) + ")")
            .attr("r", isRankdirToBottom ? "30px" : "24px")
            .attr("data-stroke", node.id)
            .attr("stroke-width", "2px")
            .attr("class", "nodeImage " + (currentNode ? "currentNode" : node.isProcess ? "process" : "node"));
        if (currentNode) {
            shapeSvg.attr("stroke", "#fb4200");
        }
        if (node.isIncomplete === true) {
            parent.attr("class", "node isIncomplete show");
            parent
                .insert("foreignObject")
                .attr("x", "-25")
                .attr("y", "-25")
                .attr("width", "50")
                .attr("height", "50")
                .append("xhtml:div")
                .insert("i")
                .attr("class", "fa fa-hourglass-half");
        }

        if (defsEl.select('pattern[id="img_' + imgName + '"]').empty()) {
            defsEl
                .append("pattern")
                .attr("x", "0%")
                .attr("y", "0%")
                .attr("patternUnits", "objectBoundingBox")
                .attr("id", "img_" + imgName)
                .attr("width", "100%")
                .attr("height", "100%")
                .append("image")
                .attr("href", function (d) {
                    var imgEl = this;
                    if (node) {
                        var getImageData = function (options) {
                            var imagePath = options.imagePath,
                                ajaxOptions = {
                                    url: imagePath,
                                    method: "GET",
                                    cache: true
                                };

                            // if (platform.name !== "IE") {
                            //     ajaxOptions["mimeType"] = "text/plain; charset=x-user-defined";
                            // }
                            shapeSvg.attr("data-iconpath", imagePath);
                            var xhr = new XMLHttpRequest();
                            xhr.onreadystatechange = function () {
                                if (xhr.readyState === 4) {
                                    if (xhr.status === 200) {
                                        if (platform.name !== "IE") {
                                            that.base64Encode(this.response, (url) => {
                                                that.imageObject[imageIconPath] = url;
                                                select(imgEl).attr("xlink:href", url);
                                            });
                                        } else {
                                            that.imageObject[imageIconPath] = imagePath;
                                        }
                                        if (imageIconPath !== shapeSvg.attr("data-iconpath")) {
                                            shapeSvg.attr("data-iconpathorigin", imageIconPath);
                                        }
                                    } else if (xhr.status === 404) {
                                        const imgPath = that.getEntityIconPath({ entityData: node, errorUrl: imagePath });
                                        if (imgPath === null) {
                                            const patternEL = select(imgEl.parentElement);
                                            patternEL.select("image").remove();
                                            patternEL
                                                .attr("patternContentUnits", "objectBoundingBox")
                                                .append("circle")
                                                .attr("r", "24px")
                                                .attr("fill", "#e8e8e8");
                                        } else {
                                            getImageData({
                                                imagePath: imgPath
                                            });
                                        }
                                    }
                                }
                            };
                            xhr.responseType = "blob";
                            xhr.open(ajaxOptions.method, ajaxOptions.url, true);
                            xhr.send(null);
                        };
                        getImageData({
                            imagePath: imageIconPath
                        });
                    }
                })
                .attr("x", isRankdirToBottom ? "11" : "4")
                .attr("y", isRankdirToBottom ? "20" : currentNode ? "3" : "4")
                .attr("width", "40")
                .attr("height", "40");
        }

        node.intersect = function (point) {
            return dagreD3.intersect.circle(node, currentNode ? that.nodeArrowDistance + 3 : that.nodeArrowDistance, point);
        };
        return shapeSvg;
    },
    /**
     * [arrowPointRender description]
     * @param  {[type]} {parent, id,           edge, type, viewOptions [description]
     * @return {[type]}           [description]
     */
    arrowPointRender: function (parent, id, edge, type, { dagreD3 }) {
        var node = parent.node(),
            parentNode = node ? node.parentNode : parent;
        select(parentNode)
            .select("path.path")
            .attr("marker-end", "url(#" + id + ")");
        var marker = parent
            .append("marker")
            .attr("id", id)
            .attr("viewBox", "0 0 10 10")
            .attr("refX", 8)
            .attr("refY", 5)
            .attr("markerUnits", "strokeWidth")
            .attr("markerWidth", 4)
            .attr("markerHeight", 4)
            .attr("orient", "auto");

        var path = marker.append("path").attr("d", "M 0 0 L 10 5 L 0 10 z").style("fill", edge.styleObj.stroke);
        dagreD3.util.applyStyle(path, edge[type + "Style"]);
    },
    /**
     * [saveSvg description]
     * @param  {[type]} options.svg              [description]
     * @param  {[type]} options.width            [description]
     * @param  {[type]} options.height           [description]
     * @param  {[type]} options.downloadFileName [description]
     * @param  {[type]} options.onExportLineage  [description]
     * @return {[type]}                          [description]
     */
    saveSvg: function ({ svg, width, height, downloadFileName, onExportLineage }) {
        var that = this,
            svgClone = svg.clone(true).node(),
            scaleFactor = 1;
        setTimeout(function () {
            if (platform.name === "Firefox") {
                svgClone.setAttribute("width", width);
                svgClone.setAttribute("height", height);
            }
            const hiddenSvgEl = select("body").append("div");
            hiddenSvgEl.classed("hidden-svg", true);
            hiddenSvgEl.node().appendChild(svgClone);

            const svgCloneEl = select(".hidden-svg svg");
            svgCloneEl.select("g").attr("transform", "scale(" + scaleFactor + ")");
            svgCloneEl.select("foreignObject").remove();

            var canvasOffset = { x: 150, y: 150 },
                setWidth = svgClone.getBBox().width + canvasOffset.x,
                setHeight = svgClone.getBBox().height + canvasOffset.y,
                xAxis = svgClone.getBBox().x,
                yAxis = svgClone.getBBox().y;
            svgClone.attributes.viewBox.value = xAxis + "," + yAxis + "," + setWidth + "," + setHeight;

            var canvas = document.createElement("canvas");
            canvas.id = "canvas";
            canvas.style.display = "none";
            canvas.width = svgClone.getBBox().width * scaleFactor + canvasOffset.x;
            canvas.height = svgClone.getBBox().height * scaleFactor + canvasOffset.y;

            // Append Canvas in DOM
            select("body").node().appendChild(canvas);

            var ctx = canvas.getContext("2d"),
                data = new XMLSerializer().serializeToString(svgClone),
                DOMURL = window.URL || window.webkitURL || window;

            ctx.fillStyle = "#FFFFFF";
            ctx.fillRect(0, 0, canvas.width, canvas.height);
            ctx.strokeRect(0, 0, canvas.width, canvas.height);
            ctx.restore();

            var img = new Image(canvas.width, canvas.height);
            var svgBlob = new Blob([data], { type: "image/svg+xml;base64" });
            if (platform.name === "Safari") {
                svgBlob = new Blob([data], { type: "image/svg+xml" });
            }
            var url = DOMURL.createObjectURL(svgBlob);

            img.onload = function () {
                try {
                    var a = document.createElement("a");
                    a.download = downloadFileName;
                    document.body.appendChild(a);
                    ctx.drawImage(img, 50, 50, canvas.width, canvas.height);
                    canvas.toBlob(function (blob) {
                        if (!blob) {
                            onExportLineage({ status: "failed", message: "There was an error in downloading Lineage!" });
                            return;
                        }
                        a.href = DOMURL.createObjectURL(blob);
                        if (blob.size > 10000000) {
                            onExportLineage({ status: "failed", message: "The Image size is huge, please open the image in a browser!" });
                        }
                        a.click();
                        onExportLineage({ status: "Success", message: "Successful" });
                        if (platform.name === "Safari") {
                            that.refreshGraphForSafari({
                                edgeEl: that.$("svg g.node")
                            });
                        }
                    }, "image/png");
                    hiddenSvgEl.remove();
                    canvas.remove();
                } catch (err) {
                    onExportLineage({ status: "failed", message: "There was an error in downloading Lineage!" });
                }
            };
            img.src = url;
        }, 0);
    }
};
export default LineageUtils;