/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package audit

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"os"
	"time"
)

var (
	ChunkSize = 4096
)

type LogWriter interface {
	Append([]byte) error
	Size() int
	Flush() error
	Close() error
}

type LogReader interface {
	Read() ([]byte, error)
	Offset() int
	Close() error
}

func OpenLogWriter(name string) (LogWriter, error) {
	file, err := os.OpenFile(name, os.O_RDWR|os.O_CREATE|os.O_APPEND, 0666)
	if err != nil {
		return nil, err
	}
	return &logWriter{
		size:   0,
		file:   file,
		writer: bufio.NewWriterSize(file, 1024*1024),
	}, nil
}

type logWriter struct {
	size          int
	lastFlushTime time.Time

	file   *os.File
	writer *bufio.Writer
}

func (w *logWriter) Append(log []byte) error {
	_, err := w.writer.Write(log)
	if err != nil {
		return err
	}
	w.size += len(log)
	_, err = w.writer.Write([]byte{'\n'})
	if err == nil {
		w.size++
	}
	if time.Since(w.lastFlushTime) >= time.Second*5 {
		w.writer.Flush()
		w.lastFlushTime = time.Now()
	}

	return err
}

func (w *logWriter) Size() int {
	return w.size
}

func (w *logWriter) Flush() error {
	return w.writer.Flush()
}

func (w *logWriter) Close() error {
	w.writer.Flush()
	return w.file.Close()
}

func OpenlogReader(name string) (LogReader, error) {
	file, err := os.Open(name)
	if err != nil {
		return nil, err
	}
	fileInfo, err := file.Stat()
	if err != nil {
		return nil, err
	}
	return &logReader{
		file:       file,
		fileOffset: int(fileInfo.Size()),
		offset:     0,
	}, nil
}

type logReader struct {
	file       *os.File
	fileOffset int
	offset     int

	current *chunkFile
}

func (r *logReader) readChunks() (*chunkFile, error) {
	if r.fileOffset == 0 {
		return nil, io.EOF
	}

	chunk, err := readChunk(r.file, r.fileOffset)
	if err == nil {
		r.fileOffset -= chunk.Size()
	}
	return chunk, err
}

func (r *logReader) Read() ([]byte, error) {
	if r.fileOffset == 0 && (r.current == nil || r.current.RemainLines() == 0) {
		return nil, io.EOF
	}

	if r.current == nil || r.current.RemainLines() == 0 {
		chunk, err := r.readChunks()
		if err != nil {
			return nil, err
		}
		r.current = chunk
	}

	line, err := r.current.Read()
	if err == nil {
		r.offset++
	}
	return line, err
}

func (r *logReader) Offset() int {
	return r.offset
}

func (r *logReader) Close() error {
	return r.file.Close()
}

func readChunk(file *os.File, endOffset int) (*chunkFile, error) {
	offset := endOffset/ChunkSize*ChunkSize - ChunkSize
	if offset < 0 {
		offset = 0
	}

	_, err := file.Seek(int64(offset), 0)
	if err != nil {
		return nil, err
	}
	chunkData := make([]byte, endOffset-offset)
	n, err := file.Read(chunkData)
	if err != nil {
		return nil, err
	}

	chunkData = chunkData[:n]
	startOffset := endOffset - len(chunkData)
	reader := bytes.NewBuffer(chunkData)
	if offset > 0 {
		// shift to first '\n'
		line, err := reader.ReadBytes('\n')
		if err != nil && err != io.EOF {
			return nil, err
		}
		startOffset += len(line)
	}

	if startOffset == endOffset {
		return nil, fmt.Errorf("can not find \\n in this chunk")
	}

	lines := make([][]byte, 0, 128)
	for {
		line, err := reader.ReadBytes('\n')
		if err != nil && err != io.EOF {
			return nil, err
		}

		line = bytes.TrimSpace(line)
		if len(line) > 0 {
			lines = append(lines, line)
		}

		if err == io.EOF {
			break
		}
	}
	return &chunkFile{
		startOffset: startOffset,
		endOffset:   endOffset,
		lines:       lines,
		offset:      0,
	}, nil
}

type chunkFile struct {
	startOffset int
	endOffset   int
	lines       [][]byte
	offset      int
}

func (r *chunkFile) Read() ([]byte, error) {
	if r.RemainLines() == 0 {
		return nil, io.EOF
	}

	l := r.lines[len(r.lines)-1-r.offset]
	r.offset++
	return l, nil
}

func (r *chunkFile) Size() int {
	return r.endOffset - r.startOffset
}

func (r *chunkFile) RemainLines() int {
	return len(r.lines) - r.offset
}
