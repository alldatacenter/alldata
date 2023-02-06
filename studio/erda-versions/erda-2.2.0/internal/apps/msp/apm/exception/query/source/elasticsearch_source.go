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

package source

import (
	"context"
	"encoding/json"
	"time"

	"github.com/recallsong/go-utils/conv"
	"google.golang.org/protobuf/types/known/structpb"

	eventpb "github.com/erda-project/erda-proto-go/core/monitor/event/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/exception/pb"
	entitypb "github.com/erda-project/erda-proto-go/oap/entity/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/model"
	"github.com/erda-project/erda/pkg/common/errors"
)

type ElasticsearchSource struct {
	Metric metricpb.MetricServiceServer
	Event  eventpb.EventQueryServiceServer
	Entity entitypb.EntityServiceServer
}

func (source *ElasticsearchSource) GetExceptions(ctx context.Context, req *pb.GetExceptionsRequest) ([]*pb.Exception, error) {
	// do es query
	conditions := map[string]string{
		"terminusKey": req.ScopeID,
	}

	entityReq := &entitypb.ListEntitiesRequest{
		Type:                  "error_exception",
		Labels:                conditions,
		Limit:                 int64(1000),
		UpdateTimeUnixNanoMin: req.StartTime * 1e6,
		UpdateTimeUnixNanoMax: req.EndTime * 1e6,
		Debug:                 req.Debug,
	}
	exceptions, _ := source.fetchErdaErrorFromES(ctx, entityReq, req.StartTime, req.EndTime)
	return exceptions, nil
}

func (source *ElasticsearchSource) GetExceptionEventIds(ctx context.Context, req *pb.GetExceptionEventIdsRequest) ([]string, error) {
	var data []string
	//do es query
	tags := map[string]string{
		"terminusKey": req.ScopeID,
	}
	eventReq := &eventpb.GetEventsRequest{
		RelationId:   req.ExceptionID,
		RelationType: "exception",
		Tags:         tags,
		PageNo:       1,
		PageSize:     999,
		Start:        time.Now().Add(-time.Hour * 24 * 7).UnixNano(),
		End:          time.Now().UnixNano(),
	}

	items, err := source.fetchErdaEventFromES(ctx, eventReq)
	if err != nil {
		return nil, err
	}

	for _, value := range items {
		data = append(data, value.EventId)
	}
	return data, nil
}

func (source *ElasticsearchSource) GetExceptionEvent(ctx context.Context, req *pb.GetExceptionEventRequest) (*pb.ExceptionEvent, error) {
	event := &pb.ExceptionEvent{}
	// do es query
	tags := map[string]string{
		"terminusKey": req.ScopeID,
	}
	eventReq := &eventpb.GetEventsRequest{
		EventId:      req.ExceptionEventID,
		RelationType: "exception",
		Tags:         tags,
		PageNo:       1,
		PageSize:     999,
		Start:        time.Now().Add(-time.Hour * 24 * 7).UnixNano(),
		End:          time.Now().UnixNano(),
		Debug:        true,
	}
	items, err := source.fetchErdaEventFromES(ctx, eventReq)
	if err != nil {
		return nil, err
	}

	if len(items) > 0 {
		item := items[0]
		event.Tags = item.Tags
		event.Id = item.EventId
		event.ExceptionID = item.ErrorId
		event.RequestID = item.RequestId
		event.RequestSampled = conv.ToBool(event.Tags["request_sampled"], false)
		event.Metadata = item.MetaData
		event.RequestContext = item.RequestContext
		event.RequestHeaders = item.RequestHeaders
		event.Timestamp = item.Timestamp / int64(time.Millisecond)
		var stacks []*pb.Stacks
		for _, info := range item.Stacks {
			var stack pb.Stacks
			var stackMap map[string]*structpb.Value
			if err := json.Unmarshal([]byte(info), &stackMap); err != nil {
				continue
			}

			stack.Stack = stackMap
			stacks = append(stacks, &stack)
		}
		event.Stacks = stacks
	}
	return event, nil
}

func (source *ElasticsearchSource) fetchErdaErrorFromES(ctx context.Context, req *entitypb.ListEntitiesRequest, startTime int64, endTime int64) (exceptions []*pb.Exception, err error) {
	listEntity, err := source.Entity.ListEntities(ctx, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	for _, value := range listEntity.Data.List {
		e := pb.Exception{}
		tags := value.Values
		e.Id = value.Key
		e.ScopeID = value.Labels["terminusKey"]
		e.ClassName = tags["class"].GetStringValue()
		e.Method = tags["method"].GetStringValue()
		e.Type = tags["type"].GetStringValue()
		e.ExceptionMessage = tags["exception_message"].GetStringValue()
		e.File = tags["file"].GetStringValue()
		e.ServiceName = tags["service_name"].GetStringValue()
		e.ApplicationID = tags["application_id"].GetStringValue()
		e.RuntimeID = tags["runtime_id"].GetStringValue()
		layout := "2006-01-02 15:04:05"

		eventReq := &eventpb.GetEventsRequest{
			RelationId:   value.Key,
			RelationType: "exception",
			Start:        startTime * 1e6,
			End:          endTime * 1e6,
			PageNo:       1,
			PageSize:     10000,
			Debug:        false,
		}
		items, err := source.fetchErdaEventFromES(ctx, eventReq)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		count := int64(0)
		for index, item := range items {
			if index == 0 {
				e.CreateTime = time.Unix(item.Timestamp/1e9, 10).Format(layout)
			}
			if index == len(items)-1 {
				e.UpdateTime = time.Unix(item.Timestamp/1e9, 10).Format(layout)
			}
			count++
		}
		e.EventCount = count
		if e.EventCount > 0 {
			exceptions = append(exceptions, &e)
		}
	}

	return exceptions, nil
}

func (source *ElasticsearchSource) fetchErdaEventFromES(ctx context.Context, req *eventpb.GetEventsRequest) (list []*model.Event, err error) {
	eventsResp, err := source.Event.GetEvents(ctx, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	for _, item := range eventsResp.Data.Items {
		erdaEvent := &model.Event{}
		erdaEvent.EventId = item.EventID
		erdaEvent.Timestamp = int64(item.TimeUnixNano)
		erdaEvent.ErrorId = item.Relations.ResID
		erdaEvent.RequestId = item.Attributes["requestId"]

		var stacks []string
		json.Unmarshal([]byte(item.Message), &stacks)
		erdaEvent.Stacks = stacks

		tagsMap := make(map[string]string)
		json.Unmarshal([]byte(item.Attributes["tags"]), &tagsMap)
		erdaEvent.Tags = tagsMap

		requestContextMap := make(map[string]string)
		json.Unmarshal([]byte(item.Attributes["requestContext"]), &requestContextMap)
		erdaEvent.RequestContext = requestContextMap

		requestHeadersMap := make(map[string]string)
		json.Unmarshal([]byte(item.Attributes["requestHeaders"]), &requestHeadersMap)
		erdaEvent.RequestHeaders = requestHeadersMap

		metaDataMap := make(map[string]string)
		json.Unmarshal([]byte(item.Attributes["metaData"]), &metaDataMap)
		erdaEvent.MetaData = metaDataMap

		list = append(list, erdaEvent)
	}

	return list, nil
}
