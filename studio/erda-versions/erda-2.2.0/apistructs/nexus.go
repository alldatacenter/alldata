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

package apistructs

import "github.com/erda-project/erda/pkg/nexus"

//////////////////////////////////////////
// nexus repository
//////////////////////////////////////////

type NexusRepositoryEnsureRequest struct {
	OrgID       *uint64
	PublisherID *uint64
	ClusterName string

	NexusServer            nexus.Server
	NexusCreateRequest     nexus.RepositoryCreator
	SyncConfigToPipelineCM NexusSyncConfigToPipelineCM
}

type NexusRepositoryListRequest struct {
	IDs          []uint64                 `json:"ids"`
	PublisherID  *uint64                  `json:"publisherID"`
	OrgID        *uint64                  `json:"orgID"`
	Formats      []nexus.RepositoryFormat `json:"formats,omitempty"`
	Types        []nexus.RepositoryType   `json:"types,omitempty"`
	NameContains []string                 `json:"nameContains"`
}

type NexusRepository struct {
	ID     uint64                 `json:"id"`
	Name   string                 `json:"name"`
	Format nexus.RepositoryFormat `json:"format"`
	URL    string                 `json:"url"`
	Type   nexus.RepositoryType   `json:"type"`

	OrgID       *uint64 `json:"orgID,omitempty"`
	PublisherID *uint64 `json:"publisherID,omitempty"`
	ClusterName string  `json:"clusterName,omitempty"`

	User *NexusUser `json:"user"`
}

//////////////////////////////////////////
// nexus user
//////////////////////////////////////////

type NexusUser struct {
	ID          uint64  `json:"id,omitempty"`
	RepoID      *uint64 `json:"repoID,omitempty"`
	OrgID       *uint64 `json:"orgID,omitempty"`
	PublisherID *uint64 `json:"publisherID,omitempty"`
	Name        string  `json:"name"`
	Password    string  `json:"password"`
}

type NexusUserListRequest struct {
	UserIDs        []uint64 `json:"userIDs,omitempty"`
	PublisherID    *uint64  `json:"publisherID"`
	OrgID          *uint64  `json:"orgID"`
	RepoID         *uint64  `json:"repoID"`
	DecodePassword bool     `json:"decodePassword"`
}

type NexusDeploymentUserEnsureRequest struct {
	RepoID   uint64
	Password string

	NexusServer            nexus.Server
	SyncConfigToPipelineCM NexusSyncConfigToPipelineCM
}

type NexusOrgReadonlyUserEnsureRequest struct {
	OrgID       uint64
	ClusterName string

	Name     string
	Password string
}

type NexusUserEnsureRequest struct {
	// ClusterName 属于哪个集群的 nexus
	// +required
	ClusterName string
	// RepoID 关联 repo 信息
	// +optional
	RepoID *uint64
	// OrgID 关联 org 信息
	// +optional
	OrgID *uint64

	// +required
	UserName string
	// +required
	Password string
	// +optional
	// 是否强制更新密码，ensure 场景一般需要保留原密码，因为原密码可能正在被打包使用中
	ForceUpdatePassword bool

	// RepoPrivileges 关联的 repo 权限
	// +optional
	RepoPrivileges map[uint64][]nexus.PrivilegeAction

	// +optional
	SyncConfigToPipelineCM NexusSyncConfigToPipelineCM

	NexusServer nexus.Server
}

type NexusSyncConfigToPipelineCM struct {
	SyncPublisher *NexusSyncConfigToPipelineCMItem
	SyncOrg       *NexusSyncConfigToPipelineCMItem
	SyncPlatform  *NexusSyncConfigToPipelineCMItem
}

type NexusSyncConfigToPipelineCMItem struct {
	ConfigPrefix string
}

type NexusUserGetResponse struct {
	Header
	Data *NexusUser `json:"data"`
}
