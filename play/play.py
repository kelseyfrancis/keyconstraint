from copy import copy
import functools
import itertools
from pygame import midi
import signal
import sys
from threading import Thread
from time import sleep

import music
import synth

def memoize(obj):
  cache = obj.cache = {}
  @functools.wraps(obj)
  def memoizer(*args, **kwargs):
    if args not in cache:
      cache[args] = obj(*args, **kwargs)
    return cache[args]
  return memoizer

@memoize
def beep(c, n):
  x = c.triangle(freq = n.frequency(), noise = .02)
  x = c.am(carrier = x, modulator = c.sine(freq = 2, amp = 0.3, base = 1))
  x = c.fm(carrier = x, modulator = c.sine(freq = 6, amp = .05, noise = .01))
  x = c.am(x, c.adsr(.01, .05, .2, .4))
  return c.table(module = x)

def scale_beeps(c, key):
  notes = list(itertools.islice(music.notes(key, 4, step=1), 0, 8))
  return list([ beep(c, n) for n in notes ])

def scale(c, key):
  x = c.add(list([ c.interval(beep, i * .5) for i, beep in enumerate(scale_beeps(c, key)) ]))
  x.add_module(c.interval(delay_seconds = 7))
  return c.irfilter(module = x, coefficients = [[.5,.5,.5,.5,.5],[0,0,0,0,0]])

class MidiListener(Thread):

  def __init__(self, on_note):
    super(MidiListener, self).__init__()
    self._halt = False
    self._on_note = on_note

  def run(self):
    midi.init()
    i = [ i for i in range(midi.get_count()) if 'KeyRig' in midi.get_device_info(i)[1] ][0]
    i = midi.Input(i)
    while not self._halt:
      e = i.read(1)
      if len(e):
        e = e[0][0]
        if e[2] and (36 <= e[1] <= 84):
          self._on_note(music.midi_note(e[1]))
      else:
        sleep(.001)
    i = None

  def stop(self):
    self._halt = True

if __name__ == '__main__':
  key = music.key(sys.argv[1])
  midi_name = sys.argv[2] if len(sys.argv) > 2 else None
  c = synth.Context()
  if midi_name:
    a = c.add(keep_alive = True)
    c.start(a)
    def on_note(note):
      a.add_module(copy(beep(c, note)))
    t = MidiListener(on_note)
    t.start()
  else:
    t = None
    c.start(scale(c, key))
  sys.stdin.readline()
  if t: t.stop()
  c.stop()

