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
	"fmt"

	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
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

func (h *GSHelper) SetGlobalManualTestPlanList(l []apistructs.TestPlan) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalManualTestPlanList"] = l
}

func (h *GSHelper) GetGlobalManualTestPlanList() []apistructs.TestPlan {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlan, 0)
	_ = assign((*h.gs)["GlobalManualTestPlanList"], &res)
	return res
}

func (h *GSHelper) SetMtBlockFilterTestPlanList(l []apistructs.TestPlan) {
	if h.gs == nil {
		return
	}
	(*h.gs)["MtBlockFilterTestPlanList"] = l
}

func (h *GSHelper) GetMtBlockFilterTestPlanList() []apistructs.TestPlan {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlan, 0)
	_ = assign((*h.gs)["MtBlockFilterTestPlanList"], &res)
	return res
}

func (h *GSHelper) SetGlobalSelectedIterationIDs(ids []uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalSelectedIterationIDs"] = ids
}

func (h *GSHelper) GetGlobalSelectedIterationIDs() []uint64 {
	if h.gs == nil {
		return nil
	}
	res := make([]uint64, 0)
	_ = assign((*h.gs)["GlobalSelectedIterationIDs"], &res)
	return res
}

func (h *GSHelper) SetGlobalSelectedIterationsByID(l map[uint64]dao.Iteration) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalSelectedIterationsByID"] = l
}

func (h *GSHelper) GetGlobalSelectedIterationsByID() map[uint64]dao.Iteration {
	if h.gs == nil {
		return nil
	}
	res := make(map[uint64]dao.Iteration, 0)
	_ = assign((*h.gs)["GlobalSelectedIterationsByID"], &res)
	return res
}

func (h *GSHelper) SetMtPlanChartFilterTestPlanList(l []apistructs.TestPlan) {
	if h.gs == nil {
		return
	}
	(*h.gs)["MtPlanChartFilterTestPlanList"] = l
}

func (h *GSHelper) GetMtPlanChartFilterTestPlanList() []apistructs.TestPlan {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlan, 0)
	_ = assign((*h.gs)["MtPlanChartFilterTestPlanList"], &res)
	return res
}

func (h *GSHelper) SetMtPlanChartFilterStatusList(l []apistructs.TestCaseExecStatus) {
	if h.gs == nil {
		return
	}
	(*h.gs)["MtPlanChartFilterStatusList"] = l
}

func (h *GSHelper) GetMtPlanChartFilterStatusList() []apistructs.TestCaseExecStatus {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestCaseExecStatus, 0)
	_ = assign((*h.gs)["MtPlanChartFilterStatusList"], &res)
	return res
}

func (h *GSHelper) SetGlobalAutoTestPlanList(l []*apistructs.TestPlanV2) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalAutoTestPlanList"] = l
}

func (h *GSHelper) GetGlobalAutoTestPlanList() []apistructs.TestPlanV2 {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlanV2, 0)
	_ = assign((*h.gs)["GlobalAutoTestPlanList"], &res)
	return res
}

func (h *GSHelper) SetGlobalAutoTestPlanIDs(l []uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalAutoTestPlanIDs"] = l
}

func (h *GSHelper) GetGlobalAutoTestPlanIDs() []uint64 {
	if h.gs == nil {
		return nil
	}
	res := make([]uint64, 0)
	_ = assign((*h.gs)["GlobalAutoTestPlanIDs"], &res)
	return res
}

func (h *GSHelper) SetAtBlockFilterTestPlanList(l []apistructs.TestPlanV2) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalAtBlockFilterTestPlanList"] = l
}

func (h *GSHelper) GetAtBlockFilterTestPlanList() []apistructs.TestPlanV2 {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlanV2, 0)
	_ = assign((*h.gs)["GlobalAtBlockFilterTestPlanList"], &res)
	return res
}

func (h *GSHelper) SetRateTrendingFilterTestPlanList(l []apistructs.TestPlanV2) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalRateTrendingFilterTestPlanList"] = l
}

func (h *GSHelper) GetRateTrendingFilterTestPlanList() []apistructs.TestPlanV2 {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlanV2, 0)
	_ = assign((*h.gs)["GlobalRateTrendingFilterTestPlanList"], &res)
	return res
}

func (h *GSHelper) SetGlobalAtStep(l []apistructs.TestPlanV2Step) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalAtStep"] = l
}

func (h *GSHelper) GetGlobalAtStep() []apistructs.TestPlanV2Step {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlanV2Step, 0)
	_ = assign((*h.gs)["GlobalAtStep"], &res)
	return res
}

type AtSceneAndApiTimeFilter struct {
	TimeStart string `json:"timeStart"`
	TimeEnd   string `json:"timeEnd"`
}

func (h *GSHelper) SetAtSceneAndApiTimeFilter(t AtSceneAndApiTimeFilter) {
	if h.gs == nil {
		return
	}
	(*h.gs)["AtSceneAndApiTimeFilter"] = t
}

func (h *GSHelper) GetAtSceneAndApiTimeFilter() AtSceneAndApiTimeFilter {
	if h.gs == nil {
		return AtSceneAndApiTimeFilter{}
	}
	return (*h.gs)["AtSceneAndApiTimeFilter"].(AtSceneAndApiTimeFilter)
}

func (h *GSHelper) SetAtCaseRateTrendingTimeFilter(t AtSceneAndApiTimeFilter) {
	if h.gs == nil {
		return
	}
	(*h.gs)["AtCaseRateTrendingTimeFilter"] = t
}

func (h *GSHelper) GetAtCaseRateTrendingTimeFilter() AtSceneAndApiTimeFilter {
	if h.gs == nil {
		return AtSceneAndApiTimeFilter{}
	}
	return (*h.gs)["AtCaseRateTrendingTimeFilter"].(AtSceneAndApiTimeFilter)
}

type SelectChartItemData struct {
	PlanID      uint64 `json:"planID"`
	Name        string `json:"name"`
	PipelineID  uint64 `json:"pipelineID"`
	ExecuteTime string `json:"executeTime"`
}

func (h *GSHelper) SetSelectChartItemData(t map[string]SelectChartItemData) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalSelectChartItemData"] = t
}

func (h *GSHelper) GetSelectChartHistoryData() map[string]SelectChartItemData {
	if h.gs == nil {
		return nil
	}
	data := make(map[string]SelectChartItemData, 0)
	err := assign((*h.gs)["GlobalSelectChartItemData"], &data)
	if err != nil {
		fmt.Println(err)
	}
	return data
}

func (h *GSHelper) SetBlockAtStep(l []apistructs.TestPlanV2Step) {
	if h.gs == nil {
		return
	}
	(*h.gs)["BlockAtStep"] = l
}

func (h *GSHelper) GetBlockAtStep() []apistructs.TestPlanV2Step {
	if h.gs == nil {
		return nil
	}
	res := make([]apistructs.TestPlanV2Step, 0)
	_ = assign((*h.gs)["BlockAtStep"], &res)
	return res
}

func (h *GSHelper) SetBlockAtSceneStep(l []apistructs.AutoTestSceneStep) {
	if h.gs == nil {
		return
	}
	(*h.gs)["BlockAtSceneStep"] = l
}

func (h *GSHelper) SetGlobalQualityScore(score float64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalQualityScore"] = score
}

func (h *GSHelper) GetGlobalQualityScore() float64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["GlobalQualityScore"]
	if !ok {
		return 0
	}
	return v.(float64)
}

func (h *GSHelper) SetWaterfallChartPipelineID(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalWaterfallChartPipelineIDs"] = id
}

func (h *GSHelper) GetWaterfallChartPipelineID() uint64 {
	if h.gs == nil {
		return 0
	}
	var data uint64
	_ = assign((*h.gs)["GlobalWaterfallChartPipelineIDs"], &data)
	return data
}
