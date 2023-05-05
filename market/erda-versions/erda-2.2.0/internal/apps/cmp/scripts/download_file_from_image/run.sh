#!/bin/bash

set -o errexit -o nounset -o pipefail
source env.sh

if [[ -z "${NETDATA_AGENT_PATH}" ]]; then
    echo "no agent path"
    exit 1
fi

# args:
# IMAGE_NAME: image name
# IMAGE_FILE_PATH: file path in image
# FILE_NAME: file name copied to the outside
# EXPECT_MD5: file md5
# EXECUTABLE: Is file executable

tempFileName=tmpfile-$RANDOM
destPath="/netdata/${NETDATA_AGENT_PATH}/${FILE_NAME}"

# destPath may be mistakenly created as a directory or other non-regular File
if [[ ! -f "${destPath}" ]]; then
    rm -fr "${destPath}"
    mkdir -p $(dirname "${destPath}")
fi

# If the target file already exists
if [[ -f "$destPath" ]]; then
    currentMD5="$(md5sum ${destPath} | cut -d ' ' -f1)"
    if [[ "${EXPECT_MD5}" == "${currentMD5}" ]]; then
        echo "current file MD5 matched, exit directly."
        exit 0
    fi
    echo "MD5 not match, expect: ${EXPECT_MD5} current: ${currentMD5}"
fi

echo "begin download file to ${destPath}"
tmpContainerID=$(docker create "${IMAGE_NAME}")
docker cp "${tmpContainerID}:${IMAGE_FILE_PATH}" ${tempFileName}
docker rm ${tmpContainerID}
mv -f "${tempFileName}" "${destPath}"

if [[ "${EXECUTABLE}" == "true" ]]; then
    chmod +x "${destPath}"
fi
