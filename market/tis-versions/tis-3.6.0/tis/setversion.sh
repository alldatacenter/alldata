mvn versions:set -DnewVersion=3.6.0  -DprocessDependencies=true -DgenerateBackupPoms=false

mvn compile deploy -pl datax-config,maven-tpi-plugin -am -Dmaven.test.skip=true -DaltDeploymentRepository=base::default::http://localhost:8080/release
