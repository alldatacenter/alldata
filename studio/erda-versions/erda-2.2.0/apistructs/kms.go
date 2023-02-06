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

package apistructs

import (
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

// create key
type KMSCreateKeyRequest struct {
	kmstypes.CreateKeyRequest
}
type KMSCreateKeyResponse struct {
	Header
	Data *kmstypes.CreateKeyResponse `json:"data,omitempty"`
}

// encrypt
type KMSEncryptRequest struct {
	kmstypes.EncryptRequest
}
type KMSEncryptResponse struct {
	Header
	Data *kmstypes.EncryptResponse `json:"data,omitempty"`
}

// decrypt
type KMSDecryptRequest struct {
	kmstypes.DecryptRequest
}
type KMSDecryptResponse struct {
	Header
	Data *kmstypes.DecryptResponse `json:"data,omitempty"`
}

// generate data key
type KMSGenerateDataKeyRequest struct {
	kmstypes.GenerateDataKeyRequest
}
type KMSGenerateDataKeyResponse struct {
	Header
	Data *kmstypes.GenerateDataKeyResponse `json:"data,omitempty"`
}

// rotate key version
type KMSRotateKeyVersionRequest struct {
	kmstypes.RotateKeyVersionRequest
}
type KMSRotateKeyVersionResponse struct {
	Header
	Data *kmstypes.RotateKeyVersionResponse `json:"data,omitempty"`
}

// describe key
type KMSDescribeKeyRequest struct {
	kmstypes.DescribeKeyRequest
}
type KMSDescribeKeyResponse struct {
	Header
	Data *kmstypes.DescribeKeyResponse `json:"data,omitempty"`
}
