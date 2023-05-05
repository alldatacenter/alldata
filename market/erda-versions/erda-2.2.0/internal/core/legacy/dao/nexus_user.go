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
)

type NexusUser struct {
	BaseModel

	RepoID      *uint64 `json:"repoID"`      // 所属 repo，可以为空
	PublisherID *uint64 `json:"publisherID"` // 所属 publisher，可以为空
	OrgID       *uint64 `json:"orgID"`       // 所属 org，可以为空
	ClusterName string  `json:"clusterName"` // 所属集群

	Name     string          `json:"name"`
	Password string          `json:"password"` // 加密存储
	Config   NexusUserConfig `json:"config"`
}

type NexusUserConfig nexus.User

func (config NexusUserConfig) Value() (driver.Value, error) {
	if b, err := json.Marshal(config); err != nil {
		return nil, errors.Wrapf(err, "failed to marshal user config")
	} else {
		return string(b), nil
	}
}
func (config *NexusUserConfig) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for user config")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, config); err != nil {
		return errors.Wrapf(err, "failed to unmarshal user config")
	}
	return nil
}

func (NexusUser) TableName() string {
	return "dice_nexus_users"
}

func (client *DBClient) CreateOrUpdateNexusUser(user *NexusUser) error {
	var query NexusUser
	err := client.Where("name = ?", user.Name).First(&query).Error
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return err
		}
		// not found, need create
		return client.Create(user).Error
	}
	// already exist, do update
	user.ID = query.ID
	user.CreatedAt = query.CreatedAt
	return client.Save(user).Error
}

func (client *DBClient) GetNexusUser(id uint64) (*NexusUser, error) {
	var user NexusUser
	if err := client.First(&user, id).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (client *DBClient) GetNexusUserByName(name string) (*NexusUser, error) {
	var user NexusUser
	if err := client.Where("name = ?", name).First(&user).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (client *DBClient) ListNexusUsers(req apistructs.NexusUserListRequest) ([]NexusUser, error) {
	query := client.New()
	if req.UserIDs != nil {
		query = query.Where("id IN (?)", req.UserIDs)
	}
	if req.RepoID != nil {
		query = query.Where("repo_id = ?", req.RepoID)
	}
	if req.PublisherID != nil {
		query = query.Where("publisher_id = ?", req.PublisherID)
	}

	var users []NexusUser
	if err := query.Find(&users).Error; err != nil {
		return nil, err
	}
	return users, nil
}
