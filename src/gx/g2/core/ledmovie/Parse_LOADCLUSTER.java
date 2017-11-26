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

public class Parse_LOADCLUSTER extends Parser
{

	public Parse_LOADCLUSTER(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		
		// LoadCluster page=* cluster=* 
		
		StringBuffer err = new StringBuffer();
		ArgumentList aList = parseSyntax(c.text,"LOADCLUSTER PAGE=* CLUSTER=*",subs,err);
		if(aList==null) return null;
		String errText = err.toString();
		if(errText.length()!=0) {
			return errText;
		}
		
		Argument a = aList.getArgument("PAGE",0);
		String pageS = a.value;
		
		a = aList.getArgument("CLUSTER",1);
		String clusterS = a.value;
		
		// Add the command
		
		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
		com.opcode = commandInfo;     		
		
        String er = Parse_Operand.parse(pageS, com, subs);
		if(er!=null) return er;
		
		er = Parse_Operand.parse(clusterS, com, subs);
		if(er!=null) return er; 		
		
		cluster.commands.add(com);
        return "";
		
	}

}