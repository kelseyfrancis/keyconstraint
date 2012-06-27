import itertools
import sys

import music
import synth

if __name__ == '__main__':
  c = synth.Context()
  c.daemon = True
  notes = list(itertools.islice(music.notes('g#', 4, start='B', step=-1), 0, 8))
  duration = .5
  for i, n in enumerate(notes):
    c.add_module(synth.Interval(c, synth.Sine(c, note=n), i * duration, duration))
  c.start()
  sys.stdin.readline()
  c.stop()

