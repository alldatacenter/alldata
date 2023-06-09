/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <Core/Types.h>
#include <Protos/optimizer_statistics.pb.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/message.h>
#include <Poco/Dynamic/Var.h>
#include <Poco/JSON/Object.h>
#include <Poco/JSON/Parser.h>
namespace DB
{
using SerdeDataType = Protos::SerdeDataType;
SerdeDataType SerdeDataTypeFromString(const String & tag_string);
String SerdeDataTypeToString(SerdeDataType tag);
using PArray = Poco::JSON::Array;
using PObject = Poco::JSON::Object;
using PVar = Poco::Dynamic::Var;
using Pparser = Poco::JSON::Parser;

class Json2Pb
{
public:
    typedef ::google::protobuf::Message ProtobufMsg;
    typedef ::google::protobuf::Reflection ProtobufReflection;
    typedef ::google::protobuf::FieldDescriptor ProtobufFieldDescriptor;
    typedef ::google::protobuf::Descriptor ProtobufDescriptor;

public:
    static void pbMsg2JsonStr(const ProtobufMsg &, String &, bool enum_str = false);
    static bool jsonStr2PbMsg(const String &, ProtobufMsg &, bool str_enum = false);
    static bool json2PbMsg(const Poco::JSON::Object &, ProtobufMsg &, bool);
    static void pbMsg2Json(const ProtobufMsg &, Poco::JSON::Object &, bool);

private:
    static bool json2RepeatedMessage(
        const Poco::JSON::Array &,
        ProtobufMsg &,
        const ProtobufFieldDescriptor * field,
        const ProtobufReflection * reflection,
        bool str_enum);
    static void repeatedMessage2Json(
        const ProtobufMsg & message,
        const ProtobufFieldDescriptor * field,
        const ProtobufReflection * reflection,
        Poco::JSON::Array & json,
        bool enum_str);
};
}
