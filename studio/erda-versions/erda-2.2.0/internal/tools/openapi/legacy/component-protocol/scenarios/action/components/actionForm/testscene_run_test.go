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
	"fmt"
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/i18n"
)

func Test_changeTestSet(t *testing.T) {
	type args struct {
		newMap map[string]interface{}
	}
	tests := []struct {
		name string
		args args
		want map[string]interface{}
	}{
		// TODO: Add test cases.

		{
			name: "Filled in the space and scene set",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run555",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_scene_set": 1600,
						"test_space":     123,
					},
					"resources": map[string]interface{}{
						"cpu": 1.54,
						"mem": 1023,
					},
					"loop": map[string]interface{}{
						"break": "3213",
						"strategy": map[string]interface{}{
							"decline_limit_sec": 35,
							"decline_ratio":     4,
							"interval_sec":      66,
							"max_times":         222,
						},
					},
				},
			},
			want: map[string]interface{}{
				"alias": "testscene-run555",
				"loop": map[string]interface{}{
					"strategy": map[string]interface{}{
						"decline_limit_sec": 35,
						"decline_ratio":     4,
						"interval_sec":      66,
						"max_times":         222,
					},
					"break": "3213",
				},
				"version": "1.0",
				"params": map[string]interface{}{
					"test_space":     123,
					"test_scene_set": 1600,
				},
				"resources": map[string]interface{}{
					"cpu": 1.54,
					"mem": 1023,
				},
			},
		},
		{
			name: "Filled in the space and scene set and scene",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run555",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_scene_set": 1603,
						"test_space":     123,
						"test_scene":     6943,
					},
					"resources": map[string]interface{}{
						"cpu": 1.54,
						"mem": 1023,
					},
					"loop": map[string]interface{}{
						"break": "3213",
						"strategy": map[string]interface{}{
							"decline_limit_sec": 35,
							"decline_ratio":     4,
							"interval_sec":      66,
							"max_times":         222,
						},
					},
				},
			},
			want: map[string]interface{}{
				"alias": "testscene-run555",
				"loop": map[string]interface{}{
					"strategy": map[string]interface{}{
						"decline_limit_sec": 35,
						"decline_ratio":     4,
						"interval_sec":      66,
						"max_times":         222,
					},
					"break": "3213",
				},
				"version": "1.0",
				"params": map[string]interface{}{
					"test_scene_set": 1603,
					"test_space":     123,
				},
				"resources": map[string]interface{}{
					"cpu": 1.54,
					"mem": 1023,
				},
			},
		},
		{
			name: "Filled in the space and scene set and scene and cms",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run555",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_scene_set": 1601,
						"test_space":     123,
						"test_scene":     7038,
						"cms":            "autotest^scope-project-autotest-testcase^scopeid-2^364723347986068574",
					},
					"resources": map[string]interface{}{
						"cpu": 1.54,
						"mem": 1023,
					},
					"loop": map[string]interface{}{
						"break": "3213",
						"strategy": map[string]interface{}{
							"decline_limit_sec": 35,
							"decline_ratio":     4,
							"interval_sec":      66,
							"max_times":         222,
						},
					},
				},
			},
			want: map[string]interface{}{
				"alias":   "testscene-run555",
				"version": "1.0",
				"params": map[string]interface{}{
					"test_scene_set": 1601,
					"test_space":     123,
					"cms":            "autotest^scope-project-autotest-testcase^scopeid-2^364723347986068574",
				},
				"resources": map[string]interface{}{
					"cpu": 1.54,
					"mem": 1023,
				},
				"loop": map[string]interface{}{
					"break": "3213",
					"strategy": map[string]interface{}{
						"decline_limit_sec": 35,
						"decline_ratio":     4,
						"interval_sec":      66,
						"max_times":         222,
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := changeTestSet(tt.args.newMap); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("changeTestSet() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_changeTestSpace(t *testing.T) {
	type args struct {
		newMap map[string]interface{}
	}
	tests := []struct {
		name string
		args args
		want map[string]interface{}
	}{
		{
			name: "just Filled in the space",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run3",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_space": 102,
					},
					"resources": map[string]interface{}{
						"cpu": 0.5,
						"mem": 10243,
					},
					"loop": map[string]interface{}{
						"strategy": map[string]interface{}{
							"max_times":         4,
							"decline_ratio":     4,
							"decline_limit_sec": 4,
							"interval_sec":      4,
						},
						"break": "43",
					},
				},
			},
			want: map[string]interface{}{
				"alias": "testscene-run3",
				"loop": map[string]interface{}{
					"strategy": map[string]interface{}{
						"max_times":         4,
						"decline_ratio":     4,
						"decline_limit_sec": 4,
						"interval_sec":      4,
					},
					"break": "43",
				},
				"version": "1.0",
				"params": map[string]interface{}{
					"test_space": 102,
				},
				"resources": map[string]interface{}{
					"cpu": 0.5,
					"mem": 10243,
				},
			},
		},
		{
			name: "Filled in the space and scene set",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run34",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_scene_set": 1534,
						"test_space":     137,
					},
					"resources": map[string]interface{}{
						"cpu": 0.45,
						"mem": 10243,
					},
					"loop": map[string]interface{}{
						"break": "43432",
						"strategy": map[string]interface{}{
							"decline_limit_sec": 3,
							"decline_ratio":     4,
							"interval_sec":      5,
							"max_times":         443,
						},
					},
				},
			},
			want: map[string]interface{}{
				"alias": "testscene-run34",
				"loop": map[string]interface{}{
					"strategy": map[string]interface{}{
						"decline_limit_sec": 3,
						"decline_ratio":     4,
						"interval_sec":      5,
						"max_times":         443,
					},
					"break": "43432",
				},
				"version": "1.0",
				"params": map[string]interface{}{
					"test_space": 137,
				},
				"resources": map[string]interface{}{
					"cpu": 0.45,
					"mem": 10243,
				},
			},
		},
		{
			name: "Filled in the space and scene set and scene",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run444",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_scene_set": 1812,
						"test_space":     118,
						"test_scene":     9231,
					},
					"resources": map[string]interface{}{
						"cpu": 44.45,
						"mem": 143,
					},
					"loop": map[string]interface{}{
						"break": "432",
						"strategy": map[string]interface{}{
							"decline_limit_sec": 3,
							"decline_ratio":     44,
							"interval_sec":      5,
							"max_times":         443,
						},
					},
				},
			},
			want: map[string]interface{}{
				"alias": "testscene-run444",
				"loop": map[string]interface{}{
					"break": "432",
					"strategy": map[string]interface{}{
						"decline_limit_sec": 3,
						"decline_ratio":     44,
						"interval_sec":      5,
						"max_times":         443,
					},
				},
				"version": "1.0",
				"params": map[string]interface{}{
					"test_space": 118,
				},
				"resources": map[string]interface{}{
					"cpu": 44.45,
					"mem": 143,
				},
			},
		},
		{
			name: "Fill in the space and scene set and scene and cms",
			args: args{
				newMap: map[string]interface{}{
					"alias":   "testscene-run444",
					"version": "1.0",
					"params": map[string]interface{}{
						"test_scene_set": 1812,
						"test_space":     118,
						"test_scene":     9231,
						"cms":            "autotest^scope-project-autotest-testcase^scopeid-2^364723347986068574",
					},
					"resources": map[string]interface{}{
						"cpu": 44.45,
						"mem": 143,
					},
					"loop": map[string]interface{}{
						"break": "432",
						"strategy": map[string]interface{}{
							"decline_limit_sec": 3,
							"decline_ratio":     44,
							"interval_sec":      5,
							"max_times":         443,
						},
					},
				},
			},
			want: map[string]interface{}{
				"alias": "testscene-run444",
				"loop": map[string]interface{}{
					"break": "432",
					"strategy": map[string]interface{}{
						"decline_limit_sec": 3,
						"decline_ratio":     44,
						"interval_sec":      5,
						"max_times":         443,
					},
				},
				"version": "1.0",
				"params": map[string]interface{}{
					"test_space": 118,
					"cms":        "autotest^scope-project-autotest-testcase^scopeid-2^364723347986068574",
				},
				"resources": map[string]interface{}{
					"cpu": 44.45,
					"mem": 143,
				},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := changeTestSpace(tt.args.newMap); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("changeTestSpace() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_fillFields(t *testing.T) {
	type args struct {
		field         []apistructs.FormPropItem
		testSpaces    []map[string]interface{}
		testSceneSets []map[string]interface{}
		testScenes    []map[string]interface{}
		cms           []map[string]interface{}
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
				testSpaces: []map[string]interface{}{
					map[string]interface{}{
						"name":  "a",
						"value": "1",
					},
					map[string]interface{}{
						"name":  "b",
						"value": "2",
					},
				},
				testSceneSets: []map[string]interface{}{
					map[string]interface{}{
						"name":  "aa",
						"value": "11",
					},
					map[string]interface{}{
						"name":  "bb",
						"value": "22",
					},
				},
				testScenes: []map[string]interface{}{
					map[string]interface{}{
						"name":  "aaa",
						"value": "111",
					},
					map[string]interface{}{
						"name":  "bbb",
						"value": "222",
					},
				},
				cms: []map[string]interface{}{
					map[string]interface{}{
						"name":  "aaaa",
						"value": "1111",
					},
					map[string]interface{}{
						"name":  "bbbb",
						"value": "2222",
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
					Label:     "测试空间",
					Component: "select",
					Required:  true,
					Key:       "params.test_space",
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
					Label:     "场景集",
					Component: "select",
					Required:  true,
					Key:       "params.test_scene_set",
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
					Label:     "场景",
					Component: "select",
					Required:  true,
					Key:       "params.test_scene",
					ComponentProps: map[string]interface{}{
						"options": []map[string]interface{}{
							map[string]interface{}{
								"name":  "aaa",
								"value": 111,
							},
							map[string]interface{}{
								"name":  "bbb",
								"value": 222,
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
								"name":  "aaaa",
								"value": 1111,
							},
							map[string]interface{}{
								"name":  "bbbb",
								"value": 2222,
							},
						},
					},
					Group: "params",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := fillFields(&i18n.LocaleResource{}, tt.args.field, tt.args.testSpaces, tt.args.testSceneSets, tt.args.testScenes, tt.args.cms); !reflect.DeepEqual(got, tt.want) {
				fmt.Println(got)
				fmt.Println(tt.want)
			}
		})
	}
}
