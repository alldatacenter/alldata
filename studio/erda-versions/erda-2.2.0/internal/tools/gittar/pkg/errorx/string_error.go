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

package errorx

import "fmt"

// StringError 简单的字符串型错误
type StringError string

// New 创建一个具有文本信息的错误
func New(text string) error {
	return StringError(text)
}

// Errorf 创建一个具有文本信息的错误
func Errorf(format string, args ...interface{}) error {
	return StringError(fmt.Sprintf(format, args...))
}

func (err StringError) Error() string {
	return string(err)
}
