package gx.sound;

public class SoundSequencerEvent 
{
	
	public SoundSequencerEvent(boolean toneVoice)
	{
		this.toneVoice = toneVoice;
	}
	
	boolean toneVoice;
	double startTime = -1.0;
    int voice = 0;
    Object eventData = null;
}
