package gx.g3.flow; 

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class Blend {
		
    public static void main(String [] args) throws Exception
    {
    	
    	if(args.length!=3) {
    		System.out.println("args: infile outfile processor");
    		return;
    	}
           	
        List<Line> codeOrg = Line.loadLines(args[0]); 
        
        main(codeOrg,args[2]);    
        
        // Store the processed code in the output file
        OutputStream os = new FileOutputStream(args[1]);
        PrintStream ps = new PrintStream(os);
        Line.linesToStream(codeOrg,ps);               
        ps.flush();
        ps.close();
    }
    
    public static List<Line> main(String rootfile, String processor) throws Exception
	{
    	List<Line> codeOrg = Line.loadLines(rootfile);         
        main(codeOrg,processor);        
        return codeOrg;		
	}
    
    public static void main(List<Line> code, String processor) throws Exception
    {            
    	
    	BlendConfig.initFlowConfig(null);
        boolean err = BlendConfig.reInit(processor);
        if(!err) {
        	System.out.println("Unknown processor '"+processor+"'");
        	return;
        }
    	
    	List<String> modules = BlendConfig.processorInfo.modules;
    	
    	// The XML for each processor contains the list of modules to
    	// run for that processor.
    	
    	for(String s : modules) {
    		Class<?> c = BlendConfig.processorInfo.getClass().getClassLoader().loadClass("gx.g3.flow."+s);
    		Method m = c.getMethod("main",List.class);
    		m.invoke(null,code);
    	}
    	
    }
}
