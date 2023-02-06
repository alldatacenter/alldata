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

package apistructs

import (
	"reflect"
	"testing"
)

func TestAutoTestSpaceListRequest_URLQueryString(t *testing.T) {
	type fields struct {
		Name          string
		ProjectID     int64
		PageNo        int64
		PageSize      int64
		Order         string
		ArchiveStatus []string
	}
	tests := []struct {
		name   string
		fields fields
		want   map[string][]string
	}{
		{
			name: "test",
			fields: fields{
				Name:          "space",
				ProjectID:     1,
				PageNo:        1,
				PageSize:      10,
				Order:         "updated_at",
				ArchiveStatus: []string{"inprogress"},
			},
			want: map[string][]string{
				"name":          {"space"},
				"projectID":     {"1"},
				"pageNo":        {"1"},
				"pageSize":      {"10"},
				"order":         {"updated_at"},
				"archiveStatus": {"inprogress"},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ats := &AutoTestSpaceListRequest{
				Name:          tt.fields.Name,
				ProjectID:     tt.fields.ProjectID,
				PageNo:        tt.fields.PageNo,
				PageSize:      tt.fields.PageSize,
				Order:         tt.fields.Order,
				ArchiveStatus: tt.fields.ArchiveStatus,
			}
			if got := ats.URLQueryString(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("AutoTestSpaceListRequest.URLQueryString() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestAutoTestSpaceArchiveStatus_Valid(t *testing.T) {
	tests := []struct {
		name string
		s    AutoTestSpaceArchiveStatus
		want bool
	}{
		{
			name: "valid",
			s:    TestSpaceInit,
			want: true,
		},
		{
			name: "invalid",
			s:    AutoTestSpaceArchiveStatus("start"),
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.s.Valid(); got != tt.want {
				t.Errorf("AutoTestSpaceArchiveStatus.Valid() = %v, want %v", got, tt.want)
			}
		})
	}
}
