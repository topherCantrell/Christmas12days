package gx.g2;

/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import gx.CodeLine;
import gx.g2.core.Argument;
import gx.g2.core.ArgumentList;
import gx.g2.core.Cluster;

import java.util.Map;
import java.util.StringTokenizer;

public abstract class Parser
{	
	protected int commandInfo;
	
	public Parser(int commandInfo)
	{
		this.commandInfo = commandInfo;
	}

	/**
	 * This method parses the given code line and adds the resulting COGCommand(s)
	 * to the cluster.
	 * @param c the CodeLine to parse
	 * @param cluster the CodeLine's container
	 * @param compiler TODO
	 * @param defines the argument-replacement text
	 * @return null if doesn't belong to us, "" if OK, or  message if parse error
	 */
	public abstract String parse(CodeLine c, Cluster cluster, Map<String,String> subs, Assemble compiler);

	/**
	 * This method adds any system defines to the map of substitutions.
	 * @param subs the map of substitutions
	 */
	public void addDefines(Map<String, String> subs) {}
	
	public static ArgumentList parseSyntax(String line, String syntax, Map<String,String> subs, StringBuffer err)
	{
		// Syntax tokens separated by whitespace
		StringTokenizer st = new StringTokenizer(syntax," ");
		String s = st.nextToken();		
		if(!line.toUpperCase().startsWith(s)) {
			return null;			
		}
				
		// Enhance as needed. Right now we handle:
		// NAME=*		
		
		line = line.substring(s.length()).trim();		
		ArgumentList ps = new ArgumentList(line,subs);
		ArgumentList ret = new ArgumentList();
		int pn = 0;
		while(st.hasMoreTokens()) {
			String n = st.nextToken();
			String name = n;
			String value = "";
			int i = n.indexOf("=");
			if(i>0) {
				name = n.substring(0,i);
				value = n.substring(i+1);
			}
			if(name.length()==0 || !value.equals("*")) {
				throw new RuntimeException("Unknown '"+n+"'");
			}
			Argument a = ps.removeArgument(name, pn);
			++pn;
			if(a==null) {
				err.append("Missing "+name+" operand.");
				return null;
			}
			ret.addArgument(a);
		}
		
		String uns = ps.reportUnremovedValues();
		if(uns.length()>0) {
			err.append(s);
			return null;
		}
		
		return ret;
	}
	
}

