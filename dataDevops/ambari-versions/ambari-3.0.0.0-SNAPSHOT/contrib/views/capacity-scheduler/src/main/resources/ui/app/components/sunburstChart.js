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
 * Unless required by applicable law or agreed to in writing, software: [],

 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

function _getSunburstChartDataForDefault(queue, json, allQueues) {
  var childrenQs = allQueues.filterBy('parentPath', queue.get('path')),
    qdata = {
      "name": queue.get('name'),
      "path": queue.get('path'),
      "capacity": queue.get('capacity'),
      "absoluteCapacity": queue.get('absolute_capacity')||0,
      "size": queue.get('capacity'),
      "isLabel": false
    };

  if (!Ember.isEmpty(childrenQs)) {
    qdata["children"] = [];
  }

  if (json["children"]) {
    json["children"].push(qdata);
  }

  childrenQs.forEach(function(child) {
    return _getSunburstChartDataForDefault(child, qdata, allQueues);
  });
  return json;
}

function _getSunburstChartDataForLabel(queue, json, allQueues, labelName) {
  var childrenQs = allQueues.filterBy('parentPath', queue.get('path')),
    qLabel = queue.get('labels').findBy('name', labelName),
    qdata = {
      "name": queue.get('name'),
      "path": queue.get('path'),
      "capacity": qLabel?qLabel.get('capacity') : 0,
      "absoluteCapacity": qLabel? qLabel.get('absolute_capacity') : 0,
      "size": qLabel? qLabel.get('capacity') : 0,
      "isLabel": true
    };

  if (!Ember.isEmpty(childrenQs)) {
    qdata["children"] = [];
  }

  if (json["children"]) {
    json["children"].push(qdata);
  }

  childrenQs.forEach(function(child) {
    return _getSunburstChartDataForLabel(child, qdata, allQueues, labelName);
  });
  return json;
}

App.SunburstChartComponent = Ember.Component.extend({
  layoutName: 'components/sunburstChart',
  queues: null,
  selectedQueue: null,
  allNodeLabels: null,

  width: 0,
  height: 0,
  radius: 0,
  data: null,
  colorScale: null,
  chartRendered: false,
  queueCapacityTypeSelected: null,

  queueCapacityTypeValues: null,

  didInsertElement: function() {
    this.initializeChart();
    this.initializeCapacityTypeOptions();
    this.set("queueCapacityTypeSelected", null);
    this.setChartDataForDefault();
    this.renderChart();
  },

  initializeChart: function() {
    var width = this.$().width();
    this.$('.panel-default').height(width);
    var height = this.$('.panel-default').height() - this.$('#top_container').outerHeight();

    var vizWidth = width - 60;
    var vizHeight = height - 80;
    var radius = Math.min(vizWidth, vizHeight) / 2;

    this.set('width', vizWidth);
    this.set('height', vizHeight);
    this.set('radius', radius);
    this.set('colorScale', d3.scale.category20());
  },

  initializeCapacityTypeOptions: function() {
    var options = [{
      name: "Default",
      value: null
    }];
    if (this.get('allNodeLabels') !== null) {
      this.get('allNodeLabels').forEach(function(label) {
        options.push({
          name: "Label: " + label.name,
          value: label.name
        });
      });
    }
    this.set("queueCapacityTypeValues", options);
  },

  setChartDataForDefault: function() {
    var queues = this.get('queues'),
      rootQ = queues.findBy('name', 'root'),
      dataJson = {
        "name": "sunburst",
        "children": []
      };

    var data = _getSunburstChartDataForDefault(rootQ, dataJson, queues);
    this.set("data", data);
  },

  setChartDataForLabel: function() {
    var queues = this.get('queues'),
      allNodeLabels = this.get('allNodeLabels'),
      rootQ = queues.findBy('name', 'root'),
      labelName = this.get('queueCapacityTypeSelected');

    var dataJson = {
      "name": "sunburst",
      "children": []
    };
    var data = _getSunburstChartDataForLabel(rootQ, dataJson, queues, labelName);
    this.set("data", data);
  },

  renderChart: function() {
    var self = this,
      width = this.get('width'),
      height = this.get('height'),
      radius = this.get('radius'),
      data = this.get('data'),
      colors = this.get('colorScale');

    var vis = d3.select("#sunburst_chart").append("svg:svg")
      .attr("width", width)
      .attr("height", height)
      .append("svg:g")
      .attr("id", "sunburst_container")
      .attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

    var partition = d3.layout.partition()
      .size([2 * Math.PI, radius * radius])
      .value(function(d) {
        return d.size;
      });

    var arc = d3.svg.arc()
      .startAngle(function(d) { return d.x; })
      .endAngle(function(d) { return d.x + d.dx; })
      .innerRadius(function(d) { return Math.sqrt(d.y); })
      .outerRadius(function(d) { return Math.sqrt(d.y + d.dy); });

    vis.append("svg:circle")
      .attr("r", radius)
      .style("opacity", 0);

    var nodes = partition.nodes(data)
      .filter(function(d) {
         return (d.dx > 0.005);
      });

    var path = vis.data([data]).selectAll("path")
      .data(nodes)
      .enter().append("svg:path")
      .attr("id", function(d) { return d.path; })
      .attr("data-node", function(d) {
        var node = {
          name: d.name,
          path: d.path,
          capacity: d.capacity,
          absoluteCapacity: d.absoluteCapacity,
          isLabel: d.isLabel
        };
        return JSON.stringify(node);
      })
      .attr("display", function(d) { return d.depth ? null : "none"; })
      .attr("d", arc)
      .attr("fill-rule", "evenodd")
      .style("fill", function(d) {
        var colr = colors(d.name);
        d.color = colr;
        return colr;
      })
      .style("opacity", 1);

    this.set('chartRendered', true);
    this.adjustPositions();
  },

  mouseover: function(d, elem) {
    this.showQueueInfo(d);
    d3.selectAll("path").style("opacity", 0.3);
    d3.select(elem).style("opacity", 1);
  },

  showQueueInfo: function(node) {
    var percentageString = node.absoluteCapacity + "%";
    d3.select("#capacityPercentage").text(percentageString);
    d3.select("#queuePath").text(node.path);
    d3.select("#type_text").text("Abs Cap:");
    d3.select("#explanation").style("visibility", "");
    d3.select("#queue_info").style("visibility", "");
  },

  hideQueueInfo: function() {
    d3.select("#queuePath").text("");
    d3.select("#type_text").text("");
    d3.select("#capacityPercentage").text("");
    d3.select("#explanation").style("visibility", "hidden");
    d3.select("#queue_info").style("visibility", "hidden");
  },

  mouseleave: function(d) {
    d3.selectAll("path").style("opacity", 1);
    d3.select("#explanation").style("visibility", "hidden");
    d3.select("#queue_info").style("visibility", "hidden");
    var queue = this.get("selectedQueue");
    if (queue) {
      var node = this.$("path[id='"+queue.get('path')+"']").data('node');
      this.showQueueInfo({
        name: node.name,
        path: node.path,
        capacity: node.capacity,
        absoluteCapacity: node.absoluteCapacity,
        isLabel: node.isLabel
      });
    }
  },

  hightlightSelectedQueue: function() {
    this.removeHighlightFromChart();
    var width = this.get('width'),
    height = this.get('height'),
    queue = this.get("selectedQueue");
    if (queue) {
      d3.selectAll("path").style("opacity", 0.3);
      d3.select("path[id='"+queue.get('path')+"']")
        .style("fill", "#3276b1")
        .style("opacity", 1);
      var node = this.$("path[id='"+queue.get('path')+"']").data('node');
      if (node) {
        this.showQueueInfo({
          name: node.name,
          path: node.path,
          capacity: node.capacity,
          absoluteCapacity: node.absoluteCapacity,
          isLabel: node.isLabel
        });
      }
    }
  },

  removeHighlightFromChart: function() {
    d3.selectAll("path").style("opacity", 1);
    d3.selectAll("path").each(function(node) {
      if (node.path) {
        d3.select("path[id='"+node.path+"']")
          .style("fill", node.color);
      }
    });
    this.hideQueueInfo();
  },

  adjustPositions: function() {
    var rootOffset = this.$("path[id='root']").offset(),
    rootPathDims = this.$("path[id='root']")[0].getBoundingClientRect(),
    rootW = rootPathDims.width,
    rootH = rootPathDims.height,
    explanationH = this.$("#explanation").height(),
    explanationW = this.$("#explanation").width();

    var rootCenterX = rootOffset.left + rootW / 2,
    rootCenterY = rootOffset.top + rootH / 2,
    posX = rootCenterX - explanationW / 2,
    posY = rootCenterY - explanationH / 2;

    this.$("#explanation").offset({left: posX, top: posY});
  },

  renderObserver: function() {
    d3.select("#sunburst_chart svg").remove();
    this.set('chartRendered', false);
    this.set("queueCapacityTypeSelected", null);
    this.setChartDataForDefault();
    this.renderChart();
    this.hightlightSelectedQueue();
  }.observes('queues', 'queues.[]', 'queues.@each.capacity'),

  queueSelectionObserver: function() {
    if (this.get('selectedQueue') && this.get('chartRendered')) {
      this.hightlightSelectedQueue();
    } else {
      this.removeHighlightFromChart();
    }
  }.observes('selectedQueue', 'chartRendered'),

  watchTypeSelection: function() {
    d3.select("#sunburst_chart svg").remove();
    this.set('chartRendered', false);
    if (this.get("queueCapacityTypeSelected") !== null) {
      this.setChartDataForLabel();
    } else {
      this.setChartDataForDefault();
    }
    this.renderChart();
    this.hightlightSelectedQueue();
  }.observes("queueCapacityTypeSelected")
});
