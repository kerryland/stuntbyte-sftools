package org.example

class Book__c {
    String id
    String title__c
    String author__c
    static transients = ['id']

    static mapping = {
    // http://docs.jboss.org/hibernate/core/3.3/reference/en/html/mapping.html#mapping-declaration-id


//         id natural:['id'], type: 'string', generator: "identity"
         id column: 'Id', type:'string', generator: "identity"
//	 id generator:'native', column:'id', type:'string'
         version 'Version__c'
		 // columns { 
	// 		 id sqlType:'varchar' 
// 		 } 
	 // id type:Text generator:'guid'


    }
//    columns { 
//	id sqlType:'varchar' 
//    } 

    static constraints = {
        title__c(blank: false)
        author__c(blank: false)
    }

}
