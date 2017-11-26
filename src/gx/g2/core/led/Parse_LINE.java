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

public class Parse_LINE extends Parser 
{

	public Parse_LINE(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("LINE") || ss.startsWith("LINE ")) {
			s=s.substring(4).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("X1",0);
            if(a==null) {
                return "Missing X1 operand.";
            }            
            String x1s = a.value;
            a = aList.removeArgument("Y1",1);
            if(a==null) {
                return "Missing Y1 operand.";
            }            
            String y1s = a.value;
            
            a = aList.removeArgument("X2",2);
            if(a==null) {
                return "Missing X2 operand.";
            }            
            String x2s = a.value;
            a = aList.removeArgument("Y2",3);
            if(a==null) {
                return "Missing Y2 operand.";
            }            
            String y2s = a.value;
            
            String cs = "1";
            a = aList.removeArgument("COLOR",4);
            if(a!=null) {
            	cs = a.value;
            }            
                        
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = commandInfo << 3;    		
    		
            String er = Parse_Operand.parse(x1s, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(y1s, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(x2s, com, subs);
    		if(er!=null) return er;
    		er = Parse_Operand.parse(y2s, com, subs);
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
