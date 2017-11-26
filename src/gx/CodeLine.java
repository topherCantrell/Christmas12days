package gx;

/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import java.util.*;

/**
 * This class wraps all the information about a single line of CCL code.
 */
public class CodeLine
{
    
    public int lineNumber;       // The line number
    public String file;          // The file name    
    public String text;          // The text with substitutions made
    
    public int bracketLevel=-1;  // Bracket level used in decoding flow constructs   
    public int bracketType;      // 0=none, 1=close, 2=open
    
    public List<String> labels;  // All the labels attached to this line
    
    /**
     * This helper functions parses a string number in possibly other
     * bases.
     * @param m the string
     * @return the converted number
     */
    public static long parseNumber(String m)
    {
        while(true) {
            int i = m.indexOf("_");
            if(i<0) break;
            String t = m.substring(0,i)+m.substring(i+1);
            m = t;
        }
        if(m.startsWith("'") && m.endsWith("'")) {
            if(m.equals("'//n'")) {
                return 10;
            }            
            return m.charAt(1);
        }
        if(m.startsWith("0x") || m.startsWith("0X")) {
            return Long.parseLong(m.substring(2),16);
        }
        if(m.startsWith("0b") || m.startsWith("0B")) {
            return Long.parseLong(m.substring(2),2);
        }
        return Long.parseLong(m);
    }
    
    /**
     * This constructs a new CodeLine.
     * @param lineNumber the line number
     * @param file the file name
     * @param text the code text
     */
    public CodeLine(int lineNumber, String file, String text)
    {
        this.lineNumber = lineNumber;
        this.file = file;
        this.text = text.trim();        
        labels = new ArrayList<String>();
    }

	public CodeLine(CodeLine originalLine, String newText) 
	{
		this(originalLine.lineNumber,originalLine.file,newText);
	}
    
    /*
    // For debugging
    public String toString()
    {
        char bt = ' ';
        if(bracketType==1) bt='}';
        if(bracketType==2) bt='{';
        String bl = ""+bracketLevel;
        if(bracketLevel<0) bl = "-";
        return bl+" "+bt+" "+file+":"+lineNumber+" "+text;
    }
     */
    
}
