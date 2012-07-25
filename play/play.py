from copy import copy
import functools
import itertools
from pygame import midi
import signal
from subprocess import Popen, PIPE
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
  x = c.add([
    c.sine(freq = n.frequency(), noise = .0002, amp = 0.5),
    c.square(freq = 5 * n.frequency(), noise = .0002, amp = 0.3),
    c.triangle(freq = 3 * n.frequency(), noise = .0002, amp = 0.1),
  ])
  x = c.am(carrier = x, modulator = c.sine(freq = 2, amp = 0.15, base = 1.))
  x = c.am(x, c.adsr(.01, .05, .2, 1.4))
  return c.table(module = x)

def scale_beeps(c, key):
  notes = list(itertools.islice(music.notes(key, 4, step=1), 0, 8))
  return list([ beep(c, n) for n in notes ])

def scale(c, key):
  x = c.add(list([ c.interval(beep, i * .2) \
    for i, beep in enumerate(scale_beeps(c, key)) ]))
  x.add_module(c.interval(delay_seconds = 7))
  return x

class MidiListener(Thread):

  def __init__(self, on_note, name):
    super(MidiListener, self).__init__()
    self._halt = False
    self._on_note = on_note
    self._name = name

  def run(self):
    midi.init()
    i = [ i for i in range(midi.get_count()) if self._name in midi.get_device_info(i)[1] ][0]
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

def _main():

  key = music.key(sys.argv[1])
  midi_name = sys.argv[2] if len(sys.argv) > 2 else None
  file_name = sys.argv[3] if len(sys.argv) > 3 else None
  c = synth.Context()
  t = None
  if midi_name:
    a = c.add(keep_alive = True)
    c.start(a)
    def on_note(note):
      shifted = note.key_shift('C', key)
      print('%s -> %s' % (note, shifted))
      if shifted:
        a.add_module(copy(beep(c, shifted.shift_octave(-1))))
    t = MidiListener(on_note, name=midi_name)
    t.start()
  else:
    c.start(scale(c, key))
  m = None
  if file_name:
    command = 'aplay %s' % (file_name,)
    print(command)
    m = Popen(command.split(), stdin=PIPE)
  sys.stdin.readline()
  if t: t.stop()
  c.stop()
  if m: m.kill()

if __name__ == '__main__':
  _main()

