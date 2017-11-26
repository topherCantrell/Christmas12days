CON
        _clkmode        = xtal1 + pll16x
        _xinfreq        = 5_000_000

        ZERO = 1
        ONE = 0

        '0 bottom right
        '3 bottom left
        '6 upper left
        '9 upper right
        
        P_CS = 3
        P_WR = 4
        P_DATA = 5
        
PUB start : i

outa[27]:=1
dira[27]:=1  

dira[P_CS] :=1
dira[P_WR] :=1
dira[P_DATA] :=1

outa[P_CS] := ONE  ' ~CS
outa[P_WR] := ONE  ' ~WR
outa[P_DATA] := ONE  ' DATA

writeCommand($0)
writeCommand($2C)
writeCommand($14)
writeCommand($01)
writeCommand($03)

initWrite(0)
repeat i from 1 to 12
  writeNibble($AA)
  writeNibble($AA)
  writeNibble($AA)
  writeNibble($AA)
  writeNibble($55)
  writeNibble($55)
  writeNibble($55)
  writeNibble($55)

outa[P_CS] := ONE

waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)
waitcnt(3_000_000 + cnt)

'initWrite(0)
'writeNibble(8)
'writeNibble(5) 
'writeNibble(5)
'writeNibble(5)

'writeNibble(10)
'writeNibble(10)
'writeNibble(10)
'writeNibble(10)

'writeNibble(5)
'writeNibble(5)
'writeNibble(5) 
'writeNibble(5)


outa[P_CS] := ONE   

  repeat
    !outa[27]
    waitcnt(3_000_000 + cnt)

PUB writeNibble(data)
  writeBit(data>>0)
  writeBit(data>>1)
  writeBit(data>>2)
  writeBit(data>>3)

PUB writeCommand(command)
  outa[P_CS]:=ZERO ' ~CS=0 (enable)
  writeBit(1)
  writeBit(0)
  writeBit(0)
  writeBit(command>>7)
  writeBit(command>>6)
  writeBit(command>>5)
  writeBit(command>>4)
  writeBit(command>>3)
  writeBit(command>>2)
  writeBit(command>>1)
  writeBit(command>>0)
  writeBit(1) ' X
  outa[P_CS]:=ONE ' ~CS=1 (disable)
  waitcnt(3_0_000 + cnt) 

PUB initWrite(address)
  
  outa[P_CS]:=ZERO  ' ~CS=0 (enable)
  writeBit(1) ' Write 101 (write command)
  writeBit(0)
  writeBit(1)

  writeBit(address>>6) ' Address is 0
  writeBit(address>>5)
  writeBit(address>>4)
  writeBit(address>>3)
  writeBit(address>>2)
  writeBit(address>>1)
  writeBit(address>>0)  

PUB writeBit(bit) : i

  i := bit&1
  outa[P_WR]:=ZERO               ' ~WR = 0
  if(i==1)
    outa[P_DATA]:=ONE
  else
    outa[P_DATA]:=ZERO
  outa[P_WR]:=ONE               ' ~WR = 1
    