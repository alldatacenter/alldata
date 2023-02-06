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

package bundle

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
)

func (b *Bundle) KMSCreateKey(req apistructs.KMSCreateKeyRequest) (*kmstypes.CreateKeyResponse, error) {
	host, err := b.urls.KMS()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.KMSCreateKeyResponse
	httpResp, err := hc.Post(host).Path("/api/kms").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !getResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), getResp.Error)
	}
	return getResp.Data, nil
}

func (b *Bundle) KMSEncrypt(req apistructs.KMSEncryptRequest) (*kmstypes.EncryptResponse, error) {
	host, err := b.urls.KMS()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var encryptResp apistructs.KMSEncryptResponse
	httpResp, err := hc.Post(host).Path("/api/kms/encrypt").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&encryptResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !encryptResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), encryptResp.Error)
	}
	return encryptResp.Data, nil
}

func (b *Bundle) KMSDecrypt(req apistructs.KMSDecryptRequest) (*kmstypes.DecryptResponse, error) {
	host, err := b.urls.KMS()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var decryptResp apistructs.KMSDecryptResponse
	httpResp, err := hc.Post(host).Path("/api/kms/decrypt").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&decryptResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !decryptResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), decryptResp.Error)
	}
	return decryptResp.Data, nil
}

// 典型使用场景（信封加密）：
// 在本地进行数据加密：
// 1. 调用 KMSGenerateDataKey 获取 DEK（数据加密密钥）
// 2. 使用 DEK 的明文，在本地完成离线数据加密，随后清除内存中的 DEK 明文
// 3. 将 DEK 的密文，和本地离线加密后的数据一并进行存储
// 在本地进行数据解密：
// 1. 调用 KMSDecrypt 解密本地存储的 DEK 密文，获取 DEK 明文
// 2. 使用 DEK 明文，在本地完成离线数据解密，随后清除内存中的 DEK 明文
func (b *Bundle) KMSGenerateDataKey(req apistructs.KMSGenerateDataKeyRequest) (*kmstypes.GenerateDataKeyResponse, error) {
	host, err := b.urls.KMS()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var generateResp apistructs.KMSGenerateDataKeyResponse
	httpResp, err := hc.Post(host).Path("/api/kms/generate-data-key").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&generateResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !generateResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), generateResp.Error)
	}
	return generateResp.Data, nil
}

func (b *Bundle) KMSRotateKeyVersion(req apistructs.KMSRotateKeyVersionRequest) (*kmstypes.RotateKeyVersionResponse, error) {
	host, err := b.urls.KMS()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var rotateResp apistructs.KMSRotateKeyVersionResponse
	httpResp, err := hc.Post(host).Path("/api/kms/rotate-key-version").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&rotateResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !rotateResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rotateResp.Error)
	}
	return rotateResp.Data, nil
}

func (b *Bundle) KMSDescribeKey(req apistructs.KMSDescribeKeyRequest) (*kmstypes.DescribeKeyResponse, error) {
	host, err := b.urls.KMS()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var descResp apistructs.KMSDescribeKeyResponse
	httpResp, err := hc.Get(host).Path("/api/kms/describe-key").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&descResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !descResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), descResp.Error)
	}
	return descResp.Data, nil
}
