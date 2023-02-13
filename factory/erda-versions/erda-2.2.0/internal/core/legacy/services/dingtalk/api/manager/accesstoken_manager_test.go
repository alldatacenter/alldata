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
	"sync"
	"testing"
	"time"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/native"
)

func Test_RegisterApp_WithMultipleTimes_Should_CreateOnlyOneRequestLock(t *testing.T) {
	m := NewManager(nil, nil)
	wg := sync.WaitGroup{}
	wg.Add(1000)

	for i := 0; i < 1000; i++ {
		go func() {
			tm := m.RegisterApp("mock_appkey", "mock_appsecret")
			if tm == nil {
				t.Errorf("GetAccessTokenManager should not return nil")
			}
			wg.Done()
		}()
	}
	wg.Wait()

	count := 0
	requestLocks.Range(func(key, value interface{}) bool {
		count++
		return true
	})

	if count != 1 {
		t.Errorf("concurrency get token manager for same key, should create only one lock")
	}
}

func Test_GetAccessToken_Should_Success(t *testing.T) {
	tokenFromApi := "mock_api_accesstoken"
	tokenFromCache := ""

	defer monkey.Unpatch(native.GetAccessToken)
	monkey.Patch(native.GetAccessToken, func(appKey, appSecret string) (accessToken string, expireIn int64, err error) {
		return tokenFromApi, 7200, nil
	})

	m := NewManager(nil, &MockCache{GetResult: tokenFromCache})
	m.RegisterApp("mock_appkey", "mock_secret")

	token, err := m.GetAccessToken("mock_appkey")
	if err != nil {
		t.Errorf("should not error: %s", err)
	}
	if token != tokenFromApi {
		t.Errorf("GetAccessToken expect: %s, but got: %s", tokenFromApi, token)
	}
}

type MockCache struct {
	GetResult string
}

func (r *MockCache) Get(key string) (string, error) {
	return r.GetResult, nil
}

func (r *MockCache) Set(key string, value string, expire time.Duration) (string, error) {
	return "", nil
}
