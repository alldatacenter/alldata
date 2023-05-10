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

package manager

import (
	"context"
	"fmt"

	"github.com/golang/protobuf/proto"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/pipeline/queue/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/queue"
)

func MakeQueueUsageBackupKey(qID string) string {
	return fmt.Sprintf("/devops/pipeline/queue_manager/actions/usage/%s", qID)
}

func (mgr *defaultManager) QueryQueueUsage(pq *apistructs.PipelineQueue) *pb.QueueUsage {
	mgr.qLock.RLock()
	defer mgr.qLock.RUnlock()
	usage := pb.QueueUsage{}
	val, err := mgr.etcd.Get(context.Background(), MakeQueueUsageBackupKey(queue.New(pq).ID()))
	if err != nil {
		logrus.Errorf("failed to query queue usage, err: %v", err)
		return nil
	}
	if err := proto.Unmarshal(val.Value, &usage); err != nil {
		logrus.Errorf("failed to unmarshal queue usage, err: %v", err)
		return nil
	}
	return &usage
}
