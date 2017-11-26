package gx.g2.core;
import gx.CodeLine;

import java.util.List;


public class Command_GOTOCOND extends Command
{	
	String label;
	int commandInfo;
	int cond;
		
	public Command_GOTOCOND(Integer commandInfo, CodeLine line, Cluster clus) {
		super(line, clus);
		this.commandInfo = commandInfo;
	}
	
	@Override
	public int getSize() {
		return 3;
	}

	@Override
	public String toSPIN(List<Cluster> clusters) {
				
		//'' GOTO rel, cond
		//'' 1_ccccc_rr__rrrrrrrr  0000_nnnn
		
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
          "_"+Command.toBinaryString(ofs>>8,3)+", %"+Command.toBinaryString(ofs&255,8)+
          ", %0000_"+Command.toBinaryString(cond,4);        
        
        return ret;
        
	}

}
