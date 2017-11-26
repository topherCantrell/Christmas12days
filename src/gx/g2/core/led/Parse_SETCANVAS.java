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

public class Parse_SETCANVAS extends Parser 
{

	public Parse_SETCANVAS(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("SETCANVAS") || ss.startsWith("SETCANVAS ")) {
			s=s.substring(9).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("ADDRESS",0);
            if(a==null) {
                return "Missing ADDRESS operand.";
            }            
            String xs = a.value;
            int cr = 0;
            a = aList.removeArgument("FILLWITH",1);
            if(a!=null) {
            	if(!a.longValueOK || a.longValue<0 || a.longValue>1) {
            		return "Invalid FILL value. Must be 0 or 1.";
            	}    
            	cr = (int)(a.longValue<<2) | 2;            	
            }      
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
                        
            Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    		com.opcode = cr;
    		
            String er = Parse_Operand.parse(xs, com, subs);
    		if(er!=null) return er;
    		
    		// Add the command
    		cluster.commands.add(com);
            return "";
        }
		
		return null;
	}

}
