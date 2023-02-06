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

package logic

import (
	"context"
	"reflect"

	"github.com/erda-project/erda/apistructs"
)

func addLineDelimiter(ctx context.Context, prefix ...string) {
	log := clog(ctx)

	var _prefix string
	if len(prefix) > 0 {
		_prefix = prefix[0]
	}
	log.Printf("%s==========", _prefix)
}

func addNewLine(ctx context.Context, num ...int) {
	log := clog(ctx)

	_num := 1
	if len(num) > 0 {
		_num = num[0]
	}
	if _num <= 0 {
		_num = 1
	}
	for i := 0; i < _num; i++ {
		log.Println()
	}
}

func printOriginalAPIInfo(ctx context.Context, api *apistructs.APIInfo) {
	log := clog(ctx)

	if api == nil {
		return
	}
	log.Printf("Original API Info:")
	defer addNewLine(ctx)
	// name
	if api.Name != "" {
		log.Printf("name: %s", api.Name)
	}
	// url
	log.Printf("url: %s", api.URL)
	// method
	log.Printf("method: %s", api.Method)
	// headers
	if len(api.Headers) > 0 {
		log.Printf("headers:")
		for _, h := range api.Headers {
			log.Printf("  key: %s", h.Key)
			log.Printf("  value: %s", h.Value)
			if h.Desc != "" {
				log.Printf("  desc: %s", h.Desc)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
	// params
	if len(api.Params) > 0 {
		log.Printf("params:")
		for _, p := range api.Params {
			log.Printf("  key: %s", p.Key)
			log.Printf("  value: %s", p.Value)
			if p.Desc != "" {
				log.Printf("  desc: %s", p.Desc)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
	// request body
	if api.Body.Type != "" {
		log.Printf("request body:")
		log.Printf("  type: %s", api.Body.Type.String())
		log.Printf("  content: %s", jsonOneLine(ctx, api.Body.Content))
	}
	// out params
	if len(api.OutParams) > 0 {
		log.Printf("out params:")
		for _, out := range api.OutParams {
			log.Printf("  arg: %s", out.Key)
			log.Printf("  source: %s", out.Source.String())
			if out.Expression != "" {
				log.Printf("  expr: %s", out.Expression)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
	// asserts
	if len(api.Asserts) > 0 {
		log.Printf("asserts:")
		for _, group := range api.Asserts {
			for _, assert := range group {
				log.Printf("  key: %s", assert.Arg)
				log.Printf("  operator: %s", assert.Operator)
				log.Printf("  value: %s", assert.Value)
				addLineDelimiter(ctx, "  ")
			}
		}
	}
}

func printGlobalAPIConfig(ctx context.Context, cfg *apistructs.APITestEnvData) {
	log := clog(ctx)

	if cfg == nil {
		return
	}
	log.Printf("Global API Config:")
	defer addNewLine(ctx)

	// name
	if cfg.Name != "" {
		log.Printf("name: %s", cfg.Name)
	}
	// domain
	log.Printf("domain: %s", cfg.Domain)
	// headers
	if len(cfg.Header) > 0 {
		log.Printf("headers:")
		for k, v := range cfg.Header {
			log.Printf("  key: %s", k)
			log.Printf("  value: %s", v)
			addLineDelimiter(ctx, "  ")
		}
	}
	// global
	if len(cfg.Global) > 0 {
		log.Printf("global configs:")
		for key, item := range cfg.Global {
			log.Printf("  key: %s", key)
			log.Printf("  value: %s", item.Value)
			log.Printf("  type: %s", item.Type)
			if item.Desc != "" {
				log.Printf("  desc: %s", item.Desc)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
}

func printRenderedHTTPReq(ctx context.Context, req *apistructs.APIRequestInfo) {
	log := clog(ctx)

	if req == nil {
		return
	}
	log.Printf("Rendered HTTP Request:")
	defer addNewLine(ctx)

	// url
	log.Printf("url: %s", req.URL)
	// method
	log.Printf("method: %s", req.Method)
	// headers
	if len(req.Headers) > 0 {
		log.Printf("headers:")
		for key, values := range req.Headers {
			log.Printf("  key: %s", key)
			if len(values) == 1 {
				log.Printf("  value: %s", values[0])
			} else {
				log.Printf("  values: %v", values)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
	// params
	if len(req.Params) > 0 {
		log.Printf("params:")
		for key, values := range req.Params {
			log.Printf("  key: %s", key)
			if len(values) == 1 {
				log.Printf("  value: %s", values[0])
			} else {
				log.Printf("  values: %v", values)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
	// body
	if req.Body.Type != "" {
		log.Printf("request body:")
		log.Printf("  type: %s", req.Body.Type.String())
		log.Printf("  content: %s", req.Body.Content)
	}
}

func printHTTPResp(ctx context.Context, resp *apistructs.APIResp) {
	log := clog(ctx)

	if resp == nil {
		return
	}
	log.Printf("HTTP Response:")
	defer addNewLine(ctx)

	// status
	log.Printf("http status: %d", resp.Status)
	// headers
	if len(resp.Headers) > 0 {
		log.Printf("response headers:")
		for key, values := range resp.Headers {
			log.Printf("  key: %s", key)
			if len(values) == 1 {
				log.Printf("  value: %s", values[0])
			} else {
				log.Printf("  values: %v", values)
			}
			addLineDelimiter(ctx, "  ")
		}
	}
	// response body
	if resp.BodyStr != "" {
		log.Printf("response body: %s", resp.BodyStr)
	}
}

func printOutParams(ctx context.Context, outParams map[string]interface{}, meta *Meta) {
	log := clog(ctx)

	if len(outParams) == 0 {
		return
	}
	log.Printf("Out Params:")
	defer addNewLine(ctx)

	// 按定义顺序返回
	for _, define := range meta.OutParamsDefine {
		k := define.Key
		v, ok := outParams[k]
		if !ok {
			continue
		}
		meta.OutParamsResult[k] = v
		log.Printf("  arg: %s", k)
		log.Printf("  source: %s", define.Source.String())
		if define.Expression != "" {
			log.Printf("  expr: %s", define.Expression)
		}
		log.Printf("  value: %s", jsonOneLine(ctx, v))
		var vtype string
		if v == nil {
			vtype = "nil"
		} else {
			vtype = reflect.TypeOf(v).String()
		}
		log.Printf("  type: %s", vtype)
		addLineDelimiter(ctx, "  ")
	}
}

func printAssertResults(ctx context.Context, success bool, results []*apistructs.APITestsAssertData) {
	log := clog(ctx)

	log.Printf("Assert Result: %t", success)
	defer addNewLine(ctx)

	log.Printf("Assert Detail:")
	for _, result := range results {
		log.Printf("  arg: %s", result.Arg)
		log.Printf("  operator: %s", result.Operator)
		log.Printf("  value: %s", result.Value)
		log.Printf("  actualValue: %s", jsonOneLine(ctx, result.ActualValue))
		log.Printf("  success: %t", result.Success)
		if result.ErrorInfo != "" {
			log.Printf("  errorInfo: %s", result.ErrorInfo)
		}
		addLineDelimiter(ctx, "  ")
	}
}
