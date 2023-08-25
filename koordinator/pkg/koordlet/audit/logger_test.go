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
	"bytes"
	"io"
	"os"
	"strings"
	"testing"
)

func TestLogWriter(t *testing.T) {
	tempDir := t.TempDir()

	name := tempDir + "/audit.log"
	writer, err := OpenLogWriter(name)
	if err != nil {
		t.Fatal("failed to create log writer", err)
	}
	defer writer.Close()

	writer.Append([]byte("1 line"))
	if writer.Size() != len("1 line")+1 {
		t.Errorf("unexpected size: %d", writer.Size())
	}

	writer.Flush()
	fileContent, err := os.ReadFile(name)
	if err != nil {
		t.Fatal("failed to read file", err)
	}
	line := strings.TrimSpace(string(fileContent))
	if line != "1 line" {
		t.Errorf("failed to load line, expected 1 line actual %s", line)
	}
}

func TestLogReader(t *testing.T) {
	tempDir := t.TempDir()

	name := tempDir + "/audit.log"
	writer, err := OpenLogWriter(name)
	if err != nil {
		t.Fatal("failed to create log writer", err)
	}
	defer writer.Close()

	blocks := make([][]byte, 2)
	blocks[0] = make([]byte, 127)
	blocks[1] = make([]byte, 127)
	for i := 0; i < 127; i++ {
		blocks[0][i] = 'a'
		blocks[1][i] = 'b'
	}
	for i := 0; i < ChunkSize/128*2; i++ {
		writer.Append(blocks[i%2])
	}
	writer.Flush()

	reader, err := OpenlogReader(name)
	if err != nil {
		t.Fatal("failed to open log", err)
	}
	defer reader.Close()
	var lines [][]byte
	for i := 0; ; i++ {
		line, err := reader.Read()
		if err == io.EOF {
			break
		}
		if err != nil {
			t.Error("failed to read line", err)
		}

		if reader.Offset() != i+1 {
			t.Errorf("unexpected offset expected %d actual %d", i+1, reader.Offset())
		}

		lines = append(lines, line)

		if !bytes.Equal(line, blocks[(i+1)%2]) {
			t.Errorf("failed to read line expected\n: %s\nactual: %v", blocks[(i+1)%2], line)
		}
	}
	if len(lines) != ChunkSize/128*2 {
		t.Errorf("unexpected chunk size expected: %d actual: %d", ChunkSize/128*2, len(lines))
	}
}
