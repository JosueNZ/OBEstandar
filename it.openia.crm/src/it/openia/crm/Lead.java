/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

/**
 *
 * @author nicholas
 */
public class Lead {
    private String id;
    private String firstname;
    private String lastname;
    private String commercialname;
    
    public Lead(String i,String fname,String lname, String cname){
        this.id = i;
        this.firstname = fname;
        this.lastname = lname;
        this.commercialname = cname;
    }
    
}
