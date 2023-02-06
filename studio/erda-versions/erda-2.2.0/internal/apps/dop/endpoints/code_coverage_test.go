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
	"fmt"
	"net/http"
	"net/url"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func TestEndpoints_GetCodeCoverageRecordStatus(t *testing.T) {
	type args struct {
		ctx                              context.Context
		r                                *http.Request
		vars                             map[string]string
		GetCodeCoverageRecordStatusError bool
	}
	tests := []struct {
		name string
		args args
		want httpserver.Responser
	}{
		{
			name: "test_parse_project_error",
			args: args{
				ctx: context.Background(),
				r: &http.Request{
					URL: &url.URL{
						RawQuery: "",
					},
				},
				vars:                             nil,
				GetCodeCoverageRecordStatusError: false,
			},
			want: apierrors.ErrGetCodeCoverageExecRecord.InvalidParameter("projectID").ToResp(),
		},
		{
			name: "GetCodeCoverageRecordStatus_error",
			args: args{
				ctx: context.Background(),
				r: &http.Request{
					URL: &url.URL{
						RawQuery: "projectID=1",
					},
				},
				vars:                             nil,
				GetCodeCoverageRecordStatusError: true,
			},
			want: apierrors.ErrGetCodeCoverageExecRecord.InternalError(fmt.Errorf("error")).ToResp(),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &Endpoints{}

			var codeCoverageSvc = &code_coverage.CodeCoverage{}
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(codeCoverageSvc), "GetCodeCoverageRecordStatus", func(code *code_coverage.CodeCoverage, projectID uint64, workspace string) (*apistructs.CodeCoverageExecRecordDetail, error) {
				if tt.args.GetCodeCoverageRecordStatusError {
					return nil, fmt.Errorf("error")
				}
				return nil, nil
			})
			defer patch.Unpatch()
			e.codeCoverageSvc = codeCoverageSvc

			got, _ := e.GetCodeCoverageRecordStatus(tt.args.ctx, tt.args.r, tt.args.vars)
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetCodeCoverageRecordStatus() got = %v, want %v", got, tt.want)
			}
		})
	}
}
