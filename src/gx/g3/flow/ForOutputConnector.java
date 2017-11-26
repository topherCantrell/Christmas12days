package gx.g3.flow; 

import java.util.*;

/**
 * This OutputConnector's toString method implements the logic flow
 * for a for loop.
 */
public class ForOutputConnector extends OutputConnector
{
    
     public ForOutputConnector(String name, SpecialConstructInfo info)
     {
         super(name,info);
     }
         
     public void toAssembly(List<Line> outlines) {  
    	 
    	 // init line as-is    	 
    	 Line fa = new Line(info.initLine);
    	 outlines.add(fa);    	          
    	
         // BEGIN label
         String label = name+"_BEGIN";
         Line a = new Line(label+":");            
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
         
         // CONTINUE LABEL
         label = name+"_CONTINUE";
         a = new Line(label+":");            
         outlines.add(a);
         a.specialType = 4; a.specialData = label;
         // iterator line as-is 
         a = new Line(info.iteratorLine);
         outlines.add(a);         
          
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

