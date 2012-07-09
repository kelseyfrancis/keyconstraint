import itertools
import math
from math import pi, floor
import numpy as np
import random
import struct
from subprocess import Popen, PIPE
import sys
from threading import Thread
from time import sleep, time

def sine(x): return math.sin(x * 2. * pi)
def triangle(x): return (4. * x - 1) if x < .5 else (-2. * x + 1)
def square(x): return -1 if x < .5 else 1

class Context:

  def __init__(self):
    self._sample_rate = 44100
    self._buffer_size = int(self._sample_rate * .1)
    self._player = Player(self)

  def open(self):
    command = 'aplay -f cd -c1 --buffer-size %d -r %d' \
      % (self.buffer_size(), self.sample_rate())
    print(command)
    return Popen(command.split(), stdin=PIPE)

  def start(self, module): 
    self._player._module = module
    self._player.start()
  
  def stop(self): 
    self._player.stop()

  def sample_rate(self): 
    return self._sample_rate

  def buffer_time(self):
    return 1. * self.buffer_size() / self._sample_rate

  def buffer_size(self):
    return self._buffer_size

  def add(*args, **kwargs): return Addition(*args, **kwargs)
  def osc(*args, **kwargs): return Oscillator(*args, **kwargs)
  def sine(*args, **kwargs): return Oscillator(*args, waveform=sine, **kwargs)
  def triangle(*args, **kwargs): return Oscillator(*args, waveform=triangle, **kwargs)
  def square(*args, **kwargs): return Oscillator(*args, waveform=square, **kwargs)
  def fm(*args, **kwargs): return FreqMod(*args, **kwargs)
  def interval(*args, **kwargs): return Interval(*args, **kwargs)
  def am(*args, **kwargs): return AmpMod(*args, **kwargs)
  def adsr(*args, **kwargs): return ADSR(*args, **kwargs)
  def irfilter(*args, **kwargs): return LinearFilter(*args, **kwargs)
  def table(*args, **kwargs): return WaveTable(*args, **kwargs)

class Player(Thread):

  def __init__(self, context):
    super(Player, self).__init__()
    self._context = context
    self._halt = False

  def run(self):
    c = self._context
    audio = c.open()
    out = audio.stdin
    intensity_shift = 2**15
    h = 0
    m = self._module
    buffer_size = self._context.buffer_size()
    pack_format = '<' + 'H' * buffer_size
    buffer_time = self._context.buffer_time()
    extra_buffer = 1.
    buffer_ahead = extra_buffer * buffer_time
    sleep_time = buffer_ahead / 8.
    t = time() - buffer_ahead
    while not self._halt:
      h = (h + 1) if m.liveness() == 'dead' else 0
      if h > 5: break
      if t < time():
        t += buffer_time
        samples = m.next(t = 1., n = buffer_size)
        samples = [ s * 100 + intensity_shift for s in samples ]
        out.write(struct.pack(pack_format, *samples))
        out.flush()
      else:
        sleep(sleep_time)
    audio.kill()

  def stop(self):
    self._halt = True

class WaveTable:

  def __init__(self, context, module = None, samples = None, oversample = 1.):
    self._context = context
    self._i = 0.
    self._table = samples
    self._oversample = oversample
    if samples is None:
      self._table = np.array([])
      t = 1. / oversample
      while module.liveness() == 'live':
        self._table = np.hstack((self._table, module.next(t, 1000)))

  def _next_index(self, t):
    if self._i is None:
      return None
    i = floor(self._i)
    self._i += t * self._oversample
    if self._i >= len(self._table):
      self._i = None
    return i

  def next(self, t, n):
    if self._i is None:
      return np.zeros(n)
    indices = np.array([ self._next_index(t) for i in range(n) ])
    indices = [ i for i in indices if i is not None ]
    x = np.hstack((self._table[indices], np.zeros(n - len(indices))))
    assert len(x.shape) == 1
    return x

  def liveness(self):
    return 'dead' if self._i is None else 'live'

  def __copy__(self):
    return WaveTable(self._context, samples = self._table, oversample = self._oversample)

class LinearFilter:

  def __init__(self, context, coefficients, module):
    self._coefficients = np.array(coefficients)
    self._samples = np.zeros(self._coefficients.shape)
    self._module = module

  def _next(self, t):
    x = np.dot(self._coefficients.ravel(), self._samples.ravel())
    np.roll(self._samples, 1)
    self._samples[0][0] = self._module.next(t)
    self._samples[1][0] = x
    return x

  def next(self, t, n):
    x = np.array([ self._next(t) for i in range(n) ])
    assert len(x.shape) == 1
    return x

  def liveness(self):
    return self._module.liveness()

class Addition:

  def __init__(self, context, modules=None, keep_alive=False):
    self._modules = modules if modules else []
    self._keep_alive = keep_alive

  def add_module(self, module):
    self._modules.append(module)

  def modules(self):
    self._modules = filter(lambda m : m.liveness() != 'dead', self._modules)
    return dict(list([ (liveness, list(filter(lambda m : m.liveness() == liveness, self._modules))) for liveness in ['live', 'sleep'] ]))

  def live_modules(self):
    return self.modules()['live']

  def next(self, t, n):
    live = self.live_modules()
    if len(live) == 0:
      return np.zeros(n)
    x = sum([ m.next(t, n) for m in live ])
    assert len(x.shape) == 1
    return x

  def liveness(self):
    if self._keep_alive:
      return 'live'
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

  def _next(self, t):
    self._phase += 1. * t * self._freq / self._context.sample_rate()
    self._phase %= 1.
    x = self._waveform( self._phase )
    if self._noise != 0:
      x = x + random.gauss(0, self._noise)
    return x

  def next(self, t, n):
    x = np.array([ self._next(t) for i in range(n) ])
    x *= self._amp
    x += self._base
    assert len(x.shape) == 1
    return x

  def liveness(self):
    return 'live'

class FreqMod:

  def __init__(self, context, carrier, modulator):
    self._carrier = carrier
    self._modulator = modulator

  def next(self, t, n):
    m = self._modulator.next(t, n)
    x = np.array([ self._carrier.next(t + t * m[i] / 2, 1)[0] for i in range(n) ])
    # todo modify next() implementations so t can be an array
    assert len(x.shape) == 1
    return x

  def liveness(self):
    c = self._carrier.liveness()
    m = self._modulator.liveness()
    if c == 'dead' or m == 'dead': return 'dead'
    if c == 'sleep' or m == 'sleep': return 'sleep'
    return 'live'

class Interval:

  def __init__(self, context, module=None, delay_seconds=0, duration_seconds=None):
    self._module = module
    self._delay_remaining = round(context.sample_rate() * delay_seconds)
    self._play_remaining = None if duration_seconds is None else round(context.sample_rate() * duration_seconds)

  def _next(self, t):
    if self._delay_remaining != 0:
      self._delay_remaining = self._delay_remaining - 1
      return 0
    if self._play_remaining is None:
      return self._module.next(t) if self._module else 0
    if self._play_remaining != 0:
      self._play_remaining = self._play_remaining - 1
      return self._module.next(t) if self._module else 0
    return 0

  def next(self, t, n):
    x = np.array([ self._next(t) for i in range(n) ])
    assert len(x.shape) == 1
    return x

  def skip(self, t):
    self._delay_remaining = max(0, self._delay_remaining - t)

  def liveness(self):
    if self._delay_remaining != 0: return 'sleep'
    if self._play_remaining is None or self._play_remaining != 0:
      return self._module.liveness() if self._module else 'dead'
    return 'dead'

class AmpMod:

  def __init__(self, context, carrier, modulator):
    self._carrier = carrier
    self._modulator = modulator

  def next(self, t, n):
    if self.liveness() != 'live':
      x = np.zeros(n)
    else:
      x = self._carrier.next(t, n) * self._modulator.next(1, n)
    assert len(x.shape) == 1
    return x

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

  def _next(self, t):
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

  def next(self, t, n):
    x = np.array([ self._next(t) for i in range(n) ])
    assert len(x.shape) == 1
    return x

  def liveness(self):
    return 'live' if self._section is not None else 'dead'

