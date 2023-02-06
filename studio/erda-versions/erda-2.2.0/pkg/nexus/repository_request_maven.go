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

type maven struct{}

func (m maven) GetFormat() RepositoryFormat { return RepositoryFormatMaven }

// hosted
type MavenHostedRepositoryCreateRequest struct {
	maven
	HostedRepositoryCreateRequest
	Maven RepositoryMavenConfig `json:"maven,omitempty"`
}
type MavenHostedRepositoryUpdateRequest MavenHostedRepositoryCreateRequest

// proxy
type MavenProxyRepositoryCreateRequest struct {
	maven
	ProxyRepositoryCreateRequest
	Maven RepositoryMavenConfig `json:"maven,omitempty"`
}
type MavenProxyRepositoryUpdateRequest MavenProxyRepositoryCreateRequest

// group
type MavenGroupRepositoryCreateRequest struct {
	maven
	GroupRepositoryCreateRequest
}
type MavenGroupRepositoryUpdateRequest MavenGroupRepositoryCreateRequest
