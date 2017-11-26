package gx.g3.flow; 

import java.util.*;
import java.io.*;

/**
 * This class manages the assembly-configuration information used to generate
 * the assembly lines.
 */
public class BlendConfig
{
    
    static List<ProcessorInfo> processorInfos;
    static ProcessorInfo processorInfo;
            
    static String gotoInstruction;    // The JUMP instruction     
    static String callInstruction;    // The CALL instruction
    static String returnInstruction;  // The RETURN instruction    
    
    static String dataByte;
    static String dataWord;
    static String dataCharacterString;
    
    static String immediateDataTag;
    
    static int maxPolarity; // Max assembly polarities in the selected processor
    
    static boolean verbose=false;
            
    /**
     * This method sets up the global information from the selected.
     * processor.
     */
    static void setParameters()
    {        
        gotoInstruction = processorInfo.jump;        
        
        maxPolarity = 2; // The IF block has two polarities
        
        for(int x=0;x<processorInfo.processorConditionInfo.size();++x) {
            ProcessorConditionsInfo pcsi = (ProcessorConditionsInfo)processorInfo.processorConditionInfo.get(x);
            for(int y=0;y<pcsi.conditions.size();++y) {
                ProcessorConditionInfo pci = (ProcessorConditionInfo)pcsi.conditions.get(y);
                if(pci.codes.size()>maxPolarity) maxPolarity = pci.codes.size();
            }
        }
        
        callInstruction = processorInfo.call;
        returnInstruction = processorInfo.rturn;  
        dataByte = processorInfo.dataByte;
        dataWord = processorInfo.dataWord;
        dataCharacterString = processorInfo.dataCharacterString;
        immediateDataTag = processorInfo.immediateDataTag;
        
        //System.out.println("::"+maxPolarity);
    }
    
    /**
     * This method re-selects the processor.
     * @param processorName the name of the processor
     * @return true if the processor was found
     */
    public static boolean reInit(String processorName)
    {
        for(int x=0;x<processorInfos.size();++x) {
            ProcessorInfo pi = (ProcessorInfo)processorInfos.get(x);
            if(pi.name.equals(processorName)) {
                processorInfo = pi;
                setParameters();
                return true;
            }
        }
        return false;
    }
    
    /**
     * This method attempts to parse the given term in the template of the
     * given condition.
     * @param term the term
     * @param cond the condition template
     * @return false if the template does not match
     */
    static ParseLeftRightOpInfo attemptParse(String term, ProcessorConditionInfo cond)
    {           
        term = term.trim();
        int i = term.indexOf(cond.symbol);
        if(i<0) return null;
        ParseLeftRightOpInfo pi = new ParseLeftRightOpInfo();
        pi.operator = cond.symbol;
        
        if(i>0) {
            pi.left = term.substring(0,i);
        }
        
        i=i+cond.symbol.length();
        pi.right = term.substring(i);
        if(pi.right.length()==0) pi.right=null;
        
        return pi;        
    }
    
    /**
     * This method resolves the @COMPARE@ content.
     * @param i the parsed info from the assembly
     * @param pcis the possible condition statements
     * @return the replacement assembly
     */
    static String resolveCompare(ParseLeftRightOpInfo i, ProcessorConditionsInfo pcis)
    {        
        for(int x=0;x<pcis.compares.size();++x) {
            ProcessorCompareInfo ci = (ProcessorCompareInfo)pcis.compares.get(x);
            
            if(ci.left!=null && i.left==null) continue;
            if(ci.right!=null && i.right==null) continue;
                        
            if(ci.left!=null && !ci.left.equals(i.left)) continue;
            if(ci.right!=null && !ci.right.equals(i.right)) continue;
            
            return ci.code;
            
        }
        return null;
    }
    
    /**
     * This method returns the possible assembly polarities for the given
     * term.
     * @param term the term
     * @return the assembly flavors
     */
    public static String [] getAssembly(String term)
    {
        ParseLeftRightOpInfo exactFnd = null;
        ParseLeftRightOpInfo bestAttempt = null;
        List<String> assem = null;
        String compareResolve = null;
        
        outer: 
        for(int x=0;x<processorInfo.processorConditionInfo.size();++x) {
            ProcessorConditionsInfo pcis = (ProcessorConditionsInfo)processorInfo.processorConditionInfo.get(x);
            
            for(int y=0;y<pcis.conditions.size();++y) {
                ProcessorConditionInfo pci = (ProcessorConditionInfo)pcis.conditions.get(y);
                
                ParseLeftRightOpInfo i = attemptParse(term,pci);
                if(i!=null) {
                    bestAttempt = i;                     
                    boolean leftFnd = false;
                    boolean rightFnd = false;
                    if(i.left !=null) leftFnd = true;
                    if(i.right !=null) rightFnd = true;
                    
                    if(pcis.leftRequired != leftFnd || pcis.rightRequired != rightFnd) continue;
                    
                    assem = pci.codes;
                    String a = (String)assem.get(0);
                    int ind = a.indexOf("@COMPARE@");
                    if(ind>=0) {
                        compareResolve = resolveCompare(i,pcis);
                        if(compareResolve == null) {
                            continue;
                        }
                    }
                    
                    exactFnd = i;
                    //procin = 
                    break outer;                    
                    
                }
            }
        }        
        
        if(exactFnd == null) {
            String m = "No assembly match for term :"+term+":";
            if(bestAttempt!=null) {
                m=m+" (Closest match is :"+bestAttempt.left+":"+bestAttempt.operator+":"+bestAttempt.right+":)";
            }
            throw new RuntimeException(m);            
        } 
        
        String [] ret = new String [assem.size()];
        
        // Decide if the right term is immediate or memory, and tag immediate with "#"
        if(exactFnd.right!=null) {
            exactFnd.right = CodeParser.processMemoryOrImmediate(exactFnd.right);        
        }
        
        for(int x=0;x<assem.size();++x) {
            String a = (String)assem.get(x);
            //System.out.println("::"+a+"::");
            a = replaceAtTag("@COMPARE@",a,compareResolve);
            a = replaceAtTag("@LEFT@",a,exactFnd.left);
            a = replaceAtTag("@RIGHT@",a,exactFnd.right);
            //System.out.println("  ::"+a+"::");
            ret[x] = a;
        }
        
        return ret;
    }
    
    /**
     * This method replaces all instances of 'tag' in the 'target' string
     * with 'replace'.
     * @param tag the tag to match
     * @param target the string to fix
     * @param replace the replacement text
     * @return the fixed-up string
     */
    public static String replaceAtTag(String tag, String target, String replace)
    {        
        while(true) {
            int i = target.indexOf(tag);
            if(i<0) break;
            String a = target.substring(0,i)+replace+target.substring(i+tag.length());
            target = a;
        }
        return target;
    }
    
    /**
     * This method loads the configuration information from the given
     * input stream.
     * @param r the input stream
     */
    public static void initFlowConfig(InputStream is) throws Exception
    {        
        if(is==null) {
            ClassLoader c = BlendConfig.class.getClassLoader();
            is = c.getResourceAsStream("gx/g3/flow/Blend.xml");            
        }
        
        processorInfos = ProcessorInfo.parseXML(is);
        processorInfo = (ProcessorInfo)processorInfos.get(0);   
        
        setParameters();
        
    }
    
     /**
     * This method prints an instruction break-down for the given processor.
     * @param pi the processor
     */
    static void textReport(ProcessorInfo pi)
    {
        System.out.println("-- Processor '"+pi.name+"' --");
        for(int x=0;x<pi.processorConditionInfo.size();++x) {
            ProcessorConditionsInfo pcis = (ProcessorConditionsInfo)pi.processorConditionInfo.get(x);
            if(pcis.compares.size()>0) {
                System.out.println(" COMPARE OPERATIONS");
                for(int y=0;y<pcis.compares.size();++y) {
                    ProcessorCompareInfo i = (ProcessorCompareInfo)pcis.compares.get(y);
                    String left = "@LEFT@";if(i.left!=null)left = i.left;
                    String right = "@RIGHT@";if(i.right!=null)right = i.right;
                    System.out.println("   "+left+" OP "+right);
                }
            }
            for(int y=0;y<pcis.conditions.size();++y) {
                ProcessorConditionInfo pci = (ProcessorConditionInfo)pcis.conditions.get(y);
                if(pcis.leftRequired) {
                    System.out.print("  @LEFT@ ");
                } else {
                    System.out.print("  ");
                }
                System.out.print(pci.symbol);
                if(pcis.rightRequired) {
                    System.out.print(" @RIGHT@ ");
                } 
                System.out.println();                
            }
            if(x!=pi.processorConditionInfo.size()-1) System.out.println();
        }
        if(pi.subs.substituteKey.size()>0) {
            System.out.println("\n DIRECT SUBSTITUTIONS");
        }
        
        for(int x=0;x<pi.subs.substituteKey.size();++x) {
            String a = (String)pi.subs.substituteKey.get(x);
            String b = (String)pi.subs.substituteCode.get(x);
            
            System.out.println("    ::"+a+":: -> ::"+b+"::");
            
        }
    }
}
