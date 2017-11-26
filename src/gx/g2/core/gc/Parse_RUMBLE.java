package gx.g2.core.gc;

import java.util.Map;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;

public class Parse_RUMBLE extends Parser 
{

	public Parse_RUMBLE(int commandInfo) {
		super(commandInfo);
	}
	
	public void addDefines(Map<String, String> subs) 
	{
		subs.put("GC_RUMBLE1", "0x771");		
		subs.put("GC_RUMBLE2", "0x772");		
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) {
		String s = c.text;
		String ss = s.toUpperCase();

		if(ss.equals("RUMBLE") || ss.startsWith("RUMBLE ")) {
			s=s.substring(6).trim();
			ArgumentList aList = new ArgumentList(s,subs);
            Argument a = aList.removeArgument("PAD",0);
            if(a==null) {
                return "Missing PAD value.";
            }            
            if(!a.longValueOK || a.longValue<1 || a.longValue>2) {
            	return "Invalid PAD value '"+a.value+"'. Must be 1 or 2.";
            }
            int pad = (int)a.longValue;
            
            a = aList.removeArgument("VALUE",0);
            if(a==null) {
                return "Missing VALUE value.";
            }            
            if(!a.longValueOK || a.longValue<0 || a.longValue>1) {
            	return "Invalid VALUE value '"+a.value+"'. Must be 0 or 1.";
            }
            
            String rem = aList.reportUnremovedValues();
            if(rem.length()!=0) {
                return "Unexpected: '"+rem+"'";
            }
            
            s = "SYS3$0x771=";
            if(pad==2) {
            	s = "SYS3$0x772=";
            }
            
            CodeLine cc = new CodeLine(c.lineNumber, c.file, s+a.longValue);
            cluster.lines.add(cluster.lines.indexOf(c)+1,cc);	
            
            return "";
		}
		return null;
	}
	
}

