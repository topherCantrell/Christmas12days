package gx.g3.flow; 

import java.util.*;

/**
 * This class houses the information for a special construct to expand.
 */
public class SpecialConstructInfo
{
    int type;           // 1=if/else 2=while 3=do-while 4=for
    String expression;  // The expression in the if/while parenthesis 
    String baseName;    // Unique baseName for this construct
    
    String initLine;      // init part of for loop
    String iteratorLine;  // iterator part of for loop
        
    List<Object> normalBlock = new ArrayList<Object>();    // The lines of the normal block
    List<Object> elseBlock = new ArrayList<Object>();      // The lines of the else block (if any)    
   
}
