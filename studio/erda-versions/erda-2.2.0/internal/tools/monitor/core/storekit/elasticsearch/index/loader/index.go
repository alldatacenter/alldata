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
	"math"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"
	"github.com/recallsong/go-utils/lang/size"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
)

func (p *provider) catIndices(ctx context.Context, prefix ...string) (elastic.CatIndicesResponse, error) {
	ctx, cancel := context.WithTimeout(ctx, p.Cfg.RequestTimeout)
	defer cancel()
	return p.es.Client().CatIndices().Index(strings.Join(prefix, ",")).Columns("index", "docs.count", "docs.deleted", "store.size").Do(ctx)
}

func (p *provider) getIndicesFromES(ctx context.Context, catIndices func(ctx context.Context, prefix ...string) (elastic.CatIndicesResponse, error)) (indices *IndexGroup, err error) {
	p.Log.Debugf("begin query indices from es")
	start := time.Now()
	defer func() {
		if err == nil {
			p.Log.Debugf("got indices from es, indices %d, duration: %s", GetIndicesNum(indices), time.Since(start))
		}
	}()

	prefixes := make([]string, len(p.matchers), len(p.matchers))
	for i, m := range p.matchers {
		prefixes[i] = m.prefix + "*"
	}

	// request index list
	resps, err := catIndices(ctx, prefixes...)
	if err != nil {
		return nil, err
	}

	// parse indices
	indices = &IndexGroup{}
	for _, item := range resps {
		// TODO: save IndexGroup split by matcher
		for _, matcher := range p.matchers {
			if !strings.HasPrefix(item.Index, matcher.prefix) {
				continue
			}
			indexName := item.Index[len(matcher.prefix):]
			var matched *index.MatchResult
			var num, timestamp int64 = -1, -1
		patterns:
			for _, ptn := range matcher.patterns {
				result, ok := ptn.Match(indexName, index.InvalidPatternValueChars)
				if !ok {
					continue
				}
				for i, v := range ptn.Vars {
					switch v {
					case index.IndexVarNumber:
						n, err := strconv.ParseInt(result.Vars[i], 10, 64)
						if err != nil {
							continue patterns
						}
						num = n
					case index.IndexVarTimestamp:
						n, err := strconv.ParseInt(result.Vars[i], 10, 64)
						if err != nil {
							continue patterns
						}
						timestamp = n
					case index.IndexVarNone:
					}
				}
				matched = result
				break
			}
			if matched == nil {
				p.Log.Debugf("invalid index format %s", item.Index)
				continue
			}
			storeBytes, err := size.ParseBytes(item.StoreSize)
			if err != nil {
				storeBytes = -1
			}
			entry := &IndexEntry{
				Index:       item.Index,
				Keys:        matched.Keys,
				Num:         num,
				DocsCount:   item.DocsCount,
				DocsDeleted: item.DocsDeleted,
				StoreBytes:  storeBytes,
				StoreSize:   item.StoreSize,
			}
			if timestamp > 0 {
				entry.MinT = time.Unix(timestamp/1000, (timestamp%1000)*int64(time.Millisecond))
				entry.MaxT = entry.MinT.Add(24*time.Hour - time.Nanosecond)
			}
			group := indices
			for _, key := range matched.Keys {
				ig, ok := group.Groups[key]
				if !ok {
					ig = &IndexGroup{}
					if group.Groups == nil {
						group.Groups = make(map[string]*IndexGroup)
					}
					group.Groups[key] = ig
				}
				group = ig
			}
			if len(matched.Vars) > 0 {
				group.List = append(group.List, entry)
			} else {
				entry.Fixed = true
				entry.Active = true
				group.Fixed = append(group.Fixed, entry)
			}
		}
	}
	return indices, nil
}

func (p *provider) getIndicesFromESWithTimeRange(ctx context.Context, catIndices func(ctx context.Context, prefix ...string) (elastic.CatIndicesResponse, error)) (*IndexGroup, error) {
	indices, err := p.getIndicesFromES(ctx, catIndices)
	if err != nil {
		return nil, err
	}
	p.initIndexGroup(indices)
	p.cleanTimeRangeCache(indices)
	return indices, nil
}

func (p *provider) initIndexGroup(ig *IndexGroup) {
	var maxn, maxt int64 = math.MinInt64, math.MinInt64
	var maxNumEntry, maxTimeEntry *IndexEntry
	for _, item := range ig.List {
		if item.Num < 0 {
			t := item.MinT.UnixNano()
			if t >= maxt {
				maxt = t
				maxTimeEntry = item
			}
		} else if item.Num > 0 {
			if item.Num >= maxn {
				maxn = item.Num
				maxNumEntry = item
			}
		}
	}
	if maxNumEntry != nil {
		maxNumEntry.Active = true
	} else if maxTimeEntry != nil {
		maxTimeEntry.Active = true
	}
	for _, entry := range ig.List {
		p.setupTimeRange(entry)
	}
	sort.Sort(sort.Reverse(IndexEntrys(ig.List)))

	// init sub groups
	for _, group := range ig.Groups {
		p.initIndexGroup(group)
	}
}

type timeRange struct {
	MinT        time.Time
	MaxT        time.Time
	DocsCount   int
	DocsDeleted int
}

func (p *provider) setupTimeRange(index *IndexEntry) {
	if p.Cfg.QueryIndexTimeRange && !index.Active && (index.MaxT.IsZero() || index.MinT.IsZero()) {
		ranges, ok := p.timeRanges[index.Index]
		if !ok || (index.DocsCount != ranges.DocsCount || index.DocsDeleted != ranges.DocsDeleted) {
			searchSource := elastic.NewSearchSource()
			searchSource.Aggregation("min_time", elastic.NewMinAggregation().Field(p.Cfg.TimestampKey))
			searchSource.Aggregation("max_time", elastic.NewMaxAggregation().Field(p.Cfg.TimestampKey))
			context, cancel := context.WithTimeout(context.Background(), p.Cfg.RequestTimeout)
			defer cancel()
			resp, err := p.es.Client().Search(index.Index).IgnoreUnavailable(true).AllowNoIndices(true).
				SearchSource(searchSource).Do(context)
			if err != nil {
				p.Log.Errorf("failed to query index %q time range: %s", index.Index, err)
				return
			} else if resp != nil && resp.Error != nil {
				p.Log.Errorf("failed to query index %q time range: %s", index.Index, jsonx.MarshalAndIndent(resp.Error))
				return
			}
			min, ok := resp.Aggregations.Min("min_time")
			if ok && min.Value != nil {
				t := int64(*min.Value)
				index.MinT = p.parseUnixToTime(t)
			}
			max, ok := resp.Aggregations.Max("max_time")
			if ok && max.Value != nil {
				t := int64(*max.Value)
				index.MaxT = p.parseUnixToTime(t)
			}
			p.Log.Debugf("query index %q , mint: %q, maxt: %q", index.Index, index.MinT.String(), index.MaxT.String())
			if min != nil && min.Value != nil &&
				max != nil && max.Value != nil {
				p.timeRanges[index.Index] = &timeRange{
					MinT:        index.MinT,
					MaxT:        index.MaxT,
					DocsCount:   index.DocsCount,
					DocsDeleted: index.DocsDeleted,
				}
			}
		} else {
			index.MinT = ranges.MinT
			index.MaxT = ranges.MaxT
		}
	}
}

func (p *provider) parseUnixToTime(t int64) time.Time {
	switch p.Cfg.TimestampUnit {
	case "s":
		return time.Unix(t, 0)
	case "ms":
		return time.Unix(t/1000, (t%1000)*1000000)
	default: // ns
		return time.Unix(t/int64(time.Second), t%int64(time.Second))
	}
}

func (p *provider) cleanTimeRangeCache(indices *IndexGroup) {
	set := make(map[string]bool)
	var setup func(indices *IndexGroup)
	setup = func(indices *IndexGroup) {
		for _, entry := range indices.List {
			set[entry.Index] = true
		}
		for _, group := range indices.Groups {
			setup(group)
		}
	}
	setup(indices)
	for index := range p.timeRanges {
		if !set[index] {
			delete(p.timeRanges, index)
		}
	}
}

func (p *provider) reloadIndicesFromES(ctx context.Context) error {
	indices, err := p.getIndicesFromESWithTimeRange(ctx, p.catIndices)
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

func (p *provider) runElasticSearchIndexLoader(ctx context.Context) error {
	p.Log.Infof("start elasticsearch-indices loader")
	defer p.Log.Info("exit elasticsearch-indices loader")
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

		err := p.reloadIndicesFromES(ctx)
		if err != nil {
			p.Log.Errorf("failed to reload indices from elasticsearch: %s", err)
		}
		for _, n := range notifiers {
			n <- err
			close(n)
		}
		notifiers = nil
		timer.Reset(p.Cfg.IndexReloadInterval)
	}
}

func (p *provider) RequestTimeout() time.Duration { return p.Cfg.RequestTimeout }
func (p *provider) QueryIndexTimeRange() bool     { return p.Cfg.QueryIndexTimeRange }
func (p *provider) Client() *elastic.Client       { return p.es.Client() }
func (p *provider) URLs() string                  { return p.es.URL() }
func (p *provider) LoadMode() LoadMode            { return LoadMode(p.Cfg.LoadMode) }

func (p *provider) WaitAndGetIndices(ctx context.Context) *IndexGroup {
	for {
		v := p.indices.Load()
		if v == nil {
			// wait for the index to complete loading
			select {
			case <-time.After(1 * time.Second):
			case <-ctx.Done():
				return nil
			}
			continue
		}
		return v.(*IndexGroup)
	}
}

func (p *provider) AllIndices() *IndexGroup {
	v, _ := p.indices.Load().(*IndexGroup)
	return v
}

func (p *provider) ReloadIndices() error {
	ch := make(chan error)
	p.reloadCh <- ch
	return <-ch
}

func (p *provider) WatchLoadEvent(f func(*IndexGroup)) {
	p.listeners = append(p.listeners, f)
}

func (p *provider) Indices(ctx context.Context, start, end int64, paths ...KeyPath) (list []string) {
	indices := p.WaitAndGetIndices(ctx)
	if indices == nil {
		if len(p.Cfg.DefaultIndex) > 0 {
			return []string{p.Cfg.DefaultIndex}
		}
		return nil
	}
	startT := time.Unix(start/int64(time.Second), start%int64(time.Second))
	endT := time.Unix(end/int64(time.Second), end%int64(time.Second))
	for _, path := range paths {
		p.findIndexByPath(path.Keys, path.Recursive, indices, startT, endT, &list)
	}
	if len(list) <= 0 && len(p.Cfg.DefaultIndex) > 0 {
		return []string{p.Cfg.DefaultIndex}
	}
	return list
}

func (p *provider) findIndexByPath(path []string, recursive bool, group *IndexGroup, start, end time.Time, list *[]string) {
	if group == nil {
		return
	}
	for _, key := range path {
		key = index.NormalizeKey(key)
		group = group.Groups[key]
		if group == nil {
			return
		}
	}
	findAllIndex(group, recursive, start, end, list)
}

func findAllIndex(group *IndexGroup, recursive bool, start, end time.Time, list *[]string) {
	for _, entry := range group.List {
		if matchTimeRange(entry, start, end) {
			*list = append(*list, entry.Index)
		}
	}
	for _, entry := range group.Fixed {
		*list = append(*list, entry.Index)
	}
	if recursive {
		for _, g := range group.Groups {
			findAllIndex(g, recursive, start, end, list)
		}
	}
}

func matchTimeRange(entry *IndexEntry, start, end time.Time) bool {
	if (entry.MinT.IsZero() || entry.MinT.Before(end) || entry.MinT.Equal(end)) &&
		(entry.MaxT.IsZero() || entry.MaxT.After(start) || entry.MaxT.Equal(start)) {
		return true
	}
	return false
}

func (p *provider) Keys(path ...string) (keys []string) {
	indices := p.WaitAndGetIndices(context.Background())
	for _, key := range path {
		key = index.NormalizeKey(key)
		indices = indices.Groups[key]
		if indices == nil {
			return nil
		}
	}
	for key := range indices.Groups {
		keys = append(keys, key)
	}
	return keys
}

func (p *provider) IndexGroup(path ...string) *IndexGroup {
	indices := p.WaitAndGetIndices(context.Background())
	for _, key := range path {
		key = index.NormalizeKey(key)
		indices = indices.Groups[key]
		if indices == nil {
			return nil
		}
	}
	return indices
}

func (p *provider) Prefixes() []string {
	prefixes := make([]string, len(p.matchers), len(p.matchers))
	for i, m := range p.matchers {
		prefixes[i] = m.prefix
	}
	return prefixes
}

func (p *provider) Match(indexName string) (matched *index.MatchResult) {
	for _, matcher := range p.matchers {
		if !strings.HasPrefix(indexName, matcher.prefix) {
			continue
		}
		indexName = indexName[len(matcher.prefix):]
	patterns:
		for _, ptn := range matcher.patterns {
			result, ok := ptn.Match(indexName, index.InvalidPatternValueChars)
			if !ok {
				continue
			}
			for i, v := range ptn.Vars {
				switch v {
				case index.IndexVarNumber:
					_, err := strconv.ParseInt(result.Vars[i], 10, 64)
					if err != nil {
						continue patterns
					}
				case index.IndexVarTimestamp:
					_, err := strconv.ParseInt(result.Vars[i], 10, 64)
					if err != nil {
						continue patterns
					}
				case index.IndexVarNone:
				}
			}
			matched = result
			break
		}
		if matched != nil {
			return matched
		}
	}
	return nil
}
