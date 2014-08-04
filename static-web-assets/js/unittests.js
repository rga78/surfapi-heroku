

describe('Testing JavadocModelUtils', function() {
  
    /**
     * Ref to the service.
     */
    var JavadocModelUtils; 

    /**
     * This function will be called before every "it" block.
     * This should be used to "reset" state for your tests.
     */
    beforeEach(function (){

        // load the module you're testing.
        module('JavaApp');

        // INJECT! This part is critical
        // inject your service for testing.
        // The _underscores_ are a convenience thing
        // so you can have your variable name be the
        // same as your injected service.
        inject(function(_JavadocModelUtils_) {
            JavadocModelUtils = _JavadocModelUtils_;
        });
    });

    // 
    it('should return name or typeName', function () { 
        expect(JavadocModelUtils.getName( { name: "myName" } )).toBe("myName");
        expect(JavadocModelUtils.getName( { name: "myName", typeName: "myTypeName" } )).toBe("myName");
        expect(JavadocModelUtils.getName( { typeName: "myTypeName" } )).toBe("myTypeName");
    });

    // 
    it('should return id', function (){
        expect(JavadocModelUtils.getId( { _id: "/my/id" } )).toBe("/my/id");
        expect(JavadocModelUtils.getId( { noid: "no.id" } )).toBe(undefined);
        // TODO: expect(JavadocModelUtils.getId( { } )).toBe(null);
        expect(JavadocModelUtils.getId( null )).toBe(null);
    });
});
    

describe('Testing saType directive', function() {
    var scope;
    var elem;
    var linkFn;

    beforeEach(function (){
        //load the module
        module('JavaApp');
    
        inject(function($compile, $rootScope) {
            // create a new scope for each test (you could just use $rootScope, I suppose)
            scope = $rootScope.$new();
   
            var html = "<div><sa-type doc='typeModel'></sa-type></div>";

            elem = angular.element( html );

            linkFn = $compile(elem);
        });
    });
    
    /**
     *
     */
    it('Should be the primitive', function() {

        scope.typeModel = {
            "typeName": "int",
            "dimension": "",
            "simpleTypeName": "int",
            "qualifiedTypeName": "int",
            "wildcardType": null,
            "toString": "int",
            "parameterizedType": null
        };
 
        linkFn(scope);

        //call digest on the scope!
        scope.$digest();

        //check to see if it's blank first.
        expect(elem.text()).toBe('int');
        expect(elem.html()).toBe('<span class="ng-scope ng-binding">int</span>');
    });

    /**
     *
     */
    it('Should be the primitive array', function() {

        scope.typeModel = {
            "typeName": "int",
            "dimension": "[]",
            "simpleTypeName": "int",
            "qualifiedTypeName": "int",
            "wildcardType": null,
            "toString": "int",
            "parameterizedType": null
        };
 
        // Bind the template function with the scope.
        // This will also execute the link function of the directive,
        // which in this case will replace the <sa-type> element with
        // a <span> element that contains interpolated content.  
        linkFn(scope);

        // Call digest on the scope!
        // The linkFn above set up watchers for things like the interpolated 
        // content.  Calling digest executes the callbacks for those watchers.
        // The callbacks do things like update the view.
        scope.$digest();

        // Verify the view was updated as we expect
        expect(elem.text()).toBe('int[]');
        expect(elem.html()).toBe('<span class="ng-scope ng-binding">int[]</span>');
    });

    /**
     *
     */
    it('Should be a simple non-parameterized type', function() {

        scope.typeModel = {
            "typeName": "String",
            "dimension": "",
            "simpleTypeName": "String",
            "qualifiedTypeName": "java.lang.String",
            "wildcardType": null,
            "toString": "java.lang.String",
            "parameterizedType": null
        };
 
        // Bind the template function with the scope.
        // This will also execute the link function of the directive,
        // which in this case will replace the <sa-type> element with
        // a <span> element that contains interpolated content.  
        linkFn(scope);

        // Call digest on the scope!
        // The linkFn above set up watchers for things like the interpolated 
        // content.  Calling digest executes the callbacks for those watchers.
        // The callbacks do things like update the view.
        scope.$digest();

        // Verify the view was updated as we expect
        expect(elem.text()).toBe('String');
        expect(elem.html()).toBe('<span class="ng-scope"><a href="#/q/java/qn/java.lang.String" class="ng-binding">String</a></span>');
    });


});

