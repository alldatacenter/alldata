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
	"os"
	"reflect"
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
)

func TestHTTPEndpoints_JobCreate(t *testing.T) {
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
	jobImpl := &job.JobImpl{}
	req1 := apistructs.JobCreateRequest{
		Name: "testjob",
	}
	reqStr1, _ := json.Marshal(req1)
	httpReq1, _ := http.NewRequest(http.MethodPut, "/v1/job/create", bytes.NewBuffer(reqStr1))
	httpReq2, _ := http.NewRequest(http.MethodPut, "/v1/job/create", bytes.NewBuffer(reqStr1))

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
				job: jobImpl,
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.JobCreateResponse{
					Name: "testjob",
					Job: apistructs.Job{
						JobFromUser: apistructs.JobFromUser{
							Name: "testjob",
						},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				r: httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusBadRequest,
				Content: apistructs.JobCreateResponse{
					Error: "failed to create job: failed",
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

			os.Setenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE, "true")
			os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
			defer os.Unsetenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.job), "Create", func(*job.JobImpl, apistructs.JobCreateRequest) (apistructs.Job, error) {
				if tt.name == "Test_01" {
					return apistructs.Job{
						JobFromUser: apistructs.JobFromUser{
							Name: "testjob",
						},
					}, nil
				}

				return apistructs.Job{}, errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.JobCreate(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("JobCreate() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("JobCreate() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_JobStart(t *testing.T) {
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

	jobImpl := &job.JobImpl{}
	req1 := apistructs.JobCreateRequest{
		Name: "testjob",
	}
	reqStr1, _ := json.Marshal(req1)
	httpReq1, _ := http.NewRequest(http.MethodPost, "/v1/job/create", bytes.NewBuffer(reqStr1))
	httpReq2, _ := http.NewRequest(http.MethodPost, "/v1/job/create", bytes.NewBuffer(reqStr1))
	httpReq3, _ := http.NewRequest(http.MethodPost, "/v1/job/create", bytes.NewBuffer(reqStr1))

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
				job: jobImpl,
			},
			args: args{
				r: httpReq1,
				vars: map[string]string{
					"namespace": "test-namespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.JobStartResponse{
					Name: "testjob",
					Job: apistructs.Job{
						JobFromUser: apistructs.JobFromUser{
							Name: "testjob",
						},
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				r: httpReq2,
				vars: map[string]string{
					"name": "test-name",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusBadRequest,
				Content: apistructs.JobStartResponse{
					Error: "failed to start job, empty name or namespace",
				},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				r: httpReq3,
				vars: map[string]string{
					"namespace": "test-namespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusInternalServerError,
				Content: apistructs.JobStartResponse{
					Error: "failed to start job, err: failed",
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

			os.Setenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE, "true")
			os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
			defer os.Unsetenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.job), "Start", func(_ *job.JobImpl, namespace, name string, env map[string]string) (apistructs.Job, error) {
				if tt.name == "Test_01" {
					return apistructs.Job{
						JobFromUser: apistructs.JobFromUser{
							Name: "testjob",
						},
					}, nil
				}

				return apistructs.Job{}, errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.JobStart(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("JobStart() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("JobStart() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_JobStop(t *testing.T) {
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

	jobImpl := &job.JobImpl{}
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
				job: jobImpl,
			},
			args: args{
				vars: map[string]string{
					"namespace": "test-namespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.JobStopResponse{
					Name: "testjob",
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				vars: map[string]string{
					"name": "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusBadRequest,
				Content: apistructs.JobStopResponse{
					Error: "failed to stop job, empty name or namespace",
				},
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				vars: map[string]string{
					"namespace": "test-namespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusBadRequest,
				Content: apistructs.JobStopResponse{
					Error: "failed to stop job, err: failed",
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

			os.Setenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE, "test-namespace")
			os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
			defer os.Unsetenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.job), "Stop", func(_ *job.JobImpl, namespace, name string) error {
				if tt.name == "Test_01" {
					return nil
				}

				return errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.JobStop(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("JobStop() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("JobStop() got = %#v, want %#v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_JobDelete(t *testing.T) {
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

	jobImpl := &job.JobImpl{}

	req1 := apistructs.Job{
		JobFromUser: apistructs.JobFromUser{
			Name:      "testjob",
			Namespace: "testnamespace",
		},
	}
	reqStr1, _ := json.Marshal(req1)
	httpReq1, _ := http.NewRequest(http.MethodDelete, fmt.Sprintf("/v1/job/%s/%s/delete", req1.Namespace, req1.Name), bytes.NewBuffer(reqStr1))
	httpReq2, _ := http.NewRequest(http.MethodDelete, fmt.Sprintf("/v1/job/%s/%s/delete", req1.Namespace, req1.Name), bytes.NewBuffer(reqStr1))

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
				job: jobImpl,
			},
			args: args{
				r: httpReq1,
				vars: map[string]string{
					"namespace": "test-namespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.JobDeleteResponse{
					Name:      "testjob",
					Namespace: "test-namespace",
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				r: httpReq2,
				vars: map[string]string{
					"name": "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusBadRequest,
				Content: apistructs.JobDeleteResponse{
					Name:      "testjob",
					Namespace: "test-namespace",
					Error:     "failed to delete job, err: failed",
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

			os.Setenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE, "test-namespace")
			os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
			defer os.Unsetenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.job), "Delete", func(_ *job.JobImpl, job apistructs.Job) error {
				if tt.name == "Test_01" {
					return nil
				}

				return errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.JobDelete(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("JobDelete() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("JobDelete() got = %#v, want %#v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_DeleteJobs(t *testing.T) {
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

	jobImpl := &job.JobImpl{}

	req := []apistructs.Job{
		{
			JobFromUser: apistructs.JobFromUser{
				Name:      "testjob1",
				Namespace: "testnamespace",
			},
		},
		{
			JobFromUser: apistructs.JobFromUser{
				Name:      "testjob2",
				Namespace: "testnamespace",
			},
		},
	}

	reqStr1, _ := json.Marshal(req)
	httpReq1, _ := http.NewRequest(http.MethodDelete, "/v1/jobs", bytes.NewBuffer(reqStr1))
	httpReq2, _ := http.NewRequest(http.MethodDelete, "/v1/jobs", bytes.NewBuffer(reqStr1))

	list := []apistructs.JobDeleteResponse{
		{
			Name:      "testjob1",
			Namespace: "testnamespace",
			Error:     "failed to delete job testjob1 in ns testnamespace, err: failed",
		},
		{
			Name:      "testjob2",
			Namespace: "testnamespace",
			Error:     "failed to delete job testjob2 in ns testnamespace, err: failed",
		},
	}

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
				job: jobImpl,
			},
			args: args{
				r: httpReq1,
			},
			want: httpserver.HTTPResponse{
				Status:  http.StatusOK,
				Content: apistructs.JobsDeleteResponse{},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				r: httpReq2,
			},
			want: httpserver.HTTPResponse{
				Status:  http.StatusBadRequest,
				Content: apistructs.JobsDeleteResponse(list),
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

			os.Setenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE, "testnamespace")
			os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
			defer os.Unsetenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.job), "Delete", func(_ *job.JobImpl, job apistructs.Job) error {
				if tt.name == "Test_01" {
					return nil
				}

				return errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.DeleteJobs(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("DeleteJobs() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("DeleteJobs() got = %#v, want %#v", got, tt.want)
			}
		})
	}
}

func TestHTTPEndpoints_JobInspect(t *testing.T) {
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

	jobImpl := &job.JobImpl{}

	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}
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
				job: jobImpl,
			},
			args: args{
				vars: map[string]string{
					"namespace": "testnamespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: apistructs.Job{
					JobFromUser: apistructs.JobFromUser{
						Name:      "testjob",
						Namespace: "testnamespace",
					},
				},
			},
			wantErr: false,
		},
		{
			name: "Test_02",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				vars: map[string]string{
					"name": "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status:  http.StatusBadRequest,
				Content: "failed to inspect job, empty name or namespace",
			},
			wantErr: false,
		},
		{
			name: "Test_03",
			fields: fields{
				job: jobImpl,
			},
			args: args{
				vars: map[string]string{
					"namespace": "testnamespace",
					"name":      "testjob",
				},
			},
			want: httpserver.HTTPResponse{
				Status:  http.StatusInternalServerError,
				Content: "failed to inspect job, err: failed",
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

			os.Setenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE, "testnamespace")
			os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
			defer os.Unsetenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(tt.fields.job), "Inspect", func(_ *job.JobImpl, namespace, name string) (apistructs.Job, error) {
				if tt.name == "Test_01" {
					return apistructs.Job{
						JobFromUser: apistructs.JobFromUser{
							Namespace: "testnamespace",
							Name:      "testjob",
						},
					}, nil
				}

				return apistructs.Job{}, errors.Errorf("failed")
			})
			defer patch1.Unpatch()

			got, err := h.JobInspect(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("JobInspect() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("JobInspect() got = %#v, want %#v", got, tt.want)
			}
		})
	}
}
