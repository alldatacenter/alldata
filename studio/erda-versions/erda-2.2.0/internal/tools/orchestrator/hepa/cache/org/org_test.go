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

package org

import (
	"testing"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/cache"
)

func TestUserCanAccessTheApp(t *testing.T) {
	scopeAccess = cache.New(scopeAccessName, time.Minute, func(i interface{}) (interface{}, bool) {
		us := i.(userScope)
		time.Sleep(time.Millisecond * 100)
		return &apistructs.ScopeRole{
			Scope: apistructs.Scope{
				Type: us.Scope,
				ID:   us.UserID,
			},
			Access: us.Scope == apistructs.AppScope,
			Roles:  nil,
		}, true
	})

	if ok := UserCanAccessTheApp("100001", "1000"); !ok {
		t.Error("it should not be false")
	}
	if ok := UserCanAccessTheProject("100001", "1000"); ok {
		t.Error("it should not be true")
	}
}
