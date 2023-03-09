/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metric;

import java.util.List;
import java.util.Map;

import org.apache.griffin.core.metric.model.Metric;
import org.apache.griffin.core.metric.model.MetricValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MetricController {

    @Autowired
    private MetricService metricService;

    @RequestMapping(value = "/metrics", method = RequestMethod.GET)
    public Map<String, List<Metric>> getAllMetrics() {
        return metricService.getAllMetrics();
    }

    @RequestMapping(value = "/metrics/values", method = RequestMethod.GET)
    public List<MetricValue> getMetricValues(@RequestParam("metricName")
                                                 String metricName,
                                             @RequestParam("size") int size,
                                             @RequestParam(value = "offset",
                                                 defaultValue = "0")
                                                 int offset,
                                             @RequestParam(value = "tmst",
                                                 defaultValue = "0")
                                                 long tmst) {
        return metricService.getMetricValues(metricName, offset, size, tmst);
    }

    @RequestMapping(value = "/metrics/values", method = RequestMethod.POST)
    public ResponseEntity<?> addMetricValues(@RequestBody List<MetricValue>
                                                 values) {
        return metricService.addMetricValues(values);
    }

    @RequestMapping(value = "/metrics/values", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> deleteMetricValues(@RequestParam("metricName")
                                                    String metricName) {
        return metricService.deleteMetricValues(metricName);
    }

    @RequestMapping(value = "/metrics/values/{instanceId}", method = RequestMethod.GET)
    public MetricValue getMetric(@PathVariable("instanceId") Long id) {
        return metricService.findMetric(id);
    }
}
