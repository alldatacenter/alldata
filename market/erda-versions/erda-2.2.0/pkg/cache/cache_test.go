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

package cache_test

import (
	"testing"
	"time"

	"github.com/erda-project/erda/pkg/cache"
)

func TestNew(t *testing.T) {
	var c = cache.New("demo", time.Millisecond*100, func(i interface{}) (interface{}, bool) {
		time.Sleep(time.Millisecond * 100)
		ii := i.(int)
		return ii * ii, true
	})
	t.Log(c.Name())
	var cost = [10]int64{}
	t.Log("first time LoadWithUpdate")
	for i := 0; i < 10; i++ {
		now := time.Now()
		c.LoadWithUpdate(i)
		cost[i] = time.Now().Sub(now).Microseconds()
	}
	var costWithCache = [10]int64{}
	time.Sleep(time.Millisecond * 100)
	t.Log("second time LoadWithUpdate")
	for i := 0; i < 10; i++ {
		now := time.Now()
		c.LoadWithUpdate(i)
		costWithCache[i] = time.Now().Sub(now).Microseconds()
	}
	for i := 0; i < 10; i++ {
		t.Logf("[%d] cost without cache: %d, cost with cache: %d", i, cost[i], costWithCache[i])
	}
}
