(function($) {
  $.fn.keypad = (function() {
    var methods = {
      '': function(newMappings) {
        return this.each(function() {
          var self = $(this);
          var map = self.keypad('map');
          $.each(newMappings, function(key, fn) {
            var fns = map[key] = (map[key] || []);
            fns.push.apply(fns, [].concat(fn));
          });
        });
      },
      init: function() {
        return this.each(function() {
          var self = $(this);
          var data = self.data('keypad');
          if (!data) {
            var fn = function(isDown) {
              return function(e) {
                var map = self.keypad('map');
                var key = e.which;
                $.each(map[key] || [], function(i, fn) {
                  var wasDown = self.keypad('state', key);
                  if (wasDown != isDown) {
                    self.keypad('state', key, isDown);
                    setTimeout(function() { 
                      fn(isDown); 
                    }, 0);
                  }
                });
              };
            };
            data = { 
              map: {}, 
              state: {},
              keydown: fn(true), 
              keyup: fn(false)
            };
            self.data('keypad', data);
            self.keypad('bind');
          }
        });
      },
      data: function() {
        var self = $(this[0]);
        self.keypad('init');
        return self.data('keypad');
      },
      map: function() {
        return this.keypad('data').map;
      },
      state: function(key, value) {
        var state = this.keypad('data').state;
        if (key === undefined) { return state; }
        if (value === undefined) { return !!state[key]; }
        if (value) { state[key] = true; } else { delete state[key]; }
        return this;
      },
      bind: function() {
        return this.each(function() {
          var self = $(this);
          var data = self.keypad('data');
          self.bind('keydown', data.keydown);
          self.bind('keyup', data.keyup);
        });
      },
      unbind: function() {
        return this.each(function() {
          var self = $(this);
          var data = self.keypad('data');
          self.unbind('keydown', data.keydown);
          self.unbind('keyup', data.keyup);
        });
      }
    };
    return function(method) {
      if (typeof method !== 'string') { return methods[''].apply(this, arguments); }
      if (!methods[method]) { throw 'no such method: jQuery.keypad.' + method; }
      return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
    };
  })();
})(jQuery);
