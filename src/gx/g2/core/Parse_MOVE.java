package gx.g2.core;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;

public class Parse_MOVE extends Parser
{

	public Parse_MOVE(int commandInfo) 
	{
		super(commandInfo);		
	}
		
	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{		
		// a = b		
				
		String s = c.text.toUpperCase().trim();
				
		int i = s.indexOf("=");
		if(i<0) {
			return null;
		}
		String aa = s.substring(0,i).trim();
		String bb = s.substring(i+1).trim();			
		if(aa.length()==0 || bb.length()==0) {
			return "Invalid (empty) term";
		}		
				
		if(subs!=null) {        	
            String rep = subs.get(aa.toUpperCase());
            if(rep!=null) {
                aa = rep.toUpperCase();
            }
            rep = subs.get(bb.toUpperCase());
            if(rep!=null) {
                bb = rep.toUpperCase();
            }
		}
		
		Command_OP opA = new Command_OP();
		String er = Parse_Operand.parse(aa, opA,subs);
		if(er!=null) return er;
		
		Command_OP opB = new Command_OP();
		er = Parse_Operand.parse(bb, opB,subs);
		if(er!=null) return er;
				
		if(opA.type == Parse_OperandType.CONSTANT) {
			return "Invalid assignment to a constant.";
		}		
		
		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
		com.opcode = 0x80 | commandInfo << 3;
		
		// Order for all assignment is a,d (d = a)						
		
		com.operands.add(opB);		
		com.operands.add(opA);		
		
		// Add the command
		cluster.commands.add(com);
        return "";
	}

}
