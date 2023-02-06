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

type AsymmetricDecryptRequest struct {
	KeyID            string `json:"keyID,omitempty"`
	CiphertextBase64 []byte `json:"ciphertextBase64,omitempty"`
}
type AsymmetricDecryptResponse struct {
	PlaintextBase64 []byte `json:"plaintextBase64,omitempty"`
}

type GetPublicKeyRequest struct {
	KeyID string `json:"keyID,omitempty"`
}

type PublicKey struct {
	Pem       string `json:"pem,omitempty"`
	Algorithm string `json:"algorithm,omitempty"`
}
