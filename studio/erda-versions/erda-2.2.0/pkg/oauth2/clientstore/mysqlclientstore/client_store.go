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

package mysqlclientstore

import (
	"time"

	"github.com/jinzhu/gorm"
	"gopkg.in/oauth2.v3"
	"gopkg.in/oauth2.v3/models"

	"github.com/erda-project/erda/pkg/database/dbengine"
)

type ClientStore struct {
	db *gorm.DB
}

// ClientStoreItem data item
type ClientStoreItem struct {
	ID        string `gorm:"primary_key"`
	Secret    string
	Domain    string
	CreatedAt time.Time
	UpdatedAt time.Time
}

func (ClientStoreItem) TableName() string {
	return "erda_token_client"
}

// NewClientStore creates PostgreSQL store instance
func NewClientStore() (*ClientStore, error) {
	db, err := dbengine.Open()
	if err != nil {
		return nil, err
	}
	store := &ClientStore{db: db.DB}
	return store, err
}

func (s *ClientStore) toClientInfo(item ClientStoreItem) oauth2.ClientInfo {
	return &models.Client{ID: item.ID, Secret: item.Secret}
}

// GetByID retrieves and returns client information by id
func (s *ClientStore) GetByID(id string) (oauth2.ClientInfo, error) {
	if id == "" {
		return nil, nil
	}

	var item ClientStoreItem
	if err := s.db.Where("id = ?", id).Find(&item).Error; err != nil {
		return nil, err
	}

	return s.toClientInfo(item), nil
}

// Create creates and stores the new client information
func (s *ClientStore) Create(info oauth2.ClientInfo) error {
	return s.db.Create(&ClientStoreItem{
		ID:        info.GetID(),
		Secret:    info.GetSecret(),
		Domain:    info.GetDomain(),
		CreatedAt: time.Time{},
		UpdatedAt: time.Time{},
	}).Error
}
