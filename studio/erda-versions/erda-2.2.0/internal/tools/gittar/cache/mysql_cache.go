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

package cache

import (
	"encoding/json"
	"errors"
	"sync"
	"sync/atomic"
	"time"

	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
)

type AtomicInt int64

type MysqlCache struct {
	mutex      sync.RWMutex
	typeName   string
	hits, gets AtomicInt
	db         *models.DBClient
}

func NewMysqlCache(typeName string, db *models.DBClient) *MysqlCache {
	return &MysqlCache{
		typeName: typeName,
		db:       db,
	}
}

//Status return the status of cache
func (c *MysqlCache) Status() *gitmodule.CacheStatus {
	status := &gitmodule.CacheStatus{
		MaxItemSize: -1,
		Hits:        c.hits.Get(),
		Gets:        c.gets.Get(),
	}
	currentSize := -1
	c.db.Model(&models.RepoCache{}).Where("type_name = ?", c.typeName).Count(&currentSize)
	status.CurrentSize = currentSize
	return status
}

//Get value with key
func (c *MysqlCache) Get(key string, outValue interface{}) error {
	c.gets.Add(1)
	var repoCache models.RepoCache
	err := c.db.Where("type_name = ? and key_name = ? ", c.typeName, key).First(&repoCache).Error
	if err == nil {
		c.hits.Add(1)
		err := json.Unmarshal([]byte(repoCache.Value), outValue)
		return err
	}
	return errors.New("key not found")
}

//Set a value with key
func (c *MysqlCache) Set(key string, value interface{}) error {
	bytes, err := json.Marshal(value)
	if err != nil {
		return err
	}

	var repoCache models.RepoCache
	err = c.db.Where("type_name = ? and key_name = ? ", c.typeName, key).First(&repoCache).Error
	if err != nil {
		//不存在 新增
		repoCache := models.RepoCache{
			TypeName:  c.typeName,
			KeyName:   key,
			Value:     string(bytes),
			CreatedAt: time.Now(),
		}
		err := c.db.Create(&repoCache).Error
		if err != nil {
			return err
		}
	} else {
		//已经存在 更新
		now := time.Now()
		repoCache.Value = string(bytes)
		repoCache.UpdatedAt = &now
		err := c.db.Save(&repoCache).Error
		if err != nil {
			return err
		}
	}
	return nil
}

//Delete delete the key
func (c *MysqlCache) Delete(key string) error {
	err := c.db.Where("type_name = ? and key_name = ? ", c.typeName, key).Delete(models.RepoCache{}).Error
	return err
}

// Add atomically adds n to i.
func (i *AtomicInt) Add(n int64) {
	atomic.AddInt64((*int64)(i), n)
}

// Get atomically gets the value of i.
func (i *AtomicInt) Get() int64 {
	return atomic.LoadInt64((*int64)(i))
}
