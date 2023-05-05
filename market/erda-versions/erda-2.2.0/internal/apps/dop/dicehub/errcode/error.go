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

// Package errcode Dicehub错误码
package errcode

// ImageErrorCode 错误码类型
type ImageErrorCode string

// Dicehub错误码
const (
	NetworkError        ImageErrorCode = "IMG0001"
	InternalServerError ImageErrorCode = "IMG0002"
	PermissionDenied    ImageErrorCode = "IMG0003"
	ResourceNotFound    ImageErrorCode = "IMG0004"
	HeaderMissing       ImageErrorCode = "IMG0005"
	BodyMissing         ImageErrorCode = "IMG0006"
	BodyInvalidFormat   ImageErrorCode = "IMG0007"
	ParamMissing        ImageErrorCode = "IMG0008"
	ParamInvalidFormat  ImageErrorCode = "IMG0009"
	ParamInvalid        ImageErrorCode = "IMG0010"
	ParamExist          ImageErrorCode = "IMG0011"
	ResourceInUse       ImageErrorCode = "IMG0012"
)
