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

package apitest_report

import (
	"encoding/base64"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

const (
	actionTypeAPITest = "api-test"
	version2          = "2.0"
)

type ApiReportMeta struct {
	ApiTotalNum      int `json:"apiTotalNum"`
	ApiSuccessNum    int `json:"apiSuccessNum"`
	ApiFailedNum     int `json:"apiFailedNum"`
	ApiNotExecNum    int `json:"apiNotExecNum"`
	ApiRefTotalNum   int `json:"apiRefTotalNum"`
	ApiRefSuccessNum int `json:"apiRefSuccessNum"`
	ApiRefFailedNum  int `json:"apiRefFailedNum"`
}

// +provider
type provider struct {
	aoptypes.PipelineBaseTunePoint
}

func (p *provider) Name() string { return "apitest-report" }

func (p *provider) Handle(ctx *aoptypes.TuneContext) error {
	// source = autotest
	if ctx.SDK.Pipeline.PipelineSource != apistructs.PipelineSourceAutoTest {
		return nil
	}
	var allTasks []*spec.PipelineTask
	// 尝试从上下文中获取，减少不必要的网络、数据库请求
	tasks, ok := ctx.TryGet(aoptypes.CtxKeyTasks)
	if ok {
		if _tasks, ok := tasks.([]*spec.PipelineTask); ok {
			allTasks = _tasks
		}
	} else {
		result, err := ctx.SDK.DBClient.GetPipelineWithTasks(ctx.SDK.Pipeline.ID)
		if err != nil {
			return err
		}
		allTasks = result.Tasks
	}
	// 过滤出 api_test task 以及 snippetTask
	var apiTestTasks []*spec.PipelineTask
	var snippetTaskPipelineIDs []uint64
	for _, task := range allTasks {
		if task.Type == apistructs.ActionTypeAPITest ||
			task.Type == apistructs.ActionTypeCustomScript {
			apiTestTasks = append(apiTestTasks, task)
			continue
		}

		// Config Sheet
		if task.Type == apistructs.ActionTypeSnippet &&
			task.Extra.Action.Labels[apistructs.AutotestType] == apistructs.AutotestSceneStep {
			b, err := base64.StdEncoding.DecodeString(task.Extra.Action.Labels[apistructs.AutotestSceneStep])
			if err != nil {
				logrus.Errorf("failed to DecodeString, err: %s", err.Error())
				continue
			}
			step := apistructs.AutoTestSceneStep{}
			if err = json.Unmarshal(b, &step); err != nil {
				logrus.Errorf("failed to Unmarshal, err: %s", err.Error())
				continue
			}
			if step.Type == apistructs.StepTypeConfigSheet {
				apiTestTasks = append(apiTestTasks, task)
				continue
			}
		}

		if task.Type == apistructs.ActionTypeSnippet {
			if task.SnippetPipelineID != nil &&
				task.Extra.CurrentPolicy.Type != apistructs.TryLatestSuccessResultPolicyType && task.Extra.CurrentPolicy.Type != apistructs.TryLatestResultPolicyType {
				snippetTaskPipelineIDs = append(snippetTaskPipelineIDs, *task.SnippetPipelineID)
			}
			continue
		}
	}

	apiTotalNum := 0
	apiSuccessNum := 0
	apiFailedNum := 0
	apiRefTotalNum := 0
	apiRefSuccessNum := 0
	apiRefFailedNum := 0

	// snippetTask 从对应的 snippetPipeline api-test 报告里获取接口执行情况
	snippetReports, err := ctx.SDK.DBClient.BatchListPipelineReportsByPipelineID(
		snippetTaskPipelineIDs,
		[]string{string(apistructs.PipelineReportTypeAPITest)},
	)
	if err != nil {
		return err
	}

	for _, apiTestTask := range apiTestTasks {
		// 总数
		apiTotalNum++
		if apiTestTask.Extra.Action.Labels[apistructs.LabelIsRefSet] == "true" {
			apiRefTotalNum++
		}
		// 执行成功
		if apiTestTask.Status.IsSuccessStatus() {
			apiSuccessNum++
			if apiTestTask.Extra.Action.Labels[apistructs.LabelIsRefSet] == "true" {
				apiRefSuccessNum++
			}
		}
		// 执行失败
		if apiTestTask.Status.IsFailedStatus() {
			apiFailedNum++
			if apiTestTask.Extra.Action.Labels[apistructs.LabelIsRefSet] == "true" {
				apiRefFailedNum++
			}
		}

	}
	for pipelineID, reports := range snippetReports {
		for _, report := range reports {
			b, err := json.Marshal(report.Meta)
			if err != nil {
				logrus.Warnf("failed to marshal api-test report, snippet pipelineID: %d, reportID: %d, err: %v",
					pipelineID, report.ID, err)
				continue
			}
			var meta ApiReportMeta
			if err := json.Unmarshal(b, &meta); err != nil {
				logrus.Warnf("failed to unmarshal api-test report to meta, snippet pipelineID: %d, reportID: %d, err: %v",
					pipelineID, report.ID, err)
				continue
			}
			// 总数
			apiTotalNum += meta.ApiTotalNum
			apiSuccessNum += meta.ApiSuccessNum
			apiFailedNum += meta.ApiFailedNum
			apiRefTotalNum += meta.ApiRefTotalNum
			apiRefSuccessNum += meta.ApiRefSuccessNum
			apiRefFailedNum += meta.ApiRefFailedNum
		}
	}

	// 处理 notExecNum
	apiNotExecNum := apiTotalNum - apiSuccessNum - apiFailedNum

	// 构造 reportMeta
	reportMeta := ApiReportMeta{
		ApiTotalNum:      apiTotalNum,
		ApiSuccessNum:    apiSuccessNum,
		ApiFailedNum:     apiFailedNum,
		ApiNotExecNum:    apiNotExecNum,
		ApiRefTotalNum:   apiRefTotalNum,
		ApiRefSuccessNum: apiRefSuccessNum,
		ApiRefFailedNum:  apiRefFailedNum,
	}
	var reqMeta map[string]interface{}
	b, _ := json.Marshal(reportMeta)
	_ = json.Unmarshal(b, &reqMeta)

	// result 信息
	_, err = ctx.SDK.Report.Create(apistructs.PipelineReportCreateRequest{
		PipelineID: ctx.SDK.Pipeline.ID,
		Type:       actionTypeAPITest,
		Meta:       reqMeta,
	})
	if err != nil {
		return err
	}
	return nil
}

func (p *provider) Init(ctx servicehub.Context) error {
	if err := aop.RegisterTunePoint(p); err != nil {
		return err
	}
	return nil
}

func init() {
	servicehub.Register(aop.NewProviderNameByPluginName(&provider{}), &servicehub.Spec{
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
