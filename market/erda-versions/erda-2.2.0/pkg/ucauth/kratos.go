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
	"fmt"
	"net/http"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

func whoami(kratosPublicAddr string, sessionID string) (UserInfo, error) {
	var s OryKratosSession
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Get(kratosPublicAddr).
		Cookie(&http.Cookie{
			Name:  "ory_kratos_session",
			Value: sessionID,
		}).
		Path("/sessions/whoami").
		Do().JSON(&s)
	if err != nil {
		return UserInfo{}, err
	}
	if !r.IsOK() {
		return UserInfo{}, fmt.Errorf("bad session")
	}
	return IdentityToUserInfo(s.Identity), nil
}

func getUserByID(kratosPrivateAddr string, userID string) (*User, error) {
	i, err := getIdentity(kratosPrivateAddr, userID)
	if err != nil {
		return nil, err
	}
	u := identityToUser(*i)
	return &u, nil
}

func getUserByIDs(kratosPrivateAddr string, userIDs []string) ([]User, error) {
	var users []User
	for _, id := range userIDs {
		u, err := getUserByID(kratosPrivateAddr, id)
		if err != nil {
			return nil, err
		}
		users = append(users, *u)
	}
	return users, nil
}

func getUserPage(kratosPrivateAddr string, page, perPage int) ([]User, error) {
	i, err := getIdentityPage(kratosPrivateAddr, page, perPage)
	if err != nil {
		return nil, err
	}
	var users []User
	for _, u := range i {
		users = append(users, identityToUser(*u))
	}
	return users, nil
}

func getUserByKey(kratosPrivateAddr string, key string) ([]User, error) {
	p := 1
	size := 100
	cnt := 0
	var users []User
	for {
		ul, err := getUserPage(kratosPrivateAddr, p, size)
		if err != nil {
			return nil, err
		}
		if len(ul) == 0 {
			return users, nil
		}
		for _, u := range ul {
			if u.State == UserActive && (strings.Contains(u.Name, key) || strings.Contains(u.Nick, key) || strings.Contains(u.Email, key)) {
				users = append(users, u)
				cnt++
			}
		}
		p++
		if p > 100 {
			return users, nil
		}
	}
}

func CreateUser(req OryKratosCreateIdentitiyRequest) (string, error) {
	var rsp OryKratosFlowResponse
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Post(conf.OryKratosPrivateAddr()).
		Path("/identities").
		JSONBody(req).
		Do().JSON(&rsp)
	if err != nil {
		return "", err
	}
	if !r.IsOK() {
		return "", errors.Errorf("get kratos user info error, statusCode: %d, err: %s", r.StatusCode(), r.Body())
	}
	return rsp.ID, nil
}
