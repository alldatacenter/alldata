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

package publisher

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/nexus"
)

func (p *Publisher) ensureNexusMavenHostedRepo(publisher *model.Publisher) error {
	nexusServer := nexus.Server{
		Addr:     conf.CentralNexusAddr(),
		Username: conf.CentralNexusUsername(),
		Password: conf.CentralNexusPassword(),
	}
	repoName := nexus.MakePublisherRepoName(nexus.RepositoryFormatMaven, nexus.RepositoryTypeHosted, uint64(publisher.ID))
	// ensure repo
	repo, err := p.nexusSvc.EnsureRepository(apistructs.NexusRepositoryEnsureRequest{
		OrgID:       &[]uint64{uint64(publisher.OrgID)}[0],
		PublisherID: &[]uint64{uint64(publisher.ID)}[0],
		ClusterName: conf.DiceClusterName(),
		NexusServer: nexusServer,
		NexusCreateRequest: nexus.MavenHostedRepositoryCreateRequest{
			HostedRepositoryCreateRequest: nexus.HostedRepositoryCreateRequest{
				BaseRepositoryCreateRequest: nexus.BaseRepositoryCreateRequest{
					Name:   repoName,
					Online: true,
					Storage: nexus.HostedRepositoryStorageConfig{
						BlobStoreName:               repoName,
						StrictContentTypeValidation: true,
						WritePolicy:                 nexus.RepositoryStorageWritePolicyDisableRedeploy,
					},
					Cleanup: nil,
				},
			},
			Maven: nexus.RepositoryMavenConfig{
				VersionPolicy: nexus.RepositoryMavenVersionPolicyRelease,
				LayoutPolicy:  nexus.RepositoryMavenLayoutPolicyStrict,
			},
		},
		SyncConfigToPipelineCM: apistructs.NexusSyncConfigToPipelineCM{
			SyncPublisher: &apistructs.NexusSyncConfigToPipelineCMItem{
				ConfigPrefix: "publisher.maven.",
			},
		},
	})
	if err != nil {
		return err
	}

	// ensure deployment user
	_, err = p.ensurePublisherDeploymentUser(apistructs.NexusDeploymentUserEnsureRequest{
		RepoID:      repo.ID,
		Password:    uuid.UUID(),
		NexusServer: nexusServer,
		SyncConfigToPipelineCM: apistructs.NexusSyncConfigToPipelineCM{
			SyncPublisher: &apistructs.NexusSyncConfigToPipelineCMItem{
				ConfigPrefix: "publisher.maven.",
			},
		},
	})
	return err
}
