package gx.g2.core;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;


public class Parse_PAUSE extends Parser
{
			
	public Parse_PAUSE(int commandInfo) {
		super(commandInfo);
	}	

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("PAUSE") || ss.startsWith("PAUSE ")) {
			
			// TODO ability to specify time constants like "100ms"
			
			s=s.substring(5).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("DELAY",0);
            if(a==null) {
                return "Missing DELAY operand.";
            }            
            String xs = a.value;
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
                        
            Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = 0x80 | commandInfo << 3;
    		  		
            String er = Parse_Operand.parse(xs, com, subs);            
    		if(er!=null) return er;   
    		
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}

}
