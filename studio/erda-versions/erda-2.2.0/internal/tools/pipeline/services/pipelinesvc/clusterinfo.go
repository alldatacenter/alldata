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
	"fmt"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/strutil"
)

// retryQueryClusterInfo query cluster info, retry if tcp error.
func (s *PipelineSvc) retryQueryClusterInfo(clusterName string, pipelineID uint64) (apistructs.ClusterInfoData, error) {
	// no need retry, cluster name is invalid
	if clusterName == "" {
		return apistructs.ClusterInfoData{}, fmt.Errorf("empty cluster name")
	}

	var result apistructs.ClusterInfoData
	var queryErr error

	// 2, 4, 8, 16, 30
	_ = loop.New(loop.WithInterval(time.Second*1), loop.WithDeclineRatio(2), loop.WithDeclineLimit(time.Second*30), loop.WithMaxTimes(5)).
		Do(func() (abort bool, err error) {
			cluster, err := s.clusterInfo.GetClusterInfoByName(clusterName)
			if err != nil {
				// need retry if tcp error
				if strutil.Contains(strings.ToLower(err.Error()),
					"dial tcp", "timeout") {
					logrus.Errorf("failed to query cluster info, will retry, clusterName: %s, pipelineID: %d, err: %v", clusterName, pipelineID, err)
					return false, err
				}
				// abnormal error, no need retry
				logrus.Errorf("failed to query cluster info, won't retry, clusterName: %s, pipelineID: %d, err: %v", clusterName, pipelineID, err)
				queryErr = err
				return true, err
			}
			result = cluster.CM
			return true, nil
		})

	return result, queryErr
}

// ClusterHook listen and dispatch cluster event from eventbox
func (s *PipelineSvc) ClusterHook(clusterEvent apistructs.ClusterEvent) error {
	if !strutil.Equal(clusterEvent.Content.Type, apistructs.K8S, true) &&
		!strutil.Equal(clusterEvent.Content.Type, apistructs.EDAS, true) &&
		!strutil.Equal(clusterEvent.Content.Type, apistructs.DCOS, true) {
		return errors.Errorf("invalid cluster event type: %s", clusterEvent.Content.Type)
	}

	if clusterEvent.Action != apistructs.ClusterActionCreate && clusterEvent.Action != apistructs.ClusterActionUpdate &&
		clusterEvent.Action != apistructs.ClusterActionDelete {
		return errors.Errorf("invalid cluster event action: %s", clusterEvent.Action)
	}
	s.clusterInfo.DispatchClusterEvent(clusterEvent)
	return nil
}
