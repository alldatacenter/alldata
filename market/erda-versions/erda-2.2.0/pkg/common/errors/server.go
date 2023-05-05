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

package errors

import (
	"fmt"
	"net/http"

	"github.com/erda-project/erda-infra/providers/i18n"
)

// InternalServerError .
type InternalServerError struct {
	Cause   error
	Message string
}

// NewInternalServerError .
func NewInternalServerError(err error) *InternalServerError {
	return &InternalServerError{Cause: err}
}

func NewInternalServerErrorMessage(message string) *InternalServerError {
	return &InternalServerError{Message: message}
}

func (e *InternalServerError) Error() string {
	return fmt.Sprintf("internal error: %s", e.Cause)
}
func (e *InternalServerError) HTTPStatus() int { return http.StatusInternalServerError }
func (e *InternalServerError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	return t.Sprintf(langs, "${internal} ${error}: %s", e.Cause)
}

// DatabaseError .
type DatabaseError struct {
	Cause error
}

// NewDatabaseError .
func NewDatabaseError(err error) *DatabaseError {
	return &DatabaseError{Cause: err}
}

func (e *DatabaseError) Error() string {
	return fmt.Sprintf("database error: %s", e.Cause)
}
func (e *DatabaseError) HTTPStatus() int { return http.StatusInternalServerError }
func (e *DatabaseError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	return t.Sprintf(langs, "${database} ${error}: %s", e.Cause)
}

// ServiceInvokingError .
type ServiceInvokingError struct {
	Service string
	Cause   error
}

// NewServiceInvokingError .
func NewServiceInvokingError(service string, err error) *ServiceInvokingError {
	return &ServiceInvokingError{
		Service: service,
		Cause:   err,
	}
}

func (e *ServiceInvokingError) Error() string {
	return fmt.Sprintf("service %s error: %s", e.Service, e.Cause)
}
func (e *ServiceInvokingError) HTTPStatus() int { return http.StatusBadGateway }
func (e *ServiceInvokingError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	return t.Sprintf(langs, "${service} %s ${error}: %s", t.Text(langs, e.Service), e.Cause)
}

// UnimplementedError .
type UnimplementedError struct {
	Service string
}

// NewUnimplementedError .
func NewUnimplementedError(service string) *UnimplementedError {
	return &UnimplementedError{Service: service}
}

func (e *UnimplementedError) Error() string {
	return fmt.Sprintf("service %s not implemented", e.Service)
}
func (e *UnimplementedError) HTTPStatus() int { return http.StatusNotImplemented }
func (e *UnimplementedError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	return t.Sprintf(langs, "${service} %s ${not implemented}", t.Text(langs, e.Service))
}
