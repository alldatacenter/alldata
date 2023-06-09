set -ex
if [ -f "block_ci_flag" ];then
  echo "FAIL, core or minidump file detected in CI"
  exit 1
fi
