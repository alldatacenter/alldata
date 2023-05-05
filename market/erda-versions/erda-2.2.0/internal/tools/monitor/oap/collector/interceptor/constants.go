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

package interceptor

import (
	"context"

	trace "github.com/erda-project/erda-proto-go/oap/trace/pb"
)

var (
	TAG_SERVICE_NAME            = "service_name"
	TAG_SERVICE_ID              = "service_id"
	TAG_SERVICE_INSTANCE_IP     = "service_instance_ip"
	TAG_SERVICE_INSTANCE_ID     = "service_instance_id"
	TAG_CLIENT_UUID             = "client_uuid"
	TAG_TERMINUS_KEY            = "terminus_key"
	TAG_IP                      = "ip"
	TAG_HTTP_PATH               = "http_path"
	TAG_HTTP_URL                = "http_url"
	TAG_HTTP_TARGET             = "http_target"
	TAG_INSTRUMENT              = "instrument"
	TAG_INSTRUMENT_VERSION      = "instrument_version"
	TAG_SPAN_KIND               = "span_kind"
	TAG_ORG_NAME                = "org_name"
	TAG_ENV_ID                  = "env_id"
	TAG_ENV_TOKEN               = "env_token"
	TAG_DUBBO_SERVICE           = "dubbo_service"
	TAG_DUBBO_METHOD            = "dubbo_method"
	TAG_RPC_SYSTEM              = "rpc_system"
	TAG_RPC_SERVICE             = "rpc_service"
	TAG_RPC_METHOD              = "rpc_method"
	TAG_RPC_TARGET              = "rpc_target"
	TAG_RPC_SYSTEM_DUBBO        = "dubbo"
	TAG_DB_SYSTEM               = "db_system"
	TAG_DB_TYPE                 = "db_type"
	TAG_DB_NAME                 = "db_name"
	TAG_DB_INSTANCE             = "db_instance"
	TAG_SPAN_LAYER              = "span_layer"
	TAG_SPAN_LAYER_HTTP         = "http"
	TAG_SPAN_LAYER_RPC          = "rpc"
	TAG_SPAN_LAYER_CACHE        = "cache"
	TAG_SPAN_LAYER_DB           = "db"
	TAG_SPAN_LAYER_MQ           = "mq"
	TAG_SPAN_LAYER_LOCAL        = "local"
	TAG_MESSAGE_BUS_DESTINATION = "message_bus_destination"
	TAG_DB_STATEMENT            = "db_statement"
	TAG_DB_TYPE_REDIS           = "redis"
	TAG_JAEGER                  = "jaeger"
	TAG_JAEGER_VERSION          = "jaeger_version"

	TAG_ERDA_ENV_ID    = "erda_env_id"
	TAG_ERDA_ENV_TOKEN = "erda_env_token"
	TAG_ERDA_ORG       = "erda_org"
	// C means compatible
	// Separator of tag key compatible with third-party protocols, such as opentracing, opentelemetry
	TAG_ERDA_ENV_ID_C    = "erda.env.id"
	TAG_ERDA_ENV_TOKEN_C = "erda.env.token"
	TAG_ERDA_ORG_C       = "erda.org"

	HEADER_ERDA_ENV_ID    = "x-erda-env-id"
	HEADER_ERDA_ENV_TOKEN = "x-erda-env-token"
	HEADER_ERDA_ORG       = "x-erda-org"

	SCOPE_MSP_ENV = "msp_env"
)

type spanKey struct{}

func WithSpans(ctx context.Context, spans []*trace.Span) context.Context {
	return context.WithValue(ctx, spanKey{}, spans)
}

func Spans(ctx context.Context) ([]*trace.Span, bool) {
	val, ok := ctx.Value(spanKey{}).([]*trace.Span)
	if !ok {
		return nil, false
	}
	return val, true
}
