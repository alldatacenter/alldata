This project contains the protobuf definition files used by Drill.

The java sources are generated into src/main/java and checked in.

To regenerate the sources after making changes to .proto files
---------------------------------------------------------------
1. Ensure that the protobuf 'protoc' tool (version 3.11.1 or newer (but 3.x series)) is
in your PATH (you may need to download and build it first). You can 
download it from https://github.com/protocolbuffers/protobuf/releases/tag/v3.11.1.

2. In protocol dir, run "mvn process-sources -P proto-compile" or "mvn clean install -P proto-compile".

3. Check in the new/updated files.

---------------------------------------------------------------
If changes are made to the DrillClient's protobuf, you would need to regenerate the sources for the C++ client as well.
Steps for regenerating the sources are available https://github.com/apache/drill/blob/master/contrib/native/client/

You can use any of the following platforms specified in the above location to regenerate the protobuf sources:
readme.linux	: Regenerating on Linux
readme.macos	: Regenerating on MacOS
readme.win.txt	: Regenerating on Windows
