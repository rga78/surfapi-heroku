#!/bin/sh
#
# Initialize the db with some data for testing
#

# reset the db
./runJavadoc.sh ResetMongoMain 

# add the jdk first...
./runJavadoc.sh JavadocMain "/java/java-sdk/1.6" \
  "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/lang" \
  "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/net" \
  "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/util" \
  "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/io"

# add our demo javadoc
./runJavadoc.sh JavadocMain --all "/java/com.surfapi/1.0" \
    "src/main/java/com/surfapi/proc" \
    "src/test/java/com/surfapi/test"

# add a pretend older version of our demo javadoc
./runJavadoc.sh JavadocMain --all "/java/com.surfapi/0.9" \
    "src/main/java/com/surfapi/proc" \
    "src/test/java/com/surfapi/test"

# add mongo
./runJavadoc.sh ExtractMain "/java/mongo-java-driver/2.9.3" \
    "/fox/tmp/javadoc/mongo-java-driver-2.9.3-sources.jar"

# run post-processor on all
./runJavadoc.sh PostProcessorMain


