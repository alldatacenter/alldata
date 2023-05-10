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

package interceptors

import (
	"net/http"
)

// Interceptor .
type Interceptor struct {
	Order   int
	Wrapper func(h http.HandlerFunc) http.HandlerFunc
}

// Interface .
type Interface interface {
	List() []*Interceptor
}

// Interceptors .
type Interceptors []*Interceptor

func (list Interceptors) Len() int           { return len(list) }
func (list Interceptors) Less(i, j int) bool { return list[i].Order < list[j].Order }
func (list Interceptors) Swap(i, j int) {
	list[i], list[j] = list[j], list[i]
}

// Config .
type Config struct {
	Order int `file:"order"`
}
