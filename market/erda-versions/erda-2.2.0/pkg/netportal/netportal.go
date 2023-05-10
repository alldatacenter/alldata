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

package netportal

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/customhttp"
)

var bdl *bundle.Bundle

func init() {
	bdl = bundle.New(bundle.WithScheduler())
}

// NewNetportalRequest 在NewRequest基础上增加ClusterName参数，完成netportal代理请求
func NewNetportalRequest(clusterName, method, url string, body io.Reader) (*http.Request, error) {
	currentClusterName := os.Getenv("DICE_CLUSTER_NAME")
	if currentClusterName != clusterName {
		url = strings.TrimPrefix(url, "http://")
		url = strings.TrimPrefix(url, "https://")
		url = fmt.Sprintf("inet://%s/%s", clusterName, url)
	}
	return customhttp.NewRequest(method, url, body)
}
