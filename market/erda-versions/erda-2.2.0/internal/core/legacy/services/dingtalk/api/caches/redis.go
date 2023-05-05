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

package caches

import (
	"time"

	"github.com/go-redis/redis"
)

type redisCache struct {
	client *redis.Client
}

func NewRedis(redis *redis.Client) *redisCache {
	return &redisCache{
		client: redis,
	}
}

func (r *redisCache) Get(key string) (string, error) {
	return r.client.Get(key).Result()
}

func (r *redisCache) Set(key string, value string, expire time.Duration) (string, error) {
	return r.client.Set(key, value, expire).Result()
}
