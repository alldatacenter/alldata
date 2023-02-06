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
	"time"
)

func (p *provider) syncIndicesToCache(ctx context.Context) {
	p.Log.Infof("start indices-cache sync task")
	defer p.Log.Info("exit indices-cache sync task")

	if p.startSyncCh != nil {
		close(p.startSyncCh)
	}
	p.syncLock.Lock()
	p.inSync = true
	p.startSyncCh = make(chan struct{})
	defer func() {
		p.inSync = false
		p.syncLock.Unlock()
		p.cond.Broadcast()
	}()

	timer := time.NewTimer(0)
	defer timer.Stop()
	var notifiers []chan error
	for {
		select {
		case <-ctx.Done():
			return
		case ch := <-p.reloadCh:
			if ch != nil {
				notifiers = append(notifiers, ch)
			}
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

		err := p.doSyncIndiceToCache(ctx)
		if err != nil {
			p.Log.Errorf("failed to sync indices: %s", err)
		} else {
			p.Log.Debug("sync indices to cache ok")
		}
		for _, n := range notifiers {
			n <- err
			close(n)
		}
		notifiers = nil
		timer.Reset(p.Cfg.IndexReloadInterval)
	}
}

func (p *provider) doSyncIndiceToCache(ctx context.Context) error {
	indices, err := p.getIndicesFromESWithTimeRange(ctx, p.catIndices)
	if err != nil {
		return err
	}
	select {
	case <-ctx.Done():
		return context.Canceled
	case p.setIndicesCh <- &indicesBundle{indices: indices}:
	}
	return p.storeIndicesToCache(indices)
}
