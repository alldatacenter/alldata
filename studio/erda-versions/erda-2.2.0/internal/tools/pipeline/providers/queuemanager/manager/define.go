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
	"sync"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager/types"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
)

// defaultManager is the default manager.
type defaultManager struct {
	queueByID         map[string]types.Queue   // key: pq id
	queueStopChanByID map[string]chan struct{} // key: pq id
	qLock             sync.RWMutex

	//pipelineCaches map[uint64]*spec.Pipeline
	//pCacheLock     sync.RWMutex

	dbClient *dbclient.Client
	etcd     *etcd.Store
	js       jsonstore.JsonStore
}

// New return a new queue manager.
func New(ctx context.Context, ops ...Option) types.QueueManager {
	var mgr defaultManager

	mgr.queueByID = make(map[string]types.Queue)
	mgr.queueStopChanByID = make(map[string]chan struct{})

	//mgr.pipelineCaches = make(map[uint64]*spec.Pipeline)

	// apply options
	for _, op := range ops {
		op(&mgr)
	}

	go func() {
		select {
		case <-ctx.Done():
			begin := time.Now()
			logrus.Infof("queueManager: begin stop")
			mgr.Stop()
			end := time.Now()
			logrus.Infof("queueManager: end stop, cost: %s", end.Sub(begin).String())
		}
	}()

	return &mgr
}

type Option func(manager *defaultManager)

func WithDBClient(dbClient *dbclient.Client) Option {
	return func(mgr *defaultManager) {
		mgr.dbClient = dbClient
	}
}

func WithEtcdClient(etcd *etcd.Store) Option {
	return func(mgr *defaultManager) {
		mgr.etcd = etcd
	}
}

func WithJsonStore(js jsonstore.JsonStore) Option {
	return func(mgr *defaultManager) {
		mgr.js = js
	}
}
