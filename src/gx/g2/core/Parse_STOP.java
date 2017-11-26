package gx.g2.core;

import java.util.Map;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

public class Parse_STOP extends Parser 
{

	public Parse_STOP(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		String s = c.text;
        String ss = s.toUpperCase();
        
        if(ss.equals("STOP") || ss.startsWith("STOP ")) {
            s = s.substring(4).trim();            
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
