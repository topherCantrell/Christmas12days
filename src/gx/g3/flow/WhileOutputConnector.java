package gx.g3.flow; 

import java.util.*;

/**
 * This OutputConnector's toString method implements the logic flow
 * for a while loop.
 */
public class WhileOutputConnector extends OutputConnector
{
    
     public WhileOutputConnector(String name, SpecialConstructInfo info)
     {
         super(name,info);
     }
         
     public void toAssembly(List<Line> outlines) { 
         
         // BEGIN label
         String label = name+"_CONTINUE";
         Line a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
         label = name+"_BEGIN";
         a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
         
         // LOGIC block
         outlines.add(new Line("$LOGIC"));
         
         // TRUE label
         label = name+"_TRUE";
         a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
         
         // TRUE block
         outlines.add(new Line("$TRUE")); 
         
         // JUMP to BEGIN
         String destination = name+"_BEGIN";
         String gg = BlendConfig.replaceAtTag("@PASS@",BlendConfig.gotoInstruction,destination);
         a = new Line(" "+gg);
         a.specialType = 3; a.specialData = destination;
         outlines.add(a); 
         
         // FALSE label
         label = name+"_FALSE";
         a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
         
         // END label
         label = name+"_END";
         a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
         label = name+"_BREAK";
         a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
                  
     }
    
}

