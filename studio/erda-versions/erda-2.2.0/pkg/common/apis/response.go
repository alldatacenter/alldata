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

package apis

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"reflect"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transgrpc "github.com/erda-project/erda-infra/pkg/transport/grpc"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	"github.com/erda-project/erda-infra/pkg/transport/interceptor"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/pkg/common"
	"github.com/erda-project/erda/pkg/common/errors"
)

// Response .
type Response struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data,omitempty"`
	UserIDs []string    `json:"userIDs,omitempty"`
	Err     *Error      `json:"err,omitempty"`
}

var _ error = (*Error)(nil)

// Error .
type Error struct {
	Code interface{} `json:"code,omitempty"`
	Msg  interface{} `json:"msg,omitempty"`
	Ctx  interface{} `json:"ctx,omitempty"`
}

func (s *Error) Error() string {
	if s.Ctx != nil {
		return fmt.Sprintf("{code: %s, msg: %s, ctx: %s}", s.Code, s.Msg, s.Ctx)
	}
	return fmt.Sprintf("{code: %s, msg: %s}", s.Code, s.Msg)
}

// I18n .
var I18n i18n.I18n

func init() {
	common.RegisterHubListener(&servicehub.DefaultListener{
		BeforeInitFunc: func(h *servicehub.Hub, config map[string]interface{}) error {
			if _, ok := config["i18n"]; !ok {
				config["i18n"] = nil // i18n is required
			}
			return nil
		},
		AfterInitFunc: func(h *servicehub.Hub) error {
			I18n = h.Service("i18n").(i18n.I18n)
			return nil
		},
	})
}

func encodeError(w http.ResponseWriter, r *http.Request, err error) {
	var status int
	if e, ok := err.(transhttp.Error); ok {
		status = e.HTTPStatus()
	} else {
		if _, ok := err.(ValidationError); ok {
			status = http.StatusBadRequest
		} else {
			status = http.StatusInternalServerError
		}
	}
	var msg string
	if e, ok := err.(i18n.Internationalizable); I18n != nil && ok {
		msg = e.Translate(I18n.Translator("apis"), HTTPLanguage(r))
	} else {
		msg = err.Error()
	}
	w = &responseWriter{status: status, ResponseWriter: w}

	resp := &Response{
		Success: false,
		Err: &Error{
			Code: strconv.Itoa(status),
			Msg:  msg,
			Ctx:  r.URL.String(),
		},
	}
	err = encoding.EncodeResponse(w, r, resp)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		byts, _ := json.Marshal(resp)
		w.Write(byts)
	}
}

type responseWriter struct {
	status int
	http.ResponseWriter
}

func (w *responseWriter) Write(b []byte) (int, error) {
	w.ResponseWriter.WriteHeader(w.status)
	return w.ResponseWriter.Write(b)
}

func wrapGrpcError(h interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, req interface{}) (interface{}, error) {
		resp, err := h(ctx, req)
		if err != nil {
			err = errors.ToGrpcError(err)
		}
		return resp, err
	}
}

func wrapResponse(h interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, req interface{}) (interface{}, error) {
		resp, err := h(ctx, req)
		if err != nil {
			return resp, err
		}
		var userIDs []string
		if resp != nil {
			val := reflect.ValueOf(resp)
			for val.Kind() == reflect.Ptr {
				val = val.Elem()
			}
			if val.Kind() == reflect.Struct {
				var fields int
				for i, n := 0, val.NumField(); i < n; i++ {
					field := val.Field(i)
					if field.CanSet() {
						fields++
					}
				}
				switch fields {
				case 1:
					if field := val.FieldByName("Data"); field.IsValid() {
						resp = field.Interface()
					} else if field := val.FieldByName("UserIDs"); field.IsValid() && field.Kind() == reflect.Slice && field.Type().Elem().Kind() == reflect.String {
						resp = nil
						userIDs = field.Interface().([]string)
					}
				case 2:
					field1 := val.FieldByName("Data")
					if field1.IsValid() {
						field2 := val.FieldByName("UserIDs")
						if field2.IsValid() && field2.Kind() == reflect.Slice && field2.Type().Elem().Kind() == reflect.String {
							resp = field1.Interface()
							userIDs = field2.Interface().([]string)
						}
					}
				}
			}
		}
		return &Response{
			Success: true,
			Data:    resp,
			UserIDs: userIDs,
		}, nil
	}
}

// Define validator and error interface as all related struct in protoc-gen-validate are defined as text templates.
// i.e. https://github.com/envoyproxy/protoc-gen-validate/blob/main/templates/go/message.go
type Validator interface {
	Validate() error
}

type ValidationError interface {
	Field() string
	Reason() string
	Key() bool
	Cause() error
	ErrorName() string
}

func validRequest(h interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, req interface{}) (interface{}, error) {
		if v, ok := req.(Validator); ok {
			err := v.Validate()
			if err != nil {
				return nil, err
			}
		}
		return h(ctx, req)
	}
}

// Options .
func Options() transport.ServiceOption {
	return transport.ServiceOption(func(opts *transport.ServiceOptions) {
		transport.WithInterceptors(validRequest)(opts)
		transport.WithHTTPOptions(transhttp.WithInterceptor(wrapResponse))(opts)
		transport.WithHTTPOptions(transhttp.WithErrorEncoder(encodeError))(opts)
		opts.GRPC = append(opts.GRPC, transgrpc.WithInterceptor(wrapGrpcError))
	})
}
