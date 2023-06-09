cmake_minimum_required(VERSION 3.1.0)
project(libbreakpad)

# external breakpad repo
set(BREAKPAD_SRC_DIR "${ClickHouse_SOURCE_DIR}/contrib/breakpad/src")
set(LIBBREAKPAD_CLIENT_SRCS
    ${BREAKPAD_SRC_DIR}/client/linux/crash_generation/crash_generation_client.cc
    ${BREAKPAD_SRC_DIR}/client/linux/crash_generation/crash_generation_server.cc
    ${BREAKPAD_SRC_DIR}/client/linux/dump_writer_common/thread_info.cc
    ${BREAKPAD_SRC_DIR}/client/linux/dump_writer_common/ucontext_reader.cc
    ${BREAKPAD_SRC_DIR}/client/linux/handler/exception_handler.cc
    ${BREAKPAD_SRC_DIR}/client/linux/handler/minidump_descriptor.cc
    ${BREAKPAD_SRC_DIR}/client/linux/log/log.cc
    ${BREAKPAD_SRC_DIR}/client/linux/microdump_writer/microdump_writer.cc
    ${BREAKPAD_SRC_DIR}/client/linux/minidump_writer/linux_core_dumper.cc
    ${BREAKPAD_SRC_DIR}/client/linux/minidump_writer/linux_dumper.cc
    ${BREAKPAD_SRC_DIR}/client/linux/minidump_writer/linux_ptrace_dumper.cc
    ${BREAKPAD_SRC_DIR}/client/linux/minidump_writer/minidump_writer.cc
    ${BREAKPAD_SRC_DIR}/client/minidump_file_writer.cc
    ${BREAKPAD_SRC_DIR}/common/convert_UTF.c
    ${BREAKPAD_SRC_DIR}/common/md5.cc
    ${BREAKPAD_SRC_DIR}/common/string_conversion.cc
    ${BREAKPAD_SRC_DIR}/common/linux/elf_core_dump.cc
    ${BREAKPAD_SRC_DIR}/common/linux/elfutils.cc
    ${BREAKPAD_SRC_DIR}/common/linux/file_id.cc
    ${BREAKPAD_SRC_DIR}/common/linux/guid_creator.cc
    ${BREAKPAD_SRC_DIR}/common/linux/linux_libc_support.cc
    ${BREAKPAD_SRC_DIR}/common/linux/memory_mapped_file.cc
    ${BREAKPAD_SRC_DIR}/common/linux/safe_readlink.cc
)
add_library(breakpad_client ${LIBBREAKPAD_CLIENT_SRCS})
target_compile_definitions(breakpad_client PUBLIC HAVE_CONFIG_H)
target_include_directories(breakpad_client PUBLIC ${BREAKPAD_SRC_DIR})
target_include_directories(breakpad_client PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})  # for config.h


set(LIBDISASM_DIR ${BREAKPAD_SRC_DIR}/third_party/libdisasm)
set(LIBDISASM_SRCS
    ${LIBDISASM_DIR}/ia32_implicit.c
    ${LIBDISASM_DIR}/ia32_insn.c
    ${LIBDISASM_DIR}/ia32_invariant.c
    ${LIBDISASM_DIR}/ia32_modrm.c
    ${LIBDISASM_DIR}/ia32_invariant.c
    ${LIBDISASM_DIR}/ia32_opcode_tables.c
    ${LIBDISASM_DIR}/ia32_operand.c
    ${LIBDISASM_DIR}/ia32_reg.c
    ${LIBDISASM_DIR}/ia32_settings.c
    ${LIBDISASM_DIR}/x86_disasm.c
    ${LIBDISASM_DIR}/x86_format.c
    ${LIBDISASM_DIR}/x86_imm.c
    ${LIBDISASM_DIR}/x86_insn.c
    ${LIBDISASM_DIR}/x86_misc.c
    ${LIBDISASM_DIR}/x86_operand_list.c
)
add_library(disasm ${LIBDISASM_SRCS})
target_compile_definitions(disasm PUBLIC HAVE_CONFIG_H)


set(PROCESSOR_SRC_DIR ${BREAKPAD_SRC_DIR}/processor)
set(MINIDUMP_STACKWALK_SRCS
    ${BREAKPAD_SRC_DIR}/common/path_helper.cc
    ${PROCESSOR_SRC_DIR}/microdump_stackwalk.cc
    ${PROCESSOR_SRC_DIR}/basic_code_modules.cc
    ${PROCESSOR_SRC_DIR}/basic_source_line_resolver.cc
    ${PROCESSOR_SRC_DIR}/call_stack.cc
    ${PROCESSOR_SRC_DIR}/convert_old_arm64_context.cc
    ${PROCESSOR_SRC_DIR}/cfi_frame_info.cc
    ${PROCESSOR_SRC_DIR}/disassembler_x86.cc
    ${PROCESSOR_SRC_DIR}/dump_context.cc
    ${PROCESSOR_SRC_DIR}/dump_object.cc
    ${PROCESSOR_SRC_DIR}/logging.cc
    ${PROCESSOR_SRC_DIR}/microdump.cc
    ${PROCESSOR_SRC_DIR}/microdump_processor.cc
    ${PROCESSOR_SRC_DIR}/pathname_stripper.cc
    ${PROCESSOR_SRC_DIR}/process_state.cc
    ${PROCESSOR_SRC_DIR}/simple_symbol_supplier.cc
    ${PROCESSOR_SRC_DIR}/source_line_resolver_base.cc
    ${PROCESSOR_SRC_DIR}/stack_frame_cpu.cc
    ${PROCESSOR_SRC_DIR}/stack_frame_symbolizer.cc
    ${PROCESSOR_SRC_DIR}/stackwalk_common.cc
    ${PROCESSOR_SRC_DIR}/stackwalker.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_address_list.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_amd64.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_arm.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_arm64.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_mips.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_ppc.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_ppc64.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_sparc.cc
    ${PROCESSOR_SRC_DIR}/stackwalker_x86.cc
    ${PROCESSOR_SRC_DIR}/tokenize.cc
)
add_executable(minidump_stackwalk ${MINIDUMP_STACKWALK_SRCS})
target_compile_definitions(minidump_stackwalk PUBLIC HAVE_CONFIG_H)
target_include_directories(minidump_stackwalk PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${BREAKPAD_SRC_DIR})
target_link_libraries(minidump_stackwalk disasm)


set(MINIDUMP_DUMP_SRCS
    ${PROCESSOR_SRC_DIR}/minidump_dump.cc
    ${PROCESSOR_SRC_DIR}/basic_code_modules.cc
    ${PROCESSOR_SRC_DIR}/convert_old_arm64_context.cc
    ${PROCESSOR_SRC_DIR}/dump_context.cc
    ${PROCESSOR_SRC_DIR}/dump_object.cc
    ${PROCESSOR_SRC_DIR}/logging.cc
    ${PROCESSOR_SRC_DIR}/minidump.cc
    ${PROCESSOR_SRC_DIR}/pathname_stripper.cc
    ${PROCESSOR_SRC_DIR}/proc_maps_linux.cc
)
add_executable(minidump_dump ${MINIDUMP_DUMP_SRCS})
target_compile_definitions(minidump_dump PUBLIC HAVE_CONFIG_H)
target_include_directories(minidump_dump PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${BREAKPAD_SRC_DIR})


set(MINIDUMP_2_CORE_SRCS
    ${BREAKPAD_SRC_DIR}/common/linux/memory_mapped_file.cc
    ${BREAKPAD_SRC_DIR}/common/path_helper.cc
    ${BREAKPAD_SRC_DIR}/tools/linux/md2core/minidump-2-core.cc
)
add_executable(minidump-2-core ${MINIDUMP_2_CORE_SRCS})
target_compile_definitions(minidump-2-core PUBLIC HAVE_CONFIG_H)
target_include_directories(minidump-2-core PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${BREAKPAD_SRC_DIR})


set(CORE2MD_SRCS
    ${BREAKPAD_SRC_DIR}/tools/linux/core2md/core2md.cc
)
add_executable(core2md ${CORE2MD_SRCS})
target_compile_definitions(core2md PUBLIC HAVE_CONFIG_H)
target_link_libraries(core2md breakpad_client)


set(DUMP_SYMS_SRCS
    ${BREAKPAD_SRC_DIR}/common/dwarf_cfi_to_module.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf_cu_to_module.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf_line_to_module.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf_range_list_handler.cc
    ${BREAKPAD_SRC_DIR}/common/language.cc
    ${BREAKPAD_SRC_DIR}/common/module.cc
    ${BREAKPAD_SRC_DIR}/common/path_helper.cc
    ${BREAKPAD_SRC_DIR}/common/stabs_reader.cc
    ${BREAKPAD_SRC_DIR}/common/stabs_to_module.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf/bytereader.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf/dwarf2diehandler.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf/dwarf2reader.cc
    ${BREAKPAD_SRC_DIR}/common/dwarf/elf_reader.cc
    ${BREAKPAD_SRC_DIR}/common/linux/crc32.cc
    ${BREAKPAD_SRC_DIR}/common/linux/dump_symbols.cc
    ${BREAKPAD_SRC_DIR}/common/linux/elf_symbols_to_module.cc
    ${BREAKPAD_SRC_DIR}/common/linux/elfutils.cc
    ${BREAKPAD_SRC_DIR}/common/linux/file_id.cc
    ${BREAKPAD_SRC_DIR}/common/linux/linux_libc_support.cc
    ${BREAKPAD_SRC_DIR}/common/linux/memory_mapped_file.cc
    ${BREAKPAD_SRC_DIR}/common/linux/safe_readlink.cc
    ${BREAKPAD_SRC_DIR}/tools/linux/dump_syms/dump_syms.cc
)
add_executable(dump_syms ${DUMP_SYMS_SRCS})
target_compile_definitions(dump_syms PUBLIC HAVE_CONFIG_H)
target_include_directories(dump_syms PRIVATE ${CMAKE_CURRENT_SOURCE_DIR} ${BREAKPAD_SRC_DIR})
