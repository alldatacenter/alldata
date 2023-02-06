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

package queue

// func TestPusherQueue_Lock(t *testing.T) {
// 	q := &PusherQueue{
// 		redisClient: redis.NewClient(&redis.Options{
// 			Addr: "127.0.0.1:6379",
// 		}),
// 	}
//
// 	// first lock
// 	r, err := q.Lock(DEPLOY_CONTINUING, "123")
// 	if assert.NoError(t, err) {
// 		assert.True(t, r)
// 	}
//
// 	// try lock twice, must fail
// 	r, err = q.Lock(DEPLOY_CONTINUING, "123")
// 	if assert.NoError(t, err) {
// 		assert.False(t, r)
// 	}
//
// 	// unlock
// 	r, err = q.Unlock(DEPLOY_CONTINUING, "123")
// 	if assert.NoError(t, err) {
// 		assert.True(t, r)
// 	}
//
// 	// then lock success
// 	r, err = q.Lock(DEPLOY_CONTINUING, "123")
// 	if assert.NoError(t, err) {
// 		assert.True(t, r)
// 	}
// }
