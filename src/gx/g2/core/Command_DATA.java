package gx.g2.core;

import java.util.ArrayList;
import java.util.List;

import gx.CodeLine;

public class Command_DATA extends Command 
{

	public List<Integer> data = new ArrayList<Integer>();
	
	public Command_DATA(CodeLine line, Cluster clus) {
		super(line, clus);		
	}

	@Override
	public int getSize() {
		return data.size();
	}

	@Override
	public String toSPIN(List<Cluster> clusters) {
		 String ret = "' "+codeLine.text+"\r\n";
		 if(data.size()>0) {
			ret = ret + "byte ";
			for(int x=0;x<data.size();++x) {
				Integer i = data.get(x);
				ret=ret+" $"+Integer.toString(i,16).toUpperCase();
				if(x!=(data.size()-1)) {
					ret = ret+",";
				}
			}     	
		 }
	     return ret;
	}

}
