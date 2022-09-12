// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Package codec defines the encoding and decoding logic between TubeMQ.
// If the protocol of encoding and decoding is changed, only this package
// will need to be changed.
package codec

// Response is the abstraction of the transport response.
type Response interface {
	// GetSerialNo returns the `serialNo` of the corresponding request.
	GetSerialNo() uint32
	// GetBuffer returns the body of the response.
	GetBuffer() []byte
}

// Decoder is the abstraction of the decoder which is used to decode the response.
type Decoder interface {
	// Decode will decode the response to frame head and body.
	Decode() (Response, error)
}

// RPCRequest represents the RPC request protocol.
type RPCRequest interface {
	// GetSerialNo returns the `serialNo` of the corresponding request.
	GetSerialNo() uint32
	// Marshal marshals the RPCRequest to bytes.
	Marshal() ([]byte, error)
}

// RPCResponse represents the RPC response from TubeMQ.
type RPCResponse interface {
	// Unmarshal unmarshals the bytes array.
	Unmarshal([]byte) error
	// GetDebugMsg returns the debug msg.
	GetDebugMsg() string
}
