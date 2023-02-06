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

package nexus

import (
	"bytes"
	"encoding/json"
)

type RealmID string

type Realm struct {
	ID   RealmID `json:"id"`
	Name string  `json:"name"`
}

var (
	LocalAuthenticatingRealm = Realm{ID: "NexusAuthenticatingRealm", Name: "Local Authenticating Realm"}
	LocalAuthorizingRealm    = Realm{ID: "NexusAuthorizingRealm", Name: "Local Authorizing Realm"}
	NpmBearerTokenRealm      = Realm{ID: "NpmToken", Name: "npm Bearer Token Realm"}
	ConanTokenRealm          = Realm{ID: "org.sonatype.repository.conan.internal.security.token.ConanTokenRealm", Name: "Conan Bearer Token Realm"}
	DefaultRoleRealm         = Realm{ID: "DefaultRole", Name: "Default Role Realm"}
	DockerTokenRealm         = Realm{ID: "DockerToken", Name: "Docker Bearer Token Realm"}
	LDAPRealm                = Realm{ID: "LdapRealm", Name: "LDAP Realm"}
	NuGetApiKeyRealm         = Realm{ID: "NuGetApiKey", Name: "NuGet API-Key Realm"}
	RutAuthRealm             = Realm{ID: "rutauth-realm", Name: "Rut Auth Realm"}
)

// List the active realm IDs in order
type RealmListActiveRequest struct{}

// Set the active security realms in the order they should be used
type RealmSetActivesRequest struct {
	ActiveRealms []RealmID
}

// List the available realms
type RealmListAvailableRequest struct{}

type RealmEnsureAddRequest struct {
	Realms []RealmID
}

//////////////////////////////////////////
// http client
//////////////////////////////////////////

func (n *Nexus) ListActiveRealms(req RealmListActiveRequest) ([]RealmID, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/security/realms/active").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var activeRealms []RealmID
	if err := json.NewDecoder(&body).Decode(&activeRealms); err != nil {
		return nil, err
	}

	return activeRealms, nil
}

func (n *Nexus) SetActiveRealms(req RealmSetActivesRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/security/realms/active").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(guaranteeRealms(req.ActiveRealms)).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

// guaranteeRealms 保证在 realms 最前面加上 AuthenticatingRealm 和 AuthorizationRealm
// if realm broken, refer to these articles to resume:
// - https://support.sonatype.com/n.hc/en-us/articles/213467158-How-to-reset-a-forgotten-admin-password-in-Nexus-3-x
// - https://support.sonatype.com/n.hc/en-us/articles/115002930827-Accessing-the-OrientDB-Console
func guaranteeRealms(activeRealms []RealmID) []RealmID {
	guaranteed := []RealmID{LocalAuthenticatingRealm.ID, LocalAuthorizingRealm.ID}
	for _, realm := range activeRealms {
		if realm == LocalAuthenticatingRealm.ID || realm == LocalAuthorizingRealm.ID {
			continue
		}
		guaranteed = append(guaranteed, realm)
	}
	return guaranteed
}

func (n *Nexus) ListAvailableRealms(req RealmListAvailableRequest) ([]Realm, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/security/realms/available").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var availableRealms []Realm
	if err := json.NewDecoder(&body).Decode(&availableRealms); err != nil {
		return nil, err
	}

	return availableRealms, nil
}

// EnsureAddRealms 保证在原有 realm 基础上增加请求里的 realm
func (n *Nexus) EnsureAddRealms(req RealmEnsureAddRequest) error {
	activeRealms, err := n.ListActiveRealms(RealmListActiveRequest{})
	if err != nil {
		return err
	}

	// allRealmsMap 保存所有 realm
	allRealmsMap := make(map[RealmID]struct{}, len(activeRealms))

	// 当前已存在的 realms 添加至 allRealmsMap
	for _, r := range activeRealms {
		allRealmsMap[r] = struct{}{}
	}

	// 请求里的 realms 添加至 allRealmMap
	for _, reqRealm := range req.Realms {
		allRealmsMap[reqRealm] = struct{}{}
	}

	// 所有 realm 都已存在，直接返回
	if len(allRealmsMap) == len(activeRealms) {
		return nil
	}

	// 重新设置 realm
	var allRealms []RealmID
	for r := range allRealmsMap {
		allRealms = append(allRealms, r)
	}
	return n.SetActiveRealms(RealmSetActivesRequest{ActiveRealms: allRealms})
}
