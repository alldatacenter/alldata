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
)

func Test_GetClient_WithMultipleTimes_Should_Success(t *testing.T) {
	m := NewManager(nil, nil)
	wg := sync.WaitGroup{}
	wg.Add(1000)

	for i := 0; i < 1000; i++ {
		go func() {
			client := m.GetClient("mock_appkey", "mock_appsecret", 123)
			if client == nil {
				t.Errorf("client factory should not return nil")
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
		t.Errorf("concurrency get same client, should create only one lock")
	}
}
