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
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/core/legacy/services/dingtalk/api/native"
)

func Test_GetUserIdsByPhones_Should_Success(t *testing.T) {
	m := NewManager(nil, &MockCache{})

	monkey.Unpatch(native.GetUserIdByMobile)
	monkey.Patch(native.GetUserIdByMobile, func(accessToken string, mobile string) (userId string, err error) {
		return "userid_" + mobile, nil
	})

	userIds, err := m.GetUserIdsByPhones("mock_accesstoken", 123, []string{"17139483930", "123232323"})
	if err != nil {
		t.Errorf("should not error")
	}

	if len(userIds) != 2 {
		t.Errorf("userIds should not empty")
	}
	fmt.Println(userIds)
}
