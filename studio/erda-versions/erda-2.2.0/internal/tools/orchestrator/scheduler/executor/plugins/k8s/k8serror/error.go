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

// Package k8serror encapsulates error of k8s object
package k8serror

import "errors"

var (
	// ErrNotFound indicates "not found" error
	ErrNotFound = errors.New("not found")
	// ErrInvalidParams indicates invalid param(s)
	ErrInvalidParams = errors.New("invalid params")
)

// NotFound return whether it is a "not found" error
func NotFound(err error) bool {
	return notFound(err)
}

func notFound(err error) bool {
	return err.Error() == ErrNotFound.Error()
}
