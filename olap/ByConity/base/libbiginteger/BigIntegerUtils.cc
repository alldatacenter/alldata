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

#include "BigIntegerUtils.hh"
#include "BigUnsignedInABase.hh"
#include <iostream>

std::string bigUnsignedToString(const BigUnsigned &x) {
	return std::string(BigUnsignedInABase(x, 10));
}

std::string bigIntegerToString(const BigInteger &x) {
	return (x.getSign() == BigInteger::negative)
		? (std::string("-") + bigUnsignedToString(x.getMagnitude()))
		: (bigUnsignedToString(x.getMagnitude()));
}

BigUnsigned stringToBigUnsigned(const char *s, const size_t & length)
{
    return BigUnsigned(BigUnsignedInABase(s, length,10));
}

BigUnsigned stringToBigUnsigned(const std::string &s) {
	return stringToBigUnsigned(s.c_str(), s.length());
}

BigInteger stringToBigInteger(const std::string &s) {
	// Recognize a sign followed by a BigUnsigned.
	return (s[0] == '-') ? BigInteger(stringToBigUnsigned(s.substr(1, s.length() - 1)), BigInteger::negative)
		: (s[0] == '+') ? BigInteger(stringToBigUnsigned(s.substr(1, s.length() - 1)))
		: BigInteger(stringToBigUnsigned(s));
}

BigInteger stringToBigInteger(const char *s, const size_t & length) {
    // Recognize a sign followed by a BigUnsigned.
    const char *data = s;
    if (length > 0)
    {
        if (*data == '-')
            return BigInteger(stringToBigUnsigned(++data, length - 1), BigInteger::negative);
        else if (*data == '+')
            return BigInteger(stringToBigUnsigned(++data, length - 1));
    }
    return BigInteger(stringToBigUnsigned(data, length));
}

bool isDigit(const char *s, const size_t & length)
{
    if (length == 0)
        return false;
    const char *data = s;
    size_t remaining_length = length;
    if (*data == '-' || *data == '+')
    {
        ++data;
        --remaining_length;
        if (remaining_length == 0)
            return false;
    }

    while (remaining_length > 0)
    {
        if (*data < '0' || *data > '9')
            return false;
        ++data;
        --remaining_length;
    }
    return true;
}

std::ostream &operator <<(std::ostream &os, const BigUnsigned &x) {
	BigUnsignedInABase::Base base;
	long osFlags = os.flags();
	if (osFlags & os.dec)
		base = 10;
	else if (osFlags & os.hex) {
		base = 16;
		if (osFlags & os.showbase)
			os << "0x";
	} else if (osFlags & os.oct) {
		base = 8;
		if (osFlags & os.showbase)
			os << '0';
	} else
		throw "std::ostream << BigUnsigned: Could not determine the desired base from output-stream flags";
	std::string s = std::string(BigUnsignedInABase(x, base));
	os << s;
	return os;
}

std::ostream &operator <<(std::ostream &os, const BigInteger &x) {
	if (x.getSign() == BigInteger::negative)
		os << '-';
	os << x.getMagnitude();
	return os;
}
