var Inline = Quill.import('blots/inline');

function SearchedStringBlot() {
  Inline.apply(this, arguments);
}

SearchedStringBlot.prototype = Object.create(Inline.prototype);
SearchedStringBlot.prototype.constructor = SearchedStringBlot;

SearchedStringBlot.blotName = 'SearchedString';
SearchedStringBlot.className = 'ql-searched-string';
SearchedStringBlot.tagName = 'div';

Quill.register(SearchedStringBlot);

function Searcher(quill) {
  this.quill = quill;
  this.container = document.getElementById("search-container");

  var self = this;

  document.getElementById("search").addEventListener("click", function () {
    self.search();
  });

  document.getElementById("search-input").addEventListener("keyup", function (e) {
    self.keyPressedHandler(e);
  });

  document.getElementById("replace").addEventListener("click", function () {
    self.replace();
  });

  document.getElementById("replace-all").addEventListener("click", function () {
    self.replaceAll();
  });
}

// Statik değişkenler
Searcher.occurrencesIndices = [];
Searcher.currentIndex = 0;
Searcher.SearchedStringLength = 0;
Searcher.SearchedString = "";

Searcher.prototype.search = function () {
  Searcher.removeStyle(this.quill);
  Searcher.SearchedString = document.getElementById("search-input").value;
  if (Searcher.SearchedString) {
    var totalText = this.quill.getText();
    var re = new RegExp(Searcher.SearchedString, "gi");
    var match = re.test(totalText);
    if (match) {
      var indices = Searcher.occurrencesIndices = totalText.getIndicesOf(Searcher.SearchedString);
      var length = Searcher.SearchedStringLength = Searcher.SearchedString.length;

      for (var i = 0; i < indices.length; i++) {
        this.quill.formatText(indices[i], length, "SearchedString", true);
      }
    } else {
      Searcher.occurrencesIndices = null;
      Searcher.currentIndex = 0;
    }
  } else {
    Searcher.removeStyle(this.quill);
  }
};

Searcher.prototype.replace = function () {
  if (!Searcher.SearchedString) return;

  if (!Searcher.occurrencesIndices) this.search();
  if (!Searcher.occurrencesIndices) return;

  var indices = Searcher.occurrencesIndices;

  var oldString = document.getElementById("search-input").value;
  var newString = document.getElementById("replace-input").value;

  this.quill.deleteText(indices[Searcher.currentIndex], oldString.length);
  this.quill.insertText(indices[Searcher.currentIndex], newString);
  this.quill.formatText(indices[Searcher.currentIndex], newString.length, "SearchedString", false);

  this.search();
};

Searcher.prototype.replaceAll = function () {
  if (!Searcher.SearchedString) return;
  var oldStringLen = document.getElementById("search-input").value.length;
  var newString = document.getElementById("replace-input").value;

  if (!Searcher.occurrencesIndices) this.search();
  if (!Searcher.occurrencesIndices) return;

  while (Searcher.occurrencesIndices && Searcher.occurrencesIndices.length > 0) {
    this.quill.deleteText(Searcher.occurrencesIndices[0], oldStringLen);
    this.quill.insertText(Searcher.occurrencesIndices[0], newString);
    this.search();
  }
  Searcher.removeStyle(this.quill);
};

Searcher.prototype.keyPressedHandler = function (e) {
  if (e.key === "Enter" || e.keyCode === 13) {
    this.search();
  }
};

Searcher.removeStyle = function (quill) {
  if (!quill) return;
  quill.formatText(0, quill.getText().length, 'SearchedString', false);
};

// String prototype helper
if (!String.prototype.getIndicesOf) {
  String.prototype.getIndicesOf = function (searchStr) {
    var searchStrLen = searchStr.length;
    var startIndex = 0, index, indices = [];
    var lowerStr = this.toLowerCase();
    var lowerSearchStr = searchStr.toLowerCase();

    while ((index = lowerStr.indexOf(lowerSearchStr, startIndex)) > -1) {
      indices.push(index);
      startIndex = index + searchStrLen;
    }
    return indices;
  };
}
