package gx.g3.flow; 

import java.util.*;

/**
 * This OutputConnector's toString method implements the logic flow
 * for an if-else block (with two polarities).
 */
public class IfOutputConnector extends OutputConnector
{
    
    boolean includeElse = false;
    
    public IfOutputConnector(String name, SpecialConstructInfo info)
    {
        super(name,info);
        if(info.elseBlock!=null) includeElse=true;
    }
    
    public void toAssembly(List<Line> outlines) {           
        int p = polarity; 
        if(p>1) p=1;
        
        if(BlendConfig.verbose) {
            outlines.add(new Line("; polarity "+p));
        }
        
        if(p==0) {        
            
            // LOGIC
            outlines.add(new Line("$LOGIC"));
            
            // TRUE label
            String label = name+"_TRUE";
            Line a = new Line(label+":");            
            outlines.add(a);
            a.specialType = 4; a.specialData = label;
            
            // TRUE block
            outlines.add(new Line("$TRUE"));
            
            // Jump to END
            String destination = name+"_END";
            String gg = BlendConfig.replaceAtTag("@PASS@",BlendConfig.gotoInstruction,destination);
            a = new Line(" "+gg);
            a.specialType = 3; a.specialData = destination;
            outlines.add(a);            
            
            // FALSE label
            label = name+"_FALSE";
            a = new Line(label+":");            
            outlines.add(a);
            a.specialType = 4; a.specialData = label;            
            
            // FALSE block (if any)
            if(includeElse) {
                outlines.add(new Line("$FALSE"));            
            }
            
            // END label
            label = name+"_END";
            a = new Line(label+":");            
            outlines.add(a);
            a.specialType = 4; a.specialData = label;            
            
        } else {            
            outlines.add(new Line("$LOGIC"));
            
            // FALSE label
            String label = name+"_FALSE";
            Line a = new Line(label+":");            
            outlines.add(a);
            a.specialType = 4; a.specialData = label;    
                        
            // FALSE block (if any)
            if(includeElse) {
                outlines.add(new Line("$FALSE"));            
            }
            
            // Jump to END
            String destination = name+"_END";
            String gg = BlendConfig.replaceAtTag("@PASS@",BlendConfig.gotoInstruction,destination);
            a = new Line(" "+gg);
            a.specialType = 3; a.specialData = destination;
            outlines.add(a); 
            
            // TRUE label
            label = name+"_TRUE";
            a = new Line(label+":");            
            outlines.add(a);
            a.specialType = 4; a.specialData = label;
            
            // TRUE block
            outlines.add(new Line("$TRUE"));
            
            // END label
            label = name+"_END";
            a = new Line(label+":");            
            outlines.add(a);
            a.specialType = 4; a.specialData = label;  
        }        
    }
    
}

