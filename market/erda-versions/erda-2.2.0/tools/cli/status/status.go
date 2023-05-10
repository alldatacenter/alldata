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

package status

import (
	"encoding/json"
	"os"
	"path/filepath"
	"time"

	"github.com/erda-project/erda/tools/cli/utils"
)

const (
	sessionFile = "sessions"
)

// {
// 	"terminus-org.app.terminus.io": {
// 		"sessionid": "",
// 		"id": "0001",
// 		"nickName": "username",
// 	},
// 	"terminus-org.test.terminus.io": {
// 		"sessionid": "",
// 		"id": "0002",
// 		"nickName": "username",
// 	}
// }
var SessionInfos = map[string]StatusInfo{}

type StatusInfo struct {
	SessionID string     `json:"sessionid"`
	ExpiredAt *time.Time `json:"expiredAt"`
	UserInfo
}

type UserInfo struct {
	ID          string `json:"id"`
	Email       string `json:"email"`
	NickName    string `json:"nickName"`
	Enabled     bool   `json:"enabled"`
	CreatedAt   string `json:"createdAt"`
	UpdatedAt   string `json:"updatedAt"`
	LastLoginAt string `json:"lastLoginAt"`
}

// GetSessionInfos fetch sessions
func GetSessionInfos() (map[string]StatusInfo, error) {
	// check directory ~/.erda.d if exist
	diceDir, err := utils.FindGlobalErdaDir()
	if err != nil {
		return nil, err
	}

	// load file ~/.erda.d/sessions
	if _, err := os.Stat(filepath.Join(diceDir, sessionFile)); err != nil {
		if os.IsNotExist(err) {
			return nil, utils.NotExist
		}
		return nil, err
	}
	f, err := os.Open(filepath.Join(diceDir, sessionFile))
	if err != nil {
		return nil, err
	}
	defer f.Close()

	if err := json.NewDecoder(f).Decode(&SessionInfos); err != nil {
		return nil, err
	}

	return SessionInfos, nil
}

// StoreSessionInfo write session info to file ~/.erda.d/sessions
func StoreSessionInfo(host string, stat StatusInfo) error {
	erdaDir, err := utils.FindGlobalErdaDir()
	if err != nil {
		if err != utils.NotExist {
			return err
		}
		erdaDir, err = utils.CreateGlobalErdaDir()
		if err != nil {
			return err
		}
	}

	sessions := make(map[string]StatusInfo)
	sessionPath := filepath.Join(erdaDir, sessionFile)
	if _, err := os.Stat(sessionPath); err == nil {
		sessions, err = GetSessionInfos()
		if err != nil {
			return err
		}
	}
	f, err := os.OpenFile(sessionPath, os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0666)
	if err != nil {
		return err
	}
	defer f.Close()

	// add new session
	sessions[host] = stat

	content, err := json.MarshalIndent(sessions, "", "  ")
	if err != nil {
		return err
	}
	f.Write(content)

	return nil
}
