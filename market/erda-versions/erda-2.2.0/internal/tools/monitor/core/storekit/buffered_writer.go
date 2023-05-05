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

package storekit

import (
	"fmt"

	"github.com/recallsong/go-utils/errorx"
)

// BufferedWriter .
type BufferedWriter struct {
	w        BatchWriter
	buf      []Data
	capacity int
}

var _ Writer = (*BufferedWriter)(nil)
var _ BatchWriter = (*BufferedWriter)(nil)

// NewBufferedWriter .
func NewBufferedWriter(w BatchWriter, capacity int) *BufferedWriter {
	return &BufferedWriter{
		w:        w,
		buf:      make([]Data, 0, capacity),
		capacity: capacity,
	}
}

// Write .
func (b *BufferedWriter) Write(data Data) error {
	if len(b.buf)+1 > b.capacity {
		err := b.Flush()
		if err != nil {
			return err
		}
	}
	b.buf = append(b.buf, data)
	return nil
}

// WriteN returns the number of buffers written to the data.
// if a Flush error occurs, the error will be returned
func (b *BufferedWriter) WriteN(data ...Data) (int, error) {
	alen := len(b.buf)
	blen := len(data)
	if alen+blen < b.capacity {
		b.buf = append(b.buf, data...)
		return blen, nil
	}
	writes := 0
	if alen >= b.capacity {
		// never reached
		err := b.Flush()
		if err != nil {
			return 0, nil
		}
	} else if alen > 0 {
		writes = b.capacity - alen
		b.buf = append(b.buf, data[0:writes]...)
		err := b.Flush()
		if err != nil {
			return writes, err
		}
		data = data[writes:]
		blen -= writes
	}
	for blen > b.capacity {
		b.buf = append(b.buf, data[0:b.capacity]...)
		writes += b.capacity
		err := b.Flush()
		if err != nil {
			return writes, err
		}
		data = data[b.capacity:]
		blen -= b.capacity
	}
	if blen > 0 {
		b.buf = append(b.buf, data...)
		writes += blen
	}
	return writes, nil
}

// Flush .
func (b *BufferedWriter) Flush() error {
	l := len(b.buf)
	if l > 0 {
		for {
			n, err := b.w.WriteN(b.buf...)
			if err != nil {
				return err
			}
			if n <= 0 {
				return fmt.Errorf("flush data got number(%d)", n)
			}
			if n != len(b.buf) {
				b.buf = b.buf[0 : l-n]
				continue
			}
			b.buf = b.buf[0:0]
			break
		}
	}
	return nil
}

// Size .
func (b *BufferedWriter) Size() int { return len(b.buf) }

// Data .
func (b *BufferedWriter) Data() []Data { return b.buf }

// Close .
func (b *BufferedWriter) Close() error {
	return errorx.NewMultiError(b.Flush(), b.w.Close()).MaybeUnwrap()
}
