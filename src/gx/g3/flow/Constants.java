package gx.g3.flow; 

import java.util.*;
import java.io.*;

// 0xNNNN   HEX
// 0bN      BINARY
// 0oN      OCTAL

public class Constants
{
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
    
    public static String fillOutConstantHex(String s)
    {        
        String ss = s.toUpperCase();
        int i = ss.indexOf("0X");
        int j = i+2;
        while(true) {
            if(j==ss.length()) break;
            char c = ss.charAt(j++);
            if(c>='0' && c<='9') continue;
            if(c>='A' && c<='F') continue;    
            --j;
            break;
        }        
        int val = Integer.parseInt(ss.substring(i+2,j), 16); 
        s = s.substring(0,i)+val+s.substring(j);
        return s;        
    }
    public static String fillOutConstantBinary(String s)
    {        
        String ss = s.toUpperCase();
        int i = ss.indexOf("0B"); 
        int j = i+2;
        while(true) {
            if(j==ss.length()) break;
            char c = ss.charAt(j++);
            if(c>='0' && c<='7') continue;     
            --j;
            break;
        }
        int val = Integer.parseInt(ss.substring(i+2,j),8);
        s = s.substring(0,i)+val+s.substring(j);
        return s;        
    }
    public static String fillOutConstantOctal(String s)
    {        
        String ss = s.toUpperCase();
        int i = ss.indexOf("0O");
        int j = i+2;
        while(true) {
            if(j==ss.length()) break;
            char c = ss.charAt(j++);
            if(c>='0' && c<='1') continue;   
            --j;
            break;
        }
        int val = Integer.parseInt(ss.substring(i+2,j),2);
        s = s.substring(0,i)+val+s.substring(j);
        return s;        
    }
    
    public static void main(List<Line> code) throws Exception
    {
        
        // First pass ... find the StructureDefinitions
        for(int x=0;x<code.size();++x) 
        {
            Line a = (Line)code.get(x);
            String s = a.rawNoComment; 
            
            boolean changes = true;
            boolean madeChanges = false;
            while(changes) {
                changes = false;
                if(s.indexOf("0x")>0) {
                    s = fillOutConstantHex(s);
                    changes = true;                    
                }
                if(s.indexOf("0b")>0) {
                    s = fillOutConstantBinary(s);
                    changes = true;                    
                }
                if(s.indexOf("0o")>0) {
                    s = fillOutConstantOctal(s);
                    changes = true;
                }
                if(changes) madeChanges = true;
            }                                    
            
            if(madeChanges) {
                a.parse(s);
            }
           
        }
        
    }
    
}
