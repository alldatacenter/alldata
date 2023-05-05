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

package apitestsv2

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/davecgh/go-spew/spew"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/encoding/jsonpath"
)

func TestJsonPath(t *testing.T) {
	var a map[string]interface{}
	assert.NoError(t, json.Unmarshal([]byte(`{"success":true}`), &a))
	data, err := jsonpath.Get(a, "success")
	assert.NoError(t, err)
	spew.Dump(data)
}

func TestAPITest_Invoke(t *testing.T) {
	type fields struct {
		API       *apistructs.APIInfo
		APIResult *apistructs.ApiTestInfo
		opt       option
	}
	type args struct {
		testEnv    *apistructs.APITestEnvData
		caseParams map[string]*apistructs.CaseParams
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *apistructs.APIRequestInfo
		want1   *apistructs.APIResp
		wantErr bool
	}{
		{
			name: "test_normal",
			fields: fields{
				API: &apistructs.APIInfo{
					URL:    "www.erda.cloud",
					Name:   "TEST",
					Method: "GET",
				},
				APIResult: &apistructs.ApiTestInfo{},
				opt:       option{},
			},
			args: args{
				testEnv:    &apistructs.APITestEnvData{},
				caseParams: nil,
			},
			want:    nil,
			want1:   &apistructs.APIResp{},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			at := &APITest{
				API:       tt.fields.API,
				APIResult: tt.fields.APIResult,
				opt:       tt.fields.opt,
			}
			got, _, err := at.Invoke(nil, tt.args.testEnv, tt.args.caseParams)
			if (err != nil) != tt.wantErr {
				t.Errorf("Invoke() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Invoke() got = %v, want %v", got, tt.want)
			}
		})
	}
}
