package gx.g3.flow; 

import java.util.*;
import java.io.*;

/**
 * This class contains static methods to parse lines of assembler.
 */
public class CodeParser
{
    
    public static String processMemoryOrImmediate(String term)
    {
    	        
        // A = #???
        // A = >???        
        // A = VarA[X]   
        // A = VarA,X
        // A = '0'
        
        // A = (VarA - VarB)
        // A = 435
        
        // A = VarA
        
                
        // We can make a stab at figuring out if a term refers to a memory
        // access or an immediate value.
        // First, we'll translate '&' to '#' and keep terms that start with '#' as is
        // Any term that starts with ">" is forced to be not-immediate and left as is
        // Any term with a "," or "[" in it is a memory reference (indirect)
        
        // Having ruled those out ...
        
        // Any term with a math operator in it (+,-,/,*) is immediate
        
        // Any term starting with "'" is immediate
        
        // Any term that is purely numeric is immediate
        
        if(term.startsWith("&")) {
            term = BlendConfig.immediateDataTag+term.substring(1);
        }
        
        if(term.startsWith(BlendConfig.immediateDataTag)) {
            return term;
        }
        
        if(term.startsWith(">")) {
            return term.substring(1);
        }
                
        if(term.indexOf(",")>=0 || term.indexOf("[")>=0) {
            return term;
        }
        
        // ---------------------
        
        // Looks like a math expression ... must be immediate
        if(term.indexOf("-")>=0 || term.indexOf("+")>=0 || 
           term.indexOf("*")>=0 || term.indexOf("/")>=0) {
               return BlendConfig.immediateDataTag+term;
        }
        
        if(term.startsWith("'")) {            
            return BlendConfig.immediateDataTag+term;
        }
        
        try {
            // Is it purely numeric?
            Integer.parseInt(term);
            return BlendConfig.immediateDataTag+term;
        } catch(Exception e) {}
        
        // If all else fails, we'll assume it is a memory access
        return term;
    }   
        
    /**
     * This method parses out the expression term from the beginning
     * of the expression string. A term is either wrapped in
     * parenthesis or ends with a connector (&&, ||)
     * @param expression the expression to parse
     * @return the first term in the string
     */
    public static String parseTerm(String expression)
    {                
        if(!expression.startsWith("(")) {
            int i = expression.indexOf("&&");
            int j = expression.indexOf("||");
            if(i<0) i=j;
            if(j<0) j=i;            
            if(j<i) i=j;            
            if(i<0) return expression;
            return expression.substring(0,i);
        }        
        int level = 0;
        for(int x=0;x<expression.length();++x) {
            if(expression.charAt(x)=='(') {
                ++level;
            } else if(expression.charAt(x)==')') {
                --level;
                if(level==0) {
                    return expression.substring(0,x+1);                   
                }
            }
        }
        throw new RuntimeException("Missing or extra parenthesis: "+expression);
    } 
    
    /**
     * This method parses an expression of two entities separated by a
     * AND or OR connector.
     * @param expression the term to disect
     * @return the parsed information
     */
    public static ParseLeftRightOpInfo parseLogicConnector(String expression)
    {
        ParseLeftRightOpInfo ret = new ParseLeftRightOpInfo();
                
        String g = expression.trim();
        
        // Get the left hand term
        String a = parseTerm(g);        
        ret.left = a.trim();
        if(ret.left.startsWith("(")) {
            ret.left = ret.left.substring(1,ret.left.length()-1);
        }
        
        g = g.substring(a.length()).trim();
        
        if(g.length()==0) {
            return ret; // Just one term ... that's OK
        }
        
        // More ... it MUST be a logic connector
        if(g.startsWith("&&")) {
            ret.operator = "&&";
        } else if(g.startsWith("||")) {
            ret.operator = "||";
        } else {
            throw new RuntimeException("Expected '&&' or '||' :"+g);
        }        
        g = g.substring(2).trim();
        
        // If there was an operation, there MUST be a right-hand term
        a = parseTerm(g);
        ret.right = a.trim();
        if(ret.right.startsWith("(")) {
            ret.right = ret.right.substring(1,ret.right.length()-1);
        }
        
        // And that's all there can be
        g = g.substring(a.length()).trim();
        if(g.length()!=0) {
            throw new RuntimeException("Extra information: "+g);
        } 
        
        return ret;
        
    }          
    
    /**
     * This method pulls an expression (for 'if' or 'while') from possibly
     * multiple lines of the input stream.
     * @param firstLine the first line of the expression
     * @param br the input stream
     * @return the combined expression
     */
    public static String pullExpression(Line firstLine, Iterator<Line> br) throws IOException 
    {
                
        String g = firstLine.rawNoComment.trim();          
        
        // This is an awkward requirement imposed by the do-while construct
        // Expressions can span multiple lines ... just make sure no line
        // ends with a ')'.
        
        // Put all the lines together until we have a complete expression.
        while(g.indexOf("{")<0 && !g.endsWith(")")) {
            if(!br.hasNext()) {
                throw new RuntimeException("Unexpected EOF");
            }
            Line a = (Line)br.next();
            String gg = a.rawNoComment.trim();               
            g = g + gg;
        }          
        
        if(g.endsWith("{")) {
            g = g.substring(0,g.length()-1).trim();
        }
        
        int a = g.indexOf("(");        
        
        g=g.substring(a+1,g.length()-1); // Stripping off surrounding parenths
        
        //System.out.println("EXPRESSION:"+g+":");
                
        return g;
         
    }   
    
    /**
     * This method parses an entire special block (if, do, while, else, etc).
     * @param firstLine the first line of the special construct
     * @param br the input input stream
     * @returns the information about the special construct
     */
    public static SpecialConstructInfo parseSpecialConstruct(Line firstLine,Iterator<Line> br) throws IOException
    {
        SpecialConstructInfo ret = new SpecialConstructInfo();            
                      
        if(firstLine.assem.startsWith("if(")) {             
            
            ret.type = 1;
            
            ret.expression = pullExpression(firstLine,br);            
            
            Line a = readBlock(br,ret.normalBlock);  
            String ns = a.rawNoComment.trim();
            
            if(ns.startsWith("} else if(")) {                      
                a = new Line(ns.substring(6)); // Including space in the new line
                SpecialConstructInfo pp = parseSpecialConstruct(a,br);
                ret.elseBlock.add(pp);
                return ret;
            } 
            
            if(ns.equals("} else {")) {                
                a = readBlock(br,ret.elseBlock);   
                ns = a.rawNoComment.trim();
            }
            
            if(!ns.equals("}")) {                
                throw new RuntimeException("Invalid IF. Expected '}' :"+a.raw);
            }     
            
            return ret;
            
        } else if(firstLine.assem.startsWith("while(")) {            
            
            ret.type = 2;
            ret.expression = pullExpression(firstLine,br);            
            Line a = readBlock(br,ret.normalBlock);
            String ns = a.rawNoComment.trim();
            
            if(!ns.equals("}")) {
                throw new RuntimeException("Invalid WHILE. Expected '}' :"+a.raw);
            }             
            
            return ret;
            
        } else if(firstLine.assem.startsWith("do ")) {           
            
            ret.type = 3;                        
            Line a = readBlock(br,ret.normalBlock);
            String ns = a.rawNoComment.trim();            
            
            if(!ns.startsWith("} while(")) {
                throw new RuntimeException("Invalid DO-WHILE. Expected '} while' :"+a.raw);
            }
            
            ns = ns.substring(1);
            a.parse(ns);
            ret.expression = pullExpression(a,br);              
                        
            return ret;
        } else if(firstLine.assem.startsWith("for(")) {
        	ret.type = 4;
        	String nns = firstLine.assem.substring(4).trim();
        	int j = nns.lastIndexOf(')');
        	nns = nns.substring(0,j);
        	StringTokenizer st = new StringTokenizer(nns,";");
        	if(st.countTokens()!=3) {
        		throw new RuntimeException("Invalid FOR. "+firstLine.raw);
        	}
        	ret.initLine = st.nextToken().trim();
        	Line a = new Line("while("+st.nextToken()+")");
            ret.expression = pullExpression(a,br);
            ret.iteratorLine = st.nextToken().trim();
            a = readBlock(br,ret.normalBlock);
            String ns = a.rawNoComment.trim();
            
            if(!ns.equals("}")) {
                throw new RuntimeException("Invalid FOR. Expected '}' :"+a.raw);
            }             
            
            return ret;
        }
        
        throw new RuntimeException("Don't know this:"+firstLine);
        
    }
    
    /**
     * This method reads an entire code-flow block of lines from the input
     * stream inserting the lines (or recursively parsed blocks) into the
     * return list.
     * @param br the file stream
     * @param ret the return list
     * @return the line terminating the block (for further processing)
     */
    public static Line readBlock(Iterator<Line> codeOrg, List<Object> ret) throws IOException
    {  
        
        while(true) {
            if(!codeOrg.hasNext()) {
                return null;
            }
            
            Line a = (Line)codeOrg.next();
            
            // Break and Continue are processed later, but we can
            // tag them here while we are parsing the lines.
            if(a.assem!=null) {
                if(a.assem.equals("break")) {
                    a.specialType = 1;
                    ret.add(a);
                    continue;
                }
                if(a.assem.equals("continue")) {
                    a.specialType = 2;
                    ret.add(a);
                    continue;
                }
            }
                        
            // If this is an end-of-block marker, return it
            if(a.rawNoComment.trim().startsWith("}")) {                
                return a;
            }
            
            // Process the line if it is something special
            boolean special = false;
            if(a.assem!=null) {
                if(a.assem.startsWith("if(")) {
                    special = true;
                } else if(a.assem.startsWith("do ")) {
                    special = true;
                } else if(a.assem.startsWith("while(")) {
                    special = true;
                } else if(a.assem.startsWith("for(")) {
                	special = true;
                }
            }
            
            if(special) {                
                // Special constructs are parsed
                SpecialConstructInfo pi = parseSpecialConstruct(a,codeOrg);            
                ret.add(pi);
            } else {
                // Other lines are added as-is
                ret.add(a);
            }            
            
        }            
                
    }        
    
}
