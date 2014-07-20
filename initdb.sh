#!/bin/sh
#
# Initialize the db with some data for testing
#

# reset the db
echo "Resetting the db....."
./runJavadoc.sh ResetMongoMain --db

MONGOLAB_URI=mongodb://localhost/test

./sajavadoc.sh $MONGOLAB_URI /java/jdk/1.7 -sourcepath /fox/tmp/javadoc/src-jdk7 \
    -subpackages java \
    -subpackages javax \
    -subpackages org.omg \
    -subpackages org.w3c \
    -subpackages org.xml

./sajavadoc.sh $MONGOLAB_URI /java/com.surfapi/1.0 -sourcepath src/test/java  com.surfapi.test
./sajavadoc.sh $MONGOLAB_URI /java/com.surfapi/1.0 -sourcepath src/main/java  com.surfapi.proc
./sajavadoc.sh $MONGOLAB_URI /java/com.surfapi/0.9 -sourcepath src/test/java  com.surfapi.test

./sajavadoc.sh $MONGOLAB_URI /java/mongo-java-driver/2.9.3 -sourcepath /fox/tmp/surfapi-heroku/mongo-java-driver-2.9.3 -subpackages com -subpackages org
./sajavadoc.sh $MONGOLAB_URI /java/javax.json/1.0.2 -sourcepath /fox/tmp/surfapi-heroku/javax.json-1.0.2 -subpackages javax
./sajavadoc.sh $MONGOLAB_URI /java/javax.enterprise.concurrent/1.0 -sourcepath /fox/tmp/surfapi-heroku/javax.enterprise.concurrent-1.0 -subpackages javax
./sajavadoc.sh $MONGOLAB_URI /java/javax.batch/1.0 -sourcepath /fox/tmp/surfapi-heroku/javax.batch-api-1.0 -subpackages javax
./sajavadoc.sh $MONGOLAB_URI /java/jaxrs/2.3.1 -sourcepath /fox/tmp/surfapi-heroku/jaxrs-api-2.3.1 -subpackages javax

./sajavadoc.sh $MONGOLAB_URI /java/org.osgi.core/5.0.0 -sourcepath /fox/tmp/surfapi-heroku/org.osgi.core-5.0.0 -subpackages org

# TODO: not all source files have javadoc
./sajavadoc.sh $MONGOLAB_URI /java/javaee/6.0 -sourcepath /fox/tmp/surfapi-heroku/javaee-api-6.0 -subpackages javax


