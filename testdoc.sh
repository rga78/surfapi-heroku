#!/bin/sh

MONGO_DBNAME=test
MONGO_LIBRARYID=/java/com.surfapi/1.0

# build the doclet path
dp=
for x in `find target/dependency`; do dp="$x;$dp"; done

javadoc \
        -doclet com.surfapi.javadoc.MongoDoclet \
        -docletpath "target/classes;$dp" \
        -J-Xms1024m \
        -J-Xmx4096m \
        -J-Dcom.surfapi.mongo.db.name=$MONGO_DBNAME \
        -J-Dcom.surfapi.mongo.library.id=$MONGO_LIBRARYID \
        -sourcepath src/test/java   \
        com.surfapi.test
        






