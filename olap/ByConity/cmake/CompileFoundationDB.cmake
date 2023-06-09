function(compile_fdb)
  cmake_minimum_required(VERSION 3.25)
  set(options)
  set(oneValueArgs TARGET)
  set(multiValueArgs)
  cmake_parse_arguments(COMPILE_FDB "${options}" "${oneValueArgs}"
                          "${multiValueArgs}" ${ARGN} )

  include(ExternalProject)
  message(INFO "fdbTarget ${COMPILE_FDB_TARGET}")
  set(FDB_DOWNLOAD_DIR "${CMAKE_BINARY_DIR}/fdb_download")
  set(FDB_LOG_DIR "${CMAKE_BINARY_DIR}/fdb_log")
  set(FDB_SRC_DIR "${CMAKE_BINARY_DIR}/fdb_src")
  set(FDB_BIN_DIR "${CMAKE_BINARY_DIR}/fdb_build")
  set(FDB_INSTALL_DIR "${CMAKE_BINARY_DIR}/fdb_install")
  ExternalProject_add("${COMPILE_FDB_TARGET}Project"
    URL                "https://github.com/apple/foundationdb/archive/refs/tags/7.1.27.tar.gz"
    URL_HASH           SHA256=406200e98ea64883dcd99a9a6b9c0f07aac76a11f0b416b5863c562ec85d9583
    BUILD_COMMAND      cmake --build ${FDB_BIN_DIR}
    BUILD_IN_SOURCE    OFF
    CMAKE_GENERATOR    Ninja
    CONFIGURE_COMMAND  cmake -DCMAKE_C_COMPILER=gcc -DCMAKE_CXX_COMPILER=g++ -DCMAKE_INSTALL_PREFIX=${FDB_INSTALL_DIR} -B ${FDB_BIN_DIR} -S ${FDB_SRC_DIR} -G Ninja

    SOURCE_DIR         ${FDB_SRC_DIR}
    BINARY_DIR         ${FDB_BIN_DIR}
    LOG_DIR            ${FDB_LOG_DIR}
    DOWNLOAD_DIR       ${FDB_DOWNLOAD_DIR}
    INSTALL_DIR        ${FDB_INSTALL_DIR}
    INSTALL_COMMAND    cmake --install ${FDB_BIN_DIR}
    UPDATE_COMMAND     ""
    DOWNLOAD_EXTRACT_TIMESTAMP ON
    BUILD_BYPRODUCTS   "${FDB_INSTALL_DIR}/lib/libfdb_c.so"
                       "${FDB_INSTALL_DIR}/include/foundationdb/fdb_c.h"
                       "${FDB_INSTALL_DIR}/include/foundationdb/fdb_c_options.g.h"
                       "${FDB_INSTALL_DIR}/include/foundationdb/fdb_c_types.h"
                       "${FDB_INSTALL_DIR}/include/foundationdb/fdb.options"
                       "${FDB_SRC_DIR}/flow/error_definitions.h")
  add_custom_command(
    OUTPUT ${FDB_INSTALL_DIR}/include/foundationdb/error_definitions.h
    COMMAND ${CMAKE_COMMAND} -E copy
            ${FDB_SRC_DIR}/flow/error_definitions.h
            ${FDB_INSTALL_DIR}/include/foundationdb/error_definitions.h
    DEPENDS ${FDB_SRC_DIR}/flow/error_definitions.h)
  add_custom_target(fdb_error_def ALL DEPENDS ${FDB_INSTALL_DIR}/include/foundationdb/error_definitions.h)
  add_dependencies(fdb_error_def ${COMPILE_FDB_TARGET}Project)
  add_library(${COMPILE_FDB_TARGET}_c STATIC IMPORTED)
  add_dependencies(${COMPILE_FDB_TARGET}_c ${COMPILE_FDB_TARGET}Project fdb_error_def)
  set_target_properties(${COMPILE_FDB_TARGET}_c PROPERTIES IMPORTED_LOCATION "${FDB_INSTALL_DIR}/lib/libfdb_c.so")
  target_include_directories(${COMPILE_FDB_TARGET}_c INTERFACE "${FDB_INSTALL_DIR}/include")

endfunction(compile_fdb)

find_package(FoundationDB-Client)
set(FORCE_FDB_BUILD OFF CACHE BOOL "Forces cmake to build FoundationDB and ignores any installed FoundationDB")
if(FoundationDB-Client_FOUND AND NOT FORCE_FDB_BUILD)
  message(STATUS "Found FoundationDB-Client package in the system, will use it")
else()
  if(FORCE_FDB_BUILD)
    message(STATUS "Compile FoundationDB because FORCE_FDB_BUILD is set")
  else()
    message(STATUS "Didn't find FoundationDB-Client -- will compile from source")
  endif()
  compile_fdb(TARGET fdb)
endif()
