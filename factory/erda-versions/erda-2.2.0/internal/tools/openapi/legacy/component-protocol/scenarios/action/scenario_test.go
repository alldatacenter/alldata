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

package action

import (
	"context"
	"encoding/json"
	"net/http"
	"os"
	"testing"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	_ "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/action/components/actionForm"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/i18n"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

func rend(req *apistructs.ComponentProtocolRequest) (cont *apistructs.ComponentProtocolRequest, err error) {
	// bundle
	os.Setenv("DICEHUB_ADDR", "dicehub.default.svc.cluster.local:10000")
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*60),
			)),
		bundle.WithDiceHub(),
	}
	bdl := bundle.New(bundleOpts...)
	r := http.Request{}
	i18nPrinter := i18n.I18nPrinter(&r)
	ctxBdl := protocol.ContextBundle{
		Bdl:         bdl,
		I18nPrinter: i18nPrinter,
		InParams:    req.InParams,
	}
	ctx := context.Background()
	ctx1 := context.WithValue(ctx, protocol.GlobalInnerKeyCtxBundle.String(), ctxBdl)

	err = protocol.RunScenarioRender(ctx1, req)
	if err != nil {
		return
	}
	cont = req
	return
}

func TestStateInjectLess(t *testing.T) {
	str1 := "{\"key1\":\"value1\",\"key2\":\"value2\"}"
	type s1 struct {
		Key1 string `json:"key1"`
	}
	type s2 struct {
		Key1 string `json:"key1"`
		Key2 string `json:"key2"`
	}
	type s3 struct {
		Key1 string `json:"key1"`
		Key2 string `json:"key2"`
		Key3 string `json:"key3"`
	}

	t1 := s1{}
	t2 := s2{}
	t3 := s3{}

	err := json.Unmarshal([]byte(str1), &t1)
	if err != nil {
		t.Logf("unmarshal str1 to t1 failed, err:%v", err)
	}
	err = json.Unmarshal([]byte(str1), &t2)
	if err != nil {
		t.Logf("unmarshal str1 to t2 failed, err:%v", err)
	}
	err = json.Unmarshal([]byte(str1), &t3)
	if err != nil {
		t.Logf("unmarshal str1 to t3 failed, err:%v", err)
	}
}
