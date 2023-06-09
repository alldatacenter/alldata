add_library(biginteger
        BigInteger.cc
        BigUnsigned.cc
        BigIntegerUtils.cc
        BigIntegerAlgorithms.cc
        BigUnsignedInABase.cc
        )
target_include_directories(biginteger PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})
