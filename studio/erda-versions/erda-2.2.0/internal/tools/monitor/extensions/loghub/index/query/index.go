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

package query

import (
	"context"
	"crypto/md5"
	"encoding/hex"
	"fmt"
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

var requestTimeout = 120 * time.Second

func (p *provider) calcIndexCacheBucketKey(cluster, esUrl string) string {
	return fmt.Sprintf("%s-%s", cluster, p.md5V(esUrl))
}

func (p *provider) md5V(str string) string {
	h := md5.New() // #nosec G401
	h.Write([]byte(str))
	return hex.EncodeToString(h.Sum(nil))
}

func (p *provider) reloadAllIndices() {
	clients := p.getAllESClients()
	indices := make(map[string]map[string][]*IndexEntry)
	clusters := make(map[string]bool)
	for _, client := range clients {

		cacheKey := p.calcIndexCacheBucketKey(client.Cluster, client.URLs)

		clusters[cacheKey] = true
		addons, err := p.reloadIndices(client.Client)
		if err != nil {
			p.L.Errorf("fail to load indices for cluster %s :", client.Cluster, err)
			continue
		}
		// 查询索引时间范围
		timeRanges := p.timeRanges[cacheKey]
		if timeRanges == nil {
			timeRanges = make(map[string]*timeRange)
			p.timeRanges[cacheKey] = timeRanges
		}
		set := make(map[string]bool)
		for _, list := range addons {
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
				p.queryIndexTimeRange(client.Client, index, timeRanges)
			}
		}
		// 清理时间范围缓存
		for index := range timeRanges {
			if !set[index] {
				delete(timeRanges, index)
			}
		}
		indices[cacheKey] = addons
	}
	for cacheKey := range p.timeRanges {
		if !clusters[cacheKey] {
			delete(p.timeRanges, cacheKey)
		}
	}
	for _, addons := range indices {
		for _, list := range addons {
			sort.Sort(sort.Reverse(IndexEntrys(list)))
		}
	}
	p.indices.Store(indices)
	p.L.Debug("store log indices")
}
func (p *provider) reloadIndices(client *elastic.Client) (map[string][]*IndexEntry, error) {
	ctx, cancel := context.WithTimeout(context.Background(), requestTimeout)
	resps, err := client.CatIndices().Index("rlogs-*").Columns("index", "docs.count", "docs.deleted", "store.size").Do(ctx)
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
		context, cancel := context.WithTimeout(context.Background(), requestTimeout)
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
