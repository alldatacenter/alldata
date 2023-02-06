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

// Package apierrors defines all service errors.
package apierrors

import (
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

var (
	ErrCheckIdentity    = err("ErrCheckIdentity", "身份校验失败")
	ErrParseRequest     = err("ErrParseRequest", "解析请求失败")
	ErrCreateKey        = err("ErrCreateKey", "创建 KMS 用户主密钥失败")
	ErrEncrypt          = err("ErrEncrypt", "对称加密失败")
	ErrDecrypt          = err("ErrDecrypt", "对称解密失败")
	ErrGenerateDataKey  = err("ErrGenerateDataKey", "生成数据加密密钥失败")
	ErrRotateKeyVersion = err("ErrRotateKeyVersion", "轮转密钥版本失败")
	ErrDescribeKey      = err("ErrDescribeKey", "查询用户主密钥失败")
)

func err(template, defaultValue string) *errorresp.APIError {
	return errorresp.New(errorresp.WithTemplateMessage(template, defaultValue))
}
