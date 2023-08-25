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
	"fmt"
	"os"
	"path/filepath"
	"testing"
)

func TestEventLogger(t *testing.T) {
	tempDir := t.TempDir()

	logger := NewEventLogger(tempDir, 10, 0)
	logger.Log(0, &Event{Reason: "hello"})
	logger.Log(0, &Event{Reason: "world"})
	logger.Close()

	content, err := os.ReadFile(fmt.Sprintf("%s/audit.log", tempDir))
	if err != nil {
		t.Fatal(err)
	}
	tokens := bytes.Split(bytes.TrimSpace(content), []byte{'\n'})
	if len(tokens) != 2 {
		t.Errorf("failed load content: %s", content)
	}
}

func TestFluentEventLogger(t *testing.T) {
	tempDir := t.TempDir()

	logger := NewFluentEventLogger(tempDir, 10, 0)
	defer logger.Close()

	block1KB := makeBlock(1023, 'a')
	for i := 0; i < 1024*12; i++ {
		logger.V(0).Node().Message(string(block1KB)).Do()
	}

	pattern := fmt.Sprintf("%s/audit-*.log", tempDir)
	files, err := filepath.Glob(pattern)
	if err != nil {
		t.Fatal(err)
	}
	if len(files) != 4 {
		t.Errorf("clean old files failed, actual files: %v", files)
	}
}

func makeBlock(n int, b byte) []byte {
	block := make([]byte, n)
	for i := 0; i < n; i++ {
		block[i] = b
	}
	return block
}

func TestReverseEventReaderSingleFile(t *testing.T) {
	tempDir := t.TempDir()

	logger := NewFluentEventLogger(tempDir, 10, 0)

	blocks := make([][]byte, 26)
	for i := 0; i < 26; i++ {
		blocks[i] = makeBlock(63, 'a'+byte(i))
		logger.V(0).Node().Message(string(blocks[i])).Do()
	}
	logger.Close()

	reader := NewEventReader(tempDir)
	iter := reader.NewReverseInterator()
	defer iter.Close()
	for i := 0; i < 26; i++ {
		event, err := iter.Next()
		if err != nil {
			break
		}
		if event.Message != string(blocks[25-i]) {
			t.Errorf("unexpected message, expected:\n%s\nactual:\n%s", blocks[25-i], event.Message)
		}
	}
}

func TestReverseEventReaderMultiFile(t *testing.T) {
	tempDir := t.TempDir()

	logger := NewFluentEventLogger(tempDir, 10, 0)

	block1KB := makeBlock(1023, 'a')
	for i := 0; i < 1024*12; i++ {
		logger.V(0).Node().Message(string(block1KB)).Do()
	}

	logger.Close()

	reader := NewEventReader(tempDir)
	iter := reader.NewReverseInterator()
	defer iter.Close()
	count := 0
	for {
		_, err := iter.Next()
		if err != nil {
			break
		}
		count++
	}
	if count < MaxFileSize/1024 {
		t.Errorf("failed to read multi files, actual got %d events", count)
	}
}
