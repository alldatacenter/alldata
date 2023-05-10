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

package xmind

import (
	"encoding/xml"
	"fmt"
	"os"
	"testing"

	"github.com/davecgh/go-spew/spew"
	"github.com/mholt/archiver"
	"github.com/stretchr/testify/assert"
	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda/pkg/strutil"
)

func TestParseXML(t *testing.T) {
	f, err := os.Open("./examples/xmind/content.xml.ok")
	assert.NoError(t, err)
	m, err := ParseXML(f)
	assert.NoError(t, err)
	spew.Dump(m)
}

func TestExportXMind(t *testing.T) {
	content := XMLContent{
		Sheet: XMLSheet{
			Topic: &XMLTopic{
				Title: "dice-backup",
				Children: &XMLTopicChildren{
					TypedTopics: &XMLTypedTopics{
						Type: TopicsTypeAttached,
						Topics: []*XMLTopic{
							{
								Title: "目录1",
								Children: &XMLTopicChildren{
									TypedTopics: &XMLTypedTopics{
										Type: TopicsTypeAttached,
										Topics: []*XMLTopic{
											{
												Title: "tc:P3__新增应用",
												Children: &XMLTopicChildren{
													TypedTopics: &XMLTypedTopics{
														Type: TopicsTypeAttached,
														Topics: []*XMLTopic{
															{
																Title: "p:已经加入项目",
																Children: &XMLTopicChildren{
																	TypedTopics: &XMLTypedTopics{
																		Type: TopicsTypeAttached,
																		Topics: []*XMLTopic{
																			{
																				Title: "步骤1",
																				Children: &XMLTopicChildren{
																					TypedTopics: &XMLTypedTopics{
																						Type: TopicsTypeAttached,
																						Topics: []*XMLTopic{
																							{
																								Title: "结果1",
																							},
																						},
																					},
																				},
																			},
																		},
																	},
																},
															},
														},
													},
												},
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}
	b, _ := xml.MarshalIndent(&content, "", "  ")
	fmt.Println(string(b))

	f, err := os.OpenFile("/tmp/exported.xmind", os.O_TRUNC|os.O_CREATE|os.O_RDWR, 0755)
	assert.NoError(t, err)

	err = Export(f, content, "sfb")
	assert.NoError(t, err)
}

func TestZipXmind(t *testing.T) {
	os.Chdir("./examples/xmind")
	files := []string{"META-INF/", "content.xml", "meta.xml"}
	err := archiver.Zip.Make("a.xmind", files)
	defer os.Remove("a.xmind")
	assert.NoError(t, err)
}

// TestAddAttachedChildTopic
func TestAddAttachedChildTopic(t *testing.T) {
	rootTopic := &XMLTopic{
		Title: "测试用例",
	}

	topic1 := rootTopic
	for _, dir := range strutil.Split("/d1", "/", true) {
		topic1 = topic1.AddAttachedChildTopic(dir, true)
	}

	topic2 := rootTopic
	for _, dir := range strutil.Split("/d1/d4", "/", true) {
		topic2 = topic2.AddAttachedChildTopic(dir, true)
	}

	topic3 := topic2
	for _, dir := range strutil.Split("/d5/d6", "/", true) {
		topic3 = topic3.AddAttachedChildTopic(dir, true)
	}

	b, err := yaml.Marshal(rootTopic)
	assert.NoError(t, err)
	fmt.Println(string(b))
}
