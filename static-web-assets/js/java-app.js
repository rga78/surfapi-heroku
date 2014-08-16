angular.module( "JavaApp", ['ui.bootstrap', 
                            'ngSanitize', 
                            'ui.keypress'] )
        
/**
 * TODO: I don't really know what html5 mode is.  I should learn that.
 *       Something about the history api.  More functionality.  Or something.
 *       One thign I notice is the URL is weird... the hash string is always
 *       HTML-escaped/url-encoded for whatever reason.
 *       More embarrassingly I don't remember why I enabled it in the first place.
 *       Probably needed it for the history manipulation done in ReferenceQueryService.
 */
.config(['$locationProvider', function($locationProvider) {
    $locationProvider.html5Mode(true);
}])


/**
 * This service listens and reacts to hash updates.
 *
 * Currently there are only two types of hash updates:
 * 1. for fetching a javadoc model
 * 2. for running the refernece query service, which eventually resolves
 *    to a javadoc model.
 */
.factory("HashListener", [ "$rootScope", "$location", "Utils", "ReferenceQueryService", "RestService", "Log",
                            function($rootScope, $location, Utils, ReferenceQueryService, RestService, Log) {

    var _this = this;
    this.logging = { prefix: "HashListener" };
    Log.log(_this, "<init>: entry");

    /**
     * Callback method when the watcher detects that the hash has changed.
     * REmember that watchers aren't triggered by events but rather are invoked
     * during $digest().  $digest is called at various strategic times by angular.
     * Would be good to know when those times are. 
     */
    var onHashChange = function(newHash, oldHash) {
        Log.log(_this, "onHashChange: " + newHash);

        if ( Utils.isEmpty(newHash) ) {
            fetchJavadocModel("/java");

        } else if (startsWith(newHash, "/java")) {
            fetchJavadocModel(newHash);

        } else if (startsWith(newHash, "/q/java/qn/")) {
            requestPending(true);
            ReferenceQueryService.getReferenceType(newHash);
        }
    };

    
    /**
     * @return true if the given str starts with the given prefix.
     */
    var startsWith = function(str, prefix) {
        return str.indexOf(prefix) == 0;
    }

    /**
     * Set the $rootScope.requestPending flag which may trigger view changes.
     * TODO: should this be an event instead?
     */
    var requestPending = function(isPending) {
        Log.log(_this, "requestPending: " + isPending);
        $rootScope.requestPending = isPending;
    };

    /**
     * Fetch a javadocModel with the given _id from the DB via the RestService.
     */
    var fetchJavadocModel = function(_id) {

        scrollTop();

        requestPending(true);

        RestService.get( _id,  { params: { "methods": "0", "allInheritedMethods": "0" } } )
                   .success( fetchJavadocModelSuccess )
                   .finally( function() {
                      requestPending(false);
                   }) ;
    };

    /**
     * Scroll to the top of the document.
     */
    var scrollTop = function() {
        document.body.scrollTop = document.documentElement.scrollTop = 0;
    }

    /**
     * Called upon successfully fetching a javadoc model from the RestService.
     */
    var fetchJavadocModelSuccess = function(javadocModel) {
        Log.log(_this, "fetchJavadocModelSuccess: " + javadocModel._id);

        $rootScope.$broadcast( "$saJavadocModelChange", javadocModel);
    }

    // Listen for hash changes.
    // Note: I was having problems before when the $watch registration was made 
    //       a little higher up in this function, before onHashChange was defined.
    //       OF COURSE I WAS!  I'm registering a callback by name (onHashChange) that
    //       wasn't defined yet.  How's angular supposed to register and callback a
    //       function that isn't defined yet.  DUH! stupid stupid.
    //
    // Note: the watch listener will be invoked at least once to "initialize" it.
    // From angular doc:
    //      After a watcher is registered with the scope, the listener fn is called asynchronously 
    //      (via $evalAsync) to initialize the watcher
    //      The $evalAsync makes no guarantees as to when the expression will be executed, only that:
    //          * it will execute after the function that scheduled the evaluation (preferably before DOM rendering).
    //          * at least one $digest cycle will be performed after expression execution.
    $rootScope.$watch(function() { return $location.hash(); }, 
                      onHashChange);

    Log.log(_this, "<init>: exit");

    /**
     * There's nothing to return from this service.
     */
    return {};
}])


/**
 * TODO: change this to a  "/q/*" rest target
 *
 * DONE: I should use factory() instead of service().  The only difference between the
 * two is that the thing returned from service will be "new'ed" up, i.e new <retVal>().
 * 'new' sets up prototype inheritance and sets the contstructor property
 * (see http://pivotallabs.com/javascript-constructors-prototypes-and-the-new-keyword/),
 * but other than that there's no need for it.  The thing returned from factory will
 * just be returned (no 'new'), which is fine for my purposes.  I.e factory is simpler.
 * Doesn't use new.  If you don't need new, use factory.
 */
.factory("AutoCompleteService", ['$http', function($http) {

    var buildUrl = function(str, indexName) {
        indexName = indexName || "java";
        return "rest/autoComplete/index?str=" + str + "&index=" + indexName; 
    }

    var onError = function(data, status, headers, config) {
        alert("AutoCompleteService (url = " + config.url + ") didn't work: " + status + ": " + data);
        // TODO: create modal with option to send error message to me for diagnosing.
        //       or maybe it should always just send the error, with as much context as possible
        //       (browser, os, request that failed, etc)
        // TODO: this reminds me I should be logging activity such as which classes
        //       get the most hits.  Should I log that here or on the server-side?
        // TODO: should this delegate to RestService, passing the url ?
    };

    var get = function(str, indexName) {
        return $http.get( buildUrl(str, indexName)  )    
                    .error( onError );
    }

    return {
        get: get 
    };
}])

/**
 * Thin wrapper around $http calls to the REST api.
 *
 * TODO: not sure if this really serves a purpose other than wrapping $http calls
 *       with tracing and an onError function.
 */
.factory("RestService", [ '$http', 'Log', function($http, Log) {

    var _this = this;
    _this.logging = { prefix: "RestService" };

    var onSuccess = function(data) {
        Log.log(_this, "onSuccess: " + data._id);
    }

    var buildUrl = function(uri) {
        return "/rest" + uri;
    }

    var onError = function(data, status, headers, config) {
        alert("RestService (url = " + config.url + ") didn't work: " + status + ": " + data);
        // TODO: create modal with option to send error message to me for diagnosing.
    };
    
    /**
     * Simple wrapper around $http.get.
     *
     * @param uri to pass to $http.get
     * @param config to pass to $http.get
     *
     * @return the promise returned from $http.get
     */
    var get = function(uri, config) {

        Log.log(_this, "get: " + uri + ", " + angular.toJson(config));

        return $http.get( buildUrl(uri), config )
                    .success( onSuccess )
                    .error( onError );
    }

    return {
        get: get
    };

}])

/**
 * Format complex types, method parameter lists, etc.
 */
.factory("Formatter", [ "_", "Utils", "JavadocModelUtils", "Log", 
                        function(_, 
                                 Utils, 
                                 JavadocModelUtils, 
                                 Log) {

    var isEmpty = Utils.isEmpty;
    var _this = this;

    var formatTypeArguments = function(typeArguments) {
        return (isEmpty(typeArguments)) ? "" :  "&lt;" + ( _.map(typeArguments, formatType).join(", ") ) + "&gt;";
    };

    var formatTypeName = function(type) {
        return (JavadocModelUtils.isMethod(type)) 
               ? JavadocModelUtils.getQualifiedName(type)
               : JavadocModelUtils.getNameAndDimension(type);
    }

    var asAnchor = function(type) {
        return "<a href='#" + type._id + "'>" + formatTypeName(type) + "</a>";
    }

    var asAnchorRef = function(type) {
        return "<a href='#/q/java/qn/" + JavadocModelUtils.getReferenceName(type) + "'>" + formatTypeName(type) + "</a>" ;
    }

    var asSpan = function(type) {
        var retMe = "<span title=" + enquote( JavadocModelUtils.getQualifiedName(type) ) + ">";

        if (type._id) {
            retMe += asAnchor(type);
        } else if ( ! JavadocModelUtils.isPrimitiveType(type)) {
            retMe += asAnchorRef(type); 
        } else {
            retMe += formatTypeName(type);
        }

        retMe += "</span>";
        return retMe;
    }

    var enquote = function(str) {
        return "'" + str + "'";
    }

    var formatType = function(type) {

        if ( isEmpty(type) ) {
            return "";
        }

        var retMe = asSpan(type);

        if ( ! isEmpty(type.parameterizedType) ) {
            retMe += formatTypeArguments( type.parameterizedType.typeArguments );
        } else if ( ! isEmpty(type.wildcardType) ) {
            retMe += formatExtendsBounds( type.wildcardType.extendsBounds );
            retMe += formatSuperBounds( type.wildcardType.superBounds);
        }

        return retMe;
    };

    var formatSuperBounds = function(superBounds) {
        return (isEmpty(superBounds)) ? "" : " super " + ( _.map( superBounds, formatType).join(" & ") );
    };

    var formatExtendsBounds = function(extendsBounds) {
        return (isEmpty(extendsBounds)) ? "" : " extends " + ( _.map( extendsBounds, formatType).join(" & ") );
    };

    var formatTypeParameter = function(typeParam) {
        return typeParam.typeName + formatExtendsBounds(typeParam.bounds); 
    };

    var formatAnnotationElementValue = function(elementValue) {
        return elementValue.element.name + "=" + elementValue.value.toString;
    };

    var formatAnnotationElementValues = function(elementValues) {
        return isEmpty(elementValues) ? "" : "(" + (_.map(elementValues, formatAnnotationElementValue).join(", ")) + ")";
    };

    var formatAnnotation = function(annotation) {
        return "@" + formatType(annotation.annotationType) + formatAnnotationElementValues(annotation.elementValues);
    };

    var formatAnnotations = function(annotations) {
        return isEmpty(annotations) ? "" : (_.map(annotations, formatAnnotation).join(" ")) + " ";
    };

    var formatParam = function(parameter) {
        return formatAnnotations(parameter.annotations) + formatType(parameter.type) + " " + parameter.name;
    };

    var formatTypeParameters = function(typeParameters) {
        return (isEmpty(typeParameters)) ? "" : "&lt;" + ( _.map(typeParameters, formatTypeParameter).join(", ") ) + "&gt;";
    };

    var formatParameters = function(parameters) {
        return _.map(parameters, formatParam).join(", ");
    };

    /**
     * This regex converts something like "Class[]" into "Class..." for var-arg parms.
     * Note the type parameters are optionally matched: "Class[]<T>" into "Class<T>..."
     */
    var varArgRegExp = new RegExp("\\[\\](.*&gt;)?");

    var formatParmAsVarArg = function(formattedParm) {
        return formattedParm.replace( varArgRegExp, "$1...");
    }

    var formatMethodSignature = function(parameters, flatSignature) {
        if (flatSignature.indexOf("...") > 0) {
            var formattedParms = _.map(parameters, formatParam);
            formattedParms[ formattedParms.length - 1 ] = formatParmAsVarArg( _.last(formattedParms) );
            return formattedParms.join(", ");
        } else {
            return formatParameters(parameters);
        }
    };

    var formatThrows = function(thrownExceptionTypes) {
        return (isEmpty( thrownExceptionTypes )) ? "" : "throws " +  _.map(thrownExceptionTypes, formatType).join(", ") ;
    };

    /**
     *
     * "seeTags": [
     *   {
     *    "text": "<a href=\"http:\/\/docs.oracle.com\/javase\/7\/docs\/technotes\/tools\/windows\/javadoc.html#link\">Javadoc Reference Guide<\/a>",
     *    "name": "@see",
     *    "kind": "@see"
     *   },
     *   {
     *    "text": "JsonDoclet",         // use current package.
     *    "name": "@see",
     *    "kind": "@see"
     *   },
     *   {
     *    "text": "com.surfapi.javadoc.JsonDoclet#processType",
     *    "name": "@see",
     *    "kind": "@see"
     *   },
     *   {
     *    "text": "#processType",       // use current class.
     *    "name": "@see",
     *    "kind": "@see"
     *   },

     *   {
     *    "text": "java.net.URL#equals The URL.equals method",
     *    "name": "@see",
     *    "kind": "@see"
     *   }
     *  ],
     * 
     */
    var formatSeeTag = function(seeTag, currentModel) {

        if (seeTag.text.indexOf("<") == 0) {
            // Starts with "<".  Must be an anchor.
            // TODO: check if url is relative/absolute.
            return seeTag.text;
        } else {

            return formatLinkTag( seeTag, 
                                  JavadocModelUtils.getPackageFor( currentModel ).name,
                                  JavadocModelUtils.getClassFor( currentModel ).name);
        }
    };

    /**
     * m[0] = entire match
     * m[1] = class name (potentially fully qualified)
     * m[2] = #m[3]m[4]
     * m[3] = method name
     * m[4] = method parms
     * m[5] = text
     */
    var linkTagRegExp = /^\s*([^\s#]*)(#([^\s()]*)(\(.*?\))?)?(\s+\S.*)?/;

    var formatLinkTag = function(tag, packageName, className) {
        return "<span class='code'>" + formatLinkplainTag(tag, packageName, className) + "</span>";
    };

    var formatLinkplainTag = function( tag, packageName, className) {
        var m = linkTagRegExp.exec(tag.text);

        if (m == null) {
            // regex failed to match.
            return tag.text;
        }

        var ref = resolveLinkTagReference(m, packageName, className);

        var tagText = (m[5] || m[0].replace("#","."));

        Log.log(_this, "Formatter.formatLinkplainTag: " + JSON.stringify(tag) + "; ref: " + ref);

        return "<a href='#/q/java/qn/" + ref + "'>" + tagText + "</a>";
    };

    /**
     * @param m - the result of matching the tag text against linkTagRegExp.
     * @param packageName - the package of the program element that specified the tag
     * @param className - the class of the program element that specified the tag
     *
     * @return the fully-qualified referenced document (e.g com.surfapi.test.DemoJavadoc#parse)
     */
    var resolveLinkTagReference = function(m, packageName, className) {

        var qualifiedName = m[1];

        if ( isEmpty(qualifiedName) ) {
            // class name not specified at all (must be a #method)
            qualifiedName = packageName + "." + className;
        } else if ( qualifiedName.indexOf(".") < 0 ) {
            // prepend current package 
            qualifiedName = packageName + "." + qualifiedName;
        }

        var methodName = m[3];
        var methodParms = m[4];

        if ( !isEmpty(methodParms) ) {
            methodParms = formatLinkTagMethodParms(methodParms);
        }

        return (isEmpty(methodName)) ? qualifiedName : qualifiedName + "+" + methodName + (methodParms || "");
    };

    /**
     * Format @see link tag method parms by removing all whitespace and arg names
     * (leaving only the arg types).
     *
     * @return formatted link tag method parms
     */
    var formatLinkTagMethodParms = function(methodParms) {

        if (methodParms.length <= 2) {
            return methodParms;
        }

        // trim off ()
        // split on ","
        // parse out first word (type) only
        // put it all back together.
        var retMe = "(" 
                + _.map( methodParms.substring(1, methodParms.length - 1).split(","),
                         function(arg) {
                             return arg.trim().split(" ")[0];
                         }).join(",")
                + ")";
        Log.log(_this, "formatLinkTagMethodParms: " + methodParms + " --> " + retMe);

        return retMe;
    }

    /**
     * @return the html-encoded string (e.g "<code>" becomes "&lt;code&gt;")
     */
    var encodeHtml = function(str) {
        var e = angular.element("<div />");
        e.text(str);
        return e.html();
    };

    var formatCodeTag = function(tag) {
        return "<code>" + formatLiteralTag(tag) + "</code>";
    };

    var formatLiteralTag = function(tag) {
        return encodeHtml( tag.text );
    };

    var formatInlineTag = function(tag, packageName, className) {
        switch (tag.name) {
            case "@code":
                return formatCodeTag(tag);
            case "@docRoot":
                return "{@docRoot " + tag.text + "}"; // TODO
            case "@inheritDoc":
                return "{@inheritDoc}"; 
            case "@link":
                return formatLinkTag(tag, packageName, className);
            case "@linkplain":
                return formatLinkplainTag(tag, packageName, className);
            case "@literal":
                return formatLiteralTag(tag);
            case "@value":
                return "{@value " + tag.text + "}"; // TODO
            default:
                return tag.text;
        }

    };

    var formatInlineTags = function(tags, packageName, className) {
        return _.map(tags, function(tag) { return formatInlineTag(tag, packageName, className); } ).join("");
    }

    // Exported functions.
    var retMe = {};
    retMe.formatType = formatType;
    retMe.formatTypeParameters = formatTypeParameters;
    retMe.formatTypeParameter = formatTypeParameter;
    retMe.formatMethodSignature = formatMethodSignature;
    retMe.formatSeeTag = formatSeeTag;
    retMe.formatInlineTag = formatInlineTag;
    retMe.formatInlineTags = formatInlineTags;
    retMe.formatAnnotation = formatAnnotation;
    return retMe;
}])

/**
 * Centralized logging.
 */
.factory("Log", function() {

    /**
     *
     * Note:The "loggingObject.logging" field may itself be an object 
     * that contains a "prefix" field. If it does, the prefix field is 
     * prefixed on all log messages.
     *
     * Log.log( { logging: { prefix: "MyPrefix" } }, "log this msg" )
     *
     * @param loggingObject Must contain a non-null field "logging", otherwise
     *                      the message is not logged.
     * @param msg the message to log
     *
     */
    var log = function(loggingObject, msg) {
        if (loggingObject.logging) {
            var prefix = (loggingObject.logging.prefix) ? loggingObject.logging.prefix + ": " : "";
            console.log(prefix + msg);
        }
    }
    
    // Exported functions.
    return { log: log };
})


/**
 * Dumping ground for un-categorizable stuff.
 */
.factory("Utils", [ "_", function(_) {

    var prevLocation = "";

    var isEmpty = function(model) {
        return (_.isUndefined(model) || model == null || model.length == 0);
    };

    var setLocation = function(newUrl, oldUrl) {
        prevLocation = decodeURIComponent(oldUrl);
    };

    var getPrevLocation = function() {
        return prevLocation;
    };

    var parseLibRegExp = /\#(.*)\//;

    var parseLibraryFromLocation = function(loc) {
        var m = parseLibRegExp.exec(loc);
        return (m != null) ? m[1] : null;
    }

    // Exported functions.
    return { 
        isEmpty: isEmpty,
        setLocation: setLocation,
        getPrevLocation: getPrevLocation,
        parseLibraryFromLocation: parseLibraryFromLocation
    };
}])


/**
 * Convenience methods for handling a javadoc models.
 */
.factory("JavadocModelUtils", ["Utils", "_", function(Utils,_) {

    var isEmpty = Utils.isEmpty;

    var ClassMetaTypes = ["class", "interface", "annotationType", "enum"];
    var ClassElementMetaTypes = ["method", "constructor", "field", "enumConstant"];     // why not annotationTypeElement?
    var MethodMetaTypes = ["method", "constructor", "annotationTypeElement"];

    var getId = function( model ) {
        return (!isEmpty(model)) ? model._id : null;
    }
    
    var getName = function(model) {
        return (model.name || model.typeName) ;
    }
    
    var getNameAndDimension = function(model) {
        return getName(model) + (model.dimension || "");
    }

    var getQualifiedName = function(model) {
        return (model.qualifiedName || model.qualifiedTypeName || model.name);  // model.name is for packages.
    }

    var getQualifiedParameterSignature = function(model) {
        return "(" + _.chain(model.parameters).pluck("type").pluck("qualifiedTypeName").value().join(",") + ")";
    }

    var getReferenceName = function(model) {
        switch( getMetaType(model) ) {
            case "field":
            case "enumConstant":
            case "annotationTypeElement":
                return getQualifiedName( model ).replace( new RegExp( "[.]" + model.name + "$"), "+" + model.name);
            case "method":
            case "constructor":
                return getQualifiedName( model ).replace( new RegExp( "[.]" + model.name + "$"), "+" + model.name)
                       + getQualifiedParameterSignature(model);
            default:
                return getQualifiedName(model);
        }
    }


    var getMetaType = function(model) {
        return (!isEmpty(model)) ? model.metaType : null;
    }

    var isInterface = function(model) {
        return "interface" == getMetaType(model);
    }

    var isClass = function(model) {
        return _.contains( ClassMetaTypes, getMetaType(model) );
    }

    var isMethod = function(model) {
        return _.contains( MethodMetaTypes, getMetaType(model) );
    }

    var isClassElement = function(model) {
        return _.contains( ClassElementMetaTypes, getMetaType(model) );
    }

    var isPackage = function(model) {
        return getMetaType(model) == "package";
    }

    var isLibrary = function(model) {
        return getMetaType(model) == "library";
    }

    var isLibraryVersions = function(model) {
        return getMetaType(model) == "library.versions";
    }

    var isLang = function(model) {
        return getMetaType(model) == "lang";
    }

    var isPrimitiveType = function(model) {
        return getName(model) == getQualifiedName(model);
    }

    var isParameterizedType = function(model) {
        return ! ( isEmpty(model.parameterizedType) && isEmpty(model.wildcardType) ) ;
    }

    var isSimpleTypeParameter = function(model) {
        return isPrimitiveType(model) && isEmpty(model.bounds);
    }

    // Note: assumes model is NOT null
    var getPackageId = function( model ) {
        return ( isPackage(model) ) ? model._id : getId( model.containingPackage );
    };

    // Note: assumes model is NOT null
    var getLibraryId = function( model ) {
        return getLibrary(model)._id;
    }

    // Note: assumes model is NOT null
    var getLibrary = function( model ) {
        return ( isLibrary(model) ) ? model : model._library;
    }

    var getClassFor = function(model) {
        // return non-null to compensate for some callers
        return (isClass(model)) ? model : ( model.containingClass || {});
    }

    var getPackageFor = function(model) {
        return (isPackage(model)) ? model : model.containingPackage;
    }

    var isUnformattedBlockTag = function(tag) {
        switch( tag.kind ) {
            case "@see":
            case "@param":
            case "@throws":
            case "@exception":
            case "@return":
                return false;

            default:
                return true;
        }
    }

    var getUnformattedTags = function(tags) {
        return _.filter( tags, isUnformattedBlockTag );
    }

    // Exported functions.
    return {
        getId: getId,
        getMetaType: getMetaType,
        getPackageId: getPackageId,
        getLibraryId: getLibraryId,
        getLibrary: getLibrary,
        getClassFor: getClassFor,
        getPackageFor: getPackageFor,
        getQualifiedName: getQualifiedName,
        getName: getName,
        getNameAndDimension: getNameAndDimension,
        isMethod: isMethod,
        isInterface: isInterface,
        isClass: isClass,
        isClassElement: isClassElement,
        isPackage: isPackage,
        isLibrary: isLibrary,
        isLibraryVersions: isLibraryVersions,
        isLang: isLang,
        getUnformattedTags: getUnformattedTags,
        getReferenceName: getReferenceName,
        isPrimitiveType: isPrimitiveType,
        isParameterizedType: isParameterizedType,
        isSimpleTypeParameter: isSimpleTypeParameter 
    };
}])


/**
 * "service" for transforming models (returned from Db) into ViewModels
 * (enhanced with fields specific for view rendering).
 *
 * TODO: get rid of this.
 */
.factory("ViewModelService", ["_", function(_) {

    var transform = function(model) {
        return model;
    };

    var getScopeName = function(model) {
        if (model.metaType == "constructor") {
            return "methodDoc";
        } else if (model.metaType == "interface") {
            return "classDoc";
        } else if (model.metaType == "enum") {
            return "classDoc";
        } else if (model.metaType == "annotationType") {
            return "classDoc";
        } else if (model.metaType == "enumConstant") {
            return "fieldDoc";
        } else {
            return model.metaType + "Doc";
        }
    };

    return {
        transform: transform,
        getScopeName: getScopeName
    };

}])

/**
 * Handles "refernece" queries - i.e. queries that attempt to resolve non-_id references
 * to packages/classes/methods/fields by querying the DB for the qualified name of the 
 * reference type. 
 */
.factory("ReferenceQueryService", ["RestService", "JavadocModelUtils", "$location", "$window", "$modal", "Utils", "_", "Log", "$timeout",
                                  function(RestService, 
                                           JavadocModelUtils, 
                                           $location, 
                                           $window, 
                                           $modal, 
                                           Utils, 
                                           _,
                                           Log,
                                           $timeout) {

    var openModalMessage = function( referenceName ) {
        var modalInstance = $modal.open({
            templateUrl: 'partials/modal-message.html',
            controller: "ModalMessageController",
            resolve: {
                message: function() { 
                             return "Sorry! Couldn't find <b>" + referenceName.replace("+",".") + "</b> in the database."
                                    + " Perhaps the package is wrong?? Try searching for the class name.";
                         },
                title: function() { 
                             return "Not Found";
                         }
            }
        });

        // Note: can obtain the 'result' via the modalInstance.
    }

    var openModalResults = function(models) {
        var modalInstance = $modal.open({
            templateUrl: 'partials/modal-reference-query-results.html',
            controller: "ModalReferenceQueryResultsController",
            size: 'lg',
            resolve: {
                models: function() { return models; }
            }
        });

        // modalInstance.result is a promise that is resolved when the modal
        // is 'closed' and rejected when the modal is 'dismissed'.
        //
        // The first function passed to promise.then() is called when the promise
        // resolved, the second function when the promise is rejected.  
        //
        // Both update the location hash, which will trigger the watch in
        // JavadocController to update the page.
        //
        // TODO: sometimes the modal-backdrop lingers around even after the
        //       modal itself has disappeared.  It's especially noticeable when 
        //       looking up DBCollection (mongo) from another library.  The backdrop
        //       lingers until after the REST call to retrieve DBCollection's methods.
        //       Not sure what the problem is.
        //       Actually, the backdrop lingers even if the modal is cancelled,
        //       which would trigger the second function below.  So maybe it has 
        //       something to do with processing the location update.
        //       maybe i should put that on a timeout?
        modalInstance.result.then( function(selectedModel) {
            // $timeout( function() { $location.hash( selectedModel._id ).replace(); }, 1 );
            $location.hash( selectedModel._id ).replace();
        }, function () {
            $window.history.back();
        });
    }

    var matchLibrary = function(model, libraryId) {
        return JavadocModelUtils.getLibraryId(model) == libraryId;
    }

    var filterModels = function(models) {
        // Prefer models that match the current libraryId (which is actually represented by the
        // *previous* route, since we changed the route to make this reference name query).
        var libraryId = Utils.parseLibraryFromLocation( Utils.getPrevLocation() );

        var filteredModels = _.filter( models, function(model) { return matchLibrary(model, libraryId); } );
        Log.log( this, "filterModels: libraryId: " + libraryId + "; filteredModels: " + JSON.stringify(filteredModels, undefined, 2));
        return (!Utils.isEmpty(filteredModels)) ? filteredModels : models;
    }

    var getReferenceTypeOnSuccess = function( models, referenceName) {
        Log.log(this, "success: " + JSON.stringify(models,undefined,2));
        models = filterModels( models );
        if ( models.length == 1 ) {
            $location.hash( models[0]._id ).replace();
        } else if (models.length == 0) {
            $window.history.back();
            openModalMessage(referenceName);
        } else {
            openModalResults(models);
        }
    }

    /**
     * This method is called when the route/location.hash changes to "/java/q/qn/<referenceName>".
     */
    var getReferenceType = function(id, prevId) {
        Log.log(this, "getReferenceType: " + id);
        var referenceName = id.substring( "/q/java/qn/".length );
        Log.log(this, "getReferenceType: referenceName: " + referenceName);

        RestService.get( id )
                 .success( function(models) { 
                     getReferenceTypeOnSuccess(models, referenceName); 
                 });
    }

    /**
     * TODO: would prefer the model from the most recent library.
     *
     * @return a promise that when fulfilled returns the _id of the first model
     *         returned by the reference name query.
     */
    var resolveFirstId = function(referenceName) {

        Log.log(this, "resolveFirstId: " + referenceName);
        var restUrl = "/q/java/qn/" + referenceName;

        return RestService.get( restUrl )
                 .then( function(response) {
                     var models = response.data;
                     return (models.length > 0) ? models[0]._id : null;
                 });
    }

    return {
        getReferenceType: getReferenceType,
        resolveFirstId: resolveFirstId
    };
}])

/**
 *
 * http://angular-ui.github.io/bootstrap/
 */
.controller("ModalMessageController", ["$scope", "$modalInstance", "message", "title", 
                                       function($scope, 
                                                $modalInstance, 
                                                message, 
                                                title) {
    $scope.message = message;
    $scope.title = title;

    $scope.ok = function () {
        $modalInstance.close();     // can pass 'result' into close() to be received by modalInstance
                                    // returned by open().
    };

    // -rx- $scope.cancel = function () {
    // -rx-     $modalInstance.dismiss('cancel');
    // -rx- };

}])

/**
 *
 */
.controller("ModalReferenceQueryResultsController", ["$scope", "$modalInstance", "models",
                                                     function($scope, $modalInstance, models) {

    $scope.models = models;

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
}])



/**
 * For use with the auto-complete box.
 */
.controller( "SearchController", [ "$scope", "AutoCompleteService", "Utils", "JavadocModelUtils", "Log", 
                                 function($scope, 
                                          AutoCompleteService, 
                                          Utils, 
                                          JavadocModelUtils,
                                          Log) {

    var _this = this;
    // _this.logging = { prefix: "SearchController" };

    /**
     * Every time the search string changes, increment this counter (see onChange)
     * and use it to match up with the search results so that only search results
     * associated with the most recent 'onChange' event are shown.
     */
    _this.onChangeCounter = 0;

    /**
     * @param str the search string
     * @param onChangeCount an 'onChange' event id. It's used for matching up the search results
     *                      to the most recent 'onChange' event.  If _this.onChangeCounter has
     *                      changed by the time we get the results, then the results are ignored
     *                      (since a more recent 'onChange' event triggered a more recent search
     *                      with more relevant results)
     */
    var callAutoCompleteService = function(str, onChangeCount) {

        // -rx- Log.log(_this, "callAutoCompleteService:  str=#" + str + "#, autoCompleteIndexName: " + $scope.autoCompleteIndexName
        // -rx-                + ", onChangeCount: " + onChangeCount + ", onChangeCounter: " + _this.onChangeCounter);

        // Note: $scope.autoCompleteIndexName is inherited by the containing scope.
        AutoCompleteService.get( str, $scope.autoCompleteIndexName )
             .success( function(data) {
                 
                 // Check that the onChangeCount associated with this request matches the current
                 // value of _this.onChangeCounter.  If it doesn't, then another 'onChange' event
                 // and search is currently being processed so ignore this one.
                 // 
                 // Note: The (!Utils.isEmpty($scope.str)) is a workaround for a firefox bug where
                 // the ESC key does weird things when i try to clear the input.
                 if (_this.onChangeCounter == onChangeCount && !Utils.isEmpty($scope.str)) {
                    $scope.autoCompleteData = data; 
                 } else {
                     Log.log(_this, "AutoCompleteService.get.success: onChangeCount (" + onChangeCount + ") != onChangeCounter (" + _this.onChangeCounter + ")");
                 }
             });
    };
        
    var clearListing = function() {
        $scope.autoCompleteData = [];
        $scope.str = "";
    }

    /**
     * If it's the ESC key, clear the input.
     */
    var onKeypress = function($event) {
        // -rx- Log.log(_this, "onKeypress: keycode: " + $event.keyCode + ", $scope.str: " + $scope.str);
        if ($event.keyCode == 27) {
            // ESC key
            clearListing();
        }
    }

    var onChange = function() {
        ++_this.onChangeCounter;

        // -rx- Log.log(_this, "onChange: $scope.str: " + $scope.str + ", onChangeCounter: " + _this.onChangeCounter);

        if (Utils.isEmpty($scope.str)) {
            clearListing();
        } else {
            callAutoCompleteService($scope.str, _this.onChangeCounter);
        }
    };

    /**
     * Note: this method may be "overridden" by containing controllers if they define
     *       scope.getItemHref themselves.
     *
     * @return the target for the anchor href
     */
    var getItemHref = function(item) {
        return "#/q/java/qn/" + JavadocModelUtils.getReferenceName( item );
    };

    // Export to scope.
    $scope.clearListing = clearListing;
    $scope.onKeypress = onKeypress;
    $scope.onChange = onChange;
    $scope.Utils = Utils;

    // Don't attach getItemHref to the scope if a containing controller has already
    // attached its own function.
    if (angular.isUndefined($scope.getItemHref)) {
        $scope.getItemHref = getItemHref;
    }
   
}])


/**
 *
 * So what does thsi controller actually do?
 *
 * It gets instantiated when its ng-controller is encountered by angular
 * during the $compile phase.
 *
 * It gets injected with all those components (lots...)
 *
 * It attaches the Formatter and JavadocModelUtils to the $scope. 
 *  - This is basically the primary function of a controller - to basically add
 *    behavior to the scope.  To define functions.  To set the scope data.
 *     
 * It sets up a watcher on location.hash.  Essentially this is a hash router.
 * Other pieces of the app update the hash.  This watcher processes those updates.
 * This could be done by a service or factory.  Doesn't need to be done by the controller.  
 *
 * Q: when do services/factories get instantiated?  Lazily, whenever they're first injected.  
 *    They are singletons.  The same object is injected everywhere.  This provides one way
 *    to share data between controllers.
 *
 * When the watcher is invoked it calls the REST service to obtain data from the server.
 * Typically this data is a javadoc model, although sometimes it's a query result, e.g.
 * the ReferenceServiceQuery or AllKnownSubclasses query.
 *
 * If it's a javadocModel, then it gets attached to the $scope associated with JavadocController.
 *
 * So... if the watcher were provided by a view-independent service... and it handled hash 
 * changes... which resulted in a REST call... which possibly returned a javadoc model...
 * then the JavadocController would want to register a callback function to be invoked 
 * whenever a new javadoc model showed up.  Conveniently we have an event for that, 
 * $saJavadocModelChange, which is emitted by the RestService.  So the JavadocController
 * really should just be listening for $saJavadocModelChange events.
 *
 * A view-independent service should handle hash updates.  This is essentially what ngRoute does.  
 * ngRoute just doesn't fit my needs however.
 * 
 *
 */
.controller( "JavadocController", [ "$scope", "$rootScope", "$location", "RestService", "ViewModelService", 
                                    "Utils", "Formatter", "ReferenceQueryService", "JavadocModelUtils", "Log", "$http", "HashListener",
                                  function($scope, 
                                           $rootScope, 
                                           $location, 
                                           RestService, 
                                           ViewModelService, 
                                           Utils, 
                                           Formatter,
                                           ReferenceQueryService,
                                           JavadocModelUtils,
                                           Log,
                                           $http,
                                           HashListener) {

    var _this = this;
    _this.logging = { prefix: "JavadocController" };
    Log.log(_this, "<init>: entry");

    // Export functions to scope.
    $scope.Formatter = Formatter;
    $scope.JavadocModelUtils = JavadocModelUtils;
    $scope.Utils = Utils;

    /**
     * Called whenever an $saJavadocModelChange event is triggered
     */
    var onJavadocModelChange = function(event, javadocModel) {
        Log.log(_this, "onJavadocModelChange: " + javadocModel._id);

        // Update the scope
        $scope.javadocModel = javadocModel;  
        $scope[ ViewModelService.getScopeName(javadocModel) ] = ViewModelService.transform( javadocModel );

        if ( JavadocModelUtils.isClass( javadocModel ) ) {

            // Follow-up calls to fetch the class's methods and allInheritedMethods.
            // These fields are fetched separately for "perceived performance" purposes.
            // They tend to be huge.  So we first fetch the model w/o them, to get the
            // rendering process started, then the rendering is finished off when these
            // requests are fulfilled.
            RestService.get( javadocModel._id, { params: { "methods": "1" } } )
                       .success( function(model) {
                            Log.log(_this, "onJavadocModelChange.fetchMethodsSuccess: " + model._id + " =? " + $scope.javadocModel._id);

                            // Make sure the scope's javadocModel hasn't changed on us..
                            if (model._id == $scope.javadocModel._id) {
                                $scope.javadocModel.methods = model.methods ;
                            }
                        });

            RestService.get( javadocModel._id, { params: { "allInheritedMethods": "1" } } )
                       .success( function(model) {
                            Log.log(_this, "onJavadocModelChange.fetchAllInheritedMethodsSuccess: " + model._id + " =? " + $scope.javadocModel._id);

                            // Make sure the scope's javadocModel hasn't changed on us..
                            if (model._id == $scope.javadocModel._id) {
                                $scope.javadocModel.allInheritedMethods = model.allInheritedMethods;
                            }
                        });

        }
    }

    /**
     * Listen for changes to the javadoc model on display.
     *
     * Note: REMEMBER to define the function *BEFORE* you pass it to $on(). Otherwise
     *       how will angular register and callback a function that isn't defined yet??
     *       Seems obvious but I've made this mistake before...
     */
    $scope.$on( "$saJavadocModelChange", onJavadocModelChange );

    Log.log(_this, "<init>: exit");
}])


/**
 *
 */
.controller( "AllKnownSubclassesController", [ "$scope", "$rootScope", "RestService", "JavadocModelUtils", "_", "Log",
                                             function($scope, 
                                                      $rootScope,
                                                      RestService,
                                                      JavadocModelUtils,
                                                      _,
                                                      Log) {

                          
    // initialize scope data
    $scope.allKnownSubclasses = [];

    var _this = this;
    _this.logging = { prefix: "AllKnownSubclassesController" };

    var buildUrl = function(model) {
        return "/q/java/allKnownSubclasses/" + JavadocModelUtils.getQualifiedName( model );
    };

    /**
     * @param model - the superclass model.  if it's null, $scope.javadocModel is used (inherited).
     */
    var fetchSubclasses = function(model) { 

        $scope.allKnownSubclasses = [];

        RestService.get( buildUrl(model || $scope.javadocModel) )
                 .success( function(data) {
                     // Note: the models are modified in place.
                     _.map( data, function(model) { model._id = null; return model; } );
                     $scope.allKnownSubclasses = data;  
                     $scope.none = (data.length == 0);
                 }) ;
    };

    /**
     * Listen for changes to the javadoc model on display.
     * Upon change, update the AllKnownSubclasses view
     */
    $rootScope.$on( "$saJavadocModelChange", function(event, model) {
        Log.log(_this, "saJavadocModelChange event: " + model._id);
        fetchSubclasses(model);  
    });

    fetchSubclasses();
}])

/**
 *
 */
.controller( "AllKnownImplementorsController", [ "$scope", "$rootScope", "RestService", "JavadocModelUtils", "_", 
                                               function($scope, 
                                                        $rootScope,
                                                        RestService,
                                                        JavadocModelUtils,
                                                        _) {

    // initialize scope data
    $scope.allKnownImplementors = []

    var buildUrl = function(model) {
        return "/q/java/allKnownImplementors/" + JavadocModelUtils.getQualifiedName( model );
    };

    /**
     *
     */
    var fetchImpls = function(model) {

        $scope.allKnownImplementors = []

        RestService.get( buildUrl(model || $scope.javadocModel) )
                 .success( function(data) {
                     // Note: the models are modified in place
                     _.map( data, function(model) { model._id = null; return model; } );
                     $scope.allKnownImplementors = data;
                     $scope.none = (data.length == 0);
                 }) ;
    };


    /**
     * Listen for changes to the javadoc model on display.
     * Upon change, update the AllKnownSubclasses view
     */
    $rootScope.$on( "$saJavadocModelChange", function(event, model) {
        fetchImpls(model);  
    });


    fetchImpls();
}])

/**
 *
 */
.controller( "MethodController", [ "$scope", "RestService", "ViewModelService", "_", "Utils", "JavadocModelUtils", "ReferenceQueryService",
                                 function($scope, 
                                          RestService, 
                                          ViewModelService, 
                                          _, 
                                          Utils, 
                                          JavadocModelUtils, 
                                          ReferenceQueryService) {

    /**
     * @return the scope model (either a fieldDoc or methodDoc).
     *  TODO: replace this with generic "model" field, if possible.
     */
    var getDoc = function() {
        // TODO: return $scope.model;
        return ( _.isUndefined( $scope.fieldDoc ) ) ? $scope.methodDoc : $scope.fieldDoc;
    }

    /**
     * @return the _id of the doc object
     */
    var getDocId = function() {
        return getDoc()._id;
    }
    
    /**
     * @return true if location.hash matches the id for this methodDoc.
     */
    var isLocationHashOnId = function( _id ) {
        return ( unescape(location.hash) == "#" + _id ) ;
    }

    /**
     * Lookup the given id to populate the full javadoc model and toggle the visibility of the full view.
     */
    var showFullDocForId = function( _id) {

        // Check if the field/method is the primary view (and if so, don't bother toggling the full doc).
        if (isLocationHashOnId( _id )) {
            return;
        }

        $scope.showFullDocToggle = !$scope.showFullDocToggle;

        if ($scope.showFullDocToggle) {
            RestService.get( _id )
                            .success( function(data) {
                               $scope[ ViewModelService.getScopeName(data) ] = ViewModelService.transform( data );
                               $scope.model = data;
                            });
        }
    }

    /**
     * Toggle the visibility of the full doc section.
     */
    $scope.showFullDoc = function() {

        var id = getDocId();

        if ( !Utils.isEmpty( id ) ) {
            showFullDocForId(id);
        } else {
            ReferenceQueryService.resolveFirstId( JavadocModelUtils.getReferenceName( getDoc() ) )
                                 .then( showFullDocForId );
        }
    }

    // If location.hash matches this doc id (meaning the user specifically looked up this method),
    // then automatically show the full doc section.
    if (isLocationHashOnId( getDocId() )) {
        $scope.showFullDocToggle = true;
    }

}])

/**
 *
 */
.controller( "NavPathBarController", ["$scope", "$rootScope", "JavadocModelUtils", "Utils", "Log", 
                                      function($scope, 
                                              $rootScope, 
                                              JavadocModelUtils, 
                                              Utils,
                                              Log) {

    var _this = this;

    /**
     * Listen for location/route changes and set the old/new in the Utils object.
     */
    $rootScope.$on( "$locationChangeSuccess", function(event, newUrl, oldUrl) {
        Log.log(_this, "$locationChangeSuccess: newUrl: " + newUrl + "; oldUrl: " + oldUrl);
        Utils.setLocation(newUrl, oldUrl);
    });

    /**
     * Listen for changes to the javadoc model on display.
     * Upon change, update the nav-path-bar
     */
    $rootScope.$on( "$saJavadocModelChange", function(event, model) {

        Log.log(_this, "$saJavadocModelChange: _id: " + JavadocModelUtils.getId(model));

        switch (JavadocModelUtils.getMetaType(model)) {
            case "package":
                $scope.anchors = buildAnchorsForPackage(model);
                break;
            case "library":
                $scope.anchors = buildAnchorsForLibrary(model);
                break;
            case "library.versions":
                $scope.anchors = buildAnchorsForLibraryVersions(model);     
                break;
            case "lang":
                $scope.anchors = buildAnchorsForLang(model.name);
                break;
            case null: 
                $scope.anchors = [ { html: "error: null", href: "/" } ];
                break;
            // Everything else are javadoc program elements
            default:
                $scope.anchors = buildAnchorsForProgramElement(model);
                break;
        }
    });


    var buildAnchorsForLang = function(lang) {
        return [ { html: lang, href: "#/" + lang } ];
    };

    var buildAnchorsForLibraryVersions = function(model) {
        var anchors = buildAnchorsForLang( model.lang );
        anchors.push( { html: model.name , href: "#/" + model.lang + "/" + model.name} );
        return anchors;
    }

    var buildAnchorsForLibrary = function(model) {
        var anchors = buildAnchorsForLibraryVersions( model );
        anchors.push( { html: model.version , href: "#" + model._id} );
        return anchors;
    }

    var buildAnchorsForPackage = function(model) {
        var anchors = buildAnchorsForLibrary( model._library );
        anchors.push( { html: model.name , href: "#" + model._id} );
        return anchors;
    }

    var buildAnchorsForProgramElement = function(model) {
        var anchors = buildAnchorsForLibrary( model._library );
        anchors.push( { html: model.containingPackage.name , href: "#" + model.containingPackage._id} );

        if (!Utils.isEmpty(model.containingClass)) {
            anchors.push( { html: model.containingClass.name , href: "#" + model.containingClass._id} );
        }

        anchors.push( { html: model.name , href: "#" + model._id} );
        return anchors;
    }

}])

/**
 *
 */
.controller( "NavLibraryController", [ "$scope", "$rootScope", "JavadocModelUtils", "RestService", "Log", "Utils",
                                     function($scope, 
                                              $rootScope, 
                                              JavadocModelUtils, 
                                              RestService,
                                              Log,
                                              Utils) {

    var _this = this;
    _this.logging = { prefix: "NavLibraryController" };


    var currentLibraryId = null;
    var currentPackageId = null;

    /**
     * Listen for changes to the javadoc model on display.
     * Upon change, check against the currently listed library/package 
     * and refresh the view only if the library/package has changed.
     */
    $rootScope.$on( "$saJavadocModelChange", function(event, model) {

        Log.log( _this, "$saJavadocModelChange: _id: " + JavadocModelUtils.getId(model) + ", showPackagesToggle: " + $scope.showPackagesToggle);
        
        setLibraryModelStub(JavadocModelUtils.getLibrary(model));

        if ($scope.showPackagesToggle) {
            switch (JavadocModelUtils.getMetaType(model)) {
                case "package":
                    showPackage(model);
                    refreshLibrary( JavadocModelUtils.getLibraryId(model) );
                    break;
                case "library":
                    showLibrary(model);
                    clearPackage();
                    break;
                case "lang":
                case "library.versions":
                case null: 
                    clearLibrary();
                    break;
                // Everything else is a javadoc program element
                default:
                    refreshLibrary( JavadocModelUtils.getLibraryId(model) );
                    refreshPackage( JavadocModelUtils.getPackageId(model) );
            }
        }
    });

    /**
     * Set the library model stub onto the scope and set the autoCompleteIndexName
     * for the SearchController.
     */
    var setLibraryModelStub = function(libraryModelStub) {
        $scope.libraryModelStub = libraryModelStub;
        $scope.autoCompleteIndexName = libraryModelStub._id;
    }

    var showPackage = function(model) {
        currentPackageId = (model != null) ? model._id : null;
        $scope.packageModel = model;
    };

    var showLibrary = function(model) {
        currentLibraryId = (model != null) ? model._id : null;
        $scope.libraryModel = model;
        $scope.autoCompleteIndexName = $scope.libraryModel._id;
    };

    /**
     * Refresh the library nav view (package list) if the library has changed.
     */
    var refreshLibrary = function(newLibraryId) {

        Log.log(_this, "refreshLibrary: current/new: " + currentLibraryId + ", " + newLibraryId);

        if ( newLibraryId != currentLibraryId ) {

            RestService.get( newLibraryId )
                       .success( showLibrary );
        }
    };


    /**
     * Refresh the package nav view (class list) if the package has changed.
     */
    var refreshPackage = function(newPackageId) {

        if ( newPackageId != currentPackageId ) {
            RestService.get( newPackageId )
                       .success( showPackage );
        }
    };

    var clearPackage = function() {
        $scope.packageModel = null;
        currentPackageId = null;
    };

    var clearLibrary = function() {
        clearPackage();
        $scope.libraryModel = null;
        $scope.autoCompleteIndexName = null;
        currentLibraryId = null;
    };

    var showPackages = function() {
        $scope.showPackagesToggle = !$scope.showPackagesToggle;

        if ($scope.showPackagesToggle) {
            refreshLibrary( $scope.libraryModelStub._id );
        } else {
            clearLibrary();
        }
    }

    /**
     * @return the target for the anchor href
     */
    var getItemHref = function(item) {
        return "#" + item.id;
    };


    // Export function to scope.
    $scope.Utils = Utils;
    $scope.showPackages = showPackages;

    $scope.showPackagesToggle = false;

    // Override default placeholder text
    $scope.placeholder = "Search this library...";

    // Override the item href.
    $scope.getItemHref = getItemHref;

}])


/**
 *
 */
.filter('prettyPrintJson', function() {

    return function(model) {
        return angular.toJson(model, true); // JSON.stringify(model, undefined, 2);
    }
})

/**
 *
 */
.filter('trim', function() {

    return function(input, txtToTrim) {
        return ( input ) ? input.replace( new RegExp( txtToTrim + "\\s*$" ), "") : "";
    }
})

/**
 * TODO: need more unit tests (just a general reminder... not specific to this filter...)
 * 
 */
.filter('formatTags', [ "Formatter", "_", "Utils", "JavadocModelUtils", 
                       function(Formatter, _, Utils, JavadocModelUtils) {

    var inlineTagRegex = /\{(\@[^\s\}]+)(?:\s*)?([^\}]*)?\}/g;

    var parseInlineTags = function(input) {
        var retMe = [];
        var m;
        while (m = inlineTagRegex.exec(input)) {
            retMe.push( { fullText: m[0], name: m[1], text: m[2] } );
        }
        return retMe;
    };

    var formatTags = function(input, currentModel) {
        var formattedTags = _.map( parseInlineTags(input), function(tag) { 
            tag.formattedText = Formatter.formatInlineTag(tag, 
                                                          JavadocModelUtils.getPackageFor(currentModel).name,
                                                          JavadocModelUtils.getClassFor(currentModel).name );
            return tag;
        });

        // console.log("filter.formatTags.formatTags: formattedTags: " + JSON.stringify(formattedTags) );

        return _.reduce(formattedTags, function(memo, tag) { return memo.replace(tag.fullText, tag.formattedText); }, input);
    }

    // TODO: write some unit tests for this (now that i finally know how to ut js)
    // testing: var input = "hello {@link java.io.FileReader bad link} and {@link java.io.FileReader#read working\nlink} blah {@inheritDoc} {@same } blah {@code some code }";
    // testing: console.log("filter.formatTags: parseInlineTags: " + JSON.stringify(parseInlineTags(input)));
    // testing: console.log("filter.formatTags: formatTags: " + formatTags(input));

    return function(input, currentModel) {
        // console.log("filter.formatTags: input: " + input);
        // console.log("filter.formatTags: currentModel: " + JSON.stringify(currentModel, undefined, 2) );
        return (Utils.isEmpty(input)) ? input : formatTags(input, currentModel);
        // -rx- if (Utils.isEmpty(input)) {
        // -rx-     return input;
        // -rx- }

        // -rx- console.log("filter.formatTags: input: " + input);
        // -rx- console.log("filter.formatTags: currentModel: " + currentModel._id );

        // -rx- return formatTags(input, currentModel);
    };
}])

/**
 * 
 */
.filter('formatInlineTags', ["Formatter", function(Formatter) {
    return function(tags, packageName, className) {
        return Formatter.formatInlineTags(tags, packageName, className);
    }
}])

/**
 *
 */
.filter('reverse', function() {
    return function(items) {
      return (items != null) ? items.slice().reverse() : null;
    };
})


/**
 * Note: could use filter:isOtherBlockTag 
 *       see: http://stackoverflow.com/questions/11753321/passing-arguments-to-angularjs-filters
 */
.filter('otherBlockTags', [ "_", function(_) {

    var isOtherBlockTag = function(tag) {
        switch( tag.kind ) {
            case "@see":
            case "@param":
            case "@throws":
            case "@exception":
            case "@return":
                return false;

            // case "@author":
            // case "@serial":
            // case "@serialData":
            // case "@serialField":
            // case "@since":
            // case "@version":
            //     return true;

            default:
                return true;
        }
    }

    return function(tags) {
      return (tags != null) ? _.filter(tags, isOtherBlockTag) : null;
    };

}])


/**
 * <sa-type> custom element. 
 *
 * TODO: types with typeParameters could maybe embed <sa-type-parameter> ?
 *
 */
.directive('saType', ["$compile", "Formatter", "JavadocModelUtils", "Log",
                      function($compile, Formatter, JavadocModelUtils, Log) {
    var _this = this;
    // -rx- this.logging = { prefix: "saType" };

    Log.log(_this, "ctor: ");

    // -rx- var compileFnCount = 0;
    // -rx- var clonedAttachCount = 0;
    // -rx- var linkFnCount = 0;

    /**
     * Pre-compiled template function for primitive types.
     */
    var primitiveTypeTemplateFn = $compile("<span>{{nameAndDimension}}</span>");

    /**
     * Pre-compiled template function for non-parameterized types.
     */
    var nonParameterizedTypeTemplateFn = $compile("<span><a href='#/q/java/qn/{{referenceName}}'>{{nameAndDimension}}</a>") ;

    /**
     * Replace the given element (<sa-type>) with a <span> for primitive types.
     * The span was pre-compiled to a template function above.  The template function
     * is called with the given scope to bind the scope to the template (to resolve
     * interpolations like {{nameAndDimension}} ). It returns a link function which,
     * when called, returns the DOM element (the *template* element).
     *
     * The link function in this case is ignored. Instead, we also pass a "cloned attach 
     * function" to the template function.  The template function will clone the template
     * element, bind the clone to the scope, then call the cloned attach function. 
     * The purpose of the cloned attach function is to attach the cloned element to the
     * DOM somewhere. In this case we replace the <sa-type> element with the cloned element.
     *
     */
    var linkPrimitiveType = function(scope, element) {
        // -rx- Log.log(_this, "linkFn: primitive type...");
        scope.nameAndDimension = JavadocModelUtils.getNameAndDimension(scope.typeDoc);
        // Why must we use the cloned element?  The element returned from the template
        // function is always the same element. An element can only exist in the DOM in 
        // one place, so if we tried to use that element repeatedly it wouldn't work. It
        // would only show up in the last place we appended it.  So instead, create a clone
        // of the element and use that.
        primitiveTypeTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned);
        });
    }

    /**
     * Replace the given element (<sa-type>) with a <span> for non-parameterized types.
     */
    var linkNonParameterizedType = function(scope, element) {
        // -rx- Log.log(_this, "linkFn: non-parameterized type...");
        scope.nameAndDimension = JavadocModelUtils.getNameAndDimension(scope.typeDoc);
        scope.referenceName = JavadocModelUtils.getReferenceName(scope.typeDoc);
        nonParameterizedTypeTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned);
        });
    }

    /**
     * Dynamically generate HTML for parameterized types.  Parameterized
     * types are too complicated/variable to apply to express with a simple html template.
     * Instead gen the HTML dynamically based on the type data.  This involves
     * calling the $compile function, which is expensive (hence the pre-compiled
     * templates for primitives and non-parameterized types).
     */
    var linkParameterizedType = function(scope, element) {

        $compile( "<span>" + Formatter.formatType(scope.typeDoc) + "</span>")(scope, function(cloned, scope) {
            // Log.log(_this, "linkFn.compile.link.clonedAttach: (" + (++clonedAttachCount) + ") " + JavadocModelUtils.getQualifiedName(scope.typeDoc));
            element.replaceWith(cloned); 
        });
    }

    /**
     * The directive's link function. 
     *
     * This is actually a 'post-link' function, which means the element passed in 
     * (the <sa-type> element) is already "linked" with the given scope.
     *
     * The post-link function gives us an opportunity after linking to make
     * further updates to the element, for example add DOM listeners or whatever.
     *
     * We can also choose to replace the given element entirely if we please, which
     * in this case, we do.  
     */
    var linkFn = function (scope, element, attrs) {
        // -rx- Log.log(_this, "linkFn: (" + (++linkFnCount) + ") " + (angular.isDefined(scope.typeDoc) ? JavadocModelUtils.getQualifiedName(scope.typeDoc): ""));

        if (angular.isDefined( scope.typeDoc ) ) {

            if (JavadocModelUtils.isPrimitiveType(scope.typeDoc)) {
                linkPrimitiveType(scope, element);

            } else if ( ! JavadocModelUtils.isParameterizedType(scope.typeDoc) ) {
                linkNonParameterizedType(scope, element);

            } else {
                linkParameterizedType(scope, element);
            }
        }
    };

    return {
      restrict: 'E',
      scope: {
        typeDoc: '=doc'
      },
      link: linkFn
    };
}])

/**
 * <sa-type-parameter> custom element. 
 *
 */
.directive('saTypeParameter', [ "$compile", "Formatter", "JavadocModelUtils", 
                               function($compile, Formatter, JavadocModelUtils) {

    var log = function(str) {
        // -rx- console.log("saTypeParameter: " + str);
    }

    /**
     * Pre-compiled template function for simple parameter types.
     * A "simple" parameter type has no bounds (i.e. no "extends" or "super")
     */
    var simpleTypeTemplateFn = $compile("<span>{{typeParameter.typeName}}</span>");

    /**
     * Replace the given element (<sa-type>) with a <span> for simple types.
     * The span was pre-compiled to a template function above.  The template function
     * is called with the given scope to bind the scope to the template (to resolve
     * interpolations like {{typeParameter.typeName}} ). It returns a link function which,
     * when called, returns the DOM element (the *template* element).
     *
     * The link function in this case is ignored. Instead, we also pass a "cloned attach 
     * function" to the template function.  The template function will clone the template
     * element, bind the clone to the scope, then call the cloned attach function. 
     * The purpose of the cloned attach function is to attach the cloned element to the
     * DOM somewhere. In this case we replace the <sa-type-parameter> element with the cloned element.
     *
     */
    var linkSimpleType = function(scope, element) {
        log("linkSimpleType: " + scope.typeParameter.typeName);
        simpleTypeTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned);
        });
    }

    var linkComplexType = function(scope, element) {
        log("linkComplexType: " + scope.typeParameter.typeName);
        $compile( "<span>" + Formatter.formatTypeParameter(scope.typeParameter) + "</span>")(scope, function(cloned, scope) {
            element.replaceWith(cloned); 
        });
    }

    var linkFn = function (scope, element, attrs) {

        // Why would it ever NOT be defined??
        if (angular.isDefined(scope.typeParameter)) {

            if (JavadocModelUtils.isSimpleTypeParameter(scope.typeParameter)) {
                linkSimpleType(scope, element);
            } else {
                linkComplexType(scope, element);
            }
        }
    };

    return {
      restrict: 'E',
      scope: {
        typeParameter: '=doc'
      },
      link: linkFn
    };
}])


/**
 * <sa-type-parameters> custom element. 
 *
 */
.directive('saTypeParameters', [ "$compile", "Formatter", 
                                 function($compile, Formatter) {

    var log = function(str) {
        // -rx- console.log("saTypeParameterS: " + str);
    }


    var noTypeParametersTemplateFn = $compile("<span></span>");

    var singleTypeParameterTemplateFn = $compile("<span>&lt;<sa-type-parameter doc='typeParameters[0]'></sa-type-parameter>&gt;</span>");

    var doubleTypeParameterTemplateFn = $compile("<span>&lt;"
                                                 + "<sa-type-parameter doc='typeParameters[0]'></sa-type-parameter>, "
                                                 + "<sa-type-parameter doc='typeParameters[1]'></sa-type-parameter>"
                                                 + "&gt;</span>");

    var linkNoTypeParameters = function(scope, element) {
        log("linkNoTypeParameters: ");
        noTypeParametersTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned); 
            scope.element = cloned; // save the new element (the cloned element) to the scope.
        });
    }

    var linkSingleTypeParameter = function(scope, element) {
        log("linkSingleTypeParameter: ");
        singleTypeParameterTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned);
            scope.element = cloned; // save the new element (the cloned element) to the scope.
        });
    }

    var linkDoubleTypeParameter = function(scope, element) {
        log("linkDoubleTypeParameter: ");
        doubleTypeParameterTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned); 
            scope.element = cloned; // save the new element (the cloned element) to the scope, to be replaced later when typeParameters is updated.
        });
    }

    /**
     * Dynamically compile html to handle multiple type parameters.
     */
    var linkManyTypeParameters = function(scope, element) {
        log("linkManyTypeParameters: ");

        var dynamicHtml =  "<span>" + Formatter.formatTypeParameters(scope.typeParameters) + "</span>";

        $compile( dynamicHtml )(scope, function(cloned, scope) {
            element.replaceWith(cloned); 

            // Need to set closure variable 'element' to 'cloned', otherwise element
            // will be null the next time scope.typeParameters is updated (and the $watch 
            // function called).
            scope.element = cloned; // save the new element (the cloned element) to the scope, to be replaced later when typeParameters is updated.
        });
    }


    var linkFn = function (scope, element, attrs) {

        // -rx- console.log("saTypeParameters.linkFn: " + JSON.stringify(scope.typeParameters, undefined, 2));
        // -rx- console.log("saTypeParameters.linkFn: " );

        // The $compile function below formats the type parameters as hard-coded html, 
        // so it's not "bound" to the scope with interpolating directives like you'd expect.
        // That's why we have to watch the typeParameters field of the scope and force it to
        // recompute the html.
        //
        // You may be wondering how come the javadocModel update doesn't automatically
        // trigger a built-in watcher on the typeParameters field and re-gen the directive
        // automatically?  Well, the javadocModel update *does* trigger a built-in watcher
        // on the typeParameters field.  However, it does NOT trigger a re-compile/link of
        // the directive.  The directive's link function is run only once -- after compilation,
        // to link the scope to the view gen'ed by the directive. Once the scope is linked,
        // it's linked forever.  Updates to the scope are processed thru watchers during $digest.  
        //
        // Since the view gen'ed by this directive doesn't contain any wathcers, it won't
        // respond to scope updates. Hence the need for the watcher.
        //

        if ( angular.isDefined( scope.typeParameters ) ) {

            // Need to save the element away in the scope so that the watcher
            // function below will be able to use it when the typeParameters field
            // is updated.  Also because we use element.replaceWith(clonedElement),
            // which means the original element is lost, replaced with the cloned.
            // We then save the cloned element to the scope, so that subsequent
            // updates to typeParameters will replace the cloned element.
            // (there must be a better way to explain this....)
            
            scope.element = element;

            scope.$watch('typeParameters', function() { 

                log("linkFn.watch: " + Formatter.formatTypeParameters(scope.typeParameters));

                if (scope.typeParameters.length == 0) {
                    linkNoTypeParameters(scope, scope.element);
                } else if (scope.typeParameters.length == 1) {
                    linkSingleTypeParameter(scope, scope.element);
                } else if (scope.typeParameters.length == 2) {
                    linkDoubleTypeParameter(scope, scope.element);
                } else {
                    linkManyTypeParameters(scope, scope.element);
                }
            });
        }
    };

    return {
      restrict: 'E',
      scope: {
        typeParameters: '=doc'
      },
      link: linkFn
    };
}])


/**
 * <sa-method-signature> custom element. 
 *
 */
.directive('saMethodSignature', ["$compile", "Formatter", 
                                 function($compile, Formatter) {

    var log = function(str) {
        // -rx- console.log("saMethodSignature: " + str);
    }

    var noParametersTemplateFn = $compile("<span>()</span>");

    var singleParameterTemplateFn = $compile("<span>(<sa-type doc='parameters[0].type'></sa-type> {{parameters[0].name}})</span>");

    var linkNoParameters = function(scope, element) {
        log("linkNoParameters: ");
        noParametersTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned); 
        });
    }

    var linkSingleParameter = function(scope, element) {
        log("linkSingleParameter: ");
        singleParameterTemplateFn(scope, function(cloned, scope) {
            element.replaceWith(cloned); 
        });
    }

    /**
     * Dynamically compile html to handle multiple parameters.
     */
    var linkMultipleParameters = function(scope, element) {
        log("linkMultipleParameters: " + scope.flatSignature);

        var dynamicHtml =  "<span>(" + Formatter.formatMethodSignature(scope.parameters, scope.flatSignature) + ")</span>";

        $compile( dynamicHtml )(scope, function(cloned, scope) {
            element.replaceWith(cloned); 
        });
    }


    var linkFn = function (scope, element, attrs) {

        if ( angular.isDefined( scope.parameters ) ) {

            if (scope.parameters.length == 0) {
                linkNoParameters(scope, element);
            } else if (scope.parameters.length == 1) {
                linkSingleParameter(scope, element);
            } else {
                linkMultipleParameters(scope, element);
            }
        }
    };

    return {
      restrict: 'E',
      scope: {
        parameters: '=',
        flatSignature: '='
      },
      link: linkFn
    };
}])



/**
 * underscore.js support.
 */
.factory('_', function() {
    return window._; // assumes underscore has already been loaded on the page
});

            
        
