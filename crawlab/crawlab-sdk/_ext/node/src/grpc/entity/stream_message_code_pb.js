// source: entity/stream_message_code.proto
/**
 * @fileoverview
 * @enhanceable
 * @suppress {missingRequire} reports error on implicit type usages.
 * @suppress {messageConventions} JS Compiler reports an error if a variable or
 *     field starts with 'MSG_' and isn't a translatable message.
 * @public
 */
// GENERATED CODE -- DO NOT EDIT!
/* eslint-disable */
// @ts-nocheck

var jspb = require('google-protobuf');
var goog = jspb;
var global = Function('return this')();

goog.exportSymbol('proto.grpc.StreamMessageCode', null, global);
/**
 * @enum {number}
 */
proto.grpc.StreamMessageCode = {
  PING: 0,
  RUN_TASK: 1,
  CANCEL_TASK: 2,
  INSERT_DATA: 3,
  INSERT_LOGS: 4,
  SEND_EVENT: 5,
  INSTALL_PLUGIN: 6,
  UNINSTALL_PLUGIN: 7,
  START_PLUGIN: 8,
  STOP_PLUGIN: 9,
  CONNECT: 10,
  DISCONNECT: 11,
  SEND: 12
};

goog.object.extend(exports, proto.grpc);
