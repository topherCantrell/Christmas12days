{

  Music processor runs on a 1ms loop.
  
  8a_bb     ... pause for abbcc ms
  01_ve_nn  ... play midi-note nn using envelope e on voice v
  02_ve     ... note off by playing envelop e on voice v
  FF        ... music tick (same cluster)
  FE        ... music tick (end cluster)

}

VAR

  long  cog
  
PUB start(ioblock)
 
  cog := cognew(@MusicDriver, ioblock)

PUB stop
  cogstop(cog)

DAT      
         org 0
         
MusicDriver

         ' Copy the pointers
         mov       tmp,par             ' Memory pointer
         mov       tmp2,#diskCommand   ' Variables in COG
         mov       tmp3,#33            ' 17 pointers to set
fd_1     movd      fd_2,tmp2           ' Set the destination variable
         nop                           ' Kill time
fd_2     mov       0,tmp               ' Set variable to pointer
         add       tmp2,#1             ' Next variable
         add       tmp,#4              ' Next long in memory
         djnz      tmp3,#fd_1          ' Do all

         rdlong    frameCnt,totalFrames  ' Number of frames to process
         rdlong    tmp3,musicOffset      ' Constant offset to music   

         ' Quick pointer to 2nd cache buffer
         mov       sectorBuffer2,sectorBuffer ' Address of ...
         add       sectorBuffer2,C_4K         ' ... second cache

         add       sectorBuffer,tmp3    ' Offset to ...
         add       sectorBuffer2,tmp3   ' ... music data

         mov       ptr,sectorBuffer    ' Start playing the first buffer

         ' TODO this needs to be part of the song           
         ' Enable the voices

         mov       chip,#0
         mov       register,#7
         mov       value,#%11_111_000
         call      #writeAY
                  
         mov       chip,#1
         mov       register,#7
         mov       value,#%11_111_000
         call      #writeAY

         ' Debug this by: first ignore notes and make sure tick is working
         ' Add note on/off after
         ' Debug by shifting the value an extra time (*2 to slow down)

           {
         rdlong    tmp,ptr
         wrlong    tmp,totalFrames
         add       ptr,#4
         rdlong    tmp,ptr
         wrlong    tmp,currentSector
         mov       tmp,#$42
         wrlong    tmp,musicStatus         

yyy jmp #yyy



ttt      mov       tmp,#1
         wrlong    tmp,musicTick
         mov       tmp,C_TEST_D
         add       tmp,cnt
         waitcnt   tmp,#0
         jmp       #ttt
            }                             

top      mov       countHold,cnt       ' Note commands are "instantaneous". Keep up with when we started.

com      rdbyte    tmp,ptr             ' Get the next ...
         add       ptr,#1              ' ... command
         cmp       tmp,#$FF wz         ' Handle ...
    if_z jmp       #comTick            ' ... TICK
         cmp       tmp,#$FE wz         ' Handle end of ...
    if_z jmp       #comClus            ' ... cluster TICK
         cmp       tmp,#1 wz           ' Handle ...
    if_z jmp       #comNoteOn          ' ... NOTE-ON
         cmp       tmp,#2 wz           ' Handle ...
    if_z jmp       #comNoteOff         ' ... NOTE-OFF

         ' Must be a 15-bit delay value

         and       tmp,#$7F            ' The command is the upper bit
         
         rdbyte    tmp2,ptr            ' Get next ...
         add       ptr,#1              ' ... byte
         shl       tmp,#8              ' Shift over running value
         or        tmp,tmp2            ' Add in the next byte
                 
         shl       tmp,#8              ' Convert the delay to counts (see Tick Math below)
         sub       tmp,countHold       ' Subtract off any "instant" note commands from earlier
  mov tmp,C_TST
         add       tmp,cnt             ' Offset from current count                                                                               
         waitcnt   tmp,#0              ' Wait the pause length

         jmp       #top                ' Mark the cnt value and continue

C_TST long 10_000000

comNoteOn   

        ' ve_nn

         rdbyte    value, ptr              ' Get the envelope and voice
         add       ptr,#1                  ' Next byte
         
         rdbyte    tmp2, ptr               ' Get the MIDI note number
         add       ptr,#1                  ' Next byte

         mov       register,value          ' Get the ...
         and       register,#$F0           ' ... voice ...
         shr       register,#8             ' ... number
         
         and       value,#$0F              ' Isolate the envelope
         shl       value,#12               ' Envelope's place in the value

         ' e_fcc [Format for register 16, 17, 18]

         mov       chip,#0                 ' Set ...
         cmp       register,#3 wz,wc       ' ... chip ...
  if_ae  mov       chip,#1                 ' ... number
  if_ae  sub       register,#3             ' Voice is now 0, 1, or 2
         add       register,#16            ' Offset to voice registers (16, 17, or 18)
  
         sub       tmp2,#23                ' Midi note conversion to table
         mov       tmp,tmp2                ' Two notes per ...
         shr       tmp,#1                  ' ... long
         add       tmp,#noteTable          ' Offset to note pair
         movs      nl,tmp                  ' Code pointer
         nop                               ' Kill a slot
nl       mov       tmp,0                   ' Get the note pair
         and       tmp2,#1 nr, wz          ' Pick the ... 
  if_z   shr       tmp,#16                 ' ... right note
         and       tmp,C_FFF               ' Get the fine and coarse value
         or        value,tmp               ' OR in the envelope

         wrlong    register,currentSector
         wrlong    value,totalFrames
         mov       tmp,#$47
         wrlong    tmp,musicStatus
         

         call      #writeAY                ' Change the registers
         jmp       #com                    ' Continue at same time count

writeAY
         rdlong    tmp,ayCommand wz        ' Wait for driver ...
  if_nz  jmp       #writeAY                ' ... to take last command
         wrlong    chip,ayChip             ' Write the chip value
         wrlong    register,ayRegister     ' Write the register address
         wrlong    value,ayValue           ' Write the register value
         mov       tmp,#1                  ' Trigger a ...
         wrlong    tmp,ayCommand           ' ... write
writeAY_ret
         ret

         
comNoteOff

         ' ve

         rdbyte    value, ptr              ' Get the envelope and voice
         add       ptr,#1                  ' Next byte

         mov       register,value          ' Get the ...
         and       register,#$F0           ' ... voice ...
         shr       register,#8             ' ... number
         
         and       value,#$0F              ' Isolate the envelope
         shl       value,#4                ' Envelope's place in the value
        
         mov       chip,#0                 ' Set ...
         cmp       register,#3 wz,wc       ' ... chip ...
  if_ae  mov       chip,#1                 ' ... number
  if_ae  sub       register,#3             ' Voice is now 0, 1, or 2
         add       register,#19            ' Offset to voice registers (19, 20, or 21)
         
         call      #writeAY                ' Change the registers
         jmp       #com                    ' Continue at same time count

comClus
         cmp       ptr,sectorBuffer2 wz, wc ' Are we pulling from the second buffer?
  if_a   mov       ptr,sectorBuffer        ' YES: Move back to first buffer
  if_b   mov       ptr,sectorBuffer2       ' NO: Move forward to second buffer

comTick
         djnz      frameCnt,#tick1         ' Count frames. Jump if more to do.

         mov       tmp,#1                  ' Tell player ...
         wrlong    tmp,musicStatus         ' ... we are done
         
done     jmp       #done                   ' Player will reload us

tick1    mov       tmp,#1                  ' Tell video about ...
         wrlong    tmp,musicTick           ' ... the tick         
         jmp       #com                    ' Next command (no time passes)
         


' Tick math
'// The sound processor delays on 819.2usec bounds.
'// This comes from shifting the delay value left 16 places
'// to get a number-of-clocks-to-wait. Thus:
'// 1<<16 = $1_00_00 = 65536.
'// The prop runs at 80_000_000 clocks per sec.
'// 65536/80_000_000 = 0.0008192
'//
'// A delay of $0001 (min) is 819.2usec.
'// A delay of $7FFF (max) is 26.84 seconds.

C_FFF          long $00_00_0F_FF


chip           long 0
register       long 0
value          long 0

frameCnt       long 0

ptr            long 0

countHold      long 0
tmp            long 0
tmp2           long 0
tmp3           long 0


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


' 96 notes defined (97 with 0=silence)
' REST is 0
' A440 (MIDI Note 69) is note 46 in this table
' 69-23=46. Subtract 23 from midi note to get table index.
noteTable
'     note-0   note-1
'      FF_CC
 long $00_00___0D_5D     ' _ C      Octave 1
 long $0C_9C___0B_E7     ' C# D
 long $0B_3C___0A_9B     ' E- E
 long $0A_02___09_73     ' F F#
 long $08_EB___08_6B     ' G G#
 long $07_F2___07_80     ' A A#
 long $07_14_______06_AE ' B _____ C  ' Octave 2
 long $06_4E___05_F4 
 long $05_9E___05_4D
 long $05_01___04_B9
 long $04_75___04_35
 long $03_F9___03_C0 
 long $03_8A_______03_57   ' Octave 3
 long $03_27___02_FA
 long $02_CF___02_A7
 long $02_84___02_5D
 long $02_3B___02_1B
 long $01_FC___01_E0
 long $01_C5_______01_AC   ' Octave 4  (Middle C)
 long $01_94___01_7D
 long $01_68___01_53
 long $01_40___01_2E
 long $01_1D___01_0D
 long $0____00_FE___00_F0  ' A440, A#
 long $00_E2_______00_D6   ' Octave 5
 long $00_CA___00_BE
 long $00_B4___00_AA
 long $00_A0___00_97
 long $00_8F___00_87
 long $00_7F___00_78
 long $00_71_______00_6B  ' Octave 6
 long $00_65___00_5F
 long $00_5A___00_55
 long $00_50___00_4C
 long $00_47___00_43
 long $00_40___00_3C
 long $00_39_______00_35' Octave 7
 long $00_32___00_30
 long $00_2D___00_2A
 long $00_28___00_26
 long $00_24___00_22
 long $00_20___00_1E
 long $00_1C_______00_1B' Octave 8
 long $00_19___00_18
 long $00_16___00_15
 long $00_14___00_13
 long $00_12___00_11
 long $00_10___00_0F
 long $00_0E___00_00   
 
lastAdr  fit        