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

package encryption

import (
	"crypto/cipher"
	"fmt"
)

type CipherCrypt struct {
	Block cipher.Block
}

//Encrypt encrypts src to dst with cipher & iv, if failed return error
//src the original source bytes
//c the defined cipher type,now support CBC,CFB,OFB,ECB
//ivs the iv for CBC,CFB,OFB mode
//dst the encrypted bytes
func (cc *CipherCrypt) Encrypt(src []byte, c Cipher, ivs ...[]byte) (dst []byte, err error) {
	block := cc.Block
	data := PKCS7Padding(src, block.BlockSize())
	if len(data)%block.BlockSize() != 0 {
		return nil, fmt.Errorf("Need a multiple of the blocksize ")
	}
	switch c {
	case CBC:
		return cbcEncrypt(block, data, ivs...)
	case CFB:
		return cfbEncrypt(block, data, ivs...)
	case OFB:
		return ofbCrypt(block, data, ivs...)
	default:
		return ecbEncrypt(block, data)
	}
}

//EncryptToString encrypts src then encodes data returned to string
//encodeType now support String,HEX,Base64
func (cc *CipherCrypt) EncryptToString(encodeType Encode, src []byte, c Cipher, ivs ...[]byte) (dst string, err error) {
	data, err := cc.Encrypt(src, c, ivs...)
	if err != nil {
		return
	}
	return EncodeToString(data, encodeType)
}

//Decrypt decrypts src to dst with cipher & iv, if failed return error
//src the original encrypted bytes
//c the defined cipher type,now support CBC,CFB,OFB,ECB
//ivs the iv for CBC,CFB,OFB mode
//dst the decrypted bytes
func (cc *CipherCrypt) Decrypt(src []byte, c Cipher, ivs ...[]byte) (dst []byte, err error) {
	block := cc.Block
	switch c {
	case CBC:
		dst, err = cbcDecrypt(block, src, ivs...)
	case CFB:
		dst, err = cfbDecrypt(block, src, ivs...)
	case OFB:
		dst, err = ofbCrypt(block, src, ivs...)
	default:
		dst, err = ecbDecrypt(block, src)
	}
	return UnPaddingPKCS7(dst), err
}

//DecryptToString decrypts src then encodes return data to string
//encodeType now support String,HEX,Base64
func (cc *CipherCrypt) DecryptToString(encodeType Encode, src []byte, c Cipher, ivs ...[]byte) (dst string, err error) {
	data, err := cc.Decrypt(src, c, ivs...)
	if err != nil {
		return
	}
	return EncodeToString(data, encodeType)
}

//ecbEncrypt encrypts data with ecb mode
func ecbEncrypt(block cipher.Block, src []byte) (dst []byte, err error) {
	out := make([]byte, len(src))
	dst = out
	for len(src) > 0 {
		block.Encrypt(dst, src[:block.BlockSize()])
		src = src[block.BlockSize():]
		dst = dst[block.BlockSize():]
	}
	return out, nil
}

//ecbDecrypt decrypts data with ecb mode
func ecbDecrypt(block cipher.Block, src []byte) (dst []byte, err error) {
	dst = make([]byte, len(src))
	out := dst
	bs := block.BlockSize()
	if len(src)%bs != 0 {
		err = fmt.Errorf("crypto/cipher: input not full blocks")
		return
	}
	for len(src) > 0 {
		block.Decrypt(out, src[:bs])
		src = src[bs:]
		out = out[bs:]
	}
	return
}

//cbcEncrypt encrypts data with cbc mode
func cbcEncrypt(block cipher.Block, src []byte, ivs ...[]byte) (dst []byte, err error) {
	var iv []byte
	if len(ivs) > 0 {
		iv = ivs[0]
	}
	bm := cipher.NewCBCEncrypter(block, iv)
	dst = make([]byte, len(src))
	bm.CryptBlocks(dst, src)
	return
}

//cbcDecrypt decrypts data with cbc mode
func cbcDecrypt(block cipher.Block, src []byte, ivs ...[]byte) (dst []byte, err error) {
	var iv []byte
	if len(ivs) > 0 {
		iv = ivs[0]
	}
	bm := cipher.NewCBCDecrypter(block, iv)
	dst = make([]byte, len(src))
	bm.CryptBlocks(dst, src)
	return
}

//cfbEncrypt encrypts data with cfb mode
func cfbEncrypt(block cipher.Block, src []byte, ivs ...[]byte) (dst []byte, err error) {
	var iv []byte
	if len(ivs) > 0 {
		iv = ivs[0]
	}
	bm := cipher.NewCFBEncrypter(block, iv)
	dst = make([]byte, len(src))
	bm.XORKeyStream(dst, src)
	return
}

//cfbDecrypt decrypts data with cfb mode
func cfbDecrypt(block cipher.Block, src []byte, ivs ...[]byte) (dst []byte, err error) {
	var iv []byte
	if len(ivs) > 0 {
		iv = ivs[0]
	}
	bm := cipher.NewCFBDecrypter(block, iv)
	dst = make([]byte, len(src))
	bm.XORKeyStream(dst, src)
	return
}

//ofbCrypt encrypts or decrypts data with ofb mode
func ofbCrypt(block cipher.Block, src []byte, ivs ...[]byte) (dst []byte, err error) {
	var iv []byte
	if len(ivs) > 0 {
		iv = ivs[0]
	}
	bm := cipher.NewOFB(block, iv)
	dst = make([]byte, len(src))
	bm.XORKeyStream(dst, src)
	return
}
