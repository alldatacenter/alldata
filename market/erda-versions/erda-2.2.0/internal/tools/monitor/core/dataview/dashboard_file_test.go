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

package dataview

import (
	"errors"
	"mime/multipart"
	"net/http"
	"reflect"
	"strings"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/monitor/core/dataview/db"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func Test_dashboardFileName(t *testing.T) {
	type args struct {
		scope   string
		scopeId string
		viewIds []string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			"case1", args{
				scope:   "org",
				scopeId: "1",
				viewIds: []string{"1"},
			}, "b3JnLTEtMj",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := dashboardFilename(tt.args.scope, tt.args.scopeId); !strings.HasPrefix(got, tt.want) {
				t.Errorf("dashboardFilename() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestCompileToDest(t *testing.T) {
	type args struct {
		scope   string
		scopeId string
		data    string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case1", args{scope: "micro_service", scopeId: "test", data: `filter__metric_scope_id\":\"xxxx\", this is a test`}, `filter__metric_scope_id\":\"test\", this is a test`},
		{"case2", args{scope: "micro_service", scopeId: "test", data: `filter__metric_scope_id":"xxxx", this is a test`}, `filter__metric_scope_id":"test", this is a test`},
		{"case3", args{scope: "micro_service", scopeId: "test", data: `filter_terminus_key\":\"xxxx\", this is a test`}, `filter_terminus_key\":\"test\", this is a test`},
		{"case4", args{scope: "micro_service", scopeId: "test", data: `filter_terminus_key":"xxxx", this is a test`}, `filter_terminus_key":"test", this is a test`},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := CompileToDest(tt.args.scope, tt.args.scopeId, tt.args.data); got != tt.want {
				t.Errorf("CompileToDest() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_ExportTask(t *testing.T) {
	type args struct {
		id string
	}
	tests := []struct {
		name string
		args args
	}{
		{"case1", args{id: "test"}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var erdaDashboardHistoryDB db.ErdaDashboardHistoryDB
			monkey.PatchInstanceMethod(reflect.TypeOf(&erdaDashboardHistoryDB), "FindById", func(erdaDashboardHistoryDB *db.ErdaDashboardHistoryDB, id string) (*db.ErdaDashboardHistory, error) {
				if id == "error" {
					return nil, errors.New("error")
				}
				return &db.ErdaDashboardHistory{ID: id, Scope: "test", ScopeId: "test"}, nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(&erdaDashboardHistoryDB), "UpdateStatusAndFileUUID", func(erdaDashboardHistoryDB *db.ErdaDashboardHistoryDB, id, status, fileUUID, errorMessage string) error {
				if id == "error" {
					return errors.New("error")
				}
				return nil
			})

			var bdl bundle.Bundle
			monkey.PatchInstanceMethod(reflect.TypeOf(&bdl), "UploadFile", func(bdl *bundle.Bundle, req apistructs.FileUploadRequest, clientTimeout ...int64) (*apistructs.File, error) {
				return &apistructs.File{}, nil
			})

			p := &provider{}
			p.ExportTask(tt.args.id)

		})
	}
}

func Test_provider_ParseDashboardTemplate(t *testing.T) {
	type args struct {
		r      *http.Request
		params struct {
			Scope   string `json:"scope"`
			ScopeId string `json:"scopeId"`
		}
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{
			params: struct {
				Scope   string `json:"scope"`
				ScopeId string `json:"scopeId"`
			}{},
		}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var r http.Request
			monkey.PatchInstanceMethod(reflect.TypeOf(&r), "FormFile", func(r *http.Request, key string) (multipart.File, *multipart.FileHeader, error) {
				return nil, nil, errors.New("error")
			})

			p := &provider{}
			got := p.ParseDashboardTemplate(tt.args.r, tt.args.params).(*api.Response)
			if got.Success == tt.wantErr {
				t.Errorf("ParseDashboardTemplate() = %v, wantErr %v", got.Success, tt.wantErr)
			}
		})
	}
}
