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

define([
    "require",
    "backbone",
    "hbs!tmpl/graph/RelationshipLayoutView_tmpl",
    "collection/VLineageList",
    "models/VEntity",
    "utils/Utils",
    "utils/CommonViewFunction",
    "d3",
    "d3-tip",
    "utils/Enums",
    "utils/UrlLinks",
    "platform"
], function(require, Backbone, RelationshipLayoutViewtmpl, VLineageList, VEntity, Utils, CommonViewFunction, d3, d3Tip, Enums, UrlLinks, platform) {
    "use strict";

    var RelationshipLayoutView = Backbone.Marionette.LayoutView.extend(
        /** @lends RelationshipLayoutView */
        {
            _viewName: "RelationshipLayoutView",

            template: RelationshipLayoutViewtmpl,
            className: "resizeGraph",
            /** Layout sub regions */
            regions: {},

            /** ui selector cache */
            ui: {
                relationshipDetailClose: '[data-id="close"]',
                searchNode: '[data-id="searchNode"]',
                relationshipViewToggle: 'input[name="relationshipViewToggle"]',
                relationshipDetailTable: "[data-id='relationshipDetailTable']",
                relationshipSVG: "[data-id='relationshipSVG']",
                relationshipDetailValue: "[data-id='relationshipDetailValue']",
                zoomControl: "[data-id='zoomControl']",
                boxClose: '[data-id="box-close"]',
                noValueToggle: "[data-id='noValueToggle']",
                relationshipDetails: ".relationship-details",
                noData: ".no-data"
            },

            /** ui events hash */
            events: function() {
                var events = {};
                events["click " + this.ui.relationshipDetailClose] = function() {
                    this.toggleInformationSlider({ close: true });
                };
                events["keyup " + this.ui.searchNode] = "searchNode";
                events["click " + this.ui.boxClose] = "toggleBoxPanel";
                events["change " + this.ui.relationshipViewToggle] = function(e) {
                    this.relationshipViewToggle(e.currentTarget.checked);
                };
                events["click " + this.ui.noValueToggle] = function(e) {
                    Utils.togglePropertyRelationshipTableEmptyValues({
                        inputType: this.ui.noValueToggle,
                        tableEl: this.ui.relationshipDetailValue
                    });
                };
                return events;
            },

            /**
             * intialize a new RelationshipLayoutView Layout
             * @constructs
             */
            initialize: function(options) {
                _.extend(this, _.pick(options, "entity", "entityName", "guid", "actionCallBack", "attributeDefs"));
                this.graphData = this.createData(this.entity);
            },
            createData: function(entity) {
                var that = this,
                    links = [],
                    nodes = {};
                if (entity && entity.relationshipAttributes) {
                    _.each(entity.relationshipAttributes, function(obj, key) {
                        if (!_.isEmpty(obj)) {
                            links.push({
                                source: nodes[that.entity.typeName] ||
                                    (nodes[that.entity.typeName] = _.extend({ name: that.entity.typeName }, { value: entity })),
                                target: nodes[key] ||
                                    (nodes[key] = _.extend({
                                        name: key
                                    }, { value: obj })),
                                value: obj
                            });
                        }
                    });
                }
                return { nodes: nodes, links: links };
            },
            onRender: function() {
                this.ui.zoomControl.hide();
                this.$el.addClass("auto-height");
            },
            onShow: function(argument) {
                if (this.graphData && _.isEmpty(this.graphData.links)) {
                    this.noRelationship();
                } else {
                    this.createGraph(this.graphData);
                }
                this.createTable();
            },
            noRelationship: function() {
                this.ui.relationshipDetails.hide();
                this.ui.noData.show();
                //this.$('svg').html('<text x="50%" y="50%" alignment-baseline="middle" text-anchor="middle">No relationship data found</text>');
            },
            toggleInformationSlider: function(options) {
                if (options.open && !this.$(".relationship-details").hasClass("open")) {
                    this.$(".relationship-details").addClass("open");
                } else if (options.close && this.$(".relationship-details").hasClass("open")) {
                    d3.selectAll("circle").attr("stroke", "none");
                    this.$(".relationship-details").removeClass("open");
                }
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
            searchNode: function(e) {
                var $el = $(e.currentTarget);
                this.updateRelationshipDetails(_.extend({}, $el.data(), { searchString: $el.val() }));
            },
            updateRelationshipDetails: function(options) {
                var data = options.obj.value,
                    typeName = data.typeName || options.obj.name,
                    searchString = _.escape(options.searchString),
                    listString = "",
                    getEntityTypelist = function(options) {
                        var activeEntityColor = "#4a90e2",
                            deletedEntityColor = "#BB5838",
                            entityTypeHtml = "<pre>",
                            getdefault = function(obj) {
                                var options = obj.options,
                                    status = Enums.entityStateReadOnly[options.entityStatus || options.status] ? " deleted-relation" : "",
                                    guid = options.guid,
                                    entityColor = obj.color,
                                    name = obj.name,
                                    typeName = options.typeName;
                                if (typeName === "AtlasGlossaryTerm") {
                                    return '<li class=' + status + '>' +
                                        '<a style="color:' + entityColor + '" href="#!/glossary/' + guid + '?guid=' + guid + '&gType=term&viewType=term&fromView=entity">' + name + ' (' + typeName + ')</a>' +
                                        '</li>';
                                } else {
                                    return "<li class=" + status + ">" +
                                        "<a style='color:" + entityColor + "' href=#!/detailPage/" + guid + "?tabActive=relationship>" + name + " (" + typeName + ")</a>" +
                                        "</li>";
                                }
                            },
                            getWithButton = function(obj) {
                                var options = obj.options,
                                    status = Enums.entityStateReadOnly[options.entityStatus || options.status] ? " deleted-relation" : "",
                                    guid = options.guid,
                                    entityColor = obj.color,
                                    name = obj.name,
                                    typeName = options.typeName,
                                    relationship = obj.relationship || false,
                                    entity = obj.entity || false,
                                    icon = '<i class="fa fa-trash"></i>',
                                    title = "Deleted";
                                if (relationship) {
                                    icon = '<i class="fa fa-long-arrow-right"></i>';
                                    status = Enums.entityStateReadOnly[options.relationshipStatus || options.status] ? "deleted-relation" : "";
                                    title = "Relationship Deleted";
                                }
                                return "<li class=" + status + ">" +
                                    "<a style='color:" + entityColor + "' href=#!/detailPage/" + options.guid + "?tabActive=relationship>" + _.escape(name) + " (" + options.typeName + ")</a>" +
                                    '<button type="button" title="' + title + '" class="btn btn-sm deleteBtn deletedTableBtn btn-action ">' + icon + '</button>' +
                                    "</li>";
                            };

                        var name = options.entityName ? options.entityName : Utils.getName(options, "displayText");
                        if (options.entityStatus == "ACTIVE") {
                            if (options.relationshipStatus == "ACTIVE") {
                                entityTypeHtml = getdefault({
                                    color: activeEntityColor,
                                    options: options,
                                    name: name
                                });
                            } else if (options.relationshipStatus == "DELETED") {
                                entityTypeHtml = getWithButton({
                                    color: activeEntityColor,
                                    options: options,
                                    name: name,
                                    relationship: true
                                });
                            }
                        } else if (options.entityStatus == "DELETED") {
                            entityTypeHtml = getWithButton({
                                color: deletedEntityColor,
                                options: options,
                                name: name,
                                entity: true
                            });
                        } else {
                            entityTypeHtml = getdefault({
                                color: activeEntityColor,
                                options: options,
                                name: name
                            });
                        }
                        return entityTypeHtml + "</pre>";
                    };
                this.ui.searchNode.hide();
                this.$("[data-id='typeName']").text(typeName);
                var getElement = function(options) {
                    var name = options.entityName ? options.entityName : Utils.getName(options, "displayText");
                    var entityTypeButton = getEntityTypelist(options);
                    return entityTypeButton;
                };
                if (_.isArray(data)) {
                    if (data.length > 1) {
                        this.ui.searchNode.show();
                    }
                    _.each(_.sortBy(data, "displayText"), function(val) {
                        var name = Utils.getName(val, "displayText"),
                            valObj = _.extend({}, val, { entityName: name });
                        if (searchString) {
                            if (name.search(new RegExp(searchString, "i")) != -1) {
                                listString += getElement(valObj);
                            } else {
                                return;
                            }
                        } else {
                            listString += getElement(valObj);
                        }
                    });
                } else {
                    listString += getElement(data);
                }
                this.$("[data-id='entityList']").html(listString);
            },
            createGraph: function(data) {
                //Ref - http://bl.ocks.org/fancellu/2c782394602a93921faff74e594d1bb1

                var that = this,
                    width = this.$("svg").width(),
                    height = this.$("svg").height(),
                    nodes = d3.values(data.nodes),
                    links = data.links;

                var activeEntityColor = "#00b98b",
                    deletedEntityColor = "#BB5838",
                    defaultEntityColor = "#e0e0e0",
                    selectedNodeColor = "#4a90e2";

                var svg = d3
                    .select(this.$("svg")[0])
                    .attr("viewBox", "0 0 " + width + " " + height)
                    .attr("enable-background", "new 0 0 " + width + " " + height),
                    node,
                    path;

                var container = svg
                    .append("g")
                    .attr("id", "container")
                    .attr("transform", "translate(0,0)scale(1,1)");

                var zoom = d3
                    .zoom()
                    .scaleExtent([0.1, 4])
                    .on("zoom", function() {
                        container.attr("transform", d3.event.transform);
                    });

                svg.call(zoom).on("dblclick.zoom", null);

                container
                    .append("svg:defs")
                    .selectAll("marker")
                    .data(["deletedLink", "activeLink"]) // Different link/path types can be defined here
                    .enter()
                    .append("svg:marker") // This section adds in the arrows
                    .attr("id", String)
                    .attr("viewBox", "-0 -5 10 10")
                    .attr("refX", 10)
                    .attr("refY", -0.5)
                    .attr("orient", "auto")
                    .attr("markerWidth", 6)
                    .attr("markerHeight", 6)
                    .append("svg:path")
                    .attr("d", "M 0,-5 L 10 ,0 L 0,5")
                    .attr("fill", function(d) {
                        return d == "deletedLink" ? deletedEntityColor : activeEntityColor;
                    })
                    .style("stroke", "none");

                var forceLink = d3
                    .forceLink()
                    .id(function(d) {
                        return d.id;
                    })
                    .distance(function(d) {
                        return 100;
                    })
                    .strength(1);

                var simulation = d3
                    .forceSimulation()
                    .force("link", forceLink)
                    .force("charge", d3.forceManyBody())
                    .force("center", d3.forceCenter(width / 2, height / 2));

                update();

                function update() {
                    path = container
                        .append("svg:g")
                        .selectAll("path")
                        .data(links)
                        .enter()
                        .append("svg:path")
                        .attr("class", "relatioship-link")
                        .attr("stroke", function(d) {
                            return getPathColor({ data: d, type: "path" });
                        })
                        .attr("marker-end", function(d) {
                            return "url(#" + (isAllEntityRelationDeleted({ data: d }) ? "deletedLink" : "activeLink") + ")";
                        });

                    node = container
                        .selectAll(".node")
                        .data(nodes)
                        .enter()
                        .append("g")
                        .attr("class", "node")
                        .on("mousedown", function() {
                            console.log(d3.event);
                            d3.event.preventDefault();
                        })
                        .on("click", function(d) {
                            if (d3.event.defaultPrevented) return; // ignore drag
                            if (d && d.value && d.value.guid == that.guid) {
                                that.ui.boxClose.trigger("click");
                                return;
                            }
                            that.toggleBoxPanel({ el: that.$(".relationship-node-details") });
                            that.ui.searchNode.data({ obj: d });
                            $(this)
                                .find("circle")
                                .addClass("node-detail-highlight");
                            that.updateRelationshipDetails({ obj: d });
                        })
                        .call(
                            d3
                            .drag()
                            .on("start", dragstarted)
                            .on("drag", dragged)
                        );

                    var circleContainer = node.append("g");

                    circleContainer
                        .append("circle")
                        .attr("cx", 0)
                        .attr("cy", 0)
                        .attr("r", function(d) {
                            d.radius = 25;
                            return d.radius;
                        })
                        .attr("fill", function(d) {
                            if (d && d.value && d.value.guid == that.guid) {
                                if (isAllEntityRelationDeleted({ data: d, type: "node" })) {
                                    return deletedEntityColor;
                                } else {
                                    return selectedNodeColor;
                                }
                            } else if (isAllEntityRelationDeleted({ data: d, type: "node" })) {
                                return deletedEntityColor;
                            } else {
                                return activeEntityColor;
                            }
                        })
                        .attr("typename", function(d) {
                            return d.name;
                        });

                    circleContainer
                        .append("text")
                        .attr("x", 0)
                        .attr("y", 0)
                        .attr("dy", 25 - 17)
                        .attr("text-anchor", "middle")
                        .style("font-family", "FontAwesome")
                        .style("font-size", function(d) {
                            return "25px";
                        })
                        .text(function(d) {
                            var iconObj = Enums.graphIcon[d.name];
                            if (iconObj && iconObj.textContent) {
                                return iconObj.textContent;
                            } else {
                                if (d && _.isArray(d.value) && d.value.length > 1) {
                                    return "\uf0c5";
                                } else {
                                    return "\uf016";
                                }
                            }
                        })
                        .attr("fill", function(d) {
                            return "#fff";
                        });

                    var countBox = circleContainer.append("g");

                    countBox
                        .append("circle")
                        .attr("cx", 18)
                        .attr("cy", -20)
                        .attr("r", function(d) {
                            if (_.isArray(d.value) && d.value.length > 1) {
                                return 10;
                            }
                        });

                    countBox
                        .append("text")
                        .attr("dx", 18)
                        .attr("dy", -16)
                        .attr("text-anchor", "middle")
                        .attr("fill", defaultEntityColor)
                        .text(function(d) {
                            if (_.isArray(d.value) && d.value.length > 1) {
                                return d.value.length;
                            }
                        });

                    node.append("text")
                        .attr("x", -15)
                        .attr("y", "35")
                        .text(function(d) {
                            return d.name;
                        });

                    simulation.nodes(nodes).on("tick", ticked);

                    simulation.force("link").links(links);
                }

                function ticked() {
                    path.attr("d", function(d) {
                        var diffX = d.target.x - d.source.x,
                            diffY = d.target.y - d.source.y,
                            // Length of path from center of source node to center of target node
                            pathLength = Math.sqrt(diffX * diffX + diffY * diffY),
                            // x and y distances from center to outside edge of target node
                            offsetX = (diffX * d.target.radius) / pathLength,
                            offsetY = (diffY * d.target.radius) / pathLength;

                        return "M" + d.source.x + "," + d.source.y + "A" + pathLength + "," + pathLength + " 0 0,1 " + (d.target.x - offsetX) + "," + (d.target.y - offsetY);
                    });

                    node.attr("transform", function(d) {
                        return "translate(" + d.x + "," + d.y + ")";
                    });
                }

                function dragstarted(d) {
                    d3.event.sourceEvent.stopPropagation();
                    if (d && d.value && d.value.guid != that.guid) {
                        if (!d3.event.active) simulation.alphaTarget(0.3).restart();
                        d.fx = d.x;
                        d.fy = d.y;
                    }
                }

                function dragged(d) {
                    if (d && d.value && d.value.guid != that.guid) {
                        d.fx = d3.event.x;
                        d.fy = d3.event.y;
                    }
                }

                function getPathColor(options) {
                    return isAllEntityRelationDeleted(options) ? deletedEntityColor : activeEntityColor;
                }

                function isAllEntityRelationDeleted(options) {
                    var data = options.data,
                        type = options.type;
                    var d = $.extend(true, {}, data);
                    if (d && !_.isArray(d.value)) {
                        d.value = [d.value];
                    }

                    return (
                        _.findIndex(d.value, function(val) {
                            if (type == "node") {
                                return (val.entityStatus || val.status) == "ACTIVE";
                            } else {
                                return val.relationshipStatus == "ACTIVE";
                            }
                        }) == -1
                    );
                }
                var zoomClick = function() {
                    var scaleFactor = 0.8;
                    if (this.id === 'zoom_in') {
                        scaleFactor = 1.3;
                    }
                    zoom.scaleBy(svg.transition().duration(750), scaleFactor);
                }

                d3.selectAll(this.$('.lineageZoomButton')).on('click', zoomClick);
            },
            createTable: function() {
                this.entityModel = new VEntity({});
                var table = CommonViewFunction.propertyTable({
                    scope: this,
                    valueObject: this.entity.relationshipAttributes,
                    attributeDefs: this.attributeDefs
                });
                this.ui.relationshipDetailValue.html(table);
                Utils.togglePropertyRelationshipTableEmptyValues({
                    inputType: this.ui.noValueToggle,
                    tableEl: this.ui.relationshipDetailValue
                });
            },
            relationshipViewToggle: function(checked) {
                this.ui.relationshipDetailTable.toggleClass("visible invisible");
                this.ui.relationshipSVG.toggleClass("visible invisible");

                if (checked) {
                    this.ui.zoomControl.hide();
                    this.$el.addClass("auto-height");
                } else {
                    this.ui.zoomControl.show();
                    this.$el.removeClass("auto-height");
                }
            }
        }
    );
    return RelationshipLayoutView;
});