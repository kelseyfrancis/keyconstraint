import itertools
from math import pi, sin
import struct
from subprocess import Popen, PIPE
import sys
from threading import Thread
from time import sleep, time

class Context:

  def __init__(self):
    self._sample_rate = 44100
    self._modules = []
    self._player = Player(self)

  def add_module(self, module):
    self._modules.append(module)

  def modules(self):
    self._modules = filter(lambda m : m.is_live(), self._modules)
    return list(self._modules)

  def open(self):
    command = 'aplay -f cd -r %d' % int(self.sample_rate())
    return Popen(command.split(), stdin=PIPE).stdin

  def start(self):
    self._player.start()

  def stop(self):
    self._player.stop()

  def sample_rate(self):
    return self._sample_rate

  def sine(self, note=None, freq=None, amp=1):
    return Sine(self, note, freq, amp)

  def fm(self, carrier, modulator):
    return FreqMod(carrier, modulator)

  def interval(self, module, delay_seconds, duration_seconds):
    return Interval(self, module, delay_seconds, duration_seconds)

class Player ( Thread ):

  def __init__(self, context):
    super(Player, self).__init__()
    self._context = context
    self._halt = False

  def run(self):
    c = self._context
    audio = c.open()
    buffer_increment = 1
    t = time()
    while not self._halt:
      now = time()
      while (t < now + 0.005):
        t = t + 1. / c.sample_rate()
        x = sum([ m.next() for m in c.modules() ])
        s = int(x * 100) + 2**15
        audio.write(struct.pack('<I', s))
      sleep(.001)

  def stop(self):
    self._halt = True

class Sine:

  def __init__(self, context, note, freq, amp):
    self._context = context
    self._freq = freq if freq is not None else note.frequency()
    self._amp = amp
    self._phase = 0

  def next(self, t=1):
    self._phase = ( self._phase + ( t * self._freq * 2 * pi / self._context.sample_rate() ) ) % ( 2 * pi )
    return sin( self._phase ) * self._amp

  def is_live(self):
    return True

class FreqMod:

  def __init__(self, carrier, modulator):
    self._carrier = carrier
    self._modulator = modulator

  def next(self, t=1):
    return self._carrier.next(t * abs(self._modulator.next(t)))

class Interval:
  
  def __init__(self, context, module, delay_seconds, duration_seconds):
    self._module = module
    self._delay_remaining = context.sample_rate() * delay_seconds
    self._play_remaining = context.sample_rate() * duration_seconds

  def next(self, t=1):
    if self._delay_remaining != 0:
      self._delay_remaining = self._delay_remaining - 1
      return 0
    if self._play_remaining != 0:
      self._play_remaining = self._play_remaining - 1
      return self._module.next(t)
    return 0

  def is_live(self):
    return self._delay_remaining != 0 or self._play_remaining != 0

