package gx.g2.core;
import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;

public class Parse_RETURN extends Parser
{
	
	public Parse_RETURN(int commandInfo) {
		super(commandInfo);		
	}
	
	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String,String> subs, Assemble compiler)
	{
		
		String s = c.text;
        String ss = s.toUpperCase();
        
        if(ss.equals("RETURN") || ss.startsWith("RETURN ")) {
            s = s.substring(6).trim();            
            ArgumentList aList = new ArgumentList(s,subs);            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }            
            Command_MULTIOP fc = new Command_MULTIOP(c,cluster);
            fc.opcode = 0x80 | commandInfo<<3;            
            cluster.commands.add(fc);
            return "";
        }
		
		return null;
	}

}
