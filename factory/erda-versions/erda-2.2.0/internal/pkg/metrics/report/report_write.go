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

package report

import "fmt"

type reportWrite struct {
	capacity int
}

func NewReportWrite(capacity int) *reportWrite {
	return &reportWrite{
		capacity: capacity,
	}
}

func (w *reportWrite) Write(data interface{}) error {
	return nil
}
func (w *reportWrite) WriteN(data ...interface{}) (int, error) {
	if w.capacity <= 0 {
		err := fmt.Errorf("buffer max capacity")
		return 0, err
	}
	w.capacity -= len(data)
	return len(data), nil
}
func (w reportWrite) Close() error {
	return nil
}
