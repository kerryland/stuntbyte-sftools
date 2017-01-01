/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
