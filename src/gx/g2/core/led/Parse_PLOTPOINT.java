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

public class Parse_PLOTPOINT extends Parser
{

	public Parse_PLOTPOINT(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("PLOTPOINT") || ss.startsWith("PLOTPOINT ")) {
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
            a = aList.removeArgument("COLOR",2);
            if(a==null) {
                return "Missing COLOR operand.";
            }            
            String cs = a.value;                        
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = commandInfo << 3;    		
    		
            String er = Parse_Operand.parse(xs, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(ys, com, subs);
    		if(er!=null) return er;	
    		er = Parse_Operand.parse(cs, com, subs);
    		if(er!=null) return er;	
    		
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}

}
