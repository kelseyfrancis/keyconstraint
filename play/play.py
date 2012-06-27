import itertools
import sys

import music
import synth

def beep(c, n, i):
  x = c.fm(
    carrier = c.sine(note = n), 
    modulator = c.fm(
      carrier = c.sine(freq = 2000),
      modulator = c.sine(freq = 10)
    )
  )
  return c.interval(x, i * .5, .4)

if __name__ == '__main__':
  c = synth.Context()
  c.daemon = True
  notes = list(itertools.islice(music.notes('C', 4), 0, 8))
  for i, n in enumerate(notes):
    c.add_module(beep(c, n, i))
  c.start()
  sys.stdin.readline()
  c.stop()

