#!/bin/sh
#
#
# TODO: delete existing library?
#       but what to do about the indexes?  need a utility to remove a library
#       from all indexes


if [ -z $2 ]; then
    echo "usage: $0 <libraryid> <javadoc-args>"
    echo "              <javadoc-args>:"
    echo "                      -sourcepath <sourcepath>"
    echo "                      -subpackages <package> -subpackages <package>..." 
    echo "                      <package> ..." 
    exit 1
fi

MONGO_DBNAME=test

MONGO_LIBRARYID=$1
shift

# build the doclet path
dp=target/classes
for x in `find target/dependency`; do dp="$dp;$x"; done

javadoc \
        -doclet com.surfapi.javadoc.MongoDoclet \
        -docletpath "$dp" \
        -J-Xms1024m \
        -J-Xmx4096m \
        -J-Dcom.surfapi.mongo.db.name=$MONGO_DBNAME \
        -J-Dcom.surfapi.mongo.library.id=$MONGO_LIBRARYID \
        $*
        

# run post-processor (for building indexes)
./runJavadoc.sh PostProcessorMain $MONGO_LIBRARYID






