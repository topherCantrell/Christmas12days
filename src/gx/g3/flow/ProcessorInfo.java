package gx.g3.flow; 

import java.util.*;
import java.io.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;

public class ProcessorInfo
{
    String name;
    String call;
    String rturn;
    String immediate;          
    String jump;
    
    String dataByte;
    String dataWord;
    String dataCharacterString;
    String immediateDataTag = "";           
    
    List<ProcessorConditionsInfo> processorConditionInfo = new ArrayList<ProcessorConditionsInfo>();
    
    List<String> modules = new ArrayList<String>();
    
    ProcessorSubstituteInfo subs = new ProcessorSubstituteInfo();
    
    
    
    public static List<ProcessorInfo> parseXML(InputStream is) throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder b = dbf.newDocumentBuilder();        
        
        Document d = b.parse(is);
        
        Element e = d.getDocumentElement();
        
        List<ProcessorInfo> processors = new ArrayList<ProcessorInfo>();
        
        NodeList o = e.getChildNodes();
        for(int x=0;x<o.getLength();++x) {
            Node oo = o.item(x);
            short t = oo.getNodeType();
            if(t!=Node.ELEMENT_NODE) continue;
            
            ProcessorInfo pi = new ProcessorInfo();
            processors.add(pi);
            
            NamedNodeMap pio = oo.getAttributes();
            Node r = pio.getNamedItem("name");
            if(r!=null) pi.name=r.getNodeValue();                                  
            r = pio.getNamedItem("jump");
            if(r!=null) pi.jump=r.getNodeValue();
            r = pio.getNamedItem("call");
            if(r!=null) pi.call = r.getNodeValue();
            r = pio.getNamedItem("return");
            if(r!=null) pi.rturn = r.getNodeValue();
            r = pio.getNamedItem("immediate");
            if(r!=null) pi.immediate = r.getNodeValue();
            r = pio.getNamedItem("immediateDataTag");
            if(r!=null) pi.immediateDataTag = r.getNodeValue();
                       
            //System.out.println("name=:"+pi.name+": comment="+pi.commentChar+" jump=:"+pi.jump+":");
            
            NodeList co = oo.getChildNodes();
            for(int y=0;y<co.getLength();++y) {
                Node ooo = co.item(y);
                if(ooo.getNodeType()!=Node.ELEMENT_NODE) continue;
                
                if(ooo.getNodeName().equals("Data")) {
                    NamedNodeMap uooa = ooo.getAttributes();
                    Node rr = uooa.getNamedItem("byte");
                    if(rr!=null) pi.dataByte = rr.getNodeValue();
                    rr = uooa.getNamedItem("word");
                    if(rr!=null) pi.dataWord = rr.getNodeValue();
                    rr = uooa.getNamedItem("string");
                    if(rr!=null) pi.dataCharacterString = rr.getNodeValue();                    
                    continue;
                }
                
                if(ooo.getNodeName().equals("Modules")) {
                	NodeList uoou = ooo.getChildNodes();
                    for(int z=0;z<uoou.getLength();++z) {
                        Node uoo = uoou.item(z);
                        if(uoo.getNodeType()!=Node.ELEMENT_NODE) continue;
                    
                        if(uoo.getNodeName().equals("Module")) {
                        	String k = uoo.getTextContent().trim();
                        	pi.modules.add(k);
                        }
                    }                	
                }
                
                if(ooo.getNodeName().equals("Substitutions")) {                    
                    NodeList uoou = ooo.getChildNodes();
                    for(int z=0;z<uoou.getLength();++z) {
                        Node uoo = uoou.item(z);
                        if(uoo.getNodeType()!=Node.ELEMENT_NODE) continue;
                    
                        if(uoo.getNodeName().equals("List")) {
                            NamedNodeMap uooa = uoo.getAttributes();
                            Node rr = uooa.getNamedItem("name");
                            String k = rr.getNodeValue().trim();
                            rr = uooa.getNamedItem("entries");
                            String v = rr.getNodeValue().trim();
                            pi.subs.sublistNames.add(k);
                            pi.subs.sublistEntries.add(v);
                        } else if(uoo.getNodeName().equals("Sub")) {NamedNodeMap uooa = uoo.getAttributes();
                            Node rr = uooa.getNamedItem("key");
                            String k = rr.getNodeValue().trim();
                            rr = uooa.getNamedItem("code");
                            String v = rr.getNodeValue().trim();
                            pi.subs.substituteKey.add(k);
                            pi.subs.substituteCode.add(v);
                            pi.subs.subKeys.add(k);
                            pi.subs.subCodes.add(v);
                        }                        
                    }
                    
                    // Go ahead and expand the lists to make for faster
                    // substitutions later
                    for(int xx=0;xx<pi.subs.sublistNames.size();++xx) {
                        String k = (String)pi.subs.sublistNames.get(xx);
                        boolean changed=true;
                        while(changed) {
                            changed = false;
                            for(int yy=0;yy<pi.subs.substituteKey.size();++yy) {
                                String a = (String)pi.subs.substituteKey.get(yy);
                                String gggt = (String)pi.subs.substituteCode.get(yy);
                                if(a.indexOf("@"+k+"@")>=0) {
                                    String bb = (String)pi.subs.sublistEntries.get(xx);
                                    StringTokenizer zt = new StringTokenizer(bb,";");
                                    while(zt.hasMoreTokens()) {
                                        String sst = zt.nextToken();
                                        String aat = BlendConfig.replaceAtTag("@"+k+"@", a, sst);
                                        pi.subs.substituteKey.add(aat);
                                        aat = BlendConfig.replaceAtTag("@"+k+"@", gggt, sst);
                                        pi.subs.substituteCode.add(aat);                                        
                                    }      
                                    pi.subs.substituteKey.remove(yy);
                                    pi.subs.substituteCode.remove(yy);
                                    changed = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    boolean changed = true;
                    while(changed) {
                        changed = false;
                        for(int xx=0;xx<pi.subs.substituteKey.size()-1;++xx) {
                            String aa = (String)pi.subs.substituteKey.get(xx);
                            String bb = (String)pi.subs.substituteCode.get(xx);
                            String cc = (String)pi.subs.substituteKey.get(xx+1);
                            String dd = (String)pi.subs.substituteCode.get(xx+1);
                            if(aa.indexOf("@")>=0 && cc.indexOf("@")<0) {
                                pi.subs.substituteKey.set(xx,cc);
                                pi.subs.substituteKey.set(xx+1,aa);
                                pi.subs.substituteCode.set(xx,dd);
                                pi.subs.substituteCode.set(xx+1,bb);
                                changed = true;                                
                            }
                        }
                    }
                    
                    continue;
                }
                
                ProcessorConditionsInfo cis = new ProcessorConditionsInfo();
                pi.processorConditionInfo.add(cis);
                
                NamedNodeMap ciso = ooo.getAttributes();
                Node rr = ciso.getNamedItem("left");
                if(rr!=null && rr.getNodeValue().equals("true")) cis.leftRequired=true;
                rr = ciso.getNamedItem("right");
                if(rr!=null && rr.getNodeValue().equals("true")) cis.rightRequired=true;
                
                //System.out.println("leftRequired="+cis.leftRequired+" rightRequired="+cis.rightRequired);
                
                NodeList cco = ooo.getChildNodes();
                for(int z=0;z<cco.getLength();++z) {
                    Node oooo = cco.item(z);
                    if(oooo.getNodeType()!=Node.ELEMENT_NODE) continue;
                    
                    if(oooo.getNodeName().equals("Compare")) {
                        ProcessorCompareInfo pci = new ProcessorCompareInfo();
                        cis.compares.add(pci);
                        
                        NamedNodeMap nnm = oooo.getAttributes();
                        Node a = nnm.getNamedItem("left");
                        if(a!=null) pci.left = a.getNodeValue();
                        a = nnm.getNamedItem("right");
                        if(a!=null) pci.right = a.getNodeValue();
                        a = nnm.getNamedItem("code");
                        if(a!=null) pci.code = a.getNodeValue();
                        
                        //System.out.println("left=:"+pci.left+": right=:"+pci.right+": code=:"+pci.code+":");
                        
                    } else if(oooo.getNodeName().equals("Condition")) {
                        ProcessorConditionInfo pcoi = new ProcessorConditionInfo();
                                                
                        NamedNodeMap nnm = oooo.getAttributes();
                        Node a = nnm.getNamedItem("symbol");
                        if(a!=null) pcoi.symbol = a.getNodeValue();
                        //System.out.println(":"+pcoi.symbol+":");
                        
                        // Sort these so that the longer symbols are at the
                        // beginning. That way we will match "<=" instead of "<" .
                        if(pcoi.symbol.length()==1) {
                            cis.conditions.add(pcoi);
                        } else {
                            cis.conditions.add(0,pcoi);
                        }
                        
                        NodeList pco = oooo.getChildNodes();
                        for(int tt=0;tt<pco.getLength();++tt) {
                            Node to = pco.item(tt);
                            if(to.getNodeType()!=Node.ELEMENT_NODE) continue;
                            if(to.getNodeName().equals("Code")) {
                                NodeList tto = to.getChildNodes();
                                String aa = "";
                                for(int zt=0;zt<tto.getLength();++zt) {
                                    Node zto = tto.item(zt);
                                    if(zto.getNodeType()!=Node.TEXT_NODE) continue;
                                    aa = aa + zto.getNodeValue().trim();
                                }               
                                //System.out.println(":"+aa+":");
                                pcoi.codes.add(aa);
                            }
                        }
                        
                    }
                }
                
            }
            
        }
        return processors;
    }    
    
}
