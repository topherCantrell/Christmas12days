package gx.g2.core.led;

import java.util.Map;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;
import gx.g2.core.Parse_Operand;

public class Parse_SEVENSEGMENT extends Parser 
{

	public Parse_SEVENSEGMENT(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) {
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("SEVENSEGMENT") || ss.startsWith("SEVENSEGMENT ")) {
			s=s.substring(12).trim();
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
            
            a = aList.removeArgument("WIDTH",2);
            if(a==null) {
                return "Missing WIDTH operand.";
            }            
            String widths = a.value;
            a = aList.removeArgument("HEIGHT",3);
            if(a==null) {
                return "Missing HEIGHT operand.";
            }            
            String heights = a.value;
            
            a = aList.removeArgument("PATTERN",4);
            if(a==null) {
                return "Missing PATTERN operand.";
            }            
            String patterns = a.value;          
                        
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = (commandInfo+1) << 3;    		
    		
            String er = Parse_Operand.parse(xs, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(ys, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(widths, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(heights, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(patterns, com, subs);
    		if(er!=null) return er;
            
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}

}
