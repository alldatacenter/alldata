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

package bufferpool

import (
	"bytes"

	"github.com/oxtoacart/bpool"
)

// BufferPool buffer pool interface
type BufferPool interface {
	Get() *bytes.Buffer
	Put(buffer *bytes.Buffer)
}

// BytePool byte pool interface
type BytePool interface {
	Get() []byte
	Put(b []byte)
}

// NewSizedBuffer news a buffer pool with specific buffer size
func NewSizedBuffer(poolSize, bufferSize int) BufferPool {
	return bpool.NewSizedBufferPool(poolSize, bufferSize)
}

// NewBuffer news a buffer pool
func NewBuffer(poolSize int) BufferPool {
	return bpool.NewBufferPool(poolSize)
}

// NewBytePool news a byte pool
func NewBytePool(poolSize, length int) BytePool {
	return bpool.NewBytePool(poolSize, length)
}
