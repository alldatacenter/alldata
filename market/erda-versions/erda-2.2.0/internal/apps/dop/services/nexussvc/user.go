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
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/nexus"
)

func getHiddenPassword() string {
	return "******"
}

func (svc *NexusSvc) convertUser(dbUser *dao.NexusUser, decodePassword bool) (*apistructs.NexusUser, error) {
	user := apistructs.NexusUser{
		ID:          uint64(dbUser.ID),
		RepoID:      dbUser.RepoID,
		OrgID:       dbUser.OrgID,
		PublisherID: dbUser.PublisherID,
		Name:        dbUser.Name,
		Password:    getHiddenPassword(),
	}
	if decodePassword {
		plainPassword, err := svc.rsaCrypt.Decrypt(dbUser.Password, encryption.Base64)
		if err != nil {
			return nil, err
		}
		user.Password = plainPassword
	}
	return &user, nil
}

func (svc *NexusSvc) convertUsers(dbUsers []dao.NexusUser, decodePassword bool) ([]apistructs.NexusUser, error) {
	var users []apistructs.NexusUser
	for _, dbUser := range dbUsers {
		user, err := svc.convertUser(&dbUser, decodePassword)
		if err != nil {
			return nil, err
		}
		users = append(users, *user)
	}
	return users, nil
}

// ListUsers 查询 user 列表
func (svc *NexusSvc) ListUsers(req apistructs.NexusUserListRequest) ([]apistructs.NexusUser, error) {
	dbUsers, err := svc.db.ListNexusUsers(req)
	if err != nil {
		return nil, apierrors.ErrListNexusRepos.InternalError(err)
	}
	return svc.convertUsers(dbUsers, req.DecodePassword)
}

func (svc *NexusSvc) GetUserByName(name string, decodePassword bool) (*apistructs.NexusUser, error) {
	dbUser, err := svc.db.GetNexusUserByName(name)
	if err != nil {
		return nil, apierrors.ErrGetNexusUserRecord.InternalError(err)
	}
	user, err := svc.convertUser(dbUser, decodePassword)
	if err != nil {
		return nil, apierrors.ErrGetNexusUserRecord.InternalError(err)
	}
	return user, nil
}

func (svc *NexusSvc) EnsureUser(req apistructs.NexusUserEnsureRequest) (*apistructs.NexusUser, error) {
	n := nexus.New(req.NexusServer)

	// 查询 相关的 nexus repo 记录
	var privilegeRepoIDs []uint64
	for repoID := range req.RepoPrivileges {
		privilegeRepoIDs = append(privilegeRepoIDs, repoID)
	}
	privilegeDbRepos, err := svc.db.ListNexusRepositories(apistructs.NexusRepositoryListRequest{IDs: privilegeRepoIDs})
	if err != nil {
		return nil, apierrors.ErrListNexusRepos.InternalError(err)
	}
	var mainDbRepo *dao.NexusRepository
	if req.RepoID != nil {
		mainDbRepo, err = svc.db.GetNexusRepository(*req.RepoID)
		if err != nil {
			return nil, apierrors.ErrGetNexusRepoRecord.InternalError(err)
		}
	}

	// 保证 nexus 物理 user 存在
	nexusUserID, err := n.EnsureUser(nexus.EnsureUserRequest{
		UserName: req.UserName,
		Password: req.Password,
		RepoPrivileges: func() map[nexus.RepositoryFormat]map[string][]nexus.PrivilegeAction {
			repoPrivileges := make(map[nexus.RepositoryFormat]map[string][]nexus.PrivilegeAction)
			for _, repo := range privilegeDbRepos {
				if _, ok := repoPrivileges[repo.Format]; !ok {
					repoPrivileges[repo.Format] = make(map[string][]nexus.PrivilegeAction)
				}
				repoPrivileges[repo.Format][repo.Name] = req.RepoPrivileges[uint64(repo.ID)]
			}
			return repoPrivileges
		}(),
		ForceUpdatePassword: req.ForceUpdatePassword,
	})
	if err != nil {
		return nil, err
	}
	nexusUser, err := n.GetUser(nexusUserID)
	if err != nil {
		return nil, apierrors.ErrGetPhysicsNexusUser.InternalError(err)
	}

	// 确认 db 记录存在
	// password 加密存储
	encryptedPassword, err := svc.rsaCrypt.Encrypt(req.Password, encryption.Base64)
	if err != nil {
		return nil, apierrors.ErrEncryptPassword.InternalError(err)
	}
	dbUser := dao.NexusUser{
		ClusterName: mainDbRepo.ClusterName,
		Name:        string(nexusUser.UserID),
		Password:    encryptedPassword,
		Config:      dao.NexusUserConfig(*nexusUser),
	}
	if mainDbRepo != nil {
		dbUser.RepoID = &[]uint64{uint64(mainDbRepo.ID)}[0]
		dbUser.PublisherID = mainDbRepo.PublisherID
		dbUser.OrgID = mainDbRepo.OrgID
	}
	if !req.ForceUpdatePassword {
		existDbUser, _ := svc.db.GetNexusUserByName(dbUser.Name)
		if existDbUser != nil {
			dbUser.Password = existDbUser.Password
		}
	}
	err = svc.db.CreateOrUpdateNexusUser(&dbUser)
	if err != nil {
		return nil, apierrors.ErrEnsureNexusUserRecord.InternalError(err)
	}
	// sync to pipeline cm
	if mainDbRepo != nil {
		plainPasswordUser, err := svc.convertUser(&dbUser, true)
		if err != nil {
			return nil, err
		}
		if err := svc.SyncUserConfigToPipelineCM(req.SyncConfigToPipelineCM, plainPasswordUser, mainDbRepo.Format); err != nil {
			return nil, apierrors.ErrSyncConfigToPipelineCM.InternalError(err)
		}
	}

	return svc.convertUser(&dbUser, false)
}
