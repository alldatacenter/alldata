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
#include <limits.h>
#include <stdlib.h>
#include "utils.hpp"
#include "logger.hpp"
#include "drill/common.hpp"
#include "drill/drillConfig.hpp"

#if defined _WIN32  || defined _WIN64
//Windows header files redefine 'max'
#ifdef max
#undef max
#endif
#endif

namespace Drill{



boost::random::random_device Utils::s_RNG;
boost::random::mt19937 Utils::s_URNG(s_RNG());
boost::uniform_int<> Utils::s_uniformDist(0,std::numeric_limits<int>::max()-1);
boost::variate_generator<boost::random::mt19937&, boost::uniform_int<> > Utils::s_randomNumber(s_URNG, s_uniformDist);

boost::mutex AllocatedBuffer::s_memCVMutex;
boost::condition_variable AllocatedBuffer::s_memCV;
size_t AllocatedBuffer::s_allocatedMem = 0;
bool AllocatedBuffer::s_isBufferLimitReached = false;
boost::mutex s_utilMutex;

ByteBuf_t Utils::allocateBuffer(size_t len){
    boost::lock_guard<boost::mutex> memLock(AllocatedBuffer::s_memCVMutex);
    AllocatedBuffer::s_allocatedMem += len;
    //http://stackoverflow.com/questions/2688466/why-mallocmemset-is-slower-than-calloc
    ByteBuf_t b = (ByteBuf_t)calloc(len, sizeof(Byte_t));
    size_t safeSize = DrillClientConfig::getBufferLimit() - MEM_CHUNK_SIZE;
    if (b != NULL && AllocatedBuffer::s_allocatedMem >= safeSize){
        AllocatedBuffer::s_isBufferLimitReached = true;
    }
    return b;
}

void Utils::freeBuffer(ByteBuf_t b, size_t len){
    boost::lock_guard<boost::mutex> memLock(AllocatedBuffer::s_memCVMutex);
    AllocatedBuffer::s_allocatedMem -= len;
    free(b);
    size_t safeSize = DrillClientConfig::getBufferLimit() - MEM_CHUNK_SIZE;
    if (b != NULL && AllocatedBuffer::s_allocatedMem < safeSize){
        AllocatedBuffer::s_isBufferLimitReached = false;
        //signal any waiting threads
        AllocatedBuffer::s_memCV.notify_one();
    }
}

void Utils::parseConnectStr(const char* connectStr,
  std::string& pathToDrill,
  std::string& protocol,
  std::string& hostPortStr){
    boost::lock_guard<boost::mutex> memLock(s_utilMutex);
    char u[MAX_CONNECT_STR + 1];
    strncpy(u, connectStr, MAX_CONNECT_STR); u[MAX_CONNECT_STR] = 0;
    char* z = strtok(u, "=");
    char* c = strtok(NULL, "/");
    char* p = strtok(NULL, "");

    if (p != NULL) pathToDrill = std::string("/") + p;
    protocol = z; hostPortStr = c;
    return;
}

void Utils::shuffle(std::vector<std::string>& vector){
    std::random_shuffle(vector.begin(), vector.end(), Utils::s_randomNumber);
    return;
}

void Utils::add(std::vector<std::string>& vector1, std::vector<std::string>& vector2){
    std::vector<std::string>::iterator it;
    for (it = vector2.begin(); it != vector2.end(); it++) {
        std::vector<std::string>::iterator it2 = std::find(vector1.begin(), vector1.end(), *it);
        if (it2 == vector1.end()){
            vector1.push_back(*it);
        }
    }
}

AllocatedBuffer::AllocatedBuffer(size_t l){
    m_pBuffer = NULL;
    m_pBuffer = Utils::allocateBuffer(l);
    m_bufSize = m_pBuffer != NULL ? l : 0;
}

AllocatedBuffer::~AllocatedBuffer(){
    Utils::freeBuffer(m_pBuffer, m_bufSize);
    m_pBuffer = NULL;
    m_bufSize = 0;
}

EncryptionContext::EncryptionContext(const bool& encryptionReqd, const int& maxWrappedSize, const int& wrapSizeLimit) {
    this->m_bEncryptionReqd = encryptionReqd;
    this->m_maxWrappedSize = maxWrappedSize;
    this->m_wrapSizeLimit = wrapSizeLimit;
}

EncryptionContext::EncryptionContext() {
    this->m_bEncryptionReqd = false;
    this->m_maxWrappedSize = 65536;
    this->m_wrapSizeLimit = 0;
}

void EncryptionContext::setEncryptionReqd(const bool& encryptionReqd) {
    this->m_bEncryptionReqd = encryptionReqd;
}

void EncryptionContext::setMaxWrappedSize(const int& maxWrappedSize) {
    this->m_maxWrappedSize = maxWrappedSize;
}

void EncryptionContext::setWrapSizeLimit(const int& wrapSizeLimit) {
    this->m_wrapSizeLimit = wrapSizeLimit;
}

bool EncryptionContext::isEncryptionReqd() const {
    return m_bEncryptionReqd;
}

int EncryptionContext::getMaxWrappedSize() const {
    return m_maxWrappedSize;
}

int EncryptionContext::getWrapSizeLimit() const {
    return m_wrapSizeLimit;
}

void EncryptionContext::reset() {
    this->m_bEncryptionReqd = false;
    this->m_maxWrappedSize = 65536;
    this->m_wrapSizeLimit = 0;
}

std::ostream& operator<<(std::ostream &contextStream, const EncryptionContext& context) {
    contextStream << " Encryption: " << (context.isEncryptionReqd() ? "enabled" : "disabled");
    contextStream << ", MaxWrappedSize: " << context.getMaxWrappedSize();
    contextStream << ", WrapSizeLimit: " << context.getWrapSizeLimit();
    return contextStream;
}

} // namespace 
