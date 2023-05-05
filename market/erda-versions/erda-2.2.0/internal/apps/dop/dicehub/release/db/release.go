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

package db

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// ReleaseConfig .
type ReleaseConfigDB struct {
	*gorm.DB
}

// CreateRelease Create Release
func (client *ReleaseConfigDB) CreateRelease(release *Release) error {
	return client.Create(release).Error
}

// UpdateRelease Update Release
func (client *ReleaseConfigDB) UpdateRelease(release *Release) error {
	return client.Save(release).Error
}

// DeleteRelease Delete Release
func (client *ReleaseConfigDB) DeleteRelease(releaseID string) error {
	return client.Where("release_id = ?", releaseID).Delete(&Release{}).Error
}

// GetRelease Get Release
func (client *ReleaseConfigDB) GetRelease(releaseID string) (*Release, error) {
	var release Release
	if err := client.Where("release_id = ?", releaseID).Find(&release).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, dbengine.ErrNotFound
		}
		return nil, err
	}
	return &release, nil
}

// GetReleasesByParams filter Releases by parameters
func (client *ReleaseConfigDB) GetReleasesByParams(
	orgID int64, req *pb.ReleaseListRequest) (int64, []Release, error) {

	var releases []Release
	db := client.DB.Debug()
	if orgID > 0 {
		db = db.Where("org_id = ?", orgID)
	}
	if len(req.ApplicationID) > 0 {
		db = db.Where("application_id in (?)", req.ApplicationID)
	}

	if req.ProjectID > 0 {
		db = db.Where("project_id = ?", req.ProjectID)
	}
	if req.Query != "" {
		db = db.Where("release_id LIKE ? or version LIKE ?", "%"+req.Query+"%", "%"+req.Query+"%")
	} else if req.ReleaseName != "" {
		db = db.Where("release_name = ?", req.ReleaseName)
	}
	if req.IsVersion {
		db = db.Not("version", "")
	}

	if req.Cluster != "" {
		db = db.Where("cluster_name = ?", req.Cluster)
	}
	if req.CrossCluster != "" {
		db = db.Where("cross_cluster = ?", req.CrossCluster)
	}
	if req.CrossClusterOrSpecifyCluster != "" {
		db = db.Where("(cluster_name = ? AND cross_cluster = 0) OR cross_cluster = 1", req.CrossClusterOrSpecifyCluster)
	}
	if req.Branch != "" {
		db = db.Where("labels LIKE ?", "%"+fmt.Sprintf("\"gitBranch\":\"%s\"", req.Branch)+"%")
	}

	if req.IsStable != "" {
		b, err := strconv.ParseBool(req.IsStable)
		if err != nil {
			return 0, nil, errors.Errorf("invalid param isStable, %v", err)
		}
		db = db.Where("is_stable = ?", b)
	}

	if req.IsProjectRelease != "" {
		b, err := strconv.ParseBool(req.IsProjectRelease)
		if err != nil {
			return 0, nil, errors.Errorf("invalid param isProjectRelease, %v", err)
		}
		db = db.Where("is_project_release = ?", b)
	}

	if req.IsFormal != "" {
		b, err := strconv.ParseBool(req.IsFormal)
		if err != nil {
			return 0, nil, errors.Errorf("invalid param isFormal, %v", err)
		}
		db = db.Where("is_formal = ?", b)
	}

	if len(req.UserID) > 0 {
		db = db.Where("user_id in (?)", req.UserID)
	}

	if req.Version != "" {
		var versions []string
		splits := strings.Split(req.Version, ",")
		for _, v := range splits {
			versions = append(versions, strings.TrimSpace(v))
		}

		if len(versions) == 1 {
			db = db.Where("version LIKE ?", fmt.Sprintf("%%%s%%", versions[0]))
		} else {
			db = db.Where("version in (?)", versions)
		}
	}

	if req.ReleaseID != "" {
		var releaseIDs []string
		splits := strings.Split(req.ReleaseID, ",")
		for _, id := range splits {
			releaseIDs = append(releaseIDs, strings.TrimSpace(id))
		}

		if len(releaseIDs) == 1 {
			db = db.Where("release_id LIKE ?", fmt.Sprintf("%%%s%%", releaseIDs[0]))
		} else {
			db = db.Where("release_id IN (?)", releaseIDs)
		}
	}

	if req.CommitID != "" {
		db = db.Where("labels LIKE ?", fmt.Sprintf("%%\"gitCommitId\":\"%s\"%%", req.CommitID))
	}

	if req.StartTime > 0 {
		db = db.Where("created_at > ?", time.Unix(req.StartTime/1000, 0))
	}

	if req.EndTime > 0 {
		db = db.Where("created_at <= ?", time.Unix(req.EndTime/1000, 0))
	}

	if req.IsLatest {
		db = db.Where("is_latest = true")
	}

	if req.OrderBy != "" {
		db = db.Order(req.OrderBy + " " + req.Order)
	} else {
		db = db.Order("created_at DESC")
	}

	if err := db.Offset((req.PageNo - 1) * req.PageSize).
		Limit(req.PageSize).Find(&releases).Error; err != nil {
		return 0, nil, err
	}

	// 获取匹配搜索结果总量
	var total int64
	if err := db.Model(&Release{}).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, releases, nil
}

// GetReleasesByAppAndVersion Get Release list by appID & version
func (client *ReleaseConfigDB) GetReleasesByAppAndVersion(orgID, projectID, appID int64, version string) ([]Release, error) {
	var releases []Release
	if err := client.Where("org_id = ?", orgID).
		Where("project_id = ?", projectID).
		Where("application_id = ?", appID).
		Where("version = ?", version).
		Find(&releases).Error; err != nil {
		return nil, err
	}
	return releases, nil
}

func (client *ReleaseConfigDB) GetReleasesByProjectAndVersion(orgID, projectID int64, version string) ([]Release, error) {
	var releases []Release
	if err := client.Where("org_id = ?", orgID).
		Where("project_id = ?", projectID).
		Where("version = ?", version).
		Find(&releases).Error; err != nil {
		return nil, err
	}
	return releases, nil
}

// GetReleaseNamesByApp Get releaseName list by appID
func (client *ReleaseConfigDB) GetReleaseNamesByApp(orgID, appID int64) ([]string, error) {
	var releaseNames []string
	if orgID == 0 {
		if err := client.Select("release_name").
			Where("application_id = ?", appID).
			Group("release_name").Find(&releaseNames).Error; err != nil {
			return nil, err
		}
	} else {
		if err := client.Select("release_name").
			Where("org_id = ?", orgID).
			Where("application_id = ?", appID).
			Group("release_name").Find(&releaseNames).Error; err != nil {
			return nil, err
		}
	}

	return releaseNames, nil
}

// GetAppIDsByProjectAndVersion Get appIDList by projectID & version
func (client *ReleaseConfigDB) GetAppIDsByProjectAndVersion(projectID int64, version string) ([]int64, error) {
	var appIDs []int64
	if err := client.Select([]string{"application_id"}).
		Where("project_id = ?", projectID).
		Where("version LIKE ?", strutil.Concat(version, "%")).
		Group("application_id").Find(&appIDs).Error; err != nil {
		return nil, err
	}
	return appIDs, nil
}

// GetLatestReleaseByAppAndVersion Get the latest release under the app
func (client *ReleaseConfigDB) GetLatestReleaseByAppAndVersion(appID int64, version string) (*Release, error) {
	var release Release
	if err := client.Where("application_id = ?", appID).
		Where("version LIKE ?", strutil.Concat(version, "%")).
		Order("created_at DESC").
		Limit(1).Find(&release).Error; err != nil {
		return nil, err
	}
	return &release, nil
}

// GetUnReferedReleasesBefore Get the Release that has not been referenced before a given point in time
func (client *ReleaseConfigDB) GetUnReferedReleasesBefore(before time.Time) ([]Release, error) {
	var releases []Release
	if err := client.Where("reference <= ?", 0).Where("updated_at < ?", before).
		Order("updated_at").Find(&releases).Error; err != nil {
		return nil, err
	}
	return releases, nil
}

// GetReleases list releases by release ids
func (client *ReleaseConfigDB) GetReleases(releaseIDs []string) ([]Release, error) {
	var releases []Release
	if err := client.Where("release_id in (?)", releaseIDs).Find(&releases).Error; err != nil {
		return nil, err
	}
	return releases, nil
}

func (client *ReleaseConfigDB) GetReleasesByBranch(projectID, appID int64, gitBranch string) ([]Release, error) {
	var releases []Release
	if err := client.Where("project_id = ?", projectID).Where("application_id = ?", appID).
		Where("git_branch = ?", gitBranch).Find(&releases).Error; err != nil {
		return nil, err
	}
	return releases, nil
}
