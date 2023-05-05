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

package server

import (
	"reflect"
	"sync"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func TestGetUpdateClientData(t *testing.T) {
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateEvent", func(_ *bundle.Bundle, ev *apistructs.EventCreateRequest) error {
		return nil
	})
	type args struct {
		clusterKey string
		clientType apistructs.ClusterManagerClientType
		data       apistructs.ClusterManagerClientDetail
	}
	type test struct {
		name       string
		args       args
		want       apistructs.ClusterManagerClientDetail
		wantExists bool
	}
	tests := []test{
		{
			name: "test pipeline client data",
			args: args{
				clusterKey: "erda-dev",
				clientType: apistructs.ClusterManagerClientTypePipeline,
				data: apistructs.ClusterManagerClientDetail{
					"host": "localhost",
				},
			},
			want: apistructs.ClusterManagerClientDetail{
				"host": "localhost",
			},
			wantExists: true,
		},
		{
			name: "test cluster agent client data",
			args: args{
				clusterKey: "erda-dev",
				clientType: apistructs.ClusterManagerClientTypeCluster,
				data: apistructs.ClusterManagerClientDetail{
					"namespace": "erda-dev",
				},
			},
			want: apistructs.ClusterManagerClientDetail{
				"namespace": "erda-dev",
			},
			wantExists: true,
		},
		{
			name: "test cluster agent client data",
			args: args{
				clusterKey: "terminus-dev",
				clientType: apistructs.ClusterManagerClientTypeCluster,
				data: apistructs.ClusterManagerClientDetail{
					"namespace": "erda-dev",
				},
			},
			want: apistructs.ClusterManagerClientDetail{
				"namespace": "erda-dev",
			},
			wantExists: true,
		},
		{
			name: "test empty cluster key",
			args: args{
				clusterKey: "",
				clientType: apistructs.ClusterManagerClientTypeCluster,
				data:       apistructs.ClusterManagerClientDetail{},
			},
			want:       apistructs.ClusterManagerClientDetail{},
			wantExists: false,
		},
	}
	wait := sync.WaitGroup{}
	wait.Add(len(tests))
	for _, tt := range tests {
		go func(t test) {
			updateClientDetailWithEvent(t.args.clientType, t.args.clusterKey, t.args.data)
			wait.Done()
		}(tt)
	}
	wait.Wait()
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, ok := getClientDetail(tt.args.clientType, tt.args.clusterKey)
			if ok != tt.wantExists {
				t.Errorf("getClientDetail() exist = %v, wantExist %v", ok, tt.wantExists)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getClientDetail() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_listClientDetailByType(t *testing.T) {
	clientDatas = map[apistructs.ClusterManagerClientType]apistructs.ClusterManagerClientMap{}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateEvent", func(_ *bundle.Bundle, ev *apistructs.EventCreateRequest) error {
		return nil
	})
	type args struct {
		clusterKey string
		clientType apistructs.ClusterManagerClientType
		data       apistructs.ClusterManagerClientDetail
	}
	type test struct {
		name string
		args args
	}
	tests := []test{
		{
			name: "test pipeline client data",
			args: args{
				clusterKey: "erda-dev",
				clientType: apistructs.ClusterManagerClientTypePipeline,
				data: apistructs.ClusterManagerClientDetail{
					"host": "localhost",
				},
			},
		},
		{
			name: "test cluster agent client data",
			args: args{
				clusterKey: "erda-dev",
				clientType: apistructs.ClusterManagerClientTypeCluster,
				data: apistructs.ClusterManagerClientDetail{
					"namespace": "erda-dev",
				},
			},
		},
		{
			name: "test empty cluster key",
			args: args{
				clusterKey: "",
				clientType: apistructs.ClusterManagerClientTypeCluster,
				data:       apistructs.ClusterManagerClientDetail{},
			},
		},
	}
	wait := sync.WaitGroup{}
	wait.Add(len(tests))
	for _, tt := range tests {
		go func(t test) {
			updateClientDetailWithEvent(t.args.clientType, t.args.clusterKey, t.args.data)
			wait.Done()
		}(tt)
	}
	wait.Wait()
	got := listClientDetailByType(apistructs.ClusterManagerClientTypePipeline)
	assert.Equal(t, got, []apistructs.ClusterManagerClientDetail{
		{
			"host": "localhost",
		},
	})
}
