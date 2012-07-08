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

def triangle(x):
  return (4. * x - 1) if x < .5 else (-2. * x + 1)

def square(x):
  return -1 if x < .5 else 1

class Context:

  def __init__(self):
    self._sample_rate = 44100 / 2
    self._player = Player(self)

  def open(self):
    command = 'aplay -f cd -c1 -r %d' % int(self.sample_rate())
    return Popen(command.split(), stdin=PIPE)

  def start(self, module): 
    self._player._module = module
    self._player.start()
  
  def stop(self): 
    self._player.stop()

  def sample_rate(self): 
    return self._sample_rate

  def add(*args, **kwargs): return Addition(*args, **kwargs)
  def osc(*args, **kwargs): return Oscillator(*args, **kwargs)
  def sine(*args, **kwargs): return Oscillator(*args, waveform=sine_table, **kwargs)
  def triangle(*args, **kwargs): return Oscillator(*args, waveform=triangle, **kwargs)
  def square(*args, **kwargs): return Oscillator(*args, waveform=square, **kwargs)
  def fm(*args, **kwargs): return FreqMod(*args, **kwargs)
  def interval(*args, **kwargs): return Interval(*args, **kwargs)
  def am(*args, **kwargs): return AmpMod(*args, **kwargs)
  def adsr(*args, **kwargs): return ADSR(*args, **kwargs)
  def irfilter(*args, **kwargs): return LinearFilter(*args, **kwargs)

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
    h = 0
    m = self._module
    while not self._halt:
      now = time()
      h = (h + 1) if m.liveness() == 'dead' else 0
      if h > 5: break
      while (t < now + self._buffer_ahead):
        t = t + t_inc
        x = m.next(t = 1.)
        s = int(x * 100) + intensity_shift
        audio.stdin.write(struct.pack('<H', s))
      sleep(self._sleep)

    sleep(2)
    audio.kill()

  def stop(self):
    self._halt = True

class LinearFilter:

  def __init__(self, context, coefficients, module):
    self._coefficients = np.array(coefficients)
    self._samples = np.zeros(self._coefficients.shape)
    self._module = module

  def next(self, t):
    x = np.dot(self._coefficients.ravel(), self._samples.ravel())
    np.roll(self._samples, 1)
    self._samples[0][0] = self._module.next(t)
    self._samples[1][0] = x
    return x

  def liveness(self):
    return self._module.liveness()

class Addition:

  def __init__(self, context, modules=None):
    self._modules = modules if modules else []
    self._live_modules_check = 0

  def add_module(self, module):
    self._modules.append(module)

  def modules(self):
    self._modules = filter(lambda m : m.liveness() != 'dead', self._modules)
    return dict(list([ (liveness, list(filter(lambda m : m.liveness() == liveness, self._modules))) for liveness in ['live', 'sleep'] ]))

  def live_modules(self):
    if self._live_modules_check % 500 == 0:
      self._live_modules = self.modules()['live']
      for m in self.modules()['sleep']:
        if m.skip:
          m.skip(self._live_modules_check)
      self._live_modules_check = 0
    self._live_modules_check += 1
    return self._live_modules

  def next(self, t):
    return sum([ m.next(t) for m in self.live_modules() ])

  def liveness(self):
    m = self.modules()
    if len(m['live']) != 0: return 'live'
    if len(m['sleep']) != 0: return 'sleep'
    return 'dead'

class Oscillator:

  def __init__(self, context, waveform, freq, amp=1, noise=0, base=0):
    self._waveform = waveform
    self._context = context
    self._freq = freq
    self._amp = amp
    self._phase = 0
    self._noise = noise
    self._base = base

  def next(self, t):
    self._phase = ( self._phase + ( t * self._freq / self._context.sample_rate() ) ) % 1.
    x = self._waveform( self._phase )
    if self._noise != 0:
      x = x + random.gauss(0, self._noise)
    return x * self._amp + self._base

  def liveness(self):
    return 'live'

class FreqMod:

  def __init__(self, context, carrier, modulator):
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
    m = self._modulator.liveness()
    c = self._carrier.liveness()
    if m == 'dead' or c == 'dead': return 'dead'
    if m == 'sleep' or c == 'sleep': return 'sleep'
    return 'live'

class ADSR:

  def __init__(self, context, a, d, s, r):
    self._duration = list([ d * context.sample_rate() for d in [a,d,s,r] ])
    self._t = 0.
    self._sustain_amp = .6
    self._section = 'a'

  def next(self, t):
    d = self._duration
    T = self._t + t
    sa = self._sustain_amp
    s = self._section
    if s == 'a':
      x = T / d[0]
      if T > d[0]:
        s = 'd'
        T = 0
    elif s == 'd':
      x = sa + T * (1-sa) / d[1]
      if T > d[1]:
        s = 's'
        T = 0
    elif s == 's':
      x = sa
      if T > d[2]:
        s = 'r'
        T = 0
    elif s == 'r':
      x = sa - T * sa / d[3]
      if T > d[3]:
        s = None
        T = 0
    else:
      x = 0
    self._section = s
    self._t = T
    return x

  def liveness(self):
    return 'live' if self._section is not None else 'dead'

