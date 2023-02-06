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

package action

import (
	"context"
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/i18n"
)

func Test_fillTestPlanFields(t *testing.T) {
	type args struct {
		field     []apistructs.FormPropItem
		testPlans []map[string]interface{}
		cms       []map[string]interface{}
	}
	tests := []struct {
		name string
		args args
		want []apistructs.FormPropItem
	}{
		// TODO: Add test cases.
		{
			name: "Filled",
			args: args{
				field: []apistructs.FormPropItem{
					apistructs.FormPropItem{
						Label:     "执行条件",
						Component: "input",
						Required:  true,
						Group:     "params",
					},
				},
				testPlans: []map[string]interface{}{
					map[string]interface{}{
						"name":  "a",
						"value": "1",
					},
					map[string]interface{}{
						"name":  "b",
						"value": "2",
					},
				},
				cms: []map[string]interface{}{
					map[string]interface{}{
						"name":  "aa",
						"value": "11",
					},
					map[string]interface{}{
						"name":  "bb",
						"value": "22",
					},
				},
			},
			want: []apistructs.FormPropItem{
				apistructs.FormPropItem{
					Label:     "执行条件",
					Component: "input",
					Required:  true,
					Group:     "params",
				},
				apistructs.FormPropItem{
					Component: "formGroup",
					ComponentProps: map[string]interface{}{
						"title": "任务参数",
					},
					Group: "params",
					Key:   "params",
				},
				apistructs.FormPropItem{
					Label:     "测试计划",
					Component: "select",
					Required:  true,
					Key:       "params.test_plan",
					ComponentProps: map[string]interface{}{
						"options": []map[string]interface{}{
							map[string]interface{}{
								"name":  "a",
								"value": 1,
							},
							map[string]interface{}{
								"name":  "b",
								"value": 2,
							},
						},
					},
					Group: "params",
				},
				apistructs.FormPropItem{
					Label:     "参数配置",
					Component: "select",
					Required:  true,
					Key:       "params.cms",
					ComponentProps: map[string]interface{}{
						"options": []map[string]interface{}{
							map[string]interface{}{
								"name":  "aa",
								"value": 11,
							},
							map[string]interface{}{
								"name":  "bb",
								"value": 22,
							},
						},
					},
					Group: "params",
				},
				apistructs.FormPropItem{
					Label:        "失败后是否继续执行",
					Component:    "input",
					Required:     false,
					Key:          "params.is_continue_execution",
					Group:        "params",
					DefaultValue: true,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := fillTestPlanFields(&i18n.LocaleResource{}, tt.args.field, tt.args.testPlans, tt.args.cms); !reflect.DeepEqual(got, tt.want) {
				fmt.Println(got)
				fmt.Println(tt.want)
			}
		})
	}
}

func Test_testPlanRun(t *testing.T) {
	type args struct {
		ctx             context.Context
		c               *apistructs.Component
		scenario        apistructs.ComponentProtocolScenario
		event           apistructs.ComponentEvent
		globalStateData *apistructs.GlobalStateData
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name:    "test empty plan",
			wantErr: false,
			args: args{
				c: &apistructs.Component{
					Props: map[string]interface{}{
						"fields": []apistructs.FormPropItem{},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var contextBundle = protocol.ContextBundle{}
			bdl := bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "PagingTestPlansV2", func(bdl *bundle.Bundle, req apistructs.TestPlanV2PagingRequest) (*apistructs.TestPlanV2PagingResponseData, error) {
				return &apistructs.TestPlanV2PagingResponseData{
					Total: 0,
					List:  []*apistructs.TestPlanV2{},
				}, nil
			})
			defer patch1.Unpatch()

			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListAutoTestGlobalConfig", func(bdl *bundle.Bundle, req apistructs.AutoTestGlobalConfigListRequest) ([]apistructs.AutoTestGlobalConfig, error) {
				return []apistructs.AutoTestGlobalConfig{}, nil
			})
			defer patch2.Unpatch()

			contextBundle.Bdl = bdl
			contextBundle.InParams = map[string]interface{}{
				"projectId": "1",
			}

			tt.args.ctx = context.WithValue(context.Background(), protocol.GlobalInnerKeyCtxBundle.String(), contextBundle)

			if err := testPlanRun(tt.args.ctx, tt.args.c, tt.args.scenario, tt.args.event, tt.args.globalStateData); (err != nil) != tt.wantErr {
				t.Errorf("testPlanRun() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
