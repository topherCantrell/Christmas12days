package gx.g3.struct;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.core.Cluster;

import java.util.List;
import java.util.Map;

/*
 
'*** MUSIC
'
'
'

 */
public abstract class StructParser 
{
	
	protected int commandInfo;
	
	public StructParser(int commandInfo)
	{
		this.commandInfo = commandInfo;
	}

	/**
	 * This method parses the given code line and adds the resulting COGCommand(s)
	 * to the cluster.
	 * @param name the name of the structure
	 * @param c the CodeLines to parse
	 * @param cluster the CodeLine's container
	 * @param compiler TODO
	 * @param defines the argument-replacement text
	 * @return null if doesn't belong to us, "" if OK, or  message if parse error
	 */
	public abstract String parse(String name, List<CodeLine> c, Cluster cluster, Map<String,String> subs, Assemble compiler);

	/**
	 * This method adds any system defines to the map of substitutions.
	 * @param subs the map of substitutions
	 */
	public void addDefines(Map<String, String> subs) {}

}
