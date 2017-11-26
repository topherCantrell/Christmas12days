package gx.g2;

/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import gx.CodeLine;
import gx.g2.core.Cluster;
import gx.g2.core.Command;
import gx.g3.flow.Blend;
import gx.g3.flow.Line;
import gx.g3.struct.StructParser;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assemble 
{
	
	protected Map<String,String> subs = new HashMap<String,String>();    // Map of define values
	
	protected List<Cluster> clusters = new ArrayList<Cluster>();     // List of clusters    
	protected List<Parser> codeParsers = new ArrayList<Parser>();   // List of code-section parsers
	protected List<StructParser> structParsers = new ArrayList<StructParser>(); // List of data structure parsers
        
	protected boolean inCommentBlock; // True if ignoring lines
	protected String commentBlockStartFile;
	protected int commentBlockStartLineNumber;    
    
	protected boolean multiClusters;        
    
    protected Object constructObjectFromString(String s) throws Exception
    {    	    	
    	Object p = null;
    	int i = s.indexOf("(");
		String cn = s.substring(0,i);  
		Class<?> cc = getClass().getClassLoader().loadClass(cn);
		int j = s.indexOf(")",i);
		String inf = s.substring(i+1,j);
		i = inf.indexOf(",");
		if(i>0) {
			String a = inf.substring(0,i).trim();
			String b = inf.substring(i+1).trim(); 
			i = a.lastIndexOf(".");
			String aa = a.substring(0,i);
			String ab = a.substring(i+1);
			Class<?> en = getClass().getClassLoader().loadClass(aa);    			
			Object[] evs = en.getEnumConstants();
			Object eo = null;
			for(Object oo : evs) {
				if(oo.toString().equals(ab)) {
					eo = oo;
					break;
				}
			}
			Constructor<?> con = cc.getConstructor(en,int.class);
			p = con.newInstance(eo,Integer.parseInt(b));			
		} else {
			Constructor<?> con = cc.getConstructor(int.class);
			p = con.newInstance(Integer.parseInt(inf));			
		}  
		return p;    	
    }
     
    /**
     * This method configures the compiler from comments in the SPIN file.
     * @param spinName name of SPIN file to parse
     * @throws Exception if something goes wrong
     */
    public void configureFromSpin(String spinName) throws Exception
    {
    	    	
    	FileReader fr = new FileReader(spinName);
		BufferedReader br = new BufferedReader(fr);
				
		List<String> commandNames = new ArrayList<String>();
		List<String> commandInfos = new ArrayList<String>();
		List<String> commandNumbers = new ArrayList<String>();
		List<String> dialectNames = new ArrayList<String>();
		
		while(true) {
			String g = br.readLine();
			if(g==null) break;
			if(g.startsWith("'   #command ")) {
				int i = g.indexOf("=");
				if(i<0) {
					throw new RuntimeException("Missing '=' in command");
				}
				String a = g.substring(13,i).trim();
				String b = g.substring(i+1).trim();
				if(b.startsWith("YES")) {
					commandNames.add(a);
					commandInfos.add(b.substring(3).trim());					
				}				
				continue;
			}
			if(g.startsWith("' #dialectCommand ")) {
				dialectNames.add(g.substring(17).trim());
				continue;
			}
			if(g.startsWith("'")) continue;
			if(g.indexOf("' #command-table")>0) {
				int i = g.indexOf("#core");
				int j = g.indexOf(" ",i);
				String a = g.substring(i+1,j);
				commandNumbers.add(a);				
			}
		}	
    	
    	for(int x=0;x<dialectNames.size();++x) {
    		String s = dialectNames.get(x);
    		s = s.replace("$", ""+x);   		
    		Object o = constructObjectFromString(s);
    		if(o instanceof Parser) {
    			addParser((Parser)o);
    		} else {
    			addStructParser((StructParser)o);
    		}    		
    	}
    	
    	
    	for(int x=0;x<commandNames.size();++x) {
    		String s = commandInfos.get(x);
    		s = s.replace("$",""+commandNumbers.indexOf(commandNames.get(x)));
    		Object o = constructObjectFromString(s);
    		if(o instanceof Parser) {
    			addParser((Parser)o);
    		} else {
    			addStructParser((StructParser)o);
    		}   
    	}
    }
    
    public void addParser(Parser parser)
    {
    	codeParsers.add(parser);
    	parser.addDefines(subs);
    }    
    
    public void addStructParser(StructParser parser)
    {
    	structParsers.add(parser);
    	parser.addDefines(subs);
    }    
    
    /**
     * This recursive method processes the lines from a single CCL file.
     * @param file name of single file to parse
     * @param workCluster the current cluster to add commands to
     * @returns error message or null if OK
     */
    protected String parse(String file, Cluster workCluster)
    {
        try {            
            int lineNumber = 0;
            Reader r = new FileReader(file);
            BufferedReader br = new BufferedReader(r);            
            while(true) {
                String g = br.readLine();
                ++lineNumber;
                if(g==null) break;
                g=g.trim();
                
                
                // TODO Comments and include processed out by BLEND
                
                int sb = g.indexOf("/*");
                int eb = g.indexOf("*/");                
                if(sb>0) {
                    return file+":"+lineNumber+" '/*' must be the first thing on the line";
                }
                if(eb>0) {
                    return file+":"+lineNumber+" and '*/' must be the first thing on the line";
                }
                if(inCommentBlock && sb==0) {
                    return file+":"+lineNumber+" '/*' not allowed ... already inside a comment block "+commentBlockStartFile+":"+commentBlockStartLineNumber;
                }
                if(!inCommentBlock && eb==0) {
                    return file+":"+lineNumber+" '*/' not allowed ... not inside a comment block";
                }
                if(inCommentBlock) {
                    if(eb==0) {
                        inCommentBlock = false;
                        if(g.substring(2).trim().length()>0) {
                            return file+":"+lineNumber+" '*/' must be the only thing on the line";
                        }
                    }
                    continue;
                }
                if(sb==0) {
                    inCommentBlock = true;
                    commentBlockStartFile = file;
                    commentBlockStartLineNumber = lineNumber;
                    continue;
                }
                
                int i = g.indexOf("//");
                if(i>=0) {
                    // Ignore comments
                    g = g.substring(0,i).trim();
                }
                String cg = g.toUpperCase();      // Case doesn't matter in keywords                
                if(cg.length()==0) continue;      // Ignore blank lines
                
                if(cg.startsWith("CLUSTER")) {
                    g = g.substring(7).trim();
                    if(g.length()==0) {
                        return file+":"+lineNumber+" CLUSTER must have name";
                    }
                    // Start a new cluster
                    workCluster = new Cluster(g);
                    clusters.add(workCluster);                    
                    continue;
                }     
                                                
                // Includes are recursive calls
                if(cg.startsWith("INCLUDE")) {     
                    g = g.substring(7).trim();
                    String e = parse(g,workCluster);
                    if(e!=null) return e;
                    continue;
                }     
                      
                
                // We'll manage defines here
                if(cg.startsWith("DEFINE")) {
                    i = g.indexOf("=");
                    if(i<0) {
                        return file+":"+lineNumber+" missing '=' in define";
                    }
                    String k = g.substring(6,i).trim().toUpperCase();
                    String v = g.substring(i+1).trim();                    
                    if(v.length()==0) {     
                    	subs.remove(k);                                         
                    } else {                        
                    	subs.put(k,v);                        
                    }                     
                    continue;
                }
                
                CodeLine cc = new CodeLine(lineNumber,file,g);
                
                workCluster.lines.add(cc);                          
            }            
        } catch (Exception e) {
            return e.getMessage();
        }        
        return null;        
    }
    
    protected String parse(List<Line> blendedCode)
    {
    	Cluster workCluster = new Cluster("");
        clusters.add(workCluster);
                
        for(Line line : blendedCode) {
        	
        	
        	String g = line.toString();
        	String cg = g.toUpperCase();      // Case doesn't matter in keywords
        	
        	//System.out.println("::"+g+"::");
            
            if(cg.startsWith("CLUSTER")) {
                g = g.substring(7).trim();
                if(g.length()==0) {
                    return line.sourceFile+":"+line.sourceFileLine+" CLUSTER must have name";
                }
                // Start a new cluster
                workCluster = new Cluster(g);
                clusters.add(workCluster);                    
                continue;
            }                                           
            
            // We'll manage defines here
            if(cg.startsWith("DEFINE")) {
                int i = g.indexOf("=");
                if(i<0) {
                    return line.sourceFile+":"+line.sourceFileLine+" missing '=' in define";
                }
                String k = g.substring(6,i).trim().toUpperCase();
                String v = g.substring(i+1).trim();                    
                if(v.length()==0) {     
                	subs.remove(k);                                         
                } else {                        
                	subs.put(k,v);                        
                }                     
                continue;
            }
            
            String gg = g.trim();
            if(gg.length()==0) continue; // Ignore blank lines
            if(gg.startsWith("//")) continue; // Ignore blank lines
            
            // TODO BLOCK COMMENTS IN BLEND
             
            CodeLine cc = new CodeLine(line.sourceFileLine,line.sourceFile,g);            
            workCluster.lines.add(cc);            
        	
        }
                
        if(workCluster.lines.size()==0) {
        	clusters.remove(0);
        }        
        
        // Lines ending with "," are merged with the next line. Allows for
        // long parameter lists to span several lines.
        for(int y=0;y<clusters.size();++y) {
            for(int x=0;x<clusters.get(y).lines.size()-1;++x) {
                CodeLine c = clusters.get(y).lines.get(x);
                if(c.text.endsWith(",")) {
                    CodeLine cc = clusters.get(y).lines.get(x+1);
                    c.text = c.text + cc.text;
                    clusters.get(y).lines.remove(x+1);
                    --x;
                }
            }
        }
        
        return null;
    }
    
    protected void attachLabels(Cluster c)
    {
        CodeLine current = null;
        for(int x=c.lines.size()-1;x>=0;--x) {
            CodeLine a = c.lines.get(x);
            if(a.text.endsWith(":") && a.text.indexOf(" ")<0 ) {
                c.lines.remove(x);
                String t = a.text.substring(0,a.text.length()-1).trim();
                if(current!=null) {
                    // Any pointless labels on the end get ignored
                    current.labels.add(t);
                }                                 
            } else {
                current = a;
            }
        }
    }
    
    public String compileLine(CodeLine c, Cluster clus)
    {
    	String er = null;                                
        for(int z=0;z<codeParsers.size();++z) {
            Parser par = codeParsers.get(z);                        
            er = par.parse(c,clus,subs,this);						
            if(er!=null) break;                        
        }                
        if(er==null) {
            er = "Unknown Command";
        }
        if(er.length()>0) {
        	return "## "+er+"\r\n"+c.file+":"+c.lineNumber+"\r\n"+c.text;
        }
        return er;    	
    }
    
    public String compileStruct(String stype, List<CodeLine> structLines, Cluster clus)
    {
    	// Find the first parser to take it
    	String er = null;
    	for(StructParser sp : structParsers) {
    		er = sp.parse(stype, structLines, clus, subs, this);
    		if(er!=null) break;                		
    	}
    	if(er==null) {
    		er = "Unknown structure '"+stype+"'";
    	}
    	if(er.length()>0) {
    		return "## "+er+"\r\n"+structLines.get(0).file+":"+structLines.get(0).lineNumber+"\r\n"+structLines.get(0).text;
    	}
    	return er;    	
    }
    
    /**
     * This method compiles the list of CodeLines. 
     * @return error string or null if OK
     */
    public String compile() 
    {
    	
        // Process the individual lines        
        for(int x=0;x<clusters.size();++x) {
            Cluster clus = clusters.get(x);
            attachLabels(clus);
            for(int y=0;y<clus.lines.size();++y) {                
                CodeLine c = clus.lines.get(y);
                
                String ccom = c.text.toUpperCase().trim();   
                
                if(ccom.startsWith("'")) {
                	
                	// First line of ' block is type
                	List<CodeLine> structLines = new ArrayList<CodeLine>();
                	structLines.add(c);
                	++y;
                	int i = ccom.indexOf(" ");
                	if(i<1 || ccom.charAt(i-1)!='*') {
                		return "## Expected '* TYPE\r\n"+c.file+":"+c.lineNumber+"\r\n"+c.text;
                	}
                	String stype = ccom.substring(i+1).trim();
                	if(stype.length()==0) {
                		return "## Expected '* TYPE\r\n"+c.file+":"+c.lineNumber+"\r\n"+c.text;
                	}                	
                	
                	// Pull all the lines in this run                	
                	while(y<clus.lines.size()) {
                		ccom = clus.lines.get(y).text.trim();
                		if(!ccom.startsWith("'")) break;
                		if(ccom.substring(1).trim().startsWith("*")) break;
                		structLines.add(clus.lines.get(y));
                		++y;
                	}
                	
                	String er = compileStruct(stype,structLines,clus);
                	if(er.length()!=0) return er;               	
                	
                	// OK ... back for more structures or commands
                	--y;
                	continue;
                }
                String er = compileLine(c,clus);
                if(er.length()!=0) return er;
                
            }
        }
         
        return null;
    }
    
    public String writeSPIN(PrintStream ps) throws IOException
    {   
        for(int x=0;x<clusters.size();++x) {
            ps.println("' Cluster '"+clusters.get(x).name+"'");     
            int ofs = 0;
            for(int y=0;y<clusters.get(x).commands.size();++y) {
               // ps.println("' @"+ofs+"    ("+(ofs/4)+")");                
                Command cc = clusters.get(x).commands.get(y);
                ofs = ofs + cc.getSize();
                CodeLine c = cc.getCodeLine();       
                String e = cc.toSPIN(clusters);
                if(e.startsWith("#")) {                    
                    return "## "+e+"\r\n"+c.file+":"+c.lineNumber+"\r\n"+c.text.trim();                    
                }
                for(int z=0;z<c.labels.size();++z) {
                    ps.println("' "+c.labels.get(z)+":");
                }                
                ps.println(e);
            }
        }
        ps.flush();
        return null;
    }    
    
    public static void main(String [] args) throws Exception
    {
    	// Configure a compiler based on the SPIN comments
    	Assemble compiler = new Assemble();
    	compiler.configureFromSpin(args[0]);
    	
    	// TODO concat lines that end in ","
    	
    	// Parse the root file
    	List<Line> orgCode = Blend.main(args[1],"MIX");
    	
    	// Parse the code
    	String er = compiler.parse(orgCode);
        if(er!=null) {
            System.out.println(er);
            return;
        } 
        
        // Compile the code
        er = compiler.compile();
        if(er!=null) {
            System.out.println(er);
            return;
        }        
        
        OutputStream oos = new FileOutputStream(args[2]);
        er = compiler.writeSPIN(new PrintStream(oos));
        if(er!=null) {
            System.out.println(er);
            return;
        }  
    }

}
