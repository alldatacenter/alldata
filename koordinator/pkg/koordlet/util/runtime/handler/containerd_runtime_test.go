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

package handler

import (
	"context"
	"fmt"
	"path/filepath"
	"testing"

	mock_client2 "github.com/koordinator-sh/koordinator/pkg/koordlet/util/runtime/handler/mockclient"

	"github.com/golang/mock/gomock"
	"github.com/prashantv/gostub"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_NewContainerdRuntimeHandler(t *testing.T) {
	stubs := gostub.Stub(&GrpcDial, func(context context.Context, target string, opts ...grpc.DialOption) (*grpc.ClientConn, error) {
		return &grpc.ClientConn{}, nil
	})
	defer stubs.Reset()

	helper := system.NewFileTestUtil(t)
	helper.MkDirAll("/var/run")
	helper.WriteFileContents("/var/run/containerd.sock", "test")
	system.Conf.VarRunRootDir = filepath.Join(helper.TempDir, "/var/run")
	ContainerdEndpoint1 = filepath.Join(system.Conf.VarRunRootDir, "containerd.sock")

	unixEndPoint := fmt.Sprintf("unix://%s", ContainerdEndpoint1)
	containerdRuntime, err := NewContainerdRuntimeHandler(unixEndPoint)
	assert.NoError(t, err)
	assert.NotNil(t, containerdRuntime)
}

func Test_Containerd_StopContainer(t *testing.T) {
	type args struct {
		name         string
		containerId  string
		runtimeError error
		expectError  bool
	}
	tests := []args{
		{
			name:         "test_stopContainer_success",
			containerId:  "test_container_id",
			runtimeError: nil,
			expectError:  false,
		},
		{
			name:         "test_stopContainer_fail",
			containerId:  "test_container_id",
			runtimeError: fmt.Errorf("stopContainer error"),
			expectError:  true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctl := gomock.NewController(t)
			defer ctl.Finish()
			mockRuntimeClient := mock_client2.NewMockRuntimeServiceClient(ctl)
			mockRuntimeClient.EXPECT().StopContainer(gomock.Any(), gomock.Any()).Return(nil, tt.runtimeError)

			runtimeHandler := ContainerdRuntimeHandler{runtimeServiceClient: mockRuntimeClient, timeout: 1, endpoint: ContainerdEndpoint1}
			gotErr := runtimeHandler.StopContainer(tt.containerId, 1)
			assert.Equal(t, gotErr != nil, tt.expectError)

		})
	}
}

func Test_Containerd_UpdateContainerResources(t *testing.T) {
	type args struct {
		name         string
		containerId  string
		runtimeError error
		expectError  bool
	}
	tests := []args{
		{
			name:         "test_UpdateContainerResources_success",
			containerId:  "test_container_id",
			runtimeError: nil,
			expectError:  false,
		},
		{
			name:         "test_UpdateContainerResources_fail",
			containerId:  "test_container_id",
			runtimeError: fmt.Errorf("UpdateContainerResources error"),
			expectError:  true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctl := gomock.NewController(t)
			defer ctl.Finish()
			mockRuntimeClient := mock_client2.NewMockRuntimeServiceClient(ctl)
			mockRuntimeClient.EXPECT().UpdateContainerResources(gomock.Any(), gomock.Any()).Return(nil, tt.runtimeError)

			runtimeHandler := ContainerdRuntimeHandler{runtimeServiceClient: mockRuntimeClient, timeout: 1, endpoint: ContainerdEndpoint1}
			gotErr := runtimeHandler.UpdateContainerResources(tt.containerId, UpdateOptions{})
			assert.Equal(t, tt.expectError, gotErr != nil)
		})
	}
}
