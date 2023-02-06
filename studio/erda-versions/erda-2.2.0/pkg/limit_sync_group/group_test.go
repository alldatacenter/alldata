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

package limit_sync_group

import (
	"testing"

	"github.com/alecthomas/assert"
)

func Test_Work(t *testing.T) {
	work := NewWorker(5)
	var num uint64
	var workNum = 5
	for i := 0; i < 5; i++ {
		work.AddFunc(func(locker *Locker, i ...interface{}) error {
			locker.Lock()
			num++
			locker.Unlock()
			return nil
		})
	}
	err := work.Do().Error()
	assert.NoError(t, err)
	assert.Equal(t, num, uint64(workNum))
}

func testLimitSyncGroup(t *testing.T, wg1 *limitSyncGroup, wg2 *limitSyncGroup) {
	n := 16
	wg1.Add(n)
	wg2.Add(n)
	exited := make(chan bool, n)
	for i := 0; i != n; i++ {
		go func() {
			wg1.Done()
			wg2.Wait()
			exited <- true
		}()
	}
	wg1.Wait()
	for i := 0; i != n; i++ {
		select {
		case <-exited:
			t.Fatal("WaitGroup released group too soon")
		default:
		}
		wg2.Done()
	}
	for i := 0; i != n; i++ {
		<-exited // Will block if barrier fails to unlock someone.
	}
}

func TestLimitSyncGroup(t *testing.T) {
	n := 5
	wg := NewSemaphore(n)
	wg.Add(n)
	go func() {
		for i := 0; i < n; i++ {
			wg.Done()
		}
	}()
	wg.Wait()
}

func TestLimitSyncGroup2(t *testing.T) {
	n := 20
	wg1 := NewSemaphore(n)
	wg2 := NewSemaphore(n)
	for i := 0; i != 20; i++ {
		testLimitSyncGroup(t, wg1, wg2)
	}
}
