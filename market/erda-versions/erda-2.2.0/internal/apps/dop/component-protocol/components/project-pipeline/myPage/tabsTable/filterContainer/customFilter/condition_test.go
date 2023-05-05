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
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
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

	type fields struct {
		bdl      *bundle.Bundle
		InParams *InParams
		sdk      *cptype.SDK
		Tran     i18n.Translator
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
					AppID: 2,
				},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "test with correct",
			fields: fields{
				bdl: bdl,
				InParams: &InParams{
					AppID: 1,
				},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
			},
			want: &model.SelectCondition{
				ConditionBase: model.ConditionBase{
					Key:         "app",
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
						Value: "erda",
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
			}
			got, err := p.AppConditionWithInParamsAppID()
			if (err != nil) != tt.wantErr {
				t.Errorf("AppConditionWithInParamsAppID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("AppConditionWithInParamsAppID() got = %v, want %v", got, tt.want)
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

	type fields struct {
		InParams *InParams
		sdk      *cptype.SDK
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
					AppID: 1,
				},
				sdk: &cptype.SDK{
					Ctx:  ctx,
					Tran: &MockTran{},
				},
			},
			want:    7,
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
			},
			want:    8,
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
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "MemberCondition", func(*CustomFilter) (MemberCondition, error) {
		return MemberCondition{
			executorCondition: &model.SelectCondition{},
			creatorCondition:  &model.SelectCondition{},
		}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "BranchCondition", func(*CustomFilter) (*model.SelectCondition, error) {
		return &model.SelectCondition{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(p), "StatusCondition", func(*CustomFilter) *model.SelectCondition {
		return &model.SelectCondition{}
	})

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p = &CustomFilter{
				InParams: tt.fields.InParams,
				sdk:      tt.fields.sdk,
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
