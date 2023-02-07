// GENERATED CODE -- DO NOT EDIT!

'use strict';
var grpc = require('@grpc/grpc-js');
var entity_plugin_request_pb = require('../entity/plugin_request_pb.js');
var entity_response_pb = require('../entity/response_pb.js');
var entity_stream_message_pb = require('../entity/stream_message_pb.js');

function serialize_grpc_PluginRequest(arg) {
  if (!(arg instanceof entity_plugin_request_pb.PluginRequest)) {
    throw new Error('Expected argument of type grpc.PluginRequest');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_grpc_PluginRequest(buffer_arg) {
  return entity_plugin_request_pb.PluginRequest.deserializeBinary(new Uint8Array(buffer_arg));
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


var PluginServiceService = exports.PluginServiceService = {
  register: {
    path: '/grpc.PluginService/Register',
    requestStream: false,
    responseStream: false,
    requestType: entity_plugin_request_pb.PluginRequest,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_PluginRequest,
    requestDeserialize: deserialize_grpc_PluginRequest,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  subscribe: {
    path: '/grpc.PluginService/Subscribe',
    requestStream: false,
    responseStream: true,
    requestType: entity_plugin_request_pb.PluginRequest,
    responseType: entity_stream_message_pb.StreamMessage,
    requestSerialize: serialize_grpc_PluginRequest,
    requestDeserialize: deserialize_grpc_PluginRequest,
    responseSerialize: serialize_grpc_StreamMessage,
    responseDeserialize: deserialize_grpc_StreamMessage,
  },
  poll: {
    path: '/grpc.PluginService/Poll',
    requestStream: true,
    responseStream: true,
    requestType: entity_stream_message_pb.StreamMessage,
    responseType: entity_stream_message_pb.StreamMessage,
    requestSerialize: serialize_grpc_StreamMessage,
    requestDeserialize: deserialize_grpc_StreamMessage,
    responseSerialize: serialize_grpc_StreamMessage,
    responseDeserialize: deserialize_grpc_StreamMessage,
  },
};

exports.PluginServiceClient = grpc.makeGenericClientConstructor(PluginServiceService);
