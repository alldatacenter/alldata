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

package cancelExecuteButton

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/pkg/i18n"
)

func TestComponentAction_marshal(t *testing.T) {
	type fields struct {
		State State
	}
	type args struct {
		c *apistructs.Component
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Filled",
			args: args{
				c: &apistructs.Component{
					State: map[string]interface{}{
						"pipelineId": "2",
						"pipelineDetail": map[string]interface{}{
							"pipelineDTO": "2",
							"sdsd":        "3",
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			a := &ComponentAction{
				State: tt.fields.State,
			}
			if err := a.marshal(tt.args.c); (err != nil) != tt.wantErr {
				t.Errorf("marshal() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestComponentAction_Import(t *testing.T) {
	type fields struct {
		State State
	}
	type args struct {
		c *apistructs.Component
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Filled",
			args: args{
				c: &apistructs.Component{
					State: map[string]interface{}{
						"pipelineId": "2",
						"pipelineDetail": map[string]interface{}{
							"pipelineDTO": "2",
							"sdsd":        "3",
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			a := &ComponentAction{
				State: tt.fields.State,
			}
			if err := a.Import(tt.args.c); (err != nil) != tt.wantErr {
				t.Errorf("Import() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestComponentAction_Render(t *testing.T) {
	var bdl = protocol.ContextBundle{Locale: "zh"}
	dl := bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
	m := monkey.PatchInstanceMethod(reflect.TypeOf(dl), "GetLocale",
		func(bdl *bundle.Bundle, local ...string) *i18n.LocaleResource {
			return &i18n.LocaleResource{}
		})
	defer m.Unpatch()
	monkey.PatchInstanceMethod(reflect.TypeOf(dl), "PagingTestPlansV2", func(b *bundle.Bundle, req apistructs.TestPlanV2PagingRequest) (*apistructs.TestPlanV2PagingResponseData, error) {
		return &apistructs.TestPlanV2PagingResponseData{}, nil
	})
	bdl.Bdl = dl
	bdl.InParams = map[string]interface{}{"projectId": 1}
	ctx := context.WithValue(context.Background(), protocol.GlobalInnerKeyCtxBundle.String(), bdl)
	a := &ComponentAction{}
	a.State.PipelineDetail = nil
	err := a.Render(ctx, &apistructs.Component{}, apistructs.ComponentProtocolScenario{},
		apistructs.ComponentEvent{
			Operation:     apistructs.RenderingOperation,
			OperationData: nil,
		}, nil)
	assert.NoError(t, err)
	a.State.PipelineDetail = &apistructs.PipelineDetailDTO{
		PipelineDTO: apistructs.PipelineDTO{
			ID: 0,
		},
	}
	assert.NoError(t, err)
}
