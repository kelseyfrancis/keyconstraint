import itertools

def note_category(x):
  return NoteCategory.get(x)

def note(x):
  return Note.get(x)

def scale(x):
  return Scale.get(x)

def key(x):
  return Key.get(x)

def frequency(x):
  return note(x).frequency()

class NoteCategory:

  _letters = list('C-D-EF-G-A-B')

  _indices = dict(filter(lambda x : x, map(
    lambda (i, s): (None if s == '-' else (s, i)), 
    enumerate(_letters))))

  _accidentals = { '#': 1, 'b': -1 }

  @staticmethod
  def _index(name):
    base = NoteCategory._indices[name[0].upper()]
    accidental = NoteCategory._accidentals[name[1]] if len(name) > 1 else 0 
    return (base + accidental) % len(NoteCategory._letters);

  @staticmethod
  def get(x):
    return x if isinstance(x, NoteCategory) else NoteCategory.parse(x)

  @staticmethod
  def parse(x):
    return NoteCategory(x)

  """
  Parameters:
    name - Examples:
             "C"  (C)
             "C#" (C sharp)
             "Eb" (E flat)
  """
  def __init__(self, name):
    name = name[0].upper() + name[1:]
    self._index = NoteCategory._index(name)
    self._name = name

  def __str__(self):
    return '<NoteCategory %s>' % self.name()

  def __hash__(self):
    return self.index()

  def __eq__(a, b):
    return a.index() == b.index()

  def __lt__(a, b):
    return a.index() < b.index()

  def index(self):
    return self._index

  def name(self):
    return self._name

class Note:

  @staticmethod
  def get(x):
    return x if isinstance(x, Note) else Note.parse(x)

  """
  Parameters:
    x - A string in the form "<note_category><octave>".
        Example: "A4" (440 Hz)
  """
  @staticmethod
  def parse(x):
    return Note( category = x[0:-1], octave = int(x[-1:]) )

  """
  Parameters:
    category - NoteCategory or str to construct a NoteCategory.
    octave   - Nonnegative integer. A0 = 27.5 Hz, A7 = 3.52 KHz
  """
  def __init__(self, category, octave):
    self._category = note_category(category)
    self._octave = octave

  def __str__(self):
    return '<Note %s>' % self.name()

  def __hash__(self):
    return self.index()

  def __eq__(a, b):
    return a.category() == b.category() and a.octave() == b.octave()

  def __lt__(a, b):
    return a.index() < b.index()

  def name(self):
    return '%s%d' % (self.category().name(), self.octave())

  def octave(self):
    return self._octave

  def category(self):
    return self._category

  def category_index(self):
    return self.category().index()

  def index(self):
    return 12 * self.octave() + self.category_index()

  def frequency(self):
    return 2 ** ( ( self.index() - note('A4').index() ) / 12. ) * 440

  """
  Shifts the note from key x to key y.
  
  Parameters:
    x - The original note belongs to this key.
    y - The returned note belongs to this key.
 
  Returns:
    An "equivalent" note in key y.
 
  Example:
    note('A4').key_shift('C', 'g#') shifts an A4 note from
    C major to G sharp minor, and the resulting note is E5.
  """
  def key_shift(self, x, y):
    i = key(x).categories().index(self.category())
    notes = key(y).notes(self.octave())
    return itertools.islice(notes, i, None).next()

class Scale:

  @staticmethod
  def get(x):
    return x if isinstance(x, Scale) else Scale.parse(x)

  @staticmethod
  def parse(x):
    return Scale(list([ note_category(n) for n in x.split() ]))

  def __init__(self, categories):
    self._categories = categories

  def __eq__(a, b):
    return a.categories() == b.categories()

  def __str__(self):
    return '<Scale %s>' % list([ str(c) for c in self.categories() ]).join(' ')

  def categories(self):
    return self._categories

  def notes(self, octave):
    i = -1
    o = octave
    for c in itertools.cycle(self.categories()):
      if c.index() < i:
        o = o + 1
      i = c.index()
      yield Note(c, o)

class Key:

  _scales = {
    True: [ # major
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
    False: [ # minor
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

  @staticmethod
  def get(x):
    return x if isinstance(x, Key) else Key.parse(x)

  @staticmethod
  def parse(x):
    return Key(major = x.istitle(), category = NoteCategory.parse(x))

  def __init__(self, major, category):
    self._major = major
    self._category = category

  def __str__(self):
    return '<Key %s>' % self.name()

  def __eq__(a, b):
    return a.major() == b.major() and a.category() == b.category()

  def name(self):
    x = self.category().name()
    return x.upper() if self.major() else x.lower()
  
  def major(self):
    return self._major

  def category(self):
    return self._category

  def category_index(self):
    return self.category().index()

  def scale(self, octave=None):
    return scale(Key._scales[self.major()][self.category_index()])

  def categories(self):
    return self.scale().categories()

  def notes(self, octave):
    return self.scale().notes(octave)

