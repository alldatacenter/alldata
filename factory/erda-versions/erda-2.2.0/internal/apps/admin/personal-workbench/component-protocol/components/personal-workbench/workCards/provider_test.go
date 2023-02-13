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

package workCards

import (
	"fmt"
	"reflect"
	"testing"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/cardlist"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	i18n2 "github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/i18n"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/services/workbench"
)

type NopTranslator struct{}

func (t NopTranslator) Get(lang i18n.LanguageCodes, key, def string) string { return key }

func (t NopTranslator) Text(lang i18n.LanguageCodes, key string) string { return key }

func (t NopTranslator) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return fmt.Sprintf(key, args...)
}

var defaultSDK = &cptype.SDK{

	GlobalState: &cptype.GlobalStateData{},
	Tran:        &NopTranslator{},
}

func TestWorkCards_getTableName(t *testing.T) {
	type fields struct{}
	type args struct {
		sdk *cptype.SDK
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		// TODO: Add test cases.
		{
			name:   "case1",
			fields: fields{},
			args:   args{sdk: defaultSDK},
			want:   "project",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			if got := wc.getTableName(tt.args.sdk); got != tt.want {
				t.Errorf("getTableName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestWorkCards_getAppTextMeta(t *testing.T) {
	type fields struct{}
	type args struct {
		sdk *cptype.SDK
		app apistructs.AppWorkBenchItem
	}
	mrOps := common.Operation{
		JumpOut: false,
		Target:  "appOpenMr",
		Query:   map[string]interface{}{"projectId": "", "appId": ""},
		Params:  nil,
	}
	runtimeOps := common.Operation{
		JumpOut: false,
		Target:  "deploy",
		Query:   map[string]interface{}{"projectId": "", "appId": ""},
		Params:  nil,
	}
	mrData := make(cptype.OpServerData)
	runtimeData := make(cptype.OpServerData)
	err := common.Transfer(mrOps, &mrData)
	if err != nil {
		logrus.Error(err)
		return
	}
	err = common.Transfer(runtimeOps, &runtimeData)
	if err != nil {
		logrus.Error(err)
		return
	}
	tests := []struct {
		name      string
		fields    fields
		args      args
		wantMetas []cardlist.TextMeta
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{sdk: defaultSDK, app: apistructs.AppWorkBenchItem{
				ApplicationDTO: apistructs.ApplicationDTO{},
				AppRuntimeNum:  0,
				AppOpenMrNum:   0,
			}},
			wantMetas: []cardlist.TextMeta{
				{
					MainText: 0,
					SubText:  "MR Count",
					Operations: map[cptype.OperationKey]cptype.Operation{"clickGoto": {
						ServerData: &mrData,
					}},
				},
				{
					MainText: 0,
					SubText:  "Runtime Count",
					Operations: map[cptype.OperationKey]cptype.Operation{"clickGoto": {
						ServerData: &runtimeData,
					}},
				}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			wc.getAppTextMeta(tt.args.app)
		})
	}
}

func TestWorkCards_getProjTextMeta(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk     *cptype.SDK
		project apistructs.WorkbenchProjOverviewItem
	}
	tests := []struct {
		name      string
		fields    fields
		args      args
		wantMetas []cardlist.TextMeta
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk: defaultSDK,
				project: apistructs.WorkbenchProjOverviewItem{
					ProjectDTO:    apistructs.ProjectDTO{Type: common.MspProject},
					IssueInfo:     nil,
					StatisticInfo: &apistructs.ProjectStatisticInfo{},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			wc.getProjTextMeta(tt.args.sdk, tt.args.project, workbench.IssueUrlQueries{}, workbench.UrlParams{})
		})
	}
}

func TestWorkCards_getAppIconOps(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk *cptype.SDK
		app apistructs.AppWorkBenchItem
	}
	tests := []struct {
		name     string
		fields   fields
		args     args
		wantIops []cardlist.IconOperations
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk: defaultSDK,
				app: apistructs.AppWorkBenchItem{
					ApplicationDTO: apistructs.ApplicationDTO{},
					AppRuntimeNum:  0,
					AppOpenMrNum:   0,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			wc.getAppIconOps(tt.args.sdk, tt.args.app)
		})
	}
}

func TestWorkCards_getProjIconOps(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk     *cptype.SDK
		project apistructs.WorkbenchProjOverviewItem
		params  workbench.UrlParams
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   []cardlist.IconOperations
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk:     defaultSDK,
				project: apistructs.WorkbenchProjOverviewItem{ProjectDTO: apistructs.ProjectDTO{Type: common.DevOpsProject}},
				params:  workbench.UrlParams{},
			},
		},
		{
			name: "case1",
			args: args{
				sdk:     defaultSDK,
				project: apistructs.WorkbenchProjOverviewItem{ProjectDTO: apistructs.ProjectDTO{Type: common.MspProject}},
				params:  workbench.UrlParams{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			wc.getProjIconOps(tt.args.sdk, tt.args.project, tt.args.params)
		})
	}
}

func TestWorkCards_getProjectTitleState(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk  *cptype.SDK
		kind string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   []cardlist.TitleState
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk:  defaultSDK,
				kind: common.MspProject,
			},
			want: []cardlist.TitleState{
				{
					Text:   i18n2.I18nKeyMspProject,
					Status: common.ProjMspStatus,
				},
			},
		},
		{
			name: "case2",
			args: args{
				sdk:  defaultSDK,
				kind: common.DevOpsProject,
			},
			want: []cardlist.TitleState{
				{
					Text:   i18n2.I18nKeyDevOpsProject,
					Status: common.ProjDevOpsStatus,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			if got := wc.getProjectTitleState(tt.args.sdk, tt.args.kind); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getProjectTitleState() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestWorkCards_getAppTitleState(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk  *cptype.SDK
		mode string
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   []cardlist.TitleState
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk:  defaultSDK,
				mode: "LIBRARY",
			},
			want: []cardlist.TitleState{
				{
					Text:   i18n2.I18nKeyAppModeLIBRARY,
					Status: common.AppLibraryStatus,
				},
			},
		},
		{
			name: "case2",
			args: args{
				sdk:  defaultSDK,
				mode: "BIGDATA",
			},
			want: []cardlist.TitleState{
				{
					Text:   i18n2.I18nKeyAppModeBIGDATA,
					Status: common.AppBigdataStatus,
				},
			},
		},
		{
			name: "case1",
			args: args{
				sdk:  defaultSDK,
				mode: "SERVICE",
			},
			want: []cardlist.TitleState{
				{
					Text:   i18n2.I18nKeyAppModeSERVICE,
					Status: common.AppServiceStatus,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			if got := wc.getAppTitleState(tt.args.sdk, tt.args.mode); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getAppTitleState() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestWorkCards_getAppCardOps(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk *cptype.SDK
		app apistructs.AppWorkBenchItem
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantOps map[cptype.OperationKey]cptype.Operation
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk: defaultSDK,
				app: apistructs.AppWorkBenchItem{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			wc.getAppCardOps(tt.args.sdk, tt.args.app)
		})
	}
}

func TestWorkCards_getProjectCardOps(t *testing.T) {
	type fields struct {
	}
	type args struct {
		sdk     *cptype.SDK
		params  workbench.UrlParams
		project apistructs.WorkbenchProjOverviewItem
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantOps map[cptype.OperationKey]cptype.Operation
	}{
		// TODO: Add test cases.
		{
			name: "case1",
			args: args{
				sdk:     defaultSDK,
				params:  workbench.UrlParams{},
				project: apistructs.WorkbenchProjOverviewItem{},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			wc := &WorkCards{}
			wc.getProjectCardOps(tt.args.sdk, tt.args.params, tt.args.project)
		})
	}
}
