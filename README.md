## Surf API - Java API repository

[http://www.rga78.com/blog?category=surfapi](http://www.rga78.com/blog?category=surfapi)

[surfapi.com](surfapi.com)


### Build and test

Note: must run mvn package before running tests in order to copy dependency jars into 
the target directory (some of the tests need them).

    $ mvn package -DskipTests=true
    $ mvn test


### Running in local sandbox

1. Start mongo
```
    $ ./startMongo.sh

    // connect to mongo
    $ mongo localhost/test
```

2. Start appserver  
    
        $ . ./mongolab.env
        $ export MONGOLAB_URI=$MONGOLAB_TEST
        $ ./startServer.sh MongoMain


### Git

    $ git push -u origin master
    $ git push -u origin heroku


### Upgrade from 2.4 to 2.6:

1. Installed mongo 2.6 locally (from binary)
2. Launched mongod v 2.6 (reads data from same place as 2.4:  /data/db)
3. Connect to local mongo:  
    $ mongo localhost/test   
Connect to remote mongo:   
    $ mongo ds055459-a0.mongolab.com:55459/prod1 -u <dbuser> -p <dbpassword>
4. db.upgradeCheck()  

Needed to drop the { _id: -1, _qn: 1} index from /q/java/qn, because some documents exceeded the index length limit (1024).
The index wasn't needed anyway.  I had already gotten rid of it (see ReferenceNameQuery.java).
Just needed to drop the index:

    $ db["/q/java/qn"].getIndexes()
    $ db["/q/java/qn"].dropIndex("_id_-1__qn_1")
      { "nIndexesWas" : 3, "ok" : 1 }
            
4. MongoLab -> Server prod1 -> Tools -> upgrade


---

Notes:

The following documents in the index /q/java/qn are incompatible with 2.6 because their keys are too long.
The key consists of two fields: "_id" and "qn".

_id is actually a concatenation of "id" and "qn".  The index could instead be built on "id" instead of "_id".

These documents must be deleted from the DB or else the index may become unusable.  If anything causes
mongodb to re-write the index, mongodb will NOT re-write the index if ANY of its documents violate the index
length limit.


    Document Error: key for index {  "v" : 1,  "key" : {  "_id" : -1,  "_qn" : 1 },  "ns" : "test./q/java/qn",  "name" : "_id_-1__qn _1" } 
    too long for document: {  
    "_id" : "/java/javaee/6.0/javax.servlet.jsp.tagext.TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)/javax.servlet.jsp.tagext.TagInfo+TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "_library" : {  "_id" : "/java/javaee/6.0",  "name" : "javaee",  "lang" : "java",  "version" : "6.0" },  
    "_qn" : "javax.servlet.jsp.tagext.TagInfo+TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "flatSignature" : "(String, String, String, String, TagLibraryInfo, TagExtraInfo, TagAttributeInfo[], String, String, String, TagVariableInfo[], boolean)",  
    "id" : "/java/javaee/6.0/javax.servlet.jsp.tagext.TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "name" : "TagInfo",  
    "qualifiedName" : "javax.servlet.jsp.tagext.TagInfo" } 
    
    Document Error: key for index {  "v" : 1,  "key" : {  "_id" : -1,  "_qn" : 1 },  "ns" : "test./q/java/qn",  "name" : "_id_-1__qn _1" } 
    too long for document: {  
    "_id" : "/java/javaee/7.0/javax.servlet.jsp.tagext.TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)/javax.servlet.jsp.tagext.TagInfo+TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "_library" : {  "_id" : "/java/javaee/7.0",  "name" : "javaee",  "lang" : "java",  "version" : "7.0" },  
    "_qn" : "javax.servlet.jsp.tagext.TagInfo+TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "flatSignature" : "(String, String, String, String, TagLibraryInfo, TagExtraInfo, TagAttributeInfo[], String, String, String, TagVariableInfo[], boolean)",  
    "id" : "/java/javaee/7.0/javax.servlet.jsp.tagext.TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "name" : "TagInfo",  
    "qualifiedName" : "javax.servlet.jsp.tagext.TagInfo" }
    
    "_id" : "/java/javaee/7.0/javax.servlet.jsp.tagext.TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)/javax.servlet.jsp.tagext.TagInfo+TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  
    "id" :  "/java/javaee/7.0/javax.servlet.jsp.tagext.TagInfo(java.lang.String,java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagLibraryInfo,javax.servlet.jsp.tagext.TagExtraInfo,javax.servlet.jsp.tagext.TagAttributeInfo[],java.lang.String,java.lang.String,java.lang.String,javax.servlet.jsp.tagext.TagVariableInfo[],boolean)",  


