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

package nexussvc

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/nexus"
)

func (svc *NexusSvc) ConvertRepo(dbRepo *dao.NexusRepository) *apistructs.NexusRepository {
	repo := apistructs.NexusRepository{
		ID:          uint64(dbRepo.ID),
		Name:        dbRepo.Name,
		Format:      dbRepo.Format,
		URL:         dbRepo.Config.URL,
		Type:        dbRepo.Type,
		OrgID:       dbRepo.OrgID,
		PublisherID: dbRepo.PublisherID,
		ClusterName: dbRepo.ClusterName,
	}
	if dbRepo.Config.Extra.ReverseProxyURL != "" {
		repo.URL = dbRepo.Config.Extra.ReverseProxyURL
	}
	return &repo
}

func (svc *NexusSvc) ConvertRepos(dbRepos []*dao.NexusRepository) []*apistructs.NexusRepository {
	var results []*apistructs.NexusRepository
	for _, dbRepo := range dbRepos {
		results = append(results, svc.ConvertRepo(dbRepo))
	}
	return results
}

// EnsureRepository 幂等保证 repository 存在
func (svc *NexusSvc) EnsureRepository(req apistructs.NexusRepositoryEnsureRequest) (*apistructs.NexusRepository, error) {
	n := nexus.New(req.NexusServer)

	// 确认 nexus 物理 blob store & repo 存在
	err := n.EnsureRepository(req.NexusCreateRequest)
	if err != nil {
		return nil, apierrors.ErrEnsurePhysicsNexusRepo.InternalError(err)
	}

	// 确认 db 记录存在
	physicsRepo, err := n.GetRepository(nexus.RepositoryGetRequest{RepositoryName: req.NexusCreateRequest.GetName()})
	if err != nil {
		return nil, apierrors.ErrGetPhysicsNexusRepo.InternalError(err)
	}

	if err := svc.handleDockerPhysicsRepo(physicsRepo); err != nil {
		return nil, apierrors.ErrHandleNexusDockerRepo.InternalError(err)
	}

	dbRepo := dao.NexusRepository{
		OrgID:       req.OrgID,
		PublisherID: req.PublisherID,
		ClusterName: req.ClusterName,
		Name:        req.NexusCreateRequest.GetName(),
		Format:      req.NexusCreateRequest.GetFormat(),
		Type:        req.NexusCreateRequest.GetType(),
		Config:      dao.NexusRepositoryConfig(*physicsRepo),
	}
	err = svc.db.CreateOrUpdateNexusRepository(&dbRepo)
	if err != nil {
		return nil, apierrors.ErrEnsureNexusRepoRecord.InternalError(err)
	}
	repo := svc.ConvertRepo(&dbRepo)

	// sync to pipeline cm
	if err := svc.SyncRepoConfigToPipelineCM(req.SyncConfigToPipelineCM, repo); err != nil {
		return nil, apierrors.ErrSyncConfigToPipelineCM.InternalError(err)
	}

	return repo, nil
}

// handleDockerPhysicsRepo 处理 docker repo
func (svc *NexusSvc) handleDockerPhysicsRepo(repo *nexus.Repository) error {
	if repo.Format != nexus.RepositoryFormatDocker {
		return nil
	}

	if repo.Type == nexus.RepositoryTypeHosted {
		rewritePath := httpclientutil.GetPath(repo.URL) + "/$1"
		domain := repo.Name + "-" + httpclientutil.RmProto(conf.CentralNexusPublicURL())
		if err := svc.bdl.CreateOrUpdateComponentIngress(apistructs.ComponentIngressUpdateRequest{
			ComponentName: conf.CentralNexusComponentName(),
			ComponentPort: httpclientutil.GetPort(conf.CentralNexusAddr()),
			ClusterName:   conf.DiceClusterName(),
			IngressName:   conf.CentralNexusComponentName() + "-" + repo.Name,
			Routes: []apistructs.IngressRoute{
				{
					Domain: domain,
					Path:   "/(.*)",
				},
			},
			RouteOptions: apistructs.RouteOptions{
				RewritePath: &rewritePath,
				UseRegex:    true,
				Annotations: map[string]string{
					"nginx.ingress.kubernetes.io/proxy-body-size": "0", // no limit body size
				},
			},
		}); err != nil {
			return err
		}
		repo.Extra.ReverseProxyURL = domain
	}

	return nil
}

// ListRepositories 查询 repositories 列表
func (svc *NexusSvc) ListRepositories(req apistructs.NexusRepositoryListRequest) ([]*apistructs.NexusRepository, error) {
	dbRepos, err := svc.db.ListNexusRepositories(req)
	if err != nil {
		return nil, apierrors.ErrListNexusRepos.InternalError(err)
	}
	repos := svc.ConvertRepos(dbRepos)

	// nexus users
	users, err := svc.ListUsers(apistructs.NexusUserListRequest{
		PublisherID:    req.PublisherID,
		DecodePassword: false,
	})
	if err != nil {
		return nil, err
	}
	userRepoIDMap := make(map[uint64]*apistructs.NexusUser)
	for _, user := range users {
		userRepoIDMap[*user.RepoID] = &user
	}

	// inject users into repos
	for i := range repos {
		repos[i].User = userRepoIDMap[repos[i].ID]
	}

	return repos, nil
}

func (svc *NexusSvc) GetRepositoryByName(name string) (*apistructs.NexusRepository, error) {
	dbRepo, err := svc.db.GetNexusRepositoryByName(name)
	if err != nil {
		return nil, apierrors.ErrGetNexusRepoRecord.InternalError(err)
	}
	return svc.ConvertRepo(dbRepo), nil
}
