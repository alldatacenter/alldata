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

package spaceList

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/i18n"
	text "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	spec "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/component_spec/table"
)

type ComponentSpaceList struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	State state                  `json:"state"`
	Props spec.Props             `json:"props"`
	Data  map[string]interface{} `json:"data"`
}

type state struct {
	Total    int64                   `json:"total"`
	PageSize int64                   `json:"pageSize"`
	PageNo   int64                   `json:"pageNo"`
	Values   common.FilterConditions `json:"values,omitempty"`
}

type props struct {
	RowKey  string    `json:"rowKey,omitempty"`
	Columns []columns `json:"columns,omitempty"`
}

type spaceList struct {
	ID      uint64                 `json:"id"`
	Name    string                 `json:"name"`
	Desc    string                 `json:"desc"`
	Operate dataTask               `json:"operate"`
	Status  map[string]interface{} `json:"status"`
}

type spaceItem struct {
	ID            uint64                 `json:"id"`
	Title         string                 `json:"title"`
	Description   string                 `json:"description"`
	PrefixImg     string                 `json:"prefixImg"`
	ArchiveStatus ArchiveStatus          `json:"status"`
	ExtraInfos    []ExtraInfos           `json:"extraInfos"`
	Operations    map[string]interface{} `json:"operations"`
}

type ArchiveStatus struct {
	Status string `json:"status"`
	Text   string `json:"text"`
}

type ExtraInfos struct {
	Icon    string `json:"icon,omitempty"`
	Text    string `json:"text,omitempty"`
	Tooltip string `json:"tooltip,omitempty"`
	Type    string `json:"type,omitempty"`
}

// apistructs.AutoTestSpaceStatus
type columns struct {
	Title     string `json:"title"`
	DataIndex string `json:"dataIndex"`
	Width     int    `json:"width,omitempty"`
}

type inParams struct {
	ProjectID int64 `json:"projectId"`
}

type RenderType string

var (
	RenderTable RenderType = "tableOperation"
)

type dataTask struct {
	RenderType RenderType             `json:"renderType"`
	Value      string                 `json:"value"`
	Operations map[string]interface{} `json:"operations"`
}

func (a *ComponentSpaceList) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	a.sdk = cputil.SDK(ctx)
	a.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	inParamsBytes, err := json.Marshal(a.sdk.InParams)
	if err != nil {
		return fmt.Errorf("failed to marshal inParams, inParams:%+v, err:%v", a.sdk.InParams, err)
	}

	var inParams inParams
	if err := json.Unmarshal(inParamsBytes, &inParams); err != nil {
		return err
	}
	switch apistructs.OperationKey(event.Operation) {
	case apistructs.AutoTestSpaceChangePageNoOperationKey, apistructs.AutoTestSpaceChangePageSizeOperationKey, apistructs.InitializeOperation, apistructs.RenderingOperation:
		if err := a.handlerListOperation(c, inParams, event); err != nil {
			return err
		}
	case apistructs.AutoTestSpaceDeleteOperationKey:
		if err := a.handlerDeleteOperation(c, inParams, event); err != nil {
			return err
		}
	case apistructs.AutoTestSpaceCopyOperationKey:
		if err := a.handlerCopyOperation(c, inParams, event); err != nil {
			return err
		}
	case apistructs.AutoTestSpaceRetryOperationKey:
		if err := a.handlerRetryOperation(c, inParams, event); err != nil {
			return err
		}
	case apistructs.AutoTestSpaceExportOperationKey:
		if err := a.handlerExportOperation(c, inParams, event); err != nil {
			return err
		}
	}
	c.Operations = getOperations()
	a.Props = getProps()
	return nil
}

func getStatus(req apistructs.AutoTestSpaceStatus) map[string]interface{} {
	res := map[string]interface{}{"renderType": "textWithBadge"}
	if req == apistructs.TestSpaceFailed {
		res["status"] = "error"
		res["value"] = "失败"
	}
	if req == apistructs.TestSpaceCopying {
		res["status"] = "processing"
		res["value"] = "复制中"
	}
	return res
}

func (a *ComponentSpaceList) setData(projectID int64, spaces apistructs.AutoTestSpaceList, statsMap map[uint64]*apistructs.AutoTestSpaceStats) error {
	access, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   a.sdk.Identity.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(projectID),
		Resource: apistructs.TestSpaceResource,
		Action:   apistructs.CreateAction,
	})
	if err != nil {
		return err
	}
	lists := []spaceItem{}
	for _, each := range spaces.List {
		var (
			edit = dataOperation{
				Key:         "edit",
				Reload:      false,
				Text:        a.sdk.I18n(i18n.I18nKeyEdit),
				Command:     map[string]interface{}{},
				Disabled:    false,
				ShowIndex:   1,
				DisabledTip: a.sdk.I18n(i18n.I18nKeyNoPermission),
			}
			copyOp = dataOperation{
				Key:         "copy",
				Reload:      true,
				Text:        a.sdk.I18n(i18n.I18nKeyCopy),
				Confirm:     a.sdk.I18n(i18n.I18nKeyCopyConfirm),
				Meta:        map[string]interface{}{},
				Disabled:    true,
				ShowIndex:   2,
				DisabledTip: a.sdk.I18n(i18n.I18nKeyNoPermission),
			}
			export = dataOperation{
				Key:         "export",
				Reload:      true,
				Text:        a.sdk.I18n(i18n.I18nKeyExport),
				Confirm:     a.sdk.I18n(i18n.I18nKeyExportConfirm),
				Meta:        map[string]interface{}{},
				Disabled:    false,
				SuccessMsg:  a.sdk.I18n(i18n.I18nKeyExportSuccessMsg),
				ShowIndex:   3,
				DisabledTip: a.sdk.I18n(i18n.I18nKeyNoPermission),
			}
			deleteOp = dataOperation{
				Key:         "delete",
				Reload:      true,
				Text:        a.sdk.I18n(i18n.I18nKeyDelete),
				Confirm:     a.sdk.I18n(i18n.I18nKeyDeleteConfirm),
				Meta:        map[string]interface{}{},
				DisabledTip: a.sdk.I18n(i18n.I18nKeyDeleteDisabledTip),
				Disabled:    true,
				ShowIndex:   4,
			}
			retry = dataOperation{
				Key:       "retry",
				Reload:    true,
				Text:      a.sdk.I18n(i18n.I18nKeyRetry),
				Meta:      map[string]interface{}{},
				Disabled:  false,
				ShowIndex: 5,
			}
			click = dataOperation{
				Key:    "click",
				Reload: false,
				Command: map[string]interface{}{
					"key":    "goto",
					"target": "project_test_spaceDetail_scenes",
				},
			}
		)
		updatedAt := each.UpdatedAt.Format("2006-01-02 15:04:05")
		text := text.UpdatedTime(a.sdk.Ctx, each.UpdatedAt)
		item := spaceItem{
			ID:          each.ID,
			Title:       each.Name,
			Description: each.Description,
			PrefixImg:   "default_test_case",
			ArchiveStatus: ArchiveStatus{
				Status: each.ArchiveStatus.GetFrontEndStatus(),
				Text:   a.sdk.I18n(fmt.Sprintf("autoTestSpace%s", each.ArchiveStatus)),
			},
			Operations: map[string]interface{}{},
			ExtraInfos: []ExtraInfos{
				{
					Text: fmt.Sprintf("%s: %v", a.sdk.I18n("sceneset"), statsMap[each.ID].SetNum),
				},
				{
					Text: fmt.Sprintf("%s: %v", a.sdk.I18n("auto-test-scene-num"), statsMap[each.ID].SceneNum),
				},
				{
					Text: fmt.Sprintf("%s: %v", a.sdk.I18n("auto-test-api-num"), statsMap[each.ID].StepNum),
				},
				{
					Text:    a.sdk.I18n("updatedAt") + " " + text,
					Tooltip: updatedAt,
				},
			},
		}
		if each.Status == apistructs.TestSpaceFailed {
			edit.Command = setCommand(item, each.ArchiveStatus)
			edit.Disabled = true
			item.Operations["a-edit"] = edit
			deleteOp.Meta = setMeta(item)
			deleteOp.Disabled = false
			item.Operations["delete"] = deleteOp
			retry.Meta = setMeta(item)
			export.Meta = setMeta(item)
			export.Disabled = true
			item.Operations["export"] = export
			item.Operations["retry"] = retry
		} else {
			edit.Command = setCommand(item, each.ArchiveStatus)
			copyOp.Meta = setMeta(item)
			deleteOp.Meta = setMeta(item)
			deleteOp.Disabled = true
			copyOp.Disabled = true
			edit.Disabled = true
			export.Disabled = true
			if each.Status == apistructs.TestSpaceOpen && access.Access {
				edit.Disabled = false
				copyOp.Disabled = false
				export.Disabled = false
			}
			item.Operations["a-edit"] = edit
			item.Operations["copy"] = copyOp
			export.Meta = setMeta(item)
			item.Operations["export"] = export
			item.Operations["delete"] = deleteOp
		}

		status := getStatus(each.Status)
		if v, ok := status["value"]; ok {
			item.ExtraInfos = append(item.ExtraInfos, ExtraInfos{
				Text: v.(string),
			})
		}
		item.Operations["click"] = click
		lists = append(lists, item)
	}
	a.Data = make(map[string]interface{})
	a.Data["list"] = lists

	return nil
}

func getOperations() map[string]interface{} {
	return map[string]interface{}{
		"changePageNo": ListSpaceOperation{
			Key:    "changePageNo",
			Reload: true,
		},
		"changePageSize": ListSpaceOperation{
			Key:    "changePageSize",
			Reload: true,
		},
		"clickRow": ClickRowOperation{
			Key:    "clickRow",
			Reload: false,
			Command: ClickRowOperationCommand{
				Key:     "goto",
				State:   map[string]interface{}{},
				Target:  "project_test_spaceDetail_scenes",
				JumpOut: false,
			},
		},
	}
}

func getProps() spec.Props {
	return spec.Props{
		PageSizeOptions: []string{"10", "20", "50", "100"},
		Visible:         true,
	}
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "spaceList",
		func() servicehub.Provider { return &ComponentSpaceList{} })
}
