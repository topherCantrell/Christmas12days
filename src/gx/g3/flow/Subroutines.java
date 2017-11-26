package gx.g3.flow; 

import java.io.*;
import java.util.*;

/*

 Subroutines are detected by lines of the form "NNNN(){" after comments
 and whitespace are stripped. Subroutines end with a closing "}" after
 all levels of brackets are counted.
 
 The start line is replaced with "NNNN: ; # --SubroutineContextBegins--"
 where '#' is the original code line.
 
 The close line is replaced with "  * ; # --SubroutineContextEnds--"
 where '*' is the processor's return statement and '#' is the original
 code line.
 
 The remainder of the code is searched for "NNNN()" after comments and
 whitespace are stripped. These lines are replaced with "  * ; #"
 where '*' is the processor's CALL syntax to NNNN and '#' is the
 original code line.
 
 Lines of "return" after comments and whitespace are stripped are replaced
 with "  * ; #" where '*' is the processor's return statement and '#'
 is the original code line.
 
 Other modules may key off of --SubroutineContext*-- to know when routines
 start and end.
 
 */

public class Subroutines {

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
        
        // First find beginning and end of all subroutines
        for(int x=0;x<code.size();++x) {
            Line a = (Line)code.get(x);            
            String s = Line.stripWhiteSpace(a.rawNoComment);
            if(!s.endsWith("(){")) continue;            
            String lab = s.substring(0,s.length()-3);
            String com = a.comment;
            if(com==null) com="";
            a.parse(lab+": ; --SubroutineContextBegins--");            
            int levcnt = 1;            
            ++x;
            while(true) {
                a = (Line)code.get(x); 
                s = Line.stripWhiteSpace(a.rawNoComment);                
                for(int z=0;z<s.length();++z) {
                    if(s.charAt(z)=='{') {
                        ++levcnt;
                    } else if(s.charAt(z)=='}') {
                        --levcnt;
                    }
                }                
                if(levcnt<=0) break;
                ++x;
            }       
            a = (Line)code.get(x);       
            a.parse(" "+BlendConfig.returnInstruction+" ; --SubroutineContextEnds--");            
        }
        
        for(int x=0;x<code.size();++x) {  
            Line a = (Line)code.get(x);            
            if(a.assem==null) continue;
            String s = Line.stripWhiteSpace(a.assem);
            if(s.endsWith("()")) {                
                String p = BlendConfig.replaceAtTag("@PASS@",BlendConfig.callInstruction,
                    s.substring(0,s.length()-2));
                a.changeAssem(p);                         
            } else if(s.equals("return")) {
                String p = BlendConfig.returnInstruction;  
                a.changeAssem(p);                               
            }
        }
    }

}