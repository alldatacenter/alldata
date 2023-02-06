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
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/nexus"
)

func (p *Publisher) ensureNexusHostedRepo(publisher *model.Publisher) error {
	// npm
	if err := p.ensureNexusNpmHostedRepo(publisher); err != nil {
		return err
	}
	// maven
	if err := p.ensureNexusMavenHostedRepo(publisher); err != nil {
		return err
	}

	return nil
}

// ensurePublisherDeploymentUser 幂等保证 nexus repo deployment user 存在
func (p *Publisher) ensurePublisherDeploymentUser(req apistructs.NexusDeploymentUserEnsureRequest) (*apistructs.NexusUser, error) {
	// 查询 nexus repo db 记录
	dbRepo, err := p.db.GetNexusRepository(req.RepoID)
	if err != nil {
		return nil, apierrors.ErrGetNexusRepoRecord.InternalError(err)
	}
	nexusUser, err := p.nexusSvc.EnsureUser(apistructs.NexusUserEnsureRequest{
		ClusterName:            dbRepo.ClusterName,
		RepoID:                 &req.RepoID,
		OrgID:                  dbRepo.OrgID,
		UserName:               nexus.MakeDeploymentUserName(dbRepo.Name),
		Password:               req.Password,
		RepoPrivileges:         map[uint64][]nexus.PrivilegeAction{req.RepoID: nexus.RepoDeploymentPrivileges},
		SyncConfigToPipelineCM: req.SyncConfigToPipelineCM,
		NexusServer:            req.NexusServer,
		ForceUpdatePassword:    true,
	})
	if err != nil {
		return nil, err
	}
	return nexusUser, nil
}
