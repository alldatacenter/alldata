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

package executeTaskTable

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/pkg/component_key"
)

type ExecuteTaskTable struct {
	bdl        *bundle.Bundle
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Data       map[string]interface{} `json:"data"`

	pipelineID uint64
	sdk        *cptype.SDK
}

type State struct {
	Total    int64  `json:"total"`
	PageSize int64  `json:"pageSize"`
	PageNo   int64  `json:"pageNo"`
	StepID   uint64 `json:"stepId"`
	Name     string `json:"name"`
	Unfold   bool   `json:"unfold"`
}

type operationData struct {
	Meta meta `json:"meta"`
}

type meta struct {
	Target RowData `json:"target"`
}

type Operation struct {
	Key           string      `json:"key"`
	Reload        bool        `json:"reload"`
	FillMeta      string      `json:"fillMeta"`
	Meta          interface{} `json:"meta"`
	ClickableKeys interface{} `json:"clickableKeys"`
}

type props struct {
	Key            string                   `json:"key"`
	Label          string                   `json:"label"`
	Component      string                   `json:"component"`
	Required       bool                     `json:"required"`
	Rules          []map[string]interface{} `json:"rules"`
	ComponentProps map[string]interface{}   `json:"componentProps,omitempty"`
}

type inParams struct {
	ProjectID int64 `json:"projectId"`
}

type columns struct {
	Title     string `json:"title"`
	DataIndex string `json:"dataIndex"`
	Width     int    `json:"width,omitempty"`
	Ellipsis  bool   `json:"ellipsis"`
	Fixed     string `json:"fixed"`
}

type dataOperation struct {
	Key         string                 `json:"key"`
	Reload      bool                   `json:"reload"`
	Text        string                 `json:"text"`
	Disabled    bool                   `json:"disabled"`
	DisabledTip string                 `json:"disabledTip,omitempty"`
	Confirm     string                 `json:"confirm,omitempty"`
	Meta        interface{}            `json:"meta,omitempty"`
	Command     map[string]interface{} `json:"command,omitempty"`
}

type RowData struct {
	Name              string `json:"name"`
	SnippetPipelineID uint64 `json:"snippetPipelineID"`
}

type AutoTestRunStep struct {
	ApiSpec     map[string]interface{} `json:"apiSpec"`
	WaitTime    int64                  `json:"waitTime"`
	Commands    []string               `json:"commands"`
	Image       string                 `json:"image"`
	WaitTimeSec int64                  `json:"waitTimeSec"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "executeTaskTable",
		func() servicehub.Provider { return &ExecuteTaskTable{} })
}

func (a *ExecuteTaskTable) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (a *ExecuteTaskTable) Export(c *cptype.Component, gs *cptype.GlobalStateData) error {
	// set component data
	b, err := json.Marshal(a)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, c); err != nil {
		return err
	}
	return nil
}

func (a *ExecuteTaskTable) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	gh := gshelper.NewGSHelper(gs)
	// import component data
	if err := a.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}
	a.pipelineID = gh.GetExecuteHistoryTablePipelineID()
	a.sdk = cputil.SDK(ctx)

	a.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	defer func() {
		fail := a.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
		// export rendered component data
		c.Operations = a.Operations
		c.Props = getProps(ctx)
	}()

	// listen on operation
	switch event.Operation {
	case cptype.OperationKey(apistructs.ExecuteChangePageNoOperationKey), cptype.RenderingOperation, cptype.InitializeOperation:
		a.State.Unfold = false
		if err := a.handlerListOperation(c, event, gh); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.ExecuteClickRowNoOperationKey):
		if err := a.handlerClickRowOperation(c, event, gh); err != nil {
			return err
		}
	}
	gh.SetExecuteHistoryTablePipelineID(a.pipelineID)
	return nil
}

func getOperations(clickableKeys []uint64) map[string]interface{} {
	return map[string]interface{}{
		"clickRow": Operation{
			Key:           "clickRow",
			Reload:        true,
			FillMeta:      "target",
			Meta:          nil,
			ClickableKeys: clickableKeys,
		},
	}
}

func getProps(ctx context.Context) map[string]interface{} {
	return map[string]interface{}{
		"rowKey":     "key",
		"scroll":     map[string]interface{}{"x": 1200},
		"hideHeader": true,
		"columns": []columns{
			{
				Title:     cputil.I18n(ctx, "stepName"),
				DataIndex: "name",
				Width:     200,
				Ellipsis:  true,
				Fixed:     "left",
			},
			{
				Title:     cputil.I18n(ctx, "stepType"),
				DataIndex: "type",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     cputil.I18n(ctx, "step"),
				DataIndex: "step",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     cputil.I18n(ctx, "subtask"),
				DataIndex: "tasksNum",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     cputil.I18n(ctx, "apiPath"),
				DataIndex: "path",
			},
			{
				Title:     cputil.I18n(ctx, "state"),
				DataIndex: "status",
				Width:     120,
				Ellipsis:  true,
			},
			{
				Title:     cputil.I18n(ctx, "operate"),
				DataIndex: "operate",
				Width:     120,
				Ellipsis:  true,
				Fixed:     "right",
			},
		},
	}
}

func (a *ExecuteTaskTable) transformStepType(str apistructs.StepAPIType) string {
	switch str {
	case apistructs.StepTypeWait:
		return a.sdk.I18n("wait")
	case apistructs.StepTypeAPI:
		return a.sdk.I18n("API")
	case apistructs.StepTypeScene:
		return a.sdk.I18n("scene")
	case apistructs.StepTypeConfigSheet:
		return a.sdk.I18n("configForm")
	case apistructs.StepTypeCustomScript:
		return a.sdk.I18n("custom")
	case apistructs.AutotestSceneSet:
		return a.sdk.I18n("sceneset")
	}
	return string(str)
}

func (a *ExecuteTaskTable) getStatus(req apistructs.PipelineStatus) map[string]interface{} {
	res := map[string]interface{}{"renderType": "textWithBadge", "value": a.sdk.I18n(util.ColumnPipelineStatus + req.String())}
	if req.IsSuccessStatus() {
		res["status"] = "success"
	}
	if req.IsFailedStatus() {
		res["status"] = "error"
	}
	if req.IsReconcilerRunningStatus() {
		res["status"] = "processing"
	}
	if req.IsBeforePressRunButton() || req.IsNoNeedBySystem() {
		res["status"] = "default"
	}
	return res
}

func (a *ExecuteTaskTable) setData(pipeline *apistructs.PipelineDetailDTO) error {
	lists := []map[string]interface{}{}
	clickableKeys := []uint64{}
	a.State.Total = 0
	stepIdx := 1
	for _, each := range pipeline.PipelineStages {

		a.State.Total += int64(len(each.PipelineTasks))

		for _, task := range each.PipelineTasks {
			var item map[string]interface{}
			// not autotest task
			// snippet acton remove operation add taskNum
			if task.Labels == nil || len(task.Labels) == 0 {
				operations := map[string]interface{}{}
				var taskNum interface{}
				if task.Type != apistructs.ActionTypeSnippet {
					operations = map[string]interface{}{
						"checkDetail": dataOperation{
							Key:         "checkDetail",
							Text:        a.sdk.I18n("checkRes"),
							Reload:      false,
							Meta:        task.Result,
							DisabledTip: "禁用接口无法查看结果",
							Disabled:    task.Status.IsDisabledStatus(),
						},
						"checkLog": dataOperation{
							Key:    "checkLog",
							Reload: false,
							Text:   a.sdk.I18n("log"),
							Meta: map[string]interface{}{
								"logId":      task.Extra.UUID,
								"pipelineId": a.pipelineID,
								"nodeId":     task.ID,
							},
							DisabledTip: "禁用接口无法查看日志",
							Disabled:    task.Status.IsDisabledStatus(),
						},
					}
					taskNum = "-"
				} else {
					clickableKeys = append(clickableKeys, task.ID)
					if task.SnippetPipelineDetail != nil {
						taskNum = task.SnippetPipelineDetail.DirectSnippetTasksNum
					}
				}
				item = map[string]interface{}{
					"id":                task.ID,
					"key":               component_key.GetKey(task.ID),
					"snippetPipelineID": task.SnippetPipelineID,
					"operate": map[string]interface{}{
						"renderType": "tableOperation",
						"operations": operations,
					},
					"tasksNum": taskNum,
					"name":     task.Name,
					"status":   a.getStatus(task.Status),
					"type":     a.transformStepType(apistructs.StepAPIType(task.Type)),
					"path":     "",
					"step":     stepIdx,
				}
				lists = append(lists, item)
			} else {
				switch task.Labels[apistructs.AutotestType] {
				case apistructs.AutotestSceneStep:
					res := apistructs.AutoTestSceneStep{}
					value := AutoTestRunStep{
						ApiSpec: map[string]interface{}{},
					}
					if _, ok := task.Labels[apistructs.AutotestSceneStep]; ok {
						resByte, err := base64.StdEncoding.DecodeString(task.Labels[apistructs.AutotestSceneStep])
						if err != nil {
							logrus.Error("error to decode ", apistructs.AutotestSceneStep, ", err: ", err)
							return err
						}
						if err := json.Unmarshal(resByte, &res); err != nil {
							return err
						}

						if res.Type == apistructs.StepTypeAPI || res.Type == apistructs.StepTypeWait || res.Type == apistructs.StepTypeCustomScript {
							err := json.Unmarshal([]byte(res.Value), &value)
							if err != nil {
								return err
							}
						}

						if res.Type == apistructs.StepTypeWait {
							if value.WaitTime > 0 {
								value.WaitTimeSec = value.WaitTime
							}
							res.Name = a.transformStepType(res.Type) + strconv.FormatInt(value.WaitTimeSec, 10) + "s"
						}
					} else {
						res.Name = task.Name
						res.Type = apistructs.StepAPIType(task.Type)
					}
					operations := map[string]interface{}{}
					if res.Type == apistructs.StepTypeAPI || res.Type == apistructs.StepTypeWait || res.Type == apistructs.StepTypeCustomScript {
						operations = map[string]interface{}{
							"checkDetail": dataOperation{
								Key:         "checkDetail",
								Text:        a.sdk.I18n("checkRes"),
								Reload:      false,
								Disabled:    task.Status.IsDisabledStatus(),
								DisabledTip: "禁用接口无法查看结果",
								Meta:        task.Result,
							},
							"checkLog": dataOperation{
								Key:         "checkLog",
								Reload:      false,
								Text:        a.sdk.I18n("log"),
								Disabled:    task.Status.IsDisabledStatus(),
								DisabledTip: "禁用接口无法查看日志",
								Meta: map[string]interface{}{
									"logId":      task.Extra.UUID,
									"pipelineId": a.pipelineID,
									"nodeId":     task.ID,
								},
							},
						}
					}
					path := value.ApiSpec["url"]
					if path == nil {
						path = ""
					}
					item = map[string]interface{}{
						"id":                task.ID,
						"key":               component_key.GetKey(task.ID),
						"snippetPipelineID": task.SnippetPipelineID,
						"operate": map[string]interface{}{
							"renderType": "tableOperation",
							"operations": operations,
						},
						"tasksNum": "-",
						"name":     res.Name,
						"status":   a.getStatus(task.Status),
						"type":     a.transformStepType(res.Type),
						"path":     path,
						"step":     stepIdx,
					}
					if task.SnippetPipelineID != nil &&
						(res.Type == apistructs.StepTypeScene || res.Type == apistructs.StepTypeConfigSheet) {
						clickableKeys = append(clickableKeys, task.ID)
						if task.SnippetPipelineDetail != nil {
							item["tasksNum"] = task.SnippetPipelineDetail.DirectSnippetTasksNum
						}
					}
					lists = append(lists, item)
				case apistructs.AutotestSceneSet:
					res := apistructs.TestPlanV2Step{}
					if _, ok := task.Labels[apistructs.AutotestSceneSet]; ok {
						resByte, err := base64.StdEncoding.DecodeString(task.Labels[apistructs.AutotestSceneSet])
						if err != nil {
							logrus.Error("error to decode ", apistructs.AutotestSceneSet, ", err: ", err)
							return err
						}
						if err := json.Unmarshal(resByte, &res); err != nil {
							return err
						}
					}
					item = map[string]interface{}{
						"id":                task.ID,
						"key":               component_key.GetKey(task.ID),
						"snippetPipelineID": task.SnippetPipelineID,
						"operate": map[string]interface{}{
							"renderType": "tableOperation",
						},
						"tasksNum": "-",
						"name":     res.SceneSetName,
						"status":   a.getStatus(task.Status),
						"type":     a.sdk.I18n("sceneset"),
						"path":     "",
						"step":     stepIdx,
					}
					if task.SnippetPipelineDetail != nil {
						item["tasksNum"] = task.SnippetPipelineDetail.DirectSnippetTasksNum
					}
					lists = append(lists, item)
					if task.SnippetPipelineID != nil {
						clickableKeys = append(clickableKeys, task.ID)
					}
				case apistructs.AutotestScene:
					res := apistructs.AutoTestScene{}
					if _, ok := task.Labels[apistructs.AutotestScene]; ok {
						resByte, err := base64.StdEncoding.DecodeString(task.Labels[apistructs.AutotestScene])
						if err != nil {
							logrus.Error("error to decode ", apistructs.AutotestScene, ", err: ", err)
							return err
						}
						if err := json.Unmarshal(resByte, &res); err != nil {
							return err
						}
					}
					item = map[string]interface{}{
						"id":                task.ID,
						"key":               component_key.GetKey(task.ID),
						"snippetPipelineID": task.SnippetPipelineID,
						"operate": map[string]interface{}{
							"renderType": "tableOperation",
						},
						"tasksNum": "-",
						"name":     res.Name,
						"status":   a.getStatus(task.Status),
						"type":     a.sdk.I18n("scene"),
						"path":     "",
						"step":     stepIdx,
					}
					if task.SnippetPipelineDetail != nil {
						item["tasksNum"] = task.SnippetPipelineDetail.DirectSnippetTasksNum
					}
					lists = append(lists, item)
					if task.SnippetPipelineID != nil {
						clickableKeys = append(clickableKeys, task.ID)
					}
				}
			}
		}
		stepIdx++
	}

	a.Data = make(map[string]interface{})
	a.Data["list"] = lists
	a.Operations = getOperations(clickableKeys)
	return nil
}

func (a *ExecuteTaskTable) marshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(a.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(a.Props)
	if err != nil {
		return err
	}
	var props cptype.ComponentProps
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Props = props
	c.State = state
	c.Type = a.Type
	return nil
}

func (e *ExecuteTaskTable) handlerListOperation(c *cptype.Component, event cptype.ComponentEvent, gh *gshelper.GSHelper) error {

	if e.pipelineID == 0 {
		c.Data = map[string]interface{}{}
		return nil
	}
	if e.State.PageNo == 0 {
		e.State.PageNo = 1
	}
	if e.State.PageSize == 0 {
		e.State.PageSize = 10
	}

	list := gh.GetPipelineInfoWithPipelineID(e.pipelineID, e.bdl)
	if list == nil {
		return fmt.Errorf("not find pipelineID %v info", e.pipelineID)
	}

	err := e.setData(list)
	if err != nil {
		return err
	}
	c.Data = e.Data
	return nil
}

func (e *ExecuteTaskTable) handlerClickRowOperation(c *cptype.Component, event cptype.ComponentEvent, gh *gshelper.GSHelper) error {

	res := operationData{}
	b, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &res); err != nil {
		return err
	}
	e.State.Name = res.Meta.Target.Name
	e.pipelineID = res.Meta.Target.SnippetPipelineID
	e.State.Unfold = true
	e.State.PageNo = 1
	if res.Meta.Target.SnippetPipelineID == 0 {
		return nil
	}
	if err := e.handlerListOperation(c, event, gh); err != nil {
		return err
	}
	return nil
}
