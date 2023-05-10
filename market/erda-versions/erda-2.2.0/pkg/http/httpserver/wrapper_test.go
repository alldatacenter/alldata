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

package httpserver_test

import (
	"context"
	"testing"

	i18nProviders "github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func TestUnwrapI18nCodes(t *testing.T) {
	codes := httpserver.UnwrapI18nCodes(context.Background())
	if len(codes) == 0 {
		t.Fatal("there should be codes")
	}
	t.Log(codes)
	var lang struct{ Lang string }
	codes, _ = i18nProviders.ParseLanguageCode("en,zh-CN;q=0.9,zh;q=0.8,en-US;q=0.7,en-GB;q=0.6")
	ctx := context.WithValue(context.Background(), lang, codes)
	codes = httpserver.UnwrapI18nCodes(ctx)
	t.Log(len(codes))
	if len(codes) == 0 {
		t.Fatal("there should be codes")
	}
}
