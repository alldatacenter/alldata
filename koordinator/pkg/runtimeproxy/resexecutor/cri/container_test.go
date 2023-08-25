/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cri

import (
	"testing"

	"github.com/stretchr/testify/assert"

	runtimeapi "k8s.io/cri-api/pkg/apis/runtime/v1alpha2"

	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/store"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/utils"
)

func TestContainerResourceExecutor_UpdateRequestForCreateContainerRequest(t *testing.T) {
	type fields struct {
		ContainerInfo store.ContainerInfo
	}
	type args struct {
		rsp interface{}
		req interface{}
	}
	tests := []struct {
		name                string
		fields              fields
		args                args
		wantAnnotations     map[string]string
		wantResource        *runtimeapi.LinuxContainerResources
		wantPodCgroupParent string
		wantErr             bool
	}{
		{
			name: "not compatible rsp type",
			args: args{
				rsp: &v1alpha1.PodSandboxHookResponse{},
				req: &runtimeapi.CreateContainerRequest{},
			},
			wantAnnotations:     nil,
			wantResource:        nil,
			wantPodCgroupParent: "",
			wantErr:             true,
		},
		{
			name: "normal case",
			fields: fields{
				ContainerInfo: store.ContainerInfo{
					ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
						ContainerAnnotations: map[string]string{
							"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_A": "true",
						},
						PodCgroupParent: "/kubepods/besteffort",
						ContainerResources: &v1alpha1.LinuxContainerResources{
							CpuPeriod:   1000,
							CpuShares:   500,
							OomScoreAdj: 10,
							Unified: map[string]string{
								"resourceA": "resource A",
							},
						},
					},
				},
			},
			args: args{
				req: &runtimeapi.CreateContainerRequest{
					Config: &runtimeapi.ContainerConfig{
						Linux: &runtimeapi.LinuxContainerConfig{},
					},
					SandboxConfig: &runtimeapi.PodSandboxConfig{
						Linux: &runtimeapi.LinuxPodSandboxConfig{},
					},
				},
				rsp: &v1alpha1.ContainerResourceHookResponse{
					ContainerAnnotations: map[string]string{
						"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_B": "true",
					},
					ContainerResources: &v1alpha1.LinuxContainerResources{
						CpuPeriod:   2000,
						CpuShares:   100,
						OomScoreAdj: 20,
						Unified: map[string]string{
							"resourceB": "resource B",
						},
					},
					PodCgroupParent: "/offline/besteffort",
				},
			},
			wantErr: false,
			wantResource: &runtimeapi.LinuxContainerResources{
				CpuPeriod:   2000,
				CpuShares:   100,
				OomScoreAdj: 20,
				Unified: map[string]string{
					"resourceA": "resource A",
					"resourceB": "resource B",
				},
			},
			wantAnnotations: map[string]string{
				"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_A": "true",
				"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_B": "true",
			},
			wantPodCgroupParent: "/offline/besteffort",
		},
	}
	for _, tt := range tests {
		c := &ContainerResourceExecutor{
			ContainerInfo: tt.fields.ContainerInfo,
		}
		err := c.UpdateRequest(tt.args.rsp, tt.args.req)
		assert.Equal(t, tt.wantErr, err != nil, err)
		assert.Equal(t, tt.wantResource, tt.args.req.(*runtimeapi.CreateContainerRequest).GetConfig().GetLinux().GetResources())
		assert.Equal(t, tt.wantAnnotations, tt.args.req.(*runtimeapi.CreateContainerRequest).GetConfig().GetAnnotations())
		assert.Equal(t, tt.wantPodCgroupParent, tt.args.req.(*runtimeapi.CreateContainerRequest).GetSandboxConfig().GetLinux().GetCgroupParent())
	}
}

func TestContainerResourceExecutor_UpdateRequestForUpdateContainerResourcesRequest(t *testing.T) {
	type fields struct {
		ContainerInfo store.ContainerInfo
	}
	type args struct {
		rsp interface{}
		req interface{}
	}
	tests := []struct {
		name            string
		fields          fields
		args            args
		wantAnnotations map[string]string
		wantResource    *runtimeapi.LinuxContainerResources
		wantErr         bool
	}{
		{
			name: "not compatible rsp type",
			args: args{
				rsp: &v1alpha1.PodSandboxHookResponse{},
				req: &runtimeapi.UpdateContainerResourcesRequest{},
			},
			wantAnnotations: nil,
			wantResource:    nil,
			wantErr:         true,
		},
		{
			name: "normal case",
			fields: fields{
				ContainerInfo: store.ContainerInfo{
					ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
						ContainerAnnotations: map[string]string{
							"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_A": "true",
						},
						PodCgroupParent: "/kubepods/besteffort",
						ContainerResources: &v1alpha1.LinuxContainerResources{
							CpuPeriod:   1000,
							CpuShares:   500,
							OomScoreAdj: 10,
							Unified: map[string]string{
								"resourceA": "resource A",
							},
						},
					},
				},
			},
			args: args{
				req: &runtimeapi.UpdateContainerResourcesRequest{
					Linux: &runtimeapi.LinuxContainerResources{},
				},
				rsp: &v1alpha1.ContainerResourceHookResponse{
					ContainerAnnotations: map[string]string{
						"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_B": "true",
					},
					ContainerResources: &v1alpha1.LinuxContainerResources{
						CpuPeriod:   2000,
						CpuShares:   100,
						OomScoreAdj: 20,
						Unified: map[string]string{
							"resourceB": "resource B",
						},
					},
					PodCgroupParent: "/offline/besteffort",
				},
			},
			wantErr: false,
			wantResource: &runtimeapi.LinuxContainerResources{
				CpuPeriod:   2000,
				CpuShares:   100,
				OomScoreAdj: 20,
				Unified: map[string]string{
					"resourceA": "resource A",
					"resourceB": "resource B",
				},
			},
			wantAnnotations: map[string]string{
				"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_A": "true",
				"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_UpdateRequest_B": "true",
			},
		},
	}
	for _, tt := range tests {
		c := &ContainerResourceExecutor{
			ContainerInfo: tt.fields.ContainerInfo,
		}
		err := c.UpdateRequest(tt.args.rsp, tt.args.req)
		assert.Equal(t, tt.wantErr, err != nil, err)
		assert.Equal(t, tt.wantResource, tt.args.req.(*runtimeapi.UpdateContainerResourcesRequest).GetLinux())
		assert.Equal(t, tt.wantAnnotations, tt.args.req.(*runtimeapi.UpdateContainerResourcesRequest).GetAnnotations())
	}
}

func TestContainerResourceExecutor_ResourceCheckPoint(t *testing.T) {
	type fields struct {
		ContainerInfo store.ContainerInfo
	}
	type args struct {
		rsp interface{}
	}
	tests := []struct {
		name          string
		fields        fields
		args          args
		wantErr       bool
		wantStoreInfo *store.ContainerInfo
	}{
		{
			name: "normal case - CreateContainerResponse - Set Container id successfully",
			args: args{
				rsp: &runtimeapi.CreateContainerResponse{
					ContainerId: "111111",
				},
			},
			fields: fields{
				ContainerInfo: store.ContainerInfo{
					ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
						ContainerMeta: &v1alpha1.ContainerMetadata{},
					},
				},
			},
			wantErr: false,
			wantStoreInfo: &store.ContainerInfo{
				ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
					ContainerMeta: &v1alpha1.ContainerMetadata{
						Id: "111111",
					},
				}},
		},
	}
	for _, tt := range tests {
		c := &ContainerResourceExecutor{
			ContainerInfo: tt.fields.ContainerInfo,
		}
		err := c.ResourceCheckPoint(tt.args.rsp)
		containerInfo := store.GetContainerInfo(c.ContainerInfo.ContainerMeta.GetId())
		assert.Equal(t, tt.wantErr, err != nil, err)
		assert.Equal(t, tt.wantStoreInfo, containerInfo)
	}
}

func TestContainerResourceExecutor_ParseRequest_CreateContainerRequest(t *testing.T) {
	type args struct {
		podReq       interface{}
		containerReq interface{}
	}
	tests := []struct {
		name                  string
		args                  args
		wantContainerExecutor store.ContainerInfo
		expectedOperation     utils.CallHookPluginOperation
	}{
		{
			name: "normal case",
			args: args{
				podReq: &runtimeapi.RunPodSandboxRequest{
					Config: &runtimeapi.PodSandboxConfig{
						Metadata: &runtimeapi.PodSandboxMetadata{
							Name:      "mock pod sandbox",
							Namespace: "mock namespace",
							Uid:       "202207121604",
						},
						Annotations: map[string]string{
							"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Pod": "true",
						},
						Labels: map[string]string{
							"label.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Pod": "true",
						},
						Linux: &runtimeapi.LinuxPodSandboxConfig{
							CgroupParent: "/kubepods/besteffort",
						},
					},
				},
				containerReq: &runtimeapi.CreateContainerRequest{
					PodSandboxId: "202207121604",
					Config: &runtimeapi.ContainerConfig{
						Metadata: &runtimeapi.ContainerMetadata{
							Name:    "test container",
							Attempt: 101010,
						},
						Annotations: map[string]string{
							"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Container": "true",
						},
						Labels: map[string]string{
							"label.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Container": "true",
						},
						Linux: &runtimeapi.LinuxContainerConfig{
							Resources: &runtimeapi.LinuxContainerResources{
								CpuPeriod:   1000,
								CpuShares:   500,
								OomScoreAdj: 10,
								Unified: map[string]string{
									"resourceA": "resource A",
								},
							},
						},
					},
					SandboxConfig: &runtimeapi.PodSandboxConfig{
						Linux: &runtimeapi.LinuxPodSandboxConfig{
							CgroupParent: "/kubepods/besteffort",
						},
					},
				},
			},
			wantContainerExecutor: store.ContainerInfo{
				ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
					PodMeta: &v1alpha1.PodSandboxMetadata{
						Name:      "mock pod sandbox",
						Namespace: "mock namespace",
						Uid:       "202207121604",
					},
					PodLabels: map[string]string{
						"label.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Pod": "true",
					},
					PodAnnotations: map[string]string{
						"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Pod": "true",
					},
					ContainerMeta: &v1alpha1.ContainerMetadata{
						Name:    "test container",
						Attempt: 101010,
					},
					ContainerAnnotations: map[string]string{
						"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Container": "true",
					},
					ContainerResources: &v1alpha1.LinuxContainerResources{
						CpuPeriod:   1000,
						CpuShares:   500,
						OomScoreAdj: 10,
						Unified: map[string]string{
							"resourceA": "resource A",
						},
					},
					PodCgroupParent: "/kubepods/besteffort",
					ContainerEnvs:   map[string]string{},
				},
			},
			expectedOperation: utils.ShouldCallHookPlugin,
		},
	}
	for _, tt := range tests {
		// mock pod cache
		p := NewPodResourceExecutor()
		operation, _ := p.ParseRequest(tt.args.podReq)
		_ = store.WritePodSandboxInfo("202207121604", &p.PodSandboxInfo)
		assert.Equal(t, tt.expectedOperation, operation, tt.name)

		// write container cache
		c := NewContainerResourceExecutor()
		operation, _ = c.ParseRequest(tt.args.containerReq)

		// check if container cache is set correctly
		assert.Equal(t, tt.expectedOperation, operation, tt.name)
		assert.Equal(t, tt.wantContainerExecutor, c.ContainerInfo, tt.name)
	}
}

func TestContainerResourceExecutor_ParseRequest_UpdateContainerResourcesRequest(t *testing.T) {
	type args struct {
		containerID               string
		containerReq              interface{}
		ExistingContainerExecutor store.ContainerInfo
	}
	tests := []struct {
		name              string
		args              args
		wantContainerInfo store.ContainerInfo
	}{
		{
			name: "normal case",
			args: args{
				containerID: "10101010",
				containerReq: &runtimeapi.UpdateContainerResourcesRequest{
					ContainerId: "10101010",
					Linux: &runtimeapi.LinuxContainerResources{
						CpusetCpus: "0-31",
					},
				},
				ExistingContainerExecutor: store.ContainerInfo{
					ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
						PodMeta: &v1alpha1.PodSandboxMetadata{
							Name:      "mock pod sandbox",
							Namespace: "mock namespace",
							Uid:       "202207121604",
						},
						ContainerMeta: &v1alpha1.ContainerMetadata{
							Name:    "test container",
							Attempt: 101010,
						},
						ContainerAnnotations: map[string]string{
							"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Container": "true",
						},
						ContainerResources: &v1alpha1.LinuxContainerResources{
							CpuPeriod:   1000,
							CpuShares:   500,
							OomScoreAdj: 10,
							Unified: map[string]string{
								"resourceA": "resource A",
							},
						},
						PodCgroupParent: "/kubepods/besteffort",
					},
				},
			},
			wantContainerInfo: store.ContainerInfo{
				ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
					PodMeta: &v1alpha1.PodSandboxMetadata{
						Name:      "mock pod sandbox",
						Namespace: "mock namespace",
						Uid:       "202207121604",
					},
					ContainerMeta: &v1alpha1.ContainerMetadata{
						Name:    "test container",
						Attempt: 101010,
					},
					ContainerAnnotations: map[string]string{
						"annotation.dummy.koordinator.sh/TestContainerResourceExecutor_ParseRequest_CreateContainerRequest_Container": "true",
					},
					ContainerResources: &v1alpha1.LinuxContainerResources{
						CpuPeriod:   1000,
						CpuShares:   500,
						OomScoreAdj: 10,
						CpusetCpus:  "0-31",
						Unified: map[string]string{
							"resourceA": "resource A",
						},
					},
					PodCgroupParent: "/kubepods/besteffort",
				},
			},
		},
	}
	for _, tt := range tests {
		c := NewContainerResourceExecutor()
		// mock container cache
		_ = store.WriteContainerInfo(tt.args.containerID, &tt.args.ExistingContainerExecutor)
		_, _ = c.ParseRequest(tt.args.containerReq)

		// check if container cache is set correctly
		assert.Equal(t, tt.wantContainerInfo, c.ContainerInfo)
	}
}

func TestContainerResourceExecutor_ParseContainer(t *testing.T) {
	tests := []struct {
		name              string
		container         *runtimeapi.Container
		podSandboxID      string
		pod               *store.PodSandboxInfo // this is the pod in store belonging to this container
		containerInternal *store.ContainerInfo
	}{
		{
			name: "container failover normal",
			container: &runtimeapi.Container{
				PodSandboxId: "podSandboxID0",
				Annotations: map[string]string{
					"containerAnnotationKey1": "containerAnnotationValue1",
				},
				Metadata: &runtimeapi.ContainerMetadata{
					Name:    "container",
					Attempt: 2,
				},
			},
			podSandboxID: "podSandboxID0",
			pod: &store.PodSandboxInfo{
				PodSandboxHookRequest: &v1alpha1.PodSandboxHookRequest{
					PodMeta: &v1alpha1.PodSandboxMetadata{
						Name: "podName",
					},
					Annotations: map[string]string{
						"annotationKey1": "annotationValue1",
					},
					Labels: map[string]string{
						"labelsKey1": "labelsValue1",
					},
				},
			},
			containerInternal: &store.ContainerInfo{
				ContainerResourceHookRequest: &v1alpha1.ContainerResourceHookRequest{
					ContainerAnnotations: map[string]string{
						"containerAnnotationKey1": "containerAnnotationValue1",
					},
					ContainerMeta: &v1alpha1.ContainerMetadata{
						Name:    "container",
						Attempt: 2,
					},
					PodMeta: &v1alpha1.PodSandboxMetadata{
						Name: "podName",
					},
					PodAnnotations: map[string]string{
						"annotationKey1": "annotationValue1",
					},
					PodLabels: map[string]string{
						"labelsKey1": "labelsValue1",
					},
				},
			},
		},
	}

	for _, tt := range tests {
		containerExecutor := NewContainerResourceExecutor()
		store.WritePodSandboxInfo(tt.podSandboxID, tt.pod)
		containerExecutor.ParseContainer(tt.container)
		assert.Equal(t, tt.containerInternal, &containerExecutor.ContainerInfo, tt.name)
	}
}
