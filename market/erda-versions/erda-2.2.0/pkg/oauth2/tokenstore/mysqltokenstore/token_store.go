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

package mysqltokenstore

import (
	"database/sql/driver"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"
	"gopkg.in/oauth2.v3"
	"gopkg.in/oauth2.v3/models"

	"github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// TODO: Move pkg to core-services

type TokenStore struct {
	db         *gorm.DB
	gcDisabled bool
	gcInterval time.Duration
	ticker     *time.Ticker
}

// TokenStoreItem data item
type TokenStoreItem struct {
	ID            string `gorm:"primary_key"`
	Scope         string
	ScopeId       string
	AccessKey     string
	SecretKey     string
	Status        string
	Description   string
	CreatorID     string
	Type          string
	CreatedAt     time.Time
	UpdatedAt     time.Time
	ExpiredAt     *time.Time
	Code          string
	Data          TokenStoreItemData
	Refresh       string
	SoftDeletedAt uint64
}

func (t *TokenStoreItem) BeforeCreate(scope *gorm.Scope) (err error) {
	id, err := uuid.NewRandom()
	if err != nil {
		return
	}
	t.ID = id.String()
	return
}

func (t *TokenStoreItem) ToPbToken() *pb.Token {
	return &pb.Token{
		Id:          t.ID,
		SecretKey:   t.SecretKey,
		AccessKey:   t.AccessKey,
		Status:      t.Status,
		Description: t.Description,
		Scope:       t.Scope,
		ScopeId:     t.ScopeId,
		Type:        t.Type,
		CreatorId:   t.CreatorID,
		CreatedAt:   timestamppb.New(t.CreatedAt),
	}
}

type TokenType string

const (
	OAuth2    TokenType = "OAuth2"
	AccessKey TokenType = "AccessKey"
	PAT       TokenType = "PAT"
)

func (s TokenType) String() string {
	return string(s)
}

func Oauth2Type(db *gorm.DB) *gorm.DB {
	return db.Where("`type` = ?", OAuth2)
}

type TokenStoreItemData struct {
	TokenInfo oauth2.TokenInfo `json:"tokenInfo"`
}

func (data TokenStoreItemData) Value() (driver.Value, error) {
	if b, err := json.Marshal(data); err != nil {
		return nil, errors.Errorf("failed to json encode token data, err: %v", err)
	} else {
		return string(b), nil
	}
}

func (data *TokenStoreItemData) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for token data")
	}
	if len(v) == 0 {
		return nil
	}
	var tm struct {
		TokenInfo *models.Token `json:"tokenInfo"`
	}
	if err := json.Unmarshal(v, &tm); err != nil {
		return errors.Errorf("failed to json decode token data, err: %v", err)
	}
	data.TokenInfo = tm.TokenInfo
	return nil
}

func (TokenStoreItem) TableName() string {
	return "erda_token"
}

// NewTokenStore creates PostgreSQL store instance
func NewTokenStore(options ...TokenStoreOption) (*TokenStore, error) {
	db, err := dbengine.Open()
	if err != nil {
		return nil, err
	}

	store := &TokenStore{
		db:         db.DB,
		gcInterval: 10 * time.Minute,
	}

	for _, o := range options {
		o(store)
	}

	if !store.gcDisabled {
		store.ticker = time.NewTicker(store.gcInterval)
		go store.gc()
	}

	return store, err
}

// Close close the store
func (s *TokenStore) Close() error {
	if !s.gcDisabled {
		s.ticker.Stop()
	}
	return nil
}

func (s *TokenStore) gc() {
	for range s.ticker.C {
		s.clean()
	}
}

func (s *TokenStore) clean() {
	now := time.Unix(time.Now().Unix(), 0)

	err := s.db.Scopes(Oauth2Type).Where("expired_at is not null and expired_at <= ?", now).Or("code='' AND access_key='' AND refresh=''").Delete(&TokenStoreItem{}).Error
	if err != nil {
		logrus.Errorf("[alert] failed to gc clean expired openapi oauth2 token, err: %v", err)
		return
	}
}

// Create create and store the new token information
func (s *TokenStore) Create(info oauth2.TokenInfo) error {
	item := &TokenStoreItem{
		Data:      TokenStoreItemData{TokenInfo: info},
		CreatedAt: time.Unix(time.Now().Unix(), 0),
	}

	if code := info.GetCode(); code != "" {
		item.Code = code
		item.ExpiredAt = handleExpiredAt(info.GetCodeCreateAt(), info.GetCodeExpiresIn())
	} else {
		item.AccessKey = info.GetAccess()
		item.ExpiredAt = handleExpiredAt(info.GetAccessCreateAt(), info.GetAccessExpiresIn())

		if refresh := info.GetRefresh(); refresh != "" {
			item.Refresh = info.GetRefresh()
			item.ExpiredAt = handleExpiredAt(info.GetRefreshCreateAt(), info.GetRefreshExpiresIn())
		}
	}

	return s.db.Create(&TokenStoreItem{
		CreatedAt: item.CreatedAt,
		ExpiredAt: item.ExpiredAt,
		Code:      item.Code,
		AccessKey: item.AccessKey,
		Refresh:   item.Refresh,
		Data:      item.Data,
		Type:      string(OAuth2),
	}).Error
}

func handleExpiredAt(createdAt time.Time, expiredIn time.Duration) *time.Time {
	if expiredIn == 0 {
		return nil
	}
	return &[]time.Time{createdAt.Add(expiredIn)}[0]
}

// RemoveByCode delete the authorization code
func (s *TokenStore) RemoveByCode(code string) error {
	err := s.db.Scopes(Oauth2Type).Where("code = ?", code).Delete(&TokenStoreItem{}).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	return nil
}

// RemoveByAccess use the access token to delete the token information
func (s *TokenStore) RemoveByAccess(access string) error {
	err := s.db.Scopes(Oauth2Type).Where("access_key = ?", access).Delete(&TokenStoreItem{}).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	return nil
}

// RemoveByRefresh use the refresh token to delete the token information
func (s *TokenStore) RemoveByRefresh(refresh string) error {
	err := s.db.Scopes(Oauth2Type).Where("refresh = ?", refresh).Delete(&TokenStoreItem{}).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	return nil
}

// GetByCode use the authorization code for token information data
func (s *TokenStore) GetByCode(code string) (oauth2.TokenInfo, error) {
	if code == "" {
		return nil, nil
	}

	var tokenItem TokenStoreItem
	err := s.db.Scopes(Oauth2Type).Where("code = ?", code).First(&tokenItem).Error
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return nil, err
	}
	return tokenItem.Data.TokenInfo, nil
}

// GetByAccess use the access token for token information data
func (s *TokenStore) GetByAccess(access string) (oauth2.TokenInfo, error) {
	if access == "" {
		return nil, nil
	}

	var tokenItem TokenStoreItem
	err := s.db.Scopes(Oauth2Type).Where("access_key = ?", access).First(&tokenItem).Error
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return nil, err
	}
	return tokenItem.Data.TokenInfo, nil
}

// GetByRefresh use the refresh token for token information data
func (s *TokenStore) GetByRefresh(refresh string) (oauth2.TokenInfo, error) {
	if refresh == "" {
		return nil, nil
	}

	var tokenItem TokenStoreItem
	err := s.db.Scopes(Oauth2Type).Where("refresh = ?", refresh).First(&tokenItem).Error
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return nil, err
	}
	return tokenItem.Data.TokenInfo, nil
}
