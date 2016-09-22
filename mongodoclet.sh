#!/bin/sh
#
# Convert javadoc to mongo documents and upload to mongo.
#
# TODO: delete existing library?
#       but what to do about the indexes?  
#       need a utility to remove a library from all indexes (see git issue)
#


if [ -z $3 ]; then
    echo "usage: $0 <mongo-uri> <libraryid> <javadoc-args>"
    echo "              <javadoc-args>:"
    echo "                      -sourcepath <sourcepath>"
    echo "                      -subpackages <package> -subpackages <package>..." 
    echo "                      <package> ..." 
    exit 1
fi

MONGOLAB_URI=$1
shift

echo "connecting to $MONGOLAB_URI..."

MONGO_LIBRARYID=$1
shift

# cp_sep=";"   # windows
cp_sep=":"     # mac

# build the doclet path
dp=target/classes
for x in `find target/dependency`; do dp="${dp}${cp_sep}${x}"; done

# include classpath if set
cp=
if [ -n "$JAVADOC_CP" ]; then
    cp="-classpath $JAVADOC_CP"
fi

javadoc \
        -doclet com.surfapi.javadoc.MongoDoclet \
        -docletpath "$dp" \
        $cp \
        -J-Xms1024m \
        -J-Xmx4096m \
        -J-DMONGOLAB_URI=$MONGOLAB_URI \
        -J-Dcom.surfapi.mongo.library.id=$MONGO_LIBRARYID \
        $*






