package gx.g2.core;

import java.util.List;

import gx.CodeLine;

public class Command_THREADGOTO extends Command
{
	
	String label;
	int commandInfo;	
	Command_OP operand;

	public Command_THREADGOTO(Integer commandInfo, Command_OP operand, CodeLine line, Cluster clus) {
		super(line, clus);	
		this.commandInfo = commandInfo;
		this.operand = operand;
	}

	@Override
	public int getSize() {
		return 2+operand.data.size();
	}

	@Override
	public String toSPIN(List<Cluster> clusters) {
		int ofs = cluster.findOffsetToLabel(label);            
        if(ofs<0) {
            return "# Label '"+label+"' not found.";
        }
        
        // Find offset of this command + getSize()    
        int nextPC = cluster.findOffsetToCommand(this) + getSize();
        ofs = ofs - nextPC;
        
        // Make sure it is in range
        if(ofs<-1024 || ofs>=2034) {
        	return "# Label '"+label+"' is too far away for a relative jump.";
        }       
        
        // 10 bit signed value
        if(ofs<0) {
        	ofs = ofs + 2048;
        }
        
        // Put fields together
        String ret = "' "+codeLine.text+"\r\n";        		
        ret = ret + "byte %1_"+Command.toBinaryString(commandInfo,4)+
          "_"+Command.toBinaryString(ofs>>8,3)+", %"+Command.toBinaryString(ofs&255,8);
        for(Integer i : operand.data) {
			 ret=ret+" ,%"+Command.toBinaryString(i,8);
		}               
        
        return ret;
	}

}
