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

package step

import (
	"encoding/json"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

const testStepsData string = `[{"id":162,"type":"API","method":"","value":"{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"DELETE\",\"name\":\"deleteOrder\",\"out_params\":[{\"expression\":\"data.id\",\"key\":\"as\",\"source\":\"body:json\"}],\"params\":null,\"url\":\"/v2/store/order/{orderId}\"}}","name":"deleteOrder","preID":0,"preType":"Serial","sceneID":54,"spaceID":6,"creatorID":"","updaterID":"","Children":null,"apiSpecID":6},{"id":163,"type":"API","method":"","value":"{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"DELETE\",\"name\":\"deleteOrder\",\"out_params\":[{\"expression\":\"data.id\",\"key\":\"asd\",\"source\":\"body:json\"},{\"expression\":\"data.status\",\"key\":\"asd\",\"source\":\"body:json\"}],\"params\":[],\"url\":\"/sadfs/sad\"}}","name":"deleteOrder","preID":162,"preType":"Serial","sceneID":54,"spaceID":6,"creatorID":"","updaterID":"","Children":null,"apiSpecID":0}]`

func TestGetStepOutPut(t *testing.T) {
	var (
		err    error
		steps  []apistructs.AutoTestSceneStep
		output map[string]map[string]string
	)
	err = json.Unmarshal([]byte(testStepsData), &steps)
	output, err = GetStepOutPut(steps)

	assert.NoError(t, err)
	assert.Equal(t, "${{ outputs.162.as }}", output["#162-deleteOrder"]["as"])
	assert.Equal(t, "${{ outputs.163.asd }}", output["#163-deleteOrder"]["asd"])
}

func TestGetStepAllOutput(t *testing.T) {
	type args struct {
		steps        []apistructs.AutoTestSceneStep
		ConfigOutput map[string]apistructs.SnippetQueryDetail
	}
	tests := []struct {
		name    string
		args    args
		want    map[string]map[string]string
		wantErr bool
	}{
		{
			name: "test_empty",
			args: args{
				steps: nil,
			},
			want:    map[string]map[string]string{},
			wantErr: false,
		},
		{
			name: "test_api_step",
			args: args{
				steps: []apistructs.AutoTestSceneStep{
					{
						Type: apistructs.StepTypeAPI,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 1,
						},
						Name:  "step1",
						Value: "{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"GET\",\"name\":\"test\",\"out_params\":[{\"expression\":\"1\",\"key\":\"name\",\"source\":\"body:json\"},{\"expression\":\"2\",\"key\":\"name1\",\"source\":\"body:json\"}],\"params\":[],\"url\":\"/api/value\"},\"loop\":null}",
					},
					{
						Type: apistructs.StepTypeAPI,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 2,
						},
						Name:  "step2",
						Value: "{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"GET\",\"name\":\"test\",\"out_params\":[{\"expression\":\"1\",\"key\":\"name\",\"source\":\"body:json\"},{\"expression\":\"2\",\"key\":\"name1\",\"source\":\"body:json\"}],\"params\":[],\"url\":\"/api/value\"},\"loop\":null}",
					},
					{
						Type: apistructs.StepTypeAPI,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 3,
						},
						Name:  "step3",
						Value: "",
					},
				},
			},
			wantErr: false,
			want: map[string]map[string]string{
				"1": {
					"name":  "${{ outputs.1.name }}",
					"name1": "${{ outputs.1.name1 }}",
				},
				"2": {
					"name":  "${{ outputs.2.name }}",
					"name1": "${{ outputs.2.name1 }}",
				},
			},
		},

		{
			name: "test_api_step_with_configSheet",
			args: args{
				steps: []apistructs.AutoTestSceneStep{
					{
						Type: apistructs.StepTypeAPI,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 1,
						},
						Name:  "step1",
						Value: "{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"GET\",\"name\":\"test\",\"out_params\":[{\"expression\":\"1\",\"key\":\"name\",\"source\":\"body:json\"},{\"expression\":\"2\",\"key\":\"name1\",\"source\":\"body:json\"}],\"params\":[],\"url\":\"/api/value\"},\"loop\":null}",
					},
					{
						Type: apistructs.StepTypeConfigSheet,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 2,
						},
						Name:  "step2",
						Value: "{\"configSheetID\":\"374612045842100850\",\"configSheetName\":\"sss\",\"runParams\":{}}",
					},
				},
				ConfigOutput: map[string]apistructs.SnippetQueryDetail{
					"2": {
						Outputs: []string{
							"${{ outputs.2.name }}",
							"${{ outputs.2.name1 }}",
						},
					},
				},
			},
			wantErr: false,
			want: map[string]map[string]string{
				"1": {
					"name":  "${{ outputs.1.name }}",
					"name1": "${{ outputs.1.name1 }}",
				},
				"2": {
					"name":  "${{ outputs.2.name }}",
					"name1": "${{ outputs.2.name1 }}",
				},
			},
		},

		{
			name: "test_api_step_with_empty_configSheet",
			args: args{
				steps: []apistructs.AutoTestSceneStep{
					{
						Type: apistructs.StepTypeAPI,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 1,
						},
						Name:  "step1",
						Value: "{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"GET\",\"name\":\"test\",\"out_params\":[{\"expression\":\"1\",\"key\":\"name\",\"source\":\"body:json\"},{\"expression\":\"2\",\"key\":\"name1\",\"source\":\"body:json\"}],\"params\":[],\"url\":\"/api/value\"},\"loop\":null}",
					},
					{
						Type: apistructs.StepTypeConfigSheet,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 2,
						},
						Name:  "step2",
						Value: "",
					},
				},
				ConfigOutput: map[string]apistructs.SnippetQueryDetail{
					"2": {
						Outputs: []string{},
					},
				},
			},
			wantErr: false,
			want: map[string]map[string]string{
				"1": {
					"name":  "${{ outputs.1.name }}",
					"name1": "${{ outputs.1.name1 }}",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var bdl = &bundle.Bundle{}
			var patch *monkey.PatchGuard
			var gs = &cptype.GlobalStateData{}
			if tt.args.ConfigOutput != nil {
				patch = monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipelineActionParamsAndOutputs", func(b *bundle.Bundle, req apistructs.SnippetQueryDetailsRequest) (map[string]apistructs.SnippetQueryDetail, error) {
					return tt.args.ConfigOutput, nil
				})
			}

			got, err := GetStepAllOutput(tt.args.steps, bdl, gs)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetStepAllOutput() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetStepAllOutput() got = %v, want %v", got, tt.want)
			}

			if patch != nil {
				patch.Unpatch()
			}
		})
	}
}

func TestGetConfigSheetStepOutPut(t *testing.T) {
	type args struct {
		steps        []apistructs.AutoTestSceneStep
		ConfigOutput map[string]apistructs.SnippetQueryDetail
	}
	tests := []struct {
		name    string
		args    args
		want    map[string]map[string]string
		wantErr bool
	}{
		{
			name: "test_empty",
			args: args{
				steps: nil,
			},
			want:    map[string]map[string]string{},
			wantErr: false,
		},
		{
			name: "test_configSheet",
			args: args{
				steps: []apistructs.AutoTestSceneStep{
					{
						Type: apistructs.StepTypeConfigSheet,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 2,
						},
						Name:  "step2",
						Value: "{\"configSheetID\":\"374612045842100850\",\"configSheetName\":\"sss\",\"runParams\":{}}",
					},
				},
				ConfigOutput: map[string]apistructs.SnippetQueryDetail{
					"2": {
						Outputs: []string{
							"${{ outputs.2.name }}",
							"${{ outputs.2.name1 }}",
						},
					},
				},
			},
			wantErr: false,
			want: map[string]map[string]string{
				"#2-step2": {
					"name":  "${{ outputs.2.name }}",
					"name1": "${{ outputs.2.name1 }}",
				},
			},
		},

		{
			name: "test_empty_configSheet",
			args: args{
				steps: []apistructs.AutoTestSceneStep{
					{
						Type: apistructs.StepTypeConfigSheet,
						AutoTestSceneParams: apistructs.AutoTestSceneParams{
							ID: 2,
						},
						Name:  "step2",
						Value: "",
					},
				},
				ConfigOutput: map[string]apistructs.SnippetQueryDetail{
					"2": {
						Outputs: []string{},
					},
				},
			},
			wantErr: false,
			want:    map[string]map[string]string{},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			var bdl = &bundle.Bundle{}
			var patch *monkey.PatchGuard
			var gs = &cptype.GlobalStateData{}
			if tt.args.ConfigOutput != nil {
				patch = monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipelineActionParamsAndOutputs", func(b *bundle.Bundle, req apistructs.SnippetQueryDetailsRequest) (map[string]apistructs.SnippetQueryDetail, error) {
					return tt.args.ConfigOutput, nil
				})
			}

			got, err := GetConfigSheetStepOutPut(tt.args.steps, bdl, gs)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetConfigSheetStepOutPut() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetConfigSheetStepOutPut() got = %v, want %v", got, tt.want)
			}

			if patch != nil {
				patch.Unpatch()
			}
		})
	}
}
