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

package page

import (
	"fmt"
	"reflect"
	"testing"
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/commodel"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/list"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/app-runtime/common"
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

func TestList_doFilter(t *testing.T) {
	type args struct {
		conditions          map[string]map[string]bool
		appRuntime          bundle.GetApplicationRuntimesDataEle
		deployAt            int64
		appName             string
		deploymentOrderName string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{
				conditions: map[string]map[string]bool{common.FilterApp: {"1": true}},
			},
			want: true,
		},
		{
			name: "2",
			args: args{
				conditions: map[string]map[string]bool{
					common.FilterDeployStatus: {"1": true},
				},
				appRuntime: bundle.GetApplicationRuntimesDataEle{
					RawDeploymentStatus: "1",
				}},
			want: true,
		},
		{
			name: "4",
			args: args{},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &List{}
			if got := p.doFilter(tt.args.conditions, tt.args.appRuntime, tt.args.deployAt, tt.args.appName, tt.args.deploymentOrderName); got != tt.want {
				t.Errorf("doFilter() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getTitleState(t *testing.T) {
	type args struct {
		sdk          *cptype.SDK
		deployStatus string
		deploymentId string
		appId        string
		delete       string
	}
	tests := []struct {
		name string
		args args
		want []list.StateInfo
	}{
		{
			name: "1",
			args: args{
				sdk:          defaultSDK,
				deployStatus: string(apistructs.DeploymentStatusInit),
			},
		},
		{
			name: "1",
			args: args{
				sdk:          defaultSDK,
				deployStatus: string(apistructs.DeploymentStatusOK),
			},
		},
		{
			name: "1",
			args: args{
				sdk:          defaultSDK,
				deployStatus: string(apistructs.DeploymentStatusFailed),
			},
		},
		{
			name: "1",
			args: args{
				sdk:          defaultSDK,
				deployStatus: string(apistructs.DeploymentStatusCanceling),
			},
		},
		{
			name: "1",
			args: args{
				sdk:          defaultSDK,
				deployStatus: string(apistructs.DeploymentStatusCanceled),
				delete:       "s",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			getTitleState(tt.args.sdk, tt.args.deployStatus, tt.args.deploymentId, tt.args.appId, tt.args.delete, false)
		})
	}
}

func Test_getMainState(t *testing.T) {
	type args struct {
		runtimeStatus string
	}
	tests := []struct {
		name string
		args args
		want *list.StateInfo
	}{
		{
			name: "1",
			args: args{runtimeStatus: apistructs.RuntimeStatusHealthy},
		},
		{
			name: "2",
			args: args{runtimeStatus: apistructs.RuntimeStatusUnHealthy},
		},
		{
			name: "3",
			args: args{runtimeStatus: ""},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			getIcon(tt.args.runtimeStatus)
		})
	}
}

func Test_getIconByServiceCnt(t *testing.T) {
	type args struct {
		healthy int
		all     int
	}
	tests := []struct {
		name string
		args args
		want *commodel.Icon
	}{
		{
			name: "1",
			args: args{
				healthy: 0,
				all:     10,
			},
			want: &commodel.Icon{URL: common.FrontedIconLoading},
		},
		{
			name: "2",
			args: args{
				healthy: 10,
				all:     10,
			},
			want: &commodel.Icon{URL: common.FrontedIconBreathing},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getIconByServiceCnt(tt.args.healthy, tt.args.all); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getOperations() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getOperations(t *testing.T) {
	type args struct {
		projectId uint64
		appId     uint64
		runtimeId uint64
		disable   bool
	}
	tests := []struct {
		name string
		args args
		want map[cptype.OperationKey]cptype.Operation
	}{
		{
			name: "1",
			args: args{
				projectId: 0,
				appId:     0,
				runtimeId: 0,
				disable:   true,
			},
			want: map[cptype.OperationKey]cptype.Operation{
				"clickGoto": {
					ServerData: &cptype.OpServerData{
						"target": "appDeployRuntime",
						"params": map[string]string{
							"projectId": "0",
							"appId":     "0",
							"runtimeId": "0",
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getOperations(defaultSDK, tt.args.projectId, tt.args.appId, tt.args.runtimeId, tt.args.disable); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getOperations() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestList_getBatchOperation(t *testing.T) {
	type args struct {
		sdk *cptype.SDK
		ids []string
	}
	tests := []struct {
		name string
		args args
		want map[cptype.OperationKey]cptype.Operation
	}{
		{
			name: "1",
			args: args{
				sdk: defaultSDK,
				ids: []string{"1"},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := List{}
			p.getBatchOperation(tt.args.sdk, tt.args.ids)
		})
	}
}

func Test_getKvInfos(t *testing.T) {
	type args struct {
		sdk              *cptype.SDK
		appName          string
		creatorName      string
		deployOrderName  string
		deployVersion    string
		healthStr        string
		runtime          bundle.GetApplicationRuntimesDataEle
		lastOperatorTime time.Time
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "1",
			args: args{sdk: defaultSDK, deployOrderName: "2", healthStr: "1"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			getKvInfos(tt.args.sdk, tt.args.appName, tt.args.creatorName, tt.args.deployOrderName, tt.args.deployVersion, tt.args.healthStr, tt.args.runtime, tt.args.lastOperatorTime)
		})
	}
}

func Test_getMoreOperations(t *testing.T) {
	type args struct {
		sdk *cptype.SDK
		id  string
	}
	tests := []struct {
		name string
		args args
	}{
		// TODO: Add test cases.
		{
			name: "1",
			args: args{sdk: defaultSDK},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			getMoreOperations(tt.args.sdk, tt.args.id)
		})
	}
}
