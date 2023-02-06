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

// NotFoundError .
type NotFoundError struct {
	Resource string
}

var _ Error = (*NotFoundError)(nil)

// NewNotFoundError .
func NewNotFoundError(resource string) *NotFoundError {
	return &NotFoundError{Resource: resource}
}

func IsNotFoundError(err error) bool {
	return err != nil && strings.HasSuffix(err.Error(), "not found")
}

func (e *NotFoundError) Error() string {
	if len(e.Resource) > 0 {
		return fmt.Sprintf("%s not found", e.Resource)
	}
	return "not found"
}
func (e *NotFoundError) HTTPStatus() int { return http.StatusNotFound }
func (e *NotFoundError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	if len(e.Resource) > 0 {
		return t.Sprintf(langs, "%s ${not found}", t.Text(langs, e.Resource))
	}
	return t.Text(langs, "not found")
}

// AlreadyExistsError .
type AlreadyExistsError struct {
	Resource string
}

var _ Error = (*AlreadyExistsError)(nil)

// NewAlreadyExistsError .
func NewAlreadyExistsError(resource string) *AlreadyExistsError {
	return &AlreadyExistsError{Resource: resource}
}

func (e *AlreadyExistsError) Error() string {
	if len(e.Resource) > 0 {
		return fmt.Sprintf("%s already exists", e.Resource)
	}
	return "already exists"
}
func (e *AlreadyExistsError) HTTPStatus() int { return http.StatusConflict }
func (e *AlreadyExistsError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	if len(e.Resource) > 0 {
		return t.Sprintf(langs, "%s ${already exists}", t.Text(langs, e.Resource))
	}
	return t.Text(langs, "already exists")
}
