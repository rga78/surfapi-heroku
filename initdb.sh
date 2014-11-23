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
export JAVADOC_CP=/fox/tmp/surfapi-heroku/org.osgi.core-5.0.0.jar; ./sajavadoc.sh $MONGOLAB_TEST /java/org.osgi.enterprise/5.0.0 -sourcepath /fox/tmp/surfapi-heroku/org.osgi.enterprise-5.0.0 -subpackages org; export JAVADOC_CP=

./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-lang3/3.3.2 -sourcepath /fox/tmp/surfapi-heroku/commons-lang3-3.3.2-src/src/main/java -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-io/2.4 -sourcepath /fox/tmp/surfapi-heroku/commons-io-2.4-src/src/main/java -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-net/3.3 -sourcepath /fox/tmp/surfapi-heroku/commons-net-3.3-src/src/main/java -subpackages org
./sajavadoc.sh $MONGOLAB_TEST /java/apache-commons-collections4/4.0 -sourcepath /fox/tmp/surfapi-heroku/commons-collections4-4.0-src/src/main/java -subpackages org

./sajavadoc.sh $MONGOLAB_TEST /java/htmlunit/2.15 -J-Dfile.encoding=UTF-8 -sourcepath /fox/tmp/surfapi-heroku/htmlunit-2.15 -subpackages com -subpackages netscape

# DONE: not all source files have javadoc. v7 has them (downloaded from maven repo)
# ./sajavadoc.sh $MONGOLAB_TEST /java/javaee/6.0 -sourcepath /fox/tmp/surfapi-heroku/javaee-api-6.0 -subpackages javax
./sajavadoc.sh $MONGOLAB_TEST /java/javaee/7.0 -sourcepath /fox/tmp/surfapi-heroku/javaee-api-7.0 -subpackages javax

./sajavadoc.sh $MONGOLAB_TEST /java/junit/4.11 \
    -sourcepath "/fox/tmp/surfapi-heroku/junit/src/main/java;/fox/tmp/surfapi-heroku/hamcrest-all-1.3" \
    -subpackages org.junit.experimental \
    -subpackages org.junit.matchers \
    -subpackages org.junit.rules \
    -subpackages org.junit.runner \
    -subpackages org.junit.runners \
    org.junit

./sajavadoc.sh $MONGOLAB_TEST /java/hamcrest/1.3 -sourcepath "/fox/tmp/surfapi-heroku/hamcrest-all-1.3" \
    -subpackages org.hamcrest.beans \
    -subpackages org.hamcrest.collection \
    -subpackages org.hamcrest.core \
    -subpackages org.hamcrest.integration \
    -subpackages org.hamcrest.internal \
    -subpackages org.hamcrest.number \
    -subpackages org.hamcrest.ojbect \
    -subpackages org.hamcrest.text \
    -subpackages org.hamcrest.xml \
    org.hamcrest


./sajavadoc.sh $MONGOLAB_TEST /java/gson/2.3.1 -sourcepath "/fox/tmp/surfapi-heroku/gson-2.3.1" -subpackages com

