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

package apiEditor

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/pkg/autotest/step"
	"github.com/erda-project/erda/pkg/expression"
)

//[
//   {
//     "label": "前置场景",
//     "value": "$alias1.params1",
//     "isLeaf": false,
//     "children": [
//       {
//         "label": "场景A",
//         "value": "cjA",
//         "isLeaf": false
//       },
//       {
//         "label": "场景B",
//         "value": "cjB",
//         "isLeaf": false
//       }
//     ]
//   },
//   {
//     "label": "全局参数",
//     "value": "$alias1.params2",
//     "isLeaf": true
//   }
// ]

//apiExecute: {
//  text: '执行',
//  type: 'primary',
//  disabled: true,
//  allowSave: true,
//  menu: [
//	{
//	  text: '开发环境',
//	  key: 'dev',
//	  operations: {
//		click: { key: 'execute', reload: true, meta: { env: 'dev' } },
//	  },
//	},
//	{
//	  text: '测试环境',
//	  key: 'test',
//	  operations: {
//		click: { key: 'execute', reload: true, meta: { env: 'test' } },
//	  },
//	},
//  ],
//}
//}
const props4 string = `}`

type optionKey string

// placeholder for whether the loop strategy is expanded by default
const LoopFormFieldDefaultExpand = optionKey("defaultExpand")

func (opt optionKey) string() string {
	return "{{_" + string(opt) + "_}}"
}

type replaceOption struct {
	key   optionKey
	value string
}

var defaultReplaceOptions = []replaceOption{
	{
		key:   LoopFormFieldDefaultExpand,
		value: "false",
	},
}

func (ae *ApiEditor) genProps(input, execute string, replaceOpts ...replaceOption) cptype.ComponentProps {
	var props1 = fmt.Sprintf(`{
    "loopFormField":[
     {
       "component":"formGroup",
       "key":"loop",
       "componentProps":{
         "defaultExpand": `+LoopFormFieldDefaultExpand.string()+`,
         "expandable":true,
         "title":"%s"
       },
       "group":"loop"
     },
     {
       "label":"%s",
       "component":"input",
       "key":"loop.break",
       "group":"loop"
     },
     {
       "label":"%s",
       "component":"inputNumber",
       "key":"loop.strategy.max_times",
       "group":"loop"
     },
     {
       "label":"%s",
       "component":"inputNumber",
       "key":"loop.strategy.decline_ratio",
       "group":"loop",
       "labelTip":"%s"
     },
     {
       "label":"%s",
       "component":"inputNumber",
       "key":"loop.strategy.decline_limit_sec",
       "group":"loop",
       "labelTip":"%s"
     },
     {
       "label":"%s",
       "component":"inputNumber",
       "key":"loop.strategy.interval_sec",
       "group":"loop"
     }
    ],
   "methodList": [
     "GET",
     "POST",
     "PUT",
     "DELETE",
     "OPTIONS",
     "PATCH",
     "COPY",
     "HEAD"
   ],
   "commonTemp": {
     "target": [
       "headers",
       "body.form"
     ],
     "temp": [
       {
         "title": "%s",
         "key": "key",
         "width": 150,
         "render": {
           "required": true,
           "uniqueValue": true,
           "rules": [
             {
               "max": 50,
               "msg": "%s"
             },
             {
               "pattern": "/^[a-zA-Z0-9_-]*$/",
               "msg": "%s"
             }
           ],
           "props": {
             "placeholder": "%s"
           }
         }
       },
       {
         "title": "%s",
         "key": "value",
         "flex": 2,
         "render": {
           "type": "inputSelect",
           "valueConvertType": "last",
           "required": true,
           "props": {
             "placeholder": "%s",
             "options": `, ae.sdk.I18n("loopStrategy"), ae.sdk.I18n("loopEndCondition"), ae.sdk.I18n("maxLoop"), ae.sdk.I18n("declineRatio"),
		ae.sdk.I18n("intervalRatio"), ae.sdk.I18n("declineMax"), ae.sdk.I18n("loopMaxInterval"), ae.sdk.I18n("startInterval"),
		ae.sdk.I18n("paramName"), ae.sdk.I18n("paramNameMessage1"), ae.sdk.I18n("paramNameMessage2"), ae.sdk.I18n("paramName"),
		ae.sdk.I18n("defaultValue"), ae.sdk.I18n("selectiveExp"))

	var props2 string = fmt.Sprintf(`}
         }
       },
       {
         "title": "%s",
         "key": "desc",
         "width": 300,
         "render": {
           "type": "textarea",
           "required": false,
           "rules": [
             {
               "max": 1000,
               "msg": "%s"
             }
           ],
           "props": {
             "placeholder": "%s"
           }
         }
       }
     ]
   },
   "params": {
     "temp": [
       {
         "title": "%s",
         "key": "key",
         "width": 150,
         "name": "key",
         "render": {
           "required": true,
           "rules": [
             {
               "max": 50,
               "msg": "%s"
             },
             {
               "pattern": "/^[.a-zA-Z0-9_-]*$/",
               "msg": "%s"
             }
           ],
           "props": {
             "placeholder": "%s"
           }
         }
       },
       {
         "title": "%s",
         "key": "value",
         "flex": 2,
         "name": "value",
         "render": {
           "type": "inputSelect",
           "valueConvertType": "last",
           "required": true,
           "props": {
             "placeholder": "%s",
             "options": `, ae.sdk.I18n("desc"), ae.sdk.I18n("descLimit"), ae.sdk.I18n("desc"), ae.sdk.I18n("paramName"),
		ae.sdk.I18n("paramNameMessage1"), ae.sdk.I18n("paramNameMessage3"), ae.sdk.I18n("paramName"), ae.sdk.I18n("defaultValue"), ae.sdk.I18n("selectiveExp"))

	var props3 string = fmt.Sprintf(`}
         }
       },
       {
         "title": "%s",
         "key": "desc",
         "placeholder": "%s",
         "name": "desc",
         "width": 300,
         "render": {
           "type": "textarea",
           "required": false,
           "rules": [
             {
               "max": 1000,
               "msg": "%s"
             }
           ],
           "props": {
             "placeholder": "%s"
           }
         }
       }
     ],
     "showTitle": false
   },
   "headers": {
     "showTitle": false
   },
   "body": {
     "form": {
       "showTitle": false
     }
   },
   "asserts": {
     "comparisonOperators": [
       {
         "label": "%s",
         "value": ">"
       },
       {
         "label": "%s",
         "value": ">="
       },
       {
         "label": "%s",
         "value": "="
       },
       {
         "label": "%s",
         "value": "<="
       },
       {
         "label": "%s",
         "value": "<"
       },
       {
         "label": "%s",
         "value": "!="
       },
       {
         "label": "%s",
         "value": "contains"
       },
       {
         "label": "%s",
         "value": "not_contains"
       },
       {
         "label": "%s",
         "value": "exist"
       },
       {
         "label": "%s",
         "value": "not_exist"
       },
       {
         "label": "%s",
         "value": "empty",
         "allowEmpty": true
       },
       {
         "label": "%s",
         "value": "not_empty",
         "allowEmpty": true
       },
       {
         "label": "%s",
         "value": "belong"
       },
       {
         "label": "%s",
         "value": "not_belong"
       }
     ]
   },
   "apiExecute":
 `, ae.sdk.I18n("desc"), ae.sdk.I18n("desc"), ae.sdk.I18n("descLimit"), ae.sdk.I18n("desc"),
		ae.sdk.I18n("greater"), ae.sdk.I18n("greaterEqual"), ae.sdk.I18n("equal"), ae.sdk.I18n("lessEqual"),
		ae.sdk.I18n("less"), ae.sdk.I18n("notEqual"), ae.sdk.I18n("contain"), ae.sdk.I18n("notContain"),
		ae.sdk.I18n("exist"), ae.sdk.I18n("notExist"), ae.sdk.I18n("isEmpty"), ae.sdk.I18n("notEmpty"),
		ae.sdk.I18n("belong"), ae.sdk.I18n("notBelong"))
	// because props are assembled by splicing json strings,
	// dynamic setting values can only be replaced by placeholders.
	var propsJson = props1 + input + props2 + input + props3 + execute + props4

	for _, opt := range replaceOpts {
		propsJson = strings.ReplaceAll(propsJson, opt.key.string(), opt.value)
	}

	for _, opt := range defaultReplaceOptions {
		propsJson = strings.ReplaceAll(propsJson, opt.key.string(), opt.value)
	}

	var propsI cptype.ComponentProps
	if err := json.Unmarshal([]byte(propsJson), &propsI); err != nil {
		logrus.Errorf("init props name=testplan component=formModal propsType=CreateTestPlan err: errMsg: %v", err)
		return cptype.ComponentProps{}
	}

	return propsI
}

// Input 接口入参结构体
type Input struct {
	Label    string  `json:"label"`
	Value    string  `json:"value"`
	IsLeaf   bool    `json:"isLeaf"`
	ToolTip  string  `json:"tooltip"`
	Children []Input `json:"children"`
}

func (ae *ApiEditor) genMockInput() Input {
	var mockInput Input
	mockInput.Label = "mock"
	mockInput.Value = "mock"
	mockInput.IsLeaf = false
	var children []Input
	for _, v := range expression.MockString {
		o := Input{
			Label:   v,
			Value:   expression.GenRandomRef(v),
			IsLeaf:  true,
			ToolTip: ae.sdk.I18n("wb.content.autotest.scene." + v),
		}
		children = append(children, o)
	}
	mockInput.Children = children
	return mockInput
}

func genEmptyAPISpecStr() (step.APISpec, string) {
	var emptySpec step.APISpec
	emptySpec.APIInfo.Method = "GET"
	emptySpecBytes, err := json.Marshal(&emptySpec)
	if err != nil {
		logrus.Errorf("gen emptyAPISpec err: %v", err)
	}
	return emptySpec, string(emptySpecBytes)
}

const (
	EmptySpecID string = "undefined"
)

var (
	emptySpec, emptySpecStr = genEmptyAPISpecStr()
)
