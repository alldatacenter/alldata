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

import (
	"encoding/base64"
	"fmt"
)

type EncryptRequest struct {
	KeyID string `json:"keyID,omitempty"`
	// Required. The data to encrypt. Must be no larger than 64KiB.
	// A base64-encoded string.
	PlaintextBase64 string `json:"plaintextBase64,omitempty"`
}

func (req *EncryptRequest) ValidateRequest() error {
	if req.KeyID == "" {
		return fmt.Errorf("missing keyID")
	}
	if len(req.PlaintextBase64) == 0 {
		return fmt.Errorf("missing plaintextBase64")
	}
	if _, err := base64.StdEncoding.DecodeString(string(req.PlaintextBase64)); err != nil {
		return fmt.Errorf("cannot decode base64 plaintext, err: %v", err)
	}
	return nil
}

type EncryptResponse struct {
	KeyID string `json:"keyID,omitempty"`
	// The encrypted data.
	// A base64-encoded string.
	CiphertextBase64 string `json:"ciphertextBase64,omitempty"`
}

type DecryptRequest struct {
	KeyID string `json:"keyID,omitempty"`
	// The encrypted data.
	// A base64-encoded string.
	CiphertextBase64 string `json:"ciphertextBase64,omitempty"`
}

func (req *DecryptRequest) ValidateRequest() error {
	if req.KeyID == "" {
		return fmt.Errorf("missing keyID")
	}
	if len(req.CiphertextBase64) == 0 {
		return fmt.Errorf("missing ciphertextBase64")
	}
	if _, err := base64.StdEncoding.DecodeString(string(req.CiphertextBase64)); err != nil {
		return fmt.Errorf("cannot decode base64 ciphertext, err: %v", err)
	}
	return nil
}

type DecryptResponse struct {
	PlaintextBase64 string `json:"plaintextBase64,omitempty"`
}
