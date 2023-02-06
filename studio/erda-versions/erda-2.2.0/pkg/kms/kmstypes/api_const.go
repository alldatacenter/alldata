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

type RequestValidator interface {
	ValidateRequest() error
}

const (
	CtxKeyKmsRequestID = "KmsRequestID"
)

const (
	PluginKind_DICE_KMS   PluginKind = "DICE_KMS"
	PluginKind_ERDA_KMS   PluginKind = "DICE_KMS"
	PluginKind_AWS_KMS    PluginKind = "AWS_KMS"
	PluginKind_ALIYUN_KMS PluginKind = "ALIYUN_KMS"

	StoreKind_ETCD  StoreKind = "ETCD"
	StoreKind_MYSQL StoreKind = "MYSQL"

	CustomerMasterKeySpec_SYMMETRIC_DEFAULT   CustomerMasterKeySpec = "SYMMETRIC_DEFAULT" // AES-256-GCM ; default
	CustomerMasterKeySpec_ASYMMETRIC_RSA_2048 CustomerMasterKeySpec = "RSA_2048"
	CustomerMasterKeySpec_ASYMMETRIC_RSA_3072 CustomerMasterKeySpec = "RSA_3072"
	CustomerMasterKeySpec_ASYMMETRIC_RSA_4096 CustomerMasterKeySpec = "RSA_4096"

	KeyUsage_ENCRYPT_DECRYPT KeyUsage = "ENCRYPT_DECRYPT"
	KeyUsage_SIGN_VERIFY     KeyUsage = "SIGN_VERIFY"

	KeyStateEnabled         KeyState = "Enabled"
	KeyStateDisabled        KeyState = "Disabled"
	KeyStatePendingDeletion KeyState = "PendingDeletion"
	KeyStatePendingImport   KeyState = "PendingImport"
	KeyStateUnavailable     KeyState = "Unavailable"
)
