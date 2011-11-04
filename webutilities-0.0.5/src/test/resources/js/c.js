var Manager = function(name,empId, reportees){
	
	this.getReporties = function(){
		return reportees;	
	}
}.inherits(Employee);
