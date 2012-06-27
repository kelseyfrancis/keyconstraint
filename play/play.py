import itertools
import sys

import music
import synth

def beep(c, n, i):
  x = c.adsr(
    c.fm(
      carrier = c.triangle(freq = n.frequency()),
      modulator = c.sine(freq = 6, amp=.05)
    ),
    [.05, .05, .2, .1]
  )
  return c.interval(x, i * .5)

if __name__ == '__main__':
  c = synth.Context()
  c.daemon = True
  notes = list(itertools.islice(music.notes('A', 4), 0, 8))
  for i, n in enumerate(notes):
    c.add_module(beep(c, n, i))
  c.start()
  sys.stdin.readline()
  c.stop()

