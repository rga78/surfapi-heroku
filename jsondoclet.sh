#!/bin/sh
#
# Convert javadoc to JSON
#


if [ -z $3 ]; then
    echo "usage: $0 <javadoc-args>"
    echo "              <javadoc-args>:"
    echo "                      -sourcepath <sourcepath>"
    echo "                      -subpackages <package> -subpackages <package>..." 
    echo "                      <package> ..." 
    exit 1
fi

# cp_sep=";"   # windows
cp_sep=":"     # mac

# build the doclet path
dp=target/classes
for x in `find target/dependency`; do dp="${dp}${cp_sep}${x}"; done

javadoc \
        -doclet com.surfapi.javadoc.JsonDoclet \
        -docletpath "$dp" \
        -J-Xms1024m \
        -J-Xmx4096m \
        $*
        





