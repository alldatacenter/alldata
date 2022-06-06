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

App.ChartView = Em.View.extend({
  dateFormat:'dd/mm/yy',
  timeFormat:'h:m',
  w:900,
  p:30, // axis padding
  shift:30,
  ticksCount:10,
  pointsLimit:300,
  areaHeight:30, // px
  axis:false,
  x:false,
  y:false,

  init:function () {
    this._super();
    var renderer = this;

    this.x = d3.time.scale().domain([renderer.getMinDate({}), renderer.getMaxDate({})]).range([0, this.get('w')]);
    this.y = d3.scale.linear().domain([0, 50]).range([this.get('h'), 0]);
    this.axis = d3.svg.axis().orient("top").scale(this.x).ticks(this.get('ticksCount'));
  },

  h: function(){
    return this.get('p') + this.get('nodeAttributes').length * this.get('areaHeight'); //default: 160
  }.property('nodeAttributes', 'p'),

  activeH: function(){
    return this.get('nodeAttributes').length * this.get('areaHeight'); // default 160;
  }.property('h'),

  ruleHeight: function(){
    return this.get('nodeAttributes').length * this.get('areaHeight');
  }.property('nodeAttributes'),

  updateY: function(){
    this.y = d3.scale.linear().domain([0, 50]).range([this.get('h'), 0]);
  }.observes('h'),

  getMinDate:function (data) {
    if (data.length)
      return new Date(Date.parse(data[0]['date']));

    return new Date();
  },

  getMaxDate:function (data) {
    if (data.length)
      return new Date(Date.parse(data[data.length - 1]['date']));

    return new Date();
  },

  area:function () {
    var renderer = this;
    var area = d3.svg.area().x(function (d) {
      return renderer.x(renderer.getDate(d));
    });

    area.y1(function (d) {
      return renderer.get('h') - (renderer.get('h') - renderer.y(d[$(this).attr("getter")])) / renderer.get('koef');
    });

    area.y0(function (d) {
      return renderer.get('h');
    });

    return area;
  },

  line:function () {
    var renderer = this;
    var area = d3.svg.line().x(function (d) {
      return renderer.x(renderer.getDate(d));
    })
      .interpolate("basis");

    area.y(function (d) {
      return renderer.get('h');
    });

    return area;
  },

  /**
   * @todo: calculate this
   * coefficient of compression
   * @param shift
   * @return {Number}
   */

  koef:function () {
    // max value divide on area height;
    return 2 * (this.get('nodeAttributes').length + 1);
  }.property('h'),

  getDate:function (d) {
    return new Date(Date.parse(d.date));
  },

  dateTimeToDateObject:function (string) {
    var ren = this;
    return new Date($.datepicker.parseDateTime(ren.dateFormat, ren.timeFormat, string));
  },

  getDefaultShift:function () {
    return  -1 * this.get('areaHeight') * (this.get('nodeAttributes').length - 1);
  },

  percentScaleXDefaultTranslate:function () {
    return this.w + 3
  },

  clearPlot: function(){
    d3.select(this.get('chartContainerSelector')).selectAll("*").remove();
  },

  drawPlot:function () {
    this.clearPlot();

    var renderer = this;
    this.x.domain([renderer.getMinDate({}), renderer.getMaxDate({})]);

    var rule = $('<div></div>').addClass("rule").css('height', renderer.get('ruleHeight')).mouseenter(function () { $(this).hide(); });
    $(this.get('chartContainerSelector')).prepend(rule);

    var vis = d3.select(this.get('chartContainerSelector'))
      .append("svg:svg")
      .attr("width", renderer.get('w') + 5)
      .attr("height", renderer.get('h'))
      .attr("rendererId", this.get('elementId'))
      .on("mousemove", function () {

        var area = d3.select(this).select("path.line");
        var d = area.data()[0];
        var x = d3.mouse(this)[0];

        var renderer = Em.View.views[d3.select(this).attr('rendererId')];
        var container = $(this).parent();
        var scale = renderer.x;

        // first move rule
        var rule = $(container).children("div.rule");
        rule.css("left", (168 + x) + "px"); // 168 - left container margin
        rule.show();

        x = x + 5; // some correction
        var selectedDate = scale.invert(x);

        // search date between this coordinates
        var prevVal = false;
        var nextVal = d[0];

        $.each(d, function (i, point) {
          if (renderer.getDate(point).getTime() <= selectedDate.getTime()) {
            prevVal = nextVal;
            nextVal = point;
          }
        });

        var len1 = Math.abs(x - scale(renderer.getDate(prevVal)));
        var len2 = Math.abs(x - scale(renderer.getDate(nextVal)));

        var clearing = 5;
        var pointToShow = false;
        // if the first point if closer
        if ((len1 < len2) && (len1 <= clearing)) {
          pointToShow = prevVal;
        } else if (len2 <= clearing) { // the second point is closer
          pointToShow = nextVal;
        }

        $.each(renderer.get('nodeAttributes'), function (i, v) {
          var value = !pointToShow ? "" : pointToShow[v] + "%";
          $(rule).children("div." + v).html(value);
        });
      });

    vis.append("svg:g")
      .attr("class", "axis")
      .attr("transform", "translate(0," + this.get('p') + ")")
      .call(renderer.axis);

    $.each(this.get('nodeAttributes'), function (i, v) {
      var element = $('<div></div>').addClass(v).addClass("stateValue").html("");
      rule.append(element);
    });

    var shift = this.getDefaultShift();
    vis.append("svg:path")
      .attr("class", "horizontal-line")
      .data([
      {}
    ])
      .attr("transform", "translate(0," + (shift - this.get('areaHeight')) + ")")
      .attr("d", renderer.line())
      .style("stroke", "#000");

    $.each(this.get('nodeAttributes'), function (i, v) {
      vis.append("svg:path").data([
        {}
      ])
        .attr("class", "line")
        .attr("getter", v)
        .attr("transform", "translate(0, " + shift + ")")
        .attr("d", renderer.area())
        .style("fill", function () {
          return "#31a354";
        });

      vis.append("svg:path")
        .attr("class", "horizontal-line")
        .data([
        {}
      ])
        .attr("transform", "translate(0," + shift + ")")
        .attr("d", renderer.line())
        .style("stroke", "#000");

      shift += renderer.get('areaHeight');
    });
  },

  getData:function (containerId) {
    return (d3.select(containerId + " path.line").data())[0];
  },

  drawChart:function () {
    var containerSel = this.get('chartContainerSelector');
    var data = this.get('data');

    while (data.length > this.get('pointsLimit')) {
      data.shift();
    }

    var renderer = this;
    var minDate = this.getMinDate(data);
    var maxDate = this.getMaxDate(data);

    this.x.domain([minDate, maxDate]);

    var ticks = data.length > 10 ? 10 : data.length;
    this.axis.scale(renderer.x).ticks(ticks);

//        remove dots axis
    $(containerSel + " svg g.axis g").remove();
    d3.select(containerSel + " svg g.axis")
      .call(this.axis);

    $.each(this.get('nodeAttributes'), function (i, v) {
      d3.select(containerSel + " path.line[getter='" + v + "']")
        .data([data])
        .transition()
        .attr("d", renderer.area());
    });

    // lines between charts
    $(containerSel + " path.horizontal-line").each(
      function (i, path) {
        d3.select(path).data([
          [
            {date:minDate},
            {date:maxDate}
          ]
        ]).attr("d", renderer.line());
      }
    );
  }
});