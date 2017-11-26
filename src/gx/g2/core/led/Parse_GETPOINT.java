package gx.g2.core.led;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;
import gx.g2.core.Parse_Operand;

import java.util.Map;

public class Parse_GETPOINT extends Parser
{

	public Parse_GETPOINT(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("GETPOINT") || ss.startsWith("GETPOINT ")) {
			s=s.substring(9).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("X",0);
            if(a==null) {
                return "Missing X operand.";
            }            
            String xs = a.value;
            a = aList.removeArgument("Y",1);
            if(a==null) {
                return "Missing Y operand.";
            }            
            String ys = a.value;
                                    
            a = aList.removeArgument("DESTINATION", 2);
            if(a==null) {
            	return "Missing DESTINATION operand.";
            }
            String zs = a.value;
            
            int rel = 0;
            a = aList.removeArgument("RELATIVE",3);
            if(a!=null) {
            	if(a.value.toUpperCase().equals("RELATIVE")) {            		
            		rel = 1;
            	} else if(!a.longValueOK || a.longValue<0 || a.longValue>1) {
            		return "Invalid RELATIVE value. Must be 1 or 0 (true or false).";
            	} else {
            		rel = (int)a.longValue;
            	}       
            }
            
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = commandInfo << 3;
    		com.opcode = com.opcode | rel;
    		
            String er = Parse_Operand.parse(xs, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(ys, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(zs, com, subs);
    		if(er!=null) return er;
                		
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}

}
