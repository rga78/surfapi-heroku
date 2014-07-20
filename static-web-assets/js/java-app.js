angular.module( "JavaApp", ['ngRoute', 
                            'ui.bootstrap', 
                            'ngSanitize', 
                            'ui.keypress'] )
        
/**
 * 
 */
.config( function($locationProvider, $routeProvider) {
    $locationProvider.html5Mode(true);
})


/**
 * TODO: change this to a  "/q/*" rest target
 */
.service("AutoCompleteService", function($http) {

    var buildUrl = function(str, indexName) {
        indexName = indexName || "java";
        return "rest/autoComplete/index?str=" + str + "&index=" + indexName; 
    }

    this.get = function(str, indexName) {
        return $http.get( buildUrl(str, indexName)  )    
                    .error( function(data, status, headers, config) {
                        alert("AutoCompleteService.get(" + str +") didn't work: " + status + ": " + data);
                    });
    }
})

/**
 *
 */
.service("DbService", function($http, $rootScope) {

    var onSuccess = function(data) {
        $rootScope.$emit( "$saJavadocModelChange", data );
    }

    var buildUrl = function(_id) {
        return "rest" + _id;
    }
    
    var get = function(_id) {
        _id = _id.replace(/\?/g, "%3F");

        console.log("DbService.get: " + _id);

        return $http.get( buildUrl(_id) )
                    .success( onSuccess )
                    .error( function(data, status, headers, config) {
                        alert("DbService.get(" + _id + ") didn't work: " + status + ": " + data);
                        // TODO: create modal with option to send error message to me for diagnosing.
                    });
    }

    /**
     * Same as get() except without the event emit.
     */
    var getQuietly = function(_id) {
        _id = _id.replace(/\?/g, "%3F");

        console.log("DbService.getQuietly: " + _id);

        return $http.get( buildUrl(_id) )
                    .error( function(data, status, headers, config) {
                        alert("DbService.get(" + _id + ") didn't work: " + status + ": " + data);
                        // TODO: create modal with option to send error message to me for diagnosing.
                    });
    }

    this.get = get;
    this.getQuietly = getQuietly;
})

/**
 *
 */
.service("Formatter", function(_, Utils, JavadocModelUtils) {

    var isEmpty = Utils.isEmpty;

    var formatTypeArguments = function(typeArguments) {
        return (isEmpty(typeArguments)) ? "" :  "&lt;" + ( _.map(typeArguments, formatType).join(", ") ) + "&gt;";
    };

    var formatTypeName = function(type) {
        return (JavadocModelUtils.isMethod(type)) 
               ? JavadocModelUtils.getQualifiedName(type)
               : JavadocModelUtils.getName(type) + (type.dimension || "");
    }

    var asAnchor = function(type) {
        return "<a href='#" + type._id + "'>" + formatTypeName(type) + "</a>";
    }

    var asAnchorRef = function(type) {
        // TODO: getQualifiedName won't work with methods (need to put the '+' in there).  
        //       But currently methods are never formatted by this code so we're good for now.
        return "<a href='#/q/java/qn/" + JavadocModelUtils.getReferenceName(type) + "'>" + formatTypeName(type) + "</a>" ;
    }

    var asSpan = function(type) {
        var retMe = "<span title=" + enquote( JavadocModelUtils.getQualifiedName(type) ) + ">";

        if (type._id) {
            retMe += asAnchor(type);
        } else if (JavadocModelUtils.getName(type) != JavadocModelUtils.getQualifiedName(type)) {
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
        // console.log("Formatter.formatMethodSignature: " + flatSignature);
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

        // console.log("Formatter.formatLinkplainTag: " + JSON.stringify(tag) + "; ref: " + ref);

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
        // -rx- console.log("formatLinkTagMethodParms: " + methodParms + " --> " + retMe);

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
                return "{@inheritDoc}"; // TODO
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
    this.formatType = formatType;
    this.formatTypeParameters = formatTypeParameters;
    this.formatMethodSignature = formatMethodSignature;
    this.formatSeeTag = formatSeeTag;
    this.formatInlineTag = formatInlineTag;
    this.formatInlineTags = formatInlineTags;
    this.formatAnnotation = formatAnnotation;
})


/**
 * Dumping ground for un-categorizable stuff.
 */
.service("Utils", function(_) {

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
    this.isEmpty = isEmpty;
    this.setLocation = setLocation;
    this.getPrevLocation = getPrevLocation;
    this.parseLibraryFromLocation = parseLibraryFromLocation;
})


/**
 * Convenience methods for handling a javadoc models.
 */
.service("JavadocModelUtils", function(Utils,_) {

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
        var retMe = _.contains( ClassElementMetaTypes, getMetaType(model) );
        // console.log("JavadocModelUtils.isClassElement: " + retMe + ", model: " + ((model) ? model.metaType : null));
        return retMe;
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

    // Note: assumes model is NOT null
    var getPackageId = function( model ) {
        return ( isPackage(model) ) ? model._id : getId( model.containingPackage );
    };

    // Note: assumes model is NOT null
    var getLibraryId = function( model ) {
        return ( isLibrary(model) ) ? model._id : getId( model._library );
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
    this.getId = getId;
    this.getMetaType = getMetaType;
    this.getPackageId = getPackageId;
    this.getLibraryId = getLibraryId;
    this.getClassFor = getClassFor;
    this.getPackageFor = getPackageFor;
    this.getQualifiedName = getQualifiedName;
    this.getName = getName;
    this.isMethod = isMethod;
    this.isInterface = isInterface;
    this.isClass = isClass;
    this.isClassElement = isClassElement;
    this.isPackage = isPackage;
    this.isLibrary = isLibrary;
    this.isLibraryVersions = isLibraryVersions;
    this.isLang = isLang;
    this.getUnformattedTags = getUnformattedTags;
    this.getReferenceName = getReferenceName;
})


/**
 * "service" for transforming models (returned from Db) into ViewModels
 * (enhanced with fields specific for view rendering).
 */
.service("ViewModelService", function(_) {

    this.transform = function(model) {
        return model;
    };

    this.getScopeName = function(model) {
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

})

/**
 * Handles "refernece" queries - i.e. queries that attempt to resolve non-_id references
 * to packages/classes/methods/fields by querying the DB for the qualified name of the 
 * reference type. 
 */
.service("ReferenceQueryService", function(DbService, JavadocModelUtils, $location, $window, $modal, Utils, _) {

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

        modalInstance.result.then( function(selectedModel) {
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
        // console.log( "ReferenceQueryService.filterModels: libraryId: " + libraryId + "; filteredModels: " + JSON.stringify(filteredModels, undefined, 2));
        return (!Utils.isEmpty(filteredModels)) ? filteredModels : models;
    }

    var getReferenceTypeOnSuccess = function( models, referenceName) {
        // console.log("ReferenceQueryService.success: " + JSON.stringify(models,undefined,2));
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
        console.log("ReferenceQueryService.getReferencetype: " + id);
        var referenceName = id.substring( "/q/java/qn/".length );
        console.log("ReferenceQueryService.getReferencetype: " + referenceName);

        DbService.getQuietly( id )
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

        console.log("ReferenceQueryService.resolveFirstId: " + referenceName);
        var restUrl = "/q/java/qn/" + referenceName;
        return DbService.getQuietly( restUrl )
                 .then( function(response) {
                     var models = response.data;
                     console.log("ReferenceQueryService.resolveFirstId.then: " + JSON.stringify(models));
                     return (models.length > 0) ? models[0]._id : null;
                 });
    }

    this.getReferenceType = getReferenceType;
    this.resolveFirstId = resolveFirstId;
})

/**
 *
 * http://angular-ui.github.io/bootstrap/
 */
.controller("ModalMessageController", function($scope, $modalInstance, message, title) {

    $scope.message = message;
    $scope.title = title;

    $scope.ok = function () {
        $modalInstance.close();     // can pass 'result' into close() to be received by modalInstance
                                    // returned by open().
    };

    // -rx- $scope.cancel = function () {
    // -rx-     $modalInstance.dismiss('cancel');
    // -rx- };

})

/**
 *
 */
.controller("ModalReferenceQueryResultsController", function($scope, $modalInstance, models) {

    $scope.models = models;

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
})



/**
 * For use with the auto-complete box.
 */
.controller( "SearchController", function($scope, 
                                          AutoCompleteService, 
                                          Utils, 
                                          JavadocModelUtils) {
    
    var callAutoCompleteService = function(str) {
        console.log("SearchController.callAutoCompleteService:  str=#" + str + "#, autoCompleteIndexName: " + $scope.autoCompleteIndexName);
        AutoCompleteService.get( str, $scope.autoCompleteIndexName )
             .success( function(data) {
                 $scope.autoCompleteData = data;
             });
    };
        
    var clearListing = function() {
        $scope.autoCompleteData = [];
        $scope.str = "";
    }

    var onKeypress = function($event) {
        // -rx- console.log("SearchController.onKeypress: " + JSON.stringify($event.keyCode));
        if ($event.keyCode == 27) {
            // ESC key
            clearListing();
        }
    }

    var onChange = function() {
        if (Utils.isEmpty($scope.str)) {
            clearListing();
        } else {
            callAutoCompleteService($scope.str);
        }
    };

    // Export to scope.
    $scope.clearListing = clearListing;
    $scope.onKeypress = onKeypress;
    $scope.onChange = onChange;
    $scope.getReferenceName = JavadocModelUtils.getReferenceName;
   
})


/**
 * For use with /java/* URIs.
 *
 * Main app controller handles routes.
 *
 * A controller will get new'ed up every time the route updates
 * if the router has 'controller' field set (templateUrl too?)
 */
.controller( "JavadocController", function($scope, 
                                           $rootScope, 
                                           $location, 
                                           DbService, 
                                           ViewModelService, 
                                           _, 
                                           Utils, 
                                           Formatter,
                                           ReferenceQueryService,
                                           JavadocModelUtils) {

    console.log("JavadocController: invoked");

    // Listen for hash changes.
    $scope.$watch(function() {
                      return $location.hash();
                  },
                  function(id) {
                      console.log("JavadocController: id: " + id);

                      if ( Utils.isEmpty(id) ) {
                          fetchJavadoc("/java");
                      } else if (id.indexOf("/java") == 0) {
                          fetchJavadoc(id);
                      } else if (id.indexOf("/q/java/qn/") == 0) {
                          ReferenceQueryService.getReferenceType(id);
                      }
                  });

    // Export functions to scope.
    $scope.formatSeeTag = Formatter.formatSeeTag;
    $scope.formatAnnotation = Formatter.formatAnnotation;
    $scope.isInterface = JavadocModelUtils.isInterface;
    $scope.isClass = JavadocModelUtils.isClass;
    $scope.isClassElement = JavadocModelUtils.isClassElement;
    $scope.isPackage = JavadocModelUtils.isPackage;
    $scope.isLibrary = JavadocModelUtils.isLibrary;
    $scope.isLibraryVersions = JavadocModelUtils.isLibraryVersions;
    $scope.isLang = JavadocModelUtils.isLang;
    $scope.getUnformattedTags = JavadocModelUtils.getUnformattedTags;

    // Export functions to rootScope.
    $rootScope.isEmpty = Utils.isEmpty;
    
    // disable logging.
    // window.console.log = function() { };
   
    /**
     * wrapper function around DbService.get with callbacks for pushing the result
     * into the $scope.
     *
     * TODO: look into using $resource - can bind returned object into scope.
     */
    var fetchJavadoc = function(id) {

        scrollTop();

        $scope.requestPending = true;
        DbService.get( id )
                 .success( function(data) {
                     
                    // -rx- $scope.javadocModel = {};
                    $scope.javadocModel = data;  

                    // -rx- $scope[ ViewModelService.getScopeName(data) ] = {};
                    $scope[ ViewModelService.getScopeName(data) ] = ViewModelService.transform( data );
                 }).finally( function() {
                    $scope.requestPending = false;
                 }) ;
    };

    var scrollTop = function() {
        document.body.scrollTop = document.documentElement.scrollTop = 0;
    }

})


/**
 *
 */
.controller( "AllKnownSubclassesController", function($scope, 
                                                      $rootScope,
                                                      DbService,
                                                      JavadocModelUtils,
                                                      Utils,
                                                      _) {

    console.log("AllKnownSubclassesController: invoked");

    // initialize scope data
    $scope.allKnownSubclasses = [];
    $scope.isEmpty = Utils.isEmpty;

    var buildUrl = function(model) {
        return "/q/java/allKnownSubclasses/" + JavadocModelUtils.getQualifiedName( model );
    };

    /**
     * @param model - the superclass model.  if it's null, $scope.javadocModel is used (inherited).
     */
    var fetchSubclasses = function(model) { 

        $scope.allKnownSubclasses = [];
        console.log("AllKnownSubclassesController.fetchSubclasses: " + ((model) ? model._id : ""));

        DbService.getQuietly( buildUrl(model || $scope.javadocModel) )
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
        fetchSubclasses(model);  
    });

    fetchSubclasses();
})

/**
 *
 */
.controller( "AllKnownImplementorsController", function($scope, 
                                                        $rootScope,
                                                        DbService,
                                                        // -rx- Utils,
                                                        JavadocModelUtils,
                                                        _) {

    console.log("AllKnownImplementorsController: invoked");

    // initialize scope data
    $scope.allKnownImplementors = []
    // -rx- $scope.isEmpty = Utils.isEmpty;

    var buildUrl = function(model) {
        return "/q/java/allKnownImplementors/" + JavadocModelUtils.getQualifiedName( model );
    };

    /**
     *
     */
    var fetchImpls = function(model) {

        $scope.allKnownImplementors = []

        DbService.getQuietly( buildUrl(model || $scope.javadocModel) )
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
})

/**
 *
 */
.controller( "MethodController", function($scope, 
                                          DbService, 
                                          ViewModelService, 
                                          _, 
                                          Formatter, 
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

        console.log("MethodController.showFullDocForId: " + _id);

        // Check if the field/method is the primary view (and if so, don't bother toggling the full doc).
        if (isLocationHashOnId( _id )) {
            return;
        }

        $scope.showFullDocToggle = !$scope.showFullDocToggle;

        if ($scope.showFullDocToggle) {
            DbService.getQuietly( _id )
                            .success( function(data) {
                               // -rx- console.log("MethodController.showFullDocForId.success: " + JSON.stringify(data));
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


    // Needed by the view to properly render the method signature
    $scope.formatMethodSignature = function() {
        return Formatter.formatMethodSignature( $scope.methodDoc.parameters, $scope.methodDoc.flatSignature );
    };

    // If location.hash matches this doc id (meaning the user specifically looked up this method),
    // then automatically show the full doc section.
    if (isLocationHashOnId( getDocId() )) {
        $scope.showFullDocToggle = true;
    }

})

/**
 *
 */
.controller( "NavPathBarController", function($scope, $rootScope, _, JavadocModelUtils, Utils) {

    /**
     * Listen for location/route changes and set the old/new in the Utils object.
     */
    $rootScope.$on( "$locationChangeSuccess", function(event, newUrl, oldUrl) {
        console.log("NavPathBarController.$locationChangeSuccess: newUrl: " + newUrl + "; oldUrl: " + oldUrl);
        Utils.setLocation(newUrl, oldUrl);
    });

    /**
     * Listen for changes to the javadoc model on display.
     * Upon change, update the nav-path-bar
     */
    $rootScope.$on( "$saJavadocModelChange", function(event, model) {

        // console.log( "NavPathBarController: on event: " + JavadocModelUtils.getId(model));
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

})

/**
 *
 */
.controller( "NavLibraryController", function($scope, $rootScope, JavadocModelUtils, _, DbService) {

    var currentLibraryId = null;
    var currentPackageId = null;

    /**
     * Listen for changes to the javadoc model on display.
     * Upon change, check against the currently listed library/package 
     * and refresh the view only if the library/package has changed.
     */
    $rootScope.$on( "$saJavadocModelChange", function(event, model) {

        // console.log( "NavLibraryController: on event: " + JavadocModelUtils.getId(model));
        
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
    });

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

        // console.log("NavLibraryController.refreshLibrary: current/new: " + currentLibraryId + ", " + newLibraryId);

        if ( newLibraryId != currentLibraryId ) {

            DbService.getQuietly( newLibraryId )
                     .success( showLibrary );
        }
    };


    /**
     * Refresh the package nav view (class list) if the package has changed.
     */
    var refreshPackage = function(newPackageId) {

        if ( newPackageId != currentPackageId ) {
            DbService.getQuietly( newPackageId )
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

})


/**
 *
 */
.filter('prettyPrintJson', function() {

    return function(model) {
        return JSON.stringify(model, undefined, 2);
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
 * TODO: i desperately need to learn a front-end/js unit testing framework.
 */
.filter('formatTags', function(Formatter, _, Utils, JavadocModelUtils) {

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
})

/**
 * 
 */
.filter('formatInlineTags', function(Formatter) {
    return function(tags, packageName, className) {
        // -rx- console.log("formatInlineTags: " + packageName + ", " + className);
        return Formatter.formatInlineTags(tags, packageName, className);
    }
})

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
.filter('otherBlockTags', function(_) {

    var isOtherBlockTag = function(tag) {
        switch( tag.kind ) {
            case "@see":
            case "@param":
            case "@throws":
            case "@exception":
            case "@return":
                return false;

            // -rx- case "@author":
            // -rx- case "@serial":
            // -rx- case "@serialData":
            // -rx- case "@serialField":
            // -rx- case "@since":
            // -rx- case "@version":
            // -rx-     return true;

            default:
                return true;
        }
    }

    return function(tags) {
      return (tags != null) ? _.filter(tags, isOtherBlockTag) : null;
    };

})


/**
 * <sa-type> custom element. 
 *
 */
.directive('saType', function($compile, Formatter) {

    var x = 0, y=0;

    var linkFn = function (scope, element, attrs) {
        // console.log("saType.link: " + JSON.stringify( scope.typeDoc, undefined, 2) );

        if (angular.isDefined( scope.typeDoc ) ) {
            $compile( "<span>" + Formatter.formatType(scope.typeDoc) + "</span>")(scope, function(cloned, scope) {
                // console.log("saTypeHelper.compile.link.callback: " + ++x);
                element.replaceWith(cloned); 
            });
        }
    };

    return {
      restrict: 'E',
      scope: {
        typeDoc: '=doc'
      },
      link: linkFn
    };
})

/**
 * <sa-type-parameters> custom element. 
 *
 */
.directive('saTypeParameters', function($compile, Formatter) {

    var x = 0, y=0;

    var linkFn = function ($scope, element, attrs) {

        // -rx- console.log("saTypeParameters.linkFn: " + JSON.stringify($scope.typeParameters, undefined, 2));

        $scope.$watch('typeParameters', function() { 

            // -rx- console.log("saTypeParameters.linkFn.watch: " + JSON.stringify($scope.typeParameters, undefined, 2));

            if ( angular.isDefined( $scope.typeParameters ) ) {
                $compile( "<span>" + Formatter.formatTypeParameters($scope.typeParameters) + "</span>")($scope, function(cloned, $scope) {
                    element.replaceWith(cloned); 

                    // Need to set closure variable 'element' to 'cloned', otherwise element
                    // will be null the next time $scope.typeParameters is updated (and the $watch 
                    // function called).
                    element = cloned;   
                });
            }

        });
    };

    return {
      restrict: 'E',
      scope: {
        typeParameters: '=doc'
      },
      link: linkFn
    };
})

/**
 * underscore.js support.
 */
.factory('_', function() {
    return window._; // assumes underscore has already been loaded on the page
});

            
        
