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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestComputeServiceStatus(t *testing.T) {
	ev := RuntimeEvent{
		RuntimeName: "whatever",
		ServiceStatuses: []ServiceStatus{
			{
				ServiceName: "x1",
				Replica:     0,
			},
			{
				ServiceName: "x2",
				Replica:     1,
				InstanceStatuses: []InstanceStatus{
					{
						ID:             "XX12",
						Ip:             "10.11.12.13",
						InstanceStatus: HEALTHY,
					},
				},
			},
			{
				ServiceName: "x3",
				Replica:     2,
				InstanceStatuses: []InstanceStatus{
					{
						ID:             "XX23",
						Ip:             "11.12.13.14",
						InstanceStatus: RUNNING,
					},
					{
						ID:             "XX24",
						Ip:             "12.13.14.15",
						InstanceStatus: HEALTHY,
					},
				},
			},
			{
				ServiceName: "x4",
				Replica:     0,
				InstanceStatuses: []InstanceStatus{
					{
						ID:             "y1",
						Ip:             "110.12.0.14",
						InstanceStatus: INSTANCE_RUNNING,
					},
					{
						ID:             "y2",
						Ip:             "110.13.0.15",
						InstanceStatus: INSTANCE_RUNNING,
					},
				},
			},
		},
	}

	computeServiceStatus(&ev)
	assert.Equal(t, "Healthy", ev.ServiceStatuses[0].ServiceStatus)
	assert.Equal(t, "Healthy", ev.ServiceStatuses[1].ServiceStatus)
	assert.Equal(t, "UnHealthy", ev.ServiceStatuses[2].ServiceStatus)
	assert.Equal(t, "UnHealthy", ev.ServiceStatuses[3].ServiceStatus)
}

func Test_getLayerInfoFromEvent(t *testing.T) {
	type args struct {
		id        string
		eventType string
	}
	tests := []struct {
		name    string
		args    args
		want    *EventLayer
		wantErr bool
	}{
		{
			name: "test_01",
			args: args{
				id:        "runtimes_v1_services_staging-773_web.e622bf15-9300-11e8-ad54-70b3d5800001",
				eventType: STATUS_UPDATE_EVENT,
			},
			want: &EventLayer{
				InstanceId:  "e622bf15-9300-11e8-ad54-70b3d5800001",
				ServiceName: "web",
				RuntimeName: "services/staging-773",
			},
			wantErr: false,
		},
		{
			name: "test_02",
			args: args{
				id:        "runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
				eventType: INSTANCE_HEALTH_CHANGED_EVENT,
			},
			want: &EventLayer{
				InstanceId:  "0ad6d3ce-946c-11e8-ad54-70b3d5800001",
				ServiceName: "web",
				RuntimeName: "services/staging-790",
			},
			wantErr: false,
		},
		{
			name: "test_03",
			args: args{
				id:        "runtimes_v1_services.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001",
				eventType: INSTANCE_HEALTH_CHANGED_EVENT,
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getLayerInfoFromEvent(tt.args.id, tt.args.eventType)
			if (err != nil) != tt.wantErr {
				t.Errorf("getLayerInfoFromEvent() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getLayerInfoFromEvent() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestNew(t *testing.T) {
	type args struct {
		sender string
		dest   map[string]interface{}
	}
	tests := []struct {
		name    string
		args    args
		want    Notifier
		wantErr bool
	}{
		{
			name: "Test_01",
			args: args{
				sender: "",
				dest:   nil,
			},
			want: &NotifierImpl{
				sender: "",
				labels: nil,
				dir:    MessageDir,
				js:     js,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := New(tt.args.sender, tt.args.dest)
			if (err != nil) != tt.wantErr {
				t.Errorf("New() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("New() got = %v, want %v", got, tt.want)
			}
		})
	}
}
