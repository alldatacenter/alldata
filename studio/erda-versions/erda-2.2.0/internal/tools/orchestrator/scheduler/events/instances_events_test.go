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

package events

import (
	"reflect"
	"sync"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events/eventtypes"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
)

func Test_convertInstanceStatus(t *testing.T) {
	type args struct {
		originEventStatus string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "Test_01",
			args: args{
				originEventStatus: KILLED,
			},
			want: INSTANCE_KILLED,
		},
		{
			name: "Test_02",
			args: args{
				originEventStatus: RUNNING,
			},
			want: INSTANCE_RUNNING,
		},
		{
			name: "Test_03",
			args: args{
				originEventStatus: FAILED,
			},
			want: INSTANCE_FAILED,
		},
		{
			name: "Test_04",
			args: args{
				originEventStatus: FINISHED,
			},
			want: INSTANCE_FINISHED,
		},
		{
			name: "Test_05",
			args: args{
				originEventStatus: "Test",
			},
			want: "Test",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := convertInstanceStatus(tt.args.originEventStatus); got != tt.want {
				t.Errorf("convertInstanceStatus() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_executorName2ClusterName(t *testing.T) {
	val := &executorconfig.ExecutorConfig{
		ClusterName: "Kubernetes",
	}
	conf.GetConfStore().ExecutorStore.Store("K8S", val)
	type args struct {
		executorName string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "Test_01",
			args: args{
				executorName: "MARATHON",
			},
			want: "",
		},
		{
			name: "Test_02",
			args: args{
				executorName: "K8S",
			},
			want: "Kubernetes",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := executorName2ClusterName(tt.args.executorName); got != tt.want {
				t.Errorf("executorName2ClusterName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_handleInstanceStatusChangedEvents(t *testing.T) {

	val := &executorconfig.ExecutorConfig{
		ClusterName: "Kubernetes",
	}
	conf.GetConfStore().ExecutorStore.Store("K8S", val)

	lstore := &sync.Map{}
	type args struct {
		e      *eventtypes.StatusEvent
		lstore *sync.Map
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: STATUS_UPDATE_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-773_web.e622bf15-9300-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.106",
					Status:  RUNNING,
					TaskId:  "runtimes_v1_services_staging-773_web.e622bf15-9300-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006052",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: INSTANCE_HEALTH_CHANGED_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.108",
					Status:  FAILED,
					TaskId:  "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006053",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: INSTANCE_HEALTH_CHANGED_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.108",
					Status:  HEALTHY,
					TaskId:  "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006053",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
		{
			name: "Test_04",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: INSTANCE_HEALTH_CHANGED_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.108",
					Status:  HEALTHY,
					TaskId:  "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006053",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
		{
			name: "Test_05",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: INSTANCE_HEALTH_CHANGED_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.108",
					Status:  RUNNING,
					TaskId:  "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006053",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
		{
			name: "Test_06",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: INSTANCE_HEALTH_CHANGED_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.108",
					Status:  UNHEALTHY,
					TaskId:  "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006053",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
		{
			name: "Test_07",
			args: args{
				e: &eventtypes.StatusEvent{
					Type: INSTANCE_HEALTH_CHANGED_EVENT,
					// ID Generate by {runtimeName}.{serviceName}.{dockerID}
					ID:      "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					IP:      "10.120.50.108",
					Status:  INSTANCE_FAILED,
					TaskId:  "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
					Cluster: "test",
					Host:    "node-010000006053",
					Message: "OK",
				},
				lstore: lstore,
			},
			wantErr: false,
		},
	}

	evm := GetEventManager()
	/*
		monkey.PatchInstanceMethod(reflect.TypeOf(evm.notifier), "Send", func(*NotifierImpl, interface{}, ...OpOperation) error {
			return nil
		})
	*/
	monkey.PatchInstanceMethod(reflect.TypeOf(evm.notifier), "SendRaw", func(*NotifierImpl, *Message) error {
		return nil
	})

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := handleInstanceStatusChangedEvents(tt.args.e, tt.args.lstore); (err != nil) != tt.wantErr {
				t.Errorf("handleInstanceStatusChangedEvents() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
