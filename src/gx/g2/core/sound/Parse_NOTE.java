package gx.g2.core.sound;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;
import gx.g2.core.Command_MULTIOP;
import gx.g2.core.Command_OP;
import gx.g2.core.Parse_Operand;
import gx.g2.core.Parse_OperandType;

import java.util.Map;

public class Parse_NOTE extends Parser
{
	
	public Parse_NOTE(int commandInfo) {
		super(commandInfo);
	}

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{
		
		// NOTE VOICE=* NOTE=*
		
		StringBuffer err = new StringBuffer();
		ArgumentList aList = parseSyntax(c.text,"NOTE VOICE=* NOTE=*",subs,err);
		if(aList==null) return null;
		String errText = err.toString();
		if(errText.length()!=0) {
			return errText;
		}
		
		Argument a = aList.removeArgument("VOICE",0);                 
        String as = a.value;
        
        a = aList.removeArgument("NOTE",1);                 
        String vs = a.value;        
		
        Command_OP aa = new Command_OP();
        String er = Parse_Operand.parse(as, aa, subs);
    	if(er!=null) return er;
    		
    	Command_OP bb = new Command_OP();
        er = Parse_Operand.parse(vs, bb, subs);
    	if(er!=null) return er;
    		
    	// Add the command
    		    		
    	if(aa.type == Parse_OperandType.CONSTANT && 
    	   bb.type == Parse_OperandType.CONSTANT) 
    	{
    		Command_NOTESHORT com = new Command_NOTESHORT(commandInfo+1,c,cluster);
    		com.voice = aa.valueIfConstant;
    		com.note = bb.valueIfConstant;
    		cluster.commands.add(com);    			
    	} else {   
    		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
        	com.opcode = commandInfo << 4;    
        	com.operands.add(aa);
        	com.operands.add(bb);  
        	cluster.commands.add(com);
    	}   
    		
        return "";
        
	}

}
