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

package opentelemetry

import (
	"compress/gzip"
	"encoding/hex"
	"errors"
	"fmt"
	"html"
	"io/ioutil"
	"net/http"
	"strconv"

	"github.com/golang/protobuf/proto"
	"github.com/recallsong/go-utils/reflectx"
	otlpv11 "go.opentelemetry.io/proto/otlp/common/v1"
	otlpv1 "go.opentelemetry.io/proto/otlp/trace/v1"

	"github.com/erda-project/erda-proto-go/oap/collector/receiver/opentelemetry/pb"
	tracepb "github.com/erda-project/erda-proto-go/oap/trace/pb"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/interceptor"
)

var (
	acceptedFormats = map[string]struct{}{
		"application/x-protobuf": {},
	}

	EncodingKey = "Content-Encoding"
	GZIP        = "gzip"

	SpanKind_Name = map[int32]string{
		0: "local",
		1: "local",
		2: "server",
		3: "client",
		4: "producer",
		5: "consumer",
	}
)

func ProtoDecoder(req *http.Request, entity interface{}) error {
	contentType := req.Header.Get("Content-Type")
	if _, ok := acceptedFormats[contentType]; !ok {
		return errors.New(fmt.Sprintf("Unsupported content type: %v", html.EscapeString(contentType)))
	}
	if entity, ok := entity.(*pb.PostSpansRequest); ok {
		body, err := readBody(req)
		if err != nil {
			return err
		}
		var tracesData otlpv1.TracesData
		err = proto.Unmarshal(body, &tracesData)
		if err != nil {
			return err
		}
		entity.Spans = convertSpans(&tracesData)
	}
	return nil
}

func convertSpans(tracesData *otlpv1.TracesData) []*tracepb.Span {
	spans := make([]*tracepb.Span, 0)
	if tracesData.ResourceSpans != nil && len(tracesData.ResourceSpans) > 0 {
		for _, resource := range tracesData.ResourceSpans {
			if resource.InstrumentationLibrarySpans != nil && len(resource.InstrumentationLibrarySpans) > 0 {
				for _, instrumentation := range resource.InstrumentationLibrarySpans {
					if instrumentation.Spans != nil && len(instrumentation.Spans) > 0 {
						for _, otlpSpan := range instrumentation.Spans {
							attributes := make(map[string]string)
							if otlpSpan.Attributes != nil {
								for _, attr := range otlpSpan.Attributes {
									attributes[attr.Key] = getStringValue(attr.Value)
								}
							}
							attributes[interceptor.TAG_INSTRUMENT] = instrumentation.InstrumentationLibrary.Name
							attributes[interceptor.TAG_INSTRUMENT_VERSION] = instrumentation.InstrumentationLibrary.Version
							attributes[interceptor.TAG_SPAN_KIND] = SpanKind_Name[int32(otlpSpan.Kind)]
							if resource.Resource != nil && resource.Resource.Attributes != nil {
								for _, attr := range resource.Resource.Attributes {
									attributes[attr.Key] = getStringValue(attr.Value)
								}
							}
							span := &tracepb.Span{
								TraceID:           hex.EncodeToString(otlpSpan.TraceId[:]),
								SpanID:            hex.EncodeToString(otlpSpan.SpanId[:]),
								StartTimeUnixNano: otlpSpan.StartTimeUnixNano,
								EndTimeUnixNano:   otlpSpan.EndTimeUnixNano,
								Name:              otlpSpan.Name,
								Attributes:        attributes,
							}
							if otlpSpan.ParentSpanId != nil {
								span.ParentSpanID = hex.EncodeToString(otlpSpan.ParentSpanId[:])
							}
							spans = append(spans, span)
						}
					}
				}
			}
		}
	}
	return spans
}

func readBody(req *http.Request) ([]byte, error) {
	if encoding := req.Header.Get(EncodingKey); encoding == GZIP {
		r, err := gzip.NewReader(req.Body)
		if err != nil {
			return nil, err
		}
		defer func() {
			_ = r.Close()
		}()
		return ioutil.ReadAll(r)
	}
	return ioutil.ReadAll(req.Body)
}

func getStringValue(value *otlpv11.AnyValue) string {
	if x, ok := value.GetValue().(*otlpv11.AnyValue_StringValue); ok {
		return x.StringValue
	}
	if x, ok := value.GetValue().(*otlpv11.AnyValue_BoolValue); ok {
		return strconv.FormatBool(x.BoolValue)
	}
	if x, ok := value.GetValue().(*otlpv11.AnyValue_IntValue); ok {
		return strconv.FormatInt(x.IntValue, 10)
	}
	if x, ok := value.GetValue().(*otlpv11.AnyValue_DoubleValue); ok {
		return strconv.FormatFloat(x.DoubleValue, 'E', -1, 64)
	}
	if x, ok := value.GetValue().(*otlpv11.AnyValue_BytesValue); ok {
		return reflectx.BytesToString(x.BytesValue)
	}
	// TODO AnyValue_ArrayValue & AnyValue_KvListValue
	return ""
}
