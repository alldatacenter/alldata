set(LIBNUMA_SOURCE_DIR ${CMAKE_SOURCE_DIR}/contrib/numactl)

set(SRCS
        ${LIBNUMA_SOURCE_DIR}/libnuma.c
        ${LIBNUMA_SOURCE_DIR}/syscall.c
        ${LIBNUMA_SOURCE_DIR}/distance.c
        ${LIBNUMA_SOURCE_DIR}/affinity.c
        ${LIBNUMA_SOURCE_DIR}/sysfs.c
        ${LIBNUMA_SOURCE_DIR}/rtnetlink.c
        ${LIBNUMA_SOURCE_DIR}/util.c
        ${LIBNUMA_SOURCE_DIR}/versions.ldscript
        )

add_library(libnuma ${SRCS})
target_include_directories(libnuma PUBLIC ${LIBNUMA_SOURCE_DIR})
