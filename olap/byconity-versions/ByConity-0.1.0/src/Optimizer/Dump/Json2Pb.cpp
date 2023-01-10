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

#include <Optimizer/Dump/Json2Pb.h>
#include <brpc/server.h>
#include <Poco/Util/HelpFormatter.h>

namespace DB
{
SerdeDataType SerdeDataTypeFromString(const String & tag_string)
{
    const google::protobuf::EnumDescriptor * descriptor = Protos::SerdeDataType_descriptor();
    return static_cast<SerdeDataType>(descriptor->FindValueByName(tag_string)->number());
}

String SerdeDataTypeToString(SerdeDataType tag)
{
    const google::protobuf::EnumDescriptor * descriptor = Protos::SerdeDataType_descriptor();
    return descriptor->FindValueByNumber(tag)->name();
}
//Set to true if enum is string, set to false if enum is int
void Json2Pb::pbMsg2JsonStr(const ProtobufMsg & src, String & dst, bool enum_str)
{
    Poco::JSON::Object object;
    pbMsg2Json(src, object, enum_str);
    std::stringstream json_string;
    object.stringify(json_string);
    dst = json_string.str();
}

bool Json2Pb::jsonStr2PbMsg(const String & src, ProtobufMsg & dst, bool str_enum)
{
    Poco::JSON::Parser parser;
    Poco::Dynamic::Var value = parser.parse(src);

    if (true != json2PbMsg(*value.extract<Poco::JSON::Object::Ptr>(), dst, str_enum))
        return false;
    return true;
}


bool Json2Pb::json2PbMsg(const Poco::JSON::Object & src, ProtobufMsg & dst, bool str_enum = false)
{
    const ProtobufDescriptor * descriptor = dst.GetDescriptor();
    const ProtobufReflection * reflection = dst.GetReflection();
    if (nullptr == descriptor || nullptr == reflection)
        return false;

    Int32 count = descriptor->field_count();
    for (Int32 i = 0; i < count; ++i)
    {
        const ProtobufFieldDescriptor * field = descriptor->field(i);
        if (nullptr == field)
            continue;

        if (!src.has(field->name()))
        {
            continue;
        }
        const Poco::Dynamic::Var & value = src.get(field->name());

        if (field->is_repeated())
        {
            if (!value.isArray())
            {
                return false;
            }
            else
            {
                json2RepeatedMessage(*value.extract<Poco::JSON::Array::Ptr>(), dst, field, reflection, str_enum);
                continue;
            }
        }
        switch (field->type())
        {
            case ProtobufFieldDescriptor::TYPE_BOOL: {
                if (value.isBoolean())
                {
                    reflection->SetBool(&dst, field, value.convert<bool>());
                }

                else if (value.isInteger())
                {
                    reflection->SetBool(&dst, field, value.isInteger());
                }

                else if (value.isString())
                {
                    if (value.toString() == "true")
                        reflection->SetBool(&dst, field, true);
                    else if (value.toString() == "false")
                        reflection->SetBool(&dst, field, false);
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_INT32:
            case ProtobufFieldDescriptor::TYPE_SINT32:
            case ProtobufFieldDescriptor::TYPE_SFIXED32: {
                if (value.isSigned())
                    reflection->SetInt32(&dst, field, value.convert<Poco::Int32>());
                break;
            }

            case ProtobufFieldDescriptor::TYPE_UINT32:
            case ProtobufFieldDescriptor::TYPE_FIXED32: {
                if (value.isInteger())
                    reflection->SetUInt32(&dst, field, value.convert<Poco::UInt32>());
                break;
            }

            case ProtobufFieldDescriptor::TYPE_INT64:
            case ProtobufFieldDescriptor::TYPE_SINT64:
            case ProtobufFieldDescriptor::TYPE_SFIXED64: {
                if (value.isSigned())
                    reflection->SetInt64(&dst, field, value.convert<Poco::Int64>());
                break;
            }
            case ProtobufFieldDescriptor::TYPE_UINT64:
            case ProtobufFieldDescriptor::TYPE_FIXED64: {
                if (value.isInteger())
                    reflection->SetUInt64(&dst, field, value.convert<Poco::UInt64>());
                break;
            }

            case ProtobufFieldDescriptor::TYPE_FLOAT: {
                if (value.isNumeric())
                    reflection->SetFloat(&dst, field, value.convert<float>());
                break;
            }

            case ProtobufFieldDescriptor::TYPE_DOUBLE: {
                if (value.isString())
                    reflection->SetDouble(&dst, field, std::stod(value.toString()));
                break;
            }

            case ProtobufFieldDescriptor::TYPE_STRING:
            case ProtobufFieldDescriptor::TYPE_BYTES: {
                if (value.isString())
                    reflection->SetString(&dst, field, value.toString());
                break;
            }

            case ProtobufFieldDescriptor::TYPE_MESSAGE: {
                if (src.isObject(field->name()))
                    json2PbMsg(*value.extract<Poco::JSON::Object::Ptr>(), *reflection->MutableMessage(&dst, field));
                break;
            }
            default: {
                break;
            }
        }
    }
    return true;
}

void Json2Pb::pbMsg2Json(const ProtobufMsg & src, Poco::JSON::Object & dst, bool enum_str = false)
{
    const ProtobufDescriptor * descriptor = src.GetDescriptor();
    const ProtobufReflection * reflection = src.GetReflection();
    if (nullptr == descriptor)
        return;

    Int32 count = descriptor->field_count();

    for (Int32 i = 0; i < count; ++i)
    {
        const ProtobufFieldDescriptor * field = descriptor->field(i);

        if (field->is_repeated())
        {
            if (reflection->FieldSize(src, field) > 0)
            {
                PArray ::Ptr tmp_obj = new PArray();
                repeatedMessage2Json(src, field, reflection, *tmp_obj, enum_str);
                dst.set(field->name(), *tmp_obj);
            }

            continue;
        }


        if (!reflection->HasField(src, field))
        {
            continue;
        }

        switch (field->type())
        {
            case ProtobufFieldDescriptor::TYPE_MESSAGE: {
                const ProtobufMsg & tmp_message = reflection->GetMessage(src, field);
                if (0 != tmp_message.ByteSizeLong())
                {
                    PObject::Ptr tmp_obj = new PObject(true);
                    pbMsg2Json(tmp_message, *tmp_obj);
                    dst.set(field->name(), *tmp_obj);
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_BOOL:
                dst.set(field->name(), reflection->GetBool(src, field) ? true : false);
                break;

            case ProtobufFieldDescriptor::TYPE_ENUM: {
                const ::google::protobuf::EnumValueDescriptor * enum_value_desc = reflection->GetEnum(src, field);
                if (enum_str)
                {
                    dst.set(field->name(), enum_value_desc->name());
                }
                else
                {
                    dst.set(field->name(), enum_value_desc->number());
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_INT32:
            case ProtobufFieldDescriptor::TYPE_SINT32:
            case ProtobufFieldDescriptor::TYPE_SFIXED32:
                dst.set(field->name(), int32_t(reflection->GetInt32(src, field)));
                break;

            case ProtobufFieldDescriptor::TYPE_UINT32:
            case ProtobufFieldDescriptor::TYPE_FIXED32:
                dst.set(field->name(), uint32_t(reflection->GetUInt32(src, field)));
                break;

            case ProtobufFieldDescriptor::TYPE_INT64:
            case ProtobufFieldDescriptor::TYPE_SINT64:
            case ProtobufFieldDescriptor::TYPE_SFIXED64:
                dst.set(field->name(), int64_t(reflection->GetInt64(src, field)));
                break;

            case ProtobufFieldDescriptor::TYPE_UINT64:
            case ProtobufFieldDescriptor::TYPE_FIXED64:
                dst.set(field->name(), uint64_t(reflection->GetUInt64(src, field)));
                break;

            case ProtobufFieldDescriptor::TYPE_FLOAT:
                dst.set(field->name(), float_t(reflection->GetFloat(src, field)));
                break;

            case ProtobufFieldDescriptor::TYPE_DOUBLE:
                dst.set(field->name(), std::to_string(reflection->GetDouble(src, field)));
                break;

            case ProtobufFieldDescriptor::TYPE_STRING:
            case ProtobufFieldDescriptor::TYPE_BYTES:
                dst.set(field->name(), reflection->GetString(src, field));
                break;

            default:
                break;
        }
    }
}


bool Json2Pb::json2RepeatedMessage(
    const Poco::JSON::Array & json,
    ProtobufMsg & message,
    const ProtobufFieldDescriptor * field,
    const ProtobufReflection * reflection,
    bool str_enum = false)
{
    size_t count = json.size();
    PVar json_var = PVar(json);
    Poco::Dynamic::Array tmp_array = json;


    for (size_t j = 0; j < count; ++j)
    {
        switch (field->type())
        {
            case ProtobufFieldDescriptor::TYPE_BOOL: {
                if (tmp_array[j].isBoolean())
                {
                    reflection->AddBool(&message, field, tmp_array[j].convert<bool>());
                }
                else if (tmp_array[j].isInteger())
                {
                    reflection->AddBool(&message, field, tmp_array[j].convert<int>());
                }
                else if (tmp_array[j].isString())
                {
                    if (tmp_array[j].toString() == "true")
                    {
                        reflection->AddBool(&message, field, true);
                    }
                    else if (tmp_array[j].toString() == "false")
                    {
                        reflection->AddBool(&message, field, false);
                    }
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_ENUM: {
                const ::google::protobuf::EnumDescriptor * pe_desc = field->enum_type();
                const ::google::protobuf::EnumValueDescriptor * pev_desc;
                if (str_enum)
                {
                    pev_desc = pe_desc->FindValueByName(tmp_array[j].toString());
                }
                else
                {
                    pev_desc = pe_desc->FindValueByNumber(tmp_array[j].convert<int>());
                }
                if (nullptr != pev_desc)
                {
                    reflection->AddEnum(&message, field, pev_desc);
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_INT32:
            case ProtobufFieldDescriptor::TYPE_SINT32:
            case ProtobufFieldDescriptor::TYPE_SFIXED32: {
                if (tmp_array[j].isSigned())
                    reflection->AddInt32(&message, field, tmp_array[j].convert<Poco::Int32>());
            }
            break;

            case ProtobufFieldDescriptor::TYPE_UINT32:
            case ProtobufFieldDescriptor::TYPE_FIXED32: {
                if (tmp_array[j].isInteger())
                    reflection->AddUInt32(&message, field, tmp_array[j].convert<Poco::UInt32>());
            }
            break;

            case ProtobufFieldDescriptor::TYPE_INT64:
            case ProtobufFieldDescriptor::TYPE_SINT64:
            case ProtobufFieldDescriptor::TYPE_SFIXED64: {
                if (tmp_array[j].isSigned())
                    reflection->AddInt64(&message, field, tmp_array[j].convert<Poco::Int64>());
            }
            break;

            case ProtobufFieldDescriptor::TYPE_UINT64:
            case ProtobufFieldDescriptor::TYPE_FIXED64: {
                if (tmp_array[j].isInteger())
                    reflection->AddUInt64(&message, field, tmp_array[j].convert<Poco::UInt64>());
            }
            break;

            case ProtobufFieldDescriptor::TYPE_FLOAT: {
                if (tmp_array[j].isString())
                    reflection->AddFloat(&message, field, tmp_array[j].convert<float>());
            }
            break;

            case ProtobufFieldDescriptor::TYPE_DOUBLE: {
                if (tmp_array[j].isString())
                    reflection->AddDouble(&message, field, std::stod(tmp_array[j].toString()));
            }
            break;

            case ProtobufFieldDescriptor::TYPE_MESSAGE: {
                if (json.isObject(j))
                    json2PbMsg(*tmp_array[j].extract<PObject::Ptr>(), *reflection->AddMessage(&message, field));
            }
            break;

            case ProtobufFieldDescriptor::TYPE_STRING:
            case ProtobufFieldDescriptor::TYPE_BYTES: {
                if (tmp_array[j].isString())
                    reflection->AddString(&message, field, tmp_array[j].toString());
            }
            break;

            default: {
                break;
            }
        }
    }
    return true;
}


void Json2Pb::repeatedMessage2Json(
    const ProtobufMsg & message,
    const ProtobufFieldDescriptor * field,
    const ProtobufReflection * reflection,
    Poco::JSON::Array & json,
    bool enum_str)
{
    for (Int32 i = 0; i < reflection->FieldSize(message, field); ++i)
    {
        switch (field->type())
        {
            case ProtobufFieldDescriptor::TYPE_MESSAGE: {
                const ProtobufMsg & tmp_message = reflection->GetRepeatedMessage(message, field, i);
                if (0 != tmp_message.ByteSizeLong())
                {
                    PObject::Ptr tmp_json = new PObject(true);
                    pbMsg2Json(tmp_message, *tmp_json);
                    json.add(tmp_json);
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_BOOL:
                json.add(reflection->GetRepeatedBool(message, field, i) ? true : false);
                break;


            case ProtobufFieldDescriptor::TYPE_ENUM: {
                const ::google::protobuf::EnumValueDescriptor * enum_value_desc = reflection->GetRepeatedEnum(message, field, i);
                if (enum_str)
                {
                    json.add(enum_value_desc->name());
                }
                else
                {
                    json.add(enum_value_desc->number());
                }
                break;
            }

            case ProtobufFieldDescriptor::TYPE_INT32:
            case ProtobufFieldDescriptor::TYPE_SINT32:
            case ProtobufFieldDescriptor::TYPE_SFIXED32:
                json.add(reflection->GetRepeatedInt32(message, field, i));
                break;

            case ProtobufFieldDescriptor::TYPE_UINT32:
            case ProtobufFieldDescriptor::TYPE_FIXED32:
                json.add(reflection->GetRepeatedUInt32(message, field, i));
                break;

            case ProtobufFieldDescriptor::TYPE_INT64:
            case ProtobufFieldDescriptor::TYPE_SINT64:
            case ProtobufFieldDescriptor::TYPE_SFIXED64:
                json.add(int64_t(reflection->GetRepeatedInt64(message, field, i)));
                break;

            case ProtobufFieldDescriptor::TYPE_UINT64:
            case ProtobufFieldDescriptor::TYPE_FIXED64:
                json.add(uint64_t(reflection->GetRepeatedUInt64(message, field, i)));
                break;

            case ProtobufFieldDescriptor::TYPE_FLOAT:
                json.add(float(reflection->GetRepeatedFloat(message, field, i)));
                break;
            case ProtobufFieldDescriptor::TYPE_DOUBLE: {
                json.add(std::to_string(reflection->GetRepeatedDouble(message, field, i)));
                break;
            }

            case ProtobufFieldDescriptor::TYPE_STRING:
            case ProtobufFieldDescriptor::TYPE_BYTES:
                json.add(reflection->GetRepeatedString(message, field, i));
                break;

            default:
                break;
        }
    }
}
}
