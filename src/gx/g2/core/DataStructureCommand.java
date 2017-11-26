package gx.g2.core;

/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import gx.CodeLine;

import java.util.*;

public interface DataStructureCommand
{
    
    /**
     * Processes any special data structure (Sprites, Waveforms, Sequences).
     * @param type the type from the structure
     * @param code the list of lines from the cluster
     * @param data the data command to fill out
     * @param defines the argument-replacement text
     */
    public String processSpecialData(String type, List<CodeLine> code, List<Command> data, 
    		Map<String,String> defines);
    
}
