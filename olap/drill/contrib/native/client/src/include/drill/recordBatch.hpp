/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef RECORDBATCH_H
#define RECORDBATCH_H


#include <assert.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <ostream>
#include <sstream>
#include <vector>
#include <boost/lexical_cast.hpp>
#include "drill/common.hpp"
#include "drill/decimalUtils.hpp"
#include "drill/protobuf/Types.pb.h"


#if defined _WIN32 || defined __CYGWIN__
  #ifdef DRILL_CLIENT_EXPORTS
      #define DECLSPEC_DRILL_CLIENT __declspec(dllexport)
  #else
    #ifdef USE_STATIC_LIBDRILL
      #define DECLSPEC_DRILL_CLIENT
    #else
      #define DECLSPEC_DRILL_CLIENT  __declspec(dllimport)
    #endif
  #endif
#else
  #if __GNUC__ >= 4
    #define DECLSPEC_DRILL_CLIENT __attribute__ ((visibility ("default")))
  #else
    #define DECLSPEC_DRILL_CLIENT
  #endif
#endif

namespace exec{
    namespace shared {
        class SerializedField;
        class RecordBatchDef;
        class QueryResult;
        class QueryData;
    };
};

namespace Drill {

class FieldBatch;
class ValueVectorBase;

//TODO: The base classes for value vectors should have abstract functions instead of implementations
//that return 'NOT IMPLEMENTED YET'

// A Read Only Sliced byte buffer
class SlicedByteBuf{
    public:
        //TODO: check the size and offset parameters. What is the largest they can be?
        SlicedByteBuf(const ByteBuf_t b, size_t offset, size_t length){
            assert(length>=0);
            this->m_buffer=b;
            this->m_start=offset;
            this->m_end=length>0?offset+length-1:offset;
            this->m_length=length;
        }

        // Carve a sliced buffer out of another sliced buffer
        SlicedByteBuf(const SlicedByteBuf& sb, size_t offset, size_t length){
            assert(length>=0);
            this->m_buffer=sb.m_buffer;
            this->m_start=sb.m_start+offset;
            this->m_end=length>0?sb.m_start+offset+length-1:sb.m_start+offset;
            this->m_length=length;
        }

        //Copy ctor
        SlicedByteBuf(const SlicedByteBuf& other ){
            if(this!=&other){
                this->m_buffer=other.m_buffer;
                this->m_start=other.m_start;
                this->m_end=other.m_end;
                this->m_length=other.m_length;
            }
        }

        SlicedByteBuf& operator=( const SlicedByteBuf& rhs ){
            if(this!=&rhs){
                this->m_buffer=rhs.m_buffer;
                this->m_start=rhs.m_start;
                this->m_end=rhs.m_end;
                this->m_length=rhs.m_length;
            }
            return *this;
        }

        size_t getStart(){
            return this->m_start;
        }
        size_t getEnd(){
            return this->m_end;
        }
        size_t getLength(){
            return this->m_length;
        }

        // ByteBuf_t getBuffer(){ return m_buffer;}
        ByteBuf_t getSliceStart(){ return this->m_buffer+this->m_start;}

        //    accessor functions
        //
        //    TYPE getTYPE(size_t index){
        //    if(index>=m_length) return 0;
        //      return (TYPE) m_buffer[offset+index];
        //    }


        template <typename T> T readAt(size_t index) const {
            // Type T can only be an integer type
            // Type T cannot be a struct of fixed size
            // Because struct alignment is compiler dependent
            // we can end up with a struct size that is larger
            // than the buffer in the sliced buf.
            assert((index + sizeof(T) <= this->m_length));
            if(index + sizeof(T) <= this->m_length)
                return *((T*)(this->m_buffer+this->m_start+index));
            return 0;

        }

        uint8_t getByte(size_t index){
            return readAt<uint8_t>(index);
        }

        uint32_t getUint32(size_t index){
            return readAt<uint32_t>(index);
        }

        uint64_t getUint64(size_t index){
            return readAt<uint64_t>(index);
        }

        ByteBuf_t getAt(size_t index){
            return this->m_buffer+m_start+index;
        }

        bool getBit(size_t index){
            // refer to BitVector.java http://bit.ly/Py1jof
            return ((this->m_buffer[m_start+index/8] &  ( 1 << (index % 8) )) !=0);
        }
    private:
        ByteBuf_t m_buffer; // the backing store
        size_t  m_start;    //offset within the backing store where this slice begins
        size_t  m_end;      //offset within the backing store where this slice ends
        size_t  m_length;   //length
};

class DECLSPEC_DRILL_CLIENT ValueVectorBase{
    public:
        ValueVectorBase(SlicedByteBuf *b, size_t rowCount){
            m_pBuffer=b;
            m_rowCount=rowCount;
        }

        virtual ~ValueVectorBase(){
        }

        // test whether the value is null in the position index
        virtual bool isNull(size_t index) const {
            return false;
        }


        const char* get(size_t index) const { return 0;}
        virtual void getValueAt(size_t index, char* buf, size_t nChars) const =0;

        virtual const ByteBuf_t getRaw(size_t index) const {
            return (ByteBuf_t)m_pBuffer->getSliceStart() ;
        }

        virtual uint32_t getSize(size_t index) const=0;
        //virtual uint32_t getSize(size_t index) const {
        //    return 0;
        //}

    protected:
        SlicedByteBuf* m_pBuffer;
        size_t m_rowCount;
};

class DECLSPEC_DRILL_CLIENT ValueVectorUnimplemented:public ValueVectorBase{
    public:
        ValueVectorUnimplemented(SlicedByteBuf *b, size_t rowCount):ValueVectorBase(b,rowCount){
        }

        virtual ~ValueVectorUnimplemented(){
        }

        const char* get(size_t index) const { return 0;};
        virtual void getValueAt(size_t index, char* buf, size_t nChars) const{
            *buf=0; return;
        }

        virtual uint32_t getSize(size_t index) const{ return 0;};

};

// Represents a value vector that has all values NULL
class DECLSPEC_DRILL_CLIENT ValueVectorNull:public ValueVectorBase{
    public:
        ValueVectorNull(SlicedByteBuf *b, size_t rowCount):ValueVectorBase(b,rowCount){
        }

        virtual ~ValueVectorNull(){
        }

        virtual void getValueAt(size_t index, char* buf, size_t nChars) const{
            *buf=0; return;
        }

        virtual uint32_t getSize(size_t index) const{ return 0;};

        virtual bool isNull(size_t index) const {
            return true;
        }

};

class DECLSPEC_DRILL_CLIENT ValueVectorFixedWidth:public ValueVectorBase{
    public:
        ValueVectorFixedWidth(SlicedByteBuf *b, size_t rowCount):ValueVectorBase(b, rowCount){
        }

        void getValueAt(size_t index, char* buf, size_t nChars) const {
            strncpy(buf, "NOT IMPLEMENTED YET", nChars);
            return;
        }

        const ByteBuf_t getRaw(size_t index) const {
            return this->m_pBuffer->getSliceStart()+index*this->getSize(index);
        }

        uint32_t getSize(size_t index) const {
            return 0;
        }
};

template <typename VALUE_TYPE>
    class ValueVectorFixed : public ValueVectorFixedWidth {
        public:
            ValueVectorFixed(SlicedByteBuf *b, size_t rowCount) :
                ValueVectorFixedWidth(b, rowCount) {}

            VALUE_TYPE get(size_t index) const {
                return m_pBuffer->readAt<VALUE_TYPE>(index * sizeof(VALUE_TYPE));
            }

            void getValueAt(size_t index, char* buf, size_t nChars) const {
                std::stringstream sstr;
                VALUE_TYPE value = this->get(index);
                sstr << value;
                strncpy(buf, sstr.str().c_str(), nChars);
            }

            uint32_t getSize(size_t index) const {
                return sizeof(VALUE_TYPE);
            }
    };


class DECLSPEC_DRILL_CLIENT ValueVectorBit:public ValueVectorFixedWidth{
    public:
        ValueVectorBit(SlicedByteBuf *b, size_t rowCount):ValueVectorFixedWidth(b, rowCount){
        }
        bool get(size_t index) const {
            #ifdef DEBUG
            uint8_t b = m_pBuffer->getByte((index)/8);
            uint8_t bitOffset = index%8;
            uint8_t setBit = (1<<bitOffset); // sets the Nth bit.
            uint8_t isSet = (b&setBit);
            return isSet;
            #else
            return (bool)((m_pBuffer->getByte((index)/8)) & (1<< (index%8) ));
            #endif

        }
        void getValueAt(size_t index, char* buf, size_t nChars) const {
            char str[64]; // Can't have more than 64 digits of precision
            //could use itoa instead of sprintf which is slow,  but it is not portable
            sprintf(str, "%s", this->get(index)?"true":"false");
            strncpy(buf, str, nChars);
            return;
        }
        uint32_t getSize(size_t index) const {
            return sizeof(uint8_t);
        }
};

template <int DECIMAL_DIGITS, int WIDTH_IN_BYTES, bool IS_SPARSE, int MAX_PRECISION = 0 >
    class ValueVectorDecimal: public ValueVectorFixedWidth {
        public:
            ValueVectorDecimal(SlicedByteBuf* b, size_t rowCount, int32_t scale):
                ValueVectorFixedWidth(b, rowCount),
                m_scale(scale)
        {
            ; // Do nothing
        }

            DecimalValue get(size_t index) const {
                if (IS_SPARSE)
                {
                    return getDecimalValueFromSparse(*m_pBuffer, index * WIDTH_IN_BYTES, DECIMAL_DIGITS, m_scale);
                }
                return getDecimalValueFromDense(*m_pBuffer, index * WIDTH_IN_BYTES, DECIMAL_DIGITS, m_scale, MAX_PRECISION, WIDTH_IN_BYTES);
            }

            void getValueAt(size_t index, char* buf, size_t nChars) const {
                const DecimalValue& val = this->get(index);
                std::string str = boost::lexical_cast<std::string>(val.m_unscaledValue);
                if (str[0] == '-') {
                    str = str.substr(1);
                    while (str.length() < m_scale) {
                        str = "0" + str;
                    }
                    str = "-" + str;
                } else {
                    while (str.length() < m_scale) {
                       str = "0" + str;
                    }
                }
                if (m_scale == 0) {
                    strncpy(buf, str.c_str(), nChars);
                } else {
                    size_t idxDecimalMark = str.length() - m_scale;
                    const std::string& decStr =
                            (idxDecimalMark == 0 ? "0" : str.substr(0, idxDecimalMark)) + "." + str.substr(idxDecimalMark, m_scale);
                    strncpy(buf, decStr.c_str(), nChars);
                }
                return;
            }

            uint32_t getSize(size_t index) const {
                return WIDTH_IN_BYTES;
            }

        private:
            int32_t m_scale;
    };

template<typename VALUE_TYPE>
    class ValueVectorDecimalTrivial: public ValueVectorFixedWidth {
        public:
            ValueVectorDecimalTrivial(SlicedByteBuf* b, size_t rowCount, int32_t scale):
                ValueVectorFixedWidth(b, rowCount),
                m_scale(scale)
        {
            ; // Do nothing
        }

            DecimalValue get(size_t index) const {
                return DecimalValue(
                        m_pBuffer->readAt<VALUE_TYPE>(index * sizeof(VALUE_TYPE)),
                        m_scale);
            }

            void getValueAt(size_t index, char* buf, size_t nChars) const {
                VALUE_TYPE value = m_pBuffer->readAt<VALUE_TYPE>(index * sizeof(VALUE_TYPE));
                std::string str = boost::lexical_cast<std::string>(value);
                if (str[0] == '-') {
                    str = str.substr(1);
                    while (str.length() < m_scale) {
                        str = "0" + str;
                    }
                    str = "-" + str;
                } else {
                    while (str.length() < m_scale) {
                       str = "0" + str;
                    }
                }
                if (m_scale == 0) {
                    strncpy(buf, str.c_str(), nChars);
                } else {
                    size_t idxDecimalMark = str.length() - m_scale;
                    const std::string& decStr= str.substr(0, idxDecimalMark) + "." + str.substr(idxDecimalMark, m_scale);
                    strncpy(buf, decStr.c_str(), nChars);
                }
                return;
            }

            uint32_t getSize(size_t index) const {
                return sizeof(VALUE_TYPE);
            }

        private:
            int32_t m_scale;
    };


template <typename VALUE_TYPE>
    class NullableValueVectorFixed : public ValueVectorBase
{
    public:
        NullableValueVectorFixed(SlicedByteBuf *b, size_t rowCount):ValueVectorBase(b, rowCount){
            size_t offsetEnd = (size_t)rowCount;
            this->m_pBitmap= new SlicedByteBuf(*b, 0, offsetEnd);
            this->m_pData= new SlicedByteBuf(*b, offsetEnd, b->getLength());
            // TODO: testing boundary case(null columns)
        }

        ~NullableValueVectorFixed(){
            delete this->m_pBitmap;
            delete this->m_pData;
        }

        // test whether the value is null in the position index
        bool isNull(size_t index) const {
            return (m_pBitmap->getByte(index)==0);
        }

        VALUE_TYPE get(size_t index) const {
            // it should not be called if the value is null
            assert( "value is null" && !isNull(index));
            return m_pData->readAt<VALUE_TYPE>(index * sizeof(VALUE_TYPE));
        }

        void getValueAt(size_t index, char* buf, size_t nChars) const {
            assert( "value is null" && !isNull(index));
            std::stringstream sstr;
            VALUE_TYPE value = this->get(index);
            sstr << value;
            strncpy(buf, sstr.str().c_str(), nChars);
        }

        uint32_t getSize(size_t index) const {
            assert("value is null" && !isNull(index));
            return sizeof(VALUE_TYPE);
        }
    private:
        SlicedByteBuf* m_pBitmap;
        SlicedByteBuf* m_pData;
};

// The 'holder' classes are (by contract) simple structs with primitive members and no dynamic allocations.
// The template classes create an instance of the class and return it to the caller in the 'get' routines.
// The compiler will create a copy and return it to the caller. If the object is more complex than a struct of
// primitives, the class _must_ provide a copy constructor.
// We don't really need a destructor here, but we declare a virtual dtor in the base class in case we ever get
// more complex and start doing dynamic allocations in these classes.

struct DECLSPEC_DRILL_CLIENT DateTimeBase{
    DateTimeBase():m_datetime(0){}
    virtual ~DateTimeBase(){}
    int64_t m_datetime;
    int64_t getMillis() const { return m_datetime; }
    virtual void load() =0;
    virtual std::string toString()=0;
};

struct DECLSPEC_DRILL_CLIENT DateHolder: public virtual DateTimeBase{
    DateHolder(){};
    DateHolder(int64_t d){m_datetime=d; load();}
    int32_t m_year;
    int32_t m_month;
    int32_t m_day;
    void load();
    std::string toString();
};

struct DECLSPEC_DRILL_CLIENT TimeHolder: public virtual DateTimeBase{
    TimeHolder(){};
    TimeHolder(uint32_t d){m_datetime=d; load();}
    uint32_t m_hr;
    uint32_t m_min;
    uint32_t m_sec;
    uint32_t m_msec;
    void load();
    std::string toString();
};

struct DECLSPEC_DRILL_CLIENT DateTimeHolder: public DateHolder, public TimeHolder{
    DateTimeHolder(){};
    DateTimeHolder(int64_t d){m_datetime=d; load();}
    void load();
    std::string toString();
};

struct DECLSPEC_DRILL_CLIENT DateTimeTZHolder: public DateTimeHolder{
    DateTimeTZHolder(ByteBuf_t b){
        m_datetime=*(int64_t*)b;
        m_tzIndex=*(uint32_t*)(b+sizeof(uint64_t));
        load();
    }
    void load();
    std::string toString();
    int32_t m_tzIndex;
    static uint32_t size(){ return sizeof(int64_t)+sizeof(uint32_t); }

};

struct IntervalYearHolder{
    IntervalYearHolder(ByteBuf_t b){
        m_month=*(int32_t*)b;
        load();
    }
    void load(){};
    std::string toString();
    int32_t m_month;
    static uint32_t size(){ return sizeof(uint32_t); }
};

struct IntervalDayHolder{
    IntervalDayHolder(ByteBuf_t b){
        m_day=*(int32_t*)(b);
        m_ms=*(int32_t*)(b+sizeof(int32_t));
        load();
    }
    void load(){};
    std::string toString();
    int32_t m_day;
    int32_t m_ms;
    static uint32_t size(){ return 2*sizeof(uint32_t)+4; }
};

struct IntervalHolder{
    IntervalHolder(ByteBuf_t b){
        m_month=*(int32_t*)b;
        m_day=*(int32_t*)(b+sizeof(int32_t));
        m_ms=*(int32_t*)(b+2*sizeof(int32_t));
        load();
    }
    void load(){};
    std::string toString();
    int32_t m_month;
    int32_t m_day;
    int32_t m_ms;
    static uint32_t size(){ return 3*sizeof(int32_t)+4; }
};

/*
 * VALUEHOLDER_CLASS_TYPE is a struct with a constructor that takes a parameter of type VALUE_VECTOR_TYPE
 * (a primitive type)
 * VALUEHOLDER_CLASS_TYPE implements a toString function
 * Note that VALUEHOLDER_CLASS_TYPE is created on the stack and the copy returned in the get function.
 * So the class needs to have the appropriate copy constructor or the default bitwise copy should work
 * correctly.
 */
template <class VALUEHOLDER_CLASS_TYPE, typename VALUE_TYPE>
    class ValueVectorTyped:public ValueVectorFixedWidth{
        public:
            ValueVectorTyped(SlicedByteBuf *b, size_t rowCount) :
                ValueVectorFixedWidth(b, rowCount) {}


            VALUEHOLDER_CLASS_TYPE get(size_t index) const {
                VALUE_TYPE v= m_pBuffer->readAt<VALUE_TYPE>(index * sizeof(VALUE_TYPE));
                VALUEHOLDER_CLASS_TYPE r(v);
                return r;
            }

            void getValueAt(size_t index, char* buf, size_t nChars) const {
                std::stringstream sstr;
                VALUEHOLDER_CLASS_TYPE value = this->get(index);
                sstr << value.toString();
                strncpy(buf, sstr.str().c_str(), nChars);
            }

            uint32_t getSize(size_t index) const {
                return sizeof(VALUE_TYPE);
            }
    };

template <class VALUEHOLDER_CLASS_TYPE>
    class ValueVectorTypedComposite:public ValueVectorFixedWidth{
        public:
            ValueVectorTypedComposite(SlicedByteBuf *b, size_t rowCount) :
                ValueVectorFixedWidth(b, rowCount) {}


            VALUEHOLDER_CLASS_TYPE get(size_t index) const {
                ByteBuf_t b= m_pBuffer->getAt(index * getSize(index));
                VALUEHOLDER_CLASS_TYPE r(b);
                return r;
            }

            void getValueAt(size_t index, char* buf, size_t nChars) const {
                std::stringstream sstr;
                VALUEHOLDER_CLASS_TYPE value = this->get(index);
                sstr << value.toString();
                strncpy(buf, sstr.str().c_str(), nChars);
            }

            uint32_t getSize(size_t index) const {
                return VALUEHOLDER_CLASS_TYPE::size();
            }
    };

template <class VALUEHOLDER_CLASS_TYPE, class VALUE_VECTOR_TYPE>
    class NullableValueVectorTyped : public ValueVectorBase {
        public:

            NullableValueVectorTyped(SlicedByteBuf *b, size_t rowCount):ValueVectorBase(b, rowCount){
                size_t offsetEnd = (size_t)rowCount;
                this->m_pBitmap= new SlicedByteBuf(*b, 0, offsetEnd);
                this->m_pData= new SlicedByteBuf(*b, offsetEnd, b->getLength()-offsetEnd);
                this->m_pVector= new VALUE_VECTOR_TYPE(m_pData, rowCount);
            }
            // Specialized for Decimal Types
            NullableValueVectorTyped(SlicedByteBuf *b, size_t rowCount, int32_t scale):ValueVectorBase(b, rowCount){
                size_t offsetEnd = (size_t)rowCount;
                this->m_pBitmap= new SlicedByteBuf(*b, 0, offsetEnd);
                this->m_pData= new SlicedByteBuf(*b, offsetEnd, b->getLength()-offsetEnd);
                this->m_pVector= new VALUE_VECTOR_TYPE(m_pData, rowCount, scale);
            }

            ~NullableValueVectorTyped(){
                delete this->m_pBitmap;
                delete this->m_pData;
                delete this->m_pVector;
            }

            bool isNull(size_t index) const{
                return (m_pBitmap->getByte(index)==0);
            }

            VALUEHOLDER_CLASS_TYPE get(size_t index) const {
                assert(!isNull(index));
                return m_pVector->get(index);
            }

            void getValueAt(size_t index, char* buf, size_t nChars) const{
                std::stringstream sstr;
                if(this->isNull(index)){
                    sstr<<"NULL";
                    strncpy(buf, sstr.str().c_str(), nChars);
                }else{
                    m_pVector->getValueAt(index, buf, nChars);
                }
            }

            uint32_t getSize(size_t index) const {
                assert(!isNull(index));
                return this->m_pVector->getSize(index);
            }

        private:
            SlicedByteBuf* m_pBitmap;
            SlicedByteBuf* m_pData;
            VALUE_VECTOR_TYPE* m_pVector;
    };

class DECLSPEC_DRILL_CLIENT VarWidthHolder{
    public:
        ByteBuf_t data;
        size_t size;
};

class DECLSPEC_DRILL_CLIENT ValueVectorVarWidth:public ValueVectorBase{
    public:
        ValueVectorVarWidth(SlicedByteBuf *b, size_t rowCount):ValueVectorBase(b, rowCount){
            size_t offsetEnd = (rowCount+1)*sizeof(uint32_t);
            this->m_pOffsetArray= new SlicedByteBuf(*b, 0, offsetEnd);
            this->m_pData= new SlicedByteBuf(*b, offsetEnd, b->getLength()-offsetEnd);
        }
        ~ValueVectorVarWidth(){
            delete this->m_pOffsetArray;
            delete this->m_pData;
        }

        VarWidthHolder get(size_t index) const {
            size_t startIdx = this->m_pOffsetArray->getUint32(index*sizeof(uint32_t));
            size_t endIdx = this->m_pOffsetArray->getUint32((index+1)*sizeof(uint32_t));
            size_t length = endIdx - startIdx;
            assert(length >= 0);
            // Return an object created on the stack. The compiler will return a
            // copy and destroy the stack object. The optimizer will hopefully
            // elide this so we can return an object with no extra memory allocation
            // and no copies.(SEE: http://en.wikipedia.org/wiki/Return_value_optimization)
            VarWidthHolder dst;
            dst.data=this->m_pData->getSliceStart()+startIdx;
            dst.size=length;
            return dst;
        }

        void getValueAt(size_t index, char* buf, size_t nChars) const {
            size_t startIdx = this->m_pOffsetArray->getUint32(index*sizeof(uint32_t));
            size_t endIdx = this->m_pOffsetArray->getUint32((index+1)*sizeof(uint32_t));
            size_t length = endIdx - startIdx;
            size_t copyChars=0;
            assert(length >= 0);
            copyChars=nChars<=length?nChars:length;
            memcpy(buf, this->m_pData->getSliceStart()+startIdx, copyChars);
            return;
        }

        const ByteBuf_t getRaw(size_t index) const {
            size_t startIdx = this->m_pOffsetArray->getUint32(index*sizeof(uint32_t));
            size_t endIdx = this->m_pOffsetArray->getUint32((index+1)*sizeof(uint32_t));
            size_t length = endIdx - startIdx;
            assert(length >= 0);
            return this->m_pData->getSliceStart()+startIdx;
        }

        uint32_t getSize(size_t index) const {
            size_t startIdx = this->m_pOffsetArray->getUint32(index*sizeof(uint32_t));
            size_t endIdx = this->m_pOffsetArray->getUint32((index+1)*sizeof(uint32_t));
            size_t length = endIdx - startIdx;
            assert(length >= 0);
            return length;
        }
    private:
        SlicedByteBuf* m_pOffsetArray;
        SlicedByteBuf* m_pData;
};

class DECLSPEC_DRILL_CLIENT ValueVectorVarChar:public ValueVectorVarWidth{
    public:
        ValueVectorVarChar(SlicedByteBuf *b, size_t rowCount):ValueVectorVarWidth(b, rowCount){
        }
        VarWidthHolder get(size_t index) const {
            return ValueVectorVarWidth::get(index);
        }
};

class DECLSPEC_DRILL_CLIENT ValueVectorVarDecimal:public ValueVectorVarWidth{
    public:
        ValueVectorVarDecimal(SlicedByteBuf *b, size_t rowCount, size_t scale):
            ValueVectorVarWidth(b, rowCount),
            m_scale(scale)
        {
        }
        DecimalValue get(size_t index) const {
            size_t length = getSize(index);
            ByteBuf_t buff = getRaw(index);
            SlicedByteBuf intermediateData(&buff[0], 0, length);
            return getDecimalValueFromByteBuf(intermediateData, length, this->m_scale);
        }

        void getValueAt(size_t index, char* buf, size_t nChars) const {
            const DecimalValue& val = this->get(index);
            std::string str = boost::lexical_cast<std::string>(val.m_unscaledValue);
            if (str[0] == '-') {
                str = str.substr(1);
                while (str.length() < m_scale) {
                    str = "0" + str;
                }
                str = "-" + str;
            } else {
                while (str.length() < m_scale) {
                    str = "0" + str;
                }
            }
            if (m_scale == 0) {
                strncpy(buf, str.c_str(), nChars);
            } else {
                size_t idxDecimalMark = str.length() - m_scale;
                const std::string& decStr =
                        (idxDecimalMark == 0 ? "0" : str.substr(0, idxDecimalMark)) + "." + str.substr(idxDecimalMark, m_scale);
                strncpy(buf, decStr.c_str(), nChars);
            }
            return;
        }

    private:
        int32_t m_scale;
};

class DECLSPEC_DRILL_CLIENT ValueVectorVarBinary:public ValueVectorVarWidth{
    public:
        ValueVectorVarBinary(SlicedByteBuf *b, size_t rowCount):ValueVectorVarWidth(b, rowCount){
        }
};
//
//TODO: For windows, we have to export instantiations of the template class.
//see: http://msdn.microsoft.com/en-us/library/twa2aw10.aspx
//for example:
//template class __declspec(dllexport) B<int>;
//class __declspec(dllexport) D : public B<int> { }
//
// --------------------------------------------------------------------------------------
// TODO: alias for all value vector types
// --------------------------------------------------------------------------------------
typedef NullableValueVectorTyped<int, ValueVectorBit > NullableValueVectorBit;
// Aliases for Decimal Types:
// The definitions for decimal digits, width, max precision are defined in
// /exec/java-exec/src/main/codegen/data/ValueVectorTypes.tdd
//
// Decimal9 and Decimal18 could be optimized, maybe write seperate classes?
typedef ValueVectorDecimalTrivial<int32_t> ValueVectorDecimal9;
typedef ValueVectorDecimalTrivial<int64_t> ValueVectorDecimal18;
typedef ValueVectorDecimal<3, 12, false, 28> ValueVectorDecimal28Dense;
typedef ValueVectorDecimal<4, 16, false, 38> ValueVectorDecimal38Dense;
typedef ValueVectorDecimal<5, 20, true, 28>  ValueVectorDecimal28Sparse;
typedef ValueVectorDecimal<6, 24, true, 38>  ValueVectorDecimal38Sparse;

typedef NullableValueVectorTyped<int32_t, ValueVectorDecimal9> NullableValueVectorDecimal9;
typedef NullableValueVectorTyped<int64_t, ValueVectorDecimal18> NullableValueVectorDecimal18;
typedef NullableValueVectorTyped<DecimalValue, ValueVectorDecimal28Dense> NullableValueVectorDecimal28Dense;
typedef NullableValueVectorTyped<DecimalValue, ValueVectorDecimal38Dense> NullableValueVectorDecimal38Dense;
typedef NullableValueVectorTyped<DecimalValue, ValueVectorDecimal28Sparse> NullableValueVectorDecimal28Sparse;
typedef NullableValueVectorTyped<DecimalValue, ValueVectorDecimal38Sparse> NullableValueVectorDecimal38Sparse;
typedef NullableValueVectorTyped<DecimalValue, ValueVectorVarDecimal> NullableValueVectorVarDecimal;

typedef ValueVectorTyped<DateHolder, int64_t> ValueVectorDate;
typedef ValueVectorTyped<DateTimeHolder, int64_t> ValueVectorTimestamp;
typedef ValueVectorTyped<TimeHolder, uint32_t> ValueVectorTime;
typedef ValueVectorTypedComposite<DateTimeTZHolder> ValueVectorTimestampTZ;
typedef ValueVectorTypedComposite<IntervalHolder> ValueVectorInterval;
typedef ValueVectorTypedComposite<IntervalDayHolder> ValueVectorIntervalDay;
typedef ValueVectorTypedComposite<IntervalYearHolder> ValueVectorIntervalYear;

typedef NullableValueVectorTyped<DateHolder, ValueVectorDate> NullableValueVectorDate;
typedef NullableValueVectorTyped<DateTimeHolder, ValueVectorTimestamp> NullableValueVectorTimestamp;
typedef NullableValueVectorTyped<TimeHolder, ValueVectorTime>  NullableValueVectorTime;
typedef NullableValueVectorTyped<DateTimeTZHolder, ValueVectorTimestampTZ>  NullableValueVectorTimestampTZ;
typedef NullableValueVectorTyped<IntervalHolder, ValueVectorInterval>  NullableValueVectorInterval;
typedef NullableValueVectorTyped<IntervalDayHolder, ValueVectorIntervalDay>  NullableValueVectorIntervalDay;
typedef NullableValueVectorTyped<IntervalYearHolder, ValueVectorIntervalYear>  NullableValueVectorIntervalYear;

class FieldBatch{
    public:
        FieldBatch(const Drill::FieldMetadata& fmd, const ByteBuf_t data, size_t start, size_t length):
            m_fieldMetadata(fmd){
                m_pValueVector=NULL;m_pFieldData=NULL;
                if(length>0){
                    m_pFieldData=new SlicedByteBuf(data, start, length);
                }
            }

        ~FieldBatch(){
            if(m_pFieldData!=NULL){
                delete m_pFieldData; m_pFieldData=NULL;
            }
            if(m_pValueVector!=NULL){
                delete m_pValueVector; m_pValueVector=NULL;
            }
        }

        // Loads the data into a Value Vector ofappropriate type
        ret_t load();
        ret_t loadNull(size_t nRecords);

        const ValueVectorBase * getVector(){
            return m_pValueVector;
        }

    private:
        const Drill::FieldMetadata& m_fieldMetadata;
        ValueVectorBase * m_pValueVector;
        SlicedByteBuf   * m_pFieldData;

};

class ValueVectorFactory{
    public:
        static ValueVectorBase* allocateValueVector(const Drill::FieldMetadata & fmd, SlicedByteBuf *b);
};

class DECLSPEC_DRILL_CLIENT RecordBatch{
    public:

        //m_allocatedBuffer is the memory block allocated to hold the incoming RPC message. Record Batches operate on
        //slices of the allocated buffer. The first slice (the first Field Batch), begins at m_buffer. Data in the
        //allocated buffer before m_buffer is mostly the RPC header, and the QueryResult object.
        RecordBatch(exec::shared::QueryData* pResult, AllocatedBufferPtr r, ByteBuf_t b);

        ~RecordBatch();

        // get the ith field metadata
        const Drill::FieldMetadata& getFieldMetadata(size_t index){
            //return this->m_pRecordBatchDef->field(index);
            return *(m_fieldDefs->at(index));
        }

        size_t getNumRecords(){ return m_numRecords;}
        std::vector<FieldBatch*>& getFields(){ return m_fields;}
        size_t getNumFields();
        DEPRECATED bool isLastChunk();

        boost::shared_ptr<std::vector<Drill::FieldMetadata*> > getColumnDefs(){ return m_fieldDefs;}

        //
        // build the record batch: i.e. fill up the value vectors from the buffer.
        // On fetching the data from the server, the caller creates a RecordBatch
        // object then calls build() to build the value vectors.The caller saves the
        // Record Batch and is responsible for freeing both the RecordBatch and the
        // raw buffer memory
        //
        ret_t build();

        void print(std::ostream& s, size_t num);

        const ValueVectorBase * getVector(size_t index){
            return m_fields[index]->getVector();
        }

        void schemaChanged(bool b){
            this->m_bHasSchemaChanged=b;
        }

        bool hasSchemaChanged(){ return m_bHasSchemaChanged;}

        #ifdef DEBUG
        const exec::shared::QueryData* getQueryResult(){ return this->m_pQueryResult;}
        #endif
    private:
        const exec::shared::QueryData* m_pQueryResult;
        const exec::shared::RecordBatchDef* m_pRecordBatchDef;
        AllocatedBufferPtr m_allocatedBuffer;
        ByteBuf_t m_buffer;
        //build the current schema out of the field metadata
        FieldDefPtr m_fieldDefs;
        std::vector<FieldBatch*> m_fields;
        size_t m_numFields;
        size_t m_numRecords;
        bool m_bHasSchemaChanged;

}; // RecordBatch

} // namespace

#endif
