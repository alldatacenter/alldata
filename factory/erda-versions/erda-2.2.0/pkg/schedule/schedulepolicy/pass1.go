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
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelconfig"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/labelpipeline"
)

// request
// -----> Pass1ScheduleInfo(aka LabelInfo)
// -----> Pass2ScheduleInfo(apistructs.ScheduleInfo)
type Pass1ScheduleInfo labelconfig.LabelInfo

func NewPass1ScheduleInfo(executorName string, executorKind string, labels map[string]string,
	configs *executorconfig.ExecutorWholeConfigs, objName string,
	selectors map[string]diceyml.Selectors) Pass1ScheduleInfo {
	return Pass1ScheduleInfo{
		Label:          labels,
		ExecutorName:   executorName,
		ExecutorKind:   executorKind,
		ExecutorConfig: configs,
		OptionsPlus:    configs.PlusConfigs,
		ObjName:        objName,
		Selectors:      selectors,
	}
}

func (p *Pass1ScheduleInfo) validate() error {
	return nil
}

func (p *Pass1ScheduleInfo) toNextPass() (Pass2ScheduleInfo, Pass2ScheduleInfo2) {
	result := labelconfig.RawLabelRuleResult{}
	result2 := labelconfig.RawLabelRuleResult2{}
	for _, f := range []labelconfig.LabelPipelineFunc{
		labelpipeline.OrgLabelFilter,
		labelpipeline.WorkspaceLabelFilter,
		labelpipeline.IdentityFilter,
		labelpipeline.HostUniqueLabelFilter,
		labelpipeline.SpecificHostLabelFilter,
		labelpipeline.LocationLabelFilter,
	} {
		labelinfo := labelconfig.LabelInfo(*p)
		f(&result, &result2, &labelinfo)
	}
	return Pass2ScheduleInfo(result), Pass2ScheduleInfo2(result2)
}
