package gx.g2.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPINEngineInfo {

	Map<String,String> defines = new HashMap<String,String>();
	Map<String,String> commands = new HashMap<String,String>();
	Map<String,List<String>> pastes = new HashMap<String,List<String>>();	

	public SPINEngineInfo(List<String> lines) {

		// Collect information:
		// - defines
		// - commands to include
		// - copy-paste information (comment out the source)
		for(int x=0;x<lines.size();++x) {
			String s = lines.get(x).trim();
			String t = parsePreprocess(s);
			if(t==null) continue;
			if(t.startsWith("define")) {
				int i = t.indexOf("=");
				if(i<0) {
					throw new RuntimeException("Missing '=' in define");
				}
				String a = t.substring(6,i).trim();
				String b = t.substring(i+1).trim();
				defines.put(a, b);
				continue;
			}
			if(t.startsWith("command ")) {
				int i = t.indexOf("=");
				if(i<0) {
					throw new RuntimeException("Missing '=' in command");
				}
				String a = t.substring(8,i).trim();
				String b = t.substring(i+1).trim();
				int j = b.indexOf(" ");
				if(j<0) j = b.length();
				defines.put(a, b.substring(0,j).trim());
				if(b.startsWith("YES")) {
					commands.put(a, b.substring(3).trim());					
				}				
				continue;
			}
			if(t.startsWith("copy ")) {
				String cn = t.substring(5).trim();				
				++x;
				List<String> pa = new ArrayList<String>();
				while(true) {
					String gg = parsePreprocess(lines.get(x));
					if(gg==null) {
						pa.add(lines.get(x));
						lines.set(x,"' #$$"+lines.get(x));
						++x;
						continue;
					}
					if(gg.startsWith("endcopy")) {
						++x;
						pastes.put(cn,pa);
						break;
					}
				}
				continue;
			}						
		}

	}
	
	public static String parsePreprocess(String s) {
		s = s.trim();
		if(!s.startsWith("'")) return null;
		s = s.substring(1).trim();
		if(!s.startsWith("#")) return null;
		s = s.substring(1).trim();
		int i = s.indexOf("//");
		if(i>=0) {
			s = s.substring(0,i).trim();
		}	
		return s;
	}

}
