VAR

  long  cog
  
PUB start(ioblock)
 
  cog := cognew(@FrameDriver, ioblock)

PUB stop
  cogstop(cog)

DAT      
         org 0
         
FrameDriver  

         ' Copy the pointers
         mov       tmp,par             ' Memory pointer
         mov       tmp2,#diskCommand   ' Variables in COG
         mov       tmp3,#33            ' 33 pointers to set
fd_1     movd      fd_2,tmp2           ' Set the destination variable
         nop                           ' Kill time
fd_2     mov       0,tmp               ' Set variable to pointer
         add       tmp2,#1             ' Next variable
         add       tmp,#4              ' Next long in memory
         djnz      tmp3,#fd_1          ' Do all                          

         ' These are constants we'll be using ... read them
         rdlong    sectorN,currentSector
         rdlong    musicN,musicOffset         ' Constant offset

         ' Quick pointer to 2nd cache buffer
         mov       sectorBuffer2,sectorBuffer ' Address of ...
         add       sectorBuffer2,C_4K         ' ... second cache 

         ' Init the first loop
         mov       framePointer,sectorBuffer  ' First frame at start of first cache                  
         mov       startOfMusic,framePointer  ' One byte past ...        
         add       startOfMusic,musicN        ' ... last frame    
         mov       tmp3,framePointer          ' Remember the start of the cache

drawFrame

         ' Start the LEDs showing 192 bytes from memory
         wrlong    framePointer,addressA      ' Write the ...
         add       framePointer,#96           ' ... pixel ...
         wrlong    framePointer,addressB      ' ... addresses
         mov       tmp,#1                     ' Start ...
         wrlong    tmp,commandA               ' ... the ...
         add       framePointer,#96           ' ... display ...
         wrlong    tmp,commandB               ' ... hardware

         ' Are we drawing the last frame in this cluster? No ... go wait
         ' for a tick.
         cmp       framePointer,startOfMusic wz
  if_nz  jmp       #waitTick

         ' Start the disk filling the next cluster over this cache area.
         ' Yes, the LED driver is probably still reading from this area,
         ' but it should be done long before the disk gets here.
         wrlong    sectorN,sectorAddress  ' Next spot on disk not read
         wrlong    tmp3,memoryPointer     ' We kept the start of the cache
         mov       tmp,#$1_08             ' Read 8 sectors (4K)
         wrlong    tmp,diskCommand        ' Start the reading
         add       sectorN,#8             ' For next time

         cmp       tmp3,sectorBuffer wz        ' At end of 1st buffer?
  if_z   jmp       #start2nd                   ' Yes ... advance to 2nd buffer
         mov       framePointer,sectorBuffer   ' Else move to 1st buffer      
         jmp       #prep                       ' Finish preparing
         
start2nd
         mov       framePointer,sectorBuffer2  ' Start of 2nd buffer     

prep     mov       tmp3,framePointer           ' Hold this for later caching
         mov       startOfMusic,framePointer   ' One byte past ...
         add       startOfMusic,musicN         ' ... last frame
         
waitTick rdlong    tmp,musicTick       ' Wait for the tick ...
         cmp       tmp,#0 wz           ' ... to be ...
  if_z   jmp       #waitTick           ' ... non-zero
  
         mov       tmp,#0              ' Acknowledge ...
         wrlong    tmp,musicTick       ' ... the tick

         jmp       #drawFrame          ' Quickly start next frame         

tmp            long 0
tmp2           long 0
tmp3           long 0

sectorN        long 0   ' Running sector pointer for the movie
musicN         long 0   ' Constant size of video in cluster

framePointer   long 0   ' Where we are reading from in memory
startOfMusic   long 0   ' Where the music starts (and video ends)

C_4K           long 4096

' -- Copied from parameters ... keep this order
diskCommand    long 0
sectorAddress  long 0 
memoryPointer  long 0          
commandA       long 0 
addressA       long 0 
commandB       long 0 
addressB       long 0 
ayCommand      long 0 
ayChip         long 0 
ayRegister     long 0 
ayValue        long 0
ayEnvelopes    long 0,0,0,0,  0,0,0,0,  0,0,0,0,  0,0,0,0
totalFrames    long 0 
musicOffset    long 0 
currentSector  long 0 
musicTick      long 0 
musicStatus    long 0 
sectorBuffer   long 0     ' Address of 1st cache buffer
' -- End of copy
sectorBuffer2  long 0     ' Address of 2nd cache buffer

         