import itertools
import sys
from time import sleep

import music
import synth

def beep(c, n, i):
  x = c.triangle(freq = n.frequency(), noise = .02)
  x = c.am(carrier = x, modulator = c.sine(freq = 2, amp=0.3, base=1))
  x = c.fm(carrier = x, modulator = c.sine(freq = 6, amp=.05, noise=.01))
  x = c.am(x, c.adsr(.05, .05, .2, .48))
  x = c.interval(x, i * .5)
  return x

def beeps(c, notes):
  x = c.add(list([ beep(c, n, i) for i, n in enumerate(notes) ]))
  x = c.irfilter(module = x, coefficients = [[.5,.5,.5,.5,.5],[0,0,0,0,0]])
  return x

if __name__ == '__main__':
  c = synth.Context()
  notes = list(itertools.islice(music.notes(sys.argv[1], 4, step=1), 0, 8))
  c.start(beeps(c, notes))

