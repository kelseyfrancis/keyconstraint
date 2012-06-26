_note = 'C        -        D        -        E        F        -        G        -        A        -        B'.split()

_frequency = [
 [   16.35,   17.32,   18.35,   19.45,   20.60,   21.83,   23.12,   24.50,   25.96,   27.50,   29.14,   30.87 ],
 [   32.70,   34.65,   36.71,   38.89,   41.20,   43.65,   46.25,   49.00,   51.91,   55.00,   58.27,   61.74 ],
 [   65.41,   69.30,   73.42,   77.78,   82.41,   87.31,   92.50,   98.00,  103.83,  110.00,  116.54,  123.47 ],
 [  130.81,  138.59,  146.83,  155.56,  164.81,  174.61,  185.00,  196.00,  207.65,  220.00,  233.08,  246.94 ],
 [  261.63,  277.18,  293.66,  311.13,  329.63,  349.23,  369.99,  392.00,  415.30,  440.00,  466.16,  493.88 ],
 [  523.25,  554.37,  587.33,  622.25,  659.26,  698.46,  739.99,  783.99,  830.61,  880.00,  932.33,  987.77 ],
 [ 1046.50, 1108.73, 1174.66, 1244.51, 1318.51, 1396.91, 1479.98, 1567.98, 1661.22, 1760.00, 1864.66, 1975.53 ],
 [ 2093.00, 2217.46, 2349.32, 2489.02, 2637.02, 2793.83, 2959.96, 3135.96, 3322.44, 3520.00, 3729.31, 3951.07 ],
]

_note_map = dict(
  filter(
    lambda x : x, 
    map(
      lambda (i, s): (None if s == '-' else (s, i)), 
      enumerate(_note)
    )
  )
)

_modifier = { '#': 1, 'b': -1 }

def _note_index(note):
  base = _note_map[note[0].upper()]
  modifier = _modifier[note[1]] if len(note) > 1 else 0 
  return (base + modifier) % len(_note);

_scale = {
  'major': [
    'C  D  E  F  G  A  B ',
    'Db Eb F  Gb Ab Bb C ',
    'D  E  F# G  A  B  C#',
    'Eb F  G  Ab Bb C  D ',
    'E  F# G# A  B  C# D#',
    'F  G  A  Bb C  D  E ',
    'Gb Ab Bb Cb Db Eb F ',
    'G  A  B  C  D  E  F#',
    'Ab Bb C  Db Eb F  G ',
    'A  B  C# D  E  F# G#',
    'Bb C  D  Eb F  G  A ',
    'B  C# D# E  F# G# A#',
  ],
  'minor': [
    'C  D  Eb F  G  Ab Bb',
    'C# D# E  F# G# A  B ',
    'D  E  F  G  A  Bb C ',
    'Eb F  Gb Ab Bb Cb Db',
    'E  F# G  A  B  C  D ',
    'F  G  Ab Bb C  Db Eb',
    'F# G# A  B  C# D  E ',
    'G  A  Bb C  D  Eb F ',
    'G# A# B  C# D# E  F#',
    'A  B  C  D  E  F  G ',
    'Bb C  Db Eb F  Gb Ab',
    'B  C# D  E  F# G  A ',
  ]
}
_scale = dict(
  list(
    map(
      lambda (name, scales) : (
        name,
        map(
          lambda s: { 
            'name': s.split(), 
            'index': map(
              lambda n: _note_index(n),
              s.split()
            )
          }, 
          scales
        )
      ),
      _scale.iteritems()
    )
  )
)

#
# Gives a frequency corresponding to a particular note.
#
# Parameters:
#   note   - Examples: "C" (C), "C#" (C sharp), "Eb" (E flat).
#   octave - int with 0 being the lowest frequency range (16-30 Hz)
#            and 7 the highest (2KHz - 4KHz).
#
# Returns:
#   The frequency of the note.
#
def frequency(note, octave):
  return _frequency[octave][_note_index(note)]

def _get_scale(key):
  type = 'major' if key.istitle() else 'minor'
  return _scale [type][_note_index(key)]

#
# Shifts a note from from_key to to_key.
#
# Parameters:
#   note     - A note in the from_key scale.
#   from_key - The original note belongs to this key.
#   to_key   - The returned note belongs to this key.
#
# Returns:
#   The name of the "equivalent" note in the new key.
#
# Example:
#   shift_key('A', 'C', 'g#') shifts an A note from C major
#   to G sharp minor, and the resulting note is 'E'.
#
def shift_key(note, from_key, to_key):
  i    = _get_scale(from_key)['index'].index(_note_index(note))
  return _get_scale(to_key)['name'][i]

