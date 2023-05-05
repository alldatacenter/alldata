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

package loader

import (
	"context"
	"encoding/json"
	"strings"
	"time"

	"github.com/go-redis/redis"
)

func (p *provider) runCacheTablesLoader(ctx context.Context) error {
	p.Log.Info("start cache tables loader")
	defer p.Log.Info("exit cache tables loader")
	timer := time.NewTimer(0)
	defer timer.Stop()
	var notifiers []chan error
	for {
		select {
		case <-ctx.Done():
			return nil
		case ch := <-p.reloadCh:
			if ch != nil {
				notifiers = append(notifiers, ch)
			}
		case <-timer.C:
		}

		p.loadLock.Lock()
		if p.suppressCacheLoader {
			p.loadLock.Unlock()
			for _, notifier := range notifiers {
				p.reloadCh <- notifier
			}
			return nil
		}

		err := p.reloadTablesFromCache(ctx)
		if err != nil {
			p.Log.Errorf("failed to reload tables from cache: %s", err)
		}

	drain:
		for {
			select {
			case ch := <-p.reloadCh:
				if ch != nil {
					notifiers = append(notifiers, ch)
				}
			default:
				break drain
			}
		}

		for _, notifier := range notifiers {
			notifier <- err
			close(notifier)
		}
		notifiers = nil
		timer.Reset(p.Cfg.ReloadInterval)
		p.loadLock.Unlock()
	}
}

func (p *provider) reloadTablesFromCache(ctx context.Context) error {
	tables := map[string]*TableMeta{}
	val, err := p.Redis.Get(p.Cfg.CacheKeyPrefix + "-all").Result()
	if err != nil && err != redis.Nil {
		return err
	}
	if len(val) == 0 {
		return nil
	}

	err = json.NewDecoder(strings.NewReader(val)).Decode(&tables)
	if err != nil {
		p.Log.Warnf("corrupted table cached-data: \n%s\n", val)
		return err
	}

	ch := p.updateTables(tables)
	select {
	case <-ch:
	case <-ctx.Done():
	}
	return nil
}
