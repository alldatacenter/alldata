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

package tool

import (
	"math"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIsTextFile(t *testing.T) {
	tests := []struct {
		desc     string
		data     []byte
		expected bool
	}{
		{"Empty", []byte{}, true},
		{"HTML document #1", []byte(`<HtMl><bOdY>blah blah blah</body></html>`), true},
		{"HTML document #2", []byte(`<HTML></HTML>`), true},
		{"HTML document #3 (leading whitespace)", []byte(`   <!DOCTYPE HTML>...`), true},
		{"HTML document #4 (leading CRLF)", []byte("\r\n<html>..."), true},
		{"Plain text", []byte(`This is not HTML. It has ☃ though.`), true},
		{"XML", []byte("\n<?xml!"), true},
		{"Binary", []byte{1, 2, 3}, false},
		{"BMP image", []byte("BM..."), false},
		{"MP4 video", []byte("\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom<\x06t\xbfmdat"), false},
		{"pdf", []byte("%PDF-"), false},
	}
	for _, v := range tests {
		assert.Equal(t, v.expected, IsTextFile(v.data))
	}
}

func TestIsImageFile(t *testing.T) {
	tests := []struct {
		desc     string
		data     []byte
		expected bool
	}{
		{"Windows icon", []byte("\x00\x00\x01\x00"), true},
		{"Windows cursor", []byte("\x00\x00\x02\x00"), true},
		{"BMP image", []byte("BM..."), true},
		{"GIF 87a", []byte(`GIF87a`), true},
		{"GIF 89a", []byte(`GIF89a...`), true},
		{"WEBP image", []byte("RIFF\x00\x00\x00\x00WEBPVP"), true},
		{"PNG image", []byte("\x89PNG\x0D\x0A\x1A\x0A"), true},
		{"JPEG image", []byte("\xFF\xD8\xFF"), true},
		{"Binary", []byte{1, 2, 3}, false},
		{"Empty", []byte{}, false},
		{"MP4 video", []byte("\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom<\x06t\xbfmdat"), false},
		{"pdf", []byte("%PDF-"), false},
	}
	for _, v := range tests {
		assert.Equal(t, v.expected, IsImageFile(v.data))
	}
}

func TestIsPDFFile(t *testing.T) {
	tests := []struct {
		desc     string
		data     []byte
		expected bool
	}{
		{"pdf", []byte("%PDF-"), true},
		{"Binary", []byte{1, 2, 3}, false},
		{"Empty", []byte{}, false},
		{"MP4 video", []byte("\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom<\x06t\xbfmdat"), false},
		{"WEBP image", []byte("RIFF\x00\x00\x00\x00WEBPVP"), false},
	}
	for _, v := range tests {
		assert.Equal(t, v.expected, IsPDFFile(v.data))
	}
}

func TestIsVideoFile(t *testing.T) {
	tests := []struct {
		desc     string
		data     []byte
		expected bool
	}{
		{"MP4 video", []byte("\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom<\x06t\xbfmdat"), true},
		{"AVI video #1", []byte("RIFF,O\n\x00AVI LISTÀ"), true},
		{"AVI video #2", []byte("RIFF,\n\x00\x00AVI LISTÀ"), true},
		{"pdf", []byte("%PDF-"), false},
		{"Binary", []byte{1, 2, 3}, false},
		{"Empty", []byte{}, false},
		{"WEBP image", []byte("RIFF\x00\x00\x00\x00WEBPVP"), false},
	}
	for _, v := range tests {
		assert.Equal(t, v.expected, IsVideoFile(v.data))
	}
}

func TestFileSize(t *testing.T) {
	tests := []struct {
		size     int64
		expected string
	}{
		{9, "9 B"},
		{1024 * 9, "9.0 KB"},
		{1024 * 10, "10 KB"},
		{1024 * 1024 * 9, "9.0 MB"},
		{1024 * 1024 * 10, "10 MB"},
		{1024 * 1024 * 1024 * 9, "9.0 GB"},
		{1024 * 1024 * 1024 * 10, "10 GB"},
		{1024 * 1024 * 1024 * 1024 * 9, "9.0 TB"},
		{1024 * 1024 * 1024 * 1024 * 10, "10 TB"},
		{1024 * 1024 * 1024 * 1024 * 1024 * 9, "9.0 PB"},
		{1024 * 1024 * 1024 * 1024 * 1024 * 10, "10 PB"},
		{math.MaxInt64, "8.0 EB"},
	}
	for _, v := range tests {
		assert.Equal(t, v.expected, FileSize(v.size))
	}
}

func TestIsReadmeFile(t *testing.T) {
	tests := []struct {
		name string
		want bool
	}{
		{"readme.txt", true},
		{"README.txt", true},
		{"read.txt", false},
	}
	for _, v := range tests {
		assert.Equal(t, IsReadmeFile(v.name), v.want)
	}
}
