angular.module( "dbApp", ['ngRoute', 'ui.bootstrap'] )
        
/**
 * Routing.
 */
.config(function($routeProvider) {
    $routeProvider
            .when('/', 
                   {
                      templateUrl: 'partials/db-stats.html',
                      controller: 'DbStatsController',
                   })
            .otherwise( { redirectTo: '/' });
})


/**
 *
 */
.service("DbService", function($http, $rootScope) {

    var buildUrl = function(_id) {
        return "rest" + _id;
    }

    var get = function(_id) {
        _id = _id.replace(/\?/g, "%3F");

        console.log("DbService.get: " + _id);

        return $http.get( buildUrl(_id) )
                    .error( function(data, status, headers, config) {
                        alert("DbService.get(" + _id + ") didn't work: " + status + ": " + data);
                        // TODO: create modal with option to send error message to me for diagnosing.
                    });
    }

    this.get = get;
})

/**
 * Main app controller.
 */
.controller( "DbStatsController", function($scope, $rootScope, DbService ) {
   
    var fetchDbStats = function() {

        DbService.get( "/_db" )
                 .success( function(data) {
                    $scope.dbStats = data;
                 })
    };

    fetchDbStats();

    document.body.scrollTop = document.documentElement.scrollTop = 0;
})

/**
 *
 */
.filter('prettyPrintJson', function() {

    return function(model) {
        return JSON.stringify(model, undefined, 2);
    }
});

            
        
