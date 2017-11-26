package gx.g3.flow; 

import java.util.*;

public class StructureDefinition
{
    
    String name;
    String [] entries;
    int [] offsets;
    int [] entrySize;
    int totalSize;
    
    StructureDefinition(StructureDefinition base, String name, List<String> e)
    {
        this.name = name;
        int ensize = e.size(); 
        if(base!=null) {
            ensize = ensize+base.entries.length;
        }
        entries = new String[ensize];
        entrySize = new int[ensize];
        offsets = new int[ensize];
        
        int xo = 0;
        int co = 0;
        
        if(base!=null) {
            for(int x=0;x<base.entries.length;++x) {
                entrySize[x] = base.entrySize[x];
                entries[x] = base.entries[x];
                offsets[x] = base.offsets[x];
            }
            co = base.totalSize;
            xo = base.entries.length;
        }
        
        for(int x=0;x<e.size();++x) {
            String a = (String)e.get(x);
            StringTokenizer st = new StringTokenizer(a);
            //System.out.println(">"+a+"<");
            int size = Integer.parseInt(st.nextToken());
            entrySize[x+xo] = size;
            entries[x+xo] = st.nextToken();
            offsets[x+xo] = co;            
            co = co + size;
        }                
        totalSize=co;
    } 
    
}
