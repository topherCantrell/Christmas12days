package gx.g3.flow; 

import java.util.*;
import java.io.*;

/*
 
 Structure are defined with the following syntax:
 
 StructureDefinition NNNN {
   i Member1
   i Member2
 }
 
 NNNN is the unique structure name
 i is the number of bytes a member takes up
 Member1 is the name of the member
 Any number of members can be added to the structure
 
 You tell the tool about how registers map to structures as follows:
 
 StructureUsing U is NNNN
 
 Where U is the register name and NNN is the structure name.
 
 Then you can access members of the structure with the "->" operator as follows:
 
 A = U->Member1 
 
 You can disassociate a structure from a register with the following syntax:
 
 StructureUsing U is *
 
 Structure associations created within a subroutine are automatically cleared 
 at the end of a subroutine.
 
 Global variables can be structured as in:
 AA rmb sizeof(NNNN) StructureUsage AA is NNNN
 
 Structure associations created outside of a subroutine are assumed to be
 fixed-memory pointers and will be coded as memory offsets in the
 expanded assembly.
 
 You can use the size-of operator to return substitue in the immediate
 size of an entire structure or of a member of a structure as follows:
 
 A = #sizeof(NNNN)
 B = #sizeof(NNNN->Member1)
 
 Inheritance is supported as follows:
 
 StructureDefinition NNNN : BBBB {
   2 NewMember
 }
 
 Where NNNN is the derived structure from the base BBBB. The new structure
 includes all of the members of the base with the new members added to
 the end.
 
 */

public class Structures
{
    
    static Map<String,StructureDefinition> structureDefinitions = new HashMap<String, StructureDefinition>();
    
    public static void main(String [] args) throws Exception
    {
        Reader r = new FileReader(args[0]);
        BufferedReader br = new BufferedReader(r);        
        List<Line> codeOrg = new ArrayList<Line>();        
        while(true) {
            String g = br.readLine();
            if(g==null) break;     
            Line aa = new Line(g);            
            codeOrg.add(aa);
        }      
        br.close();
        
        BlendConfig.initFlowConfig(null);
        
        main(codeOrg);    
        
        // Store the processed code in the output file
        OutputStream os = new FileOutputStream(args[1]);
        PrintStream ps = new PrintStream(os);
        Line.linesToStream(codeOrg,ps);               
        ps.flush();
        ps.close();
    }
    
    public static String replace(String register, int offset, boolean global)
    {
        
        if(global) {
            return ""+register+"+"+offset;
        }
        
        // TOPHER ... define and read XML
        /*
         
         <StructureDereference>
           <Base name="X" substitution="@OFFSET@,X"/>
           <Base name="Y" substitution="@OFFSET@,Y"/>
           <Base name="U" substitution="@OFFSET@,U"/>
           <Base name="S" substitution="@OFFSET@,S"/>
           <Base name="D" substitution="@OFFSET@,D"/>
         </StructureDereference>
         
         */
        // TRICKY TO DEFINE IN XML
        
        // @BASE@ @OFFSET@ -- "@OFFSET@,@BASE@"
        return ""+offset+","+register;
    }
    
    public static void main(List<Line> code) throws Exception
    {
        
        // First pass ... find the StructureDefinitions
        int x = 0;
        while(x<code.size()) {
            Line a = (Line)code.get(x);
            String s = a.rawNoComment;              
            ++x;
            int i = s.indexOf("StructureDefinition");
            if(i<0) continue;            
            a.commentOutWholeLine();
            
            int co = s.indexOf(":",i);
            StructureDefinition bs = null;
            if(co>0) {
                StringTokenizer st=new StringTokenizer(s.substring(co+1));
                String base = st.nextToken();
                bs = (StructureDefinition)structureDefinitions.get(base);
                if(bs==null) {
                    throw new RuntimeException("No definition for base-structure '"+base+"' : "+a.raw);
                }
            }
                        
            StringTokenizer st = new StringTokenizer(s.substring(i));
            st.nextToken();
            String name = st.nextToken();
            List<String> e = new ArrayList<String>();
            while(true) {
                a = (Line)code.get(x++);
                s = a.rawNoComment;                  
                if(s.startsWith(";")) continue;
                if(s.length()==0) continue;
                a.commentOutWholeLine();                
                if(s.startsWith("}")) {                   
                    break;
                }
                e.add(s); 
            }
            StructureDefinition sd = new StructureDefinition(bs,name,e);
            Object o = structureDefinitions.put(name,sd);
            if(o!=null) {
                throw new RuntimeException("Redefinition of Structure '"+name+"' : "+a.raw);
            }
        }
        
        Map<String,StructureDefinition> globalUsing = new HashMap<String, StructureDefinition>();
        Map<String,StructureDefinition> using = new HashMap<String, StructureDefinition>();
        boolean insideFunction = false;
        
        // Second pass ... resolve them
        for(x=0;x<code.size();++x) {
            Line a = (Line)code.get(x);
            String s = a.raw;
            if(s.indexOf("--SubroutineContextBegins--")>0) {
                insideFunction = true;
            }
            if(s.indexOf("--SubroutineContextEnds--")>0) {
                insideFunction = false;
                using.clear();
            }
            String ss = a.rawNoComment;            
            int i = ss.indexOf("StructureUsage");
            if(i>=0) {
                ss=ss.substring(i);
                //System.out.println(">>"+ss+"<<");
                i=s.indexOf("StructureUsage");
                a.parse(s.substring(0,i)+"; "+s.substring(i));
                StringTokenizer st = new StringTokenizer(ss);
                st.nextToken();
                String reg = st.nextToken();
                String type = st.nextToken();
                if(type.toUpperCase().equals("IS")) type = st.nextToken();
                if(type.equals("*")) {
                    if(insideFunction) {
                        using.remove(reg);  
                    } else {
                        globalUsing.remove(reg);  
                    }                                      
                } else {
                    StructureDefinition sd = (StructureDefinition)structureDefinitions.get(type);
                    if(sd==null) {
                        throw new RuntimeException("Undefined Structure '"+type+"' : "+a.raw);
                    }
                    if(insideFunction) {
                        using.put(reg,sd);
                    } else {
                        globalUsing.put(reg,sd);
                    }                     
                }
                --x; // There might be a sizeof on this line too
                continue;
            }
            i = ss.indexOf("sizeof(");
            if(i>0) {
                String field = null;
                i = s.indexOf("sizeof(");
                int j=s.indexOf(")",i);
                String con = s.substring(i+7,j);
                int ii = con.indexOf("->");
                if(ii>0) {
                    field = con.substring(ii+2);
                    con = con.substring(0,ii);
                }
                
                StructureDefinition sd = (StructureDefinition)structureDefinitions.get(con);
                if(sd==null) {
                    throw new RuntimeException("Undefined Structure '"+con+"' : "+a.raw);
                }
                
                int size=-1;
                if(field!=null) {                    
                    for(int y=0;y<sd.entries.length;++y) {
                        if(sd.entries[y].equals(field)) {
                            size = sd.entrySize[y];
                            break;
                        }
                    }
                    if(size<0) {
                        throw new RuntimeException("No entity '"+field+"' in structure definition for '"+sd.name+"' : "+a.raw);
                    }
                } else {
                    size = sd.totalSize;
                }
                
                //String rep = BlendConfig.replaceAtTag("@RIGHT@",BlendConfig.immediate,""+size);
                String rep = ""+size;
                a.parse(s.substring(0,i)+rep+s.substring(j+1) );              
            
                continue;
            }
            
            i = ss.indexOf("->");
            if(i>0) {
                i = s.indexOf("->");
                int vs = i-1;
                String stu = s.toUpperCase();
                while(true) {
                    if(vs==0) {
                        --vs;
                        break;                    
                    }
                    char g = stu.charAt(vs);
                    if( !(g>='A' && g<='Z') && !(g>='0' && g<='9') && g!='_') break;                                     
                    --vs;
                }
                ++vs;
                int replaceStart = vs;
                String reg = s.substring(vs,i);  
                i = i+2;
                vs = i;
                while(true) {
                    if(vs==stu.length()) break;                    
                    char g = stu.charAt(vs);
                    if((g<'A' || g>'Z') && g!='_') break;
                    ++vs;
                }                
                String value = s.substring(i,vs);
                boolean global = false;
                StructureDefinition sd = (StructureDefinition)using.get(reg);
                if(sd==null) {
                    sd = (StructureDefinition)globalUsing.get(reg);
                    global = true;
                }
                if(sd==null) {
                    throw new RuntimeException("No USING for '"+reg+"' : "+a.raw);
                }
                int fnd = -1;
                for(int y=0;y<sd.entries.length;++y) {
                    if(sd.entries[y].equals(value)) {
                        fnd = y;
                        break;
                    }
                }
                if(fnd<0) {
                    throw new RuntimeException("No entity '"+value+"' in structure definition for '"+sd.name+"' : "+a.raw);
                }
                value = replace(reg,sd.offsets[fnd],global);
                String re = s.substring(0,replaceStart)+value+s.substring(vs);
                a.parse(re);                
                
            }
        }
    }
    
}

