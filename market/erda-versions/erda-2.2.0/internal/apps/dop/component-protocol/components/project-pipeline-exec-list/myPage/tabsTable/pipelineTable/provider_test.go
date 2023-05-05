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

package pipelineTable

import (
	"reflect"
	"sort"
	"testing"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline-exec-list/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline-exec-list/common/gshelper"
)

func TestParticipatedInApps(t *testing.T) {
	type args struct {
		appIDs []uint64
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "test in apps",
			args: args{
				appIDs: []uint64{0, 1, 2},
			},
			want: true,
		},
		{
			name: "test not in apps",
			args: args{
				appIDs: []uint64{1, 2},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ParticipatedInApps(tt.args.appIDs); got != tt.want {
				t.Errorf("ParticipatedInApps() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetAppNames(t *testing.T) {
	type args struct {
		helper *gshelper.GSHelper
	}
	var (
		myAppNames   = []string{"erda1", "erda2"}
		appIDNameMap = map[string]string{
			"1": "erda1",
			"2": "erda2",
			"3": "erda3",
			"4": "erda4",
			"5": "erda5",
		}
	)

	helper1 := gshelper.NewGSHelper(&cptype.GlobalStateData{})
	helper1.SetAppsFilter([]uint64{1})
	helper1.SetGlobalInParamsAppName("erda1")

	helper2 := gshelper.NewGSHelper(&cptype.GlobalStateData{})
	helper2.SetAppsFilter([]uint64{common.Participated, 1, 3})
	helper2.SetGlobalMyAppNames(myAppNames)
	helper2.SetGlobalAppIDNameMap(appIDNameMap)

	helper3 := gshelper.NewGSHelper(&cptype.GlobalStateData{})
	helper3.SetAppsFilter([]uint64{4, 5})
	helper3.SetGlobalMyAppNames(myAppNames)
	helper3.SetGlobalAppIDNameMap(appIDNameMap)

	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "test with inParams app",
			args: args{
				helper: helper1,
			},
			want: []string{"erda1"},
		},
		{
			name: "test with participated",
			args: args{
				helper: helper2,
			},
			want: []string{"erda1", "erda2", "erda3"},
		},
		{
			name: "test with no participated",
			args: args{
				helper: helper3,
			},
			want: []string{"erda4", "erda5"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := GetAppNames(tt.args.helper)
			sort.Strings(got)
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetAppNames() = %v, want %v", got, tt.want)
			}
		})
	}
}
