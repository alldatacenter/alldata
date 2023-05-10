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

package executeHistoryTable

import (
	"context"
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
)

type ExecuteHistoryTable struct {
	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper
	sdk      *cptype.SDK

	Type       string                 `json:"type"`
	State      State                  `json:"state"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	Data       map[string]interface{} `json:"data"`

	pipelineID uint64
}

type State struct {
	Total    int64 `json:"total"`
	PageSize int64 `json:"pageSize"`
	PageNo   int64 `json:"pageNo"`
}

const (
	DefaultPageSize = 15
	DefaultPageNo   = 1
)

type Operation struct {
	Key      string                 `json:"key"`
	Reload   bool                   `json:"reload"`
	FillMeta string                 `json:"fillMeta"`
	Meta     map[string]interface{} `json:"meta"`
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
}

type operationData struct {
	Meta meta `json:"meta"`
}

type rowData struct {
	PipelineID uint64 `json:"pipelineId"`
}

type meta struct {
	RowData rowData `json:"rowData"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "executeHistoryTable",
		func() servicehub.Provider { return &ExecuteHistoryTable{} })
}

func (a *ExecuteHistoryTable) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (a *ExecuteHistoryTable) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	gh := gshelper.NewGSHelper(gs)
	a.sdk = cputil.SDK(ctx)
	// import component data
	if err := a.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}
	a.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	a.gsHelper = gshelper.NewGSHelper(gs)

	defer func() {
		fail := a.marshal(ctx, c)
		if err == nil && fail != nil {
			err = fail
		}
		// export rendered component data
		c.Operations = getOperations()
		c.Props = getProps(ctx)
	}()

	// listen on operation
	switch event.Operation {
	case cptype.OperationKey(apistructs.ExecuteChangePageNoOperationKey), cptype.RenderingOperation, cptype.InitializeOperation:
		if err := a.handlerListOperation(c, event, gs); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.ExecuteClickRowNoOperationKey):
		if err := a.handlerClickRowOperation(c, event); err != nil {
			return err
		}
	}

	// set global state
	gh.SetExecuteHistoryTablePipelineID(a.pipelineID)

	return nil
}

func (a *ExecuteHistoryTable) marshal(ctx context.Context, c *cptype.Component) error {
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
	c.Props = getProps(ctx)
	c.State = state
	c.Type = a.Type
	return nil
}

func getOperations() map[string]interface{} {
	return map[string]interface{}{
		"changePageNo": Operation{
			Key:    "changePageNo",
			Reload: true,
		},
		"clickRow": Operation{
			Key:      "clickRow",
			Reload:   true,
			FillMeta: "rowData",
			Meta:     map[string]interface{}{"rowData": ""},
		},
	}
}

func getProps(ctx context.Context) map[string]interface{} {
	return map[string]interface{}{
		"rowKey":     "id",
		"hideHeader": true,
		"columns": []columns{
			{
				Title:     cputil.I18n(ctx, "version"),
				DataIndex: "version",
				Width:     60,
			},
			{
				Title:     "ID",
				DataIndex: "pipelineId",
			},
			{
				Title:     cputil.I18n(ctx, "state"),
				DataIndex: "status",
			},
			{
				Title:     cputil.I18n(ctx, "executor"),
				DataIndex: "runUser",
			},
			{
				Title:     cputil.I18n(ctx, "triggerTime"),
				DataIndex: "triggerTime",
			},
		},
	}
}

func (e *ExecuteHistoryTable) getStatus(req apistructs.PipelineStatus) map[string]interface{} {
	res := map[string]interface{}{"renderType": "textWithBadge", "value": e.sdk.I18n(util.ColumnPipelineStatus + req.String())}
	if req.IsFailedStatus() {
		res["status"] = "error"
	} else if req.IsSuccessStatus() {
		res["status"] = "success"
	} else if req.IsReconcilerRunningStatus() {
		res["status"] = "processing"
	} else {
		res["status"] = "default"
	}
	return res
}

func (e *ExecuteHistoryTable) setData(pipeline *apistructs.PipelinePageListData, num int64, event cptype.OperationKey) error {
	lists := []map[string]interface{}{}
	if len(pipeline.Pipelines) > 0 && event != cptype.OperationKey(apistructs.ExecuteChangePageNoOperationKey) {
		e.pipelineID = pipeline.Pipelines[0].ID
	} else if len(pipeline.Pipelines) == 0 {
		e.pipelineID = 0
	}
	for _, each := range pipeline.Pipelines {
		var timeLayoutStr = "2006-01-02 15:04:05" //go中的时间格式化必须是这个时间
		var runUser string
		if each.Extra.RunUser != nil {
			runUser = each.Extra.RunUser.Name
		}
		list := map[string]interface{}{
			//"id":          each.ID,
			"version":     "#" + strconv.FormatInt(num, 10),
			"pipelineId":  each.ID,
			"status":      e.getStatus(each.Status),
			"runUser":     runUser,
			"triggerTime": each.TimeCreated.Format(timeLayoutStr),
		}
		lists = append(lists, list)
		num--
	}
	e.Data = make(map[string]interface{})
	e.Data["list"] = lists
	return nil
}

func (e *ExecuteHistoryTable) handlerListOperation(c *cptype.Component, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)
	setID := e.gsHelper.GetGlobalSelectedSetID()
	configKey := e.gsHelper.GetGlobalActiveConfig()
	sceneID := gh.GetFileTreeSceneID()
	if configKey == "" {
		c.Data = map[string]interface{}{}
		e.State = State{}
		return nil
	}
	if sceneID == 0 && setID == 0 {
		c.Data = map[string]interface{}{}
		e.State = State{}
		return nil
	}
	req := apistructs.PipelinePageListRequest{
		Sources: []apistructs.PipelineSource{apistructs.PipelineSourceAutoTest},
	}
	if sceneID != 0 && configKey == gshelper.SceneConfigKey {
		req.YmlNames = []string{strconv.FormatUint(sceneID, 10)}
	}
	if setID != 0 && configKey == gshelper.SceneSetConfigKey {
		req.YmlNames = []string{fmt.Sprintf("autotest-scene-set-%s", strconv.FormatUint(setID, 10))}
	}
	if e.State.PageNo == 0 {
		e.State.PageNo = DefaultPageNo
		e.State.PageSize = DefaultPageSize
	}
	req.PageNum = int(e.State.PageNo)
	req.PageSize = int(e.State.PageSize)
	list, err := e.bdl.PageListPipeline(req)
	if err != nil {
		return err
	}
	e.State.Total = list.Total
	err = e.setData(list, list.Total-(e.State.PageNo-1)*e.State.PageSize, event.Operation)
	if err != nil {
		return err
	}
	c.Data = e.Data
	return nil
}

func (e *ExecuteHistoryTable) handlerClickRowOperation(c *cptype.Component, event cptype.ComponentEvent) error {
	res := operationData{}
	b, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &res); err != nil {
		return err
	}
	e.pipelineID = res.Meta.RowData.PipelineID
	return nil
}
