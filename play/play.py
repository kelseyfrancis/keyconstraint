import itertools
import sys

import music
import synth

def beep(c, n, i):
  x = c.triangle(freq = n.frequency(), noise = .02)
  x = c.fm(carrier = x, modulator = c.sine(freq = 6, amp=.05))
  x = c.amp_env(x, c.adsr(.05, .05, .2, .1))
  x = c.interval(x, i * .5, .4)
  return x;

if __name__ == '__main__':
  c = synth.Context()
  c.daemon = True
  notes = list(itertools.islice(music.notes(sys.argv[1], 4, step=1), 0, 8))
  for i, n in enumerate(notes):
    c.add_module(beep(c, n, i))
  c.start()
  sys.stdin.readline()
  c.stop()

