package gx.g2.core;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;

public class Parse_SEGMENT extends Parser
{
			
	public Parse_SEGMENT(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) {
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("SEGMENT") || ss.startsWith("SEGMENT ")) {
			
			// TODO ability to specify time constants like "100ms"
			
			s=s.substring(7).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("AREA",0);
            if(a==null) {
                return "Missing AREA operand.";
            }            
            String xs = a.value;
            
            int v = 0;
            if(xs.equals("0")) {
            	v = 0;
            } else if(xs.equals("UPPER")) {
            	v = 1;
            } else if(xs.equals("DATA")) {
            	v = 2;
            } else if(xs.equals("CODE")) {
            	v = 3;
            } else {
            	return "Unknown AREA '"+xs+"'";
            }
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
                		            
    		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = 0x80 | commandInfo << 3;
    		com.opcode = com.opcode | v;    				
    		
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}	


}
