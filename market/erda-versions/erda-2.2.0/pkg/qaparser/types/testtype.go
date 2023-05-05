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

package types

// 测试结果解析 parser
type TestParserType string

const (
	// 使用 maven-surefire-plugin 生成的 TEST-xxx.xml 格式进行解析
	Default TestParserType = "DEFAULT"
	// 使用 testng 插件 org.testng.reporters.XMLReporter 生成的格式进行解析, 需要在 pom.xml 中配置该 reporter
	NGTest TestParserType = "NGTEST"
	// 使用 junit 生成的 xml 格式进行解析
	JUnit TestParserType = "JUNIT"
)

func (t TestParserType) TPValue() string {
	return string(t)
}
