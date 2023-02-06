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

package edgepipeline_register

import (
	"context"
	"fmt"
	"net/http"

	"github.com/erda-project/erda/apistructs"
)

func (p *provider) GetAccessToken(req apistructs.OAuth2TokenGetRequest) (*apistructs.OAuth2Token, error) {
	return &apistructs.OAuth2Token{
		AccessToken: p.ClusterAccessKey(),
		ExpiresIn:   0,
		TokenType:   "Bearer",
	}, nil
}

func (p *provider) GetOAuth2Token(req apistructs.OAuth2TokenGetRequest) (*apistructs.OAuth2Token, error) {
	return &apistructs.OAuth2Token{
		AccessToken: p.ClusterAccessKey(),
		ExpiresIn:   0,
		TokenType:   "Bearer",
	}, nil
}

func (p *provider) CheckAccessTokenFromHttpRequest(req *http.Request) error {
	if p.Cfg.IsEdge {
		token := req.Header.Get("Authorization")
		return p.CheckAccessToken(token)
	}
	return nil
}

func (p *provider) CheckAccessToken(token string) error {
	accessEtcdKey := p.makeEtcdKeyOfClusterAccessKey(token)
	_, err := p.EtcdClient.Get(context.Background(), accessEtcdKey)
	if err != nil {
		p.Log.Errorf("failed to get access token %s, err: %v", token, err)
		return fmt.Errorf("failed to get access token %s", token)
	}
	return nil
}
