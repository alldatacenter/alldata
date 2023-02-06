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
	"fmt"
	"net/http"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/pkg/common"
)

// I18n set by common package
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

// ErrorCode .
type ErrorCode interface {
	Status() int
	Code() string
}

type codedError struct {
	status int
	code   string
}

// CodedError .
func CodedError(status int, code string) ErrorCode {
	return &codedError{
		status: status,
		code:   code,
	}
}

// Status .
func (e *codedError) Status() int { return e.status }

// Code .
func (e *codedError) Code() string { return e.code }

// ErrorMessage .
type ErrorMessage interface {
	Message(ctx httpserver.Context) string
}

// TemplatedError .
type TemplatedError struct {
	status int
	code   string
	fmt    string
	args   []interface{}
}

// Status .
func (e *TemplatedError) Status() int { return e.status }

// Code .
func (e *TemplatedError) Code() string { return e.code }

// Message .
func (e *TemplatedError) Message(ctx httpserver.Context) string {
	if I18n != nil {
		lang := Language(ctx.Request())
		t := I18n.Translator("apis")
		return t.Sprintf(lang, e.fmt, e.args...)
	}
	return fmt.Sprintf(e.fmt, e.args)
}

// Clone .
func (e *TemplatedError) Clone(args ...interface{}) *TemplatedError {
	return &TemplatedError{e.status, e.code, e.fmt, args}
}

// common errors
var (
	InvalidParameterError = &TemplatedError{http.StatusBadRequest, "Invalid Parameter", `${Invalid Parameter}: %s`, nil}
	MissingParameterError = &TemplatedError{http.StatusBadRequest, "MissingParameter", `${Missing Parameter}: %s`, nil}
	InvalidStateError     = &TemplatedError{http.StatusBadRequest, "InvalidState", `${Invalid State}: %s`, nil}
	NotLoginError         = &TemplatedError{http.StatusUnauthorized, "NotLogin", `${Not Login}`, nil}
	AccessDeniedError     = &TemplatedError{http.StatusUnauthorized, "AccessDenied", `${Access Denied}`, nil}
	NotFoundError         = &TemplatedError{http.StatusNotFound, "NotFound", `${Not Found}: %s`, nil}
	AlreadyExistsError    = &TemplatedError{http.StatusBadRequest, "AlreadyExists", `${Already Exists}: %s`, nil}
	InternalError         = &TemplatedError{http.StatusInternalServerError, "InternalError", `${Internal Error}: %s`, nil}
)

type errors uint8

// Errors .
const Errors errors = 0

// InvalidParameter .
func (e errors) InvalidParameter(err interface{}, ctx ...interface{}) *Response {
	return Failure(InvalidParameterError, InvalidParameterError.Clone(err), ctx...)
}

// MissingParameter .
func (e errors) MissingParameter(key string, ctx ...interface{}) *Response {
	return Failure(MissingParameterError, MissingParameterError.Clone(key), ctx...)
}

// InvalidState .
func (e errors) InvalidState(err interface{}, ctx ...interface{}) *Response {
	return Failure(InvalidStateError, InvalidStateError.Clone(err))
}

// NotLogin .
func (e errors) NotLogin(ctx ...interface{}) *Response {
	return Failure(NotLoginError, NotLoginError, ctx...)
}

// AccessDenied .
func (e errors) AccessDenied(ctx ...interface{}) *Response {
	return Failure(AccessDeniedError, AccessDeniedError, ctx...)
}

// NotFound .
func (e errors) NotFound(err interface{}, ctx ...interface{}) *Response {
	return Failure(NotFoundError, NotFoundError.Clone(err), ctx...)
}

// AlreadyExists .
func (e errors) AlreadyExists(err interface{}, ctx ...interface{}) *Response {
	return Failure(AlreadyExistsError, AlreadyExistsError.Clone(err), ctx...)
}

// Internal .
func (e errors) Internal(err interface{}, ctx ...interface{}) *Response {
	return Failure(InternalError, InternalError.Clone(err), ctx...)
}
