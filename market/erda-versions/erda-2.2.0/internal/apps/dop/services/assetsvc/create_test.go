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

package assetsvc

import "testing"

func Test_accessDetailPath(t *testing.T) {
	var (
		orgName              = "erda"
		accessPrimary uint64 = 1
		result               = "/erda/workBench/apiManage/access-manage/detail/1"
	)
	if p := accessDetailPath(orgName, accessPrimary); p != result {
		t.Fatal("fatal to make accessDetailPath", p, result)
	}
}

func Test_myClientsPath(t *testing.T) {
	var (
		orgName              = "erda"
		clientPrimary uint64 = 1
		result               = "/erda/workBench/apiManage/client/1"
	)
	if p := myClientsPath(orgName, clientPrimary); p != result {
		t.Fatal("fatal to make myClientsPath")
	}
}
