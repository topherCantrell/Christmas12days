package gx.g2.core.sound;


import gx.CodeLine;
import gx.g2.Assemble;
import gx.g2.core.Cluster;
import gx.sound.SoundSequencerCommandFactory;

public class NOTECommandFactory implements SoundSequencerCommandFactory
{
	
	CodeLine originalLine;
	Cluster cluster;
	Assemble compiler;
	double ticTime;
	
	public NOTECommandFactory(Assemble compiler,double ticTime, CodeLine originalLine, Cluster cluster)
	{
		this.compiler = compiler;
		this.ticTime = ticTime;
		this.cluster = cluster;
		this.originalLine = originalLine;
	}

	@Override
	public String addNote(int voice, boolean tone, int note) {
		
		// A440 (MIDI Note 69) is note 46 in the table
		// note = midiNote - 23
		
		if(!tone) {
			
			if(note==-1) { // TRIGGER
				CodeLine nc = new CodeLine(originalLine,"TRIGGER");
				String er = compiler.compileLine(nc,cluster);
				if(er.length()!=0) return er;
				return null;				
			}
			
			if(note>0x1F) {
				note = 0x1F;
			}
			int vol = 4;
			int vc = voice;
			if(vc>2) {
				vc=vc-3;
				vc=vc+8;
				vc=vc+16;
			} else {
				vc=vc+8;
			}
			if(note==0) vol=0;
			CodeLine nc = null;
			if(voice>2) {
				nc = new CodeLine(originalLine,"REGISTER 22,"+note);				
			} else {
				nc = new CodeLine(originalLine,"REGISTER 6,"+note);
			}
			
			String er = compiler.compileLine(nc,cluster);
			if(er.length()!=0) return er;
			nc = new CodeLine(originalLine,"REGISTER "+vc+","+vol);
			er = compiler.compileLine(nc,cluster);
			if(er.length()!=0) return er;
			
			return null;		
		}
		
		if(note!=0) {
			note = note - 23;
			if(note<1 || note>96) {
				return "MIDI NOTE "+note+" NOT PLAYABLE.";
			}
		}
		
		//System.out.println("NOTECommandFactory.addNote("+voice+","+note+")");
		CodeLine nc = new CodeLine(originalLine,"NOTE "+voice+","+note);
		String er = compiler.compileLine(nc,cluster);
		if(er.length()!=0) return er;
		return null;
	}

	@Override
	public double addPause(double pauseTime) {
		//System.out.println("NOTECommandFactory.addPause("+pauseTime+")");
		// delayTime = (n+1)*ticTime
		// delayTime/ticTime = (n+1)
		// n = delayTime/ticTime - 1
		
		double n = pauseTime/ticTime - 1.0;
		long pause = Math.round(n);
		CodeLine nc = new CodeLine(originalLine,"PAUSE "+pause);
		String er = compiler.compileLine(nc,cluster);
		if(er.length()!=0) {
			throw new RuntimeException("Could not make 'PAUSE "+pause+"':"+er);
		}
		return (n+1)*ticTime;		
	}

}
