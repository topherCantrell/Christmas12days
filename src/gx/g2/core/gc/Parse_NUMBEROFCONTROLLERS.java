package gx.g2.core.gc;

import java.util.Map;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;

public class Parse_NUMBEROFCONTROLLERS extends Parser 
{

	public Parse_NUMBEROFCONTROLLERS(int commandInfo) {
		super(commandInfo);
	}
	
	public void addDefines(Map<String, String> subs) 
	{
		subs.put("GC_PLAYER1", "0x000");		
		subs.put("GC_1_ANALOG_Y","0x000");  // Analog X axis
		
		subs.put("GC_PLAYER2", "0x008");
		subs.put("GC_2_ANALOG_Y","0x008");  // Analog X axis
		
		// TODO Masks and other
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) {
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("NUMBEROFCONTROLLERS") || ss.startsWith("NUMBEROFCONTROLLERS ")) {
			s=s.substring(19).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("PADS",0);
            if(a==null) {
                return "Missing PADS value.";
            }            
            if(!a.longValueOK || a.longValue<1 || a.longValue>2) {
            	return "Invalid PADS value '"+a.value+"'. Must be 1 or 2.";
            }
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            CodeLine cc = new CodeLine(c.lineNumber, c.file, "SYS3$0x770="+a.longValue);
            cluster.lines.add(cluster.lines.indexOf(c)+1,cc);	
            
            return "";
		}
		return null;
	}
	

}
