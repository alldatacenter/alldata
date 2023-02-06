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

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) GetOAuth2Token(req apistructs.OAuth2TokenGetRequest) (*apistructs.OAuth2Token, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var body bytes.Buffer
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/oauth2/token")).
		Header(httputil.InternalHeader, "bundle").
		Param("grant_type", "client_credentials").
		Param("client_id", req.ClientID).
		Param("client_secret", req.ClientSecret).
		JSONBody(&req.Payload).
		Do().Body(&body)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.New(body.String()))
	}
	var token apistructs.OAuth2Token
	err = json.NewDecoder(&body).Decode(&token)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	return &token, nil
}

func (b *Bundle) InvalidateOAuth2Token(req apistructs.OAuth2TokenInvalidateRequest) (*apistructs.OAuth2Token, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var body bytes.Buffer
	httpResp, err := hc.Post(host).Path(fmt.Sprintf("/oauth2/invalidate_token")).
		Header(httputil.InternalHeader, "bundle").
		Param("access_token", req.AccessToken).
		Do().Body(&body)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.New(body.String()))
	}
	var token apistructs.OAuth2Token
	err = json.NewDecoder(&body).Decode(&token)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	return &token, nil
}
