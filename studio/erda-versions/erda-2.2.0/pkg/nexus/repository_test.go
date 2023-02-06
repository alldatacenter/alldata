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

//import (
//	"testing"
//
//	"github.com/stretchr/testify/assert"
//)
//
//func TestNexus_ListRepositories(t *testing.T) {
//	repos, err := n.ListRepositories(RepositoryListRequest{})
//	assert.NoError(t, err)
//	printJSON(repos)
//}
//
//func TestNexus_GetRepository(t *testing.T) {
//	repo, err := n.GetRepository(RepositoryGetRequest{RepositoryName: "maven-central"})
//	assert.NoError(t, err)
//	printJSON(repo)
//
//	_, err = n.GetRepository(RepositoryGetRequest{RepositoryName: "not-exist"})
//	assert.Equal(t, ErrNotFound, err)
//}
//
//func TestNexus_DeleteRepository(t *testing.T) {
//	err := n.DeleteRepository(RepositoryDeleteRequest{
//		RepositoryName: "a",
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_InvalidateRepositoryCache(t *testing.T) {
//	err := n.InvalidateRepositoryCache(RepositoryInvalidateCacheRequest{
//		RepositoryName: "npm-taobao",
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_RebuildRepositoryIndex(t *testing.T) {
//	err := n.RebuildRepositoryIndex(RepositoryRebuildIndexRequest{
//		RepositoryName: "npm-taobao",
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateMavenHostedRepository(t *testing.T) {
//	name := "sdk-maven-hosted-1"
//
//	err := n.CreateMavenHostedRepository(MavenHostedRepositoryCreateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: false,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//					WritePolicy:                 RepositoryStorageWritePolicyAllowRedploy,
//				},
//				Cleanup: nil,
//			},
//		},
//		Maven: RepositoryMavenConfig{
//			VersionPolicy: RepositoryMavenVersionPolicySnapshot,
//			LayoutPolicy:  RepositoryMavenLayoutPolicyPermissive,
//		},
//	},
//	)
//	assert.NoError(t, err)
//}
//
//func TestNexus_UpdateMavenHostedRepository(t *testing.T) {
//	name := "sdk-maven-hosted-1"
//
//	err := n.UpdateMavenHostedRepository(MavenHostedRepositoryUpdateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: true,
//					WritePolicy:                 RepositoryStorageWritePolicyAllowRedploy,
//				},
//				Cleanup: nil,
//			},
//		},
//		Maven: RepositoryMavenConfig{
//			VersionPolicy: RepositoryMavenVersionPolicySnapshot,
//			LayoutPolicy:  RepositoryMavenLayoutPolicyStrict,
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateMavenProxyRepository(t *testing.T) {
//	name := "sdk-maven-proxy-6"
//
//	err := n.CreateMavenProxyRepository(MavenProxyRepositoryCreateRequest{
//		ProxyRepositoryCreateRequest: ProxyRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//					WritePolicy:                 RepositoryStorageWritePolicyReadOnly,
//				},
//				Cleanup: nil,
//			},
//			Proxy: RepositoryProxyConfig{
//				RemoteURL:      "http://maven.aliyun.com/nexus/content/groups/public/",
//				ContentMaxAge:  -1,
//				MetadataMaxAge: 1440,
//			},
//			NegativeCache: RepositoryNegativeCacheConfig{
//				NotFoundCacheEnabled: true,
//				NotFoundCacheTTL:     60,
//			},
//			HttpClient: RepositoryHttpClientConfig{
//				Blocked:   false,
//				AutoBlock: false,
//				Connection: &RepositoryHttpClientConnection{
//					Retries:                 &[]int64{9}[0],
//					UserAgentSuffix:         &[]string{"123"}[0],
//					Timeout:                 &[]int64{100}[0],
//					EnableCircularRedirects: true,
//					EnableCookies:           false,
//				},
//				Authentication: &RepositoryHttpClientAuthentication{
//					Type:     RepositoryAuthenticationTypeUsername,
//					Username: "admin",
//					Password: "admin123",
//				},
//			},
//		},
//		Maven: RepositoryMavenConfig{
//			VersionPolicy: RepositoryMavenVersionPolicyMixed,
//			LayoutPolicy:  RepositoryMavenLayoutPolicyPermissive,
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_UpdateMavenProxyRepository(t *testing.T) {
//	name := "sdk-maven-proxy-3"
//
//	err := n.UpdateMavenProxyRepository(MavenProxyRepositoryUpdateRequest(MavenProxyRepositoryCreateRequest{
//		ProxyRepositoryCreateRequest: ProxyRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//					WritePolicy:                 RepositoryStorageWritePolicyReadOnly,
//				},
//				Cleanup: nil,
//			},
//			Proxy: RepositoryProxyConfig{
//				RemoteURL:      "http://maven.aliyun.com/nexus/content/groups/public/",
//				ContentMaxAge:  -1,
//				MetadataMaxAge: 1440,
//			},
//			NegativeCache: RepositoryNegativeCacheConfig{
//				NotFoundCacheEnabled: true,
//				NotFoundCacheTTL:     60,
//			},
//			HttpClient: RepositoryHttpClientConfig{
//				Blocked:   false,
//				AutoBlock: false,
//				Connection: &RepositoryHttpClientConnection{
//					Retries:                 &[]int64{9}[0],
//					UserAgentSuffix:         &[]string{"123"}[0],
//					Timeout:                 &[]int64{100}[0],
//					EnableCircularRedirects: true,
//					EnableCookies:           false,
//				},
//				Authentication: &RepositoryHttpClientAuthentication{
//					Type:     RepositoryAuthenticationTypeUsername,
//					Username: "admin",
//					Password: "admin123",
//				},
//			},
//			RoutingRule: "",
//		},
//		Maven: RepositoryMavenConfig{
//			VersionPolicy: RepositoryMavenVersionPolicyMixed,
//			LayoutPolicy:  RepositoryMavenLayoutPolicyPermissive,
//		},
//	}))
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateMavenGroupRepository(t *testing.T) {
//	name := "sdk-maven-group-1"
//
//	err := n.CreateMavenGroupRepository(MavenGroupRepositoryCreateRequest{
//		GroupRepositoryCreateRequest: GroupRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//				},
//			},
//			Group: RepositoryGroupConfig{
//				MemberNames: []string{"maven-central"},
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateNpmGroupRepository(t *testing.T) {
//	name := "sdk-npm-group-1"
//
//	err := n.CreateNpmGroupRepository(NpmGroupRepositoryCreateRequest{
//		GroupRepositoryCreateRequest: GroupRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//				},
//			},
//			Group: RepositoryGroupConfig{
//				MemberNames: []string{"terminus-npm-registry", "terminus-cnpm", "npm-taobao"},
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_UpdateNpmGroupRepository(t *testing.T) {
//	name := "sdk-npm-group-1"
//
//	err := n.UpdateNpmGroupRepository(NpmGroupRepositoryUpdateRequest{
//		GroupRepositoryCreateRequest: GroupRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//				},
//			},
//			Group: RepositoryGroupConfig{
//				MemberNames: []string{"terminus-cnpm", "terminus-npm-registry"},
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_EnsureNpmHostedRepository(t *testing.T) {
//	name := "sdk-npm-hosted-1"
//
//	err := n.EnsureNpmHostedRepository(NpmHostedRepositoryCreateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: true,
//					WritePolicy:                 RepositoryStorageWritePolicyReadOnly,
//				},
//				Cleanup: nil,
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateNpmHostedRepository(t *testing.T) {
//	name := "sdk-npm-hosted-100"
//
//	err := n.CreateNpmHostedRepository(NpmHostedRepositoryCreateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//					WritePolicy:                 RepositoryStorageWritePolicyDisableRedeploy,
//				},
//				Cleanup: nil,
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_UpdateNpmHostedRepository(t *testing.T) {
//	name := "sdk-npm-hosted-1"
//
//	err := n.UpdateNpmHostedRepository(NpmHostedRepositoryUpdateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: true,
//					WritePolicy:                 RepositoryStorageWritePolicyAllowRedploy,
//				},
//				Cleanup: nil,
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateNpmProxyRepository(t *testing.T) {
//	name := "sdk-npm-proxy-1"
//
//	err := n.CreateNpmProxyRepository(NpmProxyRepositoryCreateRequest{
//		ProxyRepositoryCreateRequest: ProxyRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "default",
//					StrictContentTypeValidation: false,
//					WritePolicy:                 RepositoryStorageWritePolicyReadOnly,
//				},
//				Cleanup: nil,
//			},
//			Proxy: RepositoryProxyConfig{
//				RemoteURL:      "https://registry.npm.terminus.io/",
//				ContentMaxAge:  -1,
//				MetadataMaxAge: 1440,
//			},
//			NegativeCache: RepositoryNegativeCacheConfig{
//				NotFoundCacheEnabled: true,
//				NotFoundCacheTTL:     60,
//			},
//			HttpClient: RepositoryHttpClientConfig{
//				Blocked:   false,
//				AutoBlock: false,
//				Connection: &RepositoryHttpClientConnection{
//					Retries:                 &[]int64{9}[0],
//					UserAgentSuffix:         &[]string{"123"}[0],
//					Timeout:                 &[]int64{100}[0],
//					EnableCircularRedirects: true,
//					EnableCookies:           false,
//				},
//				Authentication: &RepositoryHttpClientAuthentication{
//					Type:     RepositoryAuthenticationTypeUsername,
//					Username: "admin",
//					Password: "admin123",
//				},
//			},
//			RoutingRule: "",
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_UpdateNpmProxyRepository(t *testing.T) {
//	name := "sdk-npm-proxy-1"
//
//	err := n.UpdateNpmProxyRepository(NpmProxyRepositoryUpdateRequest(
//		NpmProxyRepositoryCreateRequest{
//			ProxyRepositoryCreateRequest: ProxyRepositoryCreateRequest{
//				BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//					Name:   name,
//					Online: true,
//					Storage: HostedRepositoryStorageConfig{
//						BlobStoreName:               "default",
//						StrictContentTypeValidation: true,
//						WritePolicy:                 RepositoryStorageWritePolicyAllowRedploy,
//					},
//					Cleanup: &RepositoryCleanupConfig{
//						PolicyNames: []string{},
//					},
//				},
//				Proxy: RepositoryProxyConfig{
//					RemoteURL:      "https://registry.npm.terminus.io/",
//					ContentMaxAge:  -1,
//					MetadataMaxAge: 1440,
//				},
//				NegativeCache: RepositoryNegativeCacheConfig{
//					NotFoundCacheEnabled: false,
//					NotFoundCacheTTL:     200,
//				},
//				HttpClient: RepositoryHttpClientConfig{
//					Blocked:   false,
//					AutoBlock: false,
//					Connection: &RepositoryHttpClientConnection{
//						Retries:                 &[]int64{5}[0],
//						UserAgentSuffix:         &[]string{"100"}[0],
//						Timeout:                 &[]int64{50}[0],
//						EnableCircularRedirects: false,
//						EnableCookies:           true,
//					},
//					Authentication: &RepositoryHttpClientAuthentication{
//						Type:     RepositoryAuthenticationTypeUsername,
//						Username: "admin",
//						Password: "admin123",
//					},
//				},
//				RoutingRule: "",
//			},
//		},
//	))
//
//	assert.NoError(t, err)
//}
//
//func TestNexus_EnsureRepository(t *testing.T) {
//	err := n.EnsureRepository(NpmHostedRepositoryCreateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   "sdk-ensure-1",
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               "sdk-ensure-1",
//					StrictContentTypeValidation: true,
//					WritePolicy:                 RepositoryStorageWritePolicyAllowRedploy,
//				},
//				Cleanup: nil,
//			},
//		},
//	})
//	assert.NoError(t, err)
//}
//
//func TestNexus_CreateDockerHostedRepository(t *testing.T) {
//	name := "sdk-docker-hosted"
//
//	err := n.EnsureRepository(DockerHostedRepositoryCreateRequest{
//		HostedRepositoryCreateRequest: HostedRepositoryCreateRequest{
//			BaseRepositoryCreateRequest: BaseRepositoryCreateRequest{
//				Name:   name,
//				Online: true,
//				Storage: HostedRepositoryStorageConfig{
//					BlobStoreName:               name,
//					StrictContentTypeValidation: true,
//					WritePolicy:                 RepositoryStorageWritePolicyAllowRedploy,
//					BlobUseNetdata:              BlobUseNetdata{UseNetdata: true},
//				},
//				Cleanup: nil,
//			},
//		},
//		Docker: RepositoryDockerConfig{
//			V1Enabled:      true,
//			ForceBasicAuth: false,
//			HttpPort:       &[]int{5500}[0],
//			HttpsPort:      &[]int{5543}[0],
//		},
//	})
//	assert.NoError(t, err)
//}
