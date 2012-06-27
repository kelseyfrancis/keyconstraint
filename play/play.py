import itertools
from math import pi, sin
import struct
from subprocess import Popen, PIPE
import sys
from threading import Thread
from time import sleep, time

import music

class Context:

  def __init__(self):
    self.sample_rate = 44100

  def open(self):
    command = 'aplay -f cd -r %d' % int(self.sample_rate)
    return Popen(command.split(), stdin=PIPE).stdin

class Play ( Thread ):

  def __init__(self, context):
    super(Play, self).__init__()
    self.context = context
    self._halt = False
    self.modules = []

  def add(self, module):
    self.modules.append(module)

  def run(self):
    audio = self.context.open()
    buffer_increment = 1
    t = time()
    while not self._halt:
      now = time()
      while (t < now + 0.005):
        t = t + 1. / self.context.sample_rate
        x = sum([ m.next() for m in self.modules ])
        s = int(x * 100) + 2**15
        audio.write(struct.pack('<I', s))
      self.modules = filter(lambda m : m.is_live(), self.modules)
      sleep(.001)

  def stop(self):
    self._halt = True

class Sine:

  def __init__(self, context, note=None, freq=None, amp=1):
    self.context = context
    self.freq = freq if freq is not None else music.frequency(note)
    self.amp = amp
    self.phase = 0

  def next(self):
    self.phase = ( self.phase + ( self.freq * 2 * pi / self.context.sample_rate ) ) % ( 2 * pi )
    return sin( self.phase ) * self.amp

  def is_live(self):
    return True

class TimeRange:
  
  def __init__(self, context, module, delay_seconds, duration_seconds):
    self.module = module
    self.delay_remaining = context.sample_rate * delay_seconds
    self.play_remaining = context.sample_rate * duration_seconds

  def next(self):
    if self.delay_remaining != 0:
      self.delay_remaining = self.delay_remaining - 1
      return 0
    if self.play_remaining != 0:
      self.play_remaining = self.play_remaining - 1
      return self.module.next()
    return 0

  def is_live(self):
    return self.delay_remaining != 0 or self.play_remaining != 0

if __name__ == '__main__':
  c = Context()
  p = Play(c)
  p.daemon = True
  notes = list(itertools.islice(music.notes('g#', 4, start='B', step=-1), 0, 8))
  duration = .5
  for i, n in enumerate(notes):
    p.add(TimeRange(c, Sine(c, note=n), i * duration, duration))
  p.start()
  sys.stdin.readline()
  p.stop()

