from math import pi, sin
from subprocess import Popen, PIPE
import sys
from threading import Thread
from time import sleep, time

class Play ( Thread ):

  def __init__(self):
    super(Play, self).__init__()
    self._halt = False
    self.tmp = 20

  def run(self):
    audio = Popen('aplay', stdin=PIPE).stdin
    buffer_increment = 1
    t = time()
    phase = 0
    while not self._halt:
      freq = 500 + 500 * ( 1 - ( self.tmp - 21 ) / 9 )
      now = time()
      while (t < now + 0.005):
        t = t + 1. / 8000
        phase = ( phase + ( freq * 2 * pi / 8000 ) ) % ( 2 * pi )
        audio.write( chr( int( sin( phase ) * 50 + 128 ) ) )
      sleep(.001)

  def stop(self):
    self._halt = True

class Note:

  def __init__(self):
    pass

if __name__ == '__main__':
  sys.stdin.readline()

