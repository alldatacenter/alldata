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

package i18n

import (
	"fmt"

	"github.com/erda-project/erda-infra/providers/i18n"
)

var (
	DefaultLocale = "en,zh-CN;q=0.9,zh;q=0.8,en-US;q=0.7,en-GB;q=0.6"

	translator      i18n.Translator
	defaultCodes, _ = i18n.ParseLanguageCode(DefaultLocale)
)

func SetSingle(trans i18n.Translator) {
	translator = trans
}

func Sprintf(locale, key string, args ...interface{}) string {
	if translator == nil {
		panic("the translator is nil")
	}
	codes, err := i18n.ParseLanguageCode(locale)
	if err != nil || len(codes) == 0 || locale == "" {
		codes = defaultCodes
	}
	if len(args) == 0 {
		return translator.Text(codes, key)
	}
	return fmt.Sprintf(translator.Text(codes, key), args...)
}
