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

import "errors"

type (
	// Data .
	Data = interface{}

	// Writer .
	Writer interface {
		Write(val Data) error
		Close() error
	}

	// BatchWriter .
	BatchWriter interface {
		WriteN(vals ...Data) (int, error)
		Close() error
	}

	// Iterator .
	Iterator interface {
		First() bool
		Last() bool
		Next() bool
		Prev() bool
		Value() Data
		Error() error
		Close() error
	}

	Counter interface {
		Total() (int64, error)
	}

	// Flusher
	Flusher interface {
		Flush() error
	}

	// Matcher .
	Matcher interface {
		Match(val Data) bool
	}

	// Comparer .
	Comparer interface {
		Compare(a, b Data) int
	}
)

var (
	// ErrInvalidData .
	ErrInvalidData = errors.New("invalid data")
	// ErrIteratorClosed .
	ErrIteratorClosed = errors.New("iterator closed")
	// ErrWriterClosed .
	ErrWriterClosed = errors.New("writer closed")
	// ErrOpNotSupported .
	ErrOpNotSupported = errors.New("operation not supported")
)
