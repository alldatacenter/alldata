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

package dispatcher

import (
	"context"
	"time"

	"github.com/buraksezer/consistent"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker/worker"
)

func (p *provider) initConsistentUntilSuccess(ctx context.Context) {
	for {
		// init new one
		c, err := p.makeConsistent(ctx)
		if err != nil {
			p.Log.Errorf("failed to init consistent(auto retry), err: %v", err)
			time.Sleep(p.Cfg.RetryInterval)
			continue
		}
		p.lock.Lock()
		p.consistent = c
		p.lock.Unlock()
		return
	}
}

func (p *provider) makeConsistent(ctx context.Context) (*consistent.Consistent, error) {
	// add worker to consistent members
	var consistentMembers []consistent.Member
	workers, err := p.LW.ListWorkers(ctx, worker.Official)
	if err != nil {
		return nil, err
	}
	for _, w := range workers {
		consistentMembers = append(consistentMembers, w)
	}
	// setup config
	// TODO adjust factor according to member count
	consistentCfg := consistent.Config{
		Hasher:            defaultHash{},
		PartitionCount:    p.Cfg.Consistent.PartitionCount,
		ReplicationFactor: p.Cfg.Consistent.ReplicationFactor,
		Load:              p.Cfg.Consistent.Load,
	}
	// construct consistent
	c := consistent.New(consistentMembers, consistentCfg)
	return c, nil
}
