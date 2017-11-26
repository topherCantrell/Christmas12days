package gx.g2.core;

import java.util.Map;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

public class Parse_THREADGOTO extends Parser
{

	public Parse_THREADGOTO(int commandInfo) {		
		super(commandInfo);		
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs,
			Assemble compiler) 
	{
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("THREADGOTO") || ss.startsWith("THREADGOTO ")) {
			
			s=s.substring(10).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("THREAD",0);
            if(a==null) {
                return "Missing THREAD operand.";
            }            
            String ts = a.value;
            
            a = aList.removeArgument("LABEL",1);
            if(a==null) {
            	return "Missing LABEL operand.";
            }
            String ls = a.value;
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            Command_OP op = new Command_OP();
            String er = Parse_Operand.parse(ts, op, subs);            
    		if(er!=null) return er;
            
            Command_THREADGOTO com = new Command_THREADGOTO(commandInfo,op,c,cluster);
            com.label = ls;
    		
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}
	
}
