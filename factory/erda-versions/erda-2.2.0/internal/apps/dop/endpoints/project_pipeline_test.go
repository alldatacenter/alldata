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

package endpoints

import (
	"context"
	"net/http"
	"net/url"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/gorilla/schema"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func TestEndpoints_projectPipelineDetail(t *testing.T) {
	type fields struct {
		result       apistructs.PipelineDetailDTO
		request      apistructs.PipelineDetailRequest
		assertUserID uint64
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		{
			name: "test",
			fields: fields{
				request: apistructs.PipelineDetailRequest{
					PipelineID:               1,
					SimplePipelineBaseResult: false,
				},
				assertUserID: 1,
				result: apistructs.PipelineDetailDTO{
					PipelineDTO: apistructs.PipelineDTO{
						ID:            1,
						ApplicationID: 1,
						Branch:        "master",
					},
				},
			},
			args: args{
				r: &http.Request{
					URL: &url.URL{
						RawQuery: "pipelineID=1&simplePipelineBaseResult=false",
					},
					Header: http.Header{
						"USER-ID":         []string{"1"},
						"Internal-Client": []string{"pipeline"},
					},
				},
			},
			want:    nil,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &Endpoints{}

			queryStringDecoder := schema.NewDecoder()
			queryStringDecoder.IgnoreUnknownKeys(true)
			e.queryStringDecoder = queryStringDecoder

			var bdl = &bundle.Bundle{}
			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipelineV2", func(d *bundle.Bundle, req apistructs.PipelineDetailRequest) (*apistructs.PipelineDetailDTO, error) {
				assert.Equal(t, req.PipelineID, tt.fields.request.PipelineID)
				assert.Equal(t, req.SimplePipelineBaseResult, tt.fields.request.SimplePipelineBaseResult)

				return &tt.fields.result, nil
			})
			e.bdl = bdl

			got, err := e.projectPipelineDetail(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("pipelineDetail() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			assert.NotNil(t, got)
			assert.Equal(t, got.GetContent().(httpserver.Resp).Data.(*apistructs.PipelineDetailDTO).ID, tt.fields.result.ID)
			assert.Equal(t, got.GetContent().(httpserver.Resp).Data.(*apistructs.PipelineDetailDTO).Branch, tt.fields.result.Branch)
			assert.Equal(t, got.GetContent().(httpserver.Resp).Data.(*apistructs.PipelineDetailDTO).ApplicationID, tt.fields.result.ApplicationID)
		})
	}
}
