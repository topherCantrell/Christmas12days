package gx.g2.core.sound;

import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.core.Cluster;
import gx.g2.core.Command_DATA;
import gx.g3.struct.StructParser;
import gx.sound.SoundSequencer;
import gx.sound.SoundSequencerCommandFactory;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


// DEFAULT STACCATO
// DEFAULT TEMPO

public class Parse_MUSICStruct extends StructParser
{

	public Parse_MUSICStruct(int commandInfo) {
		super(commandInfo);		
	}

	@Override
	public String parse(String name, List<CodeLine> c, Cluster cluster, Map<String, String> subs, Assemble compiler) {
		String ss = name.trim().toUpperCase();
		if(!ss.equals("MUSIC")) {			
			return null; 
		}
		
		Command_DATA dc = new Command_DATA(c.get(0),cluster);
		cluster.commands.add(dc);
		
		SoundSequencer [] sequencers = new SoundSequencer[7];
		for(int x=0;x<sequencers.length;++x) {
			sequencers[x] = new SoundSequencer(x); 
		}
		int voice = 0;
		for(int x=1;x<c.size();++x) {
			CodeLine codeLine = c.get(x);
			String g = codeLine.text.substring(1).trim();
			String gg = g.toUpperCase();			
			if(gg.startsWith("VOICE ")) {
				boolean toneVoice = true;
				if(gg.endsWith("NOISE")) {
					toneVoice = false;
					g = g.substring(0,g.length()-5).trim();
				}
				g = g.substring(6).trim(); 
				if(g.equals("*")) g="6";
				try {
					voice = Integer.parseInt(g);
				} catch (Exception e) {
					return "Invalid VOICE '"+g+"'";
				}
				if(voice<0 || voice>=sequencers.length) {
					return "Invalid VOICE '"+g+"'";
				}				
				sequencers[voice].setToneVoice(toneVoice);
				continue;
			}
			
			if(gg.startsWith("STACCATO ") || gg.startsWith("TEMPO ") || gg.startsWith("PAUSE ")) {
				// Whole-line commands
				String er = sequencers[voice].parseSequenceTerm(gg);
				if(er!=null) return er;
				continue;
			}
			
			StringTokenizer st = new StringTokenizer(gg," |");
			while(st.hasMoreTokens()) {
				String er = sequencers[voice].parseSequenceTerm(st.nextToken());
				if(er!=null) return er;
			}						
		}
		
		double ticTime = 20480.0/80000000.0;
		SoundSequencerCommandFactory factory = new NOTECommandFactory(compiler,ticTime,c.get(0),cluster);
		String er = SoundSequencer.colate(sequencers, c.get(0),cluster,factory);
		if(er!=null) return er;
						
		return "";
	}

}
