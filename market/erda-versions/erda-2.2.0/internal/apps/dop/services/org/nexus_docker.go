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

package org

import (
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/nexus"
)

// ensureNexusDockerGroupOrgRepos
// 1. docker hosted org repo
func (o *Org) ensureNexusDockerGroupOrgRepos(org *apistructs.OrgDTO) error {

	// 1. docker hosted org repo
	_, err := o.ensureNexusDockerHostedOrgRepo(org)
	return err
}

func (o *Org) ensureNexusDockerHostedOrgRepo(org *apistructs.OrgDTO) (*apistructs.NexusRepository, error) {
	n := nexus.Server{
		Addr:     conf.CentralNexusAddr(),
		Username: conf.CentralNexusUsername(),
		Password: conf.CentralNexusPassword(),
	}

	// repo
	dockerRepoName := nexus.MakeOrgRepoName(nexus.RepositoryFormatDocker, nexus.RepositoryTypeHosted, uint64(org.ID))
	repo, err := o.nexusSvc.EnsureRepository(apistructs.NexusRepositoryEnsureRequest{
		OrgID:       &[]uint64{uint64(org.ID)}[0],
		PublisherID: nil,
		ClusterName: conf.DiceClusterName(),
		NexusServer: n,
		NexusCreateRequest: nexus.DockerHostedRepositoryCreateRequest{
			HostedRepositoryCreateRequest: nexus.HostedRepositoryCreateRequest{
				BaseRepositoryCreateRequest: nexus.BaseRepositoryCreateRequest{
					Name:   dockerRepoName,
					Online: true,
					Storage: nexus.HostedRepositoryStorageConfig{
						BlobStoreName:               dockerRepoName,
						StrictContentTypeValidation: true,
						WritePolicy:                 nexus.RepositoryStorageWritePolicyAllowRedploy,
						BlobUseNetdata:              nexus.BlobUseNetdata{UseNetdata: true},
					},
					Cleanup: nil,
				},
			},
			Docker: nexus.RepositoryDockerConfig{
				V1Enabled:      true,
				ForceBasicAuth: true,
				HttpPort:       nil,
				HttpsPort:      nil,
			},
		},
		SyncConfigToPipelineCM: apistructs.NexusSyncConfigToPipelineCM{
			SyncOrg: &apistructs.NexusSyncConfigToPipelineCMItem{
				ConfigPrefix: "org.docker.",
			},
		},
	})
	if err != nil {
		return nil, err
	}

	// user
	dockerPushUser, err := o.nexusSvc.EnsureUser(apistructs.NexusUserEnsureRequest{
		ClusterName:    conf.DiceClusterName(),
		RepoID:         &repo.ID,
		OrgID:          nil,
		UserName:       nexus.MakeDeploymentUserName(repo.Name),
		Password:       uuid.UUID(),
		RepoPrivileges: map[uint64][]nexus.PrivilegeAction{repo.ID: nexus.RepoDeploymentPrivileges},
		SyncConfigToPipelineCM: apistructs.NexusSyncConfigToPipelineCM{
			SyncOrg: &apistructs.NexusSyncConfigToPipelineCMItem{
				ConfigPrefix: "org.docker.push.",
			},
		},
		NexusServer:         n,
		ForceUpdatePassword: false,
	})
	if err != nil {
		return nil, err
	}
	_ = dockerPushUser

	dockerPullUser, err := o.nexusSvc.EnsureUser(apistructs.NexusUserEnsureRequest{
		ClusterName:    conf.DiceClusterName(),
		RepoID:         &repo.ID,
		OrgID:          nil,
		UserName:       nexus.MakeReadonlyUserName(repo.Name),
		Password:       uuid.UUID(),
		RepoPrivileges: map[uint64][]nexus.PrivilegeAction{repo.ID: nexus.RepoReadOnlyPrivileges},
		SyncConfigToPipelineCM: apistructs.NexusSyncConfigToPipelineCM{
			SyncOrg: &apistructs.NexusSyncConfigToPipelineCMItem{
				ConfigPrefix: "org.docker.pull.",
			},
		},
		NexusServer:         n,
		ForceUpdatePassword: false,
	})
	if err != nil {
		return nil, err
	}
	repo.User = dockerPullUser

	return repo, nil
}

// GetNexusOrgDockerCredential 根据 image 返回 docker pull 认证信息
func (o *Org) GetNexusOrgDockerCredential(orgID uint64, image string) (*apistructs.NexusUser, error) {
	// image
	if image == "" {
		return nil, apierrors.ErrGetNexusDockerCredentialByImage.InvalidParameter("image is empty")
	}

	host := strings.Split(image, "/")[0]
	nexusURL := httpclientutil.RmProto(conf.CentralNexusPublicURL())
	// not dice managed image, return nil docker credential
	if !strings.Contains(host, nexusURL) {
		return nil, nil
	}

	repoName := strings.TrimSuffix(host, "-"+nexusURL)
	repo, err := o.nexusSvc.GetRepositoryByName(repoName)
	if err != nil {
		return nil, err
	}
	if repo.OrgID == nil {
		return nil, apierrors.ErrGetNexusDockerCredentialByImage.InvalidParameter("corresponding repo not belong to any org")
	}
	if *repo.OrgID != orgID {
		return nil, apierrors.ErrGetNexusDockerCredentialByImage.InvalidParameter("corresponding repo's orgID is mismatch")
	}

	return o.nexusSvc.GetUserByName(nexus.MakeReadonlyUserName(repoName), true)
}
