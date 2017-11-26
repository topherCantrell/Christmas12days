package gx.g3.flow; 

import java.io.*;
import java.util.*;

// TO DO: Allow NTCA to span multiple lines

/*
 
 NTCA("This is\n a test\n\n") ; Null-Terminated-Character-Array
  Will generate strings with fcc and fcb and a 0 on the end.
 
 */

public class Data {

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
        
        // TOPHER fcc and such in XML?
        
        for(int x=code.size()-1;x>-0;x--) {
            Line a = (Line)code.get(x); 
            String s = a.rawNoComment;
            String ss = Line.stripWhiteSpace(a.rawNoComment);
            if(ss.indexOf("NTCA(\"")>=0) {
                int i = s.indexOf("NTCA(\"");
                a.parse(s.substring(0,i)+" ; "+s.substring(i));                                
                i = s.indexOf("\"");
                int j = s.length()-1;                
                while(s.charAt(j)!='"') --j;               
                s = s.substring(i+1,j);
                //System.out.println(">>"+s+"<<");
                int y=x+1;
                while(true) {
                    i = s.indexOf("\\n");
                    if(i<0) break;
                    if(i>0) {
                        ss = s.substring(0,i);                        
                        code.add(y,new Line(" "+BlendConfig.dataCharacterString+" \""+ss+"\""));
                        ++y;
                    }                    
                    code.add(y,new Line(" "+BlendConfig.dataByte+" 13"));
                    ++y;
                    s = s.substring(i+2);
                }
                if(s.length()!=0) {
                    code.add(y,new Line(" "+BlendConfig.dataCharacterString+" \""+s+"\""));
                }
                ++y;
                code.add(y,new Line(" "+BlendConfig.dataByte+" 0"));
            }
        }
        
        
    }
}
