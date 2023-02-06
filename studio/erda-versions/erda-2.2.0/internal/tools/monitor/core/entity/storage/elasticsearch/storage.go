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
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda-proto-go/oap/entity/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/entity/storage"
)

func (p *provider) SetEntities(ctx context.Context, list []*pb.Entity) (int, error) {
	if len(list) <= 0 {
		return 0, nil
	}
	bulk := p.es.Client().Bulk()
	for _, data := range list {
		index, id, typ, upsertDoc, updateDoc, err := p.prepareSetRequest(ctx, data)
		if err != nil {
			return 0, err
		}
		req := elastic.NewBulkUpdateRequest().
			Index(index).Type(typ).Id(id).Doc(updateDoc).Upsert(upsertDoc)
		bulk.Add(req)
	}
	res, err := bulk.Timeout(p.writeTimeoutMS).Do(context.Background())
	if err != nil {
		return 0, err
	}
	if res.Errors {
		if len(res.Items) != len(list) {
			return 0, fmt.Errorf("request items(%d), but response items(%d)", len(list), len(res.Items))
		}
		berr := &elasticsearch.BatchWriteError{
			List:   make([]interface{}, 0, len(list)),
			Errors: make([]error, 0, len(list)),
		}
		for i, item := range res.Items {
			for _, result := range item { // len(item) is 1, contains index request only
				if !(result.Status >= 200 && result.Status <= 299) {
					var sb strings.Builder
					json.NewEncoder(&sb).Encode(result)
					berr.List = append(berr.List, list[i])
					berr.Errors = append(berr.Errors, errors.New(sb.String()))
					break
				}
			}
		}
		return len(list) - len(berr.Errors), berr
	}
	return len(list), nil
}

func (p *provider) SetEntity(ctx context.Context, data *pb.Entity) error {
	index, id, typ, body, doc, err := p.prepareSetRequest(ctx, data)
	_, err = p.es.Client().Update().Index(index).
		Type(typ).
		Id(id).
		Upsert(body).
		Doc(doc).
		Timeout(p.writeTimeoutMS).
		Do(ctx)
	return err
}

func (p *provider) RemoveEntity(ctx context.Context, typ, key string) (bool, error) {
	_, err := p.es.Client().Delete().
		Index(p.getIndex(typ, key)).
		Type(p.Cfg.IndexType).
		Id(p.getDocumentID(typ, key)).
		Timeout(p.writeTimeoutMS).
		Do(ctx)
	if err != nil {
		if e, ok := err.(*elastic.Error); ok && e.Status == 404 {
			return false, nil
		}
		return false, err
	}
	return true, nil
}

func (p *provider) GetEntity(ctx context.Context, typ, key string) (*pb.Entity, error) {
	query := elastic.NewBoolQuery().
		Filter(elastic.NewTermQuery("type", typ)).
		Filter(elastic.NewTermQuery("key", key))
	searchSource := elastic.NewSearchSource().Query(query)
	resp, err := p.es.Client().
		Search(p.getIndex(typ, key)).
		IgnoreUnavailable(true).AllowNoIndices(true).Timeout(p.queyTimeoutMS).
		SearchSource(searchSource).Size(1).
		Do(ctx)
	if err != nil {
		return nil, err
	}
	if resp == nil || resp.Hits == nil || len(resp.Hits.Hits) <= 0 || resp.Hits.Hits[0].Source == nil {
		return nil, nil
	}
	return parseData(*resp.Hits.Hits[0].Source)
}

func (p *provider) ListEntities(ctx context.Context, opts *storage.ListOptions) ([]*pb.Entity, int64, error) {
	query := elastic.NewBoolQuery()
	var typ string
	if opts != nil {
		typ = opts.Type
		if len(opts.Type) > 0 {
			query = query.Filter(elastic.NewTermQuery("type", opts.Type))
		}
		for k, v := range opts.Labels {
			query = query.Filter(elastic.NewTermQuery("labels."+k, v))
		}
		if opts.UpdateTimeUnixNanoMin > 0 || opts.UpdateTimeUnixNanoMax > 0 {
			rg := elastic.NewRangeQuery("updateTimeUnixNano")
			if opts.UpdateTimeUnixNanoMin > 0 {
				rg = rg.Gte(opts.UpdateTimeUnixNanoMin)
			}
			if opts.UpdateTimeUnixNanoMax > 0 {
				rg = rg.Lt(opts.UpdateTimeUnixNanoMax)
			}
			query = query.Filter(rg)
		}
	}
	limit := opts.Limit
	if limit <= 0 {
		limit = 100
	}
	searchSource := elastic.NewSearchSource().Query(query)
	indices := p.getIndices(typ)
	if opts.Debug {
		source, _ := searchSource.Source()
		fmt.Printf("search entity indices: %v\nsearchSource: %s\n", strings.Join(indices, ","), jsonx.MarshalAndIndent(source))
	}
	resp, err := p.es.Client().
		Search(indices...).
		IgnoreUnavailable(true).AllowNoIndices(true).Timeout(p.queyTimeoutMS).
		SearchSource(searchSource).Size(limit).
		Do(ctx)
	if err != nil {
		return nil, 0, err
	}
	if resp == nil || resp.Hits == nil {
		return nil, 0, nil
	}
	list := make([]*pb.Entity, 0, len(resp.Hits.Hits))
	for _, hit := range resp.Hits.Hits {
		if hit.Source == nil {
			continue
		}
		data, err := parseData(*hit.Source)
		if err != nil {
			continue
		}
		list = append(list, data)
	}
	return list, resp.Hits.TotalHits, nil
}

func parseData(data []byte) (*pb.Entity, error) {
	entity := &pb.Entity{}
	err := json.Unmarshal(data, entity)
	if err != nil {
		return nil, err
	}
	return entity, nil
}

func (p *provider) prepareSetRequest(ctx context.Context, data *pb.Entity) (index, id, typ string, upsertDoc interface{}, updateDoc interface{}, err error) {
	index, id, typ, upsertDoc, err = p.encodeToDocument(ctx)(data)
	if err != nil {
		return index, id, typ, upsertDoc, updateDoc, err
	}
	data.Id = id
	now := time.Now()
	data.CreateTimeUnixNano = now.UnixNano()
	data.UpdateTimeUnixNano = now.UnixNano()

	// always override
	// _, err = p.es.Client().Index().
	// 	Index(index).
	// 	Type(typ).
	// 	Id(id).
	// 	BodyJson(body).
	// 	Timeout(p.writeTimeoutMS).
	// 	Do(ctx)

	// upsert
	if data.Values == nil {
		data.Values = map[string]*structpb.Value{}
	}
	if data.Labels == nil {
		data.Labels = map[string]string{}
	}

	updateDoc = map[string]interface{}{
		"id":                 data.Id,
		"type":               data.Type,
		"key":                data.Key,
		"values":             data.Values,
		"labels":             data.Labels,
		"updateTimeUnixNano": data.UpdateTimeUnixNano,
	}
	return index, id, typ, upsertDoc, updateDoc, err
}
