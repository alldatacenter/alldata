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
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (mgr *defaultManager) PopOutPipelineFromQueue(pipelineID uint64) {
	p := mgr.ensureQueryPipelineDetail(pipelineID)
	if p == nil {
		return
	}

	mgr.cancelPipelineIfInQueue(p)

	relatedQueueID, ok := p.GetPipelineQueueID()
	if !ok {
		return
	}

	mgr.qLock.RLock()
	defer mgr.qLock.RUnlock()
	q := mgr.queueByID[strconv.FormatUint(relatedQueueID, 10)]
	if q == nil {
		return
	}
	q.PopOutPipeline(p)
}

func (mgr *defaultManager) cancelPipelineIfInQueue(p *spec.Pipeline) {
	if p.Status.InQueue() {
		for {
			if err := mgr.dbClient.UpdatePipelineBaseStatus(p.ID, apistructs.PipelineStatusStopByUser); err != nil {
				logrus.Errorf("%s: failed to update pipeline status from Queue to StopByUser(auto retry), pipelineID: %d, err: %v", defaultQueueManagerLogPrefix, p.ID, err)
				time.Sleep(time.Second * 5)
				continue
			}
			logrus.Infof("%s: update pipeline status from Queue to StopByUser, pipelineID: %d", defaultQueueManagerLogPrefix, p.ID)
			break
		}
	}
}
