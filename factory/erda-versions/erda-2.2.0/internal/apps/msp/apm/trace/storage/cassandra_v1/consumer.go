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

package cassandra_v1

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"hash/fnv"
	"strconv"
	"time"

	"github.com/gocql/gocql"
	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda-infra/providers/cassandra"
	oap "github.com/erda-project/erda-proto-go/oap/trace/pb"
	"github.com/erda-project/erda/internal/pkg/monitor"
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

func (p *provider) initCassandra(session *cassandra.Session) error {
	for _, stmt := range []string{
		fmt.Sprintf(`
		CREATE TABLE IF NOT EXISTS spans (
			trace_id text,
			start_time bigint,
			end_time bigint,
			operation_name text,
			parent_span_id text,
			span_id text,
			tags map<text, text>,
			PRIMARY KEY (trace_id, start_time)
		) WITH CLUSTERING ORDER BY (start_time DESC)
			AND bloom_filter_fp_chance = 0.01
			AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
			AND comment = ''
			AND compaction = {'class': 'org.apache.cassandra.db.compaction.TimeWindowCompactionStrategy', 'compaction_window_size': '4', 'compaction_window_unit': 'HOURS'}
			AND compression = {'chunk_length_in_kb': '64', 'class': 'LZ4Compressor'}
			AND gc_grace_seconds = %d;
		`, p.Cfg.Output.Cassandra.GCGraceSeconds),
		fmt.Sprintf("ALTER TABLE spans WITH gc_grace_seconds = %d;", p.Cfg.Output.Cassandra.GCGraceSeconds),
	} {
		q := session.Session().Query(stmt).Consistency(gocql.All).RetryPolicy(nil)
		err := q.Exec()
		q.Release()
		if err != nil {
			return err
		}
		p.Log.Infof("cassandra init cql: %s", stmt)
	}
	return nil
}

func (p *provider) createTraceStatement() cassandra.StatementBuilder {
	return &TraceStatement{
		p: p,
	}
}

type TraceStatement struct {
	p *provider
}

func (ts *TraceStatement) GetStatement(data interface{}) (string, []interface{}, error) {
	return ts.p.getStatement(data)
}

func (p *provider) getStatement(data interface{}) (string, []interface{}, error) {
	span, ok := data.(*monitor.Span)
	if !ok {
		return "", nil, fmt.Errorf("value %#v must be Span", data)
	}

	// PRIMARY KEY is (trace_id, start_time), avoid the same start_time in the same trace_id
	startTime, endTime := getTimeRange(span)
	const cql = `INSERT INTO spans (trace_id, start_time, end_time, operation_name, parent_span_id, span_id, tags) VALUES (?, ?, ?, ?, ?, ?, ?) USING TTL ?;`
	return cql, []interface{}{
		span.TraceID,
		startTime,
		endTime,
		span.OperationName,
		span.ParentSpanID,
		span.SpanID,
		span.Tags,
		p.ttlSec,
	}, nil
}

const millisecond = int64(time.Millisecond)
const timeTailMask = millisecond / 10

func getTimeRange(span *monitor.Span) (int64, int64) {
	startTime, endTime := span.StartTime, span.EndTime
	if startTime%millisecond == 0 {
		tail := int64(convertToIntID(span.SpanID)) % timeTailMask
		startTime = startTime + tail
		endTime = endTime + tail
	}
	return startTime, endTime
}

func convertToIntID(id string) uint32 {
	hash := fnv.New32()
	hash.Write(reflectx.StringToBytes(id))
	return hash.Sum32()
}

func (p *provider) spotSpanConsumer(key []byte, value []byte, topic *string, timestamp time.Time) error {
	// write spot span to cassandra
	metric := &metrics.Metric{}
	dec := json.NewDecoder(bytes.NewReader(value))
	dec.UseNumber()
	if err := dec.Decode(&metric); err != nil {
		return err
	}
	span, err := metricToSpan(metric)
	if err != nil {
		return err
	}
	// metric = toSpan(span)
	// err = p.output.kafka.Write(metric)
	if err != nil {
		p.Log.Errorf("fail to push kafka: %s", err)
		return err
	}
	return p.output.cassandra.Write(span)
}

func (p *provider) oapSpanConsumer(key []byte, value []byte, topic *string, timestamp time.Time) error {
	// write oap sSpan (eg. jaeger \ skywalking \ opentelemetry ) to cassandra

	sSpan := &oap.Span{}
	if err := json.Unmarshal(value, sSpan); err != nil {
		return err
	}

	mSpan := &monitor.Span{
		OperationName: sSpan.Name,
		StartTime:     int64(sSpan.StartTimeUnixNano),
		EndTime:       int64(sSpan.EndTimeUnixNano),
		TraceID:       sSpan.TraceID,
		SpanID:        sSpan.SpanID,
		ParentSpanID:  sSpan.ParentSpanID,
		Tags:          sSpan.Attributes,
	}

	return p.output.cassandra.Write(mSpan)
}

// metricToSpan .
func metricToSpan(metric *metrics.Metric) (*monitor.Span, error) {
	var span monitor.Span
	span.Tags = metric.Tags

	traceID, ok := metric.Tags["trace_id"]
	if !ok {
		return nil, errors.New("trace_id cannot be null")
	}
	span.TraceID = traceID

	spanID, ok := metric.Tags["span_id"]
	if !ok {
		return nil, errors.New("span_id cannot be null")
	}
	span.SpanID = spanID

	parentSpanID, _ := metric.Tags["parent_span_id"]
	span.ParentSpanID = parentSpanID

	opName, ok := metric.Tags["operation_name"]
	if !ok {
		return nil, errors.New("operation_name cannot be null")
	}
	span.OperationName = opName

	value, ok := metric.Fields["start_time"]
	if !ok {
		return nil, errors.New("start_time cannot be null")
	}
	startTime, err := toInt64(value)
	if err != nil {
		return nil, fmt.Errorf("invalid start_time: %s", value)
	}
	span.StartTime = startTime

	value, ok = metric.Fields["end_time"]
	if !ok {
		return nil, errors.New("end_time cannot be null")
	}
	endTime, err := toInt64(value)
	if err != nil {
		return nil, fmt.Errorf("invalid end_time: %s", value)
	}
	span.EndTime = endTime
	return &span, nil
}

// toInt64 .
func toInt64(obj interface{}) (int64, error) {
	switch val := obj.(type) {
	case int:
		return int64(val), nil
	case int8:
		return int64(val), nil
	case int16:
		return int64(val), nil
	case int32:
		return int64(val), nil
	case int64:
		return val, nil
	case uint:
		return int64(val), nil
	case uint8:
		return int64(val), nil
	case uint16:
		return int64(val), nil
	case uint32:
		return int64(val), nil
	case uint64:
		return int64(val), nil
	case float32:
		return int64(val), nil
	case float64:
		return int64(val), nil
	case json.Number:
		return val.Int64()
	case string:
		v, err := strconv.ParseInt(val, 10, 64)
		if err != nil {
			return 0, err
		}
		return v, nil
	}
	return 0, fmt.Errorf("invalid type")
}

// toSpan . Span data is generated from various language agents.
//func toSpan(span *monitor.Span) *metrics.Metric {
//	metric := &metrics.Metric{
//		Name: "span",
//		Tags: map[string]string{
//			"_lt":            "transient",
//			"trace_id":       span.TraceID,
//			"span_id":        span.SpanID,
//			"parent_span_id": span.ParentSpanID,
//			"operation_name": span.OperationName,
//		},
//		Fields: map[string]interface{}{
//			"start_time": span.StartTime,
//			"end_time":   span.EndTime,
//		},
//		Timestamp: span.StartTime,
//	}
//	for k, v := range span.Tags {
//		metric.Tags[k] = v
//	}
//	return metric
//}
