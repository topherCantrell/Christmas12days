package gx.g3.flow; 

import java.io.*;
import java.util.*;

public class Number {

    public static void main(String [] args) throws Exception
    {
        Reader r = new FileReader(args[0]);
        BufferedReader br = new BufferedReader(r);        
        List<Line> codeOrg = new ArrayList<Line>();        
        while(true) {
            String g = br.readLine();
            if(g==null) break;     
            Line aa = new Line(g);            
            codeOrg.add(aa);
        }      
        br.close();
        
        BlendConfig.initFlowConfig(null);
        
        //Processor.main(codeOrg);
        main(codeOrg);    
        
        // Store the processed code in the output file
        OutputStream os = new FileOutputStream(args[1]);
        PrintStream ps = new PrintStream(os);
        Line.linesToStream(codeOrg,ps);               
        ps.flush();
        ps.close();
    }
    
    public static void main(List<Line> code) throws Exception
    {
        int lineNumber = 1;  
        for(int x=0;x<code.size();++x) {
            Line a = (Line)code.get(x);
            if(a.label!=null || a.assem!=null) { 
                a.prependComment("OLine="+lineNumber);                
            }
            ++lineNumber;
        }        
    }
}

