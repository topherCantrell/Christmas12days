' -------------------------------------------------------------------------------
' LED Driver
' Controls two LED panels that share adjacent pixel buffers.
'
' MS-Pin         LS-Pin
' DATA, WR, CSb, CSa

'' Memory mapped interface:
'' Initialize with pin-shift in "command"    
''   long command  (trigger goes to 0 when finished)
''   long address
''
'' Write 100 to command for LED command (command data is in first data byte)
'' Any other value except 0 signals a refresh from the data memory

VAR

  long  cog
  
PUB start(mem) 
'' Start the LED
               
  cog := cognew(@LEDDriver,mem)

DAT      
         org 0
LEDDriver

         mov       CONTROL, par        ' Address of the control register
         mov       BASE,par            ' Address of the ...
         add       BASE,#4             ' ... address of the buffer (allows buffer to move)

         rdlong    pinShift,CONTROL    ' Pin configuration stored in the control

         mov       tmp,#0              ' Outputs are ...
         wrlong    tmp,CONTROL         ' (clear control)
         shl       tmp,pinShift        ' ... inverted
         mov       outa,tmp            ' CSa=1, CSb=1, WR=1, DATA=X

         mov       tmp,#%1111          ' 4 pins ... CS, WR, DATA
         shl       tmp,pinShift        ' Configure our ...
         mov       dira,tmp            ' ... pins as outputs                                                                     

         ' Initalization command sequence
         mov       command,#$00
         call      #writeCommandA
         call      #writeCommandB         
         mov       command,#$2C
         call      #writeCommandA
         call      #writeCommandB
         mov       command,#$14
         call      #writeCommandA
         call      #writeCommandB
         mov       command,#$01
         call      #writeCommandA
         call      #writeCommandB
         mov       command,#$03
         call      #writeCommandA
         call      #writeCommandB         

top      rdlong    tmp,CONTROL wz      ' Wait for ...
  if_z   jmp       #top                ' ... command or refresh
  
         rdlong    ptr,BASE            ' Base of the LED memory
          
         cmp       tmp,#100 wz         ' Handle LED ...
  if_z   jmp       #doCommand          ' ... command                 
         
         mov       count,#48           ' Reading 48 bytes (48*8=384 pixels)
         call      #initWriteA         ' Reset address to top of display   
scanA    rdbyte    command,ptr         '   8 Get next byte
         call      #writeTwoNibblesA   ' 456 Write the two nibbles
         add       ptr,#1              '   4 Next in memory
         djnz      count,#scanA        '   4 Do all bytes on the display                 
 ' Roughly 22656 clocks/scan (0.000_2832 sec)

         mov       tmp,#0              ' Set ...
         shl       tmp,pinShift        ' ... CSa=1, CSb=1, WR=1, and ...
         mov       outa,tmp            ' ... DATA=X
   
         mov       count,#48           ' Reading 48 bytes (48*8=384 pixels)
         call      #initWriteB         ' Reset address to top of display   
scanB    rdbyte    command,ptr         '   8 Get next byte
         call      #writeTwoNibblesB   ' 456 Write the two nibbles
         add       ptr,#1              '   4 Next in memory
         djnz      count,#scanB        '   4 Do all bytes on the display             
' Roughly 22656 clocks/scan (0.000_2832 sec)

         mov       tmp,#0              ' Set ...
         shl       tmp,pinShift        ' ... CSa=1, CSb=1, WR=1, and ...
         mov       outa,tmp            ' ... DATA=X

ack      mov       tmp,#0              ' Action ...
         wrlong    tmp,CONTROL         ' ... is finished
         
         jmp       #top                ' Wait for the next refresh

doCommand
         rdbyte    command,ptr         ' Commands come from ...
         call      #writeCommandA      ' ... first byte of buffer
         call      #writeCommandB      ' ... first byte of buffer
         jmp       #ack                ' ACK the command

' -----------------------------------------------------
'  Reset the address to the beginning of the display. Leave CS asserted
'  for more spew 
'
initWriteB
         mov       tmp,#%0010          ' CSb=1
         shl       tmp,pinShift        ' CSa=0 (enable) , WR=1 (should alredy be), DATA=X
         mov       outa,tmp            ' We leave CS=0 here
         
         mov       tmp,#1              ' Send ...
         call      #writeBitB           ' ... 1
         mov       tmp,#0              ' Send ...
         call      #writeBitB           ' ... 0
         mov       tmp,#1              ' Send ...
         call      #writeBitB           ' .... 1

         mov       tmp,#0
         call      #writeBitB           ' 7 bit address ...
         call      #writeBitB           ' ... 0000000
         call      #writeBitB
         call      #writeBitB
         call      #writeBitB
         call      #writeBitB
         call      #writeBitB
initWriteB_ret
         ret    
         
' -----------------------------------------------------
'  Reset the address to the beginning of the display. Leave CS asserted
'  for more spew 
'
initWriteA
         mov       tmp,#%0001          ' CSb=1
         shl       tmp,pinShift        ' CSa=0 (enable) , WR=1 (should alredy be), DATA=X
         mov       outa,tmp            ' We leave CS=0 here
         
         mov       tmp,#1              ' Send ...
         call      #writeBitA           ' ... 1
         mov       tmp,#0              ' Send ...
         call      #writeBitA           ' ... 0
         mov       tmp,#1              ' Send ...
         call      #writeBitA           ' .... 1

         mov       tmp,#0
         call      #writeBitA           ' 7 bit address ...
         call      #writeBitA           ' ... 0000000
         call      #writeBitA
         call      #writeBitA
         call      #writeBitA
         call      #writeBitA
         call      #writeBitA
initWriteA_ret
         ret    

' -----------------------------------------------------
'  Write 8 bits from command to next address on display
'
writeTwoNibblesA         ' 452
         mov       tmp,command         '  4 Write ...
         call      #writeBitA           ' 52 ... bit 0
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 1
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 2
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 3
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 4
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 5
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 6
         shr       tmp,#1              '  4 Write ...
         call      #writeBitA           ' 52 ... bit 7
writeTwoNibblesA_ret
         ret                           '  4

' -----------------------------------------------------
'  Write 8 bits from command to next address on display
'
writeTwoNibblesB         ' 452
         mov       tmp,command         '  4 Write ...
         call      #writeBitB           ' 52 ... bit 0
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 1
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 2
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 3
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 4
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 5
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 6
         shr       tmp,#1              '  4 Write ...
         call      #writeBitB           ' 52 ... bit 7
writeTwoNibblesB_ret
         ret                           '  4  

' -----------------------------------------------------
'  Write a command value. CS is toggled.
'
writeCommandA
         mov       tmp,#%0001          '
         shl       tmp,pinShift        ' CSb=1
         mov       outa,tmp            ' CSa=0 (enable) , WR=1 (should alredy be), DATA=X
         mov       tmp,#1              '
         call      #writeBitA           ' Send 1
         mov       tmp,#0              '
         call      #writeBitA           ' Send 0
         call      #writeBitA           ' Send 0

         mov       tmp,command
         shr       tmp,#7
         call      #writeBitA

         mov       tmp,command
         shr       tmp,#6
         call      #writeBitA

         mov       tmp,command
         shr       tmp,#5
         call      #writeBitA

         mov       tmp,command
         shr       tmp,#4
         call      #writeBitA

         mov       tmp,command
         shr       tmp,#3
         call      #writeBitA

         mov       tmp,command
         shr       tmp,#2
         call      #writeBitA

         mov       tmp,command
         shr       tmp,#1
         call      #writeBitA

         mov       tmp,command         
         call      #writeBitA

         mov       tmp,#1              ' Write the ...
         call      #writeBitA           ' ... X bit (9 bit commands)

         mov       tmp,#0              ' Set CSx=1 ...
         shl       tmp,pinShift        ' ... WR=1 ...
         mov       outa,tmp            ' ... DATA=X            

writeCommandA_ret
         ret

' -----------------------------------------------------
'  Write a command value. CS is toggled.
'
writeCommandB
         mov       tmp,#%0010          '
         shl       tmp,pinShift        ' CSb=0
         mov       outa,tmp            ' CSa=1 (enable) , WR=1 (should alredy be), DATA=X
         mov       tmp,#1              '
         call      #writeBitB           ' Send 1
         mov       tmp,#0              '
         call      #writeBitB           ' Send 0
         call      #writeBitB           ' Send 0

         mov       tmp,command
         shr       tmp,#7
         call      #writeBitB

         mov       tmp,command
         shr       tmp,#6
         call      #writeBitB

         mov       tmp,command
         shr       tmp,#5
         call      #writeBitB

         mov       tmp,command
         shr       tmp,#4
         call      #writeBitB

         mov       tmp,command
         shr       tmp,#3
         call      #writeBitB

         mov       tmp,command
         shr       tmp,#2
         call      #writeBitB

         mov       tmp,command
         shr       tmp,#1
         call      #writeBitB

         mov       tmp,command         
         call      #writeBitB

         mov       tmp,#1              ' Write the ...
         call      #writeBitB           ' ... X bit (9 bit commands)

         mov       tmp,#0              ' Set CSx=1 ...
         shl       tmp,pinShift        ' ... WR=1 ...
         mov       outa,tmp            ' ... DATA=X            

writeCommandB_ret
         ret

' -----------------------------------------------------
'  Write a single bit toggling the WR. We assume CS is asserted
'  and we will leave it as such.
'
writeBitA       ' 12*4 = 48

         mov       tmp2,tmp            ' 4 Invert ...
         and       tmp2,#1             ' 4 ... data ...
         xor       tmp2,#1             ' 4 ...
         shl       tmp2,#3             ' 4 Correct pin  for data
                         'DWBA
         or        tmp2,#%0101         ' 4 CSa=0 (still) CSb=1 and WR=0

         shl       tmp2,pinShift       ' 4 CSa=0, CSb=1, WR=0, DATA=D
         mov       outa,tmp2           ' 4 Signal LED board

         shr       tmp2,pinShift       ' 4 Set ...
                         'DWBA
         andn      tmp2,#%0100         ' 4 CSa=0, CSb=1, WR=1, DATA=D
         shl       tmp2,pinShift       ' 4 Signal ...
         mov       outa,tmp2           ' 4 ... LED board                  
writeBitA_ret
         ret                           ' 4

' -----------------------------------------------------
'  Write a single bit toggling the WR. We assume CS is asserted
'  and we will leave it as such.
'
writeBitB       ' 12*4 = 48

         mov       tmp2,tmp            ' 4 Invert ...
         and       tmp2,#1             ' 4 ... data ...
         xor       tmp2,#1             ' 4 ...
         shl       tmp2,#3             ' 4 Correct pin  for data
                         'DWBA
         or        tmp2,#%0110         ' 4 CSb=0 (still) CSa=1 and WR=0

         shl       tmp2,pinShift       ' 4 CSb=0, CSa=1, WR=0, DATA=D
         mov       outa,tmp2           ' 4 Signal LED board

         shr       tmp2,pinShift       ' 4 Set ...
                         'DWBA
         andn      tmp2,#%0100         ' 4 CSb=0, CSa=1, WR=1, DATA=D
         shl       tmp2,pinShift       ' 4 Signal ...
         mov       outa,tmp2           ' 4 ... LED board                  
writeBitB_ret
         ret                           ' 4
   
com      long $0

command  long $0         

tmp      long $0
ptr      long $0
count    long $0
tmp2     long $0

pinShift long 0   

CONTROL  long 0
BASE     long 0   

lastAdr  fit             