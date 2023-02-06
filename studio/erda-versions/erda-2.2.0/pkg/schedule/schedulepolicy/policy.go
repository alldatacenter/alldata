// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package schedulepolicy

import (
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	NOT_RECOGNIZED_RUNTIME_TYPE = errors.Errorf("task kind is runtime but spec not fit")
	NOT_RECOGNIZED_JOB_TYPE     = errors.Errorf("task kind is job but spec not fit")
)

//
//+-------------------------------------------------------------------------------------------+
//|                                                                                           |
//|                                 ServiceGroup / Job                                             |
//|                                                                                           |
//+------------------------------------------------------^^-----------------------------------+
//                                  ||                   ||
//                                  ||                   ||
//+---------------------------------vv--------------------------------------------------------+
//|                                    label scheduling                                       |
//|                                                                                           |
//|    +-------------------+  +------------+      +---------------+    +---------------+      |
//|    |2                  |  |1           |      |7              |    |               |      |
//|    |  org label filter <--+ prestart   |      | DC/OS CLUSTER |    |    Others     |      |
//|    |                   <--+ filter     |      |               |    |               |      |
//|    +-------------------+  +------------+      +---------^-----+    +---------^-----+      |
//|             ||                                          |                    |            |
//|             ||                                          |                    |            |
//|    +--------vv---------+  +------------+            +---+--------------------+------+     |
//|    |3                  |  |5           |            |6                              |     |
//|    |  workspace label  |  | poststop   +------------>  LabelConstrainsExitLayer     |     |
//|    |  filter           |  | filter     +------------>                               |     |
//|    |                   |  +----^^------+            |                               |     |
//|    +-------------------+       ||                   +-------------------------------+     |
//|             ||                 ||                                                         |
//|             ||                 ||                                                         |
//|             ||                 ||                                                         |
//|    +--------vv-----------------------------------------------------------------------+    |
//|    |4                          identity label filter                                 |    |
//|    |   +---------------+    +--------------+    +-------------+    +--------------+  |    |
//|    |   |  job label    |    |  pack label  |    |  stateful   |    |  stateless   |  |    |
//|    |   |               +---->              +---->  label      +---->  label       |  |    |
//|    |   +---------------+    +--------------+    +-------------+    +------+-------+  |    |
//|    |                                                                      |          |    |
//|    |                                            +-------------+    +------v-------+  |    |
//|    |                                            |   ... ...   |    |  bigdata     |  |    |
//|    |                                            |             +<---+  label       |  |    |
//|    |                                            +-------------+    +--------------+  |    |
//|    +---------------------------------------------------------------------------------+    |
//|                                                                                           |
//+-------------------------------------------------------------------------------------------+
//
// Convert label preferences into scheduling constraints that can be recognized by different clusters
// The information that needs to be obtained includes:
// 1, Cluster configuration information, including basic configuration and fine configuration
// 2, runtime or job specific label
// The first parameter returned is the specific constraint, and the second parameter is the configuration obtained by the application in the cluster fine configuration (if any)
func LabelFilterChain(configs *executorconfig.ExecutorWholeConfigs, name, kind string, obj interface{}) (apistructs.ScheduleInfo2, apistructs.ScheduleInfo, interface{}, error) {
	defaultScheduleInfo2 := apistructs.ScheduleInfo2{IsUnLocked: true}
	defaultScheduleInfo := apistructs.ScheduleInfo{IsUnLocked: true}
	// Label scheduling has not been opened
	if !configs.EnableLabelSchedule() {
		return defaultScheduleInfo2, defaultScheduleInfo, nil, nil
	}
	var (
		objLabels      = make(map[string]string)
		objName        string
		refinedConfigs interface{}

		//  serviceSelectors         map[servicename]selectors
		serviceSelectors map[string]diceyml.Selectors
	)
	switch kind {
	case labelconfig.EXECUTOR_CHRONOS, labelconfig.EXECUTOR_EDAS, labelconfig.EXECUTOR_FLINK:
		return defaultScheduleInfo2, defaultScheduleInfo, nil, nil
	case labelconfig.EXECUTOR_MARATHON, labelconfig.EXECUTOR_K8S, labelconfig.EXECUTOR_EDASV2:
		r, ok := obj.(apistructs.ServiceGroup)
		if !ok {
			return defaultScheduleInfo2, defaultScheduleInfo, nil, NOT_RECOGNIZED_RUNTIME_TYPE
		}
		objLabels = r.Labels
		objName = r.ID
		serviceSelectors = collectServiceSelectors(&r)
		// Configurations that require differentiated coverage
		if configs.PlusConfigs != nil && len(configs.PlusConfigs.Orgs) > 0 {
			//e.g. Set different cpu overselling ratios under different orgs and/or different envs
			setRuntimeRefinedConfig(&r, serviceSelectors, configs.PlusConfigs)
			if len(r.Extra) > 0 {
				refinedConfigs = r.Extra
			}
		}
	case labelconfig.EXECUTOR_METRONOME, labelconfig.EXECUTOR_SPARK, labelconfig.EXECUTOR_K8SJOB,
		labelconfig.EXECUTOR_K8SSPARK, labelconfig.EXECUTOR_K8SFLINK:
		j, ok := obj.(apistructs.Job)
		if !ok {
			return defaultScheduleInfo2, defaultScheduleInfo, nil, NOT_RECOGNIZED_JOB_TYPE
		}
		objLabels = j.Labels
		objName = j.Name
	default:
		return defaultScheduleInfo2, defaultScheduleInfo, nil, errors.Errorf("executor(%s)'s kind(%s) not recognized in LabelFilterChain", name, kind)
	}

	pass1scheduleInfo := NewPass1ScheduleInfo(
		name, kind, objLabels, configs, objName, serviceSelectors)
	if err := pass1scheduleInfo.validate(); err != nil {
		return defaultScheduleInfo2, defaultScheduleInfo, nil, err
	}
	logrus.Infof("pass1scheduleInfo: %+v", pass1scheduleInfo)
	pass2scheduleInfo, pass2scheduleInfo2 := pass1scheduleInfo.toNextPass()
	logrus.Infof("pass2scheduleInfo: %+v", pass2scheduleInfo)
	logrus.Infof("pass2scheduleInfo2: %+v", pass2scheduleInfo2)

	return apistructs.ScheduleInfo2(pass2scheduleInfo2), apistructs.ScheduleInfo(pass2scheduleInfo),
		refinedConfigs, nil
}

func setRuntimeRefinedConfig(r *apistructs.ServiceGroup, svcSelectors map[string]diceyml.Selectors, plus *executorconfig.OptPlus) {
	if len(r.Labels) == 0 || plus == nil {
		return
	}
	org := r.Labels[labelconfig.ORG_KEY]
	for _, selectors := range svcSelectors {
		if v, ok := selectors["org"]; ok && len(v.Values) > 0 {
			org = v.Values[0]
			break
		}
	}
	if org == "" {
		return
	}
	idx := -1
	for i, pOrg := range plus.Orgs {
		if pOrg.Name == org {
			idx = i
			break
		}
	}
	// runtime The org identified in is not found in the cluster fine configuration
	if idx < 0 {
		return
	}

	if r.Extra == nil {
		r.Extra = make(map[string]string)
	}

	for k, v := range plus.Orgs[idx].Options {
		r.Extra[k] = v
	}

	workspace := r.Labels[labelconfig.WORKSPACE_KEY]
	for _, selectors := range svcSelectors {
		if v, ok := selectors["workspace"]; ok && len(v.Values) > 0 {
			workspace = v.Values[0]
			break
		}
	}
	if workspace == "" {
		return
	}

	for _, pWorkspace := range plus.Orgs[idx].Workspaces {
		if pWorkspace.Name == workspace {
			for k, v := range pWorkspace.Options {
				r.Extra[k] = v
			}
			break
		}
	}
}

// collectServiceSelectors extract `selectors` from apistructs.ServiceGroup
// @return map[servicename]selectors
func collectServiceSelectors(sg *apistructs.ServiceGroup) map[string]diceyml.Selectors {
	r := make(map[string]diceyml.Selectors, len(sg.Services))
	for i := range sg.Services {
		if sg.Services[i].Selectors == nil {
			continue
		}
		r[sg.Services[i].Name] = sg.Services[i].Selectors.(diceyml.Selectors)
	}

	// LOCATION-xxxx labels
	for k := range sg.Labels {
		if !strutil.HasPrefixes(k, labelconfig.LOCATION_PREFIX) {
			continue
		}
		locationvalue := strutil.TrimPrefixes(k, labelconfig.LOCATION_PREFIX)
		for i := range sg.Services {
			r[sg.Services[i].Name] = diceyml.Selectors{"location": diceyml.Selector{
				Values: []string{strutil.ToLower(locationvalue)}}}
		}
	}

	return r
}
