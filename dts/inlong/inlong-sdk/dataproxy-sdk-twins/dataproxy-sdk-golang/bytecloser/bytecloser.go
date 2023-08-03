//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package bytecloser

import (
	"bytes"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/bufferpool"
)

// ByteCloser is the interface of a byte Buffer that can be closed
type ByteCloser interface {
	// Bytes return the bytes
	Bytes() []byte
	// Close closes the bytes
	Close()
}

// Bytes is the raw byte implementations of ByteCloser
type Bytes []byte

// Bytes return the bytes
func (b Bytes) Bytes() []byte {
	return b
}

// Close closes the bytes
func (b Bytes) Close() {
}

// BufferBytes is the buffered byte implementations of ByteCloser
type BufferBytes struct {
	BufferPool bufferpool.BufferPool
	Buffer     *bytes.Buffer
}

// Bytes return the bytes
func (b *BufferBytes) Bytes() []byte {
	if b.Buffer == nil {
		return nil
	}

	return b.Buffer.Bytes()
}

// Close closes the bytes
func (b *BufferBytes) Close() {
	if b.Buffer == nil {
		return
	}

	if b.BufferPool == nil {
		b.Buffer.Reset()
		return
	}

	b.BufferPool.Put(b.Buffer)
	b.Buffer = nil
}
