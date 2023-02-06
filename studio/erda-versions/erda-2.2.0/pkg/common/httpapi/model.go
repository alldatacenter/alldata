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

package api

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"strconv"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/i18n"
)

// baseResponse implement httpserver.Response interface
type baseResponse struct {
	status int
	body   interface{}
}

func (r *baseResponse) Error(httpserver.Context) error { return nil }
func (r *baseResponse) Status(httpserver.Context) int  { return r.status }
func (r *baseResponse) Body() interface{}              { return r.body }
func (r *baseResponse) ReadCloser(httpserver.Context) io.ReadCloser {
	byts, err := json.Marshal(r.body)
	if err != nil {
		byts, _ = json.Marshal(&Response{
			Success: false,
			Err: &Error{
				Code: InternalError.code,
				Msg:  err.Error(),
			},
		})
		r.status = InternalError.status
	}
	return ioutil.NopCloser(bytes.NewBuffer(byts))
}

func setContentType(ctx httpserver.Context) {
	header := ctx.ResponseWriter().Header()
	if header.Get("Content-Type") == "" {
		header.Set("Content-Type", "application/json; charset=UTF-8")
	}
}

// Response .
type Response struct {
	baseResponse
	Success bool        `json:"success,omitempty"`
	Data    interface{} `json:"data,omitempty"`
	Err     *Error      `json:"err,omitempty"`
}

// Response .
func (r *Response) Response(ctx httpserver.Context) httpserver.Response {
	if r.Err != nil {
		errMsg, ok := r.Err.Msg.(ErrorMessage)
		if ok {
			r.Err.Msg = errMsg.Message(ctx)
		}
	}
	r.baseResponse.body = r
	setContentType(ctx)
	return r
}

// RawResponse .
type RawResponse struct {
	baseResponse
}

// Response .
func (r *RawResponse) Response(ctx httpserver.Context) httpserver.Response {
	setContentType(ctx)
	return r
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
		return fmt.Sprintf("code: %s, msg: %s, ctx: %s", s.Code, s.Msg, s.Ctx)
	}
	return fmt.Sprintf("code: %s, msg: %s", s.Code, s.Msg)
}

// Success .
func Success(data interface{}, status ...int) httpserver.ResponseGetter {
	var statusCode int
	if len(status) > 0 {
		statusCode = status[0]
	}
	return &Response{
		baseResponse: baseResponse{
			status: statusCode,
		},
		Success: true,
		Data:    data,
	}
}

// SuccessRaw .
func SuccessRaw(data interface{}, status ...int) httpserver.ResponseGetter {
	var statusCode int
	if len(status) > 0 {
		statusCode = status[0]
	}
	return &RawResponse{
		baseResponse: baseResponse{
			status: statusCode,
			body:   data,
		},
	}
}

// Failure code、msg、ctx
func Failure(code, msg interface{}, ctx ...interface{}) *Response {
	// error code
	var (
		codeText string
		status   int
	)
	switch val := code.(type) {
	case ErrorCode:
		codeText = val.Code()
		status = val.Status()
	case string:
		codeText = val
	default:
		codeText = fmt.Sprint(code)
	}

	// error message
	var message interface{}
	switch val := msg.(type) {
	case ErrorMessage:
		message = val
	case error:
		message = val.Error()
	case string:
		message = val
	default:
		message = fmt.Sprint(msg)
	}

	// error context
	var context interface{}
	if len(ctx) > 0 {
		context = ctx[0]
	}
	return &Response{
		Success: false,
		Err: &Error{
			Code: codeText,
			Msg:  message,
			Ctx:  context,
		},
		baseResponse: baseResponse{
			status: status,
		},
	}
}

// Language .
func Language(r *http.Request) i18n.LanguageCodes {
	lang := r.Header.Get("Lang")
	if len(lang) <= 0 {
		lang = r.Header.Get("Accept-Language")
	}
	langs, _ := i18n.ParseLanguageCode(lang)
	return langs
}

// OrgID .
func OrgID(r *http.Request) string {
	return r.Header.Get("Org-ID")
}

// UserID .
func UserID(r *http.Request) string {
	return r.Header.Get("User-ID")
}

// OrgName .
func OrgName(r *http.Request) string {
	return r.Header.Get("org")
}

// OrgIDInt .
func OrgIDInt(r *http.Request) (int64, *Response) {
	id, err := strconv.ParseInt(OrgID(r), 10, 64)
	if err != nil {
		return 0, Errors.InvalidParameter("invalid Org-ID")
	}
	return id, nil
}

// UserIDInt .
func UserIDInt(r *http.Request) (int64, *Response) {
	id, err := strconv.ParseInt(UserID(r), 10, 64)
	if err != nil {
		return 0, Errors.InvalidParameter("invalid User-ID")
	}
	return id, nil
}
