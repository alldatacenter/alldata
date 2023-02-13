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

// UnauthorizedError .
type UnauthorizedError struct {
	Reason string
}

var _ Error = (*UnauthorizedError)(nil)

func NewUnauthorizedError(reason string) *UnauthorizedError {
	return &UnauthorizedError{Reason: reason}
}

func (e *UnauthorizedError) Error() string {
	return fmt.Sprintf("unauthorized%s", suffixIfNotEmpty(":", e.Reason))
}
func (e *UnauthorizedError) HTTPStatus() int { return http.StatusUnauthorized }
func (e *UnauthorizedError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	return t.Sprintf(langs, "unauthorized%s", suffixIfNotEmpty(":", e.Reason))
}

// PermissionError .
type PermissionError struct {
	Resource string
	Action   string
	Reason   string
}

var _ Error = (*PermissionError)(nil)

// NewPermissionError .
func NewPermissionError(resource, action, reason string) *PermissionError {
	return &PermissionError{
		Resource: resource,
		Action:   action,
		Reason:   reason,
	}
}

func (e *PermissionError) Error() string {
	if len(e.Resource) > 0 {
		if len(e.Action) > 0 {
			return fmt.Sprintf("permission denied to %s %s%s", e.Action, e.Resource, suffixIfNotEmpty(":", e.Reason))
		}
		return fmt.Sprintf("permission denied to access %s%s", e.Resource, suffixIfNotEmpty(":", e.Reason))
	} else if len(e.Action) > 0 {
		return fmt.Sprintf("permission denied to %s%s", e.Action, suffixIfNotEmpty(":", e.Reason))
	}
	return "permission denied"
}
func (e *PermissionError) HTTPStatus() int { return http.StatusForbidden }
func (e *PermissionError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	if len(e.Resource) > 0 {
		if len(e.Action) > 0 {
			return t.Sprintf(langs, "${permission denied}: %s %s%s", t.Text(langs, e.Action), t.Text(langs, e.Resource), suffixIfNotEmpty(",", e.Reason))
		}
		return t.Sprintf(langs, "${permission denied}: %s%s", t.Text(langs, e.Resource), suffixIfNotEmpty(",", e.Reason))
	} else if len(e.Action) > 0 {
		return t.Sprintf(langs, "${permission denied}: %s%s", t.Text(langs, e.Action), suffixIfNotEmpty(",", e.Reason))
	}
	return t.Sprintf(langs, "permission denied%s", suffixIfNotEmpty(":", e.Reason))
}
