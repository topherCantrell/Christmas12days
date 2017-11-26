package gx.g2.core;

/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import gx.CodeLine;

import java.util.*;

/**
 * This class wraps all the information about a single CCL cluster.
 */
public class Cluster
{
    
    public String name;                // The name of the cluster
    public List<CodeLine> lines;       // The text code lines of the cluster
    public List<Command> commands;  // The binary commands of the cluster
    
    /**
     * Destinations are specified as "cluster:label" where either part
     * is optional. This returns the "cluster" portion or "" if not
     * given.
     * @param m the destination string
     * @return the cluster label
     */
    public static String getCluster(String m)
    {
        int i = m.indexOf(":");
        if(i>0) {
            return m.substring(0,i).trim();
        }
        return "";
    }
    
    /**
     * Destinations are specified as "cluster:label" where either part
     * is optional. This returns the "label" portion or m if not
     * given.
     * @param m the destination string
     * @return the offset label
     */
    public static String getOffset(String m)
    {
        int i = m.indexOf(":");
        if(i>0) {
            return m.substring(i+1).trim();
        }
        return m;
    }
    
    /**
     * Helper method to find a particular cluster within the master list
     * of clusters.
     * @param cn the name of the cluster
     * @param clusters the master list of clusters to search
     * @return the index of the cluster or -1 if not found
     */
    public static int findClusterNumber(String cn,List<Cluster> clusters)
    {
        String ccn = cn.toUpperCase();
        for(int x=0;x<clusters.size();++x) {
            if(clusters.get(x).name.toUpperCase().equals(ccn)) return x;
        }
        return -1;
    }
    
    /**
     * This helper method finds the offset to the given label within the
     * given cluster.
     * @param lab the label string
     * @return the binary offset to the command or -1 if label is not found
     */
    public int findOffsetToLabel(String lab)
    {
        lab = lab.toUpperCase();
        int curOffset = 0;
        for(int x=0;x<commands.size();++x) {
            CodeLine cc = commands.get(x).getCodeLine();
            for(int y=0;y<cc.labels.size();++y) {
                if(cc.labels.get(y).toUpperCase().equals(lab)) {
                    return curOffset;
                }
            }
            curOffset = curOffset + commands.get(x).getSize();            
        }
        return -1;
    }    
    
    public int findOffsetToCommand(Command command)
    {
    	int curOffset = 0;
        for(int x=0;x<commands.size();++x) {
        	if(commands.get(x) == command) {
        		return curOffset;
        	}            
            curOffset = curOffset + commands.get(x).getSize();
        }
        return -1;    	
    }
    
    /**
     * This constructs a new Cluster.
     * @param name the name of the cluster
     */
    public Cluster(String name)
    {
        this.name = name;
        this.lines = new ArrayList<CodeLine>();
        this.commands = new ArrayList<Command>();
    }    
    
}
