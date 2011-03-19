var Person = function(name){
	
	this.getName = function(){
		return name;	
	}
}
var Employee = function(name,empId){
	
	this.getEmpId = function(){
		return empId;	
	}
}.inherits(Person);
var Manager = function(name,empId, reportees){
	
	this.getReporties = function(){
		return reportees;	
	}
}.inherits(Employee);