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

package jaeger

import (
	"errors"
	"fmt"
	"html"
	"io/ioutil"
	"net/http"
	"strconv"
	"time"

	"github.com/apache/thrift/lib/go/thrift"
	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda-oap-thirdparty-protocol/jaeger-thrift/jaeger"
	jaegerpb "github.com/erda-project/erda-proto-go/oap/collector/receiver/jaeger/pb"
	tracing "github.com/erda-project/erda-proto-go/oap/trace/pb"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/interceptor"
)

var (
	acceptedThriftFormats = map[string]struct{}{
		"application/x-thrift":                 {},
		"application/vnd.apache.thrift.binary": {},
	}
)

func ThriftDecoder(r *http.Request, entity interface{}) error {
	contentType := r.Header.Get("Content-Type")
	if _, ok := acceptedThriftFormats[contentType]; !ok {
		return errors.New(fmt.Sprintf("Unsupported content type: %v", html.EscapeString(contentType)))
	}
	if spansRequest, ok := entity.(*jaegerpb.PostSpansRequest); ok {
		body, err := ioutil.ReadAll(r.Body)
		if err != nil {
			return err
		}
		tdes := thrift.NewTDeserializer()
		batch := &jaeger.Batch{}
		if err = tdes.Read(r.Context(), batch, body); err != nil {
			return err
		}
		spansRequest.Spans = thrift2Proto(batch)
		extractAuthenticationTags(r, batch.Process.Tags)
	}
	return nil
}

func thrift2Proto(batch *jaeger.Batch) []*tracing.Span {
	if batch == nil || batch.Spans == nil || batch.Process == nil {
		return nil
	}
	blen := len(batch.Spans)
	if blen == 0 {
		return nil
	}
	spans := make([]*tracing.Span, 0)
	for _, tSpan := range batch.Spans {
		span := &tracing.Span{
			TraceID:           extractTraceID(tSpan),
			SpanID:            strconv.FormatInt(tSpan.SpanId, 10),
			StartTimeUnixNano: uint64(tSpan.StartTime) * uint64(time.Microsecond),
			EndTimeUnixNano:   uint64(tSpan.StartTime+tSpan.Duration) * uint64(time.Microsecond),
			Name:              tSpan.OperationName,
		}
		if tSpan.ParentSpanId != 0 {
			span.ParentSpanID = strconv.FormatInt(tSpan.ParentSpanId, 10)
		}
		span.Attributes = make(map[string]string)
		extractAttributes(span, batch.Process.Tags)
		extractAttributes(span, tSpan.Tags)
		span.Attributes[interceptor.TAG_SERVICE_ID] = batch.Process.ServiceName
		span.Attributes[interceptor.TAG_SERVICE_NAME] = batch.Process.ServiceName
		span.Attributes[interceptor.TAG_INSTRUMENT] = interceptor.TAG_JAEGER
		span.Attributes[interceptor.TAG_INSTRUMENT_VERSION] = span.Attributes[interceptor.TAG_JAEGER_VERSION]
		spans = append(spans, span)
	}
	return spans
}

func extractTraceID(tSpan *jaeger.Span) string {
	if tSpan.TraceIdHigh == 0 {
		return fmt.Sprintf("%016x", tSpan.TraceIdLow)
	}
	return fmt.Sprintf("%016x%016x", tSpan.TraceIdHigh, tSpan.TraceIdLow)
}

func extractAuthenticationTags(r *http.Request, tags []*jaeger.Tag) {
	if tags != nil {
		for _, tag := range tags {
			if tag.Key == interceptor.TAG_ERDA_ENV_ID || tag.Key == interceptor.TAG_ERDA_ENV_ID_C {
				// If the headers does not have x-msp-env-id, use msp.env.id contained in tags
				if val := r.Header.Get(interceptor.HEADER_ERDA_ENV_ID); val == "" {
					r.Header.Set(interceptor.HEADER_ERDA_ENV_ID, getTagValue(tag))
				}
			}
			if tag.Key == interceptor.TAG_ERDA_ENV_TOKEN || tag.Key == interceptor.TAG_ERDA_ENV_TOKEN_C {
				if val := r.Header.Get(interceptor.HEADER_ERDA_ENV_TOKEN); val == "" {
					r.Header.Set(interceptor.HEADER_ERDA_ENV_TOKEN, getTagValue(tag))
				}
			}
			if tag.Key == interceptor.TAG_ERDA_ORG || tag.Key == interceptor.TAG_ERDA_ORG_C {
				if val := r.Header.Get(interceptor.HEADER_ERDA_ORG); val == "" {
					r.Header.Set(interceptor.HEADER_ERDA_ORG, getTagValue(tag))
				}
			}
		}
	}
}

func extractAttributes(span *tracing.Span, tags []*jaeger.Tag) {
	if tags != nil {
		for _, tag := range tags {
			span.Attributes[tag.Key] = getTagValue(tag)
		}
	}
}

func getTagValue(tag *jaeger.Tag) string {
	if tag.IsSetVStr() {
		return tag.GetVStr()
	}
	if tag.IsSetVBinary() {
		return reflectx.BytesToString(tag.GetVBinary())
	}
	if tag.IsSetVBool() {
		return strconv.FormatBool(tag.GetVBool())
	}
	if tag.IsSetVDouble() {
		return strconv.FormatFloat(tag.GetVDouble(), 'E', -1, 64)
	}
	if tag.IsSetVLong() {
		return strconv.FormatInt(tag.GetVLong(), 10)
	}
	return tag.GetVStr()
}
