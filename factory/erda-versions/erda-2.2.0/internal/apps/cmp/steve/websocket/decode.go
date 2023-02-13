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

package websocket

import (
	"bytes"
	"encoding/binary"
	"unsafe"
)

const (
	// Frame header byte 1 bits from Section 5.2 of RFC 6455
	maskBit = 1 << 7
)

func DecodeFrames(data []byte) [][]byte {
	buf := bytes.NewBuffer(data)
	frames := make([][]byte, 0)
	for buf.Len() > 0 {
		first2Bytes := make([]byte, 2)
		buf.Read(first2Bytes)

		mask := first2Bytes[1]&maskBit != 0
		length := uint64(first2Bytes[1] & 0x7f)
		switch length {
		case 126:
			lenBytes := make([]byte, 2)
			buf.Read(lenBytes)
			length = binary.BigEndian.Uint64(lenBytes)
		case 127:
			lenBytes := make([]byte, 8)
			buf.Read(lenBytes)
			length = binary.BigEndian.Uint64(lenBytes)
		}

		maskKeyBytes := make([]byte, 4)
		if mask {
			buf.Read(maskKeyBytes)
		}

		payloadBytes := make([]byte, length)
		buf.Read(payloadBytes)
		if mask {
			maskBytes(maskKeyBytes, 0, payloadBytes)
		}
		frames = append(frames, payloadBytes)
	}
	return frames
}

const wordSize = int(unsafe.Sizeof(uintptr(0)))

func maskBytes(key []byte, pos int, b []byte) int {
	// Mask one byte at a time for small buffers.
	if len(b) < 2*wordSize {
		for i := range b {
			b[i] ^= key[pos&3]
			pos++
		}
		return pos & 3
	}

	// Mask one byte at a time to word boundary.
	if n := int(uintptr(unsafe.Pointer(&b[0]))) % wordSize; n != 0 {
		n = wordSize - n
		for i := range b[:n] {
			b[i] ^= key[pos&3]
			pos++
		}
		b = b[n:]
	}

	// Create aligned word size key.
	var k [wordSize]byte
	for i := range k {
		k[i] = key[(pos+i)&3]
	}
	kw := *(*uintptr)(unsafe.Pointer(&k))

	// Mask one word at a time.
	n := (len(b) / wordSize) * wordSize
	for i := 0; i < n; i += wordSize {
		*(*uintptr)(unsafe.Pointer(uintptr(unsafe.Pointer(&b[0])) + uintptr(i))) ^= kw
	}

	// Mask one byte at a time for remaining bytes.
	b = b[n:]
	for i := range b {
		b[i] ^= key[pos&3]
		pos++
	}

	return pos & 3
}
