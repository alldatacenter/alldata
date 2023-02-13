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

package testcase

import (
	"encoding/json"
	"fmt"
	"io"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/xmind"
)

type tcTopicWithDirectory struct {
	xmind.Topic
	Directory string
}

func (svc *Service) decodeFromXMindFile(r io.Reader) ([]apistructs.TestCaseXmind, error) {
	content, err := xmind.Parse(r)
	if err != nil {
		return nil, err
	}
	if len(content) == 0 {
		return nil, nil
	}
	sheet := content[0]
	// 根节点只做展示，不做解析
	rootTopic := sheet.Topic
	_ = trimTitle(rootTopic.Title)
	var allTestCases []apistructs.TestCaseXmind
	allTcTopics := make([]tcTopicWithDirectory, 0) // keep order
	for _, subTopic := range rootTopic.Topics {
		recursiveParseTestSetTopic(subTopic, nil, &allTcTopics)
	}
	for _, tcTopicWithDir := range allTcTopics {
		tc, err := parseXMindTestCaseTopic(tcTopicWithDir.Topic, tcTopicWithDir.Directory)
		if err != nil {
			return nil, fmt.Errorf("topic fullpath: %s/%s, err: %v", tcTopicWithDir.Directory, tcTopicWithDir.Title, err)
		}
		allTestCases = append(allTestCases, *tc)
	}
	return allTestCases, nil
}

// fakeTcName 用于创建空子测试集
var (
	fakeTcName             = "TerminusFakeTerminusFake"
	fakeTcNameWithPriority = fmt.Sprintf("tc:P3__%s", fakeTcName)
)

// recursiveParseTestSetTopic 从当前节点开始递归，最终返回所有 tc 节点
// tsPaths: 目录层级列表
// result: key: 拼装好的目录，value: tc 节点
func recursiveParseTestSetTopic(topic xmind.Topic, tsPaths []string, allTcTopics *[]tcTopicWithDirectory) {
	// trim space before handle
	topic.Title = trimTitle(topic.Title)
	if allTcTopics == nil {
		_allTcTopics := make([]tcTopicWithDirectory, 0)
		allTcTopics = &_allTcTopics
	}
	// 普通节点
	if !strutil.HasPrefixes(topic.Title, "tc:") {
		// 若当前叶子节点已经是最后一个节点，且非 tc 节点，表示一个空测试集
		// 此时生成 fake tc 节点，继续解析，用于创建空子测试集
		if len(topic.Topics) == 0 {
			topic.Topics = append(topic.Topics, xmind.Topic{Title: fakeTcNameWithPriority})
		}

		for _, subTopic := range topic.Topics {
			var copiedTsPaths []string
			for _, p := range tsPaths {
				copiedTsPaths = append(copiedTsPaths, p)
			}
			// 已访问路径加上当前节点后，再递归 subTopic
			copiedTsPaths = append(copiedTsPaths, topic.Title)
			recursiveParseTestSetTopic(subTopic, copiedTsPaths, allTcTopics)
		}
	} else {
		// tc 节点
		tsDir := strutil.JoinPath(tsPaths...)
		if !strutil.HasPrefixes(tsDir, "/") {
			tsDir = "/" + tsDir
		}
		*allTcTopics = append(*allTcTopics, tcTopicWithDirectory{Topic: topic, Directory: tsDir})
	}
}

// Topic 类型
// - 测试集 无标记
// - 测试用例 tc:Px__... 或 tc:Px_m_...
//   - 前置条件 p:
//   - 步骤1 - 结果1
//   - ...
//   - 步骤n - 结果n
//   - 接口测试
//     - at: (APITest)
//
// tsPaths 用于存放测试集目录列表
func parseXMindTestCaseTopic(topic xmind.Topic, directory string) (*apistructs.TestCaseXmind, error) {
	var tc apistructs.TestCaseXmind
	tc.DirectoryName = directory
	title := topic.Title
	// 用例名称
	// P0_m_... or P0__...
	priorityWithName := strutil.TrimPrefixes(title, "tc:")
	// 优先级
	priority := priorityWithName[0:2]
	nameWithPrefix := priorityWithName[2:]
	// 用例名称缺省值为 tc: 后所有字符
	name := nameWithPrefix
	if strutil.HasPrefixes(nameWithPrefix, "_m_") {
		name = nameWithPrefix[3:]
	}
	if strutil.HasPrefixes(nameWithPrefix, "__") {
		name = nameWithPrefix[2:]
	}
	// 若优先级不合法，则默认优先级 P3，整个字符串作为 name
	if !apistructs.TestCasePriority(priority).IsValid() {
		priority = string(apistructs.TestCasePriorityP3)
		name = priorityWithName
	}
	tc.PriorityName = priority
	tc.Title = name
	// 前置条件
	if strutil.HasPrefixes(topic.GetFirstSubTopicTitle(), "p:") {
		tc.PreCondition = strutil.TrimPrefixes(topic.GetFirstSubTopicTitle(), "p:")
		// topic 从 p 开始
		topic = topic.Topics[0]
	}
	for _, subTopic := range topic.Topics {
		// 接口测试
		if subTopic.Title == "接口测试" {
			for _, apiTestTopic := range subTopic.Topics {
				if !strutil.HasPrefixes(apiTestTopic.Title, "at:") {
					continue
				}
				var apiTest apistructs.APIInfo
				apiTest.Name = strutil.TrimPrefixes(apiTestTopic.Title, "at:")
				for _, apiItemTopic := range apiTestTopic.Topics {
					switch apiItemTopic.Title {
					case "headers":
						headersStr := apiItemTopic.GetFirstSubTopicTitle()
						if headersStr == "" {
							continue
						}
						var headers []apistructs.APIHeader
						if err := json.Unmarshal([]byte(headersStr), &headers); err != nil {
							return nil, fmt.Errorf("failed to parse api headers, err: %v", err)
						}
						apiTest.Headers = headers
					case "method":
						apiTest.Method = apiItemTopic.GetFirstSubTopicTitle()
					case "url":
						apiTest.URL = apiItemTopic.GetFirstSubTopicTitle()
					case "params":
						paramsStr := apiItemTopic.GetFirstSubTopicTitle()
						if paramsStr == "" {
							continue
						}
						var params []apistructs.APIParam
						if err := json.Unmarshal([]byte(paramsStr), &params); err != nil {
							return nil, fmt.Errorf("failed to parse api params, err: %v", err)
						}
						apiTest.Params = params
					case "body":
						bodyStr := apiItemTopic.GetFirstSubTopicTitle()
						if bodyStr == "" {
							continue
						}
						var body apistructs.APIBody
						if err := json.Unmarshal([]byte(bodyStr), &body); err != nil {
							return nil, fmt.Errorf("failed to parse api body, err: %v", err)
						}
						apiTest.Body = body
					case "outParams":
						outParamsStr := apiItemTopic.GetFirstSubTopicTitle()
						if outParamsStr == "" {
							continue
						}
						var outParams []apistructs.APIOutParam
						if err := json.Unmarshal([]byte(outParamsStr), &outParams); err != nil {
							return nil, fmt.Errorf("failed to parse api outParams, err: %v", err)
						}
						apiTest.OutParams = outParams
					case "asserts":
						assertsStr := apiItemTopic.GetFirstSubTopicTitle()
						if assertsStr == "" {
							continue
						}
						var asserts [][]apistructs.APIAssert
						if err := json.Unmarshal([]byte(assertsStr), &asserts); err != nil {
							return nil, fmt.Errorf("failed to parse api asserts, err: %v", err)
						}
						apiTest.Asserts = asserts
					}
				}
				tc.ApiInfos = append(tc.ApiInfos, apiTest)
			}
		} else {
			// 其他 sub topic 均为步骤和结果
			tc.StepAndResults = append(tc.StepAndResults, apistructs.TestCaseStepAndResult{Step: subTopic.Title, Result: subTopic.GetFirstSubTopicTitle()})
		}
	}
	return &tc, nil
}

// trimTitle trim title's leading and trailing space
func trimTitle(title string) string {
	return strings.TrimSpace(title)
}
