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
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"
	"github.com/recallsong/go-utils/lang/size"
)

// IndexEntry .
type IndexEntry struct {
	Index       string
	Name        string
	Time        string
	Num         int64
	MinT        time.Time
	MaxT        time.Time
	MinTS       int64
	MaxTS       int64
	DocsCount   int
	DocsDeleted int
	StoreSize   int64
	Active      bool
}

func (p *provider) reloadIndices(client *elastic.Client) (map[string][]*IndexEntry, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.C.RequestTimeout)
	resps, err := client.CatIndices().Index(p.C.IndexPrefix+"*").Columns("index", "docs.count", "docs.deleted", "store.size").Do(ctx)
	if err != nil {
		cancel()
		return nil, err
	}
	cancel()
	var count int
	indices := make(map[string][]*IndexEntry)
	for _, item := range resps {
		// rlogs-bceff2f83a74c436fbaf10a2f84ad27d2-2021.05-000232
		parts := strings.Split(item.Index, "-")
		if len(parts) != 4 {
			continue
		}
		num, err := strconv.ParseInt(parts[3], 10, 64)
		if err != nil {
			continue
		}
		storeSize, err := size.ParseBytes(item.StoreSize)
		if err != nil {
			storeSize = -1
		}
		entry := &IndexEntry{
			Index:       item.Index,
			Name:        parts[1],
			Time:        parts[2],
			Num:         num,
			DocsCount:   item.DocsCount,
			DocsDeleted: item.DocsDeleted,
			StoreSize:   storeSize,
		}
		indices[entry.Name] = append(indices[entry.Name], entry)
		count++
	}
	p.L.Infof("load indices %d, addons: %d", count, len(indices))
	return indices, nil
}

type timeRange struct {
	MinT        time.Time
	MaxT        time.Time
	DocsCount   int
	DocsDeleted int
}

func (p *provider) queryIndexTimeRange(client *elastic.Client, index *IndexEntry, timeRanges map[string]*timeRange) {
	if index.Active {
		return
	}
	ranges, ok := timeRanges[index.Index]
	// 该索引没查询过时间范围, 或者 索引数量对比之前有变化，则重新查询时间范围
	if !ok || (index.DocsCount != ranges.DocsCount || index.DocsDeleted != ranges.DocsDeleted) {
		searchSource := elastic.NewSearchSource()
		searchSource.Aggregation("min_time", elastic.NewMinAggregation().Field("timestamp"))
		searchSource.Aggregation("max_time", elastic.NewMaxAggregation().Field("timestamp"))
		context, cancel := context.WithTimeout(context.Background(), p.C.RequestTimeout)
		defer cancel()
		resp, err := client.Search(index.Index).IgnoreUnavailable(true).AllowNoIndices(true).
			SearchSource(searchSource).Do(context)
		if err != nil {
			p.L.Errorf("fail to query index %q time range: %s", index.Index, err)
			return
		} else if resp != nil && resp.Error != nil {
			p.L.Errorf("fail to query index %q time range: %s", index.Index, jsonx.MarshalAndIndent(resp.Error))
			return
		}
		min, ok := resp.Aggregations.Min("min_time")
		if ok && min.Value != nil {
			t := int64(*min.Value)
			index.MinT = time.Unix(t/int64(time.Second), t%int64(time.Second))
			index.MinTS = t
		}
		max, ok := resp.Aggregations.Max("max_time")
		if ok && max.Value != nil {
			t := int64(*max.Value)
			index.MaxT = time.Unix(t/int64(time.Second), t%int64(time.Second))
			index.MinTS = t
		}
		p.L.Debugf("query index %q , mint: %q, maxt: %q", index.Index, index.MinT.String(), index.MaxT.String())
		if min != nil && min.Value != nil &&
			max != nil && max.Value != nil {
			timeRanges[index.Index] = &timeRange{
				MinT:        index.MinT,
				MaxT:        index.MaxT,
				DocsCount:   index.DocsCount,
				DocsDeleted: index.DocsDeleted,
			}
		}
	} else {
		index.MinT = ranges.MinT
		index.MaxT = ranges.MaxT
		index.MinTS = ranges.MinT.UnixNano()
		index.MaxTS = ranges.MaxT.UnixNano()
	}
}

// IndexEntrys .
type IndexEntrys []*IndexEntry

func (entrys IndexEntrys) Len() int      { return len(entrys) }
func (entrys IndexEntrys) Swap(i, j int) { entrys[i], entrys[j] = entrys[j], entrys[i] }
func (entrys IndexEntrys) Less(i, j int) bool {
	if entrys[i].Num == entrys[j].Num {
		if entrys[i].MinT.Equal(entrys[j].MinT) {
			if entrys[i].MaxT.Equal(entrys[j].MaxT) {
				return entrys[i].StoreSize < entrys[j].StoreSize
			}
			return entrys[i].MaxT.Before(entrys[j].MaxT)
		}
		return entrys[i].MinT.Before(entrys[j].MinT)
	}
	return entrys[i].Num < entrys[j].Num
}

func (p *provider) reloadAllIndices() {
	indices, err := p.reloadIndices(p.client)
	if err != nil {
		p.L.Errorf("fail to load indices %s :", err)
		return
	}
	set := make(map[string]bool)
	for _, list := range indices {
		var max *IndexEntry
		for _, index := range list {
			if max == nil || max.Num < index.Num {
				max = index
			}
		}
		if max != nil {
			max.Active = true
		}
		for _, index := range list {
			set[index.Index] = true
			p.queryIndexTimeRange(p.client, index, p.timeRanges)
		}
	}
	// 清理时间范围缓存
	for index := range p.timeRanges {
		if !set[index] {
			delete(p.timeRanges, index)
		}
	}
	for _, list := range indices {
		sort.Sort(sort.Reverse(IndexEntrys(list)))
	}
	p.indices.Store(indices)
	// p.L.Debug("store log indices")
}

func (p *provider) getIndicesAndWait() map[string][]*IndexEntry {
	for {
		indices, _ := p.indices.Load().(map[string][]*IndexEntry)
		if indices != nil {
			return indices
		}
		time.Sleep(time.Second)
	}
}

func (p *provider) cleanIndices() {
	indices := p.getIndicesAndWait()
	deadline := time.Now().Add(-p.C.IndexTTL)
	for _, list := range indices {
		size := len(list)
		for _, entry := range list {
			if !entry.Active && !entry.MaxT.IsZero() && entry.MaxT.Before(deadline) && size > 1 {
				p.deleteIndex(entry.Index)
				size--
			}
		}
	}
	p.reload <- struct{}{}
}

func (p *provider) deleteIndex(index string) error {
	ctx, cancel := context.WithTimeout(context.Background(), p.C.RequestTimeout)
	defer cancel()
	resp, err := p.client.DeleteIndex(index).Do(ctx)
	if err != nil {
		p.L.Infof("delete index %s error %s", index, err)
		return err
	}
	p.L.Infof("delete index %s, %v", index, resp.Acknowledged)
	return nil
}

func (p *provider) createIndex(addon string) (*elastic.IndicesCreateResult, error) {
	ctx, cancel := context.WithTimeout(context.Background(), p.C.RequestTimeout)
	defer cancel()
	alias := p.C.IndexPrefix + addon
	index := "<" + alias + "-{now/d{yyyy.ww}}-000001>"
	resp, err := p.client.CreateIndex(index).BodyJson(
		map[string]interface{}{
			"aliases": map[string]interface{}{
				alias: make(map[string]interface{}),
			},
		},
	).Do(ctx)
	if err != nil {
		p.L.Infof("create index %s with alias %s error: %s", index, alias, err)
		return nil, err
	}
	p.L.Infof("create index %s with alias %s", index, alias)
	return resp, err
}
