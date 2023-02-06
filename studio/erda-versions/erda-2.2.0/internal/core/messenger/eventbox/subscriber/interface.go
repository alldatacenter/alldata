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

package subscriber

import (
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

type Subscriber interface {
	// 各个实现自己解析 dest
	// 返回 []error , 是因为发送消息的目的可能是多个
	// dest: marshaled string
	// content: marshaled string
	Publish(dest string, content string, time int64, m *types.Message) []error
	Status() interface{}
	Name() string
}
