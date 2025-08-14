(function(global) {

  function BaseModule(resizer) {
    this.overlay = resizer.overlay;
    this.img = resizer.img;
    this.options = resizer.options;
    this.requestUpdate = resizer.onUpdate.bind(resizer);
  }
  BaseModule.prototype.onCreate = function() {};
  BaseModule.prototype.onDestroy = function() {};
  BaseModule.prototype.onUpdate = function() {};

  function DisplaySize(resizer) {
    BaseModule.call(this, resizer);
  }
  DisplaySize.prototype = Object.create(BaseModule.prototype);
  DisplaySize.prototype.constructor = DisplaySize;
  DisplaySize.prototype.onCreate = function() {
    this.display = document.createElement('div');
    this.display.classList.add('image-resize-display');
    var styles = this.options.displayStyles;
    for (var k in styles) {
      this.display.style[k] = styles[k];
    }
    this.overlay.appendChild(this.display);
  };
  DisplaySize.prototype.onDestroy = function() {
    if (this.display && this.display.parentNode) {
      this.display.parentNode.removeChild(this.display);
    }
  };
  /*
  DisplaySize.prototype.onUpdate = function() {
    if (!this.display || !this.img) return;
    var width = this.img.width;
    var height = this.img.height;
    this.display.textContent = width + ' × ' + height;
    this.display.style.right = '4px';
    this.display.style.bottom = '4px';
    this.display.style.left = 'auto';
  };
  */
  
  DisplaySize.prototype.onUpdate = function() {
	  if (!this.display || !this.img) return;

	  var width = this.img.width;
	  var height = this.img.height;

	  function pxToCm(px) {
		return +(px * 0.026458333).toFixed(2);
	  }

	  var widthCm = pxToCm(width);
	  var heightCm = pxToCm(height);

	  this.display.textContent = widthCm + ' × ' + heightCm + ' cm';

	  this.display.style.right = '4px';
	  this.display.style.bottom = '4px';
	  this.display.style.left = 'auto';
	};


  function Resize(resizer) {
    BaseModule.call(this, resizer);
    this.boxes = [];
    this.dragging = false;
    this.keepRatio = this.options.keepRatio !== false;
  }
  Resize.prototype = Object.create(BaseModule.prototype);
  Resize.prototype.constructor = Resize;

  Resize.prototype.onCreate = function() {
    var cursors = [
      'nwse-resize', 'ns-resize', 'nesw-resize', 'ew-resize',
      'nwse-resize', 'ns-resize', 'nesw-resize', 'ew-resize'
    ];
    for (var i=0; i < cursors.length; i++) {
      this.addBox(cursors[i]);
    }
    this.positionBoxes();

    var self = this;
    for (i=0; i < this.boxes.length; i++) {
      this.boxes[i].addEventListener('mousedown', function(evt){
        self.handleMouseDown(evt);
      });
      this.boxes[i].addEventListener('touchstart', function(evt){
        self.handleTouchStart(evt);
      }, {passive:false});
    }
  };

  Resize.prototype.addBox = function(cursor) {
    var box = document.createElement('div');
    var styles = this.options.handleStyles;
    for (var k in styles) {
      box.style[k] = styles[k];
    }
    box.style.cursor = cursor;
    box.style.width = this.options.handleStyles.width + 'px';
    box.style.height = this.options.handleStyles.height + 'px';
    this.overlay.appendChild(box);
    this.boxes.push(box);
  };

  Resize.prototype.positionBoxes = function() {
    var size = parseFloat(this.options.handleStyles.width);
    var halfSize = size / 2;
    var positions = [
      {left:-halfSize+'px', top:-halfSize+'px'},             // top-left
      {left:'50%', top:-halfSize+'px', transform:'translateX(-50%)'}, // top-center
      {right:-halfSize+'px', top:-halfSize+'px'},            // top-right
      {right:-halfSize+'px', top:'50%', transform:'translateY(-50%)'}, // middle-right
      {right:-halfSize+'px', bottom:-halfSize+'px'},         // bottom-right
      {left:'50%', bottom:-halfSize+'px', transform:'translateX(-50%)'}, // bottom-center
      {left:-halfSize+'px', bottom:-halfSize+'px'},          // bottom-left
      {left:-halfSize+'px', top:'50%', transform:'translateY(-50%)'},    // middle-left
    ];
    for (var i=0; i < this.boxes.length; i++) {
      var box = this.boxes[i];
      var pos = positions[i];
      for (var key in pos) {
        box.style[key] = pos[key];
      }
    }
  };

  Resize.prototype.handleMouseDown = function(evt) {
    evt.preventDefault();
    this.startDrag(evt.clientX, evt.clientY, evt.target);
    var self = this;
    function onMouseMove(e) { e.preventDefault(); self.doDrag(e.clientX, e.clientY); }
    function onMouseUp(e) {
      e.preventDefault();
      self.endDrag();
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    }
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  Resize.prototype.handleTouchStart = function(evt) {
    evt.preventDefault();
    if (evt.touches.length === 1) {
      var touch = evt.touches[0];
      this.startDrag(touch.clientX, touch.clientY, evt.target);
      var self = this;
      function onTouchMove(e) {
        e.preventDefault();
        if (e.touches.length === 1) {
          var t = e.touches[0];
          self.doDrag(t.clientX, t.clientY);
        }
      }
      function onTouchEnd(e) {
        e.preventDefault();
        self.endDrag();
        document.removeEventListener('touchmove', onTouchMove);
        document.removeEventListener('touchend', onTouchEnd);
        document.removeEventListener('touchcancel', onTouchEnd);
      }
      document.addEventListener('touchmove', onTouchMove, {passive:false});
      document.addEventListener('touchend', onTouchEnd);
      document.addEventListener('touchcancel', onTouchEnd);
    }
  };
  
	Resize.prototype.startDrag = function(startX, startY, target) {
	  this.dragging = true;
	  this.dragBox = target;
	  this.startX = startX;
	  this.startY = startY;
	  this.startWidth = this.img.width || this.img.naturalWidth;
	  this.startHeight = this.img.height || this.img.naturalHeight;
	  //this.ratio = this.img.naturalWidth / this.img.naturalHeight;
	  this.ratio = this.startWidth / this.startHeight;

	  // Köşe kutucuklarıysa orantı koru
	  var corners = [this.boxes[0], this.boxes[2], this.boxes[4], this.boxes[6]];
	  this.keepRatio = corners.indexOf(target) !== -1;
	  
	  
	};

	Resize.prototype.doDrag = function(currentX, currentY) {
	  if (!this.dragging) return;

	  var dx = currentX - this.startX;
	  var dy = currentY - this.startY;

	  var newWidth = this.startWidth;
	  var newHeight = this.startHeight;

	  var isLeft = this.dragBox === this.boxes[0] || this.dragBox === this.boxes[6] || this.dragBox === this.boxes[7];
	  var isRight = this.dragBox === this.boxes[2] || this.dragBox === this.boxes[3] || this.dragBox === this.boxes[4];
	  var isTop = this.dragBox === this.boxes[0] || this.dragBox === this.boxes[1] || this.dragBox === this.boxes[2];
	  var isBottom = this.dragBox === this.boxes[4] || this.dragBox === this.boxes[5] || this.dragBox === this.boxes[6];

	  if (isLeft) newWidth = Math.round(this.startWidth - dx);
	  if (isRight) newWidth = Math.round(this.startWidth + dx);
	  if (isTop) newHeight = Math.round(this.startHeight - dy);
	  if (isBottom) newHeight = Math.round(this.startHeight + dy);

	  if (this.keepRatio) {
		if (isLeft || isRight) {
		  newHeight = Math.round(newWidth / this.ratio);
		} else if (isTop || isBottom) {
		  newWidth = Math.round(newHeight * this.ratio);
		}
	  }

	  if (newWidth < 10) newWidth = 10;
	  if (newHeight < 10) newHeight = 10;

	  this.img.width = newWidth;
	  this.img.height = newHeight;
	  this.requestUpdate();
	};


  Resize.prototype.endDrag = function() {
    this.dragging = false;
    this.dragBox = null;
    this.setCursor('');
  };

  Resize.prototype.setCursor = function(value) {
    var els = [document.body, this.img];
    for (var i=0; i < els.length; i++) {
      if (els[i]) els[i].style.cursor = value;
    }
  };

  function Toolbar(resizer) {
    BaseModule.call(this, resizer);
  }
  Toolbar.prototype = Object.create(BaseModule.prototype);
  Toolbar.prototype.constructor = Toolbar;

  Toolbar.prototype.onCreate = function() {
    var self = this;
    this.toolbar = document.createElement('div');
    this.toolbar.classList.add('image-resize-toolbar');
    var styles = this.options.toolbarStyles;
    for (var k in styles) {
      this.toolbar.style[k] = styles[k];
    }
    this.overlay.appendChild(this.toolbar);

    this.alignments = [
      {
        icon: '⬅️',
        apply: function() {
          self.img.style.float = 'left';
          self.img.style.margin = '0 1em 1em 0';
          self.img.style.display = '';
        },
        isApplied: function() { return self.img.style.float === 'left'; }
      },
      {
        icon: '⬆️',
        apply: function() {
          self.img.style.display = 'block';
          self.img.style.margin = 'auto';
          self.img.style.float = '';
        },
        isApplied: function() { return self.img.style.display === 'block' && self.img.style.margin === 'auto'; }
      },
      {
        icon: '➡️',
        apply: function() {
          self.img.style.float = 'right';
          self.img.style.margin = '0 0 1em 1em';
          self.img.style.display = '';
        },
        isApplied: function() { return self.img.style.float === 'right'; }
      },
    ];

    this.buttons = [];
    this.alignments.forEach(function(alignment) {
      var btn = document.createElement('button');
      btn.textContent = alignment.icon;
      btn.style.margin = '0 2px';
      btn.style.padding = '2px 6px';
      btn.style.cursor = 'pointer';

      btn.addEventListener('click', function() {
        if (alignment.isApplied()) {
          self.img.style.float = '';
          self.img.style.margin = '';
          self.img.style.display = '';
        } else {
          // Clear all
          self.alignments.forEach(function(a) {
            a.apply = function() {}; // temporarily disable to avoid recursion
          });
          alignment.apply();
          self.requestUpdate();
        }
      });

      self.toolbar.appendChild(btn);
      self.buttons.push(btn);
    });
  };

  Toolbar.prototype.onDestroy = function() {
    for (var i=0; i < this.buttons.length; i++) {
      this.buttons[i].parentNode.removeChild(this.buttons[i]);
    }
    if (this.toolbar) this.toolbar.parentNode.removeChild(this.toolbar);
  };

  Toolbar.prototype.onUpdate = function() {};

  var DefaultOptions = {
    modules: ['DisplaySize', 'Resize'],
    overlayStyles: {
      position: 'absolute',
      boxSizing: 'border-box',
      border: '1px dashed #444'
    },
    handleStyles: {
      position: 'absolute',
      height: 12,
      width: 12,
      backgroundColor: 'white',
      border: '1px solid #777',
      boxSizing: 'border-box',
      opacity: 0.8,
      borderRadius: '2px'
    },
    displayStyles: {
      position: 'absolute',
      font: '12px/1.0 Arial, Helvetica, sans-serif',
      padding: '4px 8px',
      textAlign: 'center',
      backgroundColor: 'white',
      color: '#333',
      border: '1px solid #777',
      boxSizing: 'border-box',
      opacity: 0.8,
      cursor: 'default'
    },
    toolbarStyles: {
      position: 'absolute',
      top: '-28px',
      right: '0',
      left: '0',
      height: '24px',
      minWidth: '100px',
      font: '12px/1.0 Arial, Helvetica, sans-serif',
      textAlign: 'center',
      color: '#333',
      boxSizing: 'border-box',
      cursor: 'default'
    }
  };

  var knownModules = {
    DisplaySize: DisplaySize,
    Toolbar: Toolbar,
    Resize: Resize
  };

  function ImageResize(quill, options) {
    options = options || {};
    this.quill = quill;
    this.options = {};
    for (var k in DefaultOptions) {
      this.options[k] = DefaultOptions[k];
    }
    for (var k2 in options) {
      this.options[k2] = options[k2];
    }
    this.moduleClasses = this.options.modules;
    this.modules = [];
    this.img = null;
    this.overlay = null;

    if (document.execCommand) {
      document.execCommand('enableObjectResizing', false, 'false');
    }

    this.handleClick = this.handleClick.bind(this);
    this.handleScroll = this.handleScroll.bind(this);
    this.checkImage = this.checkImage.bind(this);

    this.quill.root.addEventListener('click', this.handleClick);
    this.quill.root.addEventListener('scroll', this.handleScroll);

    if (!this.quill.root.parentNode.style.position) {
      this.quill.root.parentNode.style.position = 'relative';
    }
  }

  ImageResize.prototype.handleClick = function(evt) {
    if (evt.target && evt.target.tagName === 'IMG') {
      if (this.img === evt.target) return;
      if (this.img) this.hide();

      this.show(evt.target);
      evt.preventDefault();
    } else if (this.img) {
      this.hide();
    }
  };

  ImageResize.prototype.handleScroll = function() {
    this.hide();
  };

  ImageResize.prototype.show = function(img) {
    this.img = img;
    this.showOverlay();
    this.initializeModules();
  };

  ImageResize.prototype.showOverlay = function() {
    if (this.overlay) this.hideOverlay();

    this.quill.setSelection(null);
    this.setUserSelect('none');

    document.addEventListener('keyup', this.checkImage, true);
    this.quill.root.addEventListener('input', this.checkImage, true);

    this.overlay = document.createElement('div');
    this.overlay.classList.add('image-resize-overlay');
    var styles = this.options.overlayStyles;
    for (var k in styles) {
      this.overlay.style[k] = styles[k];
    }
    this.quill.root.parentNode.appendChild(this.overlay);

    this.repositionElements();
  };

  ImageResize.prototype.hideOverlay = function() {
    if (!this.overlay) return;
    this.overlay.parentNode.removeChild(this.overlay);
    this.overlay = null;

    document.removeEventListener('keyup', this.checkImage, true);
    this.quill.root.removeEventListener('input', this.checkImage, true);
    this.setUserSelect('');
  };

  ImageResize.prototype.repositionElements = function() {
    if (!this.overlay || !this.img) return;
    var parent = this.quill.root.parentNode;
    var imgRect = this.img.getBoundingClientRect();
    var containerRect = parent.getBoundingClientRect();

    this.overlay.style.left = (imgRect.left - containerRect.left - 1 + parent.scrollLeft) + 'px';
    this.overlay.style.top = (imgRect.top - containerRect.top + parent.scrollTop) + 'px';
    this.overlay.style.width = imgRect.width + 'px';
    this.overlay.style.height = imgRect.height + 'px';

    for (var i=0; i < this.modules.length; i++) {
      this.modules[i].onUpdate();
    }
  };

  ImageResize.prototype.initializeModules = function() {
    this.removeModules();
    this.modules = [];
    for (var i=0; i < this.moduleClasses.length; i++) {
      var ModuleClass = this.moduleClasses[i];
      var moduleInstance = knownModules[ModuleClass] ? new knownModules[ModuleClass](this) : new ModuleClass(this);
      this.modules.push(moduleInstance);
    }
    for (i=0; i < this.modules.length; i++) {
      this.modules[i].onCreate();
    }
    this.onUpdate();
  };

  ImageResize.prototype.removeModules = function() {
    for (var i=0; i < this.modules.length; i++) {
      this.modules[i].onDestroy();
    }
    this.modules = [];
  };

  ImageResize.prototype.onUpdate = function() {
    this.repositionElements();
    for (var i=0; i < this.modules.length; i++) {
      this.modules[i].onUpdate();
    }
  };

  ImageResize.prototype.hide = function() {
    this.hideOverlay();
    this.removeModules();
    this.img = null;
  };

  ImageResize.prototype.setUserSelect = function(value) {
    var props = ['userSelect', 'mozUserSelect', 'webkitUserSelect', 'msUserSelect'];
    for (var i=0; i < props.length; i++) {
      this.quill.root.style[props[i]] = value;
      document.documentElement.style[props[i]] = value;
    }
  };

  ImageResize.prototype.checkImage = function(evt) {
    if (this.img) {
      if (evt.keyCode === 46 || evt.keyCode === 8) {
        (window.Quill || Quill).find(this.img).deleteAt(0);
      }
      this.hide();
    }
  };

  if (global.Quill) {
    global.Quill.register('modules/imageResize', ImageResize);
  }

  // expose to global for script tag usage
  global.ImageResize = ImageResize;

})(window);
