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

package org.apache.griffin.core.measure;

import java.util.List;
import javax.validation.Valid;

import org.apache.griffin.core.measure.entity.Measure;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1")
public class MeasureController {
    @Autowired
    private MeasureService measureService;

    @RequestMapping(value = "/measures", method = RequestMethod.GET)
    public List<? extends Measure> getAllAliveMeasures(@RequestParam(value =
        "type", defaultValue = "") String type) {
        return measureService.getAllAliveMeasures(type);
    }

    @RequestMapping(value = "/measures/{id}", method = RequestMethod.GET)
    public Measure getMeasureById(@PathVariable("id") long id) {
        return measureService.getMeasureById(id);
    }

    @RequestMapping(value = "/measures/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasureById(@PathVariable("id") Long id) throws
        SchedulerException {
        measureService.deleteMeasureById(id);
    }

    @RequestMapping(value = "/measures", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeasures() throws SchedulerException {
        measureService.deleteMeasures();
    }

    @RequestMapping(value = "/measures", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public Measure updateMeasure(@RequestBody Measure measure) {
        return measureService.updateMeasure(measure);
    }

    @RequestMapping(value = "/measures/owner/{owner}", method =
        RequestMethod.GET)
    public List<Measure> getAliveMeasuresByOwner(@PathVariable("owner")
                                                 @Valid String owner) {
        return measureService.getAliveMeasuresByOwner(owner);
    }

    @RequestMapping(value = "/measures", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Measure createMeasure(@RequestBody Measure measure) {
        return measureService.createMeasure(measure);
    }
}
