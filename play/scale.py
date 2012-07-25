import play, music, synth, sys

def _main():
  key = music.key(sys.argv[1])
  c = synth.Context()
  c.start(play.scale(c, key))
  sys.stdin.readline()
  c.stop()

if __name__ == '__main__':
  _main()

