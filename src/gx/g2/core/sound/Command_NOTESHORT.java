package gx.g2.core.sound;

import gx.CodeLine;
import gx.g2.core.Cluster;
import gx.g2.core.Command;

import java.util.List;

public class Command_NOTESHORT extends Command
{		
	int commandInfo;
	int voice;
	int note;
		
	public Command_NOTESHORT(Integer commandInfo, CodeLine line, Cluster clus) {
		super(line, clus);
		this.commandInfo = commandInfo;
	}
	
	@Override
	public int getSize() {
		return 2;
	}

	@Override
	public String toSPIN(List<Cluster> clusters) {
				
		//'' NOTE voice, note
		//'' 0_ccc_vvvv__nnnnnnnn		
		        
		// Put fields together
        String ret = "' "+codeLine.text+"\r\n";        		
        ret = ret + "byte %0_"+Command.toBinaryString(commandInfo,3)+
          "_"+Command.toBinaryString(voice,4)+", %"+Command.toBinaryString(note,8);        
        
        return ret;
        
	}
}
