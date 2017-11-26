' LED Movie Engine

{

 Frames are 192 bytes (48x32 = 1536 pixels). 8 pixels per byte 1536/8 = 192.
 There are 2 frame buffers. One for playing from and one for filling from disk.
 
 Each buffer is 2K (4 sectors, 4*512 = 2K)

 There are 8 visual frames per cluster. 8*192 = 1536 bytes for video
 The remaining memory is music. 2048 - 1536 = 512 bytes

 The frame rate is driven by a "ticking" that comes from the music.
  
 Voice command to set the envelope profile.

 TODO
 *1 Construct the master file and movie file for 12-days
 2 Create new Frame/Music players for simple flow
 3 Create the compiler for the simple players
 4 Add voice envelope to driver and language       

}

PUB start(cog, param) 
'' Start the LEDScriptEngine

  coginit(cog,@LEDScriptEngine,param)

DAT      
         org 0
         
LEDScriptEngine

' Read the segment addresses from the engine descriptor

' The engine descriptor is passed in as PAR:
'   [PAR]   DATA base segment address
'   [PAR+2] CODE base segment address
'   [PAR+4] SYS1 base segment address
'   [PAR+6] SYS2 base segment address    (4K frame memory)
'   [PAR+8] SYS3 base segment address    (Disk hardware + music tick)

         mov       corePC,par         
         rdword    coreBaseAdr,corePC             
         add       corePC,#2
         rdword    coreCodeAdr,corePC         
         add       corePC,#2
         rdword    coreSys1Adr,corePC         
         add       corePC,#2
         rdword    coreSys2Adr,corePC
         add       corePC,#2
         rdword    coreSys3Adr,corePC

        ' Disk and LED parameter blocks pointed to by SYS3        
         mov       diskControl,coreSys3Adr        
         mov       diskSector,coreSys3Adr
         add       diskSector,#4
         mov       diskMemory,coreSys3Adr
         add       diskMemory,#8
         
         mov       ledControlA,coreSys3Adr
         add       ledControlA,#12
         mov       ledMemoryA,coreSys3Adr
         add       ledMemoryA,#16
         mov       ledControlB,coreSys3Adr
         add       ledControlB,#20         
         mov       ledMemoryB,coreSys3Adr
         add       ledMemoryB,#24
         
         mov       tick,coreSys3Adr
         add       tick,#28

         mov       corePC,#0           ' Programs always start at beginning of code segment       
        
' ----------------------------------------------------------------------------
' ----------------------------------------------------------------------------
'
' INITIALIZATION
'
'   The following values must be setup before entering the core here:
'     coreBaseAdr      DATA base segment address
'     coreCodeAdr      CODE base segment address
'     coreSys1Adr      SYS1 base segment address
'     coreSys2Adr      SYS2 base segment address
'     coreSys3Adr      SYS3 base segment address
'     coreUsedThreads  Number of threads used by the code (if threaded)
'     coreThreadTick   Base wait-between-thread-runs (if threaded)
'     corePC           Offset of first instruction in the CODE segment
'
'   For multiple threads, corePC is the offset of the first thread.
'   The remaining threads are +2, +4, +6, etc off the first thread.
'   Two bytes is the length of a GOTO instruction. For a multithread
'   environment the first N instructions should be GOTOs for each
'   thread.
'
' SEGMENT-PREFIX CONFIG
'
'   The engine is expected to run in the context of several other engines each
'   with their own independent memory space. However, there are certain regions
'   of memory shared by all engines. This capability is implemented through
'   memory "segment" base offsets.
'
'   The engine accesses memory operands from one of five 4K segments whose addresses
'   are established before the engine starts. By default operands are taken from
'   the default DATA segment. The DATA segment is where program variables are kept.
'   Memory operands may be prefixed with a segment-override:
'         00 CODE      the code segment (for immediate data) 
'         01 SYS1      device control (application defined)
'         10 SYS2      device control (application defined)
'         11 SYS3      device control (application defined)
'
'   #define SEG_PREFIX     = YES  // Ability to override an operands segment
'
' OPERAND CONFIG
'
'   The enigine supports a variety of constant/memory access modes. These may be
'   turned on and off at compile time as needed. Note the operand tables must be
'   paired ... getter and setter. There are no read-only or write-only modes. 
'
'   #define 4BIT           = YES      // Processing for 4-bit constants
'   #define 12BIT          = YES      // Processing for 12-bit constants
'   #define 16BIT          = YES      // Processing for 16-bit constants
'   #define MEMORY_BYTE    = YES      // Processing for reading a byte
'   #define MEMORY_WORD    = YES      // Processing for reading a word
'   #define INDIRECT_BYTE  = YES      // Processing for reading an indirect byte
'   #define INDIRECT_WORD  = YES      // Processing for reading an indirect word
'
' MULTI-THREADING
'
'   In a multi-thread environment, a single engine processes several program
'   counters. Every "tick" of the engine the processor runs a single command
'   from each program. The THREAD_TICK determines the sync-wait between ticks.
'   This value must be greater than the longest possible command times the total
'   number of threads (worst case scenario).
'
'   The engine keeps a context block for each thread including its stack, flags and
'   PC. The dialect can also tag thread-specific information within
'   a "#copy THREAD_SPECIFIC" and "#endcopy" block. The configuration engine
'   will make room for these in the the thread-specific context block.
'
'   Setting MAXTHREADS to any number greater than 1 will enable the thread processing
'   code with a significant memory footprint and processing overhead.
'                                                   
'   #define MAXTHREADS  =  1  // Number of threads to reserve resources for  
'   #define THREAD_TICK =  0  // base tick for multi-threads (MAXTHREADS>1)
'
' CALL-STACK SIZE
'
'   The CALL/RETURN commands push/pop return addresses from the call stack. The size (depth)
'   of the call stack is configured here.
'   
'   #define CALL_STACK_SIZE = 4 // Number of longs reserved for each thread's call stack
'
'
' MULTIPLY AND DIVIDE
'
'   The MULTIPLY and DIVIDE routines can be included for dialect use even if the
'   MULT or DIV commands are not in the core.
'
'   #define MULTIPLY = NO   // Include the generic multiply routine (auto if MULT=YES)
'   #define DIVIDE   = NO   // Include the generic divide routine   (auto if DIV or REM = YES)
'
' CORE COMMANDS
'
'   Core commands can be turned on and off through the following config table. Commands that
'   are turned on also specify the Java class in the compiler used to parse the active command.
'   In this way, the compiler is always in sync with the available commands for the target
'   engine. 
'
'   #command coreGOTO           = YES   gx.g2.core.Parse_GOTO(gx.g2.core.Parse_FLOWFunction.GOTO,$)
'   #command coreGOTOCOND       = YES   gx.g2.core.Parse_GOTOCOND($)
'   #command coreCALL           = YES   gx.g2.core.Parse_GOTO(gx.g2.core.Parse_FLOWFunction.CALL,$) 
'   #command coreRETURN         = YES   gx.g2.core.Parse_RETURN($)
'   #command coreSTOP           = YES   gx.g2.core.Parse_STOP($)
'   #command corePAUSE          = YES   gx.g2.core.Parse_PAUSE($)
'   NO SYNC command for multi-threads. Use PAUSE.
'   #command coreSYNC           = YES   gx.g2.core.Parse_SYNC($)
'   #command coreNATIVEMATH     = YES   gx.g2.core.Parse_NATIVEMATH($)
'   #command coreMULT           = YES   gx.g2.core.Parse_MATH(gx.g2.core.Parse_MATHFunction.MULT,$)
'   #command coreDIV            = YES   gx.g2.core.Parse_MATH(gx.g2.core.Parse_MATHFunction.DIV,$)
'   #command coreREM            = YES   gx.g2.core.Parse_MATH(gx.g2.core.Parse_MATHFunction.REM,$)  
'   #command coreMOVE           = YES   gx.g2.core.Parse_MOVE($)
'   #command coreTHREADGOTO     = NO    gx.g2.core.Parse_THREADGOTO($)
'
' ----------------------------------------------------------------------------
                
' If threaded, jump straight to advance-step
' #if(MAXTHREADS>1)
' #$$
' #$$' Threads start at corePC= 0,2,4, etc (expecting an initial GOTO)
' #$$'
' #$$         mov       coreCom,coreUsedThreads       ' Number of threads
' #$$         mov       coreTmp,#coreThreads          ' First thread data block
' #$$         add       coreTmp,#1                    ' Skip over the SYS3 address
' #$$coreSetPCs
' #$$         movd      coreMo2,coreTmp               ' coreTmp (PC)...
' #$$         add       coreTmp,#(coreThreads-coreThreadSpecific) ' Next thread block
' #$$coreMo2  mov       0,corePC                      ' Move the PC
' #$$         add       corePC,#2                     ' Next PC (expect a GOTO)
' #$$         djnz      coreCom,#coreSetPCs           ' Do all threads
' #$$
' #$$         mov       coreSyncLast,cnt      ' Get the base count for ticking
' #$$
' #$$         jmp       #coreMainThreadStart  ' Jump right into 1st thread
' #endif
          
coreMain

' #if(MAXTHREADS>1)                                                                   
' #$$' Copy data from last run-block back to its thread
' #$$         mov       coreTmp,#coreThreadSpecific   ' Copy run-block ...
' #$$         mov       coreTmp2,coreThreadPtr        ' ... back to ...
' #$$         call      #coreThreadMove               ' ... thread data
' #$$
' #$$coreMainThreadStart
' #$$' Advance the block, roll if needed
' #$$         add       coreThreadPtr,#(coreThreads-coreThreadSpecific) ' Next thread descriptor
' #$$         djnz      coreThreadCnt,#coreAllThreads ' Not time to roll around
' #$$         mov       coreThreadCnt,coreUsedThreads ' Reset to start ...
' #$$         mov       coreThreadPtr,#coreThreads    ' ... if all done
' #$$         ' Wait on tick
' #$$         add       coreSyncLast,coreThreadTick   ' New spot in time (based on last)
' #$$         waitcnt   coreSyncLast,#0               ' Wait until desired time
' #$$coreAllThreads
' #$$         ' Copy data from this thread to run-block
' #$$         mov       coreTmp,coreThreadPtr         ' Copy thread data ...
' #$$         mov       coreTmp2,#coreThreadSpecific         ' ... to ...
' #$$         call      #coreThreadMove               ' ... run block
' #$$         cmp       coreThreadDelay,#0 wz         ' Is this thread ready to run?
' #$$  if_nz  sub       coreThreadDelay,#1            ' No ... count down delay ...
' #$$  if_nz  jmp       #coreMain                     ' And skip
' #endif  

         call      #coreGetByteTmp     ' Read the next command         
         and       coreTmp,#$80 nr, wz ' Upper bit set
  if_z   jmp       #dialect            ' No ... dialect command
         mov       coreCom,coreTmp     ' Get ...
         shr       coreCom,#3          ' ... command ...
         and       coreCom,#%0000_1111 ' ... field    
         add       coreCom,#coreTable  ' Offset into list of jumps
         jmp       coreCom             ' Jump to the command 

' Get next byte to coreTmp (always from code segment) 
coreGetByteTmp
         mov       coreTmpPC,corePC         ' The PC ...
         add       coreTmpPC,coreCodeAdr    ' ... offset into code memory
         rdbyte    coreTmp,coreTmpPC        ' Read the value
         add       corePC,#1           ' Inc PC
coreGetByteTmp_ret                     '
         ret                           ' Done

' Get next byte to coreLast (always from code segment)
coreGetByteLast                        '
         mov       coreTmpPC,corePC         ' The PC ...
         add       coreTmpPC,coreCodeAdr    ' ... offset into code memory
         rdbyte    coreLast,coreTmpPC       ' Read the value
         add       corePC,#1           ' Inc PC                    
coreGetByteLast_ret                    '
         ret                           ' Done

' Get relative address (sign extended) to coreCom (always from code segment)
coreGetRelative
         mov       coreTmpPC,corePC         ' The PC ...
         add       coreTmpPC,coreCodeAdr    ' ... offset into code memory
         rdbyte    coreCom,coreTmpPC        ' Read the value
         add       corePC,#1           ' ... to corePC
         and       coreTmp,#7          ' Add in ...
         shl       coreTmp,#8          ' ... lower 3 bits ...
         or        coreCom,coreTmp     ' ... from 1st byte
         and       coreCom,coreSXB nr, wz ' Sign extend ...         
  if_nz  or        coreCom,coreSX      ' ... if negative
coreGetRelative_ret                    '
         ret                           ' Done

' #if(MAXTHREADS>1)
' #$$' coreTmp = source
' #$$' coreTmp2 = destination
' #$$' coreCom = count
' #$$coreThreadMove
' #$$         mov       coreCom,#(coreThreads-coreThreadSpecific)
' #$$coreThreadMv1
' #$$         movs      coreMo1,coreTmp
' #$$         movd      coreMo1,coreTmp2
' #$$         add       coreTmp,#1
' #$$         add       coreTmp2,#1
' #$$coreMo1  mov       0,0
' #$$         djnz      coreCom,#coreThreadMv1
' #$$coreThreadMove_ret
' #$$         ret
' #endif

' Data operands used as source, destination, and/or value have the following
' complex format where the 1st 4 bits determines the size and function of
' the operand.
'
' Segments:
'   00 = Code
'   01 = Sys1
'   10 = Sys2
'   11 = Sys3
'
' 0000_00ss                    -- segment override prefix
'
' 0001_llll                    r- 4-bit constant  - 1 byte operand
' 0010_mmmm_llllllll           r- 12-bit constant - 2 byte operand
' 0011_0000_mmmmmmmm_llllllll  r- word constant   - 3 byte operand
'
' 00100_mmmm_llllllll          rw memory byte (12 bit data segment offset)
' 00101_mmmm_llllllll          rw memory word (12 bit data segment offset)

' 00110_mmmm_llllllll          rw indirect memory (word aligned pointer) to byte *
' 00111_mmmm_llllllll          rw indirect memory (word aligned pointer) to word (word aligned) *
'
' * Note on indirection:
'   The pointer is always word aligned. It is always in the DATA segment.
'   Any segment override prefix is used in the final target access.

' #if(12BIT==YES || MEMORY_BYTE==YES || MEMORY_WORD==YES || INDIRECT_BYTE==YES || INDIRECT_WORD==YES)
' Read LSB and make 12 bit value (all memory access starts this way)
core12Bits
         call      #coreGetByteLast    ' Get the LSB
         shl       coreTmp,#8          ' Shift over the MSB
         add       coreTmp,coreLast    ' Combine
core12Bits_ret                        '
         ret
' #endif         

coreGetValue                                                                                   
         mov       coreTmp3,#coreGVT   ' Get-function table
coreGV1  mov       coreVO,coreBaseAdr  ' Access segment starts out as DATA 
coreGV2  call      #coreGetByteTmp     ' Get the 1st byte
         mov       coreTmp2,coreTmp    ' Address/data
         shr       coreTmp2,#4         ' Specific ...
         and       coreTmp2,#15        ' ... command
         and       coreTmp,#%0000_1111 ' 4 bit value (if needed)
         add       coreTmp2,coreTmp3   ' Jump to ...
         jmp       coreTmp2            ' ... command

coreGVT
' #if(SEG_PREFIX==YES)
         jmp       #coreSegPrefix
' #endif
' #if(4BIT==YES)   
         jmp       #coreGet4Bit
' #endif
' #if(12BIT==YES)        
         jmp       #coreGet12Bit
' #endif
' #if(16BIT==YES)     
         jmp       #coreGet16Bit
' #endif
' #if(MEMORY_BYTE==YES)        
         jmp       #coreGetByte
' #endif
' #if(MEMORY_WORD==YES)         
         jmp       #coreGetWord
' #endif
' #if(INDIRECT_BYTE==YES)    
         jmp       #coreGetIByte
' #endif
' #if(INDIRECT_WORD==YES)       
         jmp       #coreGetIWord
' #endif

' -------------------------------------
' #if(SEG_PREFIX==YES)
coreSegPrefix
         add       coreTmp,#coreCodeAdr  ' Table of segment pointers
         movs      coreSeg1,coreTmp      ' Lookup source
         nop                             ' Kill a cycle
coreSeg1 mov       coreVO,0              ' Get the segment override
         jmp       #coreGV2              ' Continue with operand
' #endif     
' -------------------------------------
' #if(4BIT==YES)
coreGet4Bit
         mov       coreLast,coreTmp    ' 4-bit value already in coreTmp
' #endif
coreGetValue_ret                       '                            
         ret         
' -------------------------------------
' #if(12BIT==YES)
coreGet12Bit
         call      #core12Bits         ' 12-bit value to coreTmp
         mov       coreLast,coreTmp    ' Could jump to coreGet4Bit ... this is faster
         jmp       #coreGetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(16BIT==YES)
coreGet16Bit
         call      #coreGetByteLast    ' Get the LSB
         mov       coreTmp2,coreLast   ' Hold it
         call      #coreGetByteLast    ' Get the MSB
         shl       coreLast,#8         ' Shift it over
         add       coreLast,coreTmp2   ' Combine 16 bit value
         jmp       #coreGetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(MEMORY_BYTE==YES)
coreGetByte
         call      #core12Bits         ' Get address
         add       coreTmp,coreVO      ' Add in segment address
         rdbyte    coreLast,coreTmp    ' Get the byte value
         jmp       #coreGetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(MEMORY_WORD==YES)
coreGetWord
         call      #core12Bits         ' Get address
         add       coreTmp,coreVO      ' Add in segment address
         rdword    coreLast,coreTmp    ' Get the word value
         jmp       #coreGetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(INDIRECT_BYTE==YES)
coreGetIByte
         call      #core12Bits         ' Get pointer in ...
         add       coreTmp,coreBaseAdr ' ... data segment
         rdword    coreTmp,coreTmp     ' Get address
         add       coreTmp,coreVO      ' Add in segment address
         rdbyte    coreLast,coreTmp    ' Get the byte value
         jmp       #coreGetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(INDIRECT_WORD==YES)
coreGetIWord
         call      #core12Bits         ' Get pointer in ...
         add       coreTmp,coreBaseAdr ' ... data segment
         rdword    coreTmp,coreTmp     ' Get address
         add       coreTmp,coreVO      ' Add in segment address
         rdword    coreLast,coreTmp    ' Get the word value
         jmp       #coreGetValue_ret   ' Done
' #endif
' -------------------------------------
                                                   
' Read and evaluate the operand at the current PC and store the value
' to the evaluated address.
' coreLast is value
coreSetValue     
         mov       coreCom,coreLast    ' Hold the value to write
         mov       coreTmp3,#coreGST   ' Handler table
         jmp       #coreGV1            ' Decode write commands 
         
coreGST
' #if(SEG_PREFIX==YES)
         jmp       #coreSegPrefix
' #endif
' #if(4BIT==YES)     
         jmp       #coreSetValue_ret
' #endif
' #if(12BIT==YES)
         jmp       #coreSetValue_ret
' #endif
' #if(16BIT==YES)   
         jmp       #coreSetValue_ret
' #endif
' #if(MEMORY_BYTE==YES)   
         jmp       #coreSetByte
' #endif
' #if(MEMORY_WORD==YES)       
         jmp       #coreSetWord
' #endif
' #if(INDIRECT_BYTE==YES)               
         jmp       #coreSetIByte'
' #endif       
' #if(INDIRECT_WORD==YES) 
         jmp       #coreSetIWord
' #endif       
         
' -------------------------------------
' #if(MEMORY_BYTE==YES)
coreSetByte
         call      #core12Bits         ' Get the destination
         add       coreTmp,coreVO      ' Add in segment address
         wrbyte    coreCom,coreTmp     ' Write the value
' #endif     
coreSetValue_ret                       '
         ret                           ' Done        
' -------------------------------------
' #if(MEMORY_WORD==YES)
coreSetWord
         call      #core12Bits         ' Get the destination
         add       coreTmp,coreVO      ' Add in segment address
         wrword    coreCom,coreTmp     ' Write the word value
         jmp       #coreSetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(INDIRECT_BYTE==YES)
coreSetIByte
         call      #core12Bits         ' Get pointer in ...
         add       coreTmp,coreBaseAdr ' ... data segment
         rdword    coreTmp,coreTmp     ' Get address
         add       coreTmp,coreVO      ' Add in segment address
         wrbyte    coreCom,coreTmp     ' Write the value
         jmp       #coreSetValue_ret   ' Done
' #endif
' -------------------------------------
' #if(INDIRECT_WORD==YES)
coreSetIWord
         call      #core12Bits         ' Get pointer in ...
         add       coreTmp,coreBaseAdr ' ... data segment
         rdword    coreTmp,coreTmp     ' Get address
         add       coreTmp,coreVO      ' Add in segment address
         wrword    coreCom,coreTmp     ' Write the value
         jmp       #coreSetValue_ret   ' Done
' #endif
' -------------------------------------
coreBaseAdr        long  $6000          
coreCodeAdr        long  $7000         
coreSys1Adr        long  $7800         
coreSys2Adr        long  $6000     
' -- Thread specifics begin here
coreThreadSpecific     
coreSys3Adr        long  $6000
'
corePC             long  0             ' Program counter
coreCarry          long  0             ' Carry value (1 or 0)   
coreResult         long  0             ' Last result
' #if(coreCALL==YES)
coreStackPointer   long  0             ' Index into stack
' Call stack (configured by CALL_STACK_SIZE) 
coreStack          long  0,0,0,0
                 
' #endif
' #if(MAXTHREADS>1)
' #$$coreThreadDelay    long  0             ' Thread ticks before processing this thread
' #endif   

' #paste THREAD_SPECIFIC

' Config tool adds space for each thread here

' -- Thread specifics end here
coreThreads    

' #if(MAXTHREADS>1)                   
' #$$coreThreadPtr      long  0             ' Current thread structure being run
' #$$coreThreadCnt      long  1             ' Reload threadPtr when reaches 0
' #$$coreThreadTick     long  $00_00_80_00  ' Count delay between passes
' #$$coreUsedThreads    long  1             ' #@MAXTHREADS@ Filled in at startup
' #endif
coreSyncLast       long  0             ' Last time value used in SYNC command
coreSX             long  %11111111_11111111_11111000_00000000 ' Sign extend
coreSXB            long  %00000000_00000000_00000100_00000000
coreTmp            long  0             ' Temp used in command processing
coreTmp2           long  0             ' Temp used in address decoding
coreTmp3           long  0             ' Temp used in address decoding
coreCom            long  0             ' Command number
coreTmp4           long  0             ' Source address
coreTmpPC          long  0
coreLast           long  0             

' #if(coreMULT==YES || coreDIV==YES || coreREM==YES || MULTIPLY==YES || DIVIDE==YES)
coreT1             long  0             ' For multiply and divide
coreT2             long  0             ' For multiply and divide
coreT3             long  0             ' For multiply and divide
' #endif

coreVO             long  0             ' Segment offset for data access

coreTable
         jmp       #coreGOTO           ' #command-table
         jmp       #coreGOTOCOND       ' #command-table         
         jmp       #coreCALL           ' #command-table
         jmp       #coreRETURN         ' #command-table
         jmp       #coreSTOP           ' #command-table 
         jmp       #corePAUSE          ' #command-table
         jmp       #coreSYNC           ' #command-table
         jmp       #coreMOVE           ' #command-table    
         jmp       #coreNATIVEMATH     ' #command-table         
         jmp       #coreMULT           ' #command-table
         jmp       #coreDIV            ' #command-table
         jmp       #coreREM            ' #command-table
         jmp       #coreTHREADGOTO     ' #command-table     

''
'' THEADGOTO rel, thread
'' 1_cccc_rrr__rrrrrrrr tt..
' #command-begin
coreTHREADGOTO
' #$$         call      #coreGetRelative    ' Get the relative offset (sign extended) to coreCom
' #$$         call      #coreGetValue       ' Read the thread number
' #$$         add       coreLast,#1         ' Doing at least one
' #$$         mov       coreTmp2,#coreThreads-(coreThreads-coreThreadSpecific)
' #$$coreTG1  add       coreTmp2,#(coreThreads-coreThreadSpecific)
' #$$         djnz      coreLast,#coreTG1   ' Find the pointer to the thread block
' #$$         add       coreTmp2,#(corePC-coreThreadSpecific) ' Offset to PC
' #$$         movd      coreTG2,coreTmp2    ' Destination of ADD later
' #$$         add       coreTmp2,#(coreStackPointer-corePC) ' Offset to stack pointer
' #$$         movd      coreTG3,coreTmp2    ' Destination of MOV later
' #$$         add       coreCom,corePC      ' Offset from CURRENT PC
' #$$coreTG2  mov       0,coreCom           ' Bump target thread's PC
' #$$coreTG3  mov       0,#0                ' Reset target thread's stack
' #$$         jmp       #coreMain           ' Done
' #command-end

''
'' GOTO rel
'' 1_cccc_rrr__rrrrrrrr
' #command-begin
coreGOTO 
         call      #coreGetRelative    ' Get the relative offset (sign extended)
         add       corePC,coreCom      ' Offset from current PC
         jmp       #coreMain           ' Done
' #command-end
  
''
'' GOTO-COND rel, cond
'' 1_cccc_rrr__rrrrrrrr  0000_nnnn
' #command-begin
coreGOTOCOND
         call      #coreGetRelative    ' Get the relative offset
         call      #coreGetByteTmp     ' Get the conditional bits
         shl       coreTmp,#18         ' This is where they go in the opcode
         and       coreGOCA,coreCMSK   ' Mask out last conditions
         or        coreGOCA,coreTmp    ' Set the conditional bits
         andn      coreResult,#0  wz,nr ' Set the ZERO flag from last result
         shr       coreCarry,#1 wc, nr  ' Set the CARRY flag from last result
coreGOCA
if_e     add       corePC,coreCom      ' Take the jump (if conditions match)
         jmp       #coreMain'          ' Next command
'              iiiiii_oooo_cccc_ddddddddd_sssssssss
coreCMSK long %111111_1111_0000_111111111_111111111
' #command-end
    
''
'' CALL rel
'' 1_cccc_rrr__rrrrrrrr
' #command-begin
coreCALL
         mov       coreCom,coreStackPointer ' Get the stack pointer
         add       coreCom,#coreStack  ' Offset into stack
         movd      coreCALL_i,coreCom  ' Destination pointer
         add       coreStackPointer,#1 ' Inc the stack
         mov       coreCom,corePC      ' Return to ...
         add       coreCom,#1          ' ... after fetching offset
coreCALL_i
         mov       0,coreCom           ' Write the PC to the stack
         jmp       #coreGOTO           ' Continue as if GOTO
' #command-end

''
'' RETURN
'' 1_cccc_000
' #command-begin
coreRETURN
         sub       coreStackPointer,#1 ' Dec stack pointer
         mov       coreCom,coreStackPointer ' Get the stack pointer
         add       coreCom,#coreStack  ' Offset into stack
         movs      coreRET_i,coreCom   ' Source pointer
         nop                           ' Kill a cycle before using the address
coreRET_i                              '
         mov       corePC,0            ' Read the PC from the stack
         jmp       #coreMain           ' Done
' #command-end

''
'' STOP
'' 1_cccc_000
' Other implementations may want to return from the engine loop here
' #command-begin
coreSTOP
         sub       corePC,#1           ' Point back to the STOP
         jmp       #coreMain           ' Endless loop
' #command-end
        
''
'' PAUSE n
'' 1_cccc_000__ value...
''  The N value is shifted left 16 here. Some values:
''  FFFF<<16 =  FFFF0000 / 80_000_000 =  53.686272 sec (word max)
''   FFF<<16 =   FFF0000 / 80_000_000 =   3.354624 sec (12-bit max)
''    FF<<16 =    FF0000 / 80_000_000 = 208.896  msec  (byte max)
''     F<<16 =     F0000 / 80_000_000 =  12.288  msec  (4-bit max)
''     1<<16 =     10000 / 80_000_000 =   0.8192 msec  (min)
''
'' For multithreads, the pause is (value+1)*tick_period
''   (value+1)*tic_value/80_000_000
' #command-begin
corePAUSE
         call      #coreGetValue       ' Get the value
' #if(MAXTHREADS>1)
' #$$         mov       coreThreadDelay,coreLast ' Delay the thread N ticks
' #$$         jmp       #coreMain           'Done
' #endif
' #if(MAXTHREADS==1)
         shl       coreLast,#16        ' Shift left
         add       coreLast,cnt        ' Wait ...
         waitcnt   coreLast,#0         ' ... desired count
         jmp       #coreMain           ' Done
' #endif
' #command-end

''
'' SYNC n
'' 1_cccc_000__ value...
' #command-begin
coreSYNC
         call      #coreGetValue       ' Get the value
         cmp       coreLast,#0 wz      ' A zero ...
  if_z   jmp       #coreSYN1           ' ... initializes the sync count
         shl       coreLast,#16        ' Shift left
         add       coreSyncLast,coreLast ' New spot in time (based on last)
         waitcnt   coreSyncLast,#0     ' Wait until desired time
         jmp       #coreMain           ' Done
coreSYN1 mov       coreSyncLast,cnt    ' Initialize ...
         jmp       #coreMain           ' ... spot in time to NOW
' #command-end

' #if(coreMULT==YES || coreDIV==YES || coreREM==YES || coreNATIVEMATH==YES)
coreMathPre
         call      #coreGetValue       ' Get value B
         mov       coreTmp4,coreLast   ' Hold onto it
         call      #coreGetValue       ' Get value A
coreMathPre_ret
         ret
' #endif

''
'' NATIVEMATH op,b,a,dest 
'' 1_cccc_000 00oooooo b... a... dest...
' #command-begin
coreNATIVEMATH
         call      #coreGetByteLast    ' Get the 6-bit instruction
         shl       coreLast,#3         ' Set ...
         or        coreLast,#1         ' ... ZCR flags
         movi      coreMNI,coreLast    ' Set the instruction for later
         call      #coreMathPre        ' Get the values                     
coreMNI  sub       coreLast,coreTmp4   ' Do the math
    ' Set flags and write result (maybe)
    ' Note that carry is always based on 16-bit overflow
' #command-end
' #if(coreNATIVEMATH==YES || coreMULT==YES || coreDIV==YES || coreREM==YES)
coreMathPost
         cmp       coreLast,C_FFFF wz, wc ' Set carry ...
   if_a  mov       coreCarry,#1        ' ... or ...
   if_be mov       coreCarry,#0        ' ... clear carry
         mov       coreResult,coreLast ' Hold onto math result
         call      #coreSetValue       ' Write the value
         jmp       #coreMain           ' Done
' #endif
C_FFFF   long      $FFFF               ' Max word value for carry

 
''
'' MOVE a,dest
' #command-begin
coreMOVE
         call      #coreGetValue       ' Get the value
         mov       coreResult,coreLast ' Hold onto value
         call      #coreSetValue       ' Store the value
         jmp       #coreMain           ' Done
' #command-end 

''
'' MULT b,a,dest
'' 1_cccc_000__ b... a... dest...
' #command-begin
coreMULT
         call      #coreMathPre        ' Get the values
         mov       coreT1,coreLast     ' The ...
         and       coreT1,C_FFFF       ' ... left value
         mov       coreT2,coreTmp4     ' The ...
         and       coreT2,C_FFFF       ' ... right value
         call      #coreMultiply       ' Do the math
         mov       coreLast,coreT1     ' Result
         jmp       #coreMathPost       ' Set flags and write result (maybe)
' #command-end 

''
'' DIV b,a,dest
'' 1_cccc_000__ b... a... dest...
' #command-begin
coreDIV
         call      #coreMathPre        ' Get the values
         mov       coreT1,coreLast     ' The ...
         and       coreT1,C_FFFF       ' ... left value
         mov       coreT2,coreTmp4     ' The ...
         and       coreT2,C_FFFF       ' ... right value
         call      #coreDivide         ' Do the math
         mov       coreLast,coreT1     ' Mask off ...
         and       coreLast,C_FFFF     ' ... result
         jmp       #coreMathPost       ' Set flags and write result (maybe)
' #command-end 

''
'' REM b,a,dest
'' 1_cccc_000__ b... a... dest...
' #command-begin
coreREM
         call      #coreMathPre        ' Get the values
         mov       coreT1,coreLast     ' The ...
         and       coreT1,C_FFFF       ' ... left value
         mov       coreT2,coreTmp4     ' The ...
         and       coreT2,C_FFFF       ' ... right value
         call      #coreDivide         ' Do the math
         mov       coreLast,coreT1     ' Get ...
         shr       coreLast,#16        ' ... the ...
         and       coreLast,C_FFFF     ' ... remainder
         jmp       #coreMathPost       ' Set flags and write result (maybe)
' #command-end

' Multiply (from propeller video driver)
'
'   in:  t1 = 16-bit multiplicand (t1[31..16] must be 0)
'        t2 = 16-bit multiplier
'   out: t1 = 32-bit product
'
' #if(coreMULT==YES || MULTIPLY==YES)
coreMultiply
         mov       coreT3,#16
         shl       coreT2,#16
         shr       coreT1,#1 wc
coreMloop
  if_c   add       coreT1,coreT2 wc
         rcr       coreT1,#1 wc
         djnz      coreT3,#coreMloop
coreMultiply_ret
         ret
' #endif

' http://forums.parallax.com/forums/attach.aspx?a=16161
' Divide t1[31..0] by t2[15..0] (t2[16] must be 0)
' on exit, quotient is in t1[15..0] and remainder is in t1[31-16]
'
' #if(coreDIV==YES || coreREM==YES || DIVIDE==YES)
coreDivide
          shl       coreT2,#15
          mov       coreT3,#16
coreDloop cmpsub    coreT1,coreT2 wc
          rcl       coreT1,#1
          djnz      coreT3,#coreDloop
coreDivide_ret
          ret
' #endif

' ----------------------------------------------------------------------------
' ----------------------------------------------------------------------------
' End of the common CORE of the engine. Specific dialect processing comes
' here.
' ----------------------------------------------------------------------------
' ----------------------------------------------------------------------------

' #dialectCommand gx.g2.core.ledmovie.Parse_LOADCLUSTER(0)    // page,cluster
' #dialectCommand gx.g2.core.ledmovie.Parse_RENDERFRAME(1)    // page,frame
' #dialectCommand gx.g2.core.ledmovie.Parse_WAITONTICK(2)
' #dialectCommand gx.g2.core.ledmovie.Parse_WAITONLED(3)
' #dialectCommand gx.g2.core.ledmovie.Parse_WAITONDISK(4)

' #dialectCommand gx.g2.core.Parse_DATA(-1)
                                
'' LEDMovie
''
'' SYS2 points to the frame memory ... two pages of 2K each
''
'' SYS3 points to hardware control block as follows (each entry a LONG):
''
''   DiskCommand
''   DiskSectorAddress 
''   DiskMemoryPointer
''
''   LEDControl-A
''   LEDMemory-A
''   LEDControl-B
''   LEDMemory-B
''
''   TimingTick

'#copy THREAD_SPECIFIC
'#endcopy


diskControl   long 0
diskSector    long 0
diskMemory    long 0
ledControlA   long 0
ledMemoryA    long 0 
ledControlB   long 0 
ledMemoryB    long 0
tick          long 0

pageNumber    long 0
clusterNumber long 0
tmp           long 0
hold          long 0
waitFlag      long 0 

dialect  rdlong    hold,tick           ' Current tick value
         mov       coreCom,coreTmp     ' Original value
         and       coreCom,#%00000011  ' Command number
         add       coreCom,#dialTable  ' Offset into list of jumps
         jmp       coreCom             ' Jump to the command 

dialTable
         jmp      #cmdLoadCluster      ' 0_0000000 page, cluster 
         jmp      #cmdRenderFrame      ' 0_0000001 page, frame
         jmp      #cmdWaitOnTick       ' 0_0000010
         jmp      #cmdWaitOnLED        ' 0_0000011
         jmp      #cmdWaitOnDisk       ' 0_0000100

''
'' LoadCluster
'' 0_0000000 page, cluster
cmdLoadCluster

         call      #coreGetValue             ' Get the ...
         mov       pageNumber,coreLast       ' ... page number

loWait1  call      #coreGetValue             ' Get the ...
         mov       clusterNumber,coreLast    ' ... cluster number
         
wDisk1   rdlong    coreT1,diskControl wz     ' Wait for the disk driver ... 
  if_nz  jmp       #wDisk1                   ' ... to finish last command

         shl       clusterNumber,#2          ' 4 sectors per cluster
         shl       pageNumber,#11            ' page * 2K
         add       pageNumber,coreSys2Adr    ' Offset off of SYS2         

         wrlong    clusterNumber,diskSector  ' SD address
         wrlong    pageNumber,diskMemory     ' Memory address
         mov       coreT1,#%01_00000100      ' Start reading ...                  
         wrlong    coreT1,diskControl        ' ... 4 sectors
                             
         jmp       #coreMain                 ' Next command
         
''
'' RenderFrame
'' 0_0000001 page, frame
cmdRenderFrame

         call      #coreGetValue             ' Get the ...
         mov       pageNumber,coreLast       ' ... page number          
         
         call      #coreGetValue             ' Get the ...
         mov       coreT1,coreLast           ' ... frame number

         mov       coreT2,#192               ' 192 bytes ...
         call      #coreMultiply             ' ... per frame         

         shl       pageNumber,#11            ' Page * 2K
         add       pageNumber,coreT1             ' Add frame*192
         add       pageNumber,coreSys2Adr    ' Offset SYS2

         wrlong    pageNumber,ledMemoryA     ' Memory for A
         add       pageNumber,#96            ' Add 96
         wrlong    pageNumber,ledMemoryB     ' Memory for B

         mov       coreT1,#1                 ' Start ...
         wrlong    coreT1,ledControlA        ' ... LED ...  
         wrlong    coreT1,ledControlB        ' ... refresh

         jmp       #coreMain                 ' Next command                 

''
'' WaitOnTick
'' 0_0000010
cmdWaitOnTick
         rdlong    coreT1,tick               ' Get current tick
         cmp       coreT1,hold wz            ' Has it changed?
   if_z  jmp       #cmdWaitOnTick            ' No ... wait
         jmp       #coreMain                 ' Continue to next command
         
''
'' WaitOnLED
'' 0_0000011
cmdWaitOnLED
         rdlong    coreT1,ledControlB wz     ' Is LED-B working?
  if_nz  jmp       #cmdWaitOnLED             ' Yes ... wait
         jmp       #coreMain                 ' Continue to next command

''
'' WaitOnDisk
'' 0_0000100
cmdWaitOnDisk
         rdlong    coreT1,diskControl wz    ' Is the Disk working?
  if_nz  jmp       #cmdWaitOnDisk           ' Yes ... wait
         jmp       #coreMain                ' Continue to next command

lastAdr  fit