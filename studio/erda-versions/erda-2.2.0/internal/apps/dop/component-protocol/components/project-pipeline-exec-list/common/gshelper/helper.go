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

package gshelper

import (
	"strconv"
	"time"

	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}

	return mapstructure.Decode(src, dst)
}

func (h *GSHelper) SetStatuesFilter(statues []string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalStatuesFilter"] = statues
}

func (h *GSHelper) GetStatuesFilter() []string {
	if h.gs == nil {
		return nil
	}
	var t []string
	_ = assign((*h.gs)["GlobalStatuesFilter"], &t)
	return t
}

func (h *GSHelper) SetAppsFilter(apps []uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalAppsFilter"] = apps
}

func (h *GSHelper) GetAppsFilter() []uint64 {
	if h.gs == nil {
		return nil
	}
	var t []uint64
	_ = assign((*h.gs)["GlobalAppsFilter"], &t)
	return t
}

func (h *GSHelper) GetAppNamesFilter() []string {
	if h.gs == nil {
		return nil
	}
	if h.GetGlobalInParamsAppName() != "" {
		return []string{h.GetGlobalInParamsAppName()}
	}
	ids := h.GetAppsFilter()
	idNameMap := h.GetGlobalAppIDNameMap()
	appNames := make([]string, 0, len(ids))
	for _, id := range ids {
		if name, ok := idNameMap[strconv.FormatUint(id, 10)]; ok {
			appNames = append(appNames, name)
		}
	}
	return appNames
}

func (h *GSHelper) SetExecutorsFilter(executors []string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalExecutorsFilter"] = executors
}

func (h *GSHelper) GetExecutorsFilter() []string {
	if h.gs == nil {
		return nil
	}
	var t []string
	_ = assign((*h.gs)["GlobalExecutorsFilter"], &t)
	return t
}

func (h *GSHelper) SetPipelineNameFilter(name string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalPipelineNameFilter"] = name
}

func (h *GSHelper) GetPipelineNameFilter() string {
	if h.gs == nil {
		return ""
	}
	var t string
	_ = assign((*h.gs)["GlobalPipelineNameFilter"], &t)
	return t
}

func (h *GSHelper) SetBeginTimeStartFilter(unix int64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalBeginTimeStartFilter"] = unix
}

func (h *GSHelper) GetBeginTimeStartFilter() *time.Time {
	if h.gs == nil {
		return nil
	}
	var t int64
	_ = assign((*h.gs)["GlobalBeginTimeStartFilter"], &t)
	var startTime = time.Unix(t/1000, 0)
	return &startTime
}

func (h *GSHelper) SetBeginTimeEndFilter(unix int64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalBeginTimeEndFilter"] = unix
}

func (h *GSHelper) GetBeginTimeEndFilter() *time.Time {
	if h.gs == nil {
		return nil
	}
	var t int64
	_ = assign((*h.gs)["GlobalBeginTimeEndFilter"], &t)
	var endTime = time.Unix(t/1000, 0)
	return &endTime
}

func (h *GSHelper) SetGlobalAppIDNameMap(appIDNameMap map[string]string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalAppIDNameMap"] = appIDNameMap
}

func (h *GSHelper) GetGlobalAppIDNameMap() map[string]string {
	if h.gs == nil {
		return nil
	}
	appIDNameMap := make(map[string]string)
	_ = assign((*h.gs)["GlobalAppIDNameMap"], &appIDNameMap)
	return appIDNameMap
}

func (h *GSHelper) SetGlobalMyAppNames(appNames []string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalMyAppNames"] = appNames
}

func (h *GSHelper) GetGlobalMyAppNames() []string {
	if h.gs == nil {
		return nil
	}
	var appNames []string
	_ = assign((*h.gs)["GlobalMyAppNames"], &appNames)
	return appNames
}

func (h *GSHelper) SetGlobalInParamsAppName(appName string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalInParamsAppName"] = appName
}

func (h *GSHelper) GetGlobalInParamsAppName() string {
	if h.gs == nil {
		return ""
	}
	var appName string
	_ = assign((*h.gs)["GlobalInParamsAppName"], &appName)
	return appName
}
