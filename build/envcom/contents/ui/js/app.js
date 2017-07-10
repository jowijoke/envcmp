/**
 * Create the module.
 */
var compareModule = angular.module('EnvCom', ['ui.bootstrap']);

/**
 * Controller for the EnvCom plugin.
 */
compareModule.controller('EnvComController', function(objectService, $uibModal, $scope ) {
	$scope.objectList = [
        { name: "Application", value: "Application" }, 
        { name: "Capability", value: "Capability" }, 
        { name: "CorrelationConfig", value: "CorrelationConfig"}, 
        { name: "Custom", value: "Custom" },
        { name: "Life Cycle Event", value: "IdentityTrigger" },
        { name: "ObjectConfig", value: "ObjectConfig" },
        { name: "Policy", value: "Policy" },
        { name: "Report", value: "TaskDefinition" },
        { name: "Role", value: "Bundle" },
        { name: "Rule", value: "Rule" },
        { name: "UIConfig", value: "UIConfig" },
        { name: "Workflow", value: "Workflow" },
        { name: "WorkGroup", value: "WorkGroup" }, 

    ];

	$scope.selection=[];
	// toggle selection for a given object by name
	$scope.toggleSelection = function toggleSelection(objectValue) {
    var idx = $scope.selection.indexOf(objectValue);

    // is currently selected
    if (idx > -1) {
      $scope.selection.splice(idx, 1);
    }

    // is newly selected
    else {
      $scope.selection.push(objectValue);
    }
  };
  
  $scope.checkAll = function () {
      angular.forEach($scope.objectList, function (objectValue) {
    	  objectValue.Selected = $scope.selectAll;
      });
  };   
  
  $scope.loader = {
		  loading: false,
		  };

  $scope.alertMessage = null;
  
  // Invoke this function to display the alert message
  function showAlert(alertType, msg) {
	  $scope.loader.loading = false ;
      $scope.alertMessage =   {
          // type defines the style of the alert message
          type: alertType,
          text: msg,
          // if true a button will be rendered to dismiss the alert message manually
          closable: true
          
      };
  };
    var me = this;
  
    //Once the objects are retrieved from REST this function will place each object to the right table.
    me.listObj = function() {
    	fetchObjs().then(function(objects) {
    		$scope.loader.loading = false ;
    		$scope.Extra=[];
    		$scope.Missing=[];
    		$scope.Difference=[];
    		console.log("objects " + objects);
            for (result in objects){
            	console.log("result " + result);
            	//If an warning messages is returned, it will be alertd to the user.
            	if ("warning" === result ){
            		console.log("result: " + result);
            		var text = objects[result]
                	showAlert(result, text);
                
            	}else if ("Extra" === result ){
            		var extraObjMap = objects[result] 
                	for (type in extraObjMap){
                		for(objName in extraObjMap[type]){
                			$scope.Extra.push({"name": extraObjMap[type][objName], "type": type})
                  		}
                	}
                }else if("Missing" === result){
                	 var missingObjMap = objects[result] 
                	 for (type in missingObjMap){
                		 for(objName in missingObjMap[type]){
                			$scope.Missing.push({"name": missingObjMap[type][objName], "type": type})
                		 }
                	}
                }else if("Difference" === result){
               	 var diffObjMap = objects[result] 
               	 for (type in diffObjMap){
               		 for(objName in diffObjMap[type]){
               			 console.log("Diff objName " + objName);
               			 var diffList = diffObjMap[type][objName]
               			 for(diff in diffList){
               				 console.log("diffList " + diffList);
           					 $scope.Difference.push({"name": objName, "type": type, "diff": diffList[diff]})
               			 }
               		 }
               	 }
                }else{
                	console.log("not found");
                }
                 }
             });

            }
    
    //Once Export XML button is pressed the three object arrays representing the tables are sent to the server.
    me.exportObj = function() {
    	console.log("exporting tables");
    	console.log("Extra: " + $scope.Extra); 
    	console.log("Missing: " + $scope.Missing);
    	console.log("Difference: " + $scope.Difference);
    	$scope.loader.loading = true ; // executes the loading screen
    	objectService.exportObjects($scope.Extra,$scope.Missing,$scope.Difference).then(function(response) {
    		//once a responce is received, the message will be sent to showAlert()
    		for (j in response){
            	console.log("response: " + j);
            	var k = response[j] 
            	console.log("text: " + k)
            	showAlert(j, k) ;
    		}
    	});
    }
    
    /**
     * Fetches the objects from the server.
     *
     * @return Promise A promise that resolves with the map of objects.
     */
    function fetchObjs() {
    	$scope.loader.loading = true ;
        return objectService.getObjects($scope.selection);
    }
    
});

compareModule.directive("alertMessage", function($compile) {
    return {
        scope: {
            alert: "="
        },
        link: function (scope, element) {
            // Redraw the alert message each time the object is modified
            scope.$watch('alert', function () {
                updateAlert();
            });
 
            // Close alert message
            scope.close = function() {
                scope.alert = null;
            }
 
            function updateAlert() {
                var html = "";
 
                if (scope.alert) {
                    var icon = null;
 
                    switch (scope.alert.type) {
                        case 'success': {
                            icon = 'ok-sign';
                        } break;
                        case 'warning': {
                            icon = 'exclamation-sign';
                        } break;
                        case 'info': {
                            icon = 'info-sign';
                        } break;
                        case 'danger': {
                            icon = 'remove-sign';
                        } break;
                    }
 
                    html = "<div class='alert alert-" + scope.alert.type + "' role='alert'>";
 
                    if (scope.alert.closable) {
                        html += "<button type='button' class='close' data-dismiss='alert' ng-click='close()' aria-label='Close'><span aria-hidden='true'>&times;</span></button>";
                    }
 
                    if (icon) {
                        html += "<span style='padding-right: 5px;' class='glyphicon glyphicon-" + icon + "' aria-hidden='true'></span>";
                    }
 
                    html += scope.alert.text;
                    html += "</div>";
                }
 
                var newElement = angular.element(html);
                var compiledElement = $compile(newElement)(scope);
 
                element.html(compiledElement);
 
                if (scope.alert && scope.alert.delay > 0) {
                    setTimeout(function () {
                        scope.alert = null;
                        scope.$apply();
                    }, scope.alert.delay * 1000);
                }
            }
        }
     }
});

/**
 * Service that handles functionality for iiq objects.
 */
compareModule.service('objectService', function($http) {

    var config = {
        headers: {
            'X-XSRF-TOKEN': PluginHelper.getCsrfToken()
        }
    };
 

    return {

        /**
         * Gets objects from Server
         *
         * @return Promise A promise that resolves with an array of objects.
         */
        
        getObjects: function(selection) {
        	console.log("selection: " + selection);
        	var OBJECTS_URL = PluginHelper.getPluginRestUrl('envcom/objects');
        	
        	//loop used to iterate through "selection" array to be printed onto the URL.
        	for (var i=0; i<selection.length; ++i) {
        	    if (OBJECTS_URL.indexOf('?') === -1) {
        	    	OBJECTS_URL = OBJECTS_URL + '?selection=' + selection[i];  
        	    }else {
        	    	OBJECTS_URL = OBJECTS_URL + '&selection=' + selection[i];
        	    }
        	}
        	
        	console.log(OBJECTS_URL);
            return $http.get(OBJECTS_URL, config).then(function(response) {
            	console.log("res" + response.data);
            	
                return response.data;
            });
        },
        
        //Post Request to send object from tables to export.
        exportObjects: function(Extra, Missing, Difference) {
        
        	var OBJECTS_URL = PluginHelper.getPluginRestUrl('envcom/export'),
                    payload = {
                        extra: Extra,
                        missing: Missing,
                        difference: Difference
                    };
        	
        	return $http.post(OBJECTS_URL, payload, config).then(function(response) {
            	console.log("res" + response.data);
                return response.data;
            });
        	
        },

    };

});

