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

func (p *provider) runCacheLoader(ctx context.Context) error {
	p.Log.Infof("start cache-indices loader")
	defer p.Log.Info("exit cache-indices loader")
	timer := time.NewTimer(0)
	defer timer.Stop()
	var notifiers []chan error
	for {
		p.syncLock.Lock()
		for p.inSync {
			p.cond.Wait()
		}
		startSyncCh := p.startSyncCh

		select {
		case <-ctx.Done():
			p.syncLock.Unlock()
			return nil
		case ch := <-p.reloadCh:
			if ch != nil {
				notifiers = append(notifiers, ch)
			}
		case <-startSyncCh:
			p.inSync = false
			p.syncLock.Unlock()
			continue
		case <-timer.C:
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

		err := p.reloadIndicesFromCache(ctx)
		if err != nil {
			p.Log.Errorf("failed to reload indices from cache: %s", err)
		}
		for _, n := range notifiers {
			n <- err
			close(n)
		}
		notifiers = nil
		timer.Reset(p.Cfg.IndexReloadInterval)
		p.syncLock.Unlock()
	}
}

func (p *provider) reloadIndicesFromCache(ctx context.Context) error {
	indices, err := p.queryIndexFromCache()
	if err != nil {
		return err
	}
	ch := make(chan struct{})
	p.updateIndices(&indicesBundle{
		indices: indices,
		doneCh:  ch,
	})
	select {
	case <-ch:
	case <-ctx.Done():
		return nil
	}
	return nil
}

func (p *provider) storeIndicesToCache(indices *IndexGroup) error {
	expiration := 2*p.Cfg.IndexReloadInterval + p.Cfg.RequestTimeout
	bytes, _ := json.Marshal(indices)
	if err := p.Redis.Set(p.Cfg.CacheKeyPrefix+"-all", string(bytes), expiration).Err(); err != nil {
		return err
	}
	return nil
}

func (p *provider) queryIndexFromCache() (indices *IndexGroup, err error) {
	p.Log.Debugf("begin query indices from cache")
	start := time.Now()
	defer func() {
		if err == nil {
			p.Log.Debugf("got indices from cache, indices %d, duration: %s", GetIndicesNum(indices), time.Since(start))
		}
	}()

	indices = &IndexGroup{}
	value, err := p.Redis.Get(p.Cfg.CacheKeyPrefix + "-all").Result()
	if err != nil && err != redis.Nil {
		return nil, err
	}
	if len(value) > 0 {
		err = json.NewDecoder(strings.NewReader(value)).Decode(&indices)
		if err != nil {
			p.Log.Warnf("corrupted index cached-data:\n%s\n", value)
			return nil, err
		}
	}
	return indices, nil
}
