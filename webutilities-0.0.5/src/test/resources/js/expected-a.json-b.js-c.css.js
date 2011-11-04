var PersonJSON = function(name){
	
	this.getName = function(){
		return name;	
	}
}
var EmployeeJS = function(name,empId){
	
	this.getEmpId = function(){
		return empId;	
	}
}.inherits(PersonJSON);
var ManagerCSS = function(name,empId, reportees){
	
	this.getReporties = function(){
		return reportees;	
	}
}.inherits(EmployeeJS);