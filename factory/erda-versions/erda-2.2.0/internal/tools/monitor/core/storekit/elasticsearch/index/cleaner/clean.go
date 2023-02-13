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

package cleaner

import (
	"context"
	"fmt"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

type clearRequest struct {
	waitCh chan struct{}
	list   []string
}

func (p *provider) runCleanIndices(ctx context.Context) {
	p.loader.WaitAndGetIndices(ctx)
	p.Log.Infof("run indices clean with interval(%v)", p.Cfg.CheckInterval)
	defer p.Log.Infof("exit indices clean")
	timer := time.NewTimer(10 * time.Second)
	defer timer.Stop()
	for {
		select {
		case <-timer.C:
			err := p.CleanIndices(ctx, func(*loader.IndexEntry) bool { return true })
			if err != nil {
				p.Log.Errorf("failed to CleanIndices: %s", err)
			}
		case req := <-p.clearCh:
			p.deleteIndices(req.list)
			if req.waitCh != nil {
				close(req.waitCh)
			}
		case <-ctx.Done():
			return
		}
		timer.Reset(p.Cfg.CheckInterval)
	}
}

// CleanIndices .
func (p *provider) CleanIndices(ctx context.Context, filter loader.Matcher) error {
	indices := p.loader.AllIndices()
	if indices == nil {
		return nil
	}
	now := time.Now()
	removeList := p.getRemoveList(ctx, indices, filter, now)
	p.Log.Infof("about to remove %d indices: %v", len(removeList), removeList)
	if len(removeList) > 0 {
		err := p.deleteIndices(removeList)
		if err != nil {
			return err
		}
		p.loader.ReloadIndices()
	}
	return nil
}

func (p *provider) getRemoveList(ctx context.Context, indices *loader.IndexGroup, filter loader.Matcher, now time.Time) (list []string) {
	for _, entry := range indices.List {
		if filter(entry) && p.needToDelete(entry, now) {
			list = append(list, entry.Index)
		}
	}
	for _, ig := range indices.Groups {
		list = append(list, p.getRemoveList(ctx, ig, filter, now)...)
	}
	return list
}

func (p *provider) getIndicesList(ctx context.Context, indices *loader.IndexGroup) (list []string) {
	for _, entry := range indices.List {
		list = append(list, entry.Index)
	}
	for _, group := range indices.Groups {
		list = append(list, p.getIndicesList(ctx, group)...)
	}
	return list
}

func (p *provider) needToDelete(entry *loader.IndexEntry, now time.Time) bool {
	if entry.MaxT.IsZero() || (entry.Num >= 0 && entry.Active) {
		return false
	}
	duration := p.retentions.GetTTL(entry)
	if duration <= 0 {
		return false
	}
	return now.After(entry.MaxT.Add(duration))
}

func (p *provider) deleteIndices(removeList []string) error {
	const size = 10 // delete too many at once and the request will be rejected
	for len(removeList) >= size {
		err := p.deleteIndex(removeList[:size])
		if err != nil {
			return err
		}
		removeList = removeList[size:]
	}
	if len(removeList) > 0 {
		err := p.deleteIndex(removeList)
		if err != nil {
			return err
		}
	}
	return nil
}

func (p *provider) deleteIndex(indices []string) error {
	if p.Cfg.PrintOnly {
		p.Log.Infof("clean indices %d, %v", len(indices), indices)
		return nil
	}
	ctx, cancel := context.WithTimeout(context.Background(), p.Cfg.RequestTimeout)
	defer cancel()
	resp, err := p.loader.Client().DeleteIndex(indices...).Do(ctx)
	if err != nil {
		if e, ok := err.(*elastic.Error); ok {
			if e.Status == 404 {
				return nil
			}
		}
		return err
	}
	if !resp.Acknowledged {
		return fmt.Errorf("delete indices Acknowledged=false")
	}
	p.Log.Infof("clean indices %d, %v", len(indices), indices)
	return nil
}

func (p *provider) deleteByQuery() {
	ctx := context.Background()
	p.loader.WaitAndGetIndices(ctx)
	p.Log.Infof("Running ES docs clean...")
	defer p.Log.Infof("Docs cleaned...")

	indexGroup := p.loader.AllIndices()
	if indexGroup == nil {
		return
	}
	indices := p.getIndicesList(ctx, indexGroup)

	for _, index := range indices {
		ttl := elastic.NewRangeQuery("@timestamp").
			From(0).
			To(time.Now().AddDate(0, 0, -p.Cfg.DiskClean.TTL.MaxStoreTime).UnixNano() / 1e6)

		resp, err := p.loader.Client().DeleteByQuery().
			Index(index).
			WaitForCompletion(false).
			ProceedOnVersionConflict().
			Query(ttl).
			Pretty(true).
			DoAsync(ctx)
		if err != nil {
			p.Log.Errorf("delete failed. indices: %s, err: %v", index, err.Error())
			continue
		}
		p.AddTask(&TtlTask{
			TaskId:  resp.TaskId,
			Indices: []string{index},
		})
		p.Log.Infof("Clean doc by ttl, taskId: %s", resp.TaskId)
	}
}

func (p *provider) forceMerge(ctx context.Context, indices ...string) error {
	forceMergeResponse, err := p.loader.Client().Forcemerge(indices...).Do(ctx)
	if err != nil {
		p.Log.Error(err)
		return err
	}
	if forceMergeResponse.Shards.Failures != nil && len(forceMergeResponse.Shards.Failures) > 0 {
		p.Log.Errorf("force merge failed, err: %v", forceMergeResponse.Shards.Failures)
		return fmt.Errorf("force merge failed, err: %v", forceMergeResponse.Shards.Failures)
	}
	p.Log.Infof("Force merge successful. contains indices: %v", indices)
	return nil
}
