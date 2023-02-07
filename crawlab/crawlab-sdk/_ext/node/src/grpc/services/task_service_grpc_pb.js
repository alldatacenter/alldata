// GENERATED CODE -- DO NOT EDIT!

'use strict';
var grpc = require('@grpc/grpc-js');
var entity_request_pb = require('../entity/request_pb.js');
var entity_response_pb = require('../entity/response_pb.js');
var entity_stream_message_pb = require('../entity/stream_message_pb.js');

function serialize_grpc_Request(arg) {
  if (!(arg instanceof entity_request_pb.Request)) {
    throw new Error('Expected argument of type grpc.Request');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_grpc_Request(buffer_arg) {
  return entity_request_pb.Request.deserializeBinary(new Uint8Array(buffer_arg));
}

function serialize_grpc_Response(arg) {
  if (!(arg instanceof entity_response_pb.Response)) {
    throw new Error('Expected argument of type grpc.Response');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_grpc_Response(buffer_arg) {
  return entity_response_pb.Response.deserializeBinary(new Uint8Array(buffer_arg));
}

function serialize_grpc_StreamMessage(arg) {
  if (!(arg instanceof entity_stream_message_pb.StreamMessage)) {
    throw new Error('Expected argument of type grpc.StreamMessage');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_grpc_StreamMessage(buffer_arg) {
  return entity_stream_message_pb.StreamMessage.deserializeBinary(new Uint8Array(buffer_arg));
}


var TaskServiceService = exports.TaskServiceService = {
  subscribe: {
    path: '/grpc.TaskService/Subscribe',
    requestStream: true,
    responseStream: false,
    requestType: entity_stream_message_pb.StreamMessage,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_StreamMessage,
    requestDeserialize: deserialize_grpc_StreamMessage,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  getDataSource: {
    path: '/grpc.TaskService/GetDataSource',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
};

exports.TaskServiceClient = grpc.makeGenericClientConstructor(TaskServiceService);
