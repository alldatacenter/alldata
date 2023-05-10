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

package endpoints

import (
	"context"
	"net/http"

	"golang.org/x/text/language"
	"golang.org/x/text/message"

	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

type endpoint func(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error)

func auth(f httpserver.Handler) httpserver.Handler {
	return func(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
		// TODO: auth
		return f(ctx, r, vars)
	}
}

func i18nPrinter(f httpserver.Handler) httpserver.Handler {
	return func(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
		lang := r.Header.Get("Lang")
		p := message.NewPrinter(language.English)
		if strutil.Contains(lang, "zh") {
			p = message.NewPrinter(language.SimplifiedChinese)
		}
		ctx2 := context.WithValue(ctx, "i18nPrinter", p)
		return f(ctx2, r, vars)
	}
}
