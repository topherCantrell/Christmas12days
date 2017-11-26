package gx.g3.flow; 

import java.io.*;
import java.util.*;

/**
 * This class contains static methods to find C-like expressions that control
 * program flow (if, else, do, while) and replaces them with the correct
 * assembler instructions.
 */
public class Flow {    
    
    
    static void dump(List<Object> lines)
    { 
        for(int x=0;x<lines.size();++x) {
            if(lines.get(x) instanceof SpecialConstructInfo) {
                System.out.println("** SPECIAL CONSTRUCT **");
            } else {
                Line a = (Line)lines.get(x);
                System.out.println("#"+a.raw+"#"+a.specialType+"#"+a.specialData+"#");
            }
        }
    }
    
    
    static int countGoodLines(List<Object> lines)
    {
        // Count all non-comment lines
        int ret = 0;
        for(int x=0;x<lines.size();++x) {
            if(lines.get(x) instanceof SpecialConstructInfo) {
                ++ret;
                continue;
            }
            Line a = (Line)lines.get(x);
            if(a.label!=null || a.assem!=null) {
                ++ret;
            }
        }
        return ret;        
    }
    
    /**
     * This method cleans the FLOW-generated assembly as follows:
     * 1) Jumps to the very next line are removed
     * The following are included only if "doFull"
     * 2) Branches to jumps are unchained
     * 3) Jumps to returns are changed to returns     
     * 4) Labels that are not referenced are removed 
     * 5) Redundant labels (two or more at the same place) are collapsed
     * 6) Un-labeled returns following jumps are removed    
     * @param lines the assmebly lines
     * @return the number of "clean" lines (in verbose mode we just comment them out)
     */
     static int clean(List lines, boolean doFull) 
     {
             	
         // Uncomment this if you want to see the list before cleaning
         //if(true) return lines.size();         
         
         // BREAKs and CONTINUEs are a special problem. They are almost always
         // nested in deeper constructs that aren't expanded yet, and thus
         // we can't remove unused labels until they are. The "doAll" flag
         // will tell us when we are doing a final sweep through the entire
         // code when it is safe to remove unsused labels.
         
         // For optimal cleaning, we should try all polarities for all nested
         // constructs at the same time. Maybe in a future enhancement ...
                  
         
         
         
         
         // TOPHER
         // My assumption was that the only cleans to be done in the early
         // sweeps was jumps to the next line. But I am finding that
         // redundant labels are likely too. Time to fix the logic so
         // that all cleans are performed on every sweep ... but to leave
         // _BEGIN and _END labels (break and continue) until last sweep.         
         
         
         
         // Remove branches to the very next line of good assembly
         // This might be a problem for some processors where compares
         // and branches are on the same line ... only because the compares
         // will get removed and thus the flags won't be correct. DON'T code
         // complex compares with flags rippling through the sequence.
                  
         for(int x=lines.size()-2;x>=0;--x) {             
             if(lines.get(x) instanceof SpecialConstructInfo) continue;
             Line a = (Line)lines.get(x);             
             
             // Get the branch destination (if a branch)
             String targetLabel = getFlowJump(a,false);
             if(targetLabel==null) continue; // Not a branch
             
             // Find the next line (if there is a livenext line)
             Line b = null;
             int nl = x;             
             while(true) {
                 ++nl;                 
                 if(nl>=lines.size() || lines.get(nl) instanceof SpecialConstructInfo) {
                     // Who knows what this will expand into ...
                     b = null;
                     break;
                 }
                 b = (Line)lines.get(nl);
                 if(b.assem != null || b.label!=null) break;
             }
             
             if(b==null) continue;  
             
             // If the target label is the very next line, kill the branch
             if(b.label!=null && b.label.equals(targetLabel)) {
                 //System.out.println("CLEANED JUMP TO NEXT LINE");                                 
                 if(BlendConfig.verbose) {         
                     a.appendComment("CLEAN-JumpToNextLine- ");
                     a.commentOutWholeLine();                     
                 } else {
                     lines.remove(x);
                 }
             }             
         }
                           
         // Remove unreferenced labels
         
         for(int x=0;x<lines.size();++x) {         
             if(lines.get(x) instanceof SpecialConstructInfo) continue;
             Line a = (Line)lines.get(x);             
             
             // Make sure this is a FLOW label line
             if(a.specialType!=4) continue;
             
             if(!doFull) {
                 String sd = (String)a.specialData;
                 // 'break' and 'continue' need these and may be nested
                 // in a not-yet-expanded SpecialConstructInfo
                 if(sd.endsWith("_BEGIN") || sd.endsWith("_END") || sd.endsWith("_BREAK") || sd.endsWith("_CONTINUE")) {
                     continue;
                 }
             }
             
             // Search the code for a FLOW branch to the FLOW label
             boolean found = false;
             for(int y=0;y<lines.size();++y) {
                 if(lines.get(y) instanceof SpecialConstructInfo) continue;
                 Line b = (Line)lines.get(y);
                 String targetLabel = getFlowJump(b,false);
                 if(targetLabel == null) continue;
                 if(targetLabel.equals(a.label)) {
                     found = true;
                     break;
                 }                 
             }
             
             if(!found) {                 
                 //System.out.println("CLEANED UNREFERENCED LABEL");
                 a.specialType = 0; a.specialData = null;
                 if(BlendConfig.verbose) {                     
                     a.appendComment("CLEAN-UnreferencedLabel- ");
                     a.commentOutWholeLine();
                 } else {
                     lines.remove(x);
                     --x; // We deleted a line ... back up over the gap
                 }                 
             }
             
         }
         
         // Remove redundant labels
         
         for(int x=lines.size()-2;x>=0;--x) {
             if(lines.get(x) instanceof SpecialConstructInfo) continue;
             Line a = (Line)lines.get(x);
             if(a.specialType!=4) continue;             
             
             // Find the next line (if there is a live next line)
             Line b = null;
             int nl = x;             
             while(true) {
                 ++nl;          
                 if(lines.get(nl) instanceof SpecialConstructInfo) {
                     b = null;
                     break;
                 }
                 b = (Line)lines.get(nl);
                 if(b.label!=null || b.assem!=null) break;
             }
             if(b==null || b.specialType!=4) continue;  
             // We can't remove BREAK and CONTINUE labels because they won't be
             // referenced until the last step.
             if(b.label.endsWith("_BREAK") || b.label.endsWith("_CONTINUE")) continue;
                                      
             for(int y=0;y<lines.size();++y) {
                 if(lines.get(y) instanceof SpecialConstructInfo) continue;
                 Line c = (Line)lines.get(y);
                 if(c.specialType!=3 && c.specialType!=5) continue;
                 if(!c.specialData.equals(b.label)) continue;                 
                 int i = c.assem.indexOf(b.label);
                 int j = i+b.label.length();
                 String na = c.assem.substring(0,i)+a.label+c.assem.substring(j);
                 c.changeAssem(na);                 
                 c.specialData = a.label;
                 if(BlendConfig.verbose) {
                     c.appendComment("CLEAN-RedundantLabelRedirect- from "+b.label);
                 }                 
             }
             b.specialType = 0; b.specialData = null;
             if(BlendConfig.verbose) {
                 b.appendComment("CLEAN-RedundantLabel- "); 
                 b.commentOutWholeLine();                                 
             } else {
                 lines.remove(nl);
             }
         }
         
         
         
         // If this is a short sweep, return now
         if(!doFull) {
             return countGoodLines(lines);
         }
         
         
         // Clean up any FLOW JumpToReturn (make them returns)
         // Here we only remove JUMPS ... not conditional branches
         
         for(int x=lines.size()-2;x>=0;--x) {                   
             Line a = (Line)lines.get(x);             
             
             // If this is a flow branch, get the destination label
             String targetLabel = getFlowJump(a,true);
             if(targetLabel==null) continue;
             
             // Get the target line
             Line targetLine = findInstructionAt(targetLabel,lines);
             if(targetLine==null) {
                 throw new RuntimeException("Could not find label '"+targetLabel+"' "+a.raw);                 
             }
                          
             // If the target assembly is a return, replace the jump with return
             if(targetLine.assem.equals(BlendConfig.returnInstruction)) {
                 //System.out.println("CLEANED JUMP-TO-RETURN");                                 
                 a.changeAssem(BlendConfig.returnInstruction);
                 a.specialType = 0; a.specialData = null;
                 if(BlendConfig.verbose) {
                    a.appendComment("CLEAN-JumpToReturn- "+targetLabel);
                 }                 
             }
         }          
         
         // Remove orphan returns
         
         for(int x=lines.size()-2;x>=0;--x) {            
             Line a = (Line)lines.get(x);
             String t = getFlowJump(a,true);
             if(t==null) continue;             
             int bo = isNextOrphanReturn(x,lines);
             if(bo>=0) {
                 //System.out.println("CLEANED ORPHAN RETURN");
                 Line b = (Line)lines.get(bo);
                 b.specialType = 0; b.specialData = null;
                 if(BlendConfig.verbose) {                     
                     b.commentOutWholeLine();
                     b.appendComment("CLEAN-OrphanReturn- ");
                 } else {
                     lines.remove(bo);
                 }
             }
         }
         
         // Collapse any branch-to-jump (only FLOW labels)
         // Note that the branch may be a conditional branch since all we are
         // doing is changing the destination.
         
         for(int x=lines.size()-1;x>=0;--x) {               
             Line a = (Line)lines.get(x);             
             
             String targetLabel = getFlowJump(a,false); // Get destination of branch (if it is a branch)
             if(targetLabel==null) continue;      // Not a branch
                          
             // Get the instruction at the branch destination
             Line targetLine = findInstructionAt(targetLabel,lines);
             if(targetLine==null) {                 
                 throw new RuntimeException("Could not find label '"+targetLabel+"' "+a.raw+"#"+a.specialData);                 
             }
             
             // Get the destination of the JUMP that was branched to
             String chainDestination = getFlowJump(targetLine,true);
             if(chainDestination==null) continue;  // Not a branch to a branch
             
             int i = a.assem.indexOf(targetLabel);
             int j = i+targetLabel.length();
             
             // Replace the label in the original assembly with the new label
             String na = a.assem.substring(0,i)+chainDestination+a.assem.substring(j);
             a.changeAssem(na);
             a.specialData = chainDestination;
             
             //System.out.println("CLEANED A JUMP-TO-JUMP");
             
             // In verbose mode, add a comment telling what we did
             if(BlendConfig.verbose) {
                 a.appendComment("CLEAN-JumpToJump- "+targetLabel);
             }
         }
                 
         return countGoodLines(lines);
         
     }
    
    /**
     * This method converts a String expression into a binary tree of
     * Connectors ... this is the magic of Flow!
     * @param ooc the type of output connector for this expression
     * @param baseName the baseName to use in creating assembly labels for nodes
     * @param expression the expression
     * @param inputNodes return list of all input nodes
     * @param allNodes return list of ALL nodes
     */
    static void processLogic(OutputConnector ooc, String baseName, 
        String expression, List inputNodes, List allNodes)
    {
        
        // Create logic tree from expression

        List master = new ArrayList();
        ExpressionNode root = new ExpressionNode();
        root.expression = expression;
        root.connectedToLeft = true;
        master.add(root);
        ExpressionNode.processExpressionList(master);
        
        // Create parallel Connector nodes

        int nc = 1;        
        for(int x=0;x<master.size();++x) {
            ExpressionNode n = (ExpressionNode)master.get(x);
            if(n.expression.equals("||")) {                
                OrConnector oc = new OrConnector("FLOW_"+baseName+nc+"_OR");
                ++nc;
                allNodes.add(oc);
            } else if(n.expression.equals("&&")) {                
                AndConnector ac = new AndConnector("FLOW_"+baseName+nc+"_AND");
                ++nc;
                allNodes.add(ac);
            } else {                
                String [] assembly = BlendConfig.getAssembly(n.expression);
                InputConnector ic = new InputConnector("FLOW_"+baseName+nc+"_INPUT",assembly);                    
                ++nc;
                allNodes.add(ic);
                inputNodes.add(ic);
            }
        }        
        allNodes.add(ooc);
        ++nc;
        
        // Hook up the connectors
        
        for(int x=0;x<master.size();++x) {
            ExpressionNode n = (ExpressionNode)master.get(x);
            Connector r = (Connector)allNodes.get(x);
            int ia = master.size();
            if(n.parent!=null) {
                ia = master.indexOf(n.parent);
            }
            Connector c = (Connector)allNodes.get(ia);            
            if(n.connectedToLeft) {
                c.leftInput = r;
            } else {
                c.rightInput = r;
            }
            r.output = c;
        }               
        
    }
    
    /**
     * This method takes a list of inputs and returns a new list with the
     * same inputs in a different order (changes each call to this method) and
     * with the inputs in a different polarity. A null means all combinations
     * have been returned.
     * @param maxPolarity the dimension of the largest polarity
     * @param inputs the list of input connectors
     * @param ooc the final output connector (has polarity too)
     * @param orderState tracking state for order
     * @param polarityState tracking state for polarity
     * @return the next configuration (or null if all tried)
     */
    static boolean tryAllPolarityAndOrderDone;
    static List tryAllPolarityAndOrder(int maxPolarity, List inputs, OutputConnector ooc,
        int [] orderState, int [] polarityState)
    {       
        
        if(tryAllPolarityAndOrderDone) return null;
        
        // Order the inputs based on CURRENT state
        List ret = new ArrayList();       
        List aa = new ArrayList();
        for(int x=0;x<inputs.size();++x) {
            aa.add(inputs.get(x));
        }
        for(int x=0;x<orderState.length;++x) {
            Object o = aa.remove(orderState[x]);
            ret.add(o);
        }
        ret.add(aa.remove(0));   
        
        // Configure polarity based on CURRENT state
        for(int x=0;x<inputs.size();++x) {
            Connector ic = (Connector)inputs.get(x);
            ic.polarity = polarityState[x];            
        }        
        ooc.polarity = polarityState[polarityState.length-1];
        
        // Now bump the state
        int x = 0;
        while(true) {                   
            polarityState[x] = (polarityState[x]+1)%maxPolarity;
            if(polarityState[x]!=0) return ret;
            ++x;
            if(x==polarityState.length) break;
        }
        
        x = orderState.length-1;
        while(x>=0) {
            ++orderState[x];
            if(orderState[x]!=(orderState.length+1-x)) {
                return ret;                
            }
            orderState[x]=0;
            --x;            
        }
        
        tryAllPolarityAndOrderDone = true;
        
        return ret;
    } 
    
    /**
     * This method processes a SpecialConstructInfo and returns the minimal
     * assmebler representation.
     * @param ooc the final output connector
     * @param pi the construct info
     * @return the list of assembler lines
     */
    static List processSpecialConstruct(OutputConnector ooc, SpecialConstructInfo pi) {
                
        List inputNodes = new ArrayList();
        List allNodes = new ArrayList();
        
        String baseName = pi.baseName;
        
        processLogic(ooc,baseName,pi.expression,inputNodes,allNodes);        
        
        int [] orderState = new int[inputNodes.size()-1];
        int [] polarityState = new int[inputNodes.size()+1]; // Include the output
        tryAllPolarityAndOrderDone = false;     
        
        int maxPolarity = BlendConfig.maxPolarity;
               
        // Try all polarities and orders. Keep the configuration with the
        // smallest number of lines.
        List min = null;
        int min_cleansize = 0;
        while(true) {
            
            List intry = tryAllPolarityAndOrder(maxPolarity,inputNodes,ooc,orderState,polarityState);            
            if(intry==null) break;            
            
            List o = new ArrayList();
            // In case the first listed isn't the first processed   
            String lab = ooc.getProcessConnector(null).name;
            String gcg = BlendConfig.replaceAtTag("@PASS@",BlendConfig.gotoInstruction,lab);
            Line k = new Line(" "+gcg);
            k.specialType = 3;
            k.specialData = lab;
            o.add(k);
            for(int x=0;x<intry.size();++x) {
                Connector c = (Connector)intry.get(x);
                c.toAssembly(o);
            }
            
            List outlines = new ArrayList();
            ooc.toAssembly(outlines);                        
            
            for(int x=0;x<outlines.size();++x) {                    
                Line a = (Line)outlines.get(x);                                
                if(a.raw.equals("$LOGIC")) {
                    outlines.remove(x);
                    --x;
                    for(int y=0;y<o.size();++y) {
                        ++x;
                        outlines.add(x,o.get(y));
                    }
                } else if(a.raw.equals("$TRUE")) {                          
                    outlines.remove(x);
                    --x;
                    for(int y=0;y<pi.normalBlock.size();++y) {
                        ++x;                        
                        outlines.add(x,pi.normalBlock.get(y));
                    }
                } else if(a.raw.equals("$FALSE")) {
                    outlines.remove(x);
                    --x;
                    for(int y=0;y<pi.elseBlock.size();++y) {                        
                        ++x;
                        outlines.add(x,pi.elseBlock.get(y));
                    }
                } 
            }
             
            int ss = cleanStep(outlines,false);   
            //System.out.println(":: GOT "+ss);
            
            if(min==null) {
                min = outlines;
                min_cleansize = ss;                
            } else {
                if(ss <= min_cleansize) {
                    min = outlines;
                    min_cleansize = ss;     
                    //System.out.println(":: MINIMUM "+ss);
                } 
            }
            
        }
        
        return min;
            
    }
   
    
      
    
     
      
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Helper method returns the destination of the jump if the given
     * instruction is a jump to a FLOW destination ... null otherwise.
     * @param s the line to test/parse
     * @return the jump "FLOW" destination (or null)
     */
    static String getFlowJump(Line a, boolean jumpOnly)
    {
        if(jumpOnly) {
            if(a.specialType!=3) return null;
            return (String)a.specialData;
        }
                
        if(a.specialType!=3 && a.specialType!=5) return null;        
        return (String)a.specialData;           
    }
    
    /**
     * Helper method to track down the first valid instruction after the
     * given label.
     * @param label the label to find
     * @param lines the assembler
     * @return the next valid instruction (or null)
     */
    static Line findInstructionAt(String label,List lines)
    {  
        boolean next = false;
        for(int x=0;x<lines.size();++x) {
            Line a = (Line)lines.get(x);            
            // If we found the target label, we'll take the next 
            // valid assembly line (even if it is this line).
            if(a.specialType==4 && a.specialData.equals(label)) {
                next = true;                
            }               
            if(next && a.assem!=null) {
                return a;
            }            
        }
        return null;
    }
            
    /**
     * Helper method checks to see if line after the given
     * index is an orphan RETURN instruction.
     * @param index index of the instruction to check after
     * @param lines the assembly lines
     * @return index of orphan (or -1 if no orphan)
     */
    static int isNextOrphanReturn(int index, List lines)
    {
        while(true) {
            ++index;
            if(index>=lines.size()) return -1;
            if(!(lines.get(index) instanceof Line)) {
               return -1; // No way to know
            }
            Line s = (Line)lines.get(index);                
            if(s.label!=null) return -1; // Someone can get to it by label
            if(s.assem==null) continue; // Pure comment
            
            if(s.assem.startsWith(BlendConfig.returnInstruction)) {
                return index;
            } else {
                return -1;
            }
        }        
    }
    
    /**
     * This method sweeps the given lines of code and expands the special
     * constructs. This must be called repeatedly to expand nested
     * constructs.
     * @param code the code to expand
     * @return true if something was expanded (should come back again)
     */
    static boolean processAllSpecialConstructs(List code) {        
        
        // Find a special block (or return false if no more)
        int fnd = -1;
        for(int x=0;x<code.size();++x) {
            if(code.get(x) instanceof SpecialConstructInfo) {
                fnd = x;
                break;
            } 
        }        
        if(fnd<0) return false;
         
        // Get the block's info
        SpecialConstructInfo pi = (SpecialConstructInfo)code.get(fnd);
        
        // Turn the block into a list of assembler lines
        List result;        
        if(pi.type == 1) {
            //System.out.println("IF");
            IfOutputConnector ooc = new IfOutputConnector("FLOW_"+pi.baseName+"OUTPUT",pi);            
            result = processSpecialConstruct(ooc,pi);             
        } else if(pi.type == 2) {
            //System.out.println("WHILE");
            WhileOutputConnector woc = new WhileOutputConnector("FLOW_"+pi.baseName+"OUTPUT",pi);
            result = processSpecialConstruct(woc,pi);
        } else if(pi.type == 3) {
            //System.out.println("DO");
            DoWhileOutputConnector dwoc = new DoWhileOutputConnector("FLOW_"+pi.baseName+"OUTPUT",pi);
            result = processSpecialConstruct(dwoc,pi);
        } else if(pi.type == 4) {
        	// System.out.println("FOR");
        	ForOutputConnector foc = new ForOutputConnector("FLOW_"+pi.baseName+"OUTPUT",pi);
        	result = processSpecialConstruct(foc,pi);
        }
        
        else {
            throw new RuntimeException("Not implemented yet");
        }
                
        // Replace the block with the new assembler lines
        code.remove(fnd);
        --fnd;
        for(int x=0;x<result.size();++x) {
            ++fnd;
            code.add(fnd,result.get(x));
        }
              
        return true;
    } 
  
    /**
     * This method repeatedly calls clean until the code
     * stops shrinking.
     * @param code the assembly lines to clean
     * @return the final cleaned size
     */
    static int cleanStep(List code,boolean doAll)
    {
        int ss = clean(code,doAll);
        while(true) {
            int sst = clean(code,doAll);
            if(sst == ss) break;
            ss = sst;
        }
        return ss;
    }
   
    public static void main(String [] args) throws Exception
    {
        Reader r = new FileReader(args[0]);
        BufferedReader br = new BufferedReader(r);        
        List codeOrg = new ArrayList();        
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
      
    /**
     * This recursive method assigns a unique name to all special constructs. The
     * break and continue labels are also tagged with their target's name.
     * @param a the list of assembler lines in this file
     * @param baseName the base name of all constructs in this source file
     * @param loopContext passed through ifs so that break/continues know where to go     * 
     */
    static int blockCount = 1;
    static void nameSpecialConstructs(List a,String baseName,String loopContext)
    {           
        for(int x=0;x<a.size();++x) {
            if(a.get(x) instanceof SpecialConstructInfo) {
                SpecialConstructInfo pi = (SpecialConstructInfo)a.get(x);
                pi.baseName = baseName+"_"+blockCount+"_";
                ++blockCount;                 
                String lc = loopContext;
                if(pi.type==2 || pi.type==3 || pi.type==4) {
                    lc = pi.baseName;
                }
                nameSpecialConstructs(pi.normalBlock,baseName,lc);
                if(pi.elseBlock.size()>0) {                    
                    nameSpecialConstructs(pi.elseBlock,baseName,lc);
                }
                continue;
            }
            Line g = (Line)a.get(x);
            
            // Now that we know the names of the constructs we can resolve
            // all breaks and continues into jump statements to the beginning
            // or end of the loop constructs.
            if(g.specialType==1) { // break
                String destination = "FLOW_"+loopContext+"OUTPUT_BREAK";
                String gg = BlendConfig.replaceAtTag("@PASS@",BlendConfig.gotoInstruction,destination);
                g.changeAssem(gg);
                //g.changeAssem(gg);  
                g.specialType = 3; // Note the inserted JUMP
                g.specialData = destination;
            } else if(g.specialType==2) { // continue
                String destination = "FLOW_"+loopContext+"OUTPUT_CONTINUE";
                String gg = BlendConfig.replaceAtTag("@PASS@",BlendConfig.gotoInstruction,destination);
                g.changeAssem(gg);  
                g.specialType = 3; // Note the inserted JUMP
                g.specialData = destination;
            }  
            
        }        
    } 
             
    public static void main(List code) throws Exception 
    {    
                                
        String sname = "A";
        // Parse out the special constructs
        List c = new ArrayList();
        CodeParser.readBlock(code.iterator(),c);     
                
        // Give all constructs a unique name
        nameSpecialConstructs(c,sname,null);       
                
        // Process the special blocks until they have been processed out                
        boolean changed=false;
        do {
            changed = processAllSpecialConstructs(c);
        } while(changed);
        
        // One last clean step now that everything is together
        
        //cleanStep(c,true);  
        
        code.clear();
        code.addAll(c);
        
    }
    
}