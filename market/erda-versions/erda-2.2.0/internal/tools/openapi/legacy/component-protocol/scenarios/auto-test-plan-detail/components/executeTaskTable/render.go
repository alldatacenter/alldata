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
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/pkg/component_key"
	i18nkey "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
	"github.com/erda-project/erda/pkg/i18n"
)

type ExecuteTaskTable struct {
	CtxBdl     protocol.ContextBundle
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Data       map[string]interface{} `json:"data"`
}

type State struct {
	Total          int64                         `json:"total"`
	PageSize       int64                         `json:"pageSize"`
	PageNo         int64                         `json:"pageNo"`
	PipelineID     uint64                        `json:"pipelineId"`
	StepID         uint64                        `json:"stepId"`
	Name           string                        `json:"name"`
	Unfold         bool                          `json:"unfold"`
	PipelineDetail *apistructs.PipelineDetailDTO `json:"pipelineDetail"`
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

func (a *ExecuteTaskTable) Import(c *apistructs.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (a *ExecuteTaskTable) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	// import component data
	if err := a.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}

	a.CtxBdl = ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	i18nLocale := a.CtxBdl.Bdl.GetLocale(a.CtxBdl.Locale)

	if a.CtxBdl.InParams == nil {
		return fmt.Errorf("params is empty")
	}
	inParamsBytes, err := json.Marshal(a.CtxBdl.InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", a.CtxBdl.InParams, err)
	}
	var inParams inParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}

	defer func() {
		fail := a.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
		// export rendered component data
		c.Operations = a.Operations
		c.Props = getProps(i18nLocale)
	}()

	// listen on operation
	switch event.Operation {
	case apistructs.ExecuteChangePageNoOperationKey:
		a.State.Unfold = true
		if err := a.handlerListOperation(a.CtxBdl, c, inParams, event); err != nil {
			return err
		}
	case apistructs.RenderingOperation, apistructs.InitializeOperation:
		a.State.Unfold = false
		if err := a.handlerListOperation(a.CtxBdl, c, inParams, event); err != nil {
			return err
		}
	case apistructs.ExecuteClickRowNoOperationKey:
		if err := a.handlerClickRowOperation(a.CtxBdl, c, inParams, event); err != nil {
			return err
		}
	}
	a.Props = getProps(i18nLocale)
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

func getProps(i18nLocale *i18n.LocaleResource) map[string]interface{} {
	return map[string]interface{}{
		"rowKey":     "key",
		"hideHeader": true,
		"scroll":     map[string]interface{}{"x": 1200},
		"columns": []columns{
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyStepName),
				DataIndex: "name",
				Width:     200,
				Ellipsis:  true,
				Fixed:     "left",
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyStepType),
				DataIndex: "type",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyStep),
				DataIndex: "step",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyNumberOfSubtasks),
				DataIndex: "tasksNum",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyStepExecutionTime),
				DataIndex: "time",
				Width:     85,
				Ellipsis:  true,
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyInterfacePath),
				DataIndex: "path",
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyStatus),
				DataIndex: "status",
				Width:     120,
				Ellipsis:  true,
			},
			{
				Title:     i18nLocale.Get(i18nkey.I18nKeyOperations),
				DataIndex: "operate",
				Width:     120,
				Ellipsis:  true,
				Fixed:     "right",
			},
		},
	}
}

func transformStepType(str apistructs.StepAPIType, i18nLocale *i18n.LocaleResource) string {
	switch str {
	case apistructs.StepTypeWait:
		return i18nLocale.Get(i18nkey.I18nKeyWait)
	case apistructs.StepTypeAPI:
		return i18nLocale.Get(i18nkey.I18nKeyInterface)
	case apistructs.StepTypeScene:
		return i18nLocale.Get(i18nkey.I18nKeyScene)
	case apistructs.StepTypeConfigSheet:
		return i18nLocale.Get(i18nkey.I18nKeyConfigSheet)
	case apistructs.StepTypeCustomScript:
		return i18nLocale.Get(i18nkey.I18nKeyCustomize)
	case apistructs.AutotestSceneStep:
		return i18nLocale.Get(i18nkey.I18nKeyStep)
	case apistructs.AutotestSceneSet:
		return i18nLocale.Get(i18nkey.I18nKeySceneSet)
	}
	return string(str)
}

func getStatus(req apistructs.PipelineStatus, i18nLocale *i18n.LocaleResource) map[string]interface{} {
	res := map[string]interface{}{"renderType": "textWithBadge", "value": i18nkey.TransferTaskStatus(req, i18nLocale)}
	if req.IsSuccessStatus() {
		res["status"] = "success"
	}
	if req.IsFailedStatus() {
		res["status"] = "error"
	}
	if req.IsReconcilerRunningStatus() {
		res["status"] = "processing"
	}
	if req.IsBeforePressRunButton() {
		res["status"] = "default"
	}
	if req.IsDisabledStatus() {
		res["status"] = "default"
	}
	if req.IsNoNeedBySystem() {
		res["status"] = "default"
	}
	return res
}

func (a *ExecuteTaskTable) setData(pipeline *apistructs.PipelineDetailDTO, i18nLocale *i18n.LocaleResource) error {
	lists := []map[string]interface{}{}
	clickableKeys := []uint64{}
	a.State.Total = 0
	stepIdx := 1
	for _, each := range pipeline.PipelineStages {
		a.State.Total += int64(len(each.PipelineTasks))
		for _, task := range each.PipelineTasks {
			if task.Labels == nil || len(task.Labels) == 0 {
				list := map[string]interface{}{
					"key":               component_key.GetKey(task.ID),
					"id":                task.ID,
					"snippetPipelineID": task.SnippetPipelineID,
					"operate": map[string]interface{}{
						"renderType": "tableOperation",
						"operations": map[string]interface{}{
							"checkDetail": dataOperation{
								Key:         "checkDetail",
								Text:        i18nLocale.Get(i18nkey.I18nKeyViewResult),
								Reload:      false,
								DisabledTip: i18nLocale.Get(i18nkey.I18nKeyDisableTaskTip),
								Disabled:    task.Status.IsDisabledStatus(),
								Meta:        task.Result,
							},
							"checkLog": dataOperation{
								Key:         "checkLog",
								Reload:      false,
								Text:        i18nLocale.Get(i18nkey.I18nKeyLog),
								DisabledTip: i18nLocale.Get(i18nkey.I18nKeyDisableLogTip),
								Disabled:    task.Status.IsDisabledStatus(),
								Meta: map[string]interface{}{
									"logId":      task.Extra.UUID,
									"pipelineId": a.State.PipelineID,
									"nodeId":     task.ID,
								},
							},
						},
					},
					"tasksNum": "-",
					"name":     task.Name,
					"status":   getStatus(task.Status, i18nLocale),
					"type":     transformStepType(apistructs.StepAPIType(task.Type), i18nLocale),
					"path":     "",
					"time":     a.getCostTime(task),
				}
				lists = append(lists, list)
				continue
			}
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
						res.Name = transformStepType(res.Type, i18nLocale) + strconv.FormatInt(value.WaitTimeSec, 10) + "s"
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
							Text:        i18nLocale.Get(i18nkey.I18nKeyViewResult),
							Reload:      false,
							Disabled:    task.Status.IsDisabledStatus(),
							DisabledTip: i18nLocale.Get(i18nkey.I18nKeyDisableTaskTip),
							Meta:        task.Result,
						},
						"checkLog": dataOperation{
							Key:         "checkLog",
							Reload:      false,
							Text:        i18nLocale.Get(i18nkey.I18nKeyLog),
							Disabled:    task.Status.IsDisabledStatus(),
							DisabledTip: i18nLocale.Get(i18nkey.I18nKeyDisableLogTip),
							Meta: map[string]interface{}{
								"logId":      task.Extra.UUID,
								"pipelineId": a.State.PipelineID,
								"nodeId":     task.ID,
							},
						},
					}
				}
				path := value.ApiSpec["url"]
				if path == nil {
					path = ""
				}
				list := map[string]interface{}{
					"key":               component_key.GetKey(task.ID),
					"id":                task.ID,
					"snippetPipelineID": task.SnippetPipelineID,
					"operate": map[string]interface{}{
						"renderType": "tableOperation",
						"operations": operations,
					},
					"tasksNum": "-",
					"name":     res.Name,
					"status":   getStatus(task.Status, i18nLocale),
					"type":     transformStepType(res.Type, i18nLocale),
					"path":     path,
					"time":     a.getCostTime(task),
					"step":     stepIdx,
				}

				if task.SnippetPipelineID != nil && (res.Type == apistructs.StepTypeScene ||
					res.Type == apistructs.StepTypeConfigSheet || res.Type == apistructs.AutotestSceneSet) {
					clickableKeys = append(clickableKeys, task.ID)
					if task.SnippetPipelineDetail != nil {
						list["tasksNum"] = task.SnippetPipelineDetail.DirectSnippetTasksNum
					}
				}
				lists = append(lists, list)
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
				list := map[string]interface{}{
					"key":               component_key.GetKey(task.ID),
					"id":                task.ID,
					"snippetPipelineID": task.SnippetPipelineID,
					"operate": map[string]interface{}{
						"renderType": "tableOperation",
					},
					"tasksNum": "-",
					"name":     res.SceneSetName,
					"status":   getStatus(task.Status, i18nLocale),
					"type":     transformStepType(apistructs.AutotestSceneSet, i18nLocale),
					"path":     "",
					"time":     a.getCostTime(task),
					"step":     stepIdx,
				}
				if task.SnippetPipelineDetail != nil {
					list["tasksNum"] = task.SnippetPipelineDetail.DirectSnippetTasksNum
				}
				lists = append(lists, list)
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
				list := map[string]interface{}{
					"key":               component_key.GetKey(task.ID),
					"id":                task.ID,
					"snippetPipelineID": task.SnippetPipelineID,
					"operate": map[string]interface{}{
						"renderType": "tableOperation",
					},
					"tasksNum": "-",
					"name":     res.Name,
					"status":   getStatus(task.Status, i18nLocale),
					"type":     transformStepType(apistructs.AutotestScene, i18nLocale),
					"path":     "",
					"time":     a.getCostTime(task),
					"step":     stepIdx,
				}
				if task.SnippetPipelineDetail != nil {
					list["tasksNum"] = task.SnippetPipelineDetail.DirectSnippetTasksNum
				}
				lists = append(lists, list)
				if task.SnippetPipelineID != nil {
					clickableKeys = append(clickableKeys, task.ID)
				}
			}
		}
		stepIdx++
	}
	a.Data["list"] = lists
	a.Operations = getOperations(clickableKeys)
	return nil
}

// getCostTime the format of time is "00:00:00"
// id is not end status or err return "-"
func (a *ExecuteTaskTable) getCostTime(task apistructs.PipelineTaskDTO) string {
	if !task.Status.IsEndStatus() {
		return "-"
	}
	if task.CostTimeSec < 0 {
		return "-"
	}
	return time.Unix(task.CostTimeSec, 0).In(time.UTC).Format("15:04:05")
}

func (a *ExecuteTaskTable) marshal(c *apistructs.Component) error {
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
	var props interface{}
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Props = props
	c.State = state
	c.Type = a.Type
	return nil
}

func (e *ExecuteTaskTable) handlerListOperation(bdl protocol.ContextBundle, c *apistructs.Component, inParams inParams, event apistructs.ComponentEvent) error {
	if e.State.PageNo == 0 {
		e.State.PageNo = 1
	}
	if e.State.PageSize == 0 {
		e.State.PageSize = 10
	}

	if e.State.PipelineDetail == nil {
		c.Data = map[string]interface{}{}
		return nil
	}
	if e.State.PipelineDetail.ID == 0 {
		c.Data = map[string]interface{}{}
		return nil
	}

	i18nLocale := bdl.Bdl.GetLocale(bdl.Locale)
	list := e.State.PipelineDetail
	err := e.setData(list, i18nLocale)
	if err != nil {
		return err
	}
	c.Data = e.Data
	return nil
}

func (e *ExecuteTaskTable) handlerClickRowOperation(bdl protocol.ContextBundle, c *apistructs.Component, inParams inParams, event apistructs.ComponentEvent) error {

	res := operationData{}
	b, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &res); err != nil {
		return err
	}
	e.State.Name = res.Meta.Target.Name
	e.State.PipelineID = res.Meta.Target.SnippetPipelineID
	e.State.Unfold = true
	e.State.PageNo = 1
	if res.Meta.Target.SnippetPipelineID == 0 {
		return nil
	}
	if err := e.handlerListOperation(e.CtxBdl, c, inParams, event); err != nil {
		return err
	}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ExecuteTaskTable{
		CtxBdl:     protocol.ContextBundle{},
		Props:      map[string]interface{}{},
		Operations: map[string]interface{}{},
		State:      State{},
		Data:       map[string]interface{}{},
	}
}
