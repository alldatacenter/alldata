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

package run

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_provider_createPipelineRunLabels(t *testing.T) {
	type fields struct {
		dbClient *dbclient.Client
	}
	type args struct {
		p   spec.Pipeline
		req *apistructs.PipelineRunRequest
	}

	dbClient := &dbclient.Client{}
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "BatchInsertLabels", func(dbClient *dbclient.Client, labels []spec.PipelineLabel, ops ...dbclient.SessionOption) (err error) {
		return nil
	})
	defer monkey.UnpatchAll()

	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test with create labels",
			fields: fields{
				dbClient: dbClient,
			},
			args: args{
				p: spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:              1,
						PipelineSource:  "dice",
						PipelineYmlName: "pipeline.yml",
					},
				},
				req: &apistructs.PipelineRunRequest{
					IdentityInfo: apistructs.IdentityInfo{
						UserID: "1",
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{
				dbClient: tt.fields.dbClient,
			}
			if err := s.createPipelineRunLabels(tt.args.p, tt.args.req); (err != nil) != tt.wantErr {
				t.Errorf("createPipelineRunLabels() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
