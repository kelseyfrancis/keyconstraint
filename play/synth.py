import itertools
from math import pi, sin
import numpy as np
import random
import struct
from subprocess import Popen, PIPE
import sys
from threading import Thread
from time import sleep, time

_sine_table_size = 1000
_sine_table = dict(list([ (i, sin(2 * pi * i / _sine_table_size)) for i in range(0, _sine_table_size + 1) ]))
def sine_table(x):
  return sin(2*pi*x)
  return _sine_table[round(_sine_table_size * x)]

class Context:

  def __init__(self):
    self._sample_rate = 44100
    self._modules = []
    self._player = Player(self)

  def add_module(self, module):
    self._modules.append(module)

  def modules(self):
    self._modules = filter(lambda m : m.liveness() != 'dead', self._modules)
    return dict(list([ (liveness, list(filter(lambda m : m.liveness() == liveness, self._modules))) for liveness in ['live', 'sleep'] ]))

  def open(self):
    command = 'aplay -f cd -c1 -r %d' % int(self.sample_rate())
    return Popen(command.split(), stdin=PIPE)

  def start(self):
    self._player.start()

  def stop(self):
    self._player.stop()

  def sample_rate(self):
    return self._sample_rate

  def sine(self, freq=None, amp=1, noise=0):
    return Sine(self, freq, amp, noise)

  def triangle(self, freq=None, amp=1, noise=0):
    return Triangle(self, freq, amp, noise)

  def square(self, freq=None, amp=1, noise=0):
    return Square(self, freq, amp, noise)

  def fm(self, carrier, modulator):
    return FreqMod(carrier, modulator)

  def interval(self, module, delay_seconds=0, duration_seconds=None):
    return Interval(self, module, delay_seconds, duration_seconds)

  def am(self, carrier, modulator):
    return AmpMod(self, carrier, modulator)

  def adsr(self, a, d, s, r):
    return ADSR(self, a, d, s, r)

class Player ( Thread ):

  def __init__(self, context):
    super(Player, self).__init__()
    self._context = context
    self._halt = False
    self._buffer_ahead = 0.005
    self._sleep = 0.001

  def run(self):
    c = self._context
    audio = c.open()
    buffer_increment = 1
    t = time()
    t_inc = 1. / c.sample_rate()
    intensity_shift = 2**15
    while not self._halt:
      now = time()
      modules = c.modules()
      live_modules = modules['live']
      t_diff = 0
      while (t < now + self._buffer_ahead):
        t_diff = t_diff + 1
        t = t + t_inc
        x = sum([ m.next(t = 1.) for m in live_modules ])
        s = int(x * 100) + intensity_shift
        audio.stdin.write(struct.pack('<H', s))
      for m in modules['sleep']:
        if m.skip:
          m.skip(t_diff)
      sleep(self._sleep)
    audio.kill()

  def stop(self):
    self._halt = True

class Sine:

  def __init__(self, context, freq, amp, noise):
    self._context = context
    self._freq = freq
    self._amp = amp
    self._phase = 0
    self._noise = noise

  def next(self, t):
    self._phase = ( self._phase + ( t * self._freq / self._context.sample_rate() ) ) % 1.
    x = sine_table( self._phase )
    if self._noise != 0:
      x = x + random.gauss(0, self._noise)
    return x * self._amp

  def liveness(self):
    return 'live'

class Triangle:

  def __init__(self, context, freq, amp, noise):
    self._context = context
    self._freq = freq
    self._amp = amp
    self._phase = 0
    self._noise = noise

  def next(self, t):
    self._phase = ( self._phase + ( 2 * t * self._freq / self._context.sample_rate() ) ) % 2.
    p = self._phase
    if self._noise != 0:
      p = p + random.gauss(0, self._noise)
    if self._phase < 1:
      return 2. * p * self._amp - 1
    else:
      return -1. * (p * self._amp) + 1

  def liveness(self):
    return 'live'

class Square:

  def __init__(self, context, freq, amp, noise):
    self._context = context
    self._freq = freq
    self._amp = amp
    self._phase = 0
    self._noise = noise

  def next(self, t):
    self._phase = ( self._phase + ( 2 * t * self._freq / self._context.sample_rate() ) ) % 2.
    x = -1 if self._phase < 1 else 1
    if self._noise != 0:
      x = x + random.gauss(0, self._noise)
    return x

  def liveness(self):
    return 'live'

class FreqMod:

  def __init__(self, carrier, modulator):
    self._carrier = carrier
    self._modulator = modulator

  def next(self, t):
    return self._carrier.next( t + t * self._modulator.next(t) / 2 )

  def liveness(self):
    c = self._carrier.liveness()
    m = self._modulator.liveness()
    if c == 'dead' or m == 'dead': return 'dead'
    if c == 'sleep' or m == 'sleep': return 'sleep'
    return 'live'

class Interval:
  
  def __init__(self, context, module, delay_seconds=0, duration_seconds=None):
    self._module = module
    self._delay_remaining = round(context.sample_rate() * delay_seconds)
    self._play_remaining = None if duration_seconds is None else round(context.sample_rate() * duration_seconds)

  def next(self, t):
    if self._delay_remaining != 0:
      self._delay_remaining = self._delay_remaining - 1
      return 0
    if self._play_remaining is None:
      return self._module.next(t)
    if self._play_remaining != 0:
      self._play_remaining = self._play_remaining - 1
      return self._module.next(t)
    return 0

  def skip(self, t):
    self._delay_remaining = max(0, self._delay_remaining - t)

  def liveness(self):
    if self._delay_remaining != 0: return 'sleep'
    if self._play_remaining is None or self._play_remaining != 0: return self._module.liveness()
    return 'dead'

class AmpMod:

  def __init__(self, context, carrier, modulator):
    self._carrier = carrier
    self._modulator = modulator

  def next(self, t):
    if self.liveness() != 'live':
      return 0
    return self._carrier.next(t) * self._modulator.next(1)

  def liveness(self):
    e = self._modulator.liveness()
    m = self._carrier.liveness()
    if e == 'dead' or m == 'dead': return 'dead'
    if e == 'sleep' or m == 'sleep': return 'sleep'
    return 'live'

class ADSR:

  def __init__(self, context, a, d, s, r):
    self._duration = list([ d * context.sample_rate() for d in [a,d,s,r] ])
    self._t = 0.
    self._sustain_amp = .6
    self._section = 'a'

  def next(self, t):
    d = self._duration
    _t = self._t
    sa = self._sustain_amp
    if self._section == 'a':
      x = _t / d[0]
      if t > d[0]:
        self._section = 'd'
        self._t = 0
    elif self.section == 'd':
      x = sa + _t * (1-sa) / d[1]
      if _t > d[1]:
        self._section = 's'
        self._t = 0
    elif self._section == 's':
      x = sa
      if _t > d[2]:
        self._section = 'r'
        self._t = 0
    elif self._section == 'r':
      x = sa - _t * sa / d[3]
      if _t > d[3]:
        self._section = None
        self._t = 0
    else:
      x = 0
    self._t = _t + t
    return x

  def liveness(self):
    return 'live' if self._section is not None else 'dead'

