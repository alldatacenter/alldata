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

package dbclient

import (
	"time"

	"github.com/google/uuid"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
)

type MySQLAccount struct {
	ID                string `gorm:"primary_key"`
	CreatedAt         time.Time
	UpdatedAt         time.Time
	Username          string
	Password          string
	KMSKey            string
	InstanceID        string
	RoutingInstanceID string
	Creator           string
	IsDeleted         bool
}

func (MySQLAccount) TableName() string {
	return "erda_addon_mysql_account"
}

func NotDeleted(db *gorm.DB) *gorm.DB {
	return db.Where("is_deleted = 0")
}

// GetMySQLAccountByID returns a MySQLAccount by ID
func (db *DBClient) GetMySQLAccountByID(id string) (*MySQLAccount, error) {
	var account MySQLAccount
	err := db.Scopes(NotDeleted).Find(&account, "id = ?", id).Error
	if err != nil {
		return nil, errors.Wrapf(err, "GetMySQLAccountByID: %s", id)
	}
	return &account, nil
}

// GetMySQLAccountListByRoutingInstanceID returns a list of MySQLAccount for a given routing instance
func (db *DBClient) GetMySQLAccountListByRoutingInstanceID(routingInstanceID string) ([]MySQLAccount, error) {
	if routingInstanceID == "" {
		return nil, nil
	}
	var accounts []MySQLAccount
	if err := db.Scopes(NotDeleted).
		Where("routing_instance_id = ?", routingInstanceID).
		Order("created_at desc").
		Find(&accounts).Error; err != nil {
		return nil, errors.Wrapf(err, "GetMySQLAccountListByRoutingInstanceID: %s", routingInstanceID)
	}
	return accounts, nil
}

// CreateMySQLAccount creates a new MySQLAccount
func (db *DBClient) CreateMySQLAccount(account *MySQLAccount) error {
	id, err := uuid.NewRandom()
	if err != nil {
		return err
	}
	account.ID = id.String()
	now := time.Now()
	account.CreatedAt = now
	account.UpdatedAt = now
	account.IsDeleted = false
	if err := db.Create(account).Error; err != nil {
		return errors.Wrapf(err, "CreateMySQLAccount: %+v", account)
	}
	return nil
}

// UpdateMySQLAccount updates an existing MySQLAccount
func (db *DBClient) UpdateMySQLAccount(account *MySQLAccount) error {
	if err := db.Save(account).Error; err != nil {
		return errors.Wrapf(err, "UpdateMySQLAccount: %+v", account)
	}
	return nil
}
