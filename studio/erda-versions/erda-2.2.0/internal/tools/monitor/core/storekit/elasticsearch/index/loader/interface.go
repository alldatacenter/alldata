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
	"fmt"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
)

type (
	// Matcher .
	Matcher func(index *IndexEntry) bool
	// Interface .
	Interface interface {
		WaitAndGetIndices(ctx context.Context) *IndexGroup
		AllIndices() *IndexGroup
		ReloadIndices() error
		WatchLoadEvent(func(*IndexGroup))

		Indices(ctx context.Context, start, end int64, path ...KeyPath) (list []string)
		Keys(path ...string) []string
		IndexGroup(path ...string) *IndexGroup

		Prefixes() []string
		Match(index string) *index.MatchResult

		RequestTimeout() time.Duration
		QueryIndexTimeRange() bool
		Client() *elastic.Client
		URLs() string
		LoadMode() LoadMode
	}
)

// Find .
func Find(ctx servicehub.Context, log logs.Logger, required bool) (Interface, error) {
	obj, name := index.FindService(ctx, "elasticsearch.index.loader")
	if obj != nil {
		loader, ok := obj.(Interface)
		if !ok {
			return nil, fmt.Errorf("%q is not Index Loader", name)
		}
		log.Debugf("use IndexLoader(%q)", name)
		return loader, nil
	} else if required {
		return nil, fmt.Errorf("%q is required", name)
	}
	return nil, nil
}

type (
	// IndexEntry .
	IndexEntry struct {
		Index       string    `json:"index"`
		DocsCount   int       `json:"docs_count"`
		DocsDeleted int       `json:"docs_deleted"`
		StoreBytes  int64     `json:"store_bytes"`
		StoreSize   string    `json:"store_size"`
		MinT        time.Time `json:"mint"`
		MaxT        time.Time `json:"maxt"`
		Keys        []string  `json:"keys"`
		Fixed       bool      `json:"fixed"`
		Num         int64     `json:"num"`
		Active      bool      `json:"active"`
	}
	// IndexGroup .
	IndexGroup struct {
		Groups map[string]*IndexGroup `json:"groups,omitempty"`
		List   []*IndexEntry          `json:"list,omitempty"`
		Fixed  []*IndexEntry          `json:"fixed,omitempty"`
	}
	// KeyPath .
	KeyPath struct {
		Keys      []string
		Recursive bool
	}
)

// IndexEntrys .
type IndexEntrys []*IndexEntry

func (entrys IndexEntrys) Len() int      { return len(entrys) }
func (entrys IndexEntrys) Swap(i, j int) { entrys[i], entrys[j] = entrys[j], entrys[i] }
func (entrys IndexEntrys) Less(i, j int) bool {
	if entrys[i].Num == entrys[j].Num {
		if entrys[i].MinT.Equal(entrys[j].MinT) {
			if entrys[i].MaxT.Equal(entrys[j].MaxT) {
				return entrys[i].StoreBytes < entrys[j].StoreBytes
			}
			return entrys[i].MaxT.Before(entrys[j].MaxT)
		}
		return entrys[i].MinT.Before(entrys[j].MinT)
	}
	return entrys[i].Num < entrys[j].Num
}

// GetIndicesNum .
func GetIndicesNum(indices *IndexGroup) (num int) {
	if indices == nil {
		return 0
	}
	num += len(indices.Fixed)
	num += len(indices.List)
	for _, ig := range indices.Groups {
		num += GetIndicesNum(ig)
	}
	return num
}

// LoadMode .
type LoadMode string

// LoadMode values
const (
	LoadFromElasticSearchOnly LoadMode = "LoadFromElasticSearchOnly"
	LoadFromCacheOnly         LoadMode = "LoadFromCacheOnly"
	LoadWithCache             LoadMode = "LoadWithCache"
)
