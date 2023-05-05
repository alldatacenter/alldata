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

	"github.com/erda-project/erda-proto-go/core/monitor/event/pb"
	commonPb "github.com/erda-project/erda-proto-go/oap/common/pb"
	oapPb "github.com/erda-project/erda-proto-go/oap/event/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/event/storage"
)

type eventQueryService struct {
	p             *provider
	storageReader storage.Storage
}

func (s *eventQueryService) GetEvents(ctx context.Context, req *pb.GetEventsRequest) (*pb.GetEventsResponse, error) {
	if s.storageReader == nil {
		s.p.Log.Warnf("storage service is nil")
		return &pb.GetEventsResponse{Data: &pb.GetEventsResult{}}, nil
	}
	sel := &storage.Selector{
		Start: req.Start,
		End:   req.End,
		Debug: req.Debug,
	}
	if len(req.EventId) > 0 {
		sel.Filters = append(sel.Filters, &storage.Filter{
			Key:   "event_id",
			Op:    storage.EQ,
			Value: req.EventId,
		})
	}
	if len(req.RelationType) > 0 {
		sel.Filters = append(sel.Filters, &storage.Filter{
			Key:   "relations.res_type",
			Op:    storage.EQ,
			Value: req.RelationType,
		})
	}
	if len(req.RelationId) > 0 {
		sel.Filters = append(sel.Filters, &storage.Filter{
			Key:   "relations.res_id",
			Op:    storage.EQ,
			Value: req.RelationId,
		})
	}
	if len(req.Tags) > 0 {
		for key, val := range req.Tags {
			sel.Filters = append(sel.Filters, &storage.Filter{
				Key:   "tags." + key,
				Op:    storage.EQ,
				Value: val,
			})
		}
	}
	list, err := s.storageReader.QueryPaged(ctx, sel, int(req.PageNo), int(req.PageSize))
	if err != nil {
		return nil, err
	}

	resp := &pb.GetEventsResponse{Data: &pb.GetEventsResult{}}
	for _, item := range list {
		data := &oapPb.Event{
			EventID:      item.EventID,
			Name:         item.Name,
			Kind:         oapPb.Event_EventKind(oapPb.Event_EventKind_value[item.Kind]),
			TimeUnixNano: uint64(item.Timestamp),
			Attributes:   item.Tags,
			Message:      item.Content,
		}
		if item.Relations != nil {
			data.Relations = &commonPb.Relation{
				ResID:   item.Relations.ResID,
				ResType: item.Relations.ResType,
				TraceID: item.Relations.TraceID,
			}
		}
		resp.Data.Items = append(resp.Data.Items, data)
	}

	return resp, nil
}
