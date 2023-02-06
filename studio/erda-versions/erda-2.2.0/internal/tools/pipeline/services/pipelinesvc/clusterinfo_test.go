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

package pipelinesvc

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

//import (
//	"fmt"
//	"os"
//	"testing"
//	"time"
//
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/bundle"
//	"github.com/erda-project/erda/pkg/http/httpclient"
//)
//
//func TestPipelineSvc_retryQueryClusterInfo(t *testing.T) {
//	os.Setenv("SCHEDULER_ADDR", "scheduler.default.svc.cluster.local:9091")
//	svc := PipelineSvc{
//		bdl: bundle.New(
//			bundle.WithHTTPClient(httpclient.New(httpclient.WithTimeout(time.Second, time.Second))),
//			bundle.WithScheduler(),
//		),
//	}
//
//	clusterInfo, err := svc.retryQueryClusterInfo("terminus-test", 1)
//	assert.NoError(t, err)
//	fmt.Println(clusterInfo)
//}

func TestClusterHook(t *testing.T) {
	svc := PipelineSvc{
		bdl: bundle.New(
			bundle.WithHTTPClient(httpclient.New(httpclient.WithTimeout(time.Second, time.Second))),
			bundle.WithScheduler(),
		),
	}

	err := svc.ClusterHook(apistructs.ClusterEvent{
		Content: apistructs.ClusterInfo{
			Type: "xxx",
		},
	})
	assert.Equal(t, false, err == nil)

	err = svc.ClusterHook(apistructs.ClusterEvent{
		Content: apistructs.ClusterInfo{
			Type: apistructs.K8S,
		},
		EventHeader: apistructs.EventHeader{
			Event: "patch",
		},
	})
	assert.Equal(t, false, err == nil)
}
