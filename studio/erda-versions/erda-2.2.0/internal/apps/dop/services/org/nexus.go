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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/nexus"
)

// needEnableNexusOrgGroupRepos judge if need enable nexus org group repos
// TODO: maybe use org-level nexus config in the future. Now org does not have nexus config yet.
func needEnableNexusOrgGroupRepos(nexusAddr string, org *apistructs.OrgDTO) bool {
	// disable if no nexus
	if len(nexusAddr) == 0 {
		return false
	}
	return org.EnableReleaseCrossCluster || org.PublisherID > 0
}

func (o *Org) EnsureNexusOrgGroupRepos(org *apistructs.OrgDTO) error {
	// judge if need enable nexus org group repos
	if !needEnableNexusOrgGroupRepos(conf.NexusAddr(), org) {
		return nil
	}

	// group repos

	// TODO nexus 3.24
	//// maven
	//if err := o.ensureNexusOrgMavenGroupRepos(org); err != nil {
	//	return err
	//}

	// npm
	if err := o.ensureNexusNpmGroupOrgRepos(org); err != nil {
		return err
	}

	// docker
	if err := o.ensureNexusDockerGroupOrgRepos(org); err != nil {
		return err
	}

	return nil
}

func (o *Org) GetOrgLevelNexus(orgID uint64, req *apistructs.OrgNexusGetRequest) (*apistructs.OrgNexusGetResponseData, error) {
	if orgID == 0 {
		return nil, apierrors.ErrGetOrgNexus.MissingParameter("orgID")
	}

	orgRepos, err := o.nexusSvc.ListRepositories(apistructs.NexusRepositoryListRequest{
		OrgID:   &orgID,
		Formats: req.Formats,
		Types:   req.Types,
	})
	if err != nil {
		return nil, apierrors.ErrGetOrgNexus.InternalError(err)
	}

	data := apistructs.OrgNexusGetResponseData{
		OrgGroupRepos:         make(map[nexus.RepositoryFormat]*apistructs.NexusRepository),
		OrgSnapshotRepos:      make(map[nexus.RepositoryFormat]*apistructs.NexusRepository),
		PublisherReleaseRepos: make(map[nexus.RepositoryFormat]*apistructs.NexusRepository),
		ThirdPartyProxyRepos:  make(map[nexus.RepositoryFormat][]*apistructs.NexusRepository),
	}

	for _, repo := range orgRepos {
		switch repo.Type {
		case nexus.RepositoryTypeGroup:
			data.OrgGroupRepos[repo.Format] = repo
		case nexus.RepositoryTypeHosted:
			if repo.PublisherID == nil {
				data.OrgSnapshotRepos[repo.Format] = repo
			} else {
				data.PublisherReleaseRepos[repo.Format] = repo
			}
		case nexus.RepositoryTypeProxy:
			data.ThirdPartyProxyRepos[repo.Format] = append(data.ThirdPartyProxyRepos[repo.Format], repo)
		}
	}

	return &data, nil
}

func (o *Org) ShowOrgNexusPassword(req *apistructs.OrgNexusShowPasswordRequest) (map[uint64]string, error) {
	users, err := o.nexusSvc.ListUsers(apistructs.NexusUserListRequest{
		UserIDs:        req.NexusUserIDs,
		PublisherID:    nil,
		OrgID:          &req.OrgID,
		RepoID:         nil,
		DecodePassword: true,
	})
	if err != nil {
		return nil, apierrors.ErrShowOrgNexusPassword.InternalError(err)
	}

	userPasswordMap := make(map[uint64]string, len(users))
	for _, user := range users {
		userPasswordMap[user.ID] = user.Password
	}

	var notFoundUserIDs []uint64
	for _, reqUserID := range req.NexusUserIDs {
		if _, ok := userPasswordMap[reqUserID]; !ok {
			notFoundUserIDs = append(notFoundUserIDs, reqUserID)
		}
	}
	if len(notFoundUserIDs) > 0 {
		return nil, apierrors.ErrShowOrgNexusPassword.InvalidParameter(
			fmt.Sprintf("couldn't found corresponding nexus users, invalid nexus user ids: %v", notFoundUserIDs))
	}
	return userPasswordMap, nil
}
