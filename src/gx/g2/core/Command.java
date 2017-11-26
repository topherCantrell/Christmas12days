package gx.g2.core;

/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import gx.CodeLine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Commands turn parsed data into binary. The various parsers
 * create subclasses of this class. 
 */
public abstract class Command
{
	
	/**
     * This helper function converts a number to a binary string.
     * @param num the number
     * @len the number of bits in the final field
     * @return the binary string
     */
    public static String toBinaryString(int num, int len)
    {
        String ret = Integer.toString(num,2);
        while(ret.length()<len) ret="0"+ret;
        return ret;
    }
    
    /**
     * This helper function converts a number to an 8-digit hex string.
     * @param num the number to convert
     * @return the hex string
     */
    public static String toLongString(long num)
    {
        String ret = Long.toString(num,16).toUpperCase();
        while(ret.length()<8) ret="0"+ret;
        return ret;
    }
	
    protected CodeLine codeLine; // The CodeLine that produced this binary
    protected Cluster cluster;   // The Cluster this command belongs in
        
    /**
     * Constructs a new COGCommand.
     * @param line the CodeLine
     * @param clus the Cluster
     */
    public Command(CodeLine line, Cluster clus) 
    {
    	codeLine = line;
    	cluster = clus;
    }    
    
    public void setCluster(Cluster clus) {cluster = clus;}
        
    /**
     * Returns the CodeLine that generated the binary (used for error reports)
     * @return the CodeLine
     */
    public CodeLine getCodeLine() {return codeLine;}
    
    /**
     * Changes the codeline.
     * @param codeLine the new CodeLine
     */
    public void setCodeLine(CodeLine codeLine) {this.codeLine = codeLine;}
       
    /**
     * Converts the command to binary form. It assumes the "toSPIN" method
     * returns pure binary.
     * @param clusters the master list of clusters
     * @param dest the byte array sized by "getSize".
     * @return any error string or null if OK
     */
    public String toBinary(List<Cluster> clusters, byte [] dest)
    {
        String s = toSPIN(clusters);
        if(s.startsWith("#")) return s;        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes());
            InputStreamReader isr = new InputStreamReader(bais);
            BufferedReader br = new BufferedReader(isr);
            int ind=0;
            while(true) {
                String g = br.readLine();
                if(g==null) break;
                g = g.trim();
                if(g.startsWith("'")) continue;
                int i = g.indexOf("%");
                s = "0b"+g.substring(i+1);
                long h = CodeLine.parseNumber(s);
                dest[ind+3] = (byte)((h>>24)&0xFF);
                dest[ind+2] = (byte)((h>>16)&0xFF);
                dest[ind+1] = (byte)((h>>8)&0xFF);
                dest[ind+0] = (byte)((h>>0)&0xFF); 
                ind = ind + 4;
                
            }
        } catch (IOException e) {
            return e.getMessage();
        }       
        return null;
    }
    
    /**
     * Converts the command to SPIN language data commands.
     * @param clusters the master list of clusters
     * @return any error string (starts with "#") or SPIN text
     */
    public abstract String toSPIN(List<Cluster> clusters);
    
    /**
     * Returns the size of the binary of the command.
     * @return size of the command
     */
    public abstract int getSize();   
    
}
