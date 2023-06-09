drill-mapr-plugin
=================
By default all the tests in contrib/format-maprdb are disabled.
To enable and run these tests please use -Pmapr profile to
compile and execute the tests.

Here is an example of the mvn command to use to run these tests.
mvn install -Dtests=cluster -Pmapr
