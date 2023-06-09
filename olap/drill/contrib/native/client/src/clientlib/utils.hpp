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
#ifndef __UTILS_H
#define __UTILS_H

#include <sstream>
#include <ostream>
#include <fstream>
#include <string>
#include <vector>

#if defined _WIN32  || defined _WIN64
  //Windows header files redefine 'random'
  #ifdef random
    #undef random
  #endif
#endif
#include <boost/random/mersenne_twister.hpp> // for mt19937
#include <boost/random/random_device.hpp>
#include <boost/random/uniform_int.hpp>
#include <boost/random/variate_generator.hpp>
#include <boost/thread.hpp>

#include "drill/common.hpp"
#include "drill/drillClient.hpp"

namespace Drill{

// Wrapper Class to keep track of allocated memory
class AllocatedBuffer{
    public:
        AllocatedBuffer(size_t l);
        ~AllocatedBuffer();

        ByteBuf_t m_pBuffer;
        size_t    m_bufSize;

        // keep track of allocated memory. The client lib blocks
        // if we have allocated up to a limit (defined in drillClientConfig).
        static boost::mutex s_memCVMutex;
        static boost::condition_variable s_memCV;
        static size_t s_allocatedMem;
        static bool s_isBufferLimitReached;
        static boost::mutex s_utilMutex; // for provideing safety around strtok and other non-reentrant functions

};

class DECLSPEC_DRILL_CLIENT Utils{
    public:
        static boost::random::random_device s_RNG;   //Truly random (expensive and device dependent)
        static boost::random::mt19937 s_URNG; //Pseudo random with a period of ( 2^19937 - 1 )
        static boost::uniform_int<> s_uniformDist;      // Produces a uniform distribution
        static boost::variate_generator<boost::random::mt19937&, boost::uniform_int<> > s_randomNumber; // a random number generator also usable by shuffle

        //allocate memory for Record Batches
        static ByteBuf_t allocateBuffer(size_t len);
        static void freeBuffer(ByteBuf_t b, size_t len);
        static void parseConnectStr(const char* connectStr,
            std::string& pathToDrill,
            std::string& protocol,
            std::string& hostPortStr);

        // useful vector methods/idioms

        // performs a random shuffle on a string vector
        static void shuffle(std::vector<std::string>& vector);

        // adds the contents of vector2 to vector1
        static void add(std::vector<std::string>& vector1, std::vector<std::string>& vector2);

        // removes the element from the vector
        template <typename T> static void eraseRemove(std::vector<T>& vector, T elem){
            vector.erase(std::remove(vector.begin(), vector.end(), elem), vector.end());
        }

        // Provide a to_string that works with older C++ compilers
        template <typename T> static std::string to_string(T val) {
            std::stringstream stream;
            stream << val;
            return stream.str();
        }

}; // Utils

/*
 * Encryption related configuration parameters. The member's are updated with value received from server
 * and also after the SASL Handshake is done.
 */
class EncryptionContext {

	bool m_bEncryptionReqd;
	int m_maxWrappedSize;
	int m_wrapSizeLimit;

public:
	EncryptionContext();

	EncryptionContext(const bool& encryptionReqd, const int& maxWrappedSize, const int& wrapSizeLimit);

	void setEncryptionReqd(const bool& encryptionReqd);

	void setMaxWrappedSize(const int& maxWrappedSize);

	void setWrapSizeLimit(const int& wrapSizeLimit);

	bool isEncryptionReqd() const;

	int getMaxWrappedSize() const;

	int getWrapSizeLimit() const;

	void reset();

	friend std::ostream& operator<<(std::ostream &contextStream, const EncryptionContext& context);
};

} // namespace Drill

#endif
