package gx.g3.flow; 

import java.util.*;

/**
 * The "toString(List)" method of this Connector generates the assembler
 * for the condition with the given polarity.
 */
public class InputConnector extends Connector {
    
    String [] assemblyFlavors;
        
    public InputConnector(String name, String [] assemblyFlavors) {        
        super(name);        
        this.assemblyFlavors = assemblyFlavors;        
    }
    
    public Connector getTrueConnector(Connector asker) {
        return output.getTrueConnector(this);
    }
    
    public Connector getFalseConnector(Connector asker) {
        return output.getFalseConnector(this);
    }
    
    public Connector getProcessConnector(Connector asker) {        
        // The other types of connectors will pass-through until they get here.
        return this;
    }
    
    public void toAssembly(List<Line> out) { 
        
        // The assembly should be resolved at this point except for
        // @PASS@ and @FAIL@        
        
        String labTrue = getTrueConnector(this).getName(true);
        String labFalse = getFalseConnector(this).getName(false);
        
        // Other nodes may have more polarity than we do, but we won't complain.
        int p = polarity;        
        if(p>=assemblyFlavors.length) p = assemblyFlavors.length-1;
        
        String g = assemblyFlavors[p];        
        
        // LABEL         
        Line a = new Line(name+":");            
        out.add(a);
        a.specialType = 4; a.specialData = name; 
        
        if(BlendConfig.verbose) {
            out.add(new Line("; polarity "+p));
        }
        
        // ASSEMBLY        
        while(true) {
            
            int i = g.indexOf(";");            
            if(i<0) {
                i=g.length();                
            }
                        
            String aa = g.substring(0,i);
            
            boolean jump = false;
            String lab = null;
            if(aa.indexOf("@PASS@")>0) {                 
                lab = labTrue;
                if(aa.equals(BlendConfig.gotoInstruction)) {
                    jump = true;
                }
                aa = BlendConfig.replaceAtTag("@PASS@",aa,labTrue);                
            } else if(aa.indexOf("@FAIL@")>0) {                
                 lab = labFalse;
                 if(aa.equals(BlendConfig.gotoInstruction)) {
                    jump = true;
                }
                aa = BlendConfig.replaceAtTag("@FAIL@",aa,labFalse);
            }
            
            Line b = new Line(" "+aa);
            if(lab!=null) {
                if(jump) {
                    b.specialType = 3;
                } else {
                    b.specialType = 5;
                }
                b.specialData = lab;
            }
            out.add(b);

            if(i==g.length()) break;
            g = g.substring(i+1);
        }
                
        
    }
    
}
