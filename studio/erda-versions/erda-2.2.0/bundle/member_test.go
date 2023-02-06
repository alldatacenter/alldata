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

package bundle

//import (
//	"os"
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestGetMemberByToken(t *testing.T) {
//	// set env
//	os.Setenv("CMDB_ADDR", "http://cmdb.default.svc.cluster.local:9093/")
//
//	bdl := New(WithCMDB())
//	request := &apistructs.GetMemberByTokenRequest{
//		Token: "333333",
//	}
//
//	member, err := bdl.GetMemberByToken(request)
//	assert.Nil(t, err)
//	println(member.Email)
//}
