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

package table

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-plan-list/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-plan-list/i18n"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type TestPlanManageTable struct{}

func init() {
	cpregister.RegisterLegacyComponent("auto-test-plan-list", "table", func() protocol.CompRender { return &TestPlanManageTable{} })
}

type TableItem struct {
	//Assignee    map[string]string `json:"assignee"`
	Id            uint64                 `json:"id"`
	Name          string                 `json:"name"`
	Owners        map[string]interface{} `json:"owners"`
	TestSpace     string                 `json:"testSpace"`
	Iteration     string                 `json:"iteration"`
	Operate       Operate                `json:"operate"`
	ExecuteApiNum string                 `json:"executeApiNum"`
	PassRate      PassRate               `json:"passRate"`
	ExecuteTime   string                 `json:"executeTime"`
}

type PassRate struct {
	RenderType string `json:"renderType"`
	Value      string `json:"value"`
}

type Operate struct {
	Operations map[string]interface{} `json:"operations"`
	RenderType string                 `json:"renderType"`
}

// OperationData 解析OperationData
type OperationData struct {
	Meta struct {
		ID         uint64 `json:"id"`
		IsArchived bool   `json:"isArchived"`
	} `json:"meta"`
}

type SortData struct {
	Field string `json:"field"`
	Order string `json:"order"`
}

const (
	OrderAscend  string = "ascend"
	OrderDescend string = "descend"
)

func (tpmt *TestPlanManageTable) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	sdk := cputil.SDK(ctx)
	bdl := sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	projectID := uint64(cputil.GetInParamByKey(sdk.Ctx, "projectId").(float64))

	cond := apistructs.TestPlanV2PagingRequest{ProjectID: projectID, PageNo: 1, PageSize: common.DefaultTablePageSize}
	cond.UserID = sdk.Identity.UserID
	if _, ok := c.State["pageNo"]; ok {
		cond.PageNo = uint64(c.State["pageNo"].(float64))
	}

	switch event.Operation.String() {
	case apistructs.InitializeOperation.String(), apistructs.RenderingOperation.String():
		// the table needs to be refreshed when other components are rendered, but the paging still needs to be maintained
		reloadTablePageNo, ok := c.State["reloadTablePageNo"].(bool)
		if !ok {
			reloadTablePageNo = true
		}
		if reloadTablePageNo {
			cond.PageNo = 1
		}
	case "edit":
		var operationData OperationData
		odBytes, err := json.Marshal(event.OperationData)
		if err != nil {
			return err
		}
		if err := json.Unmarshal(odBytes, &operationData); err != nil {
			return err
		}
		c.State["formModalVisible"] = true
		c.State["formModalTestPlanID"] = operationData.Meta.ID
		return nil
	case "archive":
		var operationData OperationData
		odBytes, err := json.Marshal(event.OperationData)
		if err != nil {
			return err
		}
		if err := json.Unmarshal(odBytes, &operationData); err != nil {
			return err
		}
		testplan, err := bdl.GetTestPlanV2(operationData.Meta.ID)
		if err != nil {
			return err
		}
		if err := bdl.UpdateTestPlanV2(apistructs.TestPlanV2UpdateRequest{
			Name:         testplan.Data.Name,
			Desc:         testplan.Data.Desc,
			SpaceID:      testplan.Data.SpaceID,
			Owners:       testplan.Data.Owners,
			IsArchived:   &operationData.Meta.IsArchived,
			TestPlanID:   testplan.Data.ID,
			IterationID:  testplan.Data.IterationID,
			IdentityInfo: apistructs.IdentityInfo{UserID: sdk.Identity.UserID},
		}); err != nil {
			return err
		}
		now := strconv.FormatInt(time.Now().Unix(), 10)
		project, err := bdl.GetProject(projectID)
		if err != nil {
			return err
		}
		audit := apistructs.Audit{
			UserID:       cond.UserID,
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      projectID,
			OrgID:        project.OrgID,
			ProjectID:    projectID,
			Result:       "success",
			StartTime:    now,
			EndTime:      now,
			TemplateName: apistructs.ArchiveTestplanTemplate,
			Context: map[string]interface{}{
				"projectName":  project.Name,
				"testPlanName": testplan.Data.Name,
			},
		}
		if !operationData.Meta.IsArchived {
			audit.TemplateName = apistructs.UnarchiveTestPlanTemplate
		}
		if err := bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: audit}); err != nil {
			return err
		}
	}

	// filter带过来的
	if _, ok := c.State["name"]; ok {
		cond.Name = c.State["name"].(string)
	}
	iterationIDs := getIterations(c.State)
	if iterationIDs != nil {
		cond.IterationIDs = iterationIDs
	}

	if v, ok := c.State["archive"]; ok && v != nil {
		var isArchive bool
		if s := v.(bool); s == true {
			isArchive = true
		}
		cond.IsArchived = &isArchive
	}
	// orderBy and ASC
	err := convertSortData(&cond, c)
	if err != nil {
		return err
	}

	r, err := bdl.PagingTestPlansV2(cond)
	if err != nil {
		return err
	}
	// data
	var l []TableItem
	for _, data := range r.List {
		item := TableItem{
			Id:   data.ID,
			Name: data.Name,
			Owners: map[string]interface{}{
				"renderType": "userAvatar",
				"value":      data.Owners,
				"showIcon":   false,
			},
			TestSpace: data.SpaceName,
			Iteration: data.IterationName,
			Operate: Operate{
				RenderType: "tableOperation",
				Operations: map[string]interface{}{},
			},
			ExecuteApiNum: strconv.FormatInt(data.ExecuteApiNum, 10),
			PassRate: PassRate{
				RenderType: "progress",
				Value:      fmt.Sprintf("%.2f", data.PassRate),
			},
			ExecuteTime: convertExecuteTime(data),
		}
		if data.IsArchived == true {
			item.Operate.Operations["archive"] = map[string]interface{}{
				"key":    "archive",
				"text":   sdk.I18n(i18n.I18nKeyUnarchive),
				"reload": true,
				"meta":   map[string]interface{}{"id": data.ID, "isArchived": false},
			}
		} else {
			item.Operate.Operations["archive"] = map[string]interface{}{
				"key":    "archive",
				"text":   sdk.I18n(i18n.I18nKeyArchive),
				"reload": true,
				"meta":   map[string]interface{}{"id": data.ID, "isArchived": true},
			}
			item.Operate.Operations["edit"] = map[string]interface{}{
				"key":       "edit",
				"text":      sdk.I18n(i18n.I18nKeyEdit),
				"reload":    true,
				"meta":      map[string]interface{}{"id": data.ID},
				"showIndex": 2,
			}
		}
		l = append(l, item)
	}
	c.Data = map[string]interface{}{}
	c.Data["list"] = l
	// state
	if c.State == nil {
		c.State = make(map[string]interface{}, 0)
	}
	c.State["total"] = r.Total
	c.State["pageNo"] = cond.PageNo
	c.State["pageSize"] = cond.PageSize
	c.State["formModalVisible"] = false
	c.State["formModalTestPlanID"] = 0
	(*gs)[cptype.GlobalInnerKeyUserIDs.String()] = r.UserIDs
	return nil
}

func convertSortData(req *apistructs.TestPlanV2PagingRequest, c *cptype.Component) error {
	if _, ok := c.State["sorterData"]; !ok {
		return nil
	}
	var sortData SortData
	sortDataByte, err := json.Marshal(c.State["sorterData"])
	if err != nil {
		return err
	}
	err = json.Unmarshal(sortDataByte, &sortData)
	if err != nil {
		return err
	}

	switch sortData.Field {
	case "passRate":
		sortData.Field = "pass_rate"
	case "executeTime":
		sortData.Field = "execute_time"
	case "executeApiNum":
		sortData.Field = "execute_api_num"
	}

	req.OrderBy = sortData.Field
	if sortData.Order == OrderAscend {
		req.Asc = true
	} else if sortData.Order == OrderDescend {
		req.Asc = false
	}

	return nil
}

func convertExecuteTime(data *apistructs.TestPlanV2) string {
	if data.ExecuteTime == nil {
		return ""
	}
	var executeTime string
	executeTime = data.ExecuteTime.Format("2006-01-02 15:04:05")
	return executeTime
}

func getIterations(state map[string]interface{}) (ids []uint64) {
	if state == nil {
		return
	}
	if _, ok := state["iteration"]; !ok && state["iteration"] == nil {
		return
	}

	if v, ok := state["iteration"].([]uint64); ok {
		return v
	}
	if v, ok := state["iteration"].([]interface{}); ok {
		for _, v2 := range v {
			if id, ok := v2.(float64); ok {
				ids = append(ids, uint64(id))
			}
		}
		return
	}
	return
}

// TODO:
//  1.创建更新完测试计划，要返回更新后的数据 (组件数据传递)
//  2.分页
//  3.根据标题过滤
