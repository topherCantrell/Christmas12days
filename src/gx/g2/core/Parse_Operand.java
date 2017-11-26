package gx.g2.core;

import gx.CodeLine;

import java.util.Map;
import java.util.StringTokenizer;

public class Parse_Operand 
{
	
	public static String parse(String s, Command_MULTIOP com, Map<String,String> subs)
	{
		Command_OP co = new Command_OP();    		
        String er = Parse_Operand.parse(s, co, subs);            
		if(er!=null) return er;    
		com.operands.add(co);
		return null;
	}
		
	public static String parse(String s, Command_OP operand, Map<String,String> subs)
	{
						
		// @Label             - Numeric constant (offset to label in code segment) 16 bit		
		// 0x7400             - Numeric constant (4, 12, or 16 bit)
		// -1                 - Numeric constant (sign extended 16 bit)		
		// $44                - Byte access from location 44
		// WORD$Y             - "WORD" means word access from location Y (defined)
		// CODE$0x7400        - CODE read byte from code segment
		// WORD$SYS1$DEF      - Read word from SYS1 segment
		// SYS2[X]            - [..] X is a 2-byte pointer in data segment ... indirect byte access
		// WORD[24]           - indirect word access						
		
		// 0000-00ss                    - segment prefix (CODE, SYS1, SYS2, SYS3)
		// 0001-llll                    - 4-bit constant
		// 0010-mmmm llllllll           - 12-bit constant
		// 0011-0000 llllllll mmmmmmmm  - 16-bit constant
		// 0100-aaaa aaaaaaaa           - Byte access
		// 0101-aaaa aaaaaaaa           - Word access
		// 0110-aaaa aaaaaaaa           - Indirect byte access
		// 0111-aaaa aaaaaaaa           - Indirect word access
		
		s = s.trim().toUpperCase();
		
		if(s.startsWith("@")) {
			// 16-bit constants consume three bytes
			operand.data.add(0x30);  // 0011_0000
			operand.data.add(0);     // llllllll
			operand.data.add(0);     // mmmmmmmm
			s=s.substring(1).trim();
			if(subs!=null) {
				String t = subs.get(s.toUpperCase());
				if(t!=null) s = t;
			}
			operand.label = s;
			operand.type = Parse_OperandType.LABEL;
			return null;     // OK (at least until we resolve the label)
		}		
		
		int segment = -1;	
		boolean word = false;
		boolean memory = false;
		
		// If there is a '$' or a '[' then this is NOT a constant
		if(s.indexOf("$")>=0 || s.indexOf("[")>=0) {
			memory = true;
		}
		
		StringTokenizer st = new StringTokenizer(s,"$[");
		while(st.hasMoreTokens()) {
			String g = st.nextToken().trim();
			if(g.equals("WORD")) {
				word = true;
			} else if(g.equals("CODE")) {
				segment = 0;
			} else if(g.equals("SYS1")) {
				segment = 1;
			} else if(g.equals("SYS2")) {
				segment = 2;
			} else if(g.equals("SYS3")) {
				segment = 3;
			} else {
				s = g;
			}
		}
		
		// Handle memory access (direct and indirect)
		if(memory) {		
			int baseCom = 0x04;
			if(s.endsWith("]")) {
				operand.type = Parse_OperandType.INDIRECT_BYTE;
				baseCom = 0x06;				
				s = s.substring(0,s.length()-1).trim();
				if(subs!=null) {
					String t = subs.get(s.toUpperCase());
					if(t!=null) s = t;
				}
			} else {
				operand.type = Parse_OperandType.BYTE;
				if(subs!=null) {
					String t = subs.get(s.toUpperCase());
					if(t!=null) s = t;
				}
			} 
			if(word) {
				if(operand.type == Parse_OperandType.BYTE) {
					operand.type = Parse_OperandType.WORD;
				} else {
					operand.type = Parse_OperandType.INDIRECT_WORD;
				}
				++baseCom;
			}
			
			// Add any segment prefix
			if(segment>=0) {
				operand.data.add(segment);
			}
			
			// Get the constant address
			long val = 0;
			try {
				val = CodeLine.parseNumber(s);
			} catch (Exception e) {
				return "Invalid address '"+s+"'";
			}
			if(val<0 || val>4095) {
				return "Address '"+s+"' must be 0-4095";
			}
			
			// 12-bit address
			baseCom = (baseCom<<4)  | ((int)(val>>8)); // cccc_mmmm
			operand.data.add(baseCom);
			operand.data.add((int)(val&255)); // llllllll
			return null;
		}
		
		// Not an @label or a memory access ... we assume it is a constant
		operand.type = Parse_OperandType.CONSTANT;
		
		if(segment>=0) {
			return "Numeric constant can't have a segment prefix.";
		}		
		if(word) {
			return "Numeric constants can't have a WORD specifier.";
		}
		
		if(subs!=null) {
			String t = subs.get(s.toUpperCase());
			if(t!=null) s = t;
		}
		
		// Get the constant address
		long val = 0;
		try {
			val = CodeLine.parseNumber(s);
		} catch (Exception e) {
			return "Invalid numeric constant '"+s+"'";
		}
		
		// Negative numbers are 16 bit sign extended
		if(val<0) {
			if(val<-32768) {
				return "Negative numbers must be >= -32768";
			}
			val = 65536+val;			
		}
		
		// We have modes for 16-bit constants 
		if(val>65535) {
			return "Constant '"+s+"' must be 0-65535";
		}			
		if(val<16) {
			// Very small constants fit in one byte
			operand.data.add( 0x10 | (int)(val)); // 0_011_vvvv	
		} else if(val<4096) {
			// 12-bit constants fit in two bytes
			operand.data.add(0x20 | (int)(val>>8)); // 0_100_mmmm
			operand.data.add((int)(val&255));  // llllllll
		} else {
			// 16-bit constants consume three bytes			
			operand.data.add(0x30);  // 0_101_0000
			operand.data.add((int)(val&255));  // llllllll
			operand.data.add((int)(val>>8));   // mmmmmmmm				
		}	
		
		operand.valueIfConstant = (int) val;
				
		return null; // OK
	}

}
