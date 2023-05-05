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

package task

import (
	"io"

	"github.com/erda-project/erda-infra/base/logs"
)

// WorkerManager .
type WorkerManager interface {
	Put(key int64, w Worker)
	Remove(key int64)
}

type simpleWorkerManager struct {
	worker map[int64]Worker
	log    logs.Logger
}

func newSimpleWorkerManager(log logs.Logger) WorkerManager {
	return &simpleWorkerManager{
		worker: make(map[int64]Worker),
		log:    log,
	}
}

func (p *simpleWorkerManager) Put(key int64, w Worker) {
	// remove worker if already exists
	p.Remove(key)
	p.worker[key] = w
}

func (p *simpleWorkerManager) Remove(key int64) {
	if w, ok := p.worker[key]; ok {
		if c, ok := w.(io.Closer); ok {
			if err := c.Close(); err != nil {
				p.log.Errorf("fail to close worker of key(%d)", key)
			}
		}
		delete(p.worker, key)
	}
}
