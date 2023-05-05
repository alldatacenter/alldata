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
	"context"
	"net/http"

	"github.com/erda-project/erda/pkg/goroutine_context"
)

const ZH = "zh-CN"
const EN = "en-US"

const LangHeader = "Lang"

// GetLocaleNameByRequest 从request获取语言名称
func GetLocaleNameByRequest(request *http.Request) string {
	// 优先querystring 其次header
	lang := request.URL.Query().Get("lang")
	if lang != "" {
		return lang
	}
	lang = request.Header.Get(LangHeader)
	if lang != "" {
		return lang
	}
	return ""
}

func GetGoroutineBindLang() string {
	globalContext := goroutine_context.GetContext()
	if globalContext == nil {
		return ""
	}
	key := globalContext.Value(goroutine_context.LocaleNameContextKey)
	if key == nil {
		return ""
	}
	localeName, ok := key.(string)
	if !ok {
		return ""
	}

	return localeName
}

func SetGoroutineBindLang(localeName string) {
	ctx := goroutine_context.GetContext()
	if ctx == nil {
		ctx = context.Background()
	}

	goroutine_context.SetContext(context.WithValue(ctx, goroutine_context.LocaleNameContextKey, localeName))
}
