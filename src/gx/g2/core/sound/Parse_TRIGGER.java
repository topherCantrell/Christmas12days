package gx.g2.core.sound;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;

import java.util.Map;

public class Parse_TRIGGER extends Parser 
{

	public Parse_TRIGGER(int commandInfo) {
		super(commandInfo);		
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs,
			Assemble compiler) 
	{
		StringBuffer err = new StringBuffer();
		ArgumentList aList = parseSyntax(c.text,"TRIGGER",subs,err);
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
