package ec.cusoft.facturaec.utils;

import java.util.Vector;

public class Utils {
	
	public static String[] extractByCharInArray(String cadena, String charac){
  		Vector<String> v=new Vector<String>();
  		
  		String aux="";
  		for (int i = 0; i < cadena.length(); i++) {
  			if(charac.contains(String.valueOf(cadena.charAt(i)))){
  				if(!aux.trim().equals(""))
  					v.add(aux);
  				aux="";
  			}
  			else aux+=String.valueOf(cadena.charAt(i));
  		}
  		if(!aux.trim().equals(""))
  			v.add(aux);
  		String[] res=new String[v.size()];
  		v.copyInto(res);
  		return res;		
  	}
}
