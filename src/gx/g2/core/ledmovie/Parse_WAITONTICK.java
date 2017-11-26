package gx.g2.core.ledmovie;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;

import java.util.Map;

public class Parse_WAITONTICK extends Parser
{

	public Parse_WAITONTICK(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		
		// WaitOnTick
		
		StringBuffer err = new StringBuffer();
		ArgumentList aList = parseSyntax(c.text,"WAITONTICK",subs,err);
		if(aList==null) return null;
		String errText = err.toString();
		if(errText.length()!=0) {
			return errText;
		}
		
		// Add the command
		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
		com.opcode = commandInfo;		
		cluster.commands.add(com);
        return "";
        
	}

}
