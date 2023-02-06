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

package kmscrypto

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func TestAES(t *testing.T) {
	key, err := GenerateAes256Key()
	assert.NoError(t, err)
	plaintext := []byte("hello world")

	cmk := uuid.UUID()

	// encrypt
	ciphertext, err := AesGcmEncrypt(key, plaintext, []byte(cmk))
	assert.NoError(t, err)
	fmt.Printf("ciphertext: %x\n", ciphertext)

	// decrypt
	decrypted, err := AesGcmDecrypt(key, ciphertext, []byte(cmk))
	assert.NoError(t, err)
	assert.Equal(t, plaintext, decrypted)
}

func TestWrapWithLenPrefix(t *testing.T) {
	b := []byte("hello world")
	bb, err := PrefixAppend000Length(b)
	assert.NoError(t, err)
	assert.Equal(t, "011"+string(b), string(bb))

	under, remains, err := PrefixUnAppend000Length(bb)
	assert.NoError(t, err)
	assert.Equal(t, string(b), string(under))
	assert.Equal(t, 0, len(remains))
}
