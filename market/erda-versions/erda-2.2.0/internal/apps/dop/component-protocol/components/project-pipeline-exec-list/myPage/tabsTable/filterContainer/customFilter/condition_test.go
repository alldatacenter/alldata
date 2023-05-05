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

package customFilter

import (
	"context"
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"

	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/protobuf/proto-go/cp/pb"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline-exec-list/common/gshelper"
)

func TestCustomFilter_AppConditionWithInParamsAppID(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetApp", func(bdl *bundle.Bundle, id uint64) (*apistructs.ApplicationDTO, error) {
		if id == 1 {
			return &apistructs.ApplicationDTO{
				ID:   1,
				Name: "erda",
			}, nil
		}
		return nil, fmt.Errorf("the app is not found")
	})

	sdk := &cptype.SDK{
		Tran: &MockTran{},
	}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, sdk)
	gsHelper := gshelper.NewGSHelper(&cptype.GlobalStateData{})

	type fields struct {
		bdl      *bundle.Bundle
		InParams *InParams
		sdk      *cptype.SDK
		Tran     i18n.Translator
		gsHelper *gshelper.GSHelper
	}
	tests := []struct {
		name    string
		fields  fields
		want    *model.SelectCondition
		wantErr bool
	}{
		{
			name: "test with error",
			fields: fields{
				bdl: bdl,
				InParams: &InParams{
					AppIDInt: 2,
				},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
				gsHelper: gsHelper,
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "test with correct",
			fields: fields{
				bdl: bdl,
				InParams: &InParams{
					AppIDInt: 1,
				},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
				gsHelper: gsHelper,
			},
			want: &model.SelectCondition{
				ConditionBase: model.ConditionBase{
					Key:         "appList",
					Label:       "i18n:application",
					Type:        "select",
					Placeholder: "i18n:please-choose-application",
					Disabled:    true,
					Outside:     false,
				},
				Mode: "",
				Options: []model.SelectOption{
					{
						Label: "erda",
						Value: uint64(1),
						Fix:   false,
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &CustomFilter{
				bdl:      tt.fields.bdl,
				InParams: tt.fields.InParams,
				sdk:      tt.fields.sdk,
				gsHelper: tt.fields.gsHelper,
			}
			got, err := p.AppConditionWithInParamsAppID()
			if (err != nil) != tt.wantErr {
				t.Errorf("AppConditionWithInParamsAppID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("AppConditionWithInParamsAppID() got = %v, want %+v", got, tt.want)
			}
		})
	}
}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return "i18n:" + key
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return "i18n:" + key
}

func TestCustomFilter_ConditionRetriever(t *testing.T) {
	sdk := &cptype.SDK{
		Tran: &MockTran{},
	}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, sdk)
	gsHelper := gshelper.NewGSHelper(&cptype.GlobalStateData{})

	type fields struct {
		sdk      *cptype.SDK
		InParams *InParams
		gsHelper *gshelper.GSHelper
	}
	tests := []struct {
		name    string
		fields  fields
		want    int
		wantErr bool
	}{
		{
			name: "test with inParams appID",
			fields: fields{
				InParams: &InParams{
					AppIDInt: 1,
				},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
				gsHelper: gsHelper,
			},
			want:    4,
			wantErr: false,
		},
		{
			name: "test with no inParams appID",
			fields: fields{
				InParams: &InParams{},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
				gsHelper: gsHelper,
			},
			want:    5,
			wantErr: false,
		},
	}
	var p *CustomFilter
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "AppConditionWithInParamsAppID", func(*CustomFilter) (*model.SelectCondition, error) {
		return &model.SelectCondition{}, nil
	})
	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "AppConditionWithNoInParamsAppID", func(*CustomFilter) (*model.SelectCondition, error) {
		return &model.SelectCondition{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "MemberCondition", func(*CustomFilter) (*model.SelectCondition, error) {
		return &model.SelectCondition{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "StatusCondition", func(*CustomFilter) *model.SelectCondition {
		return &model.SelectCondition{}
	})

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &CustomFilter{
				sdk:      tt.fields.sdk,
				InParams: tt.fields.InParams,
				gsHelper: tt.fields.gsHelper,
			}
			got, err := p.ConditionRetriever()
			if (err != nil) != tt.wantErr {
				t.Errorf("ConditionRetriever() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if len(got) != tt.want {
				t.Errorf("ConditionRetriever() got = %v, want %v", len(got), tt.want)
			}
		})
	}
}

func TestCustomFilter_AppConditionWithNoInParamsAppID(t *testing.T) {
	sdk := &cptype.SDK{
		Tran: &MockTran{},
	}
	ctx := context.WithValue(context.Background(), cptype.GlobalInnerKeyCtxSDK, sdk)
	gsHelper := gshelper.NewGSHelper(&cptype.GlobalStateData{})

	bdl := &bundle.Bundle{}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetAppList", func(bdl *bundle.Bundle, orgID, userID string, req apistructs.ApplicationListRequest) (*apistructs.ApplicationListResponseData, error) {
		return &apistructs.ApplicationListResponseData{
			Total: 2,
			List: []apistructs.ApplicationDTO{
				{
					ID:   1,
					Name: "erda1",
				},
				{
					ID:   2,
					Name: "erda2",
				},
			},
		}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetMyAppsByProject", func(bdl *bundle.Bundle, userid string, orgid, projectID uint64, appName string) (*apistructs.ApplicationListResponseData, error) {
		return &apistructs.ApplicationListResponseData{
			Total: 1,
			List: []apistructs.ApplicationDTO{
				{
					ID:   1,
					Name: "erda1",
				},
			},
		}, nil
	})
	defer monkey.UnpatchAll()

	type fields struct {
		bdl      *bundle.Bundle
		gsHelper *gshelper.GSHelper
		sdk      *cptype.SDK
		InParams *InParams
	}
	tests := []struct {
		name    string
		fields  fields
		want    *model.SelectCondition
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				bdl:      bdl,
				gsHelper: gsHelper,
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
					Identity: &pb.IdentityInfo{
						UserID:         "1",
						InternalClient: "1",
						OrgID:          "1",
					},
				},
				InParams: &InParams{
					ProjectIDInt: 1,
				},
			},
			want: &model.SelectCondition{
				ConditionBase: model.ConditionBase{
					Key:         "appList",
					Label:       "i18n:application",
					Type:        "select",
					Placeholder: "i18n:please-choose-application",
					Disabled:    false,
					Outside:     false,
				},
				Mode: "",
				Options: []model.SelectOption{
					{
						Label: "i18n:participated",
						Value: uint64(0),
						Fix:   false,
					},
					{
						Label: "erda1",
						Value: uint64(1),
						Fix:   false,
					},
					{
						Label: "erda2",
						Value: uint64(2),
						Fix:   false,
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &CustomFilter{
				bdl:      tt.fields.bdl,
				gsHelper: tt.fields.gsHelper,
				sdk:      tt.fields.sdk,
				InParams: tt.fields.InParams,
			}
			got, err := p.AppConditionWithNoInParamsAppID()
			if (err != nil) != tt.wantErr {
				t.Errorf("AppConditionWithNoInParamsAppID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("AppConditionWithNoInParamsAppID() got = %v, want %v", got, tt.want)
			}
		})
	}
}
