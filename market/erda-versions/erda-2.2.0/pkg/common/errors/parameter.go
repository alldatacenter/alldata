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
	"strings"

	"github.com/erda-project/erda-infra/providers/i18n"
)

// InvalidParameterError .
type InvalidParameterError struct {
	Name    string
	Message string
}

var _ Error = (*InvalidParameterError)(nil)

// NewInvalidParameterError .
func NewInvalidParameterError(name, message string) *InvalidParameterError {
	return &InvalidParameterError{
		Name:    name,
		Message: message,
	}
}

func (e *InvalidParameterError) Error() string {
	if len(e.Message) > 0 {
		return fmt.Sprintf("parameter %s invalid: %s", e.Name, e.Message)
	}
	return fmt.Sprintf("parameter %s invalid", e.Name)
}

func (e *InvalidParameterError) HTTPStatus() int { return http.StatusBadRequest }
func (e *InvalidParameterError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	if len(e.Message) > 0 {
		return t.Sprintf(langs, "${parameter} %s ${invalid}: %s", t.Text(langs, e.Name), t.Text(langs, e.Message))
	}
	return t.Sprintf(langs, "${parameter} %s ${invalid}", t.Text(langs, e.Name))
}

// MissingParameterError .
type MissingParameterError struct {
	Name string
}

var _ Error = (*MissingParameterError)(nil)

func NewMissingParameterError(name string) *MissingParameterError {
	return &MissingParameterError{Name: name}
}

func (e *MissingParameterError) Error() string {
	return fmt.Sprintf("parameter %s missing", e.Name)
}
func (e *MissingParameterError) HTTPStatus() int { return http.StatusBadRequest }
func (e *MissingParameterError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	return t.Sprintf(langs, "${parameter} %s ${missing}", t.Text(langs, e.Name))
}

// ParameterTypeError
type ParameterTypeError struct {
	Name      string
	ValidType string
}

var _ Error = (*ParameterTypeError)(nil)

// NewParameterTypeError .
func NewParameterTypeError(name string) *ParameterTypeError {
	return &ParameterTypeError{Name: name}
}

func (e *ParameterTypeError) Error() string {
	if len(e.ValidType) > 0 {
		return fmt.Sprintf("parameter %s want %s type", e.Name, e.ValidType)
	}
	return fmt.Sprintf("parameter %s type error", e.Name)
}
func (e *ParameterTypeError) HTTPStatus() int { return http.StatusBadRequest }
func (e *ParameterTypeError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	if len(e.ValidType) > 0 {
		return t.Sprintf(langs, "${parameter} %s ${want} %s ${type}", t.Text(langs, e.Name), t.Text(langs, e.ValidType))
	}
	return t.Sprintf(langs, "${parameter} %s ${type error}", t.Text(langs, e.Name))
}

// ParseValidateError
func ParseValidateError(err error) error {
	if err == nil {
		return err
	}
	msg := err.Error()
	if !strings.HasPrefix(msg, "invalid field ") {
		return err
	}
	idx := strings.Index(msg, ": ")
	if idx <= 0 {
		return err
	}
	field := msg[len("invalid field "):idx]
	msg = msg[idx+2:]
	if strings.Contains(msg, "message must exist") || strings.Contains(msg, "must not be an empty string") {
		return NewMissingParameterError(field)
	}
	return NewInvalidParameterError(field, msg)
}
