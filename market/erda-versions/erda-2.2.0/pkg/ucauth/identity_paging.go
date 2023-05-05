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

package ucauth

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

func getIdentityPage(kratosPrivateAddr string, page, perPage int) ([]*OryKratosIdentity, error) {
	var body bytes.Buffer
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Get(kratosPrivateAddr).
		Path("/identities").
		Param("page", fmt.Sprintf("%d", page)).
		Param("per_page", fmt.Sprintf("%d", perPage)).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !r.IsOK() {
		return nil, fmt.Errorf("get identity page: statuscode: %d, body: %v", r.StatusCode(), body.String())
	}

	var i []*OryKratosIdentity
	if err := json.Unmarshal(body.Bytes(), &i); err != nil {
		return nil, err
	}
	return i, nil
}

func getUserList(kratosPrivateAddr string, req *apistructs.UserPagingRequest) ([]User, int, error) {
	if req.PageNo < 1 {
		req.PageNo = 1
	}
	if req.PageSize < 1 {
		req.PageSize = 20
	}
	var identities []*OryKratosIdentity
	cnt := 0
	p := 1
	size := 100
	for {
		ul, err := getIdentityPage(kratosPrivateAddr, p, size)
		if err != nil {
			return nil, 0, err
		}
		if len(ul) == 0 {
			break
		}
		for _, u := range ul {
			if strutil.ContainsOrEmpty(u.Traits.Name, req.Name) && strutil.ContainsOrEmpty(u.Traits.Nick, req.Nick) &&
				strutil.ContainsOrEmpty(u.Traits.Email, req.Email) && strutil.ContainsOrEmpty(u.Traits.Phone, req.Phone) &&
				(req.Locked == nil || req.Locked != nil && u.State == oryKratosStateMap[*req.Locked]) {
				identities = append(identities, u)
				cnt++
			}
		}
		p++
		if p > 100 {
			break
		}
	}

	var users []User
	for _, u := range paginate(identities, req.PageNo, req.PageSize) {
		users = append(users, identityToUser(*u))
	}

	return users, len(identities), nil
}

func paginate(i []*OryKratosIdentity, pageNo int, pageSize int) []*OryKratosIdentity {
	start := (pageNo - 1) * pageSize
	if start > len(i) {
		return nil
	}
	end := start + pageSize
	if end > len(i) {
		return i[start:]
	}
	return i[start:end]
}
