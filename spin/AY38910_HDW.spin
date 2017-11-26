{

  This driver runs off of a 1302-clock base tick. This means the main polling loop must
  take less than 1302 clocks.

  The main polling loop is thus 16 usecs. The absolute fastest the driver can respond
  to new commands is once ever 16 usecs ... probably slower if there are lots of
  envelope transitions running.

  Envelopes are processed one at a time every 1302-clock tick. Thus it takes 6 (six
  voices) 1302-clock base ticks to process one envelope cycle.

  Envelope scripts are primarily VOLUME:DELAY byte pairs. A delay of 255 is
  255*6*1302 / 80_000_000 = 0.0249984 sec (roughly 25 ms).

  The main-loop interval is as short as possible. The goal is to keep the envelope
  ticks even. The cost is that direct-register mainipulations have to wait on the
  next tick free and are preempted by any envelope transitions.

  The assumption here is that this driver is not used for fast-changing frequency effects. It is
  used for musical notes where the long developing envelopes are important.


 Hardware connections to 2 AY38910s:     
   Chip A BC1   P12
   Chip A BDIR  P13
   Chip B BC1   P14
   Chip B BDIR  P15
                      0....7
   A and B Data Bus P16..P23

 Memory mapped interface:
   long  command    (trigger goes to 0 when finished) 1=writeValueToRegister
   long  chip       
   long  register
   long  value
   long[16] envelope script pointers

  Register 0-15 are the direct AY38910 registers. Registers 16-21 are added "virtual" registers:

  16  long 12-bit Tone for voice A. Upper 4 bits set one of 15 pre-defined envelopes. FF for none. 
  17  long 12-bit Tone for voice B. Upper 4 bits set one of 15 pre-defined envelopes. FF for none. 
  18  long 12-bit Tone for voice C. Upper 4 bits set one of 15 pre-defined envelopes. FF for none. 
                                  
  19  A 4-bit envelope number. FF means none.
  20  B 4-bit envelope number. FF means none.
  21  C 4-bit envelope number. FF means none.
    
    Envelope commands are 2 bytes: value, delay
    value=FF ... delay is a backwards jump
    delay=00 ... end script          

  At initialization the driver loads 16 pointers to envelope scripts. These scripts control
  the volume of a voice over time. Each entry in the script is 2 bytes: a value and a delay.
  The value sets the voice's volume to a specific value and then delay defines how long until
  the next change. Voice amplitudes are 0 (off) to 15 (loudest).

  A value of FF means the delay is treated like an 8 bit unsigned backwards jump offset. This
  allows an envelope to repeat for a warbling.

  Delay values are 1 (shortest) to 255 (25 ms).
  A delay of 00 means the script is terminated.
  
}

VAR

  long  cog
  long  ioPtr
  
PUB start(ioblock)

  ioPtr := ioblock
  cog := cognew(@AYDriver, ioblock)

PUB writeToRegister(_chip,_register,_value)  
  long[ioPtr+4] := _chip
  long[ioPtr+8] := _register
  long[ioPtr+12] := _value  
  long[ioPtr] :=1
  repeat while(long[ioPtr]<>0)  

DAT      
         org 0                
         
AYDriver                  

         ' Build the parameter pointers
         mov       tmp,par               ' Memory pointer
         mov       tmp2,#IO_Command      ' Variables in COG
         mov       tmp3,#20              ' 20 pointers to set
fd_1     movd      fd_2,tmp2             ' Set the destination variable
         nop                             ' Kill time
fd_2     mov       0,tmp                 ' Set variable to pointer
         add       tmp2,#1               ' Next variable
         add       tmp,#4                ' Next long in memory
         djnz      tmp3,#fd_1            ' Do all                      
                  
         ' Init the I/O pins
         mov       outa,#0               ' All outputs = 0
         mov       dira,C_PINS           ' Directions on pins (all outputs)

         ' Disable all sound  
         mov       address,#7            ' Mixer address
         mov       value,#%11_111_111    ' All ones ... everything off
         mov       chip,#0
         call      #writeRegister        ' Chip A
         mov       chip,#1         
         call      #writeRegister        ' ... B

         mov       curTime,cnt           ' Initialize next-tick ...
         add       curTime,C_WAIT        ' ... vaue
         
top      
         mov       volChip,#0            ' Start with Chip 0                 

         cmp       envFlagA,#1 wz        ' Is the envelope running?
  if_nz  jmp       #doComA               ' No ... there is time for a command
         sub       envCntA,#1 wz         ' Time for the envelope to process?
  if_nz  jmp       #doComA               ' No ... there is time for a command
         mov       volReg,#8             ' A's volume register                 
         mov       envPtr,envPtrA        ' Copy envelope ...
         mov       envFlag,envFlagA      ' ... params to ...
         mov       envCnt,envCntA        ' ... work area
         call      #envTick              ' Do an envelope tick
         mov       envPtrA,envPtr        ' Copy envelope ...
         mov       envFlagA,envFlag      ' ... params back ...
         mov       envCntA,envCnt        ' ... from work area
         jmp       #endA                 ' Skip command
doComA   call      #command              ' Process a general command
endA     waitcnt   curTime,C_WAIT        ' Wait for next ... regular tick     


' Repeated here. We have plenty of code space.

         cmp       envFlagB,#1 wz     
  if_nz  jmp       #doComB            
         sub       envCntB,#1 wz       
  if_nz  jmp       #doComB
         mov       volReg,#9             
         mov       envPtr,envPtrB      
         mov       envFlag,envFlagB    
         mov       envCnt,envCntB      
         call      #envTick            
         mov       envPtrB,envPtr      
         mov       envFlagB,envFlag    
         mov       envCntB,envCnt      
         jmp       #endB               
doComB   call      #command            
endB     waitcnt   curTime,C_WAIT

         cmp       envFlagC,#1 wz     
  if_nz  jmp       #doComC            
         sub       envCntC,#1 wz       
  if_nz  jmp       #doComC
         mov       volReg,#10             
         mov       envPtr,envPtrC      
         mov       envFlag,envFlagC    
         mov       envCnt,envCntC      
         call      #envTick            
         mov       envPtrC,envPtr      
         mov       envFlagC,envFlag    
         mov       envCntC,envCnt      
         jmp       #endC               
doComC   call      #command            
endC     waitcnt   curTime,C_WAIT

         mov       volChip,#1

         cmp       envFlagD,#1 wz     
  if_nz  jmp       #doComD            
         sub       envCntD,#1 wz       
  if_nz  jmp       #doComD
         mov       volReg,#8             
         mov       envPtr,envPtrD      
         mov       envFlag,envFlagD    
         mov       envCnt,envCntD      
         call      #envTick            
         mov       envPtrD,envPtr      
         mov       envFlagD,envFlag    
         mov       envCntD,envCnt      
         jmp       #endD               
doComD   call      #command            
endD     waitcnt   curTime,C_WAIT

         cmp       envFlagE,#1 wz     
  if_nz  jmp       #doComE            
         sub       envCntE,#1 wz       
  if_nz  jmp       #doComE
         mov       volReg,#9             
         mov       envPtr,envPtrE      
         mov       envFlag,envFlagE    
         mov       envCnt,envCntE      
         call      #envTick            
         mov       envPtrE,envPtr      
         mov       envFlagE,envFlag    
         mov       envCntE,envCnt      
         jmp       #endE               
doComE   call      #command            
endE     waitcnt   curTime,C_WAIT

         cmp       envFlagF,#1 wz     
  if_nz  jmp       #doComF            
         sub       envCntF,#1 wz       
  if_nz  jmp       #doComF
         mov       volReg,#10             
         mov       envPtr,envPtrF      
         mov       envFlag,envFlagF    
         mov       envCnt,envCntF      
         call      #envTick            
         mov       envPtrF,envPtr      
         mov       envFlagF,envFlag    
         mov       envCntF,envCnt      
         jmp       #endF               
doComF   call      #command            
endF     waitcnt   curTime,C_WAIT
        
         jmp       #top

            

' Roughly 4000 clocks

envTick  rdbyte    value,envPtr        ' Get next ...
         add       envPtr,#1           ' ... value
         rdbyte    del,envPtr          ' Get next ...
         add       envPtr,#1           ' ... delay  

         cmp       value,#$FF wz       ' Is this a jump command?
  if_z   jmp       #doEJump            ' Yes ... do it           

         mov       chip,volChip        ' Set ...
         mov       address,volReg      ' ... new ...         
         call      #writeRegister      ' ... volume

         cmp       del,#0 wz           ' Delay 0?
  if_z   jmp       #doEStop            ' Yes ... do the stop
         mov       envCnt,del          ' New count value   
         jmp       #envTick_ret        ' Done

doEStop  mov       envFlag,#0          ' Turn off processing
         jmp       #envTick_ret        ' Done
         
doEJump  sub       envPtr,del          ' Back the script pointer up
         jmp       #envTick            ' No waiting ... do the next command

envTick_ret
         ret                            

' Longest path through a pass is roughly 1100 clocks
'
' There are 6 voices. It takes 6 base ticks to get one envelope tick.
'
' I want 255 in the delay to represent 25 ms.
' .025 / 256 = .00009765625
' .00009765625 * 80_000_000 = 7812.5 cycles
' 7812.5 / 6 = ~1302 clocks 
         
C_WAIT   long 1302
curTime  long 0

'-- Copied parameter pointers
chip          long 0
address       long 0
value         long 0
del           long 0
hold          long 0

IO_Command    long 0
IO_Chip       long 0
IO_Register   long 0
IO_Value      long 0     
envPointers   long 0,0,0,0,   0,0,0,0,   0,0,0,0,   0,0,0,0
'-- End of copy

tmp           long 0
tmp2          long 0
tmp3          long 0
volReg        long 0
volChip       long 0

' Envelope parameters
'  Flag: 1=running, 0=stopped
'  Ptr:  Pointer to next script command (if running)
'  Cnt:  Count down to next command (if running)
envFlagA long 0
envFlagB long 0
envFlagC long 0
envFlag  long 0 ' Unused slot allows *4 math for chip number
envFlagD long 0
envFlagE long 0
envFlagF long 0

envPtrA  long 0
envPtrB  long 0
envPtrC  long 0
envPtr   long 0
envPtrD  long 0
envPtrE  long 0
envPtrF  long 0

envCntA  long 0
envCntB  long 0
envCntC  long 0
envCnt   long 0
envCntD  long 0
envCntE  long 0
envCntF  long 0

' Roughly 910 longest path

command
         ' IO_Command     1 to start write. Returns to 0 when processed.
         ' IO_Register    Register 0-15 (plain), 16-18 (voice), 19-21 (envelope)
         ' IO_Value       Byte for plain register, larger for virual registers

         rdlong    tmp,IO_Command wz     ' Get the command
   if_z  jmp       #command_ret          ' No command ... done
  
         rdlong    chip,IO_Chip          ' Get the chip number
         rdlong    address,IO_Register   ' Get the register
         rdlong    value,IO_Value        ' Get the value

         cmp       address,#16 wz,wc     ' Just a regular register?
  if_b   jmp       #doCPlain             ' Yes ... simple write

         cmp       address,#19 wz,wc     ' Tone command?
  if_b   jmp       #doCTone              ' Yes ... tone command

         ' Start envelope (restart or stop too)
         sub       address,#19           ' Now 0,1, or 2

setEnvelope
         ' chip is 0 or 1
         ' address is 0,1, or 2 (voice A,B,C)
         ' value is new envelope (FF means envelope is off)

         shl       chip,#2               ' Chip*4 for voice bank (0 or 4)
                  
         ' Set the pointer
         mov       tmp,value             ' Get memory pointer ...
         add       tmp,#envPointers      ' ... to envelope script
         movs      co1,tmp               ' Code pointer
         mov       tmp,address           ' Need this next
co1      rdlong    tmp2,0                ' Get script pointer
         add       tmp,#envPtrA          ' Offset to destination
         add       tmp,chip              ' Offset to voice bank (chip)
         movd      co2,tmp               ' Code pointer
         mov       tmp,address           ' Need this next
co2      mov       0,tmp2                ' Set the correct envelope script

         ' Set the count to 1
         add       tmp,#envCntA          ' Code pointer to ...
         add       tmp,chip              ' Offset to voice bank (chip) 
         movd      col3,tmp              ' ... specific counter
         add       address,#envFlagA     ' Need this next
         add       address,chip          ' Offset to voice bank (chip)
col3     mov       0,#1                  ' Set the counter

         ' Get flag value (FF->0 else->1)
         cmp       value,#$FF wz, wc     ' If the poiner is FF ...
  if_z   mov       value,#0              ' ... then envelope off
  if_nz  mov       value,#1              ' Else envelope on

         ' Set the flag to 1         
         movd      col4,address          ' Code pointer to flag
         nop                             ' Kill a slot
col4     mov       0,value               ' Set the flag to 1  
        
         jmp       #doCPDone             ' Done

' Roughly 800
'
doCTone  
         sub       address,#16           ' Voice 0, 1, or 2         
         shl       address,#1            ' The "fine" register 0, 2, or 4
         mov       tmp2,address          ' Hold address
         mov       tmp3,value            ' Hold value
         and       value,#$FF            ' Fine part of value
         call      #writeRegister        ' Write the fine register

         mov       address,tmp2          ' Restore "fine" register
         add       address,#1            ' Offset to coarse register
         mov       value,tmp3            ' Restore value
         shr       value,#8              ' Get the 4 bit ...
         and       value,#$0F            ' ... coarse value
         call      #writeRegister        ' Write coarse value

         mov       address,tmp2          ' Voice number to address (0,1,2)
         shr       address,#1            ' Back to number
         mov       value,tmp3            ' Restore value
         shr       value,#12             ' Get the envelope value
         jmp       #setEnvelope          ' Set the envelope and move on
        
doCPlain
         call      #writeRegister        ' Write the plain register
doCPDone
         mov       tmp,#0                ' Tell the requester ...
         wrlong    tmp,IO_Command        ' ... we are done
         
command_ret
         ret                             

' -------------------------------------------------------------
' 32 clocks + 6*44  = 296
writeRegister
'
' BDIR=0, BC1=0, BUS = address
' BDIR=1, BC1=1, BUS = address (latch address)
' BDIR=0, BC1=0, BUS = address
'
' BDIR=0, BC1=0, BUS = data
' BDIR=1, BC1=0, BUS = data (write)
' BDIR=0, BC1=0, BUS = data

         cmp       chip,#1  wz
  if_z   shl       C_A_ADDR,#2
  if_z   shl       C_A_WR,#2        
                 
         mov       hold,address
         and       hold,#15
         
         shl       hold,#16
         
         mov       tmp,hold
         mov       outa,tmp       ' addr + inactive             
         call      #delay

         mov       tmp,hold
         or        tmp,C_A_ADDR
         mov       outa,tmp       ' addr + latch           
         call      #delay

         mov       tmp,hold
         mov       outa,tmp       ' addr + inactive              
         call      #delay

         mov       hold,value
         shl       hold,#16  
         
         mov       tmp,hold
         mov       outa,tmp       ' data + inactive          
         call      #delay
 
         mov       tmp,hold
         or        tmp,C_A_WR
         mov       outa,tmp       ' data + write            
         call      #delay 

         mov       tmp,hold
         mov       outa,tmp         
         call      #delay

         cmp       chip,#1 wz
  if_z   shr       C_A_ADDR,#2
  if_z   shr       C_A_WR,#2
                     
writeRegister_ret
         ret

' 44 clock delay
delay    mov       dcnt,#8        ' 4
tdel     djnz      dcnt,#tdel     ' 7*4 + 8
delay_ret
         ret                      ' 4
dcnt     long 0

{
         mov       tmp,#$1AA
         wrlong    tmp,IO_Chip
         wrlong    value,IO_Register
         wrlong    del,IO_Value         
         jmp       #debugTone  

debugTone          
         
         mov       tmp,#0
         wrlong    tmp,IO_Command

         mov       chip,#0
         mov       address,#7
         mov       value,#%11_111_110
         call      #writeRegister

         mov       address,#8
         mov       value,#0
         call      #writeRegister
         
         mov       address,#1
         mov       value,#5
         call      #writeRegister
         
dt       jmp       #dt
}
    
'                         DATA    BB AA
C_PINS   long %0000_0000_11111111_11_11_000_000_000_000  
'
'                                 BB AA
C_A_ADDR long %0000_0000_00000000_00_11_000_000_000_000  ' BDIR=1 BC1=1
C_A_WR   long %0000_0000_00000000_00_10_000_000_000_000  ' BDIR=1 BC1=0
