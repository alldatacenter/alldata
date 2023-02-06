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
	"encoding/json"
	"strings"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
)

type SpanTree map[string]*pb.Span
type ConditionType string

const (
	INPUT ConditionType = "input"
)

const (
	JavaMemoryMetricName   = "jvm_memory"
	NodeJsMemoryMetricName = "nodejs_memory"
)

var ProcessMetrics = []string{
	JavaMemoryMetricName,
	NodeJsMemoryMetricName,
}

var sortConditions = []*pb.TraceQueryCondition{
	{Key: strings.ToLower(pb.SortCondition_TRACE_TIME_DESC.String()), Value: strings.ToLower(pb.SortCondition_TRACE_TIME_DESC.String())},
	{Key: strings.ToLower(pb.SortCondition_TRACE_TIME_ASC.String()), Value: strings.ToLower(pb.SortCondition_TRACE_TIME_ASC.String())},
	{Key: strings.ToLower(pb.SortCondition_SPAN_COUNT_DESC.String()), Value: strings.ToLower(pb.SortCondition_SPAN_COUNT_DESC.String())},
	{Key: strings.ToLower(pb.SortCondition_SPAN_COUNT_ASC.String()), Value: strings.ToLower(pb.SortCondition_SPAN_COUNT_ASC.String())},
	{Key: strings.ToLower(pb.SortCondition_TRACE_DURATION_DESC.String()), Value: strings.ToLower(pb.SortCondition_TRACE_DURATION_DESC.String())},
	{Key: strings.ToLower(pb.SortCondition_TRACE_DURATION_ASC.String()), Value: strings.ToLower(pb.SortCondition_TRACE_DURATION_ASC.String())},
}

var TraceStatusConditions = []*pb.TraceQueryCondition{
	{Key: strings.ToLower(pb.TraceStatusCondition_TRACE_ALL.String()), Value: strings.ToLower(pb.TraceStatusCondition_TRACE_ALL.String())},
	{Key: strings.ToLower(pb.TraceStatusCondition_TRACE_SUCCESS.String()), Value: strings.ToLower(pb.TraceStatusCondition_TRACE_SUCCESS.String())},
	{Key: strings.ToLower(pb.TraceStatusCondition_TRACE_ERROR.String()), Value: strings.ToLower(pb.TraceStatusCondition_TRACE_ERROR.String())},
}

var TraceQueryConditions = pb.TraceQueryConditions{
	Sort:        sortConditions,
	TraceStatus: TraceStatusConditions,
	Others: []*pb.OtherTraceQueryCondition{
		{Key: strings.ToLower(pb.OtherCondition_SERVICE_NAME.String()), ParamKey: "serviceName", Type: string(INPUT)},
		{Key: strings.ToLower(pb.OtherCondition_TRACE_ID.String()), ParamKey: "traceID", Type: string(INPUT)},
		{Key: strings.ToLower(pb.OtherCondition_RPC_METHOD.String()), ParamKey: "rpcMethod", Type: string(INPUT)},
		{Key: strings.ToLower(pb.OtherCondition_HTTP_PATH.String()), ParamKey: "httpPath", Type: string(INPUT)},
	},
}

func TranslateCondition(i18n i18n.Translator, lang i18n.LanguageCodes, key string) string {
	if lang == nil {
		return key
	}
	return i18n.Text(lang, strings.ToLower(key))
}

func DepthCopyQueryConditions() *pb.TraceQueryConditions {
	conditions, err := clone(&TraceQueryConditions)
	if err != nil {
		return nil
	}
	return conditions
}

func clone(src *pb.TraceQueryConditions) (*pb.TraceQueryConditions, error) {
	var dst pb.TraceQueryConditions
	buffer, _ := json.Marshal(&src)
	err := json.Unmarshal(buffer, &dst)
	if err != nil {
		return nil, err
	}
	return &dst, nil
}
