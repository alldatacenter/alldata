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

var App = require('app');

App.ChartLinearView = Em.View.extend({
  w:90,
  h:90,
  margin:2,

  classNames: ['linear'],

  init:function () {
    this._super();

    this.x = d3.scale.linear().domain([0, this.get('data').length]).range([0 + this.get('margin'), this.get('w') - this.get('margin')]);
    this.y = d3.scale.linear().domain([0, d3.max(this.get('data'))]).range([0 + this.get('margin'), this.get('h') - this.get('margin')]);
    this.line = d3.svg.line().x(function (d, i) { return this.x(i); }).y(function (d) {return -1 * this.y(d); })
  },

  didInsertElement:function () {
    this._super();
    this.appendSvg();
  },

  selector: Em.computed.format('#{0}', 'elementId'),

  appendSvg:function () {
    var thisChart = this;

    this.set('svg', d3.select(this.get('selector'))
      .append("svg:svg")
      .attr("width", thisChart.get('w'))
      .attr("height", thisChart.get('h')));

    this.set('g', thisChart.get('svg').append("svg:g").attr("transform", "translate(0, " + thisChart.get('h') + ")"));
    this.get('g').append("svg:path").attr("d", thisChart.line(thisChart.get('data')));


    // axis
    this.get('g').append("svg:line")
      .attr("x1", thisChart.x(0))
      .attr("y1", -1 * thisChart.y(0))
      .attr("x2", thisChart.x(this.get('data').length))
      .attr("y2", -1 * thisChart.y(0));

    this.get('g').append("svg:line")
      .attr("x1", thisChart.x(0))
      .attr("y1", -1 * thisChart.y(0))
      .attr("x2", thisChart.x(0))
      .attr("y2", -1 * thisChart.y(d3.max(thisChart.get('data'))))
  }
});