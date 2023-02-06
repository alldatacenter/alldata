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

package apitestsv2

import (
	"os"
	"strings"
)

type option struct {
	tryV1RenderJsonBodyFirst bool
	netportalOption          *netportalOption
}

type netportalOption struct {
	url                           string
	blacklistOfK8sNamespaceAccess []string
}

const defaultBlackList = "default,kube-system"
const blackListEnv = "APITEST_NETPORTAL_ACCESS_K8S_NAMESPACE_BLACKLIST"

type OpOption func(*option)

// WithTryV1RenderJsonBodyFirst 尝试先使用 v1 严格模式渲染 json body。不论是否打开开关，都会再使用 v2 逻辑渲染一遍。
// 为手动测试的接口测试提供兼容处理；自动化测试无需打开该开关。
func WithTryV1RenderJsonBodyFirst() OpOption {
	return func(opt *option) {
		opt.tryV1RenderJsonBodyFirst = true
	}
}

// WithNetportalConfigs set netportal url, whitelist and others.
func WithNetportalConfigs(netportalURL string) OpOption {
	return func(opt *option) {
		opt.netportalOption = &netportalOption{
			url:                           netportalURL,
			blacklistOfK8sNamespaceAccess: netportalAccessK8sNamespaceBlacklist(),
		}
	}
}

// netportalAccessK8sNamespaceBlacklist
func netportalAccessK8sNamespaceBlacklist() []string {
	list := os.Getenv(blackListEnv)
	if list == "" {
		list = defaultBlackList
	}
	return strings.Split(list, ",")
}
