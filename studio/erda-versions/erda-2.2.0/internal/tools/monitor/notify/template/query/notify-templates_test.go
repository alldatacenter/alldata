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

package query

import (
	"fmt"
	"testing"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/tools/monitor/notify/template/db"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/model"
)

var p *provider

func TestMain(t *testing.M) {
	p = new(provider)
	p.N = new(db.NotifyDB)
	p.N.DB, _ = gorm.Open("mysql", "localhost:3306")
	p.N.DB.LogMode(true)
	p.C = new(config)
	templateMap = map[string]model.Model{
		"issue_create": {
			Metadata: model.Metadata{
				Name:   "",
				Type:   "",
				Module: "",
				Scope:  []string{"project"},
			},
		},
		"issue_update": {
			Metadata: model.Metadata{
				Name:   "",
				Type:   "",
				Module: "",
				Scope:  []string{"project"},
			},
		},
	}
	t.Run()
}

func Test_getAllNotifyTemplates(t *testing.T) {
	tests := []struct {
		name     string
		wantList []model.Model
	}{
		{
			name: "test_getAllNotifyTemplates",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotList := getAllNotifyTemplates(); gotList != nil {
				fmt.Printf("getAllNotifyTemplates() = %+v", gotList)
			}
		})
	}
}

func Test_getNotifyTemplateList(t *testing.T) {
	type args struct {
		scope string
		name  string
		nType string
	}
	tests := []struct {
		name     string
		args     args
		wantList []*model.GetNotifyRes
	}{
		{
			name: "test_getNotifyTemplateList",
			args: args{
				scope: "project",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotList := getNotifyTemplateList(tt.args.scope, tt.args.name, tt.args.nType); gotList != nil {
				fmt.Printf("getNotifyTemplateList() = %+v", gotList)
			}
		})
	}
}

func TestToNotifyConfig(t *testing.T) {
	type args struct {
		c *model.CreateUserDefineNotifyTemplate
	}
	tests := []struct {
		name    string
		args    args
		want    *db.NotifyConfig
		wantErr bool
	}{
		{
			name: "test_toNotifyConfig",
			args: args{
				c: &model.CreateUserDefineNotifyTemplate{
					Name:     "test_notify",
					Group:    "notify_group",
					Trigger:  []string{"notify"},
					Formats:  []map[string]string{},
					Title:    []string{"pipeline start"},
					Template: []string{"### {{projectName}}/{{appName}} pipeline {{pipelineID}} start"},
					Scope:    "project",
					ScopeID:  "3",
					Targets:  []string{"dingding,email,mbox"},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := ToNotifyConfig(tt.args.c)
			if err != nil {
				t.Errorf("ToNotifyConfig() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got == nil {
				t.Errorf("ToNotifyConfig() got is nil")
			}
		})
	}
}
