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

package storage

import (
	"io"
	"os"
)

type FS struct{}

func NewFS() *FS {
	return &FS{}
}

func (fs *FS) Type() Type {
	return TypeFileSystem
}

func (fs *FS) Read(path string) (io.Reader, error) {
	return os.Open(path)
}

func (fs *FS) Write(path string, r io.Reader) error {
	dst, err := os.OpenFile(path, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0644)
	if err != nil {
		return err
	}
	if _, err = io.Copy(dst, r); err != nil {
		return err
	}
	return dst.Sync()
}

func (fs *FS) Delete(path string) error {
	return os.Remove(path)
}
