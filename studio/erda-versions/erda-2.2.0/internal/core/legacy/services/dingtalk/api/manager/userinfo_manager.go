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

	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/native"
)

func (m *Manager) GetUserIdsByPhones(accessToken string, agentId int64, phones []string) (userIds []string, err error) {
	results := sync.Map{}
	ctx := NewTaskContext(10, &results)

	for _, phone := range phones {
		ctx.Add()
		go m.getUserIdByPhone(ctx, accessToken, agentId, phone)
	}
	ctx.Wait()

	results.Range(func(key, value interface{}) bool {
		userIds = append(userIds, value.(string))
		return true
	})

	if len(userIds) == 0 {
		return nil, fmt.Errorf("fail to get userids by phone")
	}
	return userIds, err
}

func (m *Manager) getUserIdByPhone(ctx *TaskContext, accessToken string, agentId int64, phone string) {
	defer ctx.Done()

	cacheKey := m.getUserIdCacheKey(agentId, phone)
	userId, err := m.Cache.Get(cacheKey)
	if err != nil {
		m.Log.Errorf("redis get(%s) failed: %s", cacheKey, err)
	}
	if len(userId) > 0 {
		results := ctx.result.(*sync.Map)
		results.Store(phone, userId)
		return
	}

	userId, err = native.GetUserIdByMobile(accessToken, phone)
	if err == nil && len(userId) > 0 {
		results := ctx.result.(*sync.Map)
		results.Store(phone, userId)
		m.Cache.Set(cacheKey, userId, 30*24*time.Hour)
	} else {
		m.Log.Errorf("getUserIdByPhone failed: %s", err)
	}
}

func (m *Manager) getUserIdCacheKey(agentId int64, phone string) string {
	return fmt.Sprintf("erda_dingtalk_uid_%d_%s", agentId, phone)
}
