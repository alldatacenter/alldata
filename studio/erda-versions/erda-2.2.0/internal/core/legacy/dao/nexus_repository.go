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

package dao

import (
	"database/sql/driver"
	"encoding/json"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/nexus"
	"github.com/erda-project/erda/pkg/strutil"
)

type NexusRepository struct {
	BaseModel

	OrgID       *uint64 `json:"orgID"`       // 所属 org ID
	PublisherID *uint64 `json:"publisherID"` // 所属 publisher ID
	ClusterName string  `json:"clusterName"` // 所属集群

	Name   string                 `json:"name"`   // unique name
	Format nexus.RepositoryFormat `json:"format"` // maven2 / npm / ...
	Type   nexus.RepositoryType   `json:"type"`   // group / proxy / hosted
	Config NexusRepositoryConfig  `json:"config"` // repo config
}

type NexusRepositoryConfig nexus.Repository

func (NexusRepository) TableName() string {
	return "dice_nexus_repositories"
}

func (config NexusRepositoryConfig) Value() (driver.Value, error) {
	if b, err := json.Marshal(config); err != nil {
		return nil, errors.Wrapf(err, "failed to marshal repository config")
	} else {
		return string(b), nil
	}
}
func (config *NexusRepositoryConfig) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for repository config")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, config); err != nil {
		return errors.Wrapf(err, "failed to unmarshal repository config")
	}
	return nil
}

func (client *DBClient) CreateOrUpdateNexusRepository(repo *NexusRepository) error {
	var query NexusRepository
	err := client.Where("name = ?", repo.Name).First(&query).Error
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
		// not found, need create
		return client.Create(repo).Error
	}
	// already exist, do update
	repo.ID = query.ID
	repo.CreatedAt = query.CreatedAt
	return client.Save(repo).Error
}

func (client *DBClient) GetNexusRepository(id uint64) (*NexusRepository, error) {
	var repo NexusRepository
	if err := client.First(&repo, id).Error; err != nil {
		return nil, err
	}
	return &repo, nil
}

func (client *DBClient) GetNexusRepositoryByName(name string) (*NexusRepository, error) {
	var repo NexusRepository
	if err := client.Where("name = ?", name).First(&repo).Error; err != nil {
		return nil, err
	}
	return &repo, nil
}

func (client *DBClient) ListNexusRepositories(req apistructs.NexusRepositoryListRequest) ([]*NexusRepository, error) {
	query := client.New()
	if req.IDs != nil {
		query = query.Where("id IN (?)", req.IDs)
	}
	if req.OrgID != nil {
		query = query.Where("org_id = ?", req.OrgID)
	}
	if req.PublisherID != nil {
		query = query.Where("publisher_id = ?", req.PublisherID)
	}
	if len(req.Formats) > 0 {
		query = query.Where("format IN (?)", req.Formats)
	}
	if len(req.Types) > 0 {
		query = query.Where("type IN (?)", req.Types)
	}
	if len(req.NameContains) > 0 {
		for _, contain := range req.NameContains {
			query = query.Or("name LIKE ?", strutil.Concat("%", contain, "%"))
		}
	}

	var repos []*NexusRepository
	if err := query.Debug().Find(&repos).Error; err != nil {
		return nil, err
	}
	return repos, nil
}
