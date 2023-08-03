# # option (ENABLE_DATASKETCHES "Enable DataSketches" ${ENABLE_LIBRARIES})
#
# if (ENABLE_DATASKETCHES)
#
# option (USE_INTERNAL_DATASKETCHES_LIBRARY "Set to FALSE to use system DataSketches library instead of bundled" ${NOT_UNBUNDLED})
#
# if (NOT EXISTS "${ClickHouse_SOURCE_DIR}/contrib/datasketches-cpp/theta/CMakeLists.txt")
#     if (USE_INTERNAL_DATASKETCHES_LIBRARY)
#        message(WARNING "submodule contrib/datasketches-cpp is missing. to fix try run: \n git submodule update --init --recursive")
#     endif()
#     set(MISSING_INTERNAL_DATASKETCHES_LIBRARY 1)
#     set(USE_INTERNAL_DATASKETCHES_LIBRARY 0)
# endif()
#
# if (USE_INTERNAL_DATASKETCHES_LIBRARY)
#     set(DATASKETCHES_LIBRARY theta)
#     set(DATASKETCHES_INCLUDE_DIR "${ClickHouse_SOURCE_DIR}/contrib/datasketches-cpp/common/include" "${ClickHouse_SOURCE_DIR}/contrib/datasketches-cpp/theta/include")
# elseif (NOT MISSING_INTERNAL_DATASKETCHES_LIBRARY)
#     find_library(DATASKETCHES_LIBRARY theta)
#     find_path(DATASKETCHES_INCLUDE_DIR NAMES theta_sketch.hpp PATHS ${DATASKETCHES_INCLUDE_PATHS})
# endif()
#
# if (DATASKETCHES_LIBRARY AND DATASKETCHES_INCLUDE_DIR)
#     set(USE_DATASKETCHES 1)
# endif()
#
# endif()
#
# message (STATUS "Using datasketches=${USE_DATASKETCHES}: ${DATASKETCHES_INCLUDE_DIR} : ${DATASKETCHES_LIBRARY}")

# if (NOT EXISTS "${ClickHouse_SOURCE_DIR}/contrib/datasketches-cpp")
#     message(SEND_ERROR "Error: submodule contrib/datasketches-cpp is missing and it is required. to fix try run: \n git submodule update --init --recursive")
#     return()
# endif ()
#

include(ExternalProject)
# add_subdirectory(datasketches-cpp)
cmake_policy(SET CMP0097 NEW)
include(ExternalProject)
ExternalProject_Add(datasketches_proj
    SOURCE_DIR ${CMAKE_SOURCE_DIR}/contrib/datasketches-cpp
    INSTALL_DIR ${CMAKE_BINARY_DIR}/datasketches-prefix
    CMAKE_ARGS -DBUILD_TESTS=OFF -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE} -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/datasketches-prefix

    # Override the install command to add DESTDIR
    # This is necessary to work around an oddity in the RPM (but not other) package
    # generation, as CMake otherwise picks up the Datasketch files when building
    # an RPM for a dependent package. (RPM scans the directory for files in addition to installing
    # those files referenced in an "install" rule in the cmake file)
    INSTALL_COMMAND env DESTDIR= ${CMAKE_COMMAND} --build . --target install
)
ExternalProject_Get_property(datasketches_proj INSTALL_DIR)
set(datasketches_INSTALL_DIR ${INSTALL_DIR})
message("Source dir of datasketches_proj = ${datasketches_INSTALL_DIR}")
add_library(datasketches_lib INTERFACE)
target_include_directories(datasketches_lib
                            INTERFACE ${datasketches_INSTALL_DIR}/include)
add_dependencies(datasketches_lib datasketches_proj)
set(USE_DATASKETCHES 1)