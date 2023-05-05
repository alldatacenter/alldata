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
	"errors"
	"net/url"
	"strings"

	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/interceptor"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/maps"
)

var (
	INVALID_MSP_ENV_ID    = errors.New("invalid erda.env.id tag")
	INVALID_MSP_ENV_TOKEN = errors.New("invalid erda.env.token tag")
	AUTHENTICATION_FAILED = errors.New("authentication failed, please use the correct accessKey and accessKeySecret")
)

type Interceptors interface {
	Authentication(next interceptor.Handler) interceptor.Handler

	SpanTagOverwrite(next interceptor.Handler) interceptor.Handler

	ExtractHttpHeaders(next interceptor.Handler) interceptor.Handler
}

type interceptorImpl struct {
	validator authentication.Validator
}

func (i *interceptorImpl) ExtractHttpHeaders(next interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, entity interface{}) (interface{}, error) {
		req := transhttp.ContextRequest(ctx)
		req.Header.Set("Accept", "application/json")

		ctxBaggage := transport.ContextHeader(ctx)

		if envId := req.Header.Get(HEADER_ERDA_ENV_ID); envId != "" {
			ctxBaggage.Set(HEADER_ERDA_ENV_ID, envId)
		}

		if token := req.Header.Get(HEADER_ERDA_ENV_TOKEN); token != "" {
			ctxBaggage.Set(HEADER_ERDA_ENV_TOKEN, token)
		}

		if orgName := req.Header.Get(HEADER_ERDA_ORG); orgName != "" {
			ctxBaggage.Set(HEADER_ERDA_ORG, orgName)
		}

		//if data, ok := entity.(*jaegerpb.PostSpansRequest); ok {
		//	ctx = common.WithSpans(ctx, data.Spans)
		//}
		return next(ctx, entity)
	}
}

func (i *interceptorImpl) SpanTagOverwrite(next interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, req interface{}) (interface{}, error) {
		envId := apis.GetHeader(ctx, HEADER_ERDA_ENV_ID)
		orgName := apis.GetHeader(ctx, HEADER_ERDA_ORG)
		if p, ok := req.(SpansProvider); ok && p.GetSpans() != nil {
			spans := p.GetSpans()
			for _, span := range spans {
				for k, v := range span.Attributes {
					key := k
					if idx := strings.Index(key, "."); idx > -1 {
						key = strings.Replace(k, ".", "_", -1)
						span.Attributes[key] = v
						delete(span.Attributes, k)
					}
					if idx := strings.Index(key, "erda_"); idx == 0 {
						span.Attributes[key[5:]] = v
						delete(span.Attributes, key)
					}
				}
				// env fields
				if _, ok := span.Attributes[TAG_ORG_NAME]; !ok {
					span.Attributes[TAG_ORG_NAME] = orgName
				}
				if _, ok := span.Attributes[TAG_ENV_ID]; !ok {
					span.Attributes[TAG_ENV_ID] = envId
				}
				if _, ok := span.Attributes[TAG_TERMINUS_KEY]; !ok {
					span.Attributes[TAG_TERMINUS_KEY] = span.Attributes[TAG_ENV_ID]
				}
				if _, ok := span.Attributes[TAG_SERVICE_ID]; !ok {
					span.Attributes[TAG_SERVICE_ID] = span.Attributes[TAG_SERVICE_NAME]
				}
				if ip, ok := span.Attributes[TAG_IP]; ok {
					span.Attributes[TAG_SERVICE_INSTANCE_IP] = ip
					delete(span.Attributes, TAG_IP)
				}
				if _, ok := span.Attributes[TAG_SERVICE_INSTANCE_ID]; !ok {
					if uuid, ok := span.Attributes[TAG_CLIENT_UUID]; ok {
						span.Attributes[TAG_SERVICE_INSTANCE_ID] = uuid
					}
				}
				// http fields
				if _, ok := span.Attributes[TAG_HTTP_PATH]; !ok {
					if httpURL, ok := span.Attributes[TAG_HTTP_URL]; ok {
						if u, err := url.Parse(httpURL); err == nil {
							span.Attributes[TAG_HTTP_PATH] = u.Path
						}
					} else if target, ok := span.Attributes[TAG_HTTP_TARGET]; ok {
						if u, err := url.Parse(target); err == nil {
							span.Attributes[TAG_HTTP_PATH] = u.Path
						}
					}
				}
				if _, ok := span.Attributes[TAG_HTTP_TARGET]; !ok {
					if path, ok := span.Attributes[TAG_HTTP_PATH]; ok {
						span.Attributes[TAG_HTTP_TARGET] = path
					}
				}
				// rpc fields
				// dubbo rpc service
				if dubboService, ok := span.Attributes[TAG_DUBBO_SERVICE]; ok {
					span.Attributes[TAG_RPC_SERVICE] = dubboService
					span.Attributes[TAG_RPC_METHOD] = span.Attributes[TAG_DUBBO_METHOD]
					span.Attributes[TAG_RPC_SYSTEM] = TAG_RPC_SYSTEM_DUBBO
					delete(span.Attributes, TAG_DUBBO_SERVICE)
					delete(span.Attributes, TAG_DUBBO_METHOD)
				}
				if _, ok := span.Attributes[TAG_RPC_TARGET]; !ok {
					if rpcService, ok := span.Attributes[TAG_RPC_SERVICE]; ok {
						span.Attributes[TAG_RPC_TARGET] = rpcService + "." + span.Attributes[TAG_RPC_METHOD]
					}
				}

				// cache and db fields
				if dbSystem, ok := span.Attributes[TAG_DB_SYSTEM]; ok {
					span.Attributes[TAG_DB_TYPE] = dbSystem
				} else if dbType, ok := span.Attributes[TAG_DB_TYPE]; ok {
					span.Attributes[TAG_DB_SYSTEM] = dbType
				}
				if _, ok := span.Attributes[TAG_DB_NAME]; !ok {
					if dbInstance, ok := span.Attributes[TAG_DB_INSTANCE]; ok {
						span.Attributes[TAG_DB_NAME] = dbInstance
						delete(span.Attributes, TAG_DB_INSTANCE)
					}
				}

				if _, ok := span.Attributes[TAG_SPAN_LAYER]; !ok {
					span.Attributes[TAG_SPAN_LAYER] = getSpanLayer(span.Attributes)
				}
				delete(span.Attributes, TAG_ENV_TOKEN)
				delete(span.Attributes, TAG_ERDA_ENV_TOKEN)
			}
		}
		return next(ctx, req)
	}
}

func getSpanLayer(attributes map[string]string) string {
	if maps.ContainsAnyKey(attributes, TAG_HTTP_PATH, TAG_HTTP_TARGET, TAG_HTTP_URL) {
		return TAG_SPAN_LAYER_HTTP
	}
	if maps.ContainsAnyKey(attributes, TAG_RPC_TARGET, TAG_RPC_SERVICE, TAG_RPC_METHOD, TAG_DUBBO_SERVICE, TAG_DUBBO_METHOD) {
		return TAG_SPAN_LAYER_RPC
	}
	if maps.ContainsAnyKey(attributes, TAG_MESSAGE_BUS_DESTINATION) {
		return TAG_SPAN_LAYER_MQ
	}
	if maps.ContainsAnyKey(attributes, TAG_DB_STATEMENT) {
		if dbType, ok := maps.GetByAnyKey(attributes, TAG_DB_SYSTEM, TAG_DB_TYPE); ok && strings.ToLower(dbType) == TAG_DB_TYPE_REDIS {
			return TAG_SPAN_LAYER_CACHE
		}
		return TAG_SPAN_LAYER_DB
	}
	return TAG_SPAN_LAYER_LOCAL
}

func (i *interceptorImpl) Authentication(next interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, req interface{}) (interface{}, error) {
		envId := apis.GetHeader(ctx, HEADER_ERDA_ENV_ID)
		token := apis.GetHeader(ctx, HEADER_ERDA_ENV_TOKEN)

		if envId == "" {
			return nil, INVALID_MSP_ENV_ID
		}
		if token == "" {
			return nil, INVALID_MSP_ENV_TOKEN
		}

		if !i.validator.Validate(SCOPE_MSP_ENV, envId, token) {
			return nil, AUTHENTICATION_FAILED
		}

		return next(ctx, req)
	}
}
