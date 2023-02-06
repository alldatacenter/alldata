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

package queuemanager

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/manager"
)

const logPrefixContinueBackupQueueUsage = "[queue usage backup]"

func (q *provider) continueBackupQueueUsage(ctx context.Context) {
	done := make(chan struct{})
	errDone := make(chan error)

	var costTime time.Duration
	for {
		go func() {
			begin := time.Now()
			backup := q.QueueManager.Export()
			end := time.Now()
			costTime = end.Sub(begin)
			queueSnapshot := manager.SnapshotObj{}
			if err := json.Unmarshal(backup, &queueSnapshot); err != nil {
				errDone <- err
				return
			}
			errs := []string{}
			for qID, qMsg := range queueSnapshot.QueueUsageByID {
				if _, err := q.EtcdClient.Put(ctx, manager.MakeQueueUsageBackupKey(qID), string(qMsg)); err != nil {
					errs = append(errs, fmt.Sprintf("%v", err))
					continue
				}
			}
			if len(errs) > 0 {
				usageErr := fmt.Errorf(strings.Join(errs, ","))
				errDone <- usageErr
				return
			}
			done <- struct{}{}
		}()

		select {
		case <-done:
			logrus.Debugf("%s: sleep 30s for next backup (cost %s this time)", logPrefixContinueBackupQueueUsage, costTime)
			time.Sleep(time.Second * 30)
		case err := <-errDone:
			logrus.Errorf("%s: failed to backup, wait 10s for next loading, err: %v", logPrefixContinueBackupQueueUsage, err)
			time.Sleep(time.Second * 10)
		case <-ctx.Done():
			return
		}
	}
}
