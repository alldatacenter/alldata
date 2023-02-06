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

package auth

import (
	"context"
	"fmt"
	"net/http"
	"strings"
	"sync"

	"github.com/sirupsen/logrus"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/oauth2"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

const (
	CtxKeyOauth2JwtKeyPayload = "oauth2-jwt-token-payload"
)

var ucTokenAuth *ucauth.UCTokenAuth
var once sync.Once

// 获取 dice 自己的token
func GetDiceClientToken() (ucauth.OAuthToken, error) {
	// TODO kratos will not use it
	if conf.OryEnabled() {
		return ucauth.OAuthToken{
			AccessToken: conf.OryKratosPrivateAddr(),
			TokenType:   conf.OryCompatibleClientID(),
		}, nil
	}
	once.Do(func() {
		ucTokenAuth, _ = ucauth.NewUCTokenAuth(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
	})
	otoken, err := ucTokenAuth.GetServerToken(false)
	if err != nil {
		logrus.Error(err)
		return ucauth.OAuthToken{}, err
	}
	return otoken, nil
}

// @return example:
// {"id":7,"userId":null,"clientId":"dice-test","clientName":"dice测试应用","clientLogoUrl":null,"clientSecret":null,"autoApprove":false,"scope":["public_profile","email"],"resourceIds":["shinda-maru"],"authorizedGrantTypes":["client_credentials"],"registeredRedirectUris":[],"autoApproveScopes":[],"authorities":["ROLE_CLIENT"],"accessTokenValiditySeconds":433200,"refreshTokenValiditySeconds":433200,"additionalInformation":{}}
func VerifyUCClientToken(token string) (ucauth.TokenClient, error) {
	once.Do(func() {
		ucTokenAuth, _ = ucauth.NewUCTokenAuth(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
	})
	return ucTokenAuth.Auth(token)
}

func NewUCTokenClient(req *ucauth.NewClientRequest) (*ucauth.NewClientResponse, error) {
	once.Do(func() {
		ucTokenAuth, _ = ucauth.NewUCTokenAuth(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
	})
	return ucTokenAuth.NewClient(req)
}

// OAuth2APISpec .
type OAuth2APISpec interface {
	MatchPath(path string) bool
	PathVars(temp, path string) map[string]string
	Method() string
	Scheme() string
}

// OpenapiSpec .
type OpenapiSpec struct {
	*spec.Spec
}

func (s *OpenapiSpec) MatchPath(path string) bool {
	return s.Spec.Path.String() == path
}

func (s *OpenapiSpec) Method() string {
	return s.Spec.Method
}

func (s *OpenapiSpec) Scheme() string {
	return s.Spec.Scheme.String()
}

func (s *OpenapiSpec) PathVars(template, path string) map[string]string {
	return s.Spec.Path.Vars(path)
}

func VerifyOpenapiOAuth2Token(o *oauth2.OAuth2Server, spec OAuth2APISpec, r *http.Request) (TokenClient, error) {
	// add Bearer prefix
	tokenHeader := r.Header.Get(HeaderAuthorization)
	if !strings.HasPrefix(tokenHeader, HeaderAuthorizationBearerPrefix) {
		r.Header.Set(HeaderAuthorization, HeaderAuthorizationBearerPrefix+tokenHeader)
	}

	ti, err := o.Server().ValidationBearerToken(r)
	if err != nil {
		return TokenClient{}, err
	}
	claims, err := oauth2.ParseJWTAccess(ti.GetAccess())
	if err != nil {
		return TokenClient{}, err
	}
	// set jwt token payload
	*r = *(r.WithContext(context.WithValue(r.Context(), CtxKeyOauth2JwtKeyPayload, claims.Payload)))

	if !claims.Payload.AllowAccessAllAPIs {
		// validate accessible api list
		foundAccessibleAPI := false
		var path string
		for _, accessibleAPI := range claims.Payload.AccessibleAPIs {
			if matchAPISpec(accessibleAPI, spec) {
				foundAccessibleAPI = true
				path = accessibleAPI.Path
				break
			}
		}
		if !foundAccessibleAPI {
			return TokenClient{}, fmt.Errorf("this token is not permitted to access specific api, method: %s, path: %s", r.Method, r.URL)
		}

		// validate wildcards in metadata
		// wildcards: pipelineID=1
		// metadata:  pipelineID=2
		// validate failed
		wildcards := spec.PathVars(path, r.URL.Path)
		invalidWildcardNames := []string{}
		for k, v := range wildcards {
			mv, ok := claims.Payload.Metadata[k]
			if ok && v != mv {
				invalidWildcardNames = append(invalidWildcardNames, k)
			}
		}
		if len(invalidWildcardNames) > 0 {
			return TokenClient{}, fmt.Errorf("this token is not permitted to access specific api, method: %s, path: %s, invalid path vars: %s",
				r.Method, r.URL, strutil.Join(invalidWildcardNames, ", "))
		}

		// inject internal header in metadata
		for k, v := range claims.Payload.Metadata {
			if k == httputil.UserHeader && v != "" {
				r.Header.Set(httputil.UserHeader, v)
				continue
			}
			if k == httputil.InternalHeader && v != "" {
				r.Header.Set(httputil.InternalHeader, v)
				continue
			}
			if k == httputil.OrgHeader && v != "" {
				r.Header.Set(httputil.OrgHeader, v)
				continue
			}
		}
	}

	// inject metadata into header
	for k, v := range claims.Payload.Metadata {
		r.Header.Set(k, v)
	}

	return TokenClient{
		ClientID: ti.GetClientID(),
	}, nil
}

func matchAPISpec(accessibleAPI apistructs.AccessibleAPI, spec OAuth2APISpec) bool {
	return spec.MatchPath(accessibleAPI.Path) &&
		accessibleAPI.Method == spec.Method() &&
		strutil.Equal(accessibleAPI.Schema, spec.Scheme(), true)
}

func VerifyAccessKey(tokenService tokenpb.TokenServiceServer, r *http.Request) (TokenClient, error) {
	auth := r.Header.Get(HeaderAuthorization)
	token := ""
	if auth != "" && strings.HasPrefix(auth, HeaderAuthorizationBearerPrefix) {
		token = auth[len(HeaderAuthorizationBearerPrefix):]
	}
	resp, err := tokenService.QueryTokens(r.Context(), &tokenpb.QueryTokensRequest{
		Access: token,
		Scope:  strings.ToLower(tokenpb.ScopeEnum_CMP_CLUSTER.String()),
		Type:   mysqltokenstore.AccessKey.String(),
	})
	if err != nil || resp == nil {
		return TokenClient{}, err
	}
	if resp.Total == 0 {
		return TokenClient{}, fmt.Errorf("auth failed, access key: %s", token)
	} else if resp.Total > 1 {
		return TokenClient{}, fmt.Errorf("auth failed, duplidate data")
	}
	scopeId := resp.Data[0].ScopeId
	if resp.Data[0].ScopeId != "" {
		r.Header.Set(httputil.InternalHeader, scopeId)
	} else {
		r.Header.Set(httputil.InternalHeader, tokenpb.ScopeEnum_CMP_CLUSTER.String())
	}
	return TokenClient{
		ClientID:   scopeId,
		ClientName: scopeId,
	}, nil
}
