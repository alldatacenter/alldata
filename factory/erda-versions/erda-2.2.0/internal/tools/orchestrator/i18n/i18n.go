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

	"golang.org/x/text/language"
	"golang.org/x/text/message"

	"github.com/erda-project/erda-infra/providers/i18n"
	orgCache "github.com/erda-project/erda/internal/tools/orchestrator/cache/org"
)

var (
	DefaultLocale = "en,zh-CN;q=0.9,zh;q=0.8,en-US;q=0.7,en-GB;q=0.6"

	translator      i18n.Translator
	defaultCodes, _ = i18n.ParseLanguageCode(DefaultLocale)
)

func InitI18N() {
	message.SetString(language.SimplifiedChinese, "ImagePullFailed", "拉取镜像失败")
	message.SetString(language.SimplifiedChinese, "Unschedulable", "调度失败")
	message.SetString(language.SimplifiedChinese, "InsufficientResources", "资源不足")
	message.SetString(language.SimplifiedChinese, "ProbeFailed", "健康检查失败")
	message.SetString(language.SimplifiedChinese, "ContainerCannotRun", "容器无法启动")
}

func SetSingle(trans i18n.Translator) {
	translator = trans
}

func LangCodesSprintf(codes i18n.LanguageCodes, key string, args ...interface{}) string {
	if len(args) == 0 {
		return translator.Text(codes, key)
	}
	return fmt.Sprintf(translator.Text(codes, key), args...)
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

func OrgSprintf(orgID, key string, args ...interface{}) string {
	var locale = DefaultLocale
	if orgDTO, ok := orgCache.GetOrgByOrgID(orgID); ok {
		locale = orgDTO.Locale
	}
	return Sprintf(locale, key, args...)
}
