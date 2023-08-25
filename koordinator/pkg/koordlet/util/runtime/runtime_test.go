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

package runtime

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"path/filepath"
	"reflect"
	"strings"
	"testing"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/runtime/handler"

	"github.com/docker/docker/api/types"
	dclient "github.com/docker/docker/client"
	"github.com/prashantv/gostub"
	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_GetRuntimeHandler(t *testing.T) {
	type args struct {
		name                 string
		endPoint             string
		flag                 string
		runtimeType          string
		expectRuntimeHandler string
		expectErr            bool
	}

	tests := []args{
		{
			name:        "test_no_runtime",
			runtimeType: "docker",
			expectErr:   true,
		},
		{
			name:        "test_/var/run/docker.sock",
			endPoint:    "/var/run/docker.sock",
			runtimeType: "containerd",
			expectErr:   true,
		},
		{
			name:        "test_have_containerdRuntime_but_need_docker",
			endPoint:    "/var/run/containerd.sock",
			runtimeType: "docker",
			expectErr:   true,
		},
		{
			name:                 "test_have_dockerRuntime_but_need_containerd",
			endPoint:             "/var/run/docker.sock",
			runtimeType:          "docker",
			expectRuntimeHandler: "DockerRuntimeHandler",
			expectErr:            false,
		},
		{
			name:                 "test_/var/run/containerd.sock",
			endPoint:             "/var/run/containerd.sock",
			runtimeType:          "containerd",
			expectRuntimeHandler: "ContainerdRuntimeHandler",
			expectErr:            false,
		},
		{
			name:                 "test_/var/run/containerd/containerd.sock",
			endPoint:             "/var/run/containerd/containerd.sock",
			runtimeType:          "containerd",
			expectRuntimeHandler: "ContainerdRuntimeHandler",
			expectErr:            false,
		},
		{
			name:                 "custom containerd",
			endPoint:             "/var/run/test1/containerd.sock",
			flag:                 "test1/containerd.sock",
			runtimeType:          "containerd",
			expectRuntimeHandler: "ContainerdRuntimeHandler",
			expectErr:            false,
		},
		{
			name:                 "custom docker",
			endPoint:             "/var/run/test2/docker.sock",
			flag:                 "test2/docker.sock",
			runtimeType:          "docker",
			expectRuntimeHandler: "DockerRuntimeHandler",
			expectErr:            false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := system.NewFileTestUtil(t)
			system.Conf.VarRunRootDir = filepath.Join(helper.TempDir, "/var/run")
			if tt.endPoint != "" {
				helper.CreateFile(tt.endPoint)
				helper.WriteFileContents(tt.endPoint, "test")
			}
			if tt.flag != "" {
				if tt.runtimeType == "containerd" {
					system.Conf.ContainerdEndPoint = filepath.Join(system.Conf.VarRunRootDir, tt.flag)
				} else if tt.runtimeType == "docker" {
					system.Conf.DockerEndPoint = filepath.Join(system.Conf.VarRunRootDir, tt.flag)
				}
			}
			resetEndpoint()

			// for docker
			dockerStubs := dockerStub()
			defer dockerStubs.Reset()

			// for containerd
			containerdStubs := gostub.Stub(&handler.GrpcDial, func(context context.Context, target string, opts ...grpc.DialOption) (*grpc.ClientConn, error) {
				return &grpc.ClientConn{}, nil
			})
			defer containerdStubs.Reset()

			gotHandler, gotErr := GetRuntimeHandler(tt.runtimeType)
			assert.Equal(t, tt.expectErr, gotErr != nil, gotErr)
			if !tt.expectErr {
				assert.True(t, strings.Contains(reflect.TypeOf(gotHandler).String(), tt.expectRuntimeHandler))
				// fmt.Printf("handler type: %v ---",reflect.TypeOf(gotHandler).String())
			}

		})
	}
}

func dockerStub() *gostub.Stubs {
	return gostub.Stub(&handler.GetDockerClient, func(httpClient *http.Client, endPoint string) (*dclient.Client, error) {
		info := func(req *http.Request) (*http.Response, error) {
			b, err := json.Marshal(types.Info{})
			if err != nil {
				return nil, err
			}
			return &http.Response{
				StatusCode: http.StatusOK,
				Body:       io.NopCloser(bytes.NewReader(b)),
			}, nil
		}
		endpoint := fmt.Sprintf("unix://%s", handler.DockerEndpoint)
		client := &http.Client{
			Transport: transportFunc(info),
		}
		dockerClient, err := dclient.NewClient(endpoint, "", client, nil)
		return dockerClient, err
	})
}

func resetEndpoint() {
	handler.DockerEndpoint = filepath.Join(system.Conf.VarRunRootDir, "docker.sock")
	handler.ContainerdEndpoint1 = filepath.Join(system.Conf.VarRunRootDir, "containerd.sock")
	handler.ContainerdEndpoint2 = filepath.Join(system.Conf.VarRunRootDir, "containerd/containerd.sock")
	DockerHandler = nil
	ContainerdHandler = nil
}

type transportFunc func(*http.Request) (*http.Response, error)

func (tf transportFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return tf(req)
}
