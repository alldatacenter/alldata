#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#!/bin/bash
set -e

function pause(){
    read -rsp $'Press any key to continue...\n' -n1 key
}

function runCmd(){
    echo " ----------------- "  >> ${DRILL_RELEASE_OUTFILE}
    echo " ----------------- $1 "
    echo " ----------------- $1 " >> ${DRILL_RELEASE_OUTFILE}
    echo " ----------------- "  >> ${DRILL_RELEASE_OUTFILE}
    shift
    echo "Will execute $@"
    # run the command, send output to out file
    "$@" >> ${DRILL_RELEASE_OUTFILE} 2>&1
    if [ $? -ne 0 ]; then
        echo FAILED to run $1
        echo FAILED to run $1 >> ${DRILL_RELEASE_OUTFILE}
        exit 1
    fi
    # Wait for user to verify and continue
    pause
}

function copyFiles(){
		target_dir=${APACHE_DIST_WORKING_COPY}/${DRILL_RELEASE_VERSION}-rc${RELEASE_ATTEMPT}
    rm -rf $target_dir
    mkdir -p $target_dir
    cp ${DRILL_SRC}/target/apache-drill-${DRILL_RELEASE_VERSION}-src.tar.gz* $target_dir/ && \
    cp ${DRILL_SRC}/target/apache-drill-${DRILL_RELEASE_VERSION}-src.zip* $target_dir/ && \
    cp ${DRILL_SRC}/distribution/target/apache-drill-${DRILL_RELEASE_VERSION}.tar.gz* $target_dir/

}

function checkPassphrase(){
    echo "1234" | "$GPG" --batch --passphrase "${GPG_PASSPHRASE}" -o /dev/null -as -
    if [ $? -ne 0 ]; then
        echo "Invalid passphrase. Make sure the default key is set to the key you want to use (or make it the first key in the keyring)."
        exit 1
    fi
}

function createDirectoryIfAbsent() {
    DIR_NAME="$1"
    if [ ! -d "${DIR_NAME}" ]; then
        echo "Creating directory ${DIR_NAME}"
        mkdir -p "${DIR_NAME}"
    fi
}


function readInputAndSetup(){

    read -p "JAVA_HOME of the JDK 8 to use for the release : " JAVA_HOME
    export JAVA_HOME

    read -p "Drill Working Directory : " WORK_DIR
    createDirectoryIfAbsent "${WORK_DIR}"

    read -p "Drill Release Version (e.g. 1.4.0) : " DRILL_RELEASE_VERSION

    read -p "Drill Development Version (e.g. 1.5.0-SNAPSHOT) : " DRILL_DEV_VERSION

    read -p "Release Commit SHA : " RELEASE_COMMIT_SHA

    read -p "Write output to (directory) : " DRILL_RELEASE_OUTDIR
    createDirectoryIfAbsent "${DRILL_RELEASE_OUTDIR}"

    read -p "Staging (personal) repo : " MY_REPO

    read -p "Svn working copy of dist.apache.org/repos/dist/dev/drill : " APACHE_DIST_WORKING_COPY

    read -p "Apache login : " APACHE_LOGIN

    read -p "Release candidate attempt : " RELEASE_ATTEMPT

    read -s -p "GPG Passphrase (Use quotes around a passphrase with spaces) : " GPG_PASSPHRASE

    DRILL_RELEASE_OUTFILE="${DRILL_RELEASE_OUTDIR}/drill_release.out.txt"
    DRILL_SRC=${WORK_DIR}/drill-release

    echo ""
    echo "-----------------"
    echo "JAVA_HOME : " ${JAVA_HOME}
    echo "Drill Working Directory : " ${WORK_DIR}
    echo "Drill Src Directory : " ${DRILL_SRC}
    echo "Drill Release Version : " ${DRILL_RELEASE_VERSION}
    echo "Drill Development Version : " ${DRILL_DEV_VERSION}
    echo "Release Commit SHA : " ${RELEASE_COMMIT_SHA}
    echo "Write output to : " ${DRILL_RELEASE_OUTFILE}
    echo "Staging (personal) repo : " ${MY_REPO}
    echo "Svn working copy of dist.apache.org/repos/dist dir : " ${APACHE_DIST_WORKING_COPY}

    touch ${DRILL_RELEASE_OUTFILE}
}

checkPassphraseNoSpace(){
    # The checkPassphrase function does not work for passphrases with embedded spaces.
    if [ -z "${GPG_PASSPHRASE##* *}" ] ;then
        echo "Passphrase contains a space - No Validation performed."
    else
        echo -n "Validating passphrase .... "
        checkPassphrase && echo "Passphrase accepted" || echo "Invalid passphrase"
    fi
}

cloneRepo(){
    cd ${WORK_DIR}
    rm -rf ./drill-release
    git clone https://github.com/apache/drill.git drill-release  >& ${DRILL_RELEASE_OUTFILE}
    cd ${DRILL_SRC}
    git checkout ${RELEASE_COMMIT_SHA}
}

###### BEGIN  #####
# Location of checksum.sh
export CHKSMDIR="$( cd "$(dirname "$0")" && pwd)"

# Default GPG command to use
GPG="${GPG:-gpg}"

readInputAndSetup
checkPassphraseNoSpace

runCmd "Cloning the repo" cloneRepo

runCmd "Checking the build" mvn install -DskipTests

export MAVEN_OPTS=-Xmx2g
runCmd "Clearing release history" mvn release:clean \
  -Papache-release \
  -DpushChanges=false \
  -DskipTests

export MAVEN_OPTS='-Xmx4g -XX:MaxPermSize=512m'
runCmd "Preparing the release " mvn -X release:prepare \
  -Papache-release \
  -DpushChanges=false \
  -DdevelopmentVersion=${DRILL_DEV_VERSION} \
  -DreleaseVersion=${DRILL_RELEASE_VERSION} \
  -Dtag=drill-${DRILL_RELEASE_VERSION} \
  -Darguments="-Dgpg.passphrase=${GPG_PASSPHRASE} -DskipTests -Dmaven.javadoc.skip=false"

runCmd "Pushing to private repo ${MY_REPO}" git push ${MY_REPO} drill-${DRILL_RELEASE_VERSION}

runCmd "Performing the release to ${MY_REPO}" mvn release:perform \
  -DconnectionUrl=scm:git:${MY_REPO} \
  -DlocalCheckout=true \
  -Darguments="-Dgpg.passphrase=${GPG_PASSPHRASE} -DskipTests"

runCmd "Checking out release commit" git checkout drill-${DRILL_RELEASE_VERSION}

# Remove surrounding quotes
tempGPG_PASSPHRASE="${GPG_PASSPHRASE%\"}"
tempGPG_PASSPHRASE="${tempGPG_PASSPHRASE#\"}"
runCmd "Deploying ..." mvn deploy \
  -Papache-release \
  -DskipTests \
  -Dgpg.passphrase="${tempGPG_PASSPHRASE}"

runCmd "Verifying artifacts are signed correctly" ${CHKSMDIR}/checksum.sh ${DRILL_SRC}/distribution/target/apache-drill-${DRILL_RELEASE_VERSION}.tar.gz
pause

runCmd "Copying release files to your working copy of dist.apache.org/repos/dist/dev/drill" copyFiles

echo "Go to the Apache maven staging repo and close the new jar release"
echo "and go to ${APACHE_DIST_WORKING_COPY} and svn add/commit the new"
echo "release candidate after checking the pending changes there."
pause

echo "Start the vote \(good luck\)\n"
