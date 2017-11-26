package gx.sound;


public interface SoundSequencerCommandFactory 
{
	
	public double addPause(double pauseTime);
	
	public String addNote(int voice, boolean tone, int note);

}
