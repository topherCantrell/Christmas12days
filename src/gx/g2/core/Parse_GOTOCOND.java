package gx.g2.core;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;


public class Parse_GOTOCOND extends Parser
{	
	
	public Parse_GOTOCOND(int commandInfo)
	{		
		super(commandInfo);				
	}	
	
	static String [] PROPELLER_COND_TABLE = {
	"IF_ALWAYS",     "1111",
	"IF_NEVER",      "0000",
	"IF_E",          "1010",
	"IF_NE",         "0101",
	"IF_A",          "0001", 
	"IF_B",          "1100", 
	"IF_AE",         "0011", 
	"IF_BE",         "1110", 
	"IF_C",          "1100", 
	"IF_NC",         "0011", 
	"IF_Z",          "1010", 
	"IF_NZ",         "0101",
	"IF_C_EQ_Z",     "1001",
	"IF_C_NE_Z",     "0110", 
	"IF_C_AND_Z",    "1000", 
	"IF_C_AND_NZ",   "0100", 
	"IF_NC_AND_Z",   "0010", 
	"IF_NC_AND_NZ",  "0001", 
	"IF_C_OR_Z",     "1110", 
	"IF_C_OR_NZ",    "1101", 
	"IF_NC_OR_Z",    "1011", 
	"IF_NC_OR_NZ",   "0111", 
	"IF_Z_EQ_C",     "1001", 
	"IF_Z_NE_C",     "0110", 
	"IF_Z_AND_C",    "1000", 
	"IF_Z_AND_NC",   "0010", 
	"IF_NZ_AND_C",   "0100", 
	"IF_NZ_AND_NC",  "0001", 
	"IF_Z_OR_C",     "1110", 
	"IF_Z_OR_NC",    "1011", 
	"IF_NZ_OR_C",    "1101", 
	"IF_NZ_OR_NC",   "0111" 
	};

	public String parse(CodeLine c, Cluster cluster, Map<String,String> subs, Assemble compiler) 
	{
				
		String s = c.text;
        String ss = s.toUpperCase();
        
        if(ss.equals("GOTO_COND") || ss.startsWith("GOTO-COND ")) {
            s = s.substring(9).trim();  
            Command_GOTOCOND fc = new Command_GOTOCOND(commandInfo,c,cluster);
            ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("LABEL",0);
            if(a==null) {
                return "Missing LABEL value.";
            }
            fc.label = a.value;
            a = aList.removeArgument("CONDITION",1);
            if(a==null) {
            	return "Missing CONDITION value.";
            }
                        
            int fi = -1;
            for(int x=0;x<PROPELLER_COND_TABLE.length;x=x+2) {
            	if(PROPELLER_COND_TABLE[x].equals(a.value)) {
            		fi = x;
            		break;
            	}
            }
            if(fi<0) {
            	return "Invalid CONDITION value '"+a.value+"'";
            }
            fc.cond = Integer.parseInt(PROPELLER_COND_TABLE[fi+1],2);
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            cluster.commands.add(fc);
            return "";
        }
		
		return null;
	}
	
}
