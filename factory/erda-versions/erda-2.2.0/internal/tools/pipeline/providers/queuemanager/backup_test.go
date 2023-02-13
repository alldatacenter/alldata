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
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/manager"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
)

func TestContinueBackupQueueUsage(t *testing.T) {
	etcdClient := &etcd.Store{}
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(etcdClient), "Put", func(j *etcd.Store, ctx context.Context, key string, value string) error {
		return fmt.Errorf("failed to put msg")
	})
	defer pm.Unpatch()
	q := manager.New(context.Background(), manager.WithEtcdClient(etcdClient))
	//pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(q), "Export", func(mgr *types.QueueManager) json.RawMessage {
	//	u := &pb.QueueUsage{}
	//	bu, _ := json.Marshal(u)
	//	sna := manager.SnapshotObj{
	//		QueueUsageByID: map[string]json.RawMessage{
	//			"1": bu,
	//		},
	//	}
	//	snaByte, _ := json.Marshal(&sna)
	//	return snaByte
	//})
	//defer pm1.Unpatch()
	p := provider{
		QueueManager: q,
	}
	t.Run("continueBackupQueueUsage", func(t *testing.T) {
		ctx, cancel := context.WithCancel(context.Background())
		go p.continueBackupQueueUsage(ctx)
		time.Sleep(2 * time.Second)
		cancel()
	})

}
