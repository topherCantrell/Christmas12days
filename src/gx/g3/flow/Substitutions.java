package gx.g3.flow; 

import java.io.*;
import java.util.*;

public class Substitutions {

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
    
    static void bracketSubstitution(Line a)
    {   
        
        // Lots of work here ... it is either REGISTER or NUMERIC
        // Perhaps a register list in the XML? For now we'll just
        // look for pure numbers.
        
        boolean numericIndex = false;
        int i = a.assem.indexOf('[');
        int j = a.assem.indexOf(']',i);
        String ss = a.assem.substring(i+1,j);
        try {
            Integer.parseInt(ss);
            numericIndex = true;            
        } catch (Exception e) {}
        
        String n;
        if(numericIndex) {
            n = a.assem.replace('[','+');
        } else {
            n = a.assem.replace('[',',');
        }
        n = n.replace(']',' ');  
        if(!n.equals(a.assem)) {
            a.changeAssem(n);
        }               
    }
    
    static String makeSubstitutionFit(String template, String code, String s)
    {
                
        if(s.length()==0) return null;
        
        String ss = s.toUpperCase();
        
        // The simple case of complete match
        if(template.equals(ss)) {
            return code;
        }
        
        if(template.endsWith("@")) {
            int i = template.lastIndexOf('@',template.length()-2);
            String b = template.substring(0,i);            
            if(ss.startsWith(b)) {                
                String arg = s.substring(i);
                arg = CodeParser.processMemoryOrImmediate(arg);  
                return BlendConfig.replaceAtTag("@OPERAND@",code,arg);                 
            }                      
        } else if(template.startsWith("@")) {
            int i = template.indexOf('@',1);
            String b = template.substring(i+1);            
            if(ss.endsWith(b)) {                                
                String arg = s.substring(0,s.length()-b.length());                    
                arg = CodeParser.processMemoryOrImmediate(arg); 
                return BlendConfig.replaceAtTag("@OPERAND@",code,arg); 
            }
        }            
        
        return null;
    }
    
     /**
     * This method handles the one-for-one direct substitutions (including
     * subroutine calls) that can be specified in the code.
     * @param code the code lines to process
     */
    public static void main(List<Line> code) throws Exception {
        // Subroutine call short-cut notation        
        for(int x=0;x<code.size();++x) {   
            Line a = (Line)code.get(x);           
            if(a.assem==null) continue;
            
            // Things like "VarA[X]" become "Var,X"
            //bracketSubstitution(a);
                        
            // Now the more complex substitutions
            String ss = Line.stripWhiteSpace(a.assem);
            for(int y=0;y<BlendConfig.processorInfo.subs.substituteKey.size();++y) {                
                String template = (String)BlendConfig.processorInfo.subs.substituteKey.get(y);
                String cd = (String)BlendConfig.processorInfo.subs.substituteCode.get(y);
                //System.out.println("::"+ss+":"+cd);
                String z = makeSubstitutionFit(template,cd,ss);
                if(z==null) continue;
                String gg = null;
                int ii = z.indexOf(";");
                if(ii>0) {
                    gg = z.substring(ii+1);
                    z = z.substring(0,ii);
                }
                a.changeAssem(z);
                while(gg!=null) {
                    ii = z.indexOf(";");
                    if(ii>0) {
                        gg = z.substring(ii+1);
                        z = z.substring(0,ii);
                    } else {
                        z = gg;
                        gg = null;
                    }
                    ++x;
                    code.add(x,new Line(" "+z));
                }
                break;
            }
        }
        
        for(int x=0;x<code.size();++x) {   
            Line a = (Line)code.get(x);           
            if(a.assem==null) continue;
            
            // Things like "VarA[X]" become "Var,X"
            if(a.assem.indexOf("[")>=0) {
                bracketSubstitution(a);
            }
        }
        
    }    

}
