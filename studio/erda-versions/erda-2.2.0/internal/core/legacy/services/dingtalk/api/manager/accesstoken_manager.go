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

package manager

import (
	"fmt"
	"sync"
	"time"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/interfaces"
	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/native"
)

var appKeySecrets = &sync.Map{}
var requestLocks = &sync.Map{}

func (m *Manager) RegisterApp(appKey, appSecret string) interfaces.DingtalkAccessTokenManager {
	if secret, ok := appKeySecrets.Load(appKey); ok && secret == appSecret {
		return m
	}

	m.lock.Lock()
	defer m.lock.Unlock()

	if secret, ok := appKeySecrets.Load(appKey); ok && secret == appSecret {
		return m
	}
	appKeySecrets.Store(appKey, appSecret)
	requestLocks.Store(appKey, &sync.Mutex{})
	return m
}

func (m *Manager) GetAccessToken(appKey string) (string, error) {
	cacheKey := m.getAccessTokenCacheKey(appKey)
	result, err := m.Cache.Get(cacheKey)
	if err != nil && err != redis.Nil {
		return "", err
	}
	if len(result) > 0 {
		// todo: sliding extend the expire time aysnc
		return result, nil
	}

	secret, ok := appKeySecrets.Load(appKey)
	if !ok {
		return "", fmt.Errorf("appSecret not registered")
	}
	requestLock, ok := requestLocks.Load(appKey)
	if !ok {
		return "", fmt.Errorf("request lock is nil")
	}

	requestLock.(*sync.Mutex).Lock()
	defer requestLock.(*sync.Mutex).Unlock()

	result, err = m.Cache.Get(cacheKey)
	if len(result) > 0 {
		return result, nil
	}

	accessToken, expireIn, err := native.GetAccessToken(appKey, secret.(string))
	if err != nil {
		return "", err
	}
	_, err = m.Cache.Set(cacheKey, accessToken, time.Duration(expireIn)*time.Second-10*time.Minute)
	return accessToken, nil
}

func (m *Manager) getAccessTokenCacheKey(appKey string) string {
	return "erda_dingtalk_ak_" + appKey
}
