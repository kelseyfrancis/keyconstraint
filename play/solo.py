from copy import copy
import sys

import play, music, synth

def _main():
  key = music.key(sys.argv[1])
  midi_name = sys.argv[2]
  c = synth.Context()
  t = None
  if midi_name:
    a = c.add(keep_alive = True)
    c.start(a)
    def on_note(note):
      shifted = note.key_shift('C', key)
      print('%s -> %s' % (note, shifted))
      if shifted:
        a.add_module(copy(play.beep(c, shifted.shift_octave(-1))))
    t = play.MidiListener(on_note, name=midi_name)
    t.start()
  else:
    c.start(scale(c, key))
  m = None
  sys.stdin.readline()
  if t: t.stop()
  c.stop()

if __name__ == '__main__':
  _main()
