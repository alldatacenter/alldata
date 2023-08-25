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
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"path/filepath"
	"strings"
	"testing"

	"github.com/docker/docker/api"
	"github.com/docker/docker/api/types/container"
	dclient "github.com/docker/docker/client"
	"github.com/prashantv/gostub"
	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_Docker_NewDockerRuntimeHandler(t *testing.T) {
	stubs := gostub.Stub(&GetDockerClient, func(httpClient *http.Client, endPoint string) (*dclient.Client, error) {
		ping := func(req *http.Request) (*http.Response, error) {
			return &http.Response{
				StatusCode: http.StatusOK,
				Body:       io.NopCloser(bytes.NewReader([]byte(""))),
			}, nil
		}
		return createDockerClient(newMockClient(ping), fmt.Sprintf("unix://%s", DockerEndpoint))
	})
	defer stubs.Reset()

	helper := system.NewFileTestUtil(t)
	helper.MkDirAll("/var/run")
	system.Conf.VarRunRootDir = filepath.Join(helper.TempDir, "/var/run")
	helper.WriteFileContents("/var/run/docker.sock", "test")
	DockerEndpoint = filepath.Join(system.Conf.VarRunRootDir, "docker.sock")

	unixEndPoint := fmt.Sprintf("unix://%s", DockerEndpoint)
	dockerRuntime, err := NewDockerRuntimeHandler(unixEndPoint)
	assert.NoError(t, err)
	assert.NotNil(t, dockerRuntime)
}

func Test_Docker_StopContainer(t *testing.T) {
	helper := system.NewFileTestUtil(t)
	helper.MkDirAll("/var/run")
	helper.WriteFileContents("/var/run/docker.sock", "test")
	system.Conf.VarRunRootDir = filepath.Join(helper.TempDir, "/var/run")
	DockerEndpoint = filepath.Join(system.Conf.VarRunRootDir, "docker.sock")

	// for a higher version of docker client like 20.10, client version cannot be omitted in request URL
	expectedURL := "/v" + api.DefaultVersion + "/containers/test_stop_container/stop"
	stopContainer := func(req *http.Request) (*http.Response, error) {
		if !strings.HasPrefix(req.URL.Path, expectedURL) {
			return nil, fmt.Errorf("expected URL '%s', got '%s'", expectedURL, req.URL)
		}
		return &http.Response{
			StatusCode: http.StatusOK,
			Body:       io.NopCloser(bytes.NewReader([]byte(""))),
		}, nil
	}
	endPoint := fmt.Sprintf("unix://%s", DockerEndpoint)
	dockerClient, err := createDockerClient(newMockClient(stopContainer), endPoint)
	assert.NoError(t, err)
	dockerRuntimeHandler := DockerRuntimeHandler{endpoint: endPoint, dockerClient: dockerClient}
	err = dockerRuntimeHandler.StopContainer("test_stop_container", 5)
	assert.NoError(t, err)
	err = dockerRuntimeHandler.StopContainer("", 5)
	assert.Error(t, err)

	dockerRuntimeHandlerNotInit := DockerRuntimeHandler{endpoint: endPoint, dockerClient: nil}
	err = dockerRuntimeHandlerNotInit.StopContainer("test_stop_container", 5)
	assert.Error(t, err)
}

func Test_Docker_UpdateContainerResources(t *testing.T) {
	helper := system.NewFileTestUtil(t)
	helper.MkDirAll("/var/run")
	helper.WriteFileContents("/var/run/docker.sock", "test")
	system.Conf.VarRunRootDir = filepath.Join(helper.TempDir, "/var/run")
	DockerEndpoint = filepath.Join(system.Conf.VarRunRootDir, "docker.sock")

	// for a higher version of docker client like 20.10, client version cannot be omitted in request URL
	expectedURL := "/v" + api.DefaultVersion + "/containers/test_update_container/update"
	updateContainer := func(req *http.Request) (*http.Response, error) {
		if !strings.HasPrefix(req.URL.Path, expectedURL) {
			return nil, fmt.Errorf("Expected URL '%s', got '%s'", expectedURL, req.URL)
		}
		b, err := json.Marshal(container.ContainerUpdateOKBody{})
		if err != nil {
			return nil, err
		}
		return &http.Response{
			StatusCode: http.StatusOK,
			Body:       io.NopCloser(bytes.NewReader(b)),
		}, nil
	}
	endPoint := fmt.Sprintf("unix://%s", DockerEndpoint)
	dockerClient, err := createDockerClient(newMockClient(updateContainer), endPoint)
	assert.NoError(t, err)
	dockerRuntimeHandler := DockerRuntimeHandler{endpoint: endPoint, dockerClient: dockerClient}
	err = dockerRuntimeHandler.UpdateContainerResources("test_update_container", UpdateOptions{CPUQuota: 10000})
	assert.NoError(t, err)

	err = dockerRuntimeHandler.UpdateContainerResources("", UpdateOptions{CPUQuota: 10000})
	assert.Error(t, err)

	dockerRuntimeHandlerNotInit := DockerRuntimeHandler{endpoint: endPoint, dockerClient: nil}
	err = dockerRuntimeHandlerNotInit.UpdateContainerResources("test_update_container", UpdateOptions{CPUQuota: 10000})
	assert.Error(t, err)
}

type transportFunc func(*http.Request) (*http.Response, error)

func (tf transportFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return tf(req)
}

func newMockClient(doer func(*http.Request) (*http.Response, error)) *http.Client {
	return &http.Client{
		Transport: transportFunc(doer),
	}
}
