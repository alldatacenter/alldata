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

package errors

import (
	"errors"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/structpb"
)

// ToGrpcError .
func ToGrpcError(err error) error {
	if err == nil {
		return err
	}
	fields := make(map[string]interface{})
	code := codes.Unknown
	var typ string
	switch e := err.(type) {
	case *UnauthorizedError:
		typ = "UnauthorizedError"
		fields["reason"] = e.Reason
		code = codes.Unauthenticated
	case *PermissionError:
		typ = "PermissionError"
		fields["resource"] = e.Resource
		fields["action"] = e.Action
		fields["reason"] = e.Reason
		code = codes.PermissionDenied
	case *NotFoundError:
		typ = "NotFoundError"
		fields["resource"] = e.Resource
		code = codes.NotFound
	case *AlreadyExistsError:
		typ = "AlreadyExistsError"
		fields["resource"] = e.Resource
		code = codes.AlreadyExists
	case *InvalidParameterError:
		typ = "InvalidParameterError"
		fields["name"] = e.Name
		fields["message"] = e.Message
		code = codes.InvalidArgument
	case *MissingParameterError:
		typ = "MissingParameterError"
		fields["name"] = e.Name
		code = codes.InvalidArgument
	case *ParameterTypeError:
		typ = "ParameterTypeError"
		fields["name"] = e.Name
		fields["validType"] = e.ValidType
		code = codes.InvalidArgument
	case *InternalServerError:
		typ = "InternalServerError"
		fields["cause"] = e.Cause.Error()
		fields["message"] = e.Message
		code = codes.Internal
	case *DatabaseError:
		typ = "DatabaseError"
		fields["cause"] = e.Cause.Error()
		code = codes.Internal
	case *ServiceInvokingError:
		typ = "ServiceInvokingError"
		fields["cause"] = e.Cause.Error()
		fields["service"] = e.Service
		code = codes.Unavailable
	case *UnimplementedError:
		typ = "UnimplementedError"
		fields["service"] = e.Service
		code = codes.Unimplemented
	case *WarnError:
		typ = "WarnError"
		fields["name"] = e.Name
		fields["message"] = e.Message
		code = codes.Internal
	}
	details := map[string]interface{}{
		"type":   typ,
		"fields": fields,
	}
	detailsValue, _ := structpb.NewValue(details)
	se, _ := status.New(code, err.Error()).WithDetails(detailsValue)
	return se.Err()
}

// FromGrpcError .
func FromGrpcError(err error) error {
	se, ok := status.FromError(err)
	if !ok {
		return NewInternalServerError(err)
	}
	list := se.Details()
	if len(list) <= 0 {
		return NewInternalServerError(err)
	}
	details, ok := list[0].(*structpb.Value)
	if !ok {
		return NewInternalServerError(err)
	}
	m, ok := details.AsInterface().(map[string]interface{})
	if !ok {
		return NewInternalServerError(err)
	}
	typ, _ := m["type"].(string)
	fields, _ := m["fields"].(map[string]interface{})
	switch typ {
	case "UnauthorizedError":
		err := &UnauthorizedError{}
		err.Reason, _ = fields["reason"].(string)
		return err
	case "PermissionError":
		err := &PermissionError{}
		err.Resource, _ = fields["resource"].(string)
		err.Action, _ = fields["action"].(string)
		err.Reason, _ = fields["reason"].(string)
		return err
	case "NotFoundError":
		err := &NotFoundError{}
		err.Resource, _ = fields["resource"].(string)
		return err
	case "AlreadyExistsError":
		err := &AlreadyExistsError{}
		err.Resource, _ = fields["resource"].(string)
		return err
	case "InvalidParameterError":
		err := &InvalidParameterError{}
		err.Name, _ = fields["name"].(string)
		err.Message, _ = fields["message"].(string)
		return err
	case "MissingParameterError":
		err := &MissingParameterError{}
		err.Name, _ = fields["name"].(string)
		return err
	case "ParameterTypeError":
		err := &ParameterTypeError{}
		err.Name, _ = fields["name"].(string)
		err.ValidType, _ = fields["validType"].(string)
		return err
	case "InternalServerError":
		err := &InternalServerError{}
		cause, _ := fields["cause"].(string)
		if len(cause) > 0 {
			err.Cause = errors.New(cause)
		}
		err.Message, _ = fields["message"].(string)
		return err
	case "DatabaseError":
		err := &DatabaseError{}
		cause, _ := fields["cause"].(string)
		if len(cause) > 0 {
			err.Cause = errors.New(cause)
		}
		return err
	case "ServiceInvokingError":
		err := &ServiceInvokingError{}
		cause, _ := fields["cause"].(string)
		if len(cause) > 0 {
			err.Cause = errors.New(cause)
		}
		err.Service, _ = fields["service"].(string)
		return err
	case "UnimplementedError":
		err := &UnimplementedError{}
		err.Service, _ = fields["service"].(string)
		return err
	case "WarnError":
		err := &WarnError{}
		err.Name, _ = fields["name"].(string)
		err.Message, _ = fields["message"].(string)
		return err
	}
	return NewInternalServerError(err)
}
