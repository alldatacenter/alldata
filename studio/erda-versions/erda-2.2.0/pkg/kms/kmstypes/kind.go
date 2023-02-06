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

package kmstypes

import (
	"regexp"
)

const kindNameFormat = `^[A-Z0-9_]+$`

var formatter = regexp.MustCompile(kindNameFormat)

type PluginKind string

func (s PluginKind) String() string {
	return string(s)
}

func (s PluginKind) Validate() bool {
	return formatter.MatchString(string(s))
}

type StoreKind string

func (s StoreKind) String() string {
	return string(s)
}

func (s StoreKind) Validate() bool {
	return formatter.MatchString(string(s))
}
