###Build and test

Note: must run mvn package before running tests in order to copy dependency jars into 
the target directory (some of the tests need them).

$ mvn package -DskipTests=true
$ mvn test
