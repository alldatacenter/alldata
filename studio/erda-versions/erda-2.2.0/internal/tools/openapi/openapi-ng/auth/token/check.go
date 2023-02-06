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

package token

import (
	"net/http"
	"strings"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	openapiauth "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
)

func (p *provider) Weight() int64 { return p.Cfg.Weight }

func (p *provider) Match(r *http.Request, opts openapiauth.Options) (bool, interface{}) {
	check, _ := opts.Get("CheckToken").(bool)
	if check {
		if authorization := r.Header.Get("Authorization"); len(authorization) > 0 {
			return true, authorization
		}
	}
	return false, nil
}

func (p *provider) Check(r *http.Request, data interface{}, opts openapiauth.Options) (bool, *http.Request, error) {
	authorization := data.(string)
	client, err := p.checkToken(opts, r, authorization)
	if err != nil {
		return false, r, nil
	}
	r.Header.Set("Client-ID", client.ClientID)
	r.Header.Set("Client-Name", client.ClientName)
	return true, r, nil
}

type clientToken struct {
	ClientID   string
	ClientName string
}

// checkToken try:
// 1. uc token
// 2. openapi oauth2 token
// 3. access key
func (p *provider) checkToken(opts openapiauth.Options, req *http.Request, token string) (clientToken, error) {
	// 1. uc token
	ucToken, err := auth.VerifyUCClientToken(token)
	if err == nil {
		return clientToken{
			ClientID:   ucToken.ClientID,
			ClientName: ucToken.ClientName,
		}, nil
	}
	// 2. openapi oauth2 token
	oauthToken, err := auth.VerifyOpenapiOAuth2Token(p.oauth2server, &OAuth2APISpec{opts}, req)
	if err == nil {
		return clientToken{
			ClientID:   oauthToken.ClientID,
			ClientName: oauthToken.ClientName,
		}, nil
	}
	// 3. access key
	accesskey, err := auth.VerifyAccessKey(p.TokenService, req)
	if err != nil {
		return clientToken{}, err
	}
	return clientToken{
		ClientID:   accesskey.ClientID,
		ClientName: accesskey.ClientName,
	}, nil
}

// OAuth2APISpec .
type OAuth2APISpec struct {
	opts openapiauth.Options
}

func (s *OAuth2APISpec) MatchPath(template string) bool {
	path, ok := s.opts.Get("path").(string)
	if !ok {
		return false
	}
	list1 := strings.Split(path, "/")
	list2 := strings.Split(template, "/")
	if len(list1) != len(list2) {
		return false
	}
	for i, part := range list1 {
		if (strings.HasPrefix(part, "<") && strings.HasSuffix(part, ">")) ||
			(strings.HasPrefix(part, "{") && strings.HasSuffix(part, "}")) {
			// ingore variable name
			continue
		}
		if part != list2[i] {
			return false
		}
	}
	return true
}

func (s *OAuth2APISpec) PathVars(template, path string) map[string]string {
	list1 := strings.Split(path, "/")
	list2 := strings.Split(template, "/")
	if len(list1) != len(list2) {
		return nil
	}
	vars := make(map[string]string)
	for i, part := range list2 {
		if (strings.HasPrefix(part, "<") && strings.HasSuffix(part, ">")) ||
			(strings.HasPrefix(part, "{") && strings.HasSuffix(part, "}")) {
			name := part[1 : len(part)-1]
			vars[name] = list1[i]
		}
	}
	return vars
}

func (s *OAuth2APISpec) Method() string {
	method, _ := s.opts.Get("method").(string)
	return method
}

func (s *OAuth2APISpec) Scheme() string {
	return "http" // only support http now
}
