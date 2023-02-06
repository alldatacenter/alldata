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

package nexus

type docker struct{}

func (d docker) GetFormat() RepositoryFormat { return RepositoryFormatDocker }

// hosted
type DockerHostedRepositoryCreateRequest struct {
	docker
	HostedRepositoryCreateRequest
	Docker RepositoryDockerConfig `json:"docker,omitempty"`
}
type DockerHostedRepositoryUpdateRequest DockerHostedRepositoryCreateRequest

// proxy
type DockerProxyRepositoryCreateRequest struct {
	docker
	ProxyRepositoryCreateRequest
	Docker      RepositoryDockerConfig      `json:"docker,omitempty"`
	DockerProxy RepositoryDockerProxyConfig `json:"dockerProxy,omitempty"`
}
type DockerProxyRepositoryUpdateRequest DockerProxyRepositoryCreateRequest
