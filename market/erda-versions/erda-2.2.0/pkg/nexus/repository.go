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
	"fmt"
)

type Repository struct {
	Name            string                         `json:"name"`
	Format          RepositoryFormat               `json:"format"`
	URL             string                         `json:"url"`
	Online          bool                           `json:"online"`
	Storage         HostedRepositoryStorageConfig  `json:"storage"`
	Group           *RepositoryGroupConfig         `json:"group"`
	Cleanup         *RepositoryCleanupConfig       `json:"cleanup"`
	Proxy           *RepositoryProxyConfig         `json:"proxy"`
	NegativeCache   *RepositoryNegativeCacheConfig `json:"negativeCache"`
	HttpClient      *RepositoryHttpClientConfig    `json:"httpClient"`
	RoutingRuleName *string                        `json:"routingRuleName"`
	Maven           *RepositoryMavenConfig         `json:"maven"`
	Type            RepositoryType                 `json:"type"`
	Extra           RepositoryExtra                `json:"extra"`
}

type RepositoryExtra struct {
	// ReverseProxyURL docker hosted or group repo will have
	ReverseProxyURL string `json:"reverseProxyURL,omitempty"`
}

type (
	RepositoryFormat             string
	RepositoryType               string
	RepositoryMavenVersionPolicy string
	RepositoryMavenLayoutPolicy  string
	RepositoryStorageWritePolicy string
	RepositoryAuthenticationType string
)

var (
	RepositoryFormatMaven  RepositoryFormat = "maven"
	RepositoryFormatNpm    RepositoryFormat = "npm"
	RepositoryFormatDocker RepositoryFormat = "docker"
	RepositoryFormatAny    RepositoryFormat = "*"

	RepositoryTypeHosted RepositoryType = "hosted"
	RepositoryTypeGroup  RepositoryType = "group"
	RepositoryTypeProxy  RepositoryType = "proxy"

	RepositoryMavenVersionPolicyRelease  RepositoryMavenVersionPolicy = "RELEASE"
	RepositoryMavenVersionPolicySnapshot RepositoryMavenVersionPolicy = "SNAPSHOT"
	RepositoryMavenVersionPolicyMixed    RepositoryMavenVersionPolicy = "MIXED"

	RepositoryMavenLayoutPolicyStrict     RepositoryMavenLayoutPolicy = "STRICT"
	RepositoryMavenLayoutPolicyPermissive RepositoryMavenLayoutPolicy = "PERMISSIVE"

	RepositoryStorageWritePolicyAllowRedploy    RepositoryStorageWritePolicy = "ALLOW"
	RepositoryStorageWritePolicyDisableRedeploy RepositoryStorageWritePolicy = "ALLOW_ONCE"
	RepositoryStorageWritePolicyReadOnly        RepositoryStorageWritePolicy = "DENY"

	RepositoryAuthenticationTypeUsername RepositoryAuthenticationType = "username"
	RepositoryAuthenticationTypeNTLM     RepositoryAuthenticationType = "ntlm" // windows ntlm
)

type RepositoryProxyConfig struct {
	// Location of the remote repository being proxied
	// example: https://registry.npmjs.org
	RemoteURL string `json:"remoteUrl"`
	// How long to cache artifacts before rechecking the remote repository (in minutes)
	// Release repositories should use -1.
	// example: 1440
	ContentMaxAge int64 `json:"contentMaxAge"`
	// How long to cache metadata before rechecking the remote repository (in minutes)
	// example: 1440
	MetadataMaxAge int64 `json:"metadataMaxAge"`
}

type RepositoryGroupConfig struct {
	// Member repositories' names
	// example: maven-public
	MemberNames []string `json:"memberNames"`
}

type RepositoryMavenConfig struct {
	// What type of artifacts does this repository store?
	// example: mixed
	VersionPolicy RepositoryMavenVersionPolicy `json:"versionPolicy"`
	// Validate that all paths are maven artifact or metadata paths
	// example: strict
	LayoutPolicy RepositoryMavenLayoutPolicy `json:"layoutPolicy"`
}

type RepositoryDockerConfig struct {
	// Whether to allow clients to use the V1 API to interact with this repository
	// example: false
	V1Enabled bool `json:"v1Enabled"`
	// Whether to force authentication (Docker Bearer Token Realm required if false)
	// example: true
	ForceBasicAuth bool `json:"forceBasicAuth"`
	// Create an HTTP connector at specified port
	// +Optional
	// example: 8082
	HttpPort *int `json:"httpPort"`
	// Create an HTTPS connector at specified port
	// +Optional
	// example: 8083
	HttpsPort *int `json:"httpsPort"`
}

type RepositoryDockerProxyConfig struct {
	IndexType string `json:"indexType"`
	IndexUrl  string `json:"indexUrl"`
}
type RepositoryNegativeCacheConfig struct {
	// Whether to cache responses for content not present in the proxied repository
	// example: false
	NotFoundCacheEnabled bool `json:"enabled"`
	// How long to cache the fact that a file was not found in the repository (in minutes)
	// example: 1440
	NotFoundCacheTTL int64 `json:"timeToLive"`
}

type RepositoryHttpClientConfig struct {
	// Whether to block outbound connections on the repository
	Blocked bool `json:"blocked"`
	// Whether to auto-block outbound connections if remote peer is detected as unreachable/unresponsive
	AutoBlock  bool                            `json:"autoBlock"`
	Connection *RepositoryHttpClientConnection `json:"connection"`
	// TODO not take effect yet
	Authentication *RepositoryHttpClientAuthentication `json:"authentication"`
}

type RepositoryHttpClientConnection struct {
	// Total retries if the initial connection attempt suffers a timeout
	// example: 0
	// minimum: 0
	// maximum: 10
	Retries *int64 `json:"retries"`
	// Custom fragment to append to User-Agent header in HTTP requests
	UserAgentSuffix *string `json:"userAgentSuffix"`
	// Seconds to wait for activity before stopping and retrying the connection
	// example: 60
	// minimum: 1
	// maximum: 3600
	Timeout *int64 `json:"timeout"`
	// Whether to enable redirects to the same location (may be required by some servers)
	EnableCircularRedirects bool `json:"enableCircularRedirects"`
	// Whether to allow cookies to be stored and used
	EnableCookies bool `json:"enableCookies"`
}

type RepositoryHttpClientAuthentication struct {
	// Authentication type
	Type     RepositoryAuthenticationType `json:"type"`
	Username string                       `json:"username"`
	Password string                       `json:"password"`
}

// UseNetdata means if blob store in netdata.
type BlobUseNetdata struct {
	UseNetdata bool `json:"useNetdata,omitempty"`
}

type HostedRepositoryStorageConfig struct {
	// Blob store used to store repository contents
	// example: default
	BlobStoreName string `json:"blobStoreName"`
	// Whether to validate uploaded content's MIME type appropriate for the repository format
	// example: true
	StrictContentTypeValidation bool `json:"strictContentTypeValidation"`
	// Controls if deployments of and updates to assets are allowed
	// example: allow_once
	WritePolicy RepositoryStorageWritePolicy `json:"writePolicy"`

	BlobUseNetdata
}

type RepositoryCleanupConfig struct {
	// Components that match any of the applied policies will be deleted
	// example: weekly-cleanup
	PolicyNames []string `json:"policyNames"`
}

// Standard 返回 format 标准值
// maven -> maven2
func (format RepositoryFormat) Standard() string {
	switch format {
	case RepositoryFormatMaven:
		return "maven2"
	}
	return string(format)
}

//////////////////////////////////////////
// http client
//////////////////////////////////////////

func (n *Nexus) ListRepositories(req RepositoryListRequest) ([]Repository, error) {
	var body bytes.Buffer
	httpResp, err := n.hc.Get(n.Addr).Path("/service/rest/beta/repositories").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !httpResp.IsOK() {
		return nil, ErrNotOK(httpResp.StatusCode(), body.String())
	}

	var repos []Repository
	if err := json.NewDecoder(&body).Decode(&repos); err != nil {
		return nil, err
	}

	return repos, nil
}

func (n *Nexus) GetRepository(req RepositoryGetRequest) (*Repository, error) {
	repos, err := n.ListRepositories(RepositoryListRequest{})
	if err != nil {
		return nil, err
	}
	for i := range repos {
		if repos[i].Name == req.RepositoryName {
			return &repos[i], nil
		}
	}
	return nil, ErrNotFound
}

func (n *Nexus) DeleteRepository(req RepositoryDeleteRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Delete(n.Addr).Path("/service/rest/beta/repositories/"+req.RepositoryName).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) InvalidateRepositoryCache(req RepositoryInvalidateCacheRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/"+req.RepositoryName+"/invalidate-cache").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) RebuildRepositoryIndex(req RepositoryRebuildIndexRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/"+req.RepositoryName+"/rebuild-index").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) CreateMavenHostedRepository(req MavenHostedRepositoryCreateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/maven/hosted").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateMavenHostedRepository(req MavenHostedRepositoryUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/maven/hosted/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) CreateMavenProxyRepository(req MavenProxyRepositoryCreateRequest) error {
	if _, err := json.MarshalIndent(req, "", "  "); err != nil {
		return err
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/maven/proxy").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateMavenProxyRepository(req MavenProxyRepositoryUpdateRequest) error {
	if _, err := json.MarshalIndent(req, "", "  "); err != nil {
		return err
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/maven/proxy/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

// TODO  3.22 not provide maven group repository create rest api
func (n *Nexus) CreateMavenGroupRepository(req MavenGroupRepositoryCreateRequest) error {
	if _, err := json.MarshalIndent(req, "", "  "); err != nil {
		return err
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/maven/group").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) CreateNpmGroupRepository(req NpmGroupRepositoryCreateRequest) error {
	if _, err := json.MarshalIndent(req, "", "  "); err != nil {
		return err
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/npm/group").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateNpmGroupRepository(req NpmGroupRepositoryUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/npm/group/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) EnsureNpmHostedRepository(req NpmHostedRepositoryCreateRequest) error {
	_, err := n.GetRepository(RepositoryGetRequest{RepositoryName: req.Name})
	if err != nil {
		if err != ErrNotFound {
			return err
		}
		// create
		return n.CreateNpmHostedRepository(req)
	}
	// update
	return n.UpdateNpmHostedRepository(NpmHostedRepositoryUpdateRequest(req))
}

func (n *Nexus) CreateNpmHostedRepository(req NpmHostedRepositoryCreateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/npm/hosted").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateNpmHostedRepository(req NpmHostedRepositoryUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/npm/hosted/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

// TODO  3.22 not provide maven group repository create rest api
func (n *Nexus) CreateNpmProxyRepository(req NpmProxyRepositoryCreateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/npm/proxy").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateNpmProxyRepository(req NpmProxyRepositoryUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/npm/proxy/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

// EnsureRepository make sure repository exist, create or update according to request
func (n *Nexus) EnsureRepository(req RepositoryCreator) error {
	// realm
	var ensureRealm RealmID
	switch req.GetFormat() {
	case RepositoryFormatNpm:
		ensureRealm = NpmBearerTokenRealm.ID
	}
	if ensureRealm != "" {
		if err := n.EnsureAddRealms(RealmEnsureAddRequest{Realms: []RealmID{ensureRealm}}); err != nil {
			return err
		}
	}

	// blob store
	err := n.EnsureFileBlobStore(FileBlobStoreCreateRequest{
		SoftQuota:      nil,
		Path:           req.GetName(),
		Name:           req.GetName(),
		BlobUseNetdata: BlobUseNetdata{UseNetdata: req.GetStorage().UseNetdata},
	})
	if err != nil {
		return err
	}

	// repo
	_, err = n.GetRepository(RepositoryGetRequest{RepositoryName: req.GetName()})
	if err != nil {
		if err != ErrNotFound {
			return err
		}
		// not found, create
		return n.CreateRepository(req)
	}
	// update
	return n.UpdateRepository(req)
}

func (n *Nexus) CreateRepository(req RepositoryCreator) error {
	if req.GetFormat() == "" {
		return ErrMissingRepoFormat
	}
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path(fmt.Sprintf("/service/rest/beta/repositories/%s/%s",
		req.GetFormat(), req.GetType())).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}
	return nil
}

func (n *Nexus) UpdateRepository(req RepositoryCreator) error {
	if req.GetFormat() == "" {
		return ErrMissingRepoFormat
	}
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path(fmt.Sprintf("/service/rest/beta/repositories/%s/%s/%s",
		req.GetFormat(), req.GetType(), req.GetName())).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}
	return nil
}

func (n *Nexus) CreateDockerHostedRepository(req DockerHostedRepositoryCreateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/docker/hosted").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateDockerHostedRepository(req DockerHostedRepositoryUpdateRequest) error {
	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/docker/hosted/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) CreateDockerProxyRepository(req DockerProxyRepositoryCreateRequest) error {
	if _, err := json.MarshalIndent(req, "", "  "); err != nil {
		return err
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Post(n.Addr).Path("/service/rest/beta/repositories/docker/proxy").
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}

func (n *Nexus) UpdateDockerProxyRepository(req DockerProxyRepositoryUpdateRequest) error {
	if _, err := json.MarshalIndent(req, "", "  "); err != nil {
		return err
	}

	var body bytes.Buffer
	httpResp, err := n.hc.Put(n.Addr).Path("/service/rest/beta/repositories/docker/proxy/"+req.Name).
		Header(HeaderAuthorization, n.basicAuthBase64Value()).
		JSONBody(req).
		Do().Body(&body)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return ErrNotOK(httpResp.StatusCode(), body.String())
	}

	return nil
}
