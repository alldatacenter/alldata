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

package compressor

import (
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
)

type gzipEncoder struct {
	buf    *bytes.Buffer
	writer *gzip.Writer
}

func NewGzipEncoder(level int) *gzipEncoder {
	buf := bytes.NewBuffer(nil)
	gc, _ := gzip.NewWriterLevel(buf, level)
	return &gzipEncoder{
		buf:    buf,
		writer: gc,
	}
}

func (ge *gzipEncoder) Compress(buf []byte) ([]byte, error) {
	defer func() {
		ge.buf.Reset()
		ge.writer.Reset(ge.buf)
	}()
	if _, err := ge.writer.Write(buf); err != nil {
		return nil, fmt.Errorf("gizp write data: %w", err)
	}
	if err := ge.writer.Flush(); err != nil {
		return nil, fmt.Errorf("gzip flush data: %w", err)
	}
	if err := ge.writer.Close(); err != nil {
		return nil, fmt.Errorf("gzip close: %w", err)
	}
	tmp := bytes.NewBuffer(nil)
	if _, err := io.Copy(tmp, ge.buf); err != nil {
		return nil, fmt.Errorf("gzip copy: %w", err)
	}
	return tmp.Bytes(), nil
}
