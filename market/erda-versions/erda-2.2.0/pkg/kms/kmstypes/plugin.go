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

// kms package 提供统一加密服务。
package kmstypes

import (
	"context"
)

type Plugin interface {
	Kind() PluginKind
	SetStore(Store)
	BasePlugin
	SymmetricPlugin
	AsymmetricPlugin
}

type BasePlugin interface {
	// CreateKey create symmetric or asymmetric CMK
	CreateKey(ctx context.Context, req *CreateKeyRequest) (*CreateKeyResponse, error)
	DescribeKey(ctx context.Context, req *DescribeKeyRequest) (*DescribeKeyResponse, error)
	ListKeys(ctx context.Context, req *ListKeysRequest) (*ListKeysResponse, error)
}

// SymmetricPlugin 对称加密插件
// 加密流程：
// 1. 调用 Encrypt 进行加密
// 解密流程：
// 1. 调用 Decrypt 进行解密
type SymmetricPlugin interface {
	Encrypt(ctx context.Context, req *EncryptRequest) (*EncryptResponse, error)
	Decrypt(ctx context.Context, req *DecryptRequest) (*DecryptResponse, error)
	// GenerateDataKey generate AES 256 DEK, encrypted by CMK
	// 典型使用场景（信封加密）：
	// 在本地进行数据加密：
	// 1. 调用 GenerateDataKey 获取 DEK（数据加密密钥）
	// 2. 使用 DEK 的明文，在本地完成离线数据加密，随后清除内存中的 DEK 明文
	// 3. 将 DEK 的密文，和本地离线加密后的数据一并进行存储
	// 在本地进行数据解密：
	// 1. 调用 Decrypt 解密本地存储的 DEK 密文，获取 DEK 明文
	// 2. 使用 DEK 明文，在本地完成离线数据解密，随后清除内存中的 DEK 明文
	GenerateDataKey(ctx context.Context, req *GenerateDataKeyRequest) (*GenerateDataKeyResponse, error)
	// RotateKeyVersion rotate key version for CMK manually, old key version still can be used to decrypt old data
	RotateKeyVersion(ctx context.Context, req *RotateKeyVersionRequest) (*RotateKeyVersionResponse, error)
}

// AsymmetricPlugin 非对称加密插件
// 加密流程：
// 1. GetPublicKey 获取公钥
// 2. 使用公钥加密数据
// 3. 存储加密后的数据以及密钥版本
// 解密流程：
// 1. 调用 AsymmetricDecrypt，传入密文和 解密
type AsymmetricPlugin interface {
	GetPublicKey(ctx context.Context, req *GetPublicKeyRequest) (*PublicKey, error)
	// AsymmetricDecrypt decrypts data that was encrypted with a public key retrieved from GetPublicKey
	// corresponding to a CryptoKeyVersion with CryptoKey.purpose ASYMMETRIC_DECRYPT.
	AsymmetricDecrypt(ctx context.Context, req *AsymmetricDecryptRequest) (*AsymmetricDecryptResponse, error)
}
