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
	"strconv"
	"strings"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
)

type (
	Key   = int64
	Value = *pb.Checker
)

// Cache .
type Cache struct {
	root  string
	redis *redis.Client
}

// New .
func New(root string, redis *redis.Client) *Cache {
	return &Cache{
		root:  root,
		redis: redis,
	}
}

func (c *Cache) Keys() ([]Key, error) {
	list, err := c.redis.HKeys(c.root).Result()
	if err != nil {
		return nil, err
	}
	var keys []Key
	for _, item := range list {
		v, err := strconv.ParseInt(item, 10, 64)
		if err != nil {
			continue
		}
		keys = append(keys, v)
	}
	return keys, nil
}

func (c *Cache) ListByKeys(keys []Key) (map[Key]Value, error) {
	if len(keys) <= 0 {
		return nil, nil
	}
	var fields []string
	for _, k := range keys {
		fields = append(fields, strconv.FormatInt(k, 10))
	}
	vals, err := c.redis.HMGet(c.root, fields...).Result()
	if err != nil {
		return nil, err
	}
	values := make(map[Key]Value)
	for i, val := range vals {
		if v, ok := val.(string); ok && len(v) > 0 {
			var out pb.Checker
			err := json.NewDecoder(strings.NewReader(v)).Decode(&out)
			if err != nil {
				continue
			}
			values[keys[i]] = &out
		}
	}
	return values, nil
}

func (c *Cache) Put(data *pb.Checker) error {
	if data == nil {
		return nil
	}
	sb := &strings.Builder{}
	_ = json.NewEncoder(sb).Encode(data)
	return c.redis.HSet(c.root, strconv.FormatInt(data.Id, 10), sb.String()).Err()
}

func (c *Cache) Remove(key Key) error {
	return c.redis.HDel(c.root, strconv.FormatInt(key, 10)).Err()
}

func (c *Cache) Contains(key Key) (bool, error) {
	return c.redis.HExists(c.root, strconv.FormatInt(key, 10)).Result()
}

// CacheStorage .
type CacheStorage struct {
	*Cache
}

func (s *CacheStorage) ListIDs() ([]int64, error) {
	return s.Cache.Keys()
}

func (s *CacheStorage) ListByIDs(ids []int64) (map[int64]*pb.Checker, error) {
	return s.Cache.ListByKeys(ids)
}
