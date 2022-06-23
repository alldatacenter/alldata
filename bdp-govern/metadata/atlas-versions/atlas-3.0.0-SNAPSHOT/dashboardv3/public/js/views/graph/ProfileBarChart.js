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

define(["require", "d3", "d3-tip"], function(require, d3, d3Tip) {
    "use strict";
    var ProfileBarChart = {
        render: function(options) {
            var el = options.el,
                type = options.data.key,
                data = options.data.values,
                formatValue = options.formatValue,
                xAxisLabel = options.xAxisLabel,
                yAxisLabel = options.yAxisLabel,
                rotateXticks = options.rotateXticks,
                onBarClick = options.onBarClick,
                size = el.getBoundingClientRect(),
                svg = d3.select(el),
                margin = { top: 20, right: 30, bottom: 100, left: 80 },
                width = size.width - margin.left - margin.right,
                height = size.height - margin.top - margin.bottom;

            // set the ranges
            var x = d3.scaleBand().range([0, width]).padding(0.5);

            var y = d3.scaleLinear().range([height, 0]);

            var g = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            x.domain(
                data.map(function(d) {
                    return d.value;
                })
            );
            y.domain([
                0,
                d3.max(data, function(d) {
                    return d.count;
                })
            ]);

            // gridlines in x axis function
            function make_x_gridlines() {
                return d3.axisBottom(x).ticks(5);
            }

            // gridlines in y axis function
            function make_y_gridlines() {
                return d3.axisLeft(y).ticks(5);
            }

            // add the X gridlines
            g.append("g")
                .attr("class", "grid")
                .attr("transform", "translate(0," + height + ")")
                .call(make_x_gridlines().tickSize(-height).tickFormat(""));

            // add the Y gridlines
            g.append("g").attr("class", "grid").call(make_y_gridlines().tickSize(-width).tickFormat(""));

            // add the x Axis
            var xAxis = g
                .append("g")
                .attr("transform", "translate(0," + height + ")")
                .call(d3.axisBottom(x));

            if (rotateXticks) {
                xAxis.selectAll("text").style("text-anchor", "end").attr("dx", "-.8em").attr("dy", ".15em").attr("transform", "rotate(-45)");
            }

            // add the y Axis
            g.append("g").call(d3.axisLeft(y).ticks(3, "s"));

            g.append("text")
                .attr("transform", "translate(" + width / 2 + " ," + (margin.top - 25) + ")")
                .attr("class", "axislabel")
                .text(xAxisLabel);

            // text label for the y axis
            g.append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", 0 - margin.left)
                .attr("x", 0 - height / 2)
                .attr("dy", "1em")
                .attr("class", "axislabel")
                .text(yAxisLabel);

            var tooltip = d3Tip()
                .attr("class", "d3-tip")
                .offset([10, 0])
                .html(function(d) {
                    console.log(d)
                    return '<table><thead><tr><td colspan="3"><strong class="x-value">' + d.value + '</strong></td></tr></thead><tbody><tr><td class="key">' + type + '</td><td class="value">' + d.count + '</td></tr></tbody></table>';
                });

            // append the rectangles for the bar chart
            g.selectAll(".bar")
                .data(data)
                .enter()
                .append("rect")
                .attr("class", "profile-bar")
                .attr("x", function(d) {
                    return x(d.value);
                })
                .attr("width", x.bandwidth())
                .attr("y", function(d) { return height; })
                .attr("height", 0)
                .on("click", function(e) {
                    tooltip.hide();
                    if (onBarClick) {
                        onBarClick(e);
                    }
                })
                .on("mouseover", function(d) {
                    tooltip.show(d, this);
                })
                .on("mouseout", function(d) {
                    tooltip.hide();
                })
                .transition()
                .duration(450)
                .delay(function(d, i) {
                    return i * 50;
                })
                .attr("y", function(d) {
                    return y(d.count);
                })
                .attr("height", function(d) {
                    return height - y(d.count);
                });

            g.call(tooltip);

        }
    };
    return ProfileBarChart;
});