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

package errorsx

import (
	"fmt"
	"strings"
)

const (
	sessionNotFoundError = "failed to find session"
)

type errType string

var (
	platformAbnormalError errType = "platform-error"
	userAbnormalError     errType = "user-error"
)

type fundamental struct {
	msg string
	errType
}

func (f *fundamental) Error() string {
	return fmt.Sprintf("errType: %s, %s", f.errType, f.msg)
}

func New(message string) error {
	return &fundamental{
		msg: message,
	}
}

func Errorf(format string, args ...interface{}) error {
	return &fundamental{
		msg: fmt.Sprintf(format, args...),
	}
}

func PlatformErrorf(format string, args ...interface{}) error {
	return &fundamental{
		msg:     fmt.Sprintf(format, args...),
		errType: platformAbnormalError,
	}
}

func UserErrorf(format string, args ...interface{}) error {
	return &fundamental{
		msg:     fmt.Sprintf(format, args...),
		errType: userAbnormalError,
	}
}

func IsPlatformError(err error) bool {
	if err == nil {
		return false
	}
	e, ok := err.(*fundamental)
	if !ok {
		return false
	}
	return e.errType == platformAbnormalError
}

func IsUserError(err error) bool {
	if err == nil {
		return false
	}
	e, ok := err.(*fundamental)
	if !ok {
		return false
	}
	return e.errType == userAbnormalError
}

func IsContainUserError(err error) bool {
	return strings.Contains(err.Error(), fmt.Sprintf("errType: %s", userAbnormalError))
}

func IsSessionNotFound(err error) bool {
	return strings.Contains(strings.ToLower(err.Error()), sessionNotFoundError)
}

func IsTimeoutError(err error) bool {
	return strings.Contains(strings.ToLower(err.Error()), "timeout") || strings.Contains(strings.ToLower(err.Error()), "time out")
}

func IsNetworkError(err error) bool {
	return IsSessionNotFound(err) || IsTimeoutError(err)
}
