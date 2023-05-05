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

// WarnError warn error
type WarnError struct {
	Name    string
	Message string
}

var _ Error = (*WarnError)(nil)

// NewWarnError init warn error
func NewWarnError(message string) *WarnError {
	return &WarnError{
		Message: message,
	}
}

func (e *WarnError) Error() string {
	if len(e.Message) > 0 {
		return fmt.Sprintf(e.Message)
	}
	return fmt.Sprintf(e.Name)
}

func (e *WarnError) HTTPStatus() int { return http.StatusInternalServerError }

func (e *WarnError) Translate(t i18n.Translator, langs i18n.LanguageCodes) string {
	if len(e.Message) > 0 {
		return t.Sprintf(langs, t.Text(langs, e.Message))
	}
	return t.Sprintf(langs, t.Text(langs, e.Name))
}
