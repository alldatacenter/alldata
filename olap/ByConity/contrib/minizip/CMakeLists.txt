add_library(minizip
    ioapi.c
    ioapi.h
    unzip.c
    unzip.h

    zlib.h
    zconf.h
    )

target_link_libraries(minizip PRIVATE ${ZLIB_LIBRARIES})
