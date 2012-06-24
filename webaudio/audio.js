function def(o) {
  o = $.extend({}, o);
  var constructor = o.constructor;
  delete o.constructor;
  var x = constructor 
    ? function() { constructor.apply(this, arguments); } 
    : function() {};
  $.extend(x.prototype, o);
  return x;
}

var twopi = 2 * Math.PI;

Sine = def({
  phase: 0,
  freq: 440,
  rate: 44100,
  constructor: function() {
    this.inc = twopi * this.freq / this.rate;
  },
  sample: function() {
    var p = this.phase;
    var s = Math.sin(p) * 0.5;
    p += this.inc;
    if (p > twopi) { p -= twopi; }
    this.phase = p;
    return s;
  }
});

Flat = def({
  sample: function() {
    return 0;
  }
});

$(function() {
  var sine = new Sine();
  var flat = new Flat();
  var c = new webkitAudioContext();
  n = c.createJavaScriptNode(Math.pow(2, 11), 0, 1);
  n.onaudioprocess = function(e) {
    var out = e.outputBuffer.getChannelData(0);
    var len = out.length;
    for (var i = 0; i < len; i++) {
      out[i] = sine.sample();
    }
  };
  $(document).keypad({
    74: function(x) {
      if (x) { 
        n.connect(c.destination); 
      } else {
        n.disconnect();
      }
      $('body').toggleClass('tone', x);
    }
  });
});
