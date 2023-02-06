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
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"fmt"
	"io"
	"strconv"
)

// AesGcmEncrypt similar with AesGcmEncryptWithNonce, return nonce-contained ciphertext.
// Use AesGcmDecrypt to decrypt.
func AesGcmEncrypt(key, plaintext, additionalData []byte) (ciphertext []byte, err error) {
	var nonce []byte

	ciphertext, nonce, err = AesGcmEncryptWithNonce(key, plaintext, additionalData)
	if err != nil {
		return nil, err
	}

	// decode with `PrefixUnAppend000Length`
	nonceWithLength, err := PrefixAppend000Length(nonce)
	if err != nil {
		return nil, err
	}

	ciphertext = append(nonceWithLength, ciphertext...)

	return
}

// AesGcmEncryptWithNonce takes an encryption key and a plaintext string and encrypts it with AES256 in GCM mode,
// which provides authenticated encryption. Returns the ciphertext and the used nonce.
func AesGcmEncryptWithNonce(key, plaintext, additionalData []byte) (ciphertext, nonce []byte, err error) {
	plaintextBytes := plaintext

	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, nil, err
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, nil, err
	}

	nonce = make([]byte, aesgcm.NonceSize())
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, nil, err
	}

	ciphertext = aesgcm.Seal(nil, nonce, plaintextBytes, additionalData)

	return
}

// AesGcmDecrypt similar to AesGcmDecryptWithNonce, but ciphertext is nonce-contained.
func AesGcmDecrypt(key, ciphertext, additionalData []byte) (plaintext []byte, err error) {
	nonce, ciphertext, err := PrefixUnAppend000Length(ciphertext)
	if err != nil {
		return nil, err
	}

	return AesGcmDecryptWithNonce(key, ciphertext, nonce, additionalData)
}

// AesGcmDecryptWithNonce takes an decryption key, a ciphertext and the corresponding nonce and decrypts it with AES256
// in GCM mode. Returns the plaintext string.
func AesGcmDecryptWithNonce(key, ciphertext, nonce, additionalData []byte) (plaintext []byte, err error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return
	}

	aesgcm, err := cipher.NewGCM(block)
	if err != nil {
		return
	}

	plaintext, err = aesgcm.Open(nil, nonce, ciphertext, additionalData)
	if err != nil {
		return
	}

	return
}

// GenerateAes256Key generate 256-bit aes symmetric key.
func GenerateAes256Key() ([]byte, error) {
	nonce := make([]byte, 32)
	if _, err := io.ReadFull(rand.Reader, nonce); err != nil {
		return nil, err
	}
	return nonce, nil
}

// PrefixAppend000Length max byte length is 999.
// []byte(b) -> 000[]byte(b)
func PrefixAppend000Length(b []byte) ([]byte, error) {
	if len(b) > 999 {
		return nil, fmt.Errorf("byte length too large")
	}
	return []byte(fmt.Sprintf("%03d%s", len(b), string(b))), nil
}

// PrefixUnAppend000Length
// 000[]byte(b)[]byte(a) -> []byte(b), []byte(a)
func PrefixUnAppend000Length(b []byte) (under, remains []byte, err error) {
	// parse keyVersion
	prefixLen, err := strconv.ParseInt(string(b[:3]), 10, 64)
	if err != nil {
		return nil, nil, err
	}
	return b[3 : 3+prefixLen], b[3+prefixLen:], nil
}
