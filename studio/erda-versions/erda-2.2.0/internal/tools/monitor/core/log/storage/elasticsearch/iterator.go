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

package elasticsearch

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

const useScrollQuery = false
const useInMemContentFilter = false

func (p *provider) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	// TODO check org
	var keyPaths []loader.KeyPath
	for _, orgName := range sel.Meta.OrgNames {
		keyPaths = append(keyPaths, loader.KeyPath{
			Keys:      []string{orgName},
			Recursive: true,
		})
	}
	indices := p.Loader.Indices(ctx, sel.Start, sel.End, keyPaths...)
	pageSize := p.Cfg.ReadPageSize
	if sel.Meta.PreferredBufferSize > 0 {
		pageSize = sel.Meta.PreferredBufferSize
	}
	var matcher func(data *pb.LogItem) bool
	if useInMemContentFilter {
		for _, filter := range sel.Filters {
			val, _ := filter.Value.(string)
			if filter.Key != "content" || len(val) <= 0 {
				continue
			}
			switch filter.Op {
			case storage.EQ:
				matcher = func(data *pb.LogItem) bool {
					return data.Content == val
				}
			case storage.REGEXP:
				regex, err := regexp.Compile(val)
				if err != nil {
					p.Log.Debugf("invalid regexp %q", val)
					return storekit.EmptyIterator{}, nil
				}
				matcher = func(data *pb.LogItem) bool {
					return regex.MatchString(data.Content)
				}
			case storage.CONTAINS:
				matcher = func(data *pb.LogItem) bool {
					return strings.Contains(data.Content, val)
				}
			}
		}
	}

	if useScrollQuery || sel.Meta.PreferredIterateStyle == storage.Scroll {
		searchSource := getSearchSource(sel.Start, sel.End, sel)
		searchSource.From(sel.Skip.FromOffset).SearchAfter(sel.Skip.AfterId.Raw()...)
		if sel.Debug {
			source, _ := searchSource.Source()
			fmt.Printf("indices: %v\nsearchSource: %s\n", strings.Join(indices, ","), jsonx.MarshalAndIndent(source))
		}
		return elasticsearch.NewScrollIterator(
			ctx, p.client, p.Cfg.QueryTimeout,
			pageSize, indices, []*elasticsearch.SortItem{
				{
					Key:       "timestamp",
					Ascending: true,
				},
				{
					Key:       "id",
					Ascending: true,
				},
				{
					Key:       "offset",
					Ascending: true,
				},
			},
			func() (*elastic.SearchSource, error) { return searchSource, nil },
			decodeFunc(sel.Start, sel.End, matcher),
		)
	}
	if sel.Debug {
		searchSource := getSearchSource(sel.Start, sel.End, sel)
		source, _ := searchSource.Source()
		fmt.Printf("indices: %v\nsearchSource: %s\n", strings.Join(indices, ","), jsonx.MarshalAndIndent(source))
	}
	return elasticsearch.NewSearchIterator(
		ctx, p.client, p.Cfg.QueryTimeout,
		sel.Skip.FromOffset, pageSize,
		indices, []*elasticsearch.SortItem{
			{
				Key:       "timestamp",
				Ascending: true,
			},
			{
				Key:       "id",
				Ascending: true,
			},
			{
				Key:       "offset",
				Ascending: true,
			},
		},
		sel.Skip.AfterId.Raw(),
		func() (*elastic.SearchSource, error) {
			return getSearchSource(sel.Start, sel.End, sel), nil
		},
		decodeFunc(sel.Start, sel.End, matcher),
	)
}

func getSearchSource(start, end int64, sel *storage.Selector) *elastic.SearchSource {
	searchSource := elastic.NewSearchSource()
	query := elastic.NewBoolQuery().Filter(elastic.NewRangeQuery("timestamp").Gte(start).Lt(end))

	// compatibility for source=deploy
	isContainer := true
	for _, filter := range sel.Filters {
		if filter.Key != "source" {
			continue
		}
		if val, ok := filter.Value.(string); ok && val != "container" {
			isContainer = false
		}
	}

	for _, filter := range sel.Filters {
		val, ok := filter.Value.(string)
		if !ok {
			continue
		}
		if useInMemContentFilter {
			if filter.Key == "content" {
				continue
			}
		}
		// compatibility for source=deploy, ignore tags filters
		if !isContainer && strings.HasPrefix(filter.Key, "tags.") {
			continue
		}
		switch filter.Op {
		case storage.EQ:
			query = query.Filter(elastic.NewTermQuery(filter.Key, val))
		case storage.REGEXP:
			query = query.Filter(elastic.NewRegexpQuery(filter.Key, val))
		case storage.EXPRESSION:
			query = query.Filter(elastic.NewQueryStringQuery(val).DefaultField("content").DefaultOperator("AND"))
		case storage.CONTAINS:
			query = query.Filter(elastic.NewQueryStringQuery(escapeQueryString(val)).DefaultField("content").DefaultOperator("AND"))
		}
	}
	if sel.Meta.Highlight {
		searchSource.Highlight(elastic.NewHighlight().
			PreTags("").
			PostTags("").
			FragmentSize(1).
			RequireFieldMatch(true).
			BoundaryScannerType("word").
			Field("*"))
	}
	return searchSource.Query(query)
}

var replacer = strings.NewReplacer(`"`, `\"`)

func escapeQueryString(value string) string {
	return fmt.Sprintf(`"%s"`, replacer.Replace(value))
}

var skip = errors.New("skip")

func decodeFunc(start, end int64, matcher func(data *pb.LogItem) bool) func(body *elastic.SearchHit) (interface{}, error) {
	return func(hit *elastic.SearchHit) (interface{}, error) {
		var data log.Log
		err := json.Unmarshal(*hit.Source, &data)
		if err != nil {
			return nil, err
		}
		if data.Timestamp < start || data.Timestamp >= end {
			return nil, skip
		}
		item := &pb.LogItem{
			Source:    data.Source,
			Id:        data.ID,
			Stream:    data.Stream,
			Timestamp: strconv.FormatInt(data.Timestamp, 10),
			UnixNano:  data.Timestamp,
			Offset:    data.Offset,
			Content:   data.Content,
			Level:     data.Tags["level"],
			RequestId: data.Tags["request_id"],
			Tags:      data.Tags,
			UniqId:    hit.Id,
		}
		if len(hit.Highlight) > 0 {
			highlight := map[string]*structpb.ListValue{}
			for k, v := range hit.Highlight {
				var items []interface{}
				for _, token := range v {
					items = append(items, token)
				}
				list, err := structpb.NewList(items)
				if err != nil {
					continue
				}
				highlight[k] = list
			}
			item.Highlight = highlight
		}
		if matcher != nil && !matcher(item) {
			return nil, skip
		}
		return item, nil
	}
}
