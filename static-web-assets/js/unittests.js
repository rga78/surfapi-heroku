

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

describe('Testing filter formatTags', function() {
    var formatTagsFilter;

    /**
     * Load the module and inject the formatTags filter into the test code.
     */
    beforeEach( function() {

        module("JavaApp");

        // Filters are injected via "<filter-name>Filter", just cuz.
        inject( function(_formatTagsFilter_) {
            formatTagsFilter = _formatTagsFilter_;
        });
    });



    it("should format @link tag", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@link java.io.FileReader bad link}"; 
        var expectedOutput = "hello <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader" + "'>" + " bad link" + "</a></span>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });

    it("should format @link tag with no link text", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@link java.io.FileReader}"; 
        var expectedOutput = "hello <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader" + "'>" + "java.io.FileReader" + "</a></span>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });

    it("should format @link tag across newlines", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@link java.io.FileReader bad link}"; 
        var expectedOutput = "hello <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader" + "'>" + " bad link" + "</a></span>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);

        var input2 = input + " blah {@link java.io.FileReader#read working\nlink}";
        var expectedOutput2 = expectedOutput + " blah <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader+read" + "'>" + " working\nlink" + "</a></span>";
        expect(formatTagsFilter(input2, javadocModel)).toBe(expectedOutput2);

    });


    it("should format @linkplain tag", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@linkplain java.io.FileReader bad link}"; 
        var expectedOutput = "hello <a href='#/q/java/qn/" + "java.io.FileReader" + "'>" + " bad link" + "</a>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });

    it("should format package-relative @link tag", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@link FileReader link text}"; 
        var expectedOutput = "hello <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader" + "'>" + " link text" + "</a></span>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });


    it("should format class-relative @link tag", function() {

        var packageModel = { metaType: "package",
                             name: "java.io" };
        var javadocModel = { metaType: "class",
                             containingPackage: packageModel,
                             name: "FileReader" };
            
        // TODO: notice that "#read" is converted to ".read". Would be nice to be smart enough not to include the leading ".".
        var input = "hello {@link #read}"; 
        var expectedOutput = "hello <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader+read" + "'>.read</a></span>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });


    it("should format @code tag", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@link java.io.FileReader bad link} along with {@code some code }"; 
        var expectedOutput = "hello <span class='code'><a href='#/q/java/qn/" + "java.io.FileReader" + "'>" + " bad link" + "</a></span>"
                              + " along with <code>some code </code>";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });


    it("should not format unknown tags", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@code some code } {@same }"; 
        var expectedOutput = "hello <code>some code </code> {@same }";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });

    it("should format @literal tags", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "hello {@literal hypothetical <b>BOLD</b>}"; 
        var expectedOutput = "hello hypothetical &lt;b&gt;BOLD&lt;/b&gt;";
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });

    it("doesn't format @value tags yet", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "The value of this field is {@value}"; 
        var expectedOutput = "The value of this field is {@value}"; 
        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);

        var input2 =  "Evaluates the script starting with {@value #SCRIPT_START}.";
        var expectedOutput2 =  "Evaluates the script starting with {@value #SCRIPT_START}.";
        expect(formatTagsFilter(input2, javadocModel)).toBe(expectedOutput2);
    });

    it("should format encoded tags... or should it?", function() {

        var javadocModel = { metaType: "package",
                             name: "java.io" };

        var input = "{&#64;linkplain javax.enterprise.context lifecycle context model}"
        var expectedOutput = "<a href='#/q/java/qn/" + "javax.enterprise.context" + "'>" + " lifecycle context model" + "</a>";

        expect(formatTagsFilter(input, javadocModel)).toBe(expectedOutput);
    });

});




