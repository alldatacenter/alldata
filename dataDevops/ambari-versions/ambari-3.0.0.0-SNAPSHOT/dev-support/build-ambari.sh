#!/usr/bin/env bash
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.


# Clean up files from previous build to make sure there are no artifacts that would trigger rat failure.
pushd .
cd $WORKSPACE
git clean -xdf
popd

# We stash node.js installation into this weird place
# because on one hand we want it to be somewhere inside
# the current workspace and on the other hand we don't
# want RAT to be ran on files that comprise it. The following
# location is excluded from RAT by default
REPO_NAME=contrib/views/node.js/target

[ -d "$REPO_NAME"  ] && exit 0

rm -rf $REPO_NAME
mkdir -p $REPO_NAME.tmp
pushd $REPO_NAME.tmp
  curl --retry 3 --retry-delay 5 -k http://nodejs.org/dist/v4.5.0/node-v4.5.0-linux-x64.tar.gz | tar xzvf -
  mv node-v4.5.0-linux-x64/* .
  rmdir node-v4.5.0-linux-x64
popd
mv $REPO_NAME.tmp $REPO_NAME

export PATH=`pwd`/$REPO_NAME/bin:$PATH
npm install -g brunch@1.7.20

export _JAVA_OPTIONS="-Xmx2048m -Djava.awt.headless=true"
export JAVA_HOME=/home/jenkins/tools/java/latest1.8
export MAVEN_HOME=/home/jenkins/tools/maven/latest
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
PATH=`pwd`/contrib/views/node.js/target/bin:$PATH
echo "Checking Hudson Java"
ls -l /home/hudson/tools/java
echo "Checking Jenkins Java"
ls -l /home/jenkins/tools/java
which java
java -version
python -V
uname -a

cd $WORKSPACE
$MAVEN_HOME/bin/mvn -fae clean install

