package gx.g2.core;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ConfigureCORE {

	public static void main(String[] args) throws Exception 
	{
		FileReader fr = new FileReader(args[0]);
		BufferedReader br = new BufferedReader(fr);
		
		List<String> lines = new ArrayList<String>();
		
		// Read in lines. Remove and lines that were auto-added by last configure.
		// Un-comment any lines commented out in last configure.
		while(true) {
			String g = br.readLine();
			if(g==null) break;
			if(g.trim().endsWith("' #%%")) {
				// These were added by the last configure ... leave them out
				continue;
			}
			if(g.trim().startsWith("' #$$")) {
				// These were commented out by last config ... put them back in
				g=g.trim().substring(5);				
			}
			lines.add(g);			
		}		
		br.close();
		
		SPINEngineInfo engineInfo = new SPINEngineInfo(lines);		
		
		// Fill in substitutions
		for(int x=0;x<lines.size();++x) {
			String s = lines.get(x);
			int i = s.indexOf("' #@");
			if(i<0) continue;
			int j = s.indexOf("@",i+4);
			String def = s.substring(i+4,j);
			String val = engineInfo.defines.get(def);
			if(val==null) {
				throw new RuntimeException("Could not find define '"+def+"'");
			}			
			while(s.charAt(i-1)==' ') --i;
			j = i;
			while(s.charAt(j-1)!=' ' && s.charAt(j-1)!='#') --j;
			String t = s.substring(0,j)+val+s.substring(i);
			lines.set(x, t);
		}
		
		// Handle pastes (mark the end of the lines that they were pasted)
		for(int x=0;x<lines.size();++x) {
			String s = lines.get(x).trim();
			String t = SPINEngineInfo.parsePreprocess(s);
			if(t==null) continue;
			
			if(t.startsWith("paste ")) {
				t = t.substring(6).trim();
				List<String> st = engineInfo.pastes.get(t);
				if(st==null) {
					continue;
					//throw new RuntimeException("Could not find paste '"+t+"'");
				}
				for(String ss : st) {
					++x;
					lines.add(x,ss+"' #%%");					
				}
			}
		}
		
		// Comment out commands
		Map<String,Integer> commandIndex = new HashMap<String,Integer>();
		int ni = 0;
		for(int x=0;x<lines.size();++x) {
			String s = lines.get(x);
			if(s.endsWith("' #command-table")) {
				int i = s.indexOf("#core");
				int j = s.indexOf(" ",i);
				String c = s.substring(i+1,j);
				String v = engineInfo.commands.get(c);
				if(v==null) {
					lines.set(x, "' #$$"+lines.get(x));
				} else {
					commandIndex.put(c, ni++);
				}
				continue;
			} 
			String t = SPINEngineInfo.parsePreprocess(s);
			if(t==null || !t.startsWith("command-begin")) continue;
			t = lines.get(x+1).trim();
			if(engineInfo.commands.get(t)!=null) continue;
			x=x+2;
			while(true) {
				t = SPINEngineInfo.parsePreprocess(lines.get(x));
				if(t!=null && t.startsWith("command-end")) break;
				lines.set(x, "' #$$"+lines.get(x));
				++x;
			}
		}
		
		// Comment out lines based on IF
		for(int x=0;x<lines.size();++x) {
			String s = lines.get(x).trim();
			String t = SPINEngineInfo.parsePreprocess(s);
			if(t==null) continue;
			
			if(t.startsWith("if(")) {
				t = t.substring(3,t.length()-1).trim();
				boolean b = false;
				if(t.indexOf("||")>0) {
					b = false;
					StringTokenizer st = new StringTokenizer(t,"||");
					while(st.hasMoreElements()) {
						String t1 = st.nextToken().trim();
						b = b | resolveTerm(t1,engineInfo.defines);
					}					
				} else {					
					if(t.indexOf("&&")>0){
						b = true;
						StringTokenizer st = new StringTokenizer(t,"&&");
						while(st.hasMoreElements()) {
							String t1 = st.nextToken().trim();
							b = b & resolveTerm(t1,engineInfo.defines);
						}	
					} else {
						b = resolveTerm(t,engineInfo.defines);
					}
				}
				++x;
				if(!b) {
					while(true) {
						t = SPINEngineInfo.parsePreprocess(lines.get(x));
						if(t!=null && t.startsWith("endif")) break;
						lines.set(x, "' #$$"+lines.get(x));
						++x;
					}
				}
			}
		}
		
		int stackSize = Integer.parseInt(engineInfo.defines.get("CALL_STACK_SIZE"));		
		for(int x=0;x<lines.size();++x) {
			String s = lines.get(x).trim();
			if(s.startsWith("coreStack ")) {
				s = "coreStack          long  0";
				for(int y=1;y<stackSize;++y) {
					s=s+",0";
				}
				lines.set(x,s);
				break;
			}
		}
				
		// Count threads and create thread memory
		int numThreads = Integer.parseInt(engineInfo.defines.get("MAXTHREADS"));
		if(numThreads>1) {
			for(int x=0;x<lines.size();++x) {
				String s = lines.get(x).trim();
				if(s.startsWith("coreThreadSpecific")) {
					int tc = 0;
					++x;
					while(true) {
						String ss = lines.get(x++).trim();
						if(ss.startsWith("'") || ss.length()==0) continue;
						if(ss.startsWith("coreThreads")) break;
						++tc;
					}
					for(int y=0;y<numThreads;++y) {
						String g = "    long 0";
						for(int z=1;z<tc+stackSize-1;++z) {
							g=g+",0";
						}
						lines.add(x++,g+"' #%%");
					}
					break;
				}
			}
		}
		
		// Rename old file
		File f = new File(args[0]);
		File f2 = new File(args[0]+".bak");
		f2.delete();
		f.renameTo(f2);
		
		// Write updated file
		OutputStream os = new FileOutputStream(args[0]);
		PrintStream ps = new PrintStream(os);
		for(String s : lines) {
			ps.print(s+"\r\n");
		}
		ps.flush();
		ps.close();
		
	}
	
	static String [] ops = {"==","!=",">=","<=","<",">"};
	private static boolean resolveTerm(String term,Map<String,String> defines) 
	{
		int opid = -1;
		int ti = -1;
		for(int x=0;x<ops.length;++x) {
			ti = term.indexOf(ops[x]);
			if(ti>0) {
				opid = x;
				break;
			}								
		}
		if(opid<0) {
			throw new RuntimeException("Unknown IF op '"+term+"'");
		}
		String a = term.substring(0,ti);
		String b = term.substring(ti+ops[opid].length());
		String aa = defines.get(a);
		if(aa!=null) a = aa;
		String bb = defines.get(b);
		if(bb!=null) b = bb;
		int ai = 0;
		int bi = 0;
		if(opid>1) {
			ai = Integer.parseInt(a);
			bi = Integer.parseInt(b);
		}
		switch(opid) {
		case 0:
			return a.equals(b);
		case 1:
			return !a.equals(b);
		case 2:
			if(ai>=bi) return true;
			return false;
		case 3:
			if(ai<=bi) return true;
			return false;
		case 4:
			if(ai<bi) return true;
			return false;
		case 5:
			if(ai>bi) return true;
			return false;
		}
		throw new RuntimeException("Software error");
	}

}
