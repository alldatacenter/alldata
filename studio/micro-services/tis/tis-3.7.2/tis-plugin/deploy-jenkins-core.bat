mvn deploy:deploy-file ^
 -DgroupId=com.qlangtech.tis ^
 -DartifactId=tis-jenkins-core ^
 -Dversion=2.7.3 ^
 -Dpackaging=jar ^
 -Dsources=D:\j2ee_solution\mvn_repository\org\jenkins-ci\main\jenkins-core\2.7.3\bak\jenkins-core-2.7.3-sources.jar ^
 -DpomFile=./jenkins-core-2.7.3.pom ^
 -Dfile=D:\j2ee_solution\mvn_repository\org\jenkins-ci\main\jenkins-core\2.7.3\bak\jenkins-core-2.7.3.jar ^
 -DrepositoryId=releases ^
 -Durl=http://nexus.2dfire-dev.com/repository/thirdparty/