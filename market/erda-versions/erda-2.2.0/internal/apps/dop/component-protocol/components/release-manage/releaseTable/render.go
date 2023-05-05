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

package releaseTable

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	dicehubpb "github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	cmpTypes "github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/release-manage/access"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

func init() {
	base.InitProviderWithCreator("release-manage", "releaseTable", func() servicehub.Provider {
		return &ComponentReleaseTable{}
	})
}

func (r *ComponentReleaseTable) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	r.InitComponent(ctx)
	if err := r.GenComponentState(component); err != nil {
		return errors.Errorf("failed to gen release table component state, %v", err)
	}

	switch event.Operation {
	case cptype.InitializeOperation:
		r.State.PageNo = 1
		r.State.PageSize = 10
		if err := r.DecodeURLQuery(); err != nil {
			return errors.Errorf("failed to deocode url query for release table component, %v", err)
		}
	case cptype.RenderingOperation, "changePageSize", "changeSort":
		r.State.PageNo = 1
	case "formal":
		var selectedIDs []string
		id, err := getReleaseID(event.OperationData)
		if err != nil {
			selectedIDs = r.State.SelectedRowKeys
		} else {
			selectedIDs = append(selectedIDs, id)
		}
		if err = r.formalReleases(ctx, selectedIDs); err != nil {
			return errors.Errorf("%s, %v", r.sdk.I18n("releaseFormalFailed"), err)
		}
	case "delete":
		var selectedIDs []string
		id, err := getReleaseID(event.OperationData)
		if err != nil {
			selectedIDs = r.State.SelectedRowKeys
		} else {
			selectedIDs = append(selectedIDs, id)
		}
		if err = r.deleteReleases(ctx, selectedIDs); err != nil {
			return errors.Errorf("%s, %v", r.sdk.I18n("releaseDeleteFailed"), err)
		}
	case "putOn":
		id, err := getReleaseID(event.OperationData)
		if err != nil {
			return err
		}
		if err = r.putOnRelease(ctx, id); err != nil {
			return err
		}
	case "putOff":
		id, err := getReleaseID(event.OperationData)
		if err != nil {
			return err
		}
		if err = r.putOffRelease(ctx, id); err != nil {
			return err
		}
	}
	logrus.Debugf("[DEBUG] start render table")
	if err := r.RenderTable(ctx, gs); err != nil {
		return err
	}
	logrus.Debugf("[DEBUG] end render table")
	logrus.Debugf("[DEBUG] start encode url query")
	if err := r.EncodeURLQuery(); err != nil {
		return errors.Errorf("failed to encode url query for release table component, %v", err)
	}
	logrus.Debugf("[DEBUG] end encode url query")
	r.SetComponentValue()
	r.Transfer(component)
	return nil
}

func (r *ComponentReleaseTable) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	r.sdk = sdk
	bdl := ctx.Value(cmpTypes.GlobalCtxKeyBundle).(*bundle.Bundle)
	r.bdl = bdl
	svc := ctx.Value(types.DicehubReleaseService).(dicehubpb.ReleaseServiceServer)
	r.svc = svc
}

func (r *ComponentReleaseTable) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}
	var state State
	jsonData, err := json.Marshal(component.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(jsonData, &state); err != nil {
		return err
	}
	r.State = state
	return nil
}

func (r *ComponentReleaseTable) DecodeURLQuery() error {
	urlQuery, ok := r.sdk.InParams["releaseTable__urlQuery"].(string)
	if !ok {
		return nil
	}
	decode, err := base64.StdEncoding.DecodeString(urlQuery)
	if err != nil {
		return err
	}
	query := make(map[string]interface{})
	if err := json.Unmarshal(decode, &query); err != nil {
		return err
	}
	r.State.PageNo = int64(query["pageNo"].(float64))
	r.State.PageSize = int64(query["pageSize"].(float64))
	sorter := query["sorterData"].(map[string]interface{})
	r.State.Sorter.Field, _ = sorter["field"].(string)
	r.State.Sorter.Order, _ = sorter["order"].(string)
	return nil
}

func (r *ComponentReleaseTable) EncodeURLQuery() error {
	urlQuery := make(map[string]interface{})
	urlQuery["pageNo"] = r.State.PageNo
	urlQuery["pageSize"] = r.State.PageSize
	urlQuery["sorterData"] = r.State.Sorter
	jsonData, err := json.Marshal(urlQuery)
	if err != nil {
		return err
	}

	encoded := base64.StdEncoding.EncodeToString(jsonData)
	r.State.ReleaseTableURLQuery = encoded
	return nil
}

func (r *ComponentReleaseTable) RenderTable(ctx context.Context, gs *cptype.GlobalStateData) error {
	userID := r.sdk.Identity.UserID
	orgID := r.sdk.Identity.OrgID
	projectID := r.State.ProjectID
	logrus.Debugf("[DEBUG] start check read access")
	hasReadAccess, err := access.HasReadAccess(r.bdl, userID, uint64(projectID))
	logrus.Debugf("[DEBUG] end check read access")
	if err != nil {
		return errors.Errorf("failed to check access, %v", err)
	}
	if !hasReadAccess {
		return errors.Errorf(r.sdk.I18n("accessDenied"))
	}

	var startTime, endTime int64 = 0, 0
	if len(r.State.FilterValues.CreatedAtStartEnd) == 2 {
		startTime = r.State.FilterValues.CreatedAtStartEnd[0]
		endTime = r.State.FilterValues.CreatedAtStartEnd[1]
	}

	order := "DESC"
	if r.State.Sorter.Order == "ascend" {
		order = "ASC"
	}
	orderBy := ""
	if r.State.Sorter.Field == "createdAt" {
		orderBy = "created_at"
	}

	ctx = metadata.NewOutgoingContext(ctx, metadata.New(map[string]string{
		"internal-client": "true",
		"org-id":          r.sdk.Identity.OrgID,
	}))
	logrus.Debugf("[DEBUG] start list releases")
	isFormal := ""
	if r.State.IsFormal != nil {
		isFormal = strconv.FormatBool(*r.State.IsFormal)
	}
	releaseResp, err := r.svc.ListRelease(ctx, &dicehubpb.ReleaseListRequest{
		ReleaseID:        r.State.FilterValues.ReleaseID,
		Branch:           r.State.FilterValues.BranchID,
		IsLatest:         r.State.FilterValues.Latest == "true" && r.State.IsFormal == nil && !r.State.IsProjectRelease,
		IsStable:         "true",
		IsFormal:         isFormal,
		IsProjectRelease: strconv.FormatBool(r.State.IsProjectRelease),
		UserID:           r.State.FilterValues.UserIDs,
		Query:            r.State.FilterValues.Version,
		CommitID:         r.State.FilterValues.CommitID,
		Tags:             r.State.FilterValues.Tags,
		ApplicationID:    r.State.FilterValues.ApplicationIDs,
		ProjectID:        projectID,
		StartTime:        startTime,
		EndTime:          endTime,
		PageSize:         r.State.PageSize,
		PageNo:           r.State.PageNo,
		OrderBy:          orderBy,
		Order:            order,
	})
	logrus.Debugf("[DEBUG] end list releases")
	if err != nil {
		return errors.Errorf("failed to list releases, %v", err)
	}

	r.State.Total = releaseResp.Data.Total

	logrus.Debugf("[DEBUG] start get org")
	org, err := r.bdl.GetOrg(orgID)
	logrus.Debugf("[DEBUG] end get org")
	if err != nil {
		return errors.Errorf("failed to get org, %v", err)
	}

	// pre check access
	hasWriteAccess := true
	if r.State.IsProjectRelease {
		logrus.Debugf("[DEBUG] start check write access")
		hasWriteAccess, err = access.HasWriteAccess(r.bdl, userID, uint64(projectID), true, 0)
		logrus.Debugf("[DEBUG] end check write access")
		if err != nil {
			return errors.Errorf("failed to check access, %v", err)
		}
	}
	existedUser := make(map[string]struct{})
	var userIDs []string
	var list []Item
	logrus.Debugf("[DEBUG] start release loop")
	for _, release := range releaseResp.Data.List {
		typ := "application"
		if release.IsProjectRelease {
			typ = "project"
		}
		editOperation := Operation{
			Command: Command{
				JumpOut: false,
				Key:     "goto",
				Target: fmt.Sprintf("/%s/dop/projects/%d/release/%s/updateRelease/%s",
					org.Name, r.State.ProjectID, typ, release.ReleaseID),
			},
			Key:         "gotoDetail",
			Reload:      false,
			Text:        r.sdk.I18n("editRelease"),
			Disabled:    !hasWriteAccess,
			DisabledTip: r.sdk.I18n("accessDenied"),
		}
		formalOperation := Operation{
			Confirm: r.sdk.I18n("confirmFormal"),
			Key:     "formal",
			Reload:  true,
			Text:    r.sdk.I18n("toFormal"),
			Meta: map[string]interface{}{
				"id": release.ReleaseID,
			},
			SuccessMsg:  r.sdk.I18n("formalSucceeded"),
			Disabled:    !hasWriteAccess,
			DisabledTip: r.sdk.I18n("accessDenied"),
		}
		deleteOperation := Operation{
			Confirm: r.sdk.I18n("confirmDelete"),
			Key:     "delete",
			Reload:  true,
			Text:    r.sdk.I18n("deleteRelease"),
			Meta: map[string]interface{}{
				"id": release.ReleaseID,
			},
			SuccessMsg:  r.sdk.I18n("deleteSucceeded"),
			Disabled:    !hasWriteAccess,
			DisabledTip: r.sdk.I18n("accessDenied"),
		}

		downloadPath := fmt.Sprintf("/api/%s/releases/%s/actions/download", org.Name, release.ReleaseID)
		downloadOperation := Operation{
			Command: Command{
				JumpOut: true,
				Key:     "goto",
				Target:  downloadPath,
			},
			Key:    "download",
			Reload: false,
			Text:   r.sdk.I18n("downloadDice"),
		}

		if _, ok := existedUser[release.UserID]; !ok && release.UserID != "" {
			existedUser[release.UserID] = struct{}{}
			userIDs = append(userIDs, release.UserID)
		}

		var tags []TagValue
		for i := range release.Tags {
			tags = append(tags, TagValue{
				Label: release.Tags[i].Name,
				Color: release.Tags[i].Color,
			})
		}

		item := Item{
			ID: release.ReleaseID,
			Version: DoubleRowWithIcon{
				RenderType: "doubleRowWithIcon",
				Value:      release.Version,
				ExtraContent: ExtraContent{
					RenderType: "tags",
					ShowCount:  4,
					Value:      tags,
				},
			},
			Application: release.ApplicationName,
			Creator: Creator{
				RenderType: "userAvatar",
				Value:      []string{release.UserID},
			},
			CreatedAt: release.CreatedAt.AsTime().Local().Format("2006/01/02 15:04:05"),
			Operations: TableOperations{
				Operations: map[string]interface{}{},
				RenderType: "tableOperation",
			},
		}

		if release.IsFormal && r.State.IsFormal == nil {
			item.Formal = &TagsRow{
				RenderType: "tagsRow",
				Size:       "normal",
				Value: TagValue{
					Label: r.sdk.I18n("formal"),
					Color: "blue",
				},
			}
		}

		if release.IsProjectRelease {
			item.Operations.Operations["download"] = downloadOperation

			models := make(map[string]apistructs.ReleaseDeployMode)
			if err := json.Unmarshal([]byte(release.Modes), &models); err != nil {
				logrus.Errorf("failed to unmarshal application release list for release %s, %v", release.ReleaseID, err)
			}
			var list []string
			for _, model := range models {
				for i := 0; i < len(model.ApplicationReleaseList); i++ {
					list = append(list, model.ApplicationReleaseList[i]...)
				}
			}
			item.Operations.Operations["referencedReleases"] = Operation{
				Meta: map[string]interface{}{
					"releaseID": strings.Join(list, ","),
					"latest":    "",
				},
				Key:  "referencedReleases",
				Text: r.sdk.I18n("referencedReleases"),
			}

			if release.OpusID == "" {
				item.Operations.Operations["putOn"] = Operation{
					Confirm: r.sdk.I18n("confirmPutOn"),
					Key:     "putOn",
					Reload:  true,
					Text:    r.sdk.I18n("putOn"),
					Meta: map[string]interface{}{
						"id": release.ReleaseID,
					},
					SuccessMsg: r.sdk.I18n("putOnSucceeded"),
				}
			} else {
				item.Operations.Operations["putOff"] = Operation{
					Confirm: r.sdk.I18n("confirmPutOff"),
					Key:     "putOff",
					Reload:  true,
					Text:    r.sdk.I18n("putOff"),
					Meta: map[string]interface{}{
						"id": release.ReleaseID,
					},
					SuccessMsg: r.sdk.I18n("putOffSucceeded"),
				}
			}
		}
		if !release.IsFormal {
			if hasWriteAccess {
				item.BatchOperations = []string{"formal", "delete"}
			}
		} else {
			editOperation.Disabled = true
			editOperation.DisabledTip = r.sdk.I18n("formalReleaseCanNotBeModified")
			formalOperation.Disabled = true
			formalOperation.DisabledTip = r.sdk.I18n("formalReleaseCanNotBeModified")
			deleteOperation.Disabled = true
			deleteOperation.DisabledTip = r.sdk.I18n("formalReleaseCanNotBeModified")
		}
		if release.IsProjectRelease && release.OpusID != "" {
			deleteOperation.Disabled = true
			deleteOperation.DisabledTip = r.sdk.I18n("canNotDeletePutOnRelease")
		}
		item.Operations.Operations["edit"] = editOperation
		item.Operations.Operations["formal"] = formalOperation
		item.Operations.Operations["delete"] = deleteOperation

		list = append(list, item)
	}
	logrus.Debugf("[DEBUG] end release loop")

	r.Data.List = list

	if gs == nil {
		gsd := make(cptype.GlobalStateData)
		gs = &gsd
	}
	r.sdk.GlobalState = gs
	logrus.Debugf("[DEBUG] start set userIDs")
	r.sdk.SetUserIDs(userIDs)
	logrus.Debugf("[DEBUG] end set userIDs")
	return nil
}

func (r *ComponentReleaseTable) SetComponentValue() {
	r.Operations = map[string]interface{}{
		"changePageNo": Operation{
			Key:    "changePageNo",
			Reload: true,
		},
		"changePageSize": Operation{
			Key:    "changePageSize",
			Reload: true,
		},
		"formal": Operation{
			Key:        "formal",
			Reload:     true,
			Text:       r.sdk.I18n("toFormal"),
			Confirm:    r.sdk.I18n("confirmFormal"),
			SuccessMsg: r.sdk.I18n("formalSucceeded"),
		},
		"delete": Operation{
			Key:        "delete",
			Reload:     true,
			Text:       r.sdk.I18n("deleteRelease"),
			Confirm:    r.sdk.I18n("confirmDelete"),
			SuccessMsg: r.sdk.I18n("deleteSucceeded"),
		},
		"changeSort": Operation{
			Key:    "changeSort",
			Reload: true,
		},
		"putOn": Operation{
			Key:        "putOn",
			Reload:     true,
			Text:       r.sdk.I18n("putOn"),
			Confirm:    r.sdk.I18n("confirmPutOn"),
			SuccessMsg: r.sdk.I18n("putOnSucceeded"),
		},
		"putOff": Operation{
			Key:        "putOff",
			Reload:     true,
			Text:       r.sdk.I18n("putOff"),
			Confirm:    r.sdk.I18n("confirmPutOff"),
			SuccessMsg: r.sdk.I18n("putOffSucceeded"),
		},
	}

	var batchOperations []string
	if r.State.IsFormal != nil && !*r.State.IsFormal {
		batchOperations = []string{"formal", "delete"}
	}

	columns := []Column{
		{
			DataIndex: "version",
			Title:     r.sdk.I18n("version"),
		},
		{
			DataIndex: "application",
			Title:     r.sdk.I18n("application"),
		},
		{
			DataIndex: "creator",
			Title:     r.sdk.I18n("creator"),
		},
		{
			DataIndex: "createdAt",
			Title:     r.sdk.I18n("createdAt"),
			Sorter:    true,
		},
	}

	// 项目制品、全部应用制品、非正式应用制品需要有操作列
	if r.State.IsProjectRelease || r.State.IsFormal == nil || !*r.State.IsFormal {
		columns = append(columns, Column{
			DataIndex: "operations",
			Title:     r.sdk.I18n("operations"),
			Align:     "right",
		})
	}

	// 项目制品不需要应用列
	if r.State.IsProjectRelease {
		columns = append(columns[:1], columns[2:]...)
	}

	// 全部页面显示是否为正式版
	if r.State.IsFormal == nil {
		columns = append(columns[:1], append([]Column{{
			DataIndex: "formal",
			Title:     "",
		}}, columns[1:]...)...)
	}

	// 最后一列右对齐
	columns[len(columns)-1].Align = "right"

	r.Props = Props{
		RequestIgnore:   []string{"data"},
		BatchOperations: batchOperations,
		Selectable:      r.State.IsFormal != nil && !*r.State.IsFormal,
		Columns:         columns,
		PageSizeOptions: []string{"10", "20", "50", "100"},
		RowKey:          "id",
	}
}

func (r *ComponentReleaseTable) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(r.Props)
	c.Data = map[string]interface{}{
		"list": r.Data.List,
	}
	c.State = map[string]interface{}{
		"releaseTable__urlQuery": r.State.ReleaseTableURLQuery,
		"pageNo":                 r.State.PageNo,
		"pageSize":               r.State.PageSize,
		"total":                  r.State.Total,
		"selectedRowKeys":        r.State.SelectedRowKeys,
		"sorterData":             r.State.Sorter,
		"isProjectRelease":       r.State.IsProjectRelease,
		"projectID":              r.State.ProjectID,
		"isFormal":               r.State.IsFormal,
		"applicationID":          r.State.ApplicationID,
		"filterValues":           r.State.FilterValues,
	}
	c.Operations = r.Operations
}

func (r *ComponentReleaseTable) formalReleases(ctx context.Context, releaseID []string) error {
	userID := r.sdk.Identity.UserID
	projectID := r.State.ProjectID

	ctx = metadata.NewOutgoingContext(ctx, metadata.New(map[string]string{
		"internal-client": "true",
		"org-id":          r.sdk.Identity.OrgID,
		"user-id":         userID,
	}))

	if r.State.IsProjectRelease {
		hasAccess, err := access.HasWriteAccess(r.bdl, userID, uint64(projectID), true, 0)
		if err != nil {
			return errors.Errorf("failed to check access, %v", err)
		}
		if !hasAccess {
			return errors.Errorf(r.sdk.I18n("accessDenied"))
		}
	} else {
		for _, id := range releaseID {
			resp, err := r.svc.GetRelease(ctx, &dicehubpb.ReleaseGetRequest{ReleaseID: id})
			if err != nil {
				return err
			}
			hasAccess, err := access.HasWriteAccess(r.bdl, userID, uint64(projectID), false, resp.Data.ApplicationID)
			if err != nil {
				return errors.Errorf("failed to check access, %v", err)
			}
			if !hasAccess {
				return errors.Errorf(r.sdk.I18n("accessDenied"))
			}
		}
	}

	_, err := r.svc.ToFormalReleases(ctx, &dicehubpb.FormalReleasesRequest{
		ProjectId: projectID,
		ReleaseId: releaseID,
	})
	return err
}

func (r *ComponentReleaseTable) deleteReleases(ctx context.Context, releaseID []string) error {
	userID := r.sdk.Identity.UserID
	projectID := r.State.ProjectID

	ctx = metadata.NewOutgoingContext(ctx, metadata.New(map[string]string{
		"internal-client": "true",
		"org-id":          r.sdk.Identity.OrgID,
		"user-id":         userID,
	}))

	if r.State.IsProjectRelease {
		hasAccess, err := access.HasWriteAccess(r.bdl, userID, uint64(projectID), true, 0)
		if err != nil {
			return errors.Errorf("failed to check access, %v", err)
		}
		if !hasAccess {
			return errors.Errorf(r.sdk.I18n("accessDenied"))
		}
	} else {
		for _, id := range releaseID {
			resp, err := r.svc.GetRelease(ctx, &dicehubpb.ReleaseGetRequest{ReleaseID: id})
			if err != nil {
				return err
			}
			hasAccess, err := access.HasWriteAccess(r.bdl, userID, uint64(projectID), false, resp.Data.ApplicationID)
			if err != nil {
				return errors.Errorf("failed to check access, %v", err)
			}
			if !hasAccess {
				return errors.Errorf(r.sdk.I18n("accessDenied"))
			}
		}
	}

	_, err := r.svc.DeleteReleases(ctx, &dicehubpb.ReleasesDeleteRequest{
		ProjectId: projectID,
		ReleaseId: releaseID,
	})
	return err
}

func getReleaseID(operationData map[string]interface{}) (string, error) {
	meta, ok := operationData["meta"].(map[string]interface{})
	if !ok {
		return "", errors.New("invalid meta in event.operationData")
	}
	id, ok := meta["id"].(string)
	if !ok {
		return "", errors.New("invalid release id in event.operationData")
	}
	return id, nil
}
