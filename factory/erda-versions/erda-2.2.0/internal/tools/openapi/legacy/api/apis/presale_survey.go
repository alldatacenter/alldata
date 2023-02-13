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

package apis

import (
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
)

var PRESALE_SURVEY = ApiSpec{
	Path:   "/api/survey/login",
	Scheme: "http",
	Method: "GET",
	Doc:    "summary: 售前调查 API（伪装成登录接口）",
	Custom: func(rw http.ResponseWriter, req *http.Request) {
		bdl := bundle.New(bundle.WithCoreServices())
		f := req.URL.Query()
		content := ""
		content += "真实姓名　　: " + f.Get("realname") + "\n" +
			"手机号码　　: " + f.Get("mobile") + "\n" +
			"企业邮箱地址: " + f.Get("email") + "\n" +
			"所处职位　　: " + f.Get("position") + "\n" +
			"企业名称　　: " + f.Get("company") + "\n" +
			"企业规模　　: " + f.Get("company_size") + "\n" +
			"IT部门规模　: " + f.Get("it_size") + "\n" +
			"申请目的　　: " + f.Get("purpose") + "\n"
		msg := apistructs.MessageCreateRequest{
			Sender: "survey",
			Labels: map[apistructs.MessageLabel]interface{}{
				apistructs.DingdingLabel: []apistructs.Target{{Receiver: conf.SurveyDingding(), Secret: ""}},
			},
			Content: content,
		}
		if err := bdl.CreateMessage(&msg); err != nil {
			logrus.Warnf("failed to POST survey, err: %v", err)
			rw.Header().Set("Content-Type", "application/json")
			rw.WriteHeader(500)
			rw.Write([]byte(`{"success":false,"err":{"msg":"失败"}}`))
			return
		}
		rw.Header().Set("Content-Type", "application/json")
		rw.WriteHeader(200)
		rw.Write([]byte(`{"success":true}`))
	},
}
