package gx.g2.core.ledmovie;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;
import gx.g2.core.Parse_Operand;

import java.util.Map;

public class Parse_RENDERFRAME extends Parser
{

	public Parse_RENDERFRAME(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		
		// LoadCluster page=* frame=* {WAIT}
		
		StringBuffer err = new StringBuffer();
		ArgumentList aList = parseSyntax(c.text,"RENDERFRAME PAGE=* FRAME=*",subs,err);
		if(aList==null) return null;
		String errText = err.toString();
		if(errText.length()!=0) {
			return errText;
		}
		
		Argument a = aList.getArgument("PAGE",0);
		String pageS = a.value;
		
		a = aList.getArgument("FRAME",1);
		String frameS = a.value;
		
		// Add the command
		
		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
		com.opcode = commandInfo;     		
		
        String er = Parse_Operand.parse(pageS, com, subs);
		if(er!=null) return er;
		
		er = Parse_Operand.parse(frameS, com, subs);
		if(er!=null) return er; 		
		
		cluster.commands.add(com);
        return "";
	}

}
