### deploy user
deployUser=hadoop

### ssh port
SSH_PORT=22

##The Max Heap size for the JVM
SERVER_HEAP_SIZE="512M"

##The Port of Streamis
STREAMIS_PORT=9400

### The install home path of STREAMIS，Must provided
STREAMIS_INSTALL_HOME=/appcom/Install/streamis

###  Linkis EUREKA  information.  # Microservices Service Registration Discovery Center
EUREKA_INSTALL_IP=127.0.0.1
EUREKA_PORT=20303

### Specifies the user workspace, which is used to store the user's script files and log files.
### Generally local directory
#WORKSPACE_USER_ROOT_PATH=file:///tmp/linkis/
#### Path to store job ResultSet：file or hdfs path
#RESULT_SET_ROOT_PATH=hdfs:///tmp/linkis

### Linkis Gateway  information
GATEWAY_INSTALL_IP=127.0.0.1
GATEWAY_PORT=9001


################### The install Configuration of all Micro-Services #####################
#
#    NOTICE:
#       1. If you just wanna try, the following micro-service configuration can be set without any settings.
#            These services will be installed by default on this machine.
#       2. In order to get the most complete enterprise-level features, we strongly recommend that you install
#          the following microservice parameters
#

STREAMIS_SERVER_INSTALL_IP=127.0.0.1
STREAMIS_SERVER_INSTALL_PORT=9400

STREAMIS_VERSION=0.2.0

STREAMIS_FILE_NAME="STREAMIS-$STREAMIS_VERSION"