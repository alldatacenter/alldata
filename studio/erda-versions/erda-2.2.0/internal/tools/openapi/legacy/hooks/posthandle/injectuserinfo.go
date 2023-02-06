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
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"sync"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/pkg/desensitize"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/ucauth"
)

var (
	once sync.Once
	uc   *ucauth.UCClient
)

// InjectUserInfo 对 resp 的 body 中注入 userinfo
func InjectUserInfo(resp *http.Response, needDesensitize bool) error {
	content, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return err
	}
	var bodyjson map[string]interface{}
	if err := json.Unmarshal(content, &bodyjson); err != nil {
		// response body 结构不是 JSON Object, 忽略这种情况即可
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}
	userIDs, ok := bodyjson["userIDs"]
	if !ok {
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return nil
	}
	userInfoMap := map[string]apistructs.UserInfo{}
	switch v := userIDs.(type) {
	case []interface{}:
		ids := make([]string, 0, len(v))
		for _, id := range v {
			idstr, ok := id.(string)
			if !ok {
				resp.Body = ioutil.NopCloser(bytes.NewReader(content))
				return fmt.Errorf("failed to inject userinfo, invalid type of userIDs, id: %v", id)
			}
			ids = append(ids, idstr)
		}
		var err error
		if userInfoMap, err = GetUsers(ids, needDesensitize); err != nil {
			resp.Body = ioutil.NopCloser(bytes.NewReader(content))
			return err
		}
	case []string:
		var err error
		if userInfoMap, err = GetUsers(v, needDesensitize); err != nil {
			resp.Body = ioutil.NopCloser(bytes.NewReader(content))
			return err
		}
	}
	// inject to response body
	bodyjson["userInfo"] = userInfoMap
	newbody, err := json.Marshal(bodyjson)
	if err != nil {
		resp.Body = ioutil.NopCloser(bytes.NewReader(content))
		return err
	}
	resp.Body = ioutil.NopCloser(bytes.NewReader(newbody))
	resp.Header["Content-Length"] = []string{fmt.Sprint(len(newbody))}
	return nil
}

func GetUsers(IDs []string, needDesensitize bool) (map[string]apistructs.UserInfo, error) {
	once.Do(func() {
		uc = ucauth.NewUCClient(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
		if conf.OryEnabled() {
			uc = ucauth.NewUCClient(conf.OryKratosPrivateAddr(), conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
			db, err := ucauth.NewDB()
			if err != nil {
				logrus.Errorf("fail to initial db err: %v", err)
				return
			}
			uc.SetDBClient(db)
		}
	})

	b, err := uc.FindUsers(IDs)
	if err != nil {
		return nil, err
	}

	users := make(map[string]apistructs.UserInfo, len(b))
	if needDesensitize {
		for i := range b {
			users[string(b[i].ID)] = apistructs.UserInfo{
				ID:     "",
				Email:  desensitize.Email(b[i].Email),
				Phone:  desensitize.Mobile(b[i].Phone),
				Avatar: b[i].AvatarURL,
				Name:   desensitize.Name(b[i].Name),
				Nick:   desensitize.Name(b[i].Nick),
			}
		}
	} else {
		for i := range b {
			users[string(b[i].ID)] = apistructs.UserInfo{
				ID:     string(b[i].ID),
				Email:  b[i].Email,
				Phone:  b[i].Phone,
				Avatar: b[i].AvatarURL,
				Name:   b[i].Name,
				Nick:   b[i].Nick,
			}
		}
	}
	for _, userID := range IDs {
		_, exist := users[userID]
		if !exist {
			users[userID] = apistructs.UserInfo{
				ID:     userID,
				Email:  "",
				Phone:  "",
				Avatar: "",
				Name:   "用户已注销",
				Nick:   "用户已注销",
			}
		}
	}
	return users, nil
}
