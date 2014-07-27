#!/bin/sh
#
# Initialize the db with some data for testing
#

# reset the db
echo "Resetting the db....."
./surfapi.sh drop --db

MONGOLAB_TEST=mongodb://localhost/test

./sajavadoc.sh $MONGOLAB_TEST /java/jdk/1.7 -sourcepath /fox/tmp/javadoc/src-jdk7 \
    -subpackages java \
    -subpackages javax \
    -subpackages org.omg \
    -subpackages org.w3c \
    -subpackages org.xml

./sajavadoc.sh $MONGOLAB_TEST /java/com.surfapi/1.0 -sourcepath src/test/java  com.surfapi.test
./sajavadoc.sh $MONGOLAB_TEST /java/com.surfapi/1.0 -sourcepath src/main/java  com.surfapi.proc
./sajavadoc.sh $MONGOLAB_TEST /java/com.surfapi/0.9 -sourcepath src/test/java  com.surfapi.test

./sajavadoc.sh $MONGOLAB_TEST /java/mongo-java-driver/2.9.3 -sourcepath /fox/tmp/surfapi-heroku/mongo-java-driver-2.9.3 -subpackages com -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/javax.json/1.0.2 -sourcepath /fox/tmp/surfapi-heroku/javax.json-1.0.2 -subpackages javax
./sajavadoc.sh $MONGOLAB_TEST /java/javax.enterprise.concurrent/1.0 -sourcepath /fox/tmp/surfapi-heroku/javax.enterprise.concurrent-1.0 -subpackages javax
./sajavadoc.sh $MONGOLAB_TEST /java/javax.batch/1.0 -sourcepath /fox/tmp/surfapi-heroku/javax.batch-api-1.0 -subpackages javax
./sajavadoc.sh $MONGOLAB_TEST /java/jaxrs/2.3.1 -sourcepath /fox/tmp/surfapi-heroku/jaxrs-api-2.3.1 -subpackages javax

./sajavadoc.sh $MONGOLAB_TEST /java/org.osgi.core/5.0.0 -sourcepath /fox/tmp/surfapi-heroku/org.osgi.core-5.0.0 -subpackages org

./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-lang3/3.3.2 -sourcepath /fox/tmp/surfapi-heroku/commons-lang3-3.3.2-src/src/main/java -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-io/2.4 -sourcepath /fox/tmp/surfapi-heroku/commons-io-2.4-src/src/main/java -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-net/3.3 -sourcepath /fox/tmp/surfapi-heroku/commons-net-3.3-src/src/main/java -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-collections4/4.0 -sourcepath /fox/tmp/surfapi-heroku/commons-collections4-4.0-src/src/main/java -subpackages org

# TODO: not all source files have javadoc
./sajavadoc.sh $MONGOLAB_TEST /java/javaee/6.0 -sourcepath /fox/tmp/surfapi-heroku/javaee-api-6.0 -subpackages javax


