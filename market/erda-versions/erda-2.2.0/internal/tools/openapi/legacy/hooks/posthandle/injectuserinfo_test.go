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

package posthandle

import (
	"io/ioutil"
	"net/http"
	"strings"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

func TestInjectUserInfo(t *testing.T) {
	monkey.Patch(GetUsers, func(IDs []string, needDesensitize bool) (map[string]apistructs.UserInfo, error) {
		return map[string]apistructs.UserInfo{
			"1": {
				ID:     "123",
				Name:   "name",
				Avatar: "avatar_url1",
				Phone:  "123",
			},
			"2": {
				ID:     "314",
				Name:   "name",
				Avatar: "avatar_url2",
				Phone:  "312",
			},
		}, nil
	})
	r := http.Response{Header: http.Header{}, Body: ioutil.NopCloser(strings.NewReader(`{"userIDs": ["123", "2345"], "data": {"a": "1", "b": 2}}`))}
	assert.Nil(t, InjectUserInfo(&r, false))
	body, err := ioutil.ReadAll(r.Body)
	assert.Nil(t, err)
	assert.True(t, strutil.Contains(string(body), "userInfo"))
	assert.True(t, strutil.Contains(string(body), "data"))
}

func TestInjectUserInfo2(t *testing.T) {
	originbody := `{"NOuserIDs":["123","2345"],"data":{"a":"1","b":2}}`
	r := http.Response{Body: ioutil.NopCloser(strings.NewReader(originbody))}
	assert.Nil(t, InjectUserInfo(&r, false))
	body, err := ioutil.ReadAll(r.Body)
	assert.Nil(t, err)
	assert.Equal(t, len(originbody), len(string(body)))
}
