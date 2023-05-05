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

	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda-proto-go/msp/apm/exception/pb"
)

type CassandraSource struct {
	CassandraSession *cassandra.Session
}

func (source *CassandraSource) GetExceptions(ctx context.Context, req *pb.GetExceptionsRequest) ([]*pb.Exception, error) {
	exceptions, err := source.fetchErdaErrorFromCassandra(req)
	if err != nil {
		return nil, err
	}
	return exceptions, nil
}

func (source *CassandraSource) GetExceptionEventIds(ctx context.Context, req *pb.GetExceptionEventIdsRequest) ([]string, error) {
	// do cassandra query
	iter := source.CassandraSession.Session().Query("SELECT event_id FROM error_event_mapping WHERE error_id= ? limit ?", req.ExceptionID, 999).Iter()
	var data []string
	for {
		row := make(map[string]interface{})
		if !iter.MapScan(row) {
			break
		}
		data = append(data, conv.ToString(row["event_id"]))
	}
	return data, nil
}

func (source *CassandraSource) GetExceptionEvent(ctx context.Context, req *pb.GetExceptionEventRequest) (*pb.ExceptionEvent, error) {
	return source.fetchErdaEventFromCassandra(req), nil
}

func (source *CassandraSource) fetchErdaEventFromCassandra(req *pb.GetExceptionEventRequest) *pb.ExceptionEvent {
	iter := source.CassandraSession.Session().Query("SELECT * FROM error_events WHERE event_id = ?", req.ExceptionEventID).Iter()
	event := pb.ExceptionEvent{}
	for {
		row := make(map[string]interface{})
		if !iter.MapScan(row) {
			break
		}
		event.Tags = row["tags"].(map[string]string)
		if conv.ToString(event.Tags["terminus_key"]) != req.ScopeID {
			continue
		}
		event.Id = conv.ToString(row["event_id"])
		event.ExceptionID = conv.ToString(row["error_id"])
		event.RequestID = conv.ToString(row["request_id"])
		event.RequestSampled = conv.ToBool(event.Tags["request_sampled"], false)
		event.Metadata = row["meta_data"].(map[string]string)
		event.RequestContext = row["request_context"].(map[string]string)
		event.RequestHeaders = row["request_headers"].(map[string]string)
		event.Timestamp = row["timestamp"].(int64) / int64(time.Millisecond)
		var stacks []*pb.Stacks
		for _, info := range row["stacks"].([]string) {
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
	return &event
}

func (source *CassandraSource) fetchErdaErrorFromCassandra(req *pb.GetExceptionsRequest) ([]*pb.Exception, error) {
	iter := source.CassandraSession.Session().Query("SELECT * FROM error_description_v2 where terminus_key=? ALLOW FILTERING", req.ScopeID).Iter()

	var exceptions []*pb.Exception
	for {
		row := make(map[string]interface{})
		if !iter.MapScan(row) {
			break
		}
		e := pb.Exception{}
		tags := row["tags"].(map[string]string)
		e.Id = row["error_id"].(string)
		e.ScopeID = conv.ToString(row["terminus_key"])
		e.ClassName = conv.ToString(tags["class"])
		e.Method = conv.ToString(tags["method"])
		e.Type = conv.ToString(tags["type"])
		e.ExceptionMessage = conv.ToString(tags["exception_message"])
		e.File = conv.ToString(tags["file"])
		e.ServiceName = conv.ToString(tags["service_name"])
		e.ApplicationID = conv.ToString(tags["application_id"])
		e.RuntimeID = conv.ToString(tags["runtime_id"])

		source.fetchErdaErrorEventCount(req, &e)
		if e.EventCount > 0 {
			exceptions = append(exceptions, &e)
		}
	}
	return exceptions, nil
}

func (source *CassandraSource) fetchErdaErrorEventCount(req *pb.GetExceptionsRequest, exception *pb.Exception) {
	layout := "2006-01-02 15:04:05"
	count := int64(0)

	stat := "SELECT timestamp,count FROM error_count WHERE error_id= ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC"
	iterCount := source.CassandraSession.Session().Query(stat, exception.Id, req.StartTime*1e6, req.EndTime*1e6).Iter()
	index := 0
	for {
		rowCount := make(map[string]interface{})
		if !iterCount.MapScan(rowCount) {
			break
		}
		if index == 0 {
			exception.CreateTime = time.Unix(conv.ToInt64(rowCount["timestamp"], 0)/1e9, 10).Format(layout)
		}
		count += conv.ToInt64(rowCount["count"], 0)
		index++
		if index == iterCount.NumRows() {
			exception.UpdateTime = time.Unix(conv.ToInt64(rowCount["timestamp"], 0)/1e9, 10).Format(layout)
		}
	}

	exception.EventCount = count
}
