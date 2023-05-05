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

package dumpstack

import (
	"fmt"
	"os"
	"os/signal"
	"runtime"
	"syscall"
)

func Open() {
	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGUSR1)

	go func() {
		for range c {
			dumpStacks()
		}
	}()
}

func dumpStacks() {
	var (
		buf  []byte
		size int
	)

	bufLen := 16 * 1024 // 16K

	for size == len(buf) {
		buf = make([]byte, bufLen)
		size = runtime.Stack(buf, true)
		bufLen *= 2
	}
	buf = buf[:size]

	fmt.Printf("=== BEGIN goroutine stack === \n%s\n=== END goroutine stack ===\n", buf)
}
