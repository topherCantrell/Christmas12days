package gx.sound;
/**
 * Copyright (c) Chris Cantrell 2009
 * All rights reserved.
 * ccantrell@knology.net
 */

import gx.CodeLine;
import gx.g2.core.Cluster;

import java.util.*;

public class SoundSequencer
{
    
    int lastOctave = 4;
    double lastLength = 1.0;
    double tieLength = 0.0; 
    
    int myVoice;
    
    int termNumber = 0;
    
    double staccatoValue = 0.90;
    boolean staccatoIsPercent = true;
    
    double currentTime = 0.0;     
    
    List<SoundSequencerEvent> events = new ArrayList<SoundSequencerEvent>();
    
    double tempo = 120.0;  // Whole-notes per minute (one whole note in 4 seconds)
    
    boolean toneVoice;
    
    public void setToneVoice(boolean toneVoice)
    {
    	this.toneVoice = toneVoice;
    }
    
    public static double midiFrequency(int midiNote)
    {
        // A4 (our notation) is A440 on the piano (MIDI note #69)      
        double n = midiNote - 69; // Integral offset from A4
        n = n / 12.0;
        n = Math.pow(2.0,n) * 440.0;        
        return n;
    }
    
    public SoundSequencer(int voice)
    {
        myVoice = voice;        
    }
    
    void finalPause()
    {
        SoundSequencerEvent ev = new SoundSequencerEvent(toneVoice);
        ev.startTime = currentTime;
        events.add(ev);        
    }
    
    public String parseSequenceTerm(String term)
    {
        ++termNumber;
        //System.out.println("::::"+termNumber+"::"+term+"::::");          
        
        // Pause ... just advance the currentTime
        if(term.startsWith("PAUSE ")) {            
            term = term.substring(6);
            try {
                double i = Double.parseDouble(term);
                i = i / 1000.0;
                currentTime = currentTime + i;
                return null;
            } catch (Exception e) {
                return "Invalid pause value '"+term+"'";
            }                        
        }
        
        if(term.startsWith("TEMPO ")) {            
            term = term.substring(6).trim();            
            try {
                tempo = Double.parseDouble(term);
                if(tempo<=0) {
                    return "Invalid tempoo '"+term+"'";
                }                
            } catch (Exception e) {
                return "Invalid tempo '"+term+"'";
            }
            return null;
        }
        
        // Staccato<...> ... either percent or time
        if(term.startsWith("STACCATO ")) {            
            term = term.substring(9).trim();
            if(term.endsWith("%")) {
                try {
                    staccatoValue = Double.parseDouble(term.substring(0,term.length()-1));
                    if(staccatoValue<0 || staccatoValue>100) {
                        return "Staccato percentage must be between 0 and 100.";
                    }
                    staccatoValue = staccatoValue/100.0;
                    staccatoIsPercent = true;
                    return null; 
                } catch (Exception e) {
                    return "Invalid staccato percentage '"+term+"'";
                }
            }
            if(!term.endsWith("MS")) {
                return "Staccato value must end with '%' or 'MS'.";
            }
            term = term.substring(0,term.length()-2);
            try {
                staccatoValue = Double.parseDouble(term);
                if(staccatoValue<0 || staccatoValue>100) {
                    return "Staccato time must be greater than 0.";
                }
                staccatoValue = staccatoValue/1000.0;
                staccatoIsPercent = false;
                return null; 
            } catch (Exception e) {
                return "Invalid staccato time '"+term+"'";
            }                       
        }
                        
        int i = 0;
        char cc = 0;
        // Get note length
        while(true) {
        	if(i==term.length()) {
                return "Excpected an A,B,C,D,E,F,G, or R";
            }
            cc = term.charAt(i);
            if(cc=='R' || cc=='X' || (cc>='A' && cc<='G')) break;
            ++i;
        }
        if(cc=='D') { // It is a 'double' note length if another note name follows
            if((i+1)<term.length()) {
                if(term.charAt(i+1)=='R' || (term.charAt(i+1)>='A' && term.charAt(i+1)<='G')) {                    
                    ++i;
                    cc = term.charAt(i);
                }
            }
        }
        String noteLengthS = term.substring(0,i);        
        
        double noteLength = lastLength;
        if(i>0) {
            int j = 0;
            while(j!=noteLengthS.length()) {
                if(noteLengthS.charAt(j)<'0' || noteLengthS.charAt(j)>'9') {
                    break;
                }      
                ++j;
            }
            if(j==0) {
                return "Invalid note length '"+noteLengthS+"'";
            }
            noteLength = 1.0 / Double.parseDouble(noteLengthS.substring(0,j));
            if(j!=noteLengthS.length()) {
                if(noteLengthS.charAt(j)=='.') {
                    // Only single dots for now
                    noteLength = noteLength + noteLength/2.0;
                    ++j;
                }
            }
            if(j!=noteLengthS.length()) {
                if(noteLengthS.charAt(j)=='T') {
                    noteLength = noteLength*2.0/3.0;
                    ++j;
                } else if(noteLengthS.charAt(j)=='D') {
                    noteLength = noteLength*3.0/2.0;
                    ++j;
                }
            }
            
            if(j!=noteLengthS.length()) {
                return "Extra characters in note length '"+noteLengthS+"'";
            }
            
        }
        
        // Note value (key of C)
        int noteVal = 0;
        switch(cc) {
            case 'C': noteVal = -9; break;                
            case 'D': noteVal = -7; break;
            case 'E': noteVal = -5; break;
            case 'F': noteVal = -4; break;
            case 'G': noteVal = -2; break;
            case 'A': noteVal = 0; break;
            case 'B': noteVal = 2; break;
            case 'X': noteVal = 10000; break;
        }    
        
        ++i;
        
        // Get absolute octave (or use last)        
        int noteOctave = lastOctave;
        
        if(cc!='R') { // Only notes have accidentals and octaves
            
            // Absolute octave
            if(i!=term.length()) {
                if(term.charAt(i)>='0' && term.charAt(i)<='9') {
                    noteOctave = term.charAt(i)-'0';
                    ++i;
                }
            }
            
            // Octave offsets
            while(i!=term.length()) {
                if(term.charAt(i)=='+') {
                    ++noteOctave;
                } else if(term.charAt(i)=='-') {
                    --noteOctave;
                } else {
                    break;
                }
                ++i;
            }
            
            // Accidentals
            while(i!=term.length()) {
                if(term.charAt(i)=='#') {
                    ++noteVal;
                } else if(term.charAt(i)=='B') {
                    --noteVal;
                } else {
                    break;
                }
                ++i;
            }
            
        }
        
        // Tie
        boolean tie = false;
        if(i!=term.length()) {
            if(term.charAt(i)=='_') {
                tie = true;
                ++i;
            }
        }
        
        // Make sure that's all
        if(i!=term.length()) {
            return "Extra note characters '"+term.substring(i)+"'";
        }
        
        // Remember length and octave for next note if not specified
        lastLength = noteLength;
        lastOctave = noteOctave;
        
        // Note length now in seconds
        noteLength = noteLength * 60.0/tempo;        
        
        // Actual midi note number
        if(cc!='R' && cc!='X') {
            noteVal += noteOctave*12+21;  // A4+21 = 4*12+21 = 69 (MIDI #) 
            if(noteVal<0 || noteVal>127) {
                return "Invalid note number '"+noteVal+"'";
            }
        }
        
        // If we are tieing to the next note, just remember the length
        // and move on.
        if(tie) {
            tieLength += noteLength;
            return null;
        }
        
        // Here is what we have ...        
        // noteLength   -- in seconds        
        // noteVal      -- midi note number or -1 for rest   
        // tieLength    -- amount of tie from last note(s) in seconds
        
        //System.out.println(":"+noteVal);
                
        double totalTime = noteLength+tieLength;
        tieLength = 0.0;
        
        if(cc!='R') { // Rests just advance the time
            // Find where note's sound ends (maybe some silence a little after)
            double onTime = totalTime;            
            if(staccatoIsPercent) {
                // Staccato percent only applies to the current noteLength and
                // not the tieLength coming in.
                onTime = onTime - noteLength*(1.0-staccatoValue);
            } else {
                if(staccatoValue < onTime) {
                    onTime = onTime - staccatoValue; 
                }
            }
            
            if(noteVal > 1000) { // Trigger
            	SoundSequencerEvent ev = new SoundSequencerEvent(false);
            	ev.startTime = currentTime;            
                ev.eventData = new Integer(-1);            
                ev.voice = myVoice;
                events.add(ev);
            } else {
            
            // Note on
            SoundSequencerEvent ev = new SoundSequencerEvent(toneVoice);
            ev.startTime = currentTime;            
            ev.eventData = new Integer(noteVal);            
            ev.voice = myVoice;
            events.add(ev);
            
            // Note off
            ev = new SoundSequencerEvent(toneVoice);
            ev.startTime = currentTime+onTime;
            ev.eventData = new Integer(0);
            ev.voice = myVoice;
            events.add(ev);
            
            }
            
        }
        
        // Advance time past note (or rest) duration
        currentTime = currentTime + totalTime;    
        return null;       
        
    }  
    
    public static String colate(SoundSequencer [] sequencers, CodeLine c, Cluster cluster, 
    		SoundSequencerCommandFactory factory)
    {
    	
    	List<SoundSequencerEvent> eventList = new ArrayList<SoundSequencerEvent>();
    	
    	// Make sure any dangling pauses get added
    	for(SoundSequencer s : sequencers) {
            if(s!=null) {
                s.finalPause();
            }
            // Add the seqencer's events to the list
            for(int y=0;y<s.events.size();++y) {
                eventList.add(s.events.get(y));
            }
        }    	    	
    	
    	// Now bubble sort the list
        boolean changed = true;
        while(changed) {
            changed = false;
            for(int x=0;x<eventList.size()-1;++x) {
                SoundSequencerEvent a = eventList.get(x);
                SoundSequencerEvent b = eventList.get(x+1);
                if(a.startTime>b.startTime) {
                    changed = true;
                    eventList.set(x,b);
                    eventList.set(x+1,a);
                }
            }
        }    
        
        // Lay out the events and pauses. We use "simTime" to track the
        // round-off errors in pause loop-counts.
        double currentTime = 0.0;
        double simTime = 0.0;
        
        for(int x=0;x<eventList.size();++x) {
            SoundSequencerEvent a = eventList.get(x);
            
            // Add pause to bring time up to present
            if(a.startTime>currentTime) {
                // Insert a pause
            	double pt = a.startTime - simTime;
            	double ptDelta = factory.addPause(pt);  
            	currentTime = a.startTime;
            	simTime += ptDelta;
            }
            
            // If this was a voice's final pause then there is no event to follow.            
            if(a.eventData==null) continue;
                        	            
            // Add the note-on           
            String er = factory.addNote(a.voice, a.toneVoice, ((Integer)a.eventData).intValue());
            if(er!=null) return er;
        }        
        
        return null;
    	
    }
    
}

