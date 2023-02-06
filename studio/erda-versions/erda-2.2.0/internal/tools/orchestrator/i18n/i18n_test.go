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

package i18n_test

import (
	"fmt"
	"testing"

	providersI18n "github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
)

type mockTranslator struct {
}

func (mockTranslator) Get(lang providersI18n.LanguageCodes, key string, def string) string {
	return key + def
}

func (mockTranslator) Text(lang providersI18n.LanguageCodes, key string) string {
	return key
}

func (mockTranslator) Sprintf(lang providersI18n.LanguageCodes, key string, args ...interface{}) string {
	return fmt.Sprintf(key, args...)
}

func TestSprintf(t *testing.T) {
	i18n.SetSingle(new(mockTranslator))
	i18n.Sprintf("", "hello erda")
	i18n.Sprintf("", "hello %s", "erda")
}

func TestOrgSprintf(t *testing.T) {
	i18n.SetSingle(new(mockTranslator))
	i18n.OrgSprintf("0", "hello erda")
}

func TestInitI18N(t *testing.T) {
	i18n.InitI18N()
}
