package gx.g2.core;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;


public class Parse_GOTO extends Parser
{	
	
	Parse_FLOWFunction type;
		
	public Parse_GOTO(Parse_FLOWFunction type, int commandInfo)
	{		
		super(commandInfo);
		this.type = type;		
	}	

	public String parse(CodeLine c, Cluster cluster, Map<String,String> subs, Assemble compiler) 
	{
		
		String c1 = type.name();
		String c2 = c1+" ";
		
		String s = c.text;
        String ss = s.toUpperCase();
        
        if(ss.equals(c1) || ss.startsWith(c2)) {
            s = s.substring(c1.length()).trim();  
            Command_GOTO fc = new Command_GOTO(commandInfo,c,cluster);
            ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("LABEL",0);
            if(a==null) {
                return "Missing LABEL value.";
            }
            fc.label = a.value;            
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
