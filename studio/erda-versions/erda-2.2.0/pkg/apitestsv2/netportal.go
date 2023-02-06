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
	"fmt"
	"net/http"
	"net/url"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/customhttp"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/strutil"
)

const k8sServiceSuffix = ".svc.cluster.local"
const netNetportalSchemeHeader = "X-Portal-Scheme"

func handleCustomNetportalRequest(apiReq *apistructs.APIRequestInfo, netportalOpt *netportalOption) (*http.Request, error) {
	if err := checkNetportal(apiReq.URL, netportalOpt); err != nil {
		return nil, err
	}

	// use netportal
	var scheme = httpclientutil.GetProto(apiReq.URL)
	apiReq.URL = strutil.Concat(netportalOpt.url, "/", httpclientutil.RmProto(apiReq.URL))

	customReq, err := customhttp.NewRequest(apiReq.Method, apiReq.URL, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to handle custom netportal request, err: %v", err)
	}
	for k, values := range customReq.Header {
		for _, v := range values {
			apiReq.Headers.Add(k, v)
		}
	}

	apiReq.Headers.Add(netNetportalSchemeHeader, scheme)

	return customReq, nil
}

func checkNetportal(inputURL string, netportalOpt *netportalOption) error {
	if netportalOpt == nil || netportalOpt.url == "" {
		return fmt.Errorf("not find netportal")
	}

	// black access check
	r, err := url.ParseRequestURI(inputURL)
	if err != nil {
		return fmt.Errorf("failed to parse apitest url, url: %s, err: %v", inputURL, err)
	}
	ss := strings.SplitN(r.Host, ":", 2)
	ns := getK8sNamespace(ss[0])
	inBlacklist := strutil.Exist(netportalOpt.blacklistOfK8sNamespaceAccess, ns)
	if inBlacklist {
		return fmt.Errorf("no access to blacklist addresses %v", r.Host)
	}
	return nil
}

func getK8sNamespace(k8sHost string) string {
	hostWithoutSuf := strings.TrimSuffix(k8sHost, k8sServiceSuffix)
	ss := strings.Split(hostWithoutSuf, ".")
	ns := ss[len(ss)-1]
	return ns
}
