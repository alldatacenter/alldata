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

package instanceinfo

import (
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/instanceinfo"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func TestQueryPodConditions_IsEmpty(t *testing.T) {
	q := &QueryPodConditions{
		Cluster:         "",
		OrgName:         "",
		OrgID:           "",
		ProjectName:     "",
		ProjectID:       "",
		ApplicationName: "",
		ApplicationID:   "",
		RuntimeName:     "",
		RuntimeID:       "",
		ServiceName:     "",
		Workspace:       "",
		ServiceType:     "",
		AddonID:         "",
		Phases:          make([]string, 0),
	}

	assert.Equal(t, true, q.IsEmpty())
}

func TestQueryInstanceConditions_IsEmpty(t *testing.T) {
	q := &QueryInstanceConditions{
		Cluster:             "",
		OrgName:             "",
		OrgID:               "",
		ProjectName:         "",
		ProjectID:           "",
		ApplicationName:     "",
		EdgeApplicationName: "",
		EdgeSite:            "",
		ApplicationID:       "",
		RuntimeName:         "",
		RuntimeID:           "",
		ServiceName:         "",
		Workspace:           "",
		ContainerID:         "",
		ServiceType:         "",
		AddonID:             "",
		InstanceIP:          "",
		HostIP:              "",
		Phases:              make([]string, 0),
		Limit:               0,
	}

	assert.Equal(t, true, q.IsEmpty())
}

func TestQueryServiceConditions_IsEmpty(t *testing.T) {
	q := &QueryServiceConditions{
		OrgName:         "",
		OrgID:           "",
		ProjectName:     "",
		ProjectID:       "",
		ApplicationName: "",
		ApplicationID:   "",
		RuntimeName:     "",
		RuntimeID:       "",
		ServiceName:     "",
		Workspace:       "",
		ServiceType:     "",
	}
	assert.Equal(t, true, q.IsEmpty())
}

func TestInstanceInfoImpl_QueryPod(t *testing.T) {
	type fields struct {
		db *instanceinfo.Client
	}
	db := &instanceinfo.Client{}
	tTime := time.Now()

	podList := make([]apistructs.PodInfoData, 0)
	podList = append(podList, apistructs.PodInfoData{
		Cluster:         "fake-cluster",
		Namespace:       "fake-cluster",
		Name:            "fake-pod01",
		OrgName:         "fake-org",
		OrgID:           "1",
		ProjectName:     "fake-proj",
		ProjectID:       "1",
		ApplicationName: "fake-app",
		ApplicationID:   "1",
		RuntimeName:     "fake-runtime",
		RuntimeID:       "1",
		ServiceName:     "fake-service",
		Workspace:       "fake-workspace",
		ServiceType:     "fake-type",
		AddonID:         "1",
		Uid:             "1",
		K8sNamespace:    "fake-namespace",
		PodName:         "fake-pod",
		Phase:           "fake-phase",
		Message:         "fake-message",
		PodIP:           "1.1.1.1",
		HostIP:          "2.2.2.2",
		StartedAt:       &tTime,
		MemRequest:      128,
		MemLimit:        256,
		CpuRequest:      0.2,
		CpuLimit:        0.4,
	})

	phase := make([]string, 0)
	phase = append(phase, "fake-phase")
	type args struct {
		cond QueryPodConditions
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    apistructs.PodInfoDataList
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				db: db,
			},
			args: args{
				cond: QueryPodConditions{
					Cluster:         "fake-cluster",
					OrgName:         "fake-org",
					OrgID:           "1",
					ProjectName:     "fake-proj",
					ProjectID:       "1",
					ApplicationName: "fake-app",
					ApplicationID:   "1",
					RuntimeName:     "fake-runtime",
					RuntimeID:       "1",
					ServiceName:     "fake-service",
					Workspace:       "fake-workspace",
					ServiceType:     "fake-type",
					AddonID:         "1",
					Phases:          phase,
					Limit:           10,
				},
			},
			want:    podList,
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &InstanceInfoImpl{
				db: tt.fields.db,
			}

			DoPatch := monkey.PatchInstanceMethod(reflect.TypeOf(i.db.PodReader()), "Do", func(*instanceinfo.PodReader) ([]instanceinfo.PodInfo, error) {
				pods := make([]instanceinfo.PodInfo, 0)
				pods = append(pods, instanceinfo.PodInfo{
					BaseModel: dbengine.BaseModel{
						ID:        1,
						CreatedAt: tTime,
						UpdatedAt: tTime,
					},
					Cluster:   "fake-cluster",
					Namespace: "fake-cluster",
					Name:      "fake-pod01",

					OrgName:         "fake-org",
					OrgID:           "1",
					ProjectName:     "fake-proj",
					ProjectID:       "1",
					ApplicationName: "fake-app",
					ApplicationID:   "1",
					RuntimeName:     "fake-runtime",
					RuntimeID:       "1",
					ServiceName:     "fake-service",
					Workspace:       "fake-workspace",
					ServiceType:     "fake-type",
					AddonID:         "1",

					Uid:          "1",
					K8sNamespace: "fake-namespace",
					PodName:      "fake-pod",

					Phase:   "fake-phase",
					Message: "fake-message",
					PodIP:   "1.1.1.1",
					HostIP:  "2.2.2.2",

					StartedAt:  &tTime,
					MemRequest: 128,
					MemLimit:   256,
					CpuRequest: 0.2,
					CpuLimit:   0.4,
				})
				return pods, nil
			})

			defer DoPatch.Unpatch()
			got, err := i.QueryPod(tt.args.cond)
			if (err != nil) != tt.wantErr {
				t.Errorf("QueryPod() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("QueryPod() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestInstanceInfoImpl_QueryInstance(t *testing.T) {
	db := &instanceinfo.Client{}
	tTime := time.Now()

	phase := make([]string, 0)
	phase = append(phase, "fake-phase")

	instanceList := make([]apistructs.InstanceInfoData, 0)
	instanceList = append(instanceList, apistructs.InstanceInfoData{
		Cluster:             "fake-cluster",
		Namespace:           "fake-cluster",
		Name:                "fake-pod01",
		OrgName:             "fake-org",
		OrgID:               "1",
		ProjectName:         "fake-proj",
		ProjectID:           "1",
		ApplicationName:     "fake-app",
		EdgeApplicationName: "fake-app",
		EdgeSite:            "fake-app",
		ApplicationID:       "1",
		RuntimeName:         "fake-runtime",
		RuntimeID:           "1",
		ServiceName:         "fake-service",
		Workspace:           "fake-workspace",
		ServiceType:         "fake-type",
		AddonID:             "1",
		Meta:                "fake-meta",
		TaskID:              "fake-taskID",
		Phase:               "fake-phase",
		Message:             "fake-message",
		ContainerID:         "fakecontainer",
		ContainerIP:         "1.1.1.1",
		HostIP:              "2.2.2.2",
		ExitCode:            0,
		CpuOrigin:           0.2,
		MemOrigin:           128,
		CpuRequest:          0.2,
		MemRequest:          128,
		CpuLimit:            0.4,
		MemLimit:            256,
		Image:               "fake-image",
		StartedAt:           tTime,
		FinishedAt:          &tTime,
	})

	type fields struct {
		db *instanceinfo.Client
	}

	type args struct {
		cond QueryInstanceConditions
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    apistructs.InstanceInfoDataList
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				db: db,
			},
			args: args{
				cond: QueryInstanceConditions{
					Cluster:             "fake-cluster",
					OrgName:             "fake-org",
					OrgID:               "1",
					ProjectName:         "fake-proj",
					ProjectID:           "1",
					ApplicationName:     "fake-app",
					EdgeApplicationName: "fake-app",
					EdgeSite:            "fake-app",
					ApplicationID:       "1",
					RuntimeName:         "fake-runtime",
					RuntimeID:           "1",
					ServiceName:         "fake-service",
					Workspace:           "fake-workspace",
					ContainerID:         "fakecontainer",
					ServiceType:         "fake-type",
					AddonID:             "1",
					InstanceIP:          "1.1.1.1",
					HostIP:              "2.2.2.2",
					Phases:              phase,
					Limit:               10,
				},
			},
			want: instanceList,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &InstanceInfoImpl{
				db: tt.fields.db,
			}

			DoPatch := monkey.PatchInstanceMethod(reflect.TypeOf(i.db.InstanceReader()), "Do", func(*instanceinfo.InstanceReader) ([]instanceinfo.InstanceInfo, error) {
				instances := make([]instanceinfo.InstanceInfo, 0)
				instances = append(instances, instanceinfo.InstanceInfo{
					BaseModel: dbengine.BaseModel{
						ID:        1,
						CreatedAt: tTime,
						UpdatedAt: tTime,
					},
					Cluster:   "fake-cluster",
					Namespace: "fake-cluster",
					Name:      "fake-pod01",

					OrgName:             "fake-org",
					OrgID:               "1",
					ProjectName:         "fake-proj",
					ProjectID:           "1",
					ApplicationName:     "fake-app",
					EdgeApplicationName: "fake-app",
					EdgeSite:            "fake-app",
					ApplicationID:       "1",
					RuntimeName:         "fake-runtime",
					RuntimeID:           "1",
					ServiceName:         "fake-service",
					Workspace:           "fake-workspace",
					ServiceType:         "fake-type",
					AddonID:             "1",

					Meta:   "fake-meta",
					TaskID: "fake-taskID",

					Phase:       "fake-phase",
					Message:     "fake-message",
					ContainerID: "fakecontainer",
					ContainerIP: "1.1.1.1",
					HostIP:      "2.2.2.2",
					StartedAt:   tTime,
					FinishedAt:  &tTime,

					ExitCode:   0,
					CpuOrigin:  0.2,
					MemOrigin:  128,
					CpuRequest: 0.2,
					MemRequest: 128,
					CpuLimit:   0.4,
					MemLimit:   256,
					Image:      "fake-image",
				})
				return instances, nil
			})

			defer DoPatch.Unpatch()

			got, err := i.QueryInstance(tt.args.cond)
			if (err != nil) != tt.wantErr {
				t.Errorf("QueryInstance() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("QueryInstance() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestInstanceInfoImpl_QueryService(t *testing.T) {

	db := &instanceinfo.Client{}
	tTime := time.Now()

	phase := make([]string, 0)
	phase = append(phase, "fake-phase")

	serviceList := make([]apistructs.ServiceInfoData, 0)
	serviceList = append(serviceList, apistructs.ServiceInfoData{
		Cluster:         "fake-cluster",
		Namespace:       "fake-cluster",
		Name:            "fake-pod01",
		OrgName:         "fake-org",
		OrgID:           "1",
		ProjectName:     "fake-proj",
		ProjectID:       "1",
		ApplicationName: "fake-app",
		ApplicationID:   "1",
		RuntimeName:     "fake-runtime",
		RuntimeID:       "1",
		ServiceName:     "fake-service",
		Workspace:       "fake-workspace",
		ServiceType:     "fake-type",

		Meta: "fake-meta",

		Phase:      "fake-phase",
		Message:    "fake-message",
		StartedAt:  tTime,
		FinishedAt: &tTime,
	})

	type fields struct {
		db *instanceinfo.Client
	}
	type args struct {
		cond QueryServiceConditions
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    apistructs.ServiceInfoDataList
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				db: db,
			},
			args: args{
				cond: QueryServiceConditions{
					OrgName:         "fake-org",
					OrgID:           "1",
					ProjectName:     "fake-proj",
					ProjectID:       "1",
					ApplicationName: "fake-app",
					ApplicationID:   "1",
					RuntimeName:     "fake-runtime",
					RuntimeID:       "1",
					ServiceName:     "fake-service",
					Workspace:       "fake-workspace",
					ServiceType:     "fake-type",
				},
			},
			want:    serviceList,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &InstanceInfoImpl{
				db: tt.fields.db,
			}

			DoPatch := monkey.PatchInstanceMethod(reflect.TypeOf(i.db.ServiceReader()), "Do", func(*instanceinfo.ServiceReader) ([]instanceinfo.ServiceInfo, error) {
				services := make([]instanceinfo.ServiceInfo, 0)
				services = append(services, instanceinfo.ServiceInfo{
					BaseModel: dbengine.BaseModel{
						ID:        1,
						CreatedAt: tTime,
						UpdatedAt: tTime,
					},
					Cluster:   "fake-cluster",
					Namespace: "fake-cluster",
					Name:      "fake-pod01",

					OrgName:         "fake-org",
					OrgID:           "1",
					ProjectName:     "fake-proj",
					ProjectID:       "1",
					ApplicationName: "fake-app",
					ApplicationID:   "1",
					RuntimeName:     "fake-runtime",
					RuntimeID:       "1",
					ServiceName:     "fake-service",
					Workspace:       "fake-workspace",
					ServiceType:     "fake-type",

					Meta: "fake-meta",

					Phase:      "fake-phase",
					Message:    "fake-message",
					StartedAt:  tTime,
					FinishedAt: &tTime,
				})
				return services, nil
			})
			defer DoPatch.Unpatch()

			got, err := i.QueryService(tt.args.cond)
			if (err != nil) != tt.wantErr {
				t.Errorf("QueryService() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("QueryService() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestInstanceInfoImpl_GetInstanceInfo(t *testing.T) {
	type fields struct {
		db *instanceinfo.Client
	}
	type args struct {
		req apistructs.InstanceInfoRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    apistructs.InstanceInfoDataList
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				db: &instanceinfo.Client{},
			},
			args: args{
				req: apistructs.InstanceInfoRequest{
					Cluster: "Fake",
				},
			},
			want:    apistructs.InstanceInfoDataList{},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &InstanceInfoImpl{
				db: tt.fields.db,
			}
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(i), "QueryInstance", func(_ *InstanceInfoImpl, cond QueryInstanceConditions) (apistructs.InstanceInfoDataList, error) {
				return apistructs.InstanceInfoDataList{}, nil
			})
			defer patch.Unpatch()

			got, err := i.GetInstanceInfo(tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetInstanceInfo() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetInstanceInfo() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestInstanceInfoImpl_GetPodInfo(t *testing.T) {
	type fields struct {
		db *instanceinfo.Client
	}
	type args struct {
		req apistructs.PodInfoRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    apistructs.PodInfoDataList
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				db: &instanceinfo.Client{},
			},
			args: args{
				req: apistructs.PodInfoRequest{
					Cluster: "Fake",
				},
			},
			want:    apistructs.PodInfoDataList{},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &InstanceInfoImpl{
				db: tt.fields.db,
			}

			patch := monkey.PatchInstanceMethod(reflect.TypeOf(i), "QueryPod", func(_ *InstanceInfoImpl, cond QueryPodConditions) (apistructs.PodInfoDataList, error) {
				return apistructs.PodInfoDataList{}, nil
			})
			defer patch.Unpatch()

			got, err := i.GetPodInfo(tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetPodInfo() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPodInfo() got = %v, want %v", got, tt.want)
			}
		})
	}
}
