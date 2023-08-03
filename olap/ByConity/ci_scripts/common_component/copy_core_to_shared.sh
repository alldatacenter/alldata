set -x
# copy core file to shared folder
CORE_FILE_PATH='/test_output/'
ARTIFACT_FOLDER_PATH='/test_output/coredump'
MINIDUMP_FILE_PATH='/test_output/'

for FILES in  $(ls -l ${CORE_FILE_PATH}  | grep -v ^d | grep "core." | awk '{print $9}' )
do
  echo "block_ci_flag=1" >> block_ci_flag
  mkdir -p ${ARTIFACT_FOLDER_PATH}
  for i in FILES:
  do
    gdb -q ${GITHUB_WORKSPACE}/build/programs/clickhouse /${FILES} -ex "set pagination off" -ex bt -ex "quit" |& tee ${ARTIFACT_FOLDER_PATH}/${FILES}_coredump_backtrace.log  # use gdb to get stack information
    python3 /${GITHUB_WORKSPACE}/.codebase/ci_scripts/common_component/extract_gdb_log.py --log-path ${ARTIFACT_FOLDER_PATH}/${FILES}_coredump_backtrace.log
    cp -r /${FILES} ${ARTIFACT_FOLDER_PATH}/.
    done
done

for FILES in  $(find ${MINIDUMP_FILE_PATH} -name "*.dmp" )
do
  echo "block_ci_flag=1" >> block_ci_flag
  for i in FILES:
  do
    echo $FILES
    minidump-2-core $FILES > $FILES.core
    gdb -q ${GITHUB_WORKSPACE}/build/programs/clickhouse $FILES.core -ex "set pagination off" -ex "bt" -ex "quit" > ${FILES}_minidump_backtrace.log
    python3 /${GITHUB_WORKSPACE}/.codebase/ci_scripts/common_component/extract_gdb_log.py --log-path ${FILES}_minidump_backtrace.log
  done
done

