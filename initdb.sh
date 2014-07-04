#!/bin/sh
#
# Initialize the db with some data for testing
#

MONGO_DBNAME=test

# reset the db
echo "Resetting the db....."
./runJavadoc.sh ResetMongoMain --db

# build the doclet path
dp=target/classes
for x in `find target/dependency`; do dp="$dp;$x"; done

./sajavadoc.sh /java/jdk/1.7 -sourcepath /fox/tmp/javadoc/src-jdk7 -subpackages java.lang -subpackages java.util -subpackages java.io 

./sajavadoc.sh /java/com.surfapi/1.0 -sourcepath src/test/java  com.surfapi.test
./sajavadoc.sh /java/com.surfapi/1.0 -sourcepath src/main/java  com.surfapi.proc
./sajavadoc.sh /java/com.surfapi/0.9 -sourcepath src/test/java  com.surfapi.test

./sajavadoc.sh /java/mongo-java-driver/2.9.3 -sourcepath /fox/tmp/surfapi-heroku/mongo-java-driver-2.9.3 -subpackages com -subpackages org
./sajavadoc.sh /java/javax.json/1.0.2 -sourcepath /fox/tmp/surfapi-heroku/javax.json-1.0.2 -subpackages javax
./sajavadoc.sh /java/javaee/6.0 -sourcepath /fox/tmp/surfapi-heroku/javaee-api-6.0 -subpackages javax
./sajavadoc.sh /java/javax.enterprise.concurrent/1.0 -sourcepath /fox/tmp/surfapi-heroku/javax.enterprise.concurrent-1.0 -subpackages javax
./sajavadoc.sh /java/javax.batch/1.0 -sourcepath /fox/tmp/surfapi-heroku/javax.batch-api-1.0 -subpackages javax
./sajavadoc.sh /java/jaxrs/2.3.1 -sourcepath /fox/tmp/surfapi-heroku/jaxrs-api-2.3.1 -subpackages javax
