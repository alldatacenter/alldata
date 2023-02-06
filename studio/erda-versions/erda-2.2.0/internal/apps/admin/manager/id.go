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
	"net/http"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/http/httputil"
)

type USERID string

// Invalid return UserID is valid or invalid
func (uid USERID) Invalid() bool {
	return string(uid) == ""
}

func GetOrgID(req *http.Request) (uint64, error) {
	// get organization id
	orgIDStr, err := GetOrgIDStr(req)
	if err != nil {
		return 0, err
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return 0, errors.Errorf("invalid param, orgID is invalid")
	}
	return orgID, nil
}

func GetOrgIDStr(req *http.Request) (string, error) {
	orgIDStr := req.URL.Query().Get("orgID")
	if orgIDStr == "" {
		orgIDStr = req.Header.Get(httputil.OrgHeader)
		if orgIDStr == "" {
			return "", errors.Errorf("invalid param, orgID is empty")
		}
	}
	return orgIDStr, nil
}
