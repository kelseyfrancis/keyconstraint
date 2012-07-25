from copy import copy
from subprocess import Popen, PIPE
import sys
from threading import Thread

import play, music, synth

def _main():
  c = synth.Context()
  PrecomputeWaveTables(c).start()
  midi_name = sys.argv[1]
  file_name = sys.argv[2]
  command = './identifykey.sh -f %s' % file_name
  print(command)
  p = Popen(command, shell=True, stdout=PIPE)
  key = p.stdout.readline().strip()
  print('Detected key: [%s]' % key)
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
  if file_name:
    command = 'aplay %s' % (file_name,)
    print(command)
    m = Popen(command.split(), stdin=PIPE)
  sys.stdin.readline()
  if t: t.stop()
  c.stop()
  if m: m.kill()

class PrecomputeWaveTables(Thread):
  
  def __init__(self, c):
    super(PrecomputeWaveTables, self).__init__()
    self._c = c
    self.daemon = True

  def run(self):
    c = self._c
    for i in range(67, 75):
      n = music.midi_note(i-12)
      play.beep(c, n)

if __name__ == '__main__':
  _main()
