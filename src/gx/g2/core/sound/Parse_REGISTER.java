package gx.g2.core.sound;

import java.util.Map;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;
import gx.g2.core.Parse_Operand;

public class Parse_REGISTER extends Parser
{
	
	public Parse_REGISTER(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		
		// REGISTER ADDRESS=* VALUE=*
		
		StringBuffer err = new StringBuffer();
		ArgumentList aList = parseSyntax(c.text,"REGISTER ADDRESS=* VALUE=*",subs,err);
		if(aList==null) return null;
		String errText = err.toString();
		if(errText.length()!=0) {
			return errText;
		}
		
		Argument a = aList.removeArgument("ADDRESS",0);
        String as = a.value;
        
        a = aList.removeArgument("VALUE",1);               
        String vs = a.value;  
            
        Command_MULTIOP com = new Command_MULTIOP(c,cluster);
    	com.opcode = commandInfo << 4;    		
    		
        String er = Parse_Operand.parse(as, com, subs);
    	if(er!=null) return er;
    	
    	er = Parse_Operand.parse(vs, com, subs);
    	if(er!=null) return er;    		
            
    	// Add the command
    	cluster.commands.add(com);
        return "";
       
	}

}
