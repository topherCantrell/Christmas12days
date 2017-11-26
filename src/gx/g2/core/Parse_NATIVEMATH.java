package gx.g2.core;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.Parser;

import java.util.Map;

public class Parse_NATIVEMATH extends Parser
{

	public Parse_NATIVEMATH(int commandInfo) 
	{
		super(commandInfo);		
	}
	
	static final String [] NATIVE_TABLE = {
	"&"     ,"011000",
	"ANDN"  ,"011001",
	"|"     ,"011010",	
	"^"     ,"011011",	
	"MAX"   ,"010011",
	"MIN"   ,"010010",
	"<<"    ,"001011",
	">>"    ,"001010",
	"+"     ,"100000",
	"-"     ,"100001"
	};

	@Override
	public String parse(CodeLine c, Cluster cluster, Map<String, String> subs, Assemble compiler) 
	{		
		// a = b
		// a = b # c
		// b # c
				
		String s = c.text.toUpperCase().trim();
		
		int fn=-1;
		for(int x=0;x<NATIVE_TABLE.length;x=x+2) {
			if(s.indexOf(NATIVE_TABLE[x])>0) {
				fn=x;
				break;
			}
		}
		
		if(fn<0) {
			// Not us
			return null;
		}
		
		// For operation-only we assign to a constant, which is do-nothing
		if(s.indexOf("=")<0) {
			s="0="+s;
		}		
		
		String aa = "";
		String bb = "";
		String cc = "";
		
		int i = s.indexOf("=");
		int j = s.indexOf(NATIVE_TABLE[fn]);
		if(j<=i) {
			return "Operation '"+NATIVE_TABLE[fn]+"' must come after the '='";
		}
		aa = s.substring(0,i).trim();
		bb = s.substring(i+1,j).trim();
		cc = s.substring(j+NATIVE_TABLE[fn].length()).trim();
		if(aa.length()==0 || bb.length()==0 || cc.length()==0) {
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
            rep = subs.get(cc.toUpperCase());
            if(rep!=null) {
                cc = rep.toUpperCase();
            }
		}
		
		Command_OP opA = new Command_OP();
		String er = Parse_Operand.parse(aa, opA,subs);
		if(er!=null) return er;
		
		Command_OP opB = new Command_OP();
		er = Parse_Operand.parse(bb, opB,subs);
		if(er!=null) return er;
		
		Command_OP opC = new Command_OP();		
		er = Parse_Operand.parse(cc, opC,subs);
		if(er!=null) return er;		
		
		Command_MULTIOP com = new Command_MULTIOP(c,cluster);
		com.opcode = 0x80 | commandInfo << 3;
		
		// The bits of the instruction
		Command_OP opbits = new Command_OP();
		
		Integer ii = new Integer(Integer.parseInt(NATIVE_TABLE[fn+1],2));		
		opbits.data.add(ii);
		com.operands.add(opbits);
		
		// Order for all math is b,a,d (d = a op b)	
						
		com.operands.add(opC);				
		com.operands.add(opB);		
		com.operands.add(opA);		
		
		// Add the command
		cluster.commands.add(com);
        return "";
	}

}
