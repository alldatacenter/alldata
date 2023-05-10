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

package endpoints

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"reflect"
	"strings"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cap"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/job"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/labelmanager"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/resourceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func generateServiceGroupCreateV2Request() apistructs.ServiceGroupCreateV2Request {
	services := make(map[string]*diceyml.Service)
	services["mysql-1"] = &diceyml.Service{
		Image: "registry.erda.cloud/erda-addons-enterprise/addon-mysql:5.7.29-1.0.1-init",
		Ports: make([]diceyml.ServicePort, 0),
		Envs:  make(map[string]string),
		Resources: diceyml.Resources{
			CPU: 1,
			Mem: 4301,
		},
		Labels:      make(map[string]string),
		Annotations: make(map[string]string),
		Binds:       make([]string, 0),
		Deployments: diceyml.Deployments{
			Replicas: 1,
		},
	}

	services["mysql-1"].Ports = append(services["mysql-1"].Ports, diceyml.ServicePort{
		Port:       3306,
		Protocol:   "TCP",
		L4Protocol: "TCP",
		Expose:     false,
		Default:    false,
	})

	services["mysql-1"].Envs = diceyml.EnvMap{
		"ADDON_GROUPS":        "2",
		"ADDON_ID":            "z44f5f6543f004d54ac2a2538efd4e9ec",
		"ADDON_NODE_ID":       "f54fc4ff4197e4c4fa1cdc5b929ca5849",
		"ADDON_TYPE":          "mysql",
		"DICE_ADDON":          "z44f5f6543f004d54ac2a2538efd4e9ec",
		"DICE_ADDON_TYPE":     "mysql",
		"DICE_CLUSTER_NAME":   "test",
		"MYSQL_ROOT_PASSWORD": "cR7yf6zEBVFQ8WgE",
		"SERVER_ID":           "1",
		"SERVICE_TYPE":        "ADDONS",
	}

	services["mysql-1"].Labels = map[string]string{
		"ADDON_GROUP_ID": "mysql-master",
	}

	services["mysql-1"].Binds = []string{
		"/netdata/addon/mysql/backup/z44f5f6543f004d54ac2a2538efd4e9ec_1:/var/backup/mysql:rw",
		"z44f5f6543f004d54ac2a2538efd4e9ec_1:/var/lib/mysql:rw",
	}
	services["mysql-1"].HealthCheck = diceyml.HealthCheck{
		Exec: &diceyml.ExecCheck{Cmd: fmt.Sprintf("mysql -uroot -p%s  -e 'select 1'", "xxxxxx")},
	}

	req := apistructs.ServiceGroupCreateV2Request{
		DiceYml: diceyml.Object{
			Version:  "2.0",
			Services: services,
		},
		ClusterName: "test",
		ID:          "z44f5f6543f004d54ac2a2538efd4e9ec",
		Type:        strings.Join([]string{"addon-", strings.Replace(strings.Replace("mysql", "terminus-", "", 1), "-operator", "", 1)}, ""),
		GroupLabels: make(map[string]string),
		Volumes:     make(map[string]apistructs.RequestVolumeInfo),
	}

	volumes := make(map[string]apistructs.RequestVolumeInfo)
	volumes["mysql-1"] = apistructs.RequestVolumeInfo{
		ID:            "101",
		Type:          "local",
		ContainerPath: "/opt/test",
	}
	req.Volumes = volumes

	return req
}

func TestHTTPEndpoints_ServiceGroupCreate(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	req := generateServiceGroupCreateV2Request()

	reqStr, _ := json.Marshal(req)
	httpReq, _ := http.NewRequest(http.MethodPost, "/api/servicegroup", bytes.NewBuffer(reqStr))

	reqStr, _ = json.Marshal(req)
	httpReq2, _ := http.NewRequest(http.MethodPost, "/api/servicegroup", bytes.NewBuffer(reqStr))

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: &http.Request{
					Method: http.MethodPost,
					Body:   httpReq.Body,
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupCreateV2Response{
					Header: apistructs.Header{Success: true},
					Data: apistructs.ServiceGroupCreateV2Data{
						ID:   "xxxxxtestsg",
						Type: "testsg",
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: &http.Request{
					Method: http.MethodPost,
					Body:   httpReq2.Body,
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupCreateV2Response{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "create servicegroup fail: failed"},
					}},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.serviceGroupImpl), "Create", func(_ *servicegroup.ServiceGroupImpl, sg apistructs.ServiceGroupCreateV2Request) (apistructs.ServiceGroup, error) {
				if tt.name == "Test_01" {
					return apistructs.ServiceGroup{
						Dice: apistructs.Dice{
							ID:   "xxxxxtestsg",
							Type: "testsg",
						},
					}, nil
				}

				return apistructs.ServiceGroup{}, errors.Errorf("failed")
			})

			defer patch1.Unpatch()
			got, err := h.ServiceGroupCreate(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ServiceGroupCreate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServiceGroupCreate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_ServiceGroupDelete(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	httpReq1, _ := http.NewRequest(http.MethodDelete, "/api/servicegroup?namespace=test&name=test&force=false", nil)
	httpReq2, _ := http.NewRequest(http.MethodDelete, "/api/servicegroup?name=test&force=false", nil)

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Test_01",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupDeleteV2Response{
					apistructs.Header{Success: true}},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupDeleteV2Response{
					apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "empty namespace or name"},
					}},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupDeleteV2Response{
					apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "delete servicegroup fail: failed"},
					}},
			},
			wantErr: false,
		},
		{
			name: "Test_04",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupDeleteV2Response{
					apistructs.Header{
						Success: false,
						Error: apistructs.ErrorResponse{
							Code: "404",
							Msg:  "delete servicegroup fail: not found",
						},
					}},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.serviceGroupImpl), "Delete", func(_ *servicegroup.ServiceGroupImpl, namespace string, name, force string) error {
				if tt.name == "Test_01" || tt.name == "Test_02" {
					return nil
				}
				if tt.name == "Test_03" {
					return errors.Errorf("failed")
				}

				return errors.New("not found")
			})
			defer patch1.Unpatch()

			got, err := h.ServiceGroupDelete(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ServiceGroupDelete() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServiceGroupDelete() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_ServiceGroupInfo(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	httpReq1, _ := http.NewRequest(http.MethodDelete, "/api/servicegroup?namespace=test&name=test", nil)
	httpReq2, _ := http.NewRequest(http.MethodDelete, "/api/servicegroup?name=test", nil)

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Test_01",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupInfoResponse{
					apistructs.Header{Success: true},
					apistructs.ServiceGroup{
						Dice: apistructs.Dice{
							ID:   "xxxxxtestsg",
							Type: "testsg",
						},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupInfoResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "empty namespace or name"},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupInfoResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "get servicegroup info fail: failed"},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.serviceGroupImpl), "Info", func(_ *servicegroup.ServiceGroupImpl, ctx context.Context, namespace string, name string) (apistructs.ServiceGroup, error) {
				if tt.name == "Test_01" {
					return apistructs.ServiceGroup{
						Dice: apistructs.Dice{
							ID:   "xxxxxtestsg",
							Type: "testsg",
						},
					}, nil
				}

				return apistructs.ServiceGroup{}, errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.ServiceGroupInfo(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ServiceGroupInfo() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServiceGroupInfo() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_ServiceGroupPrecheck(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}
	req := generateServiceGroupCreateV2Request()

	reqStr, _ := json.Marshal(req)
	httpReq, _ := http.NewRequest(http.MethodPut, "/api/servicegroup", bytes.NewBuffer(reqStr))

	reqStr, _ = json.Marshal(req)
	httpReq2, _ := http.NewRequest(http.MethodPut, "/api/servicegroup", bytes.NewBuffer(reqStr))

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: &http.Request{
					Method: http.MethodPost,
					Body:   httpReq.Body,
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupPrecheckResponse{
					Header: apistructs.Header{Success: true},
					Data: apistructs.ServiceGroupPrecheckData{
						Nodes:  nil,
						Status: "",
						Info:   "OK",
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				r: &http.Request{
					Method: http.MethodPost,
					Body:   httpReq2.Body,
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupPrecheckResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "precheck servicegroup fail: failed"},
					},
				},
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			//sg apistructs.ServiceGroupPrecheckRequest) (apistructs.ServiceGroupPrecheckData, error)
			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.serviceGroupImpl), "Precheck", func(_ *servicegroup.ServiceGroupImpl, sg apistructs.ServiceGroupPrecheckRequest) (apistructs.ServiceGroupPrecheckData, error) {
				if tt.name == "Test_01" {
					return apistructs.ServiceGroupPrecheckData{
						Nodes:  nil,
						Status: "",
						Info:   "OK",
					}, nil
				}

				return apistructs.ServiceGroupPrecheckData{}, errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.ServiceGroupPrecheck(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ServiceGroupPrecheck() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServiceGroupPrecheck() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_ServiceGroupKillPod(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	req1 := apistructs.ServiceGroupKillPodRequest{
		Namespace: "namespace-test",
		Name:      "name-test",
		PodName:   "pod-test",
	}
	req2 := apistructs.ServiceGroupKillPodRequest{
		Name:    "name-test",
		PodName: "pod-test",
	}
	ctx := context.Background()
	reqStr1, _ := json.Marshal(req1)
	httpReq1, _ := http.NewRequest(http.MethodPost, "/api/servicegroup/actions/killpod", bytes.NewBuffer(reqStr1))

	reqStr2, _ := json.Marshal(req2)
	httpReq2, _ := http.NewRequest(http.MethodPost, "/api/servicegroup/actions/killpod", bytes.NewBuffer(reqStr2))

	httpReq3, _ := http.NewRequest(http.MethodPost, "/api/servicegroup/actions/killpod", bytes.NewBuffer(reqStr1))

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				ctx: ctx,
				r:   httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupKillPodResponse{
					Header: apistructs.Header{
						Success: true,
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				ctx: ctx,
				r:   httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupKillPodResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "empty namespace or name or containerID"},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				ctx: ctx,
				r:   httpReq3,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupKillPodResponse{
					Header: apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "failed"},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.serviceGroupImpl), "KillPod", func(_ *servicegroup.ServiceGroupImpl, ctx context.Context, namespace string, name string, podname string) error {
				if tt.name == "Test_01" {
					return nil
				}

				return errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.ServiceGroupKillPod(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ServiceGroupKillPod() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServiceGroupKillPod() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_ServiceGroupConfigUpdate(t *testing.T) {
	type fields struct {
		volumeImpl        volume.Volume
		serviceGroupImpl  servicegroup.ServiceGroup
		clusterImpl       cluster.Cluster
		job               job.Job
		labelManager      labelmanager.LabelManager
		instanceinfoImpl  instanceinfo.InstanceInfo
		clusterinfoImpl   clusterinfo.ClusterInfo
		componentinfoImpl instanceinfo.ComponentInfo
		resourceinfoImpl  resourceinfo.ResourceInfo
		Cap               cap.Cap
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	ctx := context.Background()

	req1 := apistructs.ServiceGroup{
		Dice: apistructs.Dice{
			Type: "testType",
			ID:   "testId",
		},
	}
	reqStr1, _ := json.Marshal(req1)
	httpReq1, _ := http.NewRequest(http.MethodPut, "/api/servicegroup/actions/config", bytes.NewBuffer(reqStr1))

	req2 := apistructs.ServiceGroup{
		Dice: apistructs.Dice{
			ID: "testId",
		},
	}
	reqStr2, _ := json.Marshal(req2)
	httpReq2, _ := http.NewRequest(http.MethodPut, "/api/servicegroup/actions/config", bytes.NewBuffer(reqStr2))
	httpReq3, _ := http.NewRequest(http.MethodPut, "/api/servicegroup/actions/config", bytes.NewBuffer(reqStr1))

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				ctx: ctx,
				r:   httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupConfigUpdateResponse{
					apistructs.Header{Success: true},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				ctx: ctx,
				r:   httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupConfigUpdateResponse{
					apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "empty namespace or name"},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				serviceGroupImpl: &servicegroup.ServiceGroupImpl{},
			},
			args: args{
				ctx: ctx,
				r:   httpReq3,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.ServiceGroupConfigUpdateResponse{
					apistructs.Header{
						Success: false,
						Error:   apistructs.ErrorResponse{Msg: "configupdate servicegroup fail: failed"},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			h := &HTTPEndpoints{
				volumeImpl:        tt.fields.volumeImpl,
				ServiceGroupImpl:  tt.fields.serviceGroupImpl,
				clusterImpl:       tt.fields.clusterImpl,
				Job:               tt.fields.job,
				labelManager:      tt.fields.labelManager,
				instanceinfoImpl:  tt.fields.instanceinfoImpl,
				ClusterinfoImpl:   tt.fields.clusterinfoImpl,
				componentinfoImpl: tt.fields.componentinfoImpl,
				resourceinfoImpl:  tt.fields.resourceinfoImpl,
				Cap:               tt.fields.Cap,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.serviceGroupImpl), "ConfigUpdate", func(_ *servicegroup.ServiceGroupImpl, sg apistructs.ServiceGroup) error {
				if tt.name == "Test_01" {
					return nil
				}

				return errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.ServiceGroupConfigUpdate(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("ServiceGroupConfigUpdate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServiceGroupConfigUpdate() got = %v, want %v", got, tt.want)
			}
		})
	}
}
