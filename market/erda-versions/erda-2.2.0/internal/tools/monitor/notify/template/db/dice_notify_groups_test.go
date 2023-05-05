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

package db

import (
	"fmt"
	"testing"
	"time"

	"github.com/erda-project/erda/apistructs"
)

func TestNotifyGroup_ToApiData(t *testing.T) {
	type fields struct {
		BaseModel   BaseModel
		Name        string
		ScopeType   string
		ScopeID     string
		OrgID       int64
		TargetData  string
		Label       string
		ClusterName string
		AutoCreate  bool
		Creator     string
	}
	tests := []struct {
		name   string
		fields fields
		want   *apistructs.NotifyGroup
	}{
		{
			name: "test_ToApiData",
			fields: fields{
				BaseModel: BaseModel{
					ID:        14,
					CreatedAt: time.Now(),
					UpdatedAt: time.Now(),
				},
				Name:       "test",
				ScopeType:  "app",
				ScopeID:    "18",
				OrgID:      1,
				TargetData: `[{"type":"dingding","values":[{"receiver":"https://oapi.dingtalk.com","secret":"e69d6e340a07c47fb"}]}]`,
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			notifyGroup := &NotifyGroup{
				BaseModel:   tt.fields.BaseModel,
				Name:        tt.fields.Name,
				ScopeType:   tt.fields.ScopeType,
				ScopeID:     tt.fields.ScopeID,
				OrgID:       tt.fields.OrgID,
				TargetData:  tt.fields.TargetData,
				Label:       tt.fields.Label,
				ClusterName: tt.fields.ClusterName,
				AutoCreate:  tt.fields.AutoCreate,
				Creator:     tt.fields.Creator,
			}
			if got := notifyGroup.ToApiData(); got != nil {
				fmt.Printf("ToApiData() = %+v", got)
			}
		})
	}
}
