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

package backend

import (
	"path/filepath"
	"strconv"
	"testing"
	"time"

	// "github.com/erda-project/erda/pkg/jsonstore"

	"github.com/stretchr/testify/assert"
)

// func TestEtcdBackend(t *testing.T) {
// 	js, err := jsonstore.New()
// 	assert.Nil(t, err)
// 	stat := NewEtcd(js)
// 	assert.Nil(t, stat.Clear(time.Now()))
// 	stat.Emit("tag1", 1)
// 	stat.Emit("tag2", 2)
// 	stat.Emit("tag2", 3)
// 	stat.Emit("tag1", 4)

// 	time.Sleep(61 * time.Second)

// 	r, err := stat.Last1Day()
// 	assert.Equal(t, int64(5), r["tag1"])
// 	assert.Equal(t, int64(5), r["tag2"])
// }

func TestClearDir(t *testing.T) {
	now := time.Now()
	dir := clearDir("aa", 2, now)
	before := now.Add(-3 * 24 * time.Hour)
	d := filepath.Join("aa", strconv.Itoa(before.Year()), strconv.Itoa(int(before.Month())), strconv.Itoa(before.Day()))
	assert.Equal(t, d, dir)

}
