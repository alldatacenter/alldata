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

//import (
//	"fmt"
//	"os"
//	"testing"
//	"time"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//	"github.com/erda-project/erda/pkg/http/httpclient"
//)
//
//func TestBundle_ListGittarFileTreeNodes(t *testing.T) {
//	hc := httpclient.New(httpclient.WithEnableAutoRetry(false))
//	os.Setenv("GITTAR_ADAPTOR_ADDR", "gittar-adaptor.default.svc.cluster.local:1086")
//	bdl := New(WithGittarAdaptor(), WithHTTPClient(hc))
//	begin := time.Now()
//	nodes, err := bdl.ListGittarFileTreeNodes(apistructs.UnifiedFileTreeNodeListRequest{
//		Scope:        "project-app",
//		ScopeID:      "59",
//		Pinode:       "NTkvMjI=",
//		IdentityInfo: apistructs.IdentityInfo{UserID: "1"},
//	}, 1)
//	end := time.Now()
//	fmt.Println(end.Sub(begin))
//	assert.NoError(t, err)
//	fmt.Println(len(nodes))
//}
