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

package common

import (
	"strings"

	"github.com/gin-gonic/gin"
)

type ErrInfo struct {
	Code  string `json:"code,omitempty"`
	Msg   string `json:"msg,omitempty"`
	EnMsg string `json:"-"`
}

type StandardResult struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data"`
	Err     *ErrInfo    `json:"err"`
}

type StandaredReturnCode interface {
	GetCode() string
	GetMessage() string
}

func NewStandardResult(succ ...bool) *StandardResult {
	isSucc := true
	if len(succ) != 0 {
		isSucc = succ[0]
	}
	return &StandardResult{Success: isSucc}
}

func (result *StandardResult) SwitchLang(c *gin.Context) *StandardResult {
	lang := c.GetHeader("lang")
	if strings.Contains(lang, "en-US") && result.Err.EnMsg != "" {
		result.Err.Msg = result.Err.EnMsg
	}
	return result
}

func (result *StandardResult) SetReturnCode(returnCode StandaredReturnCode) *StandardResult {
	result.Err = &ErrInfo{
		Code: returnCode.GetCode(),
		Msg:  returnCode.GetMessage(),
	}
	return result
}

func (result *StandardResult) SetSuccessAndData(data interface{}) *StandardResult {
	result.Data = data
	result.Success = true
	return result
}

func (result *StandardResult) SetErrorInfo(errInfo *ErrInfo) *StandardResult {
	result.Err = errInfo
	return result
}
