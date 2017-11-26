package gx.g2.core;

import java.util.ArrayList;
import java.util.List;

import gx.CodeLine;

public class Command_MULTIOP extends Command  
{
	
	public int opcode;
	public List<Command_OP> operands = new ArrayList<Command_OP>();
	
	public Command_MULTIOP(CodeLine line, Cluster clus) 
    {
    	super(line,clus);
    }

	@Override
	public int getSize() {
		int oc = 0;
		for(Command_OP o : operands) {
			oc = oc + o.data.size();
		}
		return oc+1;
	}

	@Override
	public String toSPIN(List<Cluster> clusters) {
		 String ret = "' "+codeLine.text+"\r\n";
		 
		 int fb = opcode >> 7;
		 opcode = opcode & 0x7F;
		 if(fb==1) {
			 ret = ret + "byte %1_"+Command.toBinaryString(opcode,7);
		 } else {
			 ret = ret + "byte   %0_"+Command.toBinaryString(opcode,7);
		 }
		 for(Command_OP o : operands) {
			 if(o.label!=null && o.label.length()>0) {
				 int j = cluster.findOffsetToLabel(o.label);
				 if(j<0) {
					 return "Could not find label '"+o.label+"'";
				 } 				 
				 // Assume 3-byte constant
				 o.data.set(1,j&255);
				 o.data.set(2,j>>8);
			 }
			 for(Integer i : o.data) {
				 ret=ret+" ,%"+Command.toBinaryString(i,8);
			 }
		 }
		 return ret;
	}

}
