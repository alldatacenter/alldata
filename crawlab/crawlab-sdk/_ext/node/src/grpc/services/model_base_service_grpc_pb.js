// GENERATED CODE -- DO NOT EDIT!

'use strict';
var grpc = require('@grpc/grpc-js');
var entity_request_pb = require('../entity/request_pb.js');
var entity_response_pb = require('../entity/response_pb.js');

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


var ModelBaseServiceService = exports.ModelBaseServiceService = {
  getById: {
    path: '/grpc.ModelBaseService/GetById',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  get: {
    path: '/grpc.ModelBaseService/Get',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  getList: {
    path: '/grpc.ModelBaseService/GetList',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  deleteById: {
    path: '/grpc.ModelBaseService/DeleteById',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  delete: {
    path: '/grpc.ModelBaseService/Delete',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  deleteList: {
    path: '/grpc.ModelBaseService/DeleteList',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  forceDeleteList: {
    path: '/grpc.ModelBaseService/ForceDeleteList',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  updateById: {
    path: '/grpc.ModelBaseService/UpdateById',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  update: {
    path: '/grpc.ModelBaseService/Update',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  updateDoc: {
    path: '/grpc.ModelBaseService/UpdateDoc',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  insert: {
    path: '/grpc.ModelBaseService/Insert',
    requestStream: false,
    responseStream: false,
    requestType: entity_request_pb.Request,
    responseType: entity_response_pb.Response,
    requestSerialize: serialize_grpc_Request,
    requestDeserialize: deserialize_grpc_Request,
    responseSerialize: serialize_grpc_Response,
    responseDeserialize: deserialize_grpc_Response,
  },
  count: {
    path: '/grpc.ModelBaseService/Count',
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

exports.ModelBaseServiceClient = grpc.makeGenericClientConstructor(ModelBaseServiceService);
