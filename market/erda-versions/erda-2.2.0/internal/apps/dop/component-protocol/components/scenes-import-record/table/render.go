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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/core/legacy/conf"
)

type ComponentAction struct {
	sdk    *cptype.SDK
	ctxBdl *bundle.Bundle

	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	State      State                  `json:"state"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	Data       Data                   `json:"data"`
}

type State struct {
	Values struct {
		Type []string `json:"type"`
	} `json:"values"`
	AutoRefresh bool `json:"autoRefresh"`
}

type Status struct {
	RenderType string `json:"renderType"`
	Value      string `json:"value"`
	Status     string `json:"status"`
}

type Result struct {
	RenderType string `json:"renderType"`
	URL        string `json:"url"`
	Value      string `json:"value"`
}

type DataItem struct {
	ID       string `json:"id"`
	Type     string `json:"type"`
	Operator string `json:"operator"`
	Time     string `json:"time"`
	Desc     string `json:"desc"`
	Status   Status `json:"status"`
	Result   Result `json:"result"`
}

type Data struct {
	List []DataItem `json:"list"`
}

type Column struct {
	DataIndex string `json:"dataIndex"`
	Title     string `json:"title"`
	Width     uint64 `json:"width,omitempty"`
}

func (ca *ComponentAction) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	ca.State = state
	return nil
}

func (ca *ComponentAction) setData() error {
	projectID, ok := ca.sdk.InParams["projectId"].(float64)
	if !ok {
		return fmt.Errorf("invalid project id: %v", projectID)
	}
	spacIDStr, ok := ca.sdk.InParams["spaceId"].(string)
	if !ok {
		return fmt.Errorf("invalid space id: %v", spacIDStr)
	}
	spaceID, err := strconv.ParseUint(spacIDStr, 10, 64)
	if err != nil {
		return err
	}
	fileTypes := make([]apistructs.FileActionType, 0)
	for _, t := range ca.State.Values.Type {
		if t == "export" {
			fileTypes = append(fileTypes, apistructs.FileSceneSetActionTypeExport)
		}
		if t == "import" {
			fileTypes = append(fileTypes, apistructs.FileSceneSetActionTypeImport)
		}
	}
	if len(fileTypes) == 0 {
		fileTypes = append(fileTypes, apistructs.FileSceneSetActionTypeExport, apistructs.FileSceneSetActionTypeImport)
	}
	rsp, err := ca.ctxBdl.ListFileRecords(ca.sdk.Identity.UserID, apistructs.ListTestFileRecordsRequest{
		ProjectID: uint64(projectID),
		Types:     fileTypes,
		SpaceID:   spaceID,
	})
	if err != nil {
		return err
	}

	ca.Data.List = make([]DataItem, 0)
	for _, fileRecord := range rsp.Data.List {
		var recordType string
		switch fileRecord.Type {
		case apistructs.FileSceneSetActionTypeImport:
			recordType = ca.sdk.I18n("import")
		case apistructs.FileSceneSetActionTypeExport:
			recordType = ca.sdk.I18n("export")
		default:
		}
		var status string
		var recordState string
		switch fileRecord.State {
		case apistructs.FileRecordStateFail:
			status = ca.sdk.I18n("status-failed")
			recordState = "error"
		case apistructs.FileRecordStateSuccess:
			status = ca.sdk.I18n("status-success")
			recordState = "success"
		case apistructs.FileRecordStatePending:
			status = ca.sdk.I18n("status-pending")
			recordState = "pending"
		case apistructs.FileRecordStateProcessing:
			status = ca.sdk.I18n("status-processing")
			recordState = "processing"
		default:
		}
		var operatorName string
		operator, err := ca.ctxBdl.GetCurrentUser(fileRecord.OperatorID)
		if err == nil {
			operatorName = operator.Nick
		}
		ca.Data.List = append(ca.Data.List, DataItem{
			ID:       strconv.FormatInt(int64(fileRecord.ID), 10),
			Type:     recordType,
			Operator: operatorName,
			Time:     fileRecord.CreatedAt.Format("2006-01-02 15:04:05"),
			Desc:     fileRecord.Description,
			Status: Status{
				RenderType: "textWithBadge",
				Value:      status,
				Status:     recordState,
			},
			Result: Result{
				RenderType: "downloadUrl",
				URL:        fmt.Sprintf("%s/api/files/%s", conf.RootDomain(), fileRecord.ApiFileUUID),
			},
		})
	}
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.GenComponentState(c); err != nil {
		return err
	}

	bdl := ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.ctxBdl = bdl
	ca.sdk = cputil.SDK(ctx)
	ca.Type = "Table"
	ca.Name = "recordTable"
	columns := make([]Column, 0)
	columns = append(columns, Column{
		DataIndex: "id",
		Title:     "ID",
		Width:     80,
	}, Column{
		DataIndex: "type",
		Title:     ca.sdk.I18n("type"),
		Width:     80,
	}, Column{
		DataIndex: "operator",
		Title:     ca.sdk.I18n("operator"),
		Width:     150,
	}, Column{
		DataIndex: "time",
		Title:     ca.sdk.I18n("time"),
		Width:     170,
	}, Column{
		DataIndex: "desc",
		Title:     ca.sdk.I18n("desc"),
		Width:     200,
	}, Column{
		DataIndex: "status",
		Title:     ca.sdk.I18n("status"),
		Width:     80,
	}, Column{
		DataIndex: "result",
		Title:     ca.sdk.I18n("result"),
	})
	ca.Props = map[string]interface{}{
		"columns": columns,
		"rowKey":  "id",
	}
	if err := ca.setData(); err != nil {
		return err
	}
	ca.State.AutoRefresh = false
	return nil
}

func init() {
	base.InitProviderWithCreator("scenes-import-record", "table", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
