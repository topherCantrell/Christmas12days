{

  TO DO:

  We have plenty of room on the SD card. The best solution is to keep the frame/music player simple
  and copy/paste frames and song fragments as needed for repeats. INCLUDES make it easy in the
  movie compiler.

  The first two sectors on the disk contain the master movie list. Each entry
  in the list is 4 bytes as follows:
     long startCluster (FFFFFFFF = end of list)
     word framesPerCluster
     word totalFrames

  Cluster 0 starts with sector 2.

  The new frame-player loop should work like this:  
  - Wait for tick
  - Draw next frame
  - Move pointer to next frame
  - If drawn last frame
    - Flip buffers
    - Start SD driver loading next cluster
    - Fix next frame pointer

  The new music-player should work like this:
  - Play all music commands until a tick
  - Tell frame-player about tick
  - Move pointer to next frame
  - If played last frame in cluster
    - Flip buffers (frame-player will load)
  - If played last frame in song
    - Signal main loop and infinite loop

  Main loop starts a movie then waits for end-signal. Then it starts next movie (or restarts)

  The movie compiler must mix the music data into the visual frames.

  Enhance the AY driver to maintain envelopes over the length of a note (like moon patrol)

  The debug terminal should allow poking of the AY registers (may be too slow) and filling of the
  frame buffer. It should allow filling of the cluster memory for testing. 

}

CON
        _clkmode        = xtal1 + pll16x
        _xinfreq        = 5_000_000

OBJ  
   
  led_hdw     : "DEDP016x2_HDW"  
  disk_hdw    : "Disk_HDW"
  ay38910_hdw : "AY38910_HDW"
  frames      : "MovieFrames"
  music       : "MovieMusic"         

  PST         : "Parallax Serial Terminal"

VAR

' Disk_HDW
  long  diskCommand
  long  sectorAddress
  long  memoryPointer           

' DEP016x2_HDW (A)
  long  commandA
  long  addressA

' DEP016x2_HDW (B)
  long  commandB
  long  addressB

' AY39010_HDW
  long  ayCommand
  long  ayChip
  long  ayRegister
  long  ayValue
  long  envelopePointers[16]

' Info about the current movie  
  long  totalFrames
  long  musicOffset
  long  currentSector

' Shared between music and video
  long  musicTick
  long  musicStatus  
  byte  sectorBuffer[4096 * 2]  ' 2 pages of 4K bytes for frame buffers
  
  byte  musicList[1024] ' Master list of movies
  byte  envelopeData[1024] ' 1K of envelope info
                                     
PUB boot | i, j, curMov

   ' Start the serial terminal
  PST.Start(115200)   
  PauseMSec(2000)   
  PST.Home
  PST.Clear
  PST.str(string("Starting LED Movie Runner",13))

  ' Start the LED display hardware (wait for it)
  commandA := 0
  commandB := 4
  led_hdw.start(@commandA)   ' Lower right + left
  led_hdw.start(@commandB)   ' Upper right + left
  repeat while commandB<>0

  ' Start the AY38910 hardware (wait for it)
  ayCommand := 1
  ay38910_hdw.start(@ayCommand)
  repeat while ayCommand<>0  
    
  ' Start disk hardware and mount the SD card
  diskCommand := 1
  memoryPointer := @sectorBuffer
  disk_hdw.start(@diskCommand)      
  repeat while diskCommand<>0    

  ' Load the movie list
  memoryPointer := @musicList
  sectorAddress := 0
  diskCommand := $1_02
  repeat while diskCommand<>0


  

  ' Start with first movie
  curMov := @musicList[0]

  ' Process the data about the movie                  
  currentSector := long[curMov]*4+2
  musicOffset   := word[curMov+4]*192
  totalFrames   := word[curMov+6]

  PST.hex(currentSector,4)
  PST.char(" ")
  PST.hex(musicOffset,4)
  PST.char(" ")
  PST.hex(totalFrames,4)   

  ' Load the envelope data
  memoryPointer := @envelopeData
  sectorAddress := currentSector
  diskCommand := $1_02
  repeat while diskCommand<>0
  
  currentSector += 8 ' The envelope data is in the 1st cluster. Movie starts after.     
 
  ' Find the starts of all the envelope scripts 
  i := 0
  repeat j from 0 to 15   
    envelopePointers[j] := i+@envelopeData
    PST.hex(i,4)
    PST.char(" ")
    repeat while envelopeData[i+1]<>0
      i := i+2    
    i := i+2

  ' Load the first two clusters of the movie (4K each, 8 sectors each, 16 sectors total)
  memoryPointer := @sectorBuffer
  sectorAddress := currentSector
  diskCommand := $1_10
  repeat while diskCommand<>0     

   {   
  addressA := @sectorBuffer      +192*8  +4096
  commandA := 1
  addressB := @sectorBuffer+96   +192*8 +4096
  commandB := 1
  repeat
    }  
  
  currentSector := currentSector + 16 ' The player will start loading here
     
  musicTick := 0
  musicStatus := 0
  frames.start(@diskCommand)
  music.start(@diskCommand)
  repeat while musicStatus==0
  PST.str(string(13,13,13))
  PST.hex(musicStatus,2)
  PST.char(" ")
  PST.hex(totalFrames,8)
  PST.char(" ")
  PST.hex(currentSector,8)
  repeat
 
  repeat
    pauseMsec(100)
    musicTick := 1

  ' While experimenting I ran the clock up to 30 frames per second with no noticable
  ' problems. There were no obvious problems faster than that (even down to 1ms between
  ' ticks) but I could not judge the visual content.

  ' Wait for movie to end
  repeat while musicStatus<>0

  PST.str(string("Movie finished.",13))

  repeat
        
PRI PauseMSec(Duration)
  waitcnt(((clkfreq / 1_000 * Duration - 3932) #> 381) + cnt)