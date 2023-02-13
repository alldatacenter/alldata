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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

var (
	defaultPageNo   = 1
	defaultPageSize = 15
)

type ComponentAction struct {
	sdk    *cptype.SDK
	ctxBdl *bundle.Bundle

	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	State      State                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
	Data       Data                   `json:"data"`
	InParams   InParams               `json:"-"`
}

type InParams struct {
	ProjectID uint64 `json:"projectId,omitempty"`
}

type State struct {
	PageNo     uint64     `json:"pageNo"`
	PageSize   uint64     `json:"pageSize"`
	Total      uint64     `json:"total"`
	SorterData SorterData `json:"sorterData"`
	Values     struct {
		Type      string   `json:"type"`
		Name      string   `json:"name"`
		Iteration []uint64 `json:"iteration"`
	} `json:"values"`
}

type Data struct {
	List []DataItem `json:"list"`
}

type DataItem struct {
	ID         uint64                 `json:"id"`
	Name       string                 `json:"name"`
	Iteration  string                 `json:"iteration"`
	Creator    string                 `json:"creator"`
	Quality    string                 `json:"quality"`
	CreateTime string                 `json:"createTime"`
	Operate    map[string]interface{} `json:"operate"`
}

type SorterData struct {
	Field string `json:"field"`
	Order string `json:"order"`
}

func (i *ComponentAction) setInParams(ctx context.Context) error {
	b, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &i.InParams); err != nil {
		return err
	}
	return err
}

func (i *ComponentAction) GenComponentState(c *cptype.Component) error {
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
	i.State = state
	return nil
}

func (ca *ComponentAction) setData(ctx context.Context) error {
	var req apistructs.TestReportRecordListRequest
	req.UserID = ca.sdk.Identity.UserID
	req.ProjectID = ca.InParams.ProjectID
	if ca.State.PageNo == 0 {
		ca.State.PageNo = uint64(defaultPageNo)
	}
	if ca.State.PageSize == 0 {
		ca.State.PageSize = uint64(defaultPageSize)
	}
	req.PageNo = ca.State.PageNo
	req.PageSize = ca.State.PageSize
	if ca.State.SorterData.Field == "createTime" {
		req.OrderBy = "createdAt"
	}
	if ca.State.SorterData.Field == "quality" {
		req.OrderBy = "qualityScore"
	}
	if ca.State.SorterData.Order == "ascend" {
		req.Asc = true
	}
	if len(ca.State.Values.Iteration) > 0 {
		req.IterationIDS = ca.State.Values.Iteration
	}
	if ca.State.Values.Name != "" {
		req.Name = ca.State.Values.Name
	}
	rsp, err := ca.ctxBdl.ListTestReportRecord(req)
	if err != nil {
		return err
	}
	ca.State.Total = rsp.Data.Total
	ca.Data = Data{
		List: make([]DataItem, 0),
	}
	for _, record := range rsp.Data.List {
		creator, err := ca.ctxBdl.GetCurrentUser(record.CreatorID)
		if err != nil {
			return err
		}
		iteration, err := ca.ctxBdl.GetIteration(record.IterationID)
		if err != nil {
			return err
		}
		item := DataItem{
			ID:         record.ID,
			Name:       record.Name,
			Iteration:  iteration.Title,
			Creator:    creator.Nick,
			Quality:    strconv.FormatFloat(record.QualityScore, 'f', -1, 64),
			CreateTime: record.CreatedAt.Format("2006-01-02 15:04:05"),
			Operate: map[string]interface{}{
				"operations": map[string]interface{}{
					"download": map[string]interface{}{
						"key": "download",
						"meta": map[string]interface{}{
							"reportId": record.ID,
						},
						"reload": false,
						"text":   cputil.I18n(ctx, "download"),
					},
				},
				"renderType": "tableOperation",
			},
		}
		ca.Data.List = append(ca.Data.List, item)
	}
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.GenComponentState(c); err != nil {
		return err
	}
	if err := ca.setInParams(ctx); err != nil {
		return err
	}
	bdl := ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.ctxBdl = bdl
	ca.sdk = cputil.SDK(ctx)
	ca.Name = "table"
	ca.Type = "Table"

	ca.Operations = map[string]interface{}{
		"changePageNo": map[string]interface{}{
			"key":    "changePageNo",
			"reload": true,
		},
		"changeSort": map[string]interface{}{
			"key":    "changeSort",
			"reload": true,
		},
	}
	ca.Props = map[string]interface{}{
		"columns": []interface{}{
			map[string]interface{}{
				"dataIndex": "name",
				"title":     cputil.I18n(ctx, "testReportName"),
			},
			map[string]interface{}{
				"dataIndex": "iteration",
				"title":     cputil.I18n(ctx, "belongIteration"),
			},
			map[string]interface{}{
				"dataIndex": "creator",
				"title":     cputil.I18n(ctx, "creator"),
			},
			map[string]interface{}{
				"dataIndex": "quality",
				"sorter":    true,
				"title":     cputil.I18n(ctx, "qualityScore"),
			},
			map[string]interface{}{
				"dataIndex": "createTime",
				"sorter":    true,
				"title":     cputil.I18n(ctx, "creationTime"),
			},
			map[string]interface{}{
				"dataIndex": "operate",
				"title":     cputil.I18n(ctx, "operations"),
				"fixed":     "right",
				"width":     120,
			},
		},
		"rowKey":          "id",
		"pageSizeOptions": []interface{}{"10", "20", "50", "100"},
	}
	if err := ca.setData(ctx); err != nil {
		return err
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("test-report", "table", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
