// GENERATED CODE -- DO NOT EDIT!

'use strict';
var grpc = require('@grpc/grpc-js');
var entity_stream_message_pb = require('../entity/stream_message_pb.js');

function serialize_grpc_StreamMessage(arg) {
  if (!(arg instanceof entity_stream_message_pb.StreamMessage)) {
    throw new Error('Expected argument of type grpc.StreamMessage');
  }
  return Buffer.from(arg.serializeBinary());
}

function deserialize_grpc_StreamMessage(buffer_arg) {
  return entity_stream_message_pb.StreamMessage.deserializeBinary(new Uint8Array(buffer_arg));
}


var MessageServiceService = exports.MessageServiceService = {
  connect: {
    path: '/grpc.MessageService/Connect',
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

exports.MessageServiceClient = grpc.makeGenericClientConstructor(MessageServiceService);
