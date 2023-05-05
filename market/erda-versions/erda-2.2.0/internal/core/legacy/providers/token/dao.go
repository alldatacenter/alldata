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

package token

import (
	"context"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/pkg/common/errors"
	tokenstore "github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
)

type Dao interface {
	QueryToken(ctx context.Context, req *pb.QueryTokensRequest) ([]tokenstore.TokenStoreItem, int64, error)
	CreateToken(ctx context.Context, obj tokenstore.TokenStoreItem) (*tokenstore.TokenStoreItem, error)
	GetToken(ctx context.Context, req *pb.GetTokenRequest) (*tokenstore.TokenStoreItem, error)
	UpdateToken(ctx context.Context, req *pb.UpdateTokenRequest) error
	DeleteToken(ctx context.Context, req *pb.DeleteTokenRequest) error
}

type dao struct {
	db *gorm.DB
}

func NotDeleted(db *gorm.DB) *gorm.DB {
	return db.Where("soft_deleted_at = ?", 0)
}

func (d *dao) QueryToken(ctx context.Context, req *pb.QueryTokensRequest) ([]tokenstore.TokenStoreItem, int64, error) {
	var objs []tokenstore.TokenStoreItem
	q := d.db.Model(&tokenstore.TokenStoreItem{}).Scopes(NotDeleted).Order("created_at desc")
	where := make(map[string]interface{})
	if req.Status != "" {
		where["status"] = req.Status
	}
	if req.Scope != "" {
		where["scope"] = req.Scope
	}
	if req.ScopeId != "" {
		where["scope_id"] = req.ScopeId
	}
	if req.Type != "" {
		where["type"] = req.Type
	}
	if req.Access != "" {
		where["access_key"] = req.Access
	}
	if req.CreatorId != "" {
		where["creator_id"] = req.CreatorId
	}

	var count int64
	cres := q.Where(where).Count(&count)
	if cres.Error != nil {
		return nil, 0, cres.Error
	}

	if req.PageNo > 0 && req.PageSize > 0 {
		q = q.Offset((req.PageNo - 1) * req.PageSize).Limit(req.PageSize)
	}
	res := q.Where(where).Find(&objs)
	if res.Error != nil {
		return nil, 0, res.Error
	}

	if cres.Error != nil {
		return nil, 0, res.Error
	}

	return objs, count, nil
}

func (d *dao) CreateToken(ctx context.Context, obj tokenstore.TokenStoreItem) (*tokenstore.TokenStoreItem, error) {
	q := d.db.Create(&obj)
	if q.Error != nil {
		return nil, q.Error
	}
	return &obj, nil
}

func (d *dao) GetToken(ctx context.Context, req *pb.GetTokenRequest) (*tokenstore.TokenStoreItem, error) {
	var obj tokenstore.TokenStoreItem
	q := d.db.Where(&tokenstore.TokenStoreItem{ID: req.Id}).Scopes(NotDeleted).Find(&obj)
	if q.RecordNotFound() {
		return nil, errors.NewNotFoundError("token")
	}
	if q.Error != nil {
		return nil, q.Error
	}
	return &obj, nil
}

func (d *dao) UpdateToken(ctx context.Context, req *pb.UpdateTokenRequest) error {
	q := d.db.Model(&tokenstore.TokenStoreItem{}).Scopes(NotDeleted).Where(&tokenstore.TokenStoreItem{ID: req.Id})
	updated := tokenstore.TokenStoreItem{}
	if req.Status != "" {
		updated.Status = req.Status
	}
	if req.Description != "" {
		updated.Description = req.Description
	}
	q = q.Update(updated)
	return q.Error
}

func (d *dao) DeleteToken(ctx context.Context, req *pb.DeleteTokenRequest) error {
	return d.db.Model(&tokenstore.TokenStoreItem{}).Scopes(NotDeleted).Where(&tokenstore.TokenStoreItem{ID: req.Id}).
		Update(map[string]interface{}{"soft_deleted_at": time.Now().UnixNano() / 1e6}).Error
}
