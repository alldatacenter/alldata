# Script Test Overview

The tests here exercise the Drill shell scripts with a wide variety of options.
The Drill scripts are designed to be run from the command line or from YARN.
The scripts allow passing in values from a vendor-specific configuration file
($DRILL_HOME/conf/distrib-env.sh), a user-specific configuration file
($DRILL_SITE/drill-env.sh) or from environment variables set by YARN.

Testing scripts is normally tedious because the scripts are designed to start
a process, perhaps a Drillbit or Sqlline. To make automated tests possible,
the scripts incorporate a "shim": an environment variable that, if set, is
used to put a "wrapper" script around the Java execution line. The wrapper
captures the environment and the command line, and generates stderr and
stdout output. The test progams use this captured output to determine if
the Java command line has the options we expect. (We boldly assume that
if we give Java the right options, it will do the right thing with them.)

Why are the script tests in the drill-yarn project? Because YARN is the most
sensitive to any changes: YARN provides several levels of indirection between
the user and the scripts; the scripts must work exactly as the Drill-on-YARN
code expects (or visa-versa) or things tend to break.

## Inputs

The test program needs the following inputs:

- The scripts, which are copied from the source tree. (Need details)
- /src/test/resources/wrapper.sh which is the "magic" wrapper script
for capturing the command line, etc.
- A temporary directory where the program can build its mock Drill
and site directories.

## Running the Tests

Simply run the tests. Each test sets up its distribution and
optional site directory and required environment variables. Each
test uses a builder to build up the required script launch and
to analyze the results. Each test function usually does a single
setup, then does a bunch of test runs against that environment.

Each test uses "gobbler" threads to read the stdout and stderr
from the test run. If you run the test in a debugger, you'll
see a steady stream of threads come and go. This is a test, so
we don't bother with a thread pool; we just brute-force create
new threads as needed.

## Extending the Tests

You should extend the tests if you:

- Add new environment variables to any script.
- Change the logic of any script.
- Find a bug that these tests somehow did not catch.

Note that it is very important to ensure that each new enviornment
variable or other option works when set in distrib-env.sh,
drill-env.sh or in the environment. These tests are the only (sane)
way to test the many combinations, and to do that on each
subsequent change.

## About the Code

TestScripts.java are the tests, organized by functional area. ScriptUtils.java
is a large number of (mostly ad-hoc) utilities needed to set up, run, tear down
and analyze the results of each run.