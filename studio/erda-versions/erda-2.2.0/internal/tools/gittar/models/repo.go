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

package models

import (
	"encoding/json"
	"errors"
	"os"
	"path"
	"strconv"
	"time"

	"github.com/patrickmn/go-cache"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/pkg/gittarutil"
)

var ModeExternal = "external"

type Repo struct {
	ID          int64
	OrgID       int64
	ProjectID   int64
	AppID       int64
	OrgName     string `gorm:"size:150;index:idx_org_name"`
	ProjectName string `gorm:"size:150;index:idx_project_name"`
	AppName     string `gorm:"size:150;index:idx_app_name"`
	Path        string `gorm:"size:150;index:idx_path"`
	IsLocked    bool   `gorm:"size:150;index:idx_is_locked"`
	Size        int64
	IsExternal  bool
	Config      string
}

func (Repo) TableName() string {
	return "dice_repos"
}

func (r *Repo) DiskPath() string {
	return path.Join(conf.RepoRoot(), r.Path)
}

var repoCache = cache.New(24*time.Hour, 60*time.Minute)

func getConfigStr(config *apistructs.GitRepoConfig) string {
	configBytes, _ := json.Marshal(map[string]string{
		"url":  config.Url,
		"desc": config.Desc,
		"type": config.Type,
	})
	return string(configBytes)
}

func (svc *Service) CreateRepo(request *apistructs.CreateRepoRequest) (*Repo, error) {

	repo := &Repo{
		OrgID:       request.OrgID,
		ProjectID:   request.ProjectID,
		AppID:       request.AppID,
		OrgName:     request.OrgName,
		ProjectName: request.ProjectName,
		AppName:     request.AppName,
		IsExternal:  request.IsExternal,
	}
	if request.IsExternal {
		repo.Config = getConfigStr(request.Config)
	}
	repo.Path = gittarutil.MakeRepoPath(repo.OrgName, repo.ProjectName, repo.AppName)
	if request.OnlyCheck {
		if request.IsExternal {
			err := gitmodule.CheckRemoteHttpRepo(request.Config.Url, request.Config.Username, request.Config.Password)
			if err != nil {
				return nil, err
			}
		}
		return repo, nil
	}
	var count int64
	err := svc.db.Model(&Repo{}).
		Where("org_name=? and project_name=? and app_name=? ", repo.OrgName, repo.ProjectName, repo.AppName).
		Count(&count).Error
	if err != nil {
		return nil, err
	}
	if count == 0 {
		if request.IsExternal {
			// 先校验有效性
			err := gitmodule.CheckRemoteHttpRepo(request.Config.Url, request.Config.Username, request.Config.Password)
			if err != nil {
				return nil, err
			}
			err = gitmodule.InitExternalRepository(
				repo.DiskPath(),
				request.Config.Url,
				request.Config.Username,
				request.Config.Password,
			)
			if err != nil {
				return nil, err
			}
		} else {
			err = gitmodule.InitRepository(repo.DiskPath(), true)
			if err != nil {
				return nil, err
			}
		}
		err := svc.db.Create(repo).Error
		return repo, err
	} else {
		//已经存在
		return nil, errors.New("repo is exist")
	}
}

var ERROR_REPO_LOCKED = errors.New("locked denied")

func (svc *Service) SetLocked(repo *gitmodule.Repository, user *User, info *apistructs.LockedRepoRequest) (*apistructs.LockedRepoRequest, error) {
	if err := svc.CheckPermission(repo, user, PermissionRepoLocked, nil); err != nil {
		return nil, ERROR_REPO_LOCKED
	}

	err := svc.db.Table("dice_repos").Where("app_id = ?", info.AppID).Update("is_locked", info.IsLocked).Error
	if err != nil {
		return nil, err
	}
	return info, nil
}

func (svc *Service) DeleteRepo(repo *Repo) error {
	repoPath := repo.DiskPath()
	logrus.Infof("remove gitRepo %v", repoPath)
	tmpRepoPath := repoPath + "-delete-" + strconv.FormatInt(time.Now().UnixNano(), 10)
	if _, err := os.Stat(repoPath); err == nil {
		err := os.Rename(repoPath, tmpRepoPath)
		if err != nil {
			logrus.Errorf("failed to move repo path %s => %s", repoPath, tmpRepoPath)
			return err
		}
		go func() {
			err = os.RemoveAll(tmpRepoPath)
			if err != nil {
				logrus.Errorf("failed to delete repo path:%s tmpPath:%s", repoPath, tmpRepoPath)
			}
		}()
	}
	err := svc.db.Delete(repo).Error
	if err != nil {
		return err
	}
	err = svc.RemoveProjectHooks(repo)
	if err != nil {
		return err
	}
	err = svc.RemoveMR(repo)
	return err
}

func (svc *Service) UpdateRepoSizeCache(id int64, size int64) error {
	err := svc.db.Model(&Repo{}).Where("id = ?", id).Update("size", size).Error
	return err
}

func (svc *Service) GetRepoByApp(appId int64) (*Repo, error) {
	var currentRepo Repo
	err := svc.db.Where("app_id =?", appId).First(&currentRepo).Error
	if err != nil {
		return nil, err
	}
	return &currentRepo, nil
}

func (svc *Service) UpdateRepo(repo *Repo, request *apistructs.UpdateRepoRequest) error {
	if repo.IsExternal {
		if request.Config == nil {
			return errors.New("repo config is nil")
		}
		err := gitmodule.CheckRemoteHttpRepo(request.Config.Url, request.Config.Username, request.Config.Password)
		if err != nil {
			return err
		}
	}
	err := gitmodule.UpdateExternalRepository(repo.DiskPath(), request.Config.Url, request.Config.Username, request.Config.Password)
	if err != nil {
		return err
	}
	repo.Config = getConfigStr(request.Config)
	return svc.db.Save(repo).Error
}

func (svc *Service) GetRepoById(id int64) (*Repo, error) {
	var currentRepo Repo
	err := svc.db.Where("id =?", id).First(&currentRepo).Error
	if err != nil {
		return nil, err
	}
	return &currentRepo, nil
}

func (svc *Service) GetRepoByPath(path string) (*Repo, error) {
	var currentRepo Repo
	err := svc.db.Where("path =?", path).First(&currentRepo).Error
	if err != nil {
		return nil, err
	}
	return &currentRepo, nil
}

func (svc *Service) GetRepoByNames(orgID int64, project, app string) (*Repo, error) {
	var currentRepo Repo
	err := svc.db.Where("org_id =? and project_name =? and app_name =?", orgID, project, app).First(&currentRepo).Error
	if err != nil {
		return nil, err
	}
	return &currentRepo, nil
}

func (svc *Service) GetRepoLocked(project, app int64) (bool, error) {
	var currentRepo Repo
	err := svc.db.Table("dice_repos").Where("project_id =? and app_id =?", project, app).First(&currentRepo).Error
	if err != nil {
		return false, err
	}
	return currentRepo.IsLocked, nil
}

// Repository struct
type Repository struct {
	Organization string `json:"org"`
	Repository   string `json:"repo"`
	Url          string `json:"url"`
}

// Root for Repository
func (r *Repository) Root() string {
	return conf.RepoRoot()
}

func (r *Repository) FullName() string {
	return r.Organization + "/" + r.Repository
}

// DiskPath for Repository
func (r *Repository) Path() string {
	return path.Join(r.Root(), r.Organization, r.Repository)
}

// CheckPath for Repository
func (r *Repository) CheckPath(file string) (string, error) {
	requestPath := path.Join(r.Path(), file)
	_, err := os.Stat(requestPath)
	if os.IsNotExist(err) {
		return "", err
	}
	return requestPath, nil
}

// InfoPacksPath for Repository
func (r *Repository) InfoPacksPath() string {
	return path.Join(r.Path(), "info", "packs")
}

// LooseObjectPath for Repository
func (r *Repository) LooseObjectPath(prefix string, suffix string) string {
	return path.Join(r.Path(), "objects", prefix, suffix)
}

// PackIdxPath for Repository
func (r *Repository) PackIdxPath(pack string) string {
	return path.Join(r.Path(), "pack", pack)
}
