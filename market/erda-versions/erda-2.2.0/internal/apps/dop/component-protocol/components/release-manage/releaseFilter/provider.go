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

package releaseFilter

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/bundle/apierrors"
	cmpTypes "github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

type ReleaseFilter struct {
	impl.DefaultFilter

	sdk *cptype.SDK
	bdl *bundle.Bundle

	State State `json:"state"`
}

type State struct {
	Values                Values `json:"values"`
	ReleaseFilterURLQuery string `json:"releaseFilter__urlQuery,omitempty"`
	IsProjectRelease      bool   `json:"isProjectRelease"`
	ProjectID             int64  `json:"projectID"`
	IsFormal              *bool  `json:"isFormal,omitempty"`
}

type Values struct {
	ApplicationIDs    []string `json:"applicationIDs,omitempty"`
	BranchID          string   `json:"branchID,omitempty"`
	CommitID          string   `json:"commitID,omitempty"`
	CreatedAtStartEnd []int64  `json:"createdAtStartEnd,omitempty"`
	Latest            string   `json:"latest,omitempty"`
	ReleaseID         string   `json:"releaseID,omitempty"`
	UserIDs           []string `json:"userIDs,omitempty"`
	Version           string   `json:"version,omitempty"`
	Tags              []uint64 `json:"tags,omitempty"`
}

type Condition struct {
	Key         string   `json:"key,omitempty"`
	Label       string   `json:"label,omitempty"`
	Placeholder string   `json:"placeholder,omitempty"`
	Type        string   `json:"type,omitempty"`
	Options     []Option `json:"options,omitempty"`
	Outside     bool     `json:"outside,omitempty"`
	Mode        string   `json:"mode,omitempty"`
	Required    bool     `json:"required"`
}

type Option struct {
	Label string      `json:"label,omitempty"`
	Value interface{} `json:"value,omitempty"`
	Color string      `json:"color,omitempty"`
}

func init() {
	base.InitProviderWithCreator("release-manage", "releaseFilter", func() servicehub.Provider {
		return &ReleaseFilter{}
	})
}

func (f *ReleaseFilter) BeforeHandleOp(sdk *cptype.SDK) {
	f.sdk = sdk
	bdl := sdk.Ctx.Value(cmpTypes.GlobalCtxKeyBundle).(*bundle.Bundle)
	f.bdl = bdl
	cputil.MustObjJSONTransfer(&f.StdStatePtr, &f.State)
}

func (f *ReleaseFilter) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		f.State.Values.Latest = "true"
		if err := f.renderFilter(); err != nil {
			panic(err)
		}
		return nil
	}
}

func (f *ReleaseFilter) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		if err := f.renderFilter(); err != nil {
			panic(err)
		}
		return nil
	}
}

func (f *ReleaseFilter) AfterHandleOp(_ *cptype.SDK) {
	if err := f.encodeURLQuery(); err != nil {
		panic(err)
	}
	cputil.MustObjJSONTransfer(&f.State, &f.StdStatePtr)
}

func (f *ReleaseFilter) RegisterFilterOp(_ filter.OpFilter) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (f *ReleaseFilter) RegisterFilterItemSaveOp(_ filter.OpFilterItemSave) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (f *ReleaseFilter) RegisterFilterItemDeleteOp(_ filter.OpFilterItemDelete) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (f *ReleaseFilter) renderFilter() error {
	if err := f.decodeURLQuery(); err != nil {
		return err
	}

	f.StdDataPtr.HideSave = true
	userID := f.sdk.Identity.UserID
	orgIDStr := f.sdk.Identity.OrgID
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrInvoke.InvalidParameter(fmt.Errorf("invalid org id %s, %v", orgIDStr, err))
	}

	if !f.State.IsProjectRelease {
		appResp, err := f.bdl.GetAppsByProject(uint64(f.State.ProjectID), orgID, userID)
		if err != nil {
			return errors.Errorf("failed to list apps, %v", err)
		}
		appCondition := Condition{
			Key:         "applicationIDs",
			Label:       f.sdk.I18n("application"),
			Placeholder: f.sdk.I18n("selectApplication"),
			Type:        "select",
		}
		var appOptions []Option
		for i := range appResp.List {
			name := appResp.List[i].DisplayName
			id := appResp.List[i].ID
			appOptions = append(appOptions, Option{
				Label: name,
				Value: strconv.FormatInt(int64(id), 10),
			})
		}
		appCondition.Options = appOptions
		f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, appCondition)
		f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, Condition{
			Key:         "branchID",
			Label:       f.sdk.I18n("branch"),
			Placeholder: f.sdk.I18n("inputBranch"),
			Type:        "input",
		})
		f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, Condition{
			Key:         "commitID",
			Label:       "commitID",
			Placeholder: f.sdk.I18n("inputCommitID"),
			Type:        "input",
		})
		f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, Condition{
			Key:         "releaseID",
			Label:       f.sdk.I18n("releaseID"),
			Placeholder: f.sdk.I18n("inputReleaseID"),
			Type:        "input",
		})
	}

	userCondition := Condition{
		Key:         "userIDs",
		Label:       f.sdk.I18n("creator"),
		Placeholder: f.sdk.I18n("selectCreator"),
		Type:        "select",
	}
	var userOptions []Option
	usersResp, err := f.bdl.ListMembers(apistructs.MemberListRequest{
		ScopeType: apistructs.ProjectScope,
		ScopeID:   f.State.ProjectID,
		PageNo:    1,
		PageSize:  500,
	})
	if err != nil {
		return errors.Errorf("failed to list user, %v", err)
	}

	for i := range usersResp {
		userOptions = append(userOptions, Option{
			Label: usersResp[i].Nick,
			Value: usersResp[i].UserID,
		})
	}
	userCondition.Options = userOptions
	f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, userCondition)

	tags, err := f.bdl.ListLabel(apistructs.ProjectLabelListRequest{
		ProjectID: uint64(f.State.ProjectID),
		Type:      apistructs.LabelTypeRelease,
		PageNo:    1,
		PageSize:  1000,
	})
	var tagOptions []Option
	for i := range tags.List {
		tagOptions = append(tagOptions, Option{
			Label: tags.List[i].Name,
			Value: tags.List[i].ID,
			Color: tags.List[i].Color,
		})
	}
	tagCondition := Condition{
		Key:         "tags",
		Label:       f.sdk.I18n("tag"),
		Placeholder: f.sdk.I18n("selectTag"),
		Type:        "tagsSelect",
		Options:     tagOptions,
	}
	f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, tagCondition)

	if f.State.IsFormal == nil && !f.State.IsProjectRelease {
		f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, Condition{
			Key:   "latest",
			Label: f.sdk.I18n("aggregateByBranch"),
			Type:  "select",
			Options: []Option{
				{
					Label: f.sdk.I18n("true"),
					Value: "true",
				},
			},
			Mode: "single",
		})
	}
	f.StdDataPtr.Conditions = append(f.StdDataPtr.Conditions, Condition{
		Key:         "version",
		Label:       "version",
		Placeholder: f.sdk.I18n("searchByVersionOrID"),
		Type:        "input",
		Outside:     true,
	})
	return nil
}

func (f *ReleaseFilter) decodeURLQuery() error {
	urlQuery, ok := f.sdk.InParams["releaseFilter__urlQuery"].(string)
	if !ok {
		return nil
	}
	decoded, err := base64.StdEncoding.DecodeString(urlQuery)
	if err != nil {
		return err
	}
	return json.Unmarshal(decoded, &f.State.Values)
}

func (f *ReleaseFilter) encodeURLQuery() error {
	data, err := json.Marshal(f.State.Values)
	if err != nil {
		return err
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	f.State.ReleaseFilterURLQuery = encoded
	return nil
}
