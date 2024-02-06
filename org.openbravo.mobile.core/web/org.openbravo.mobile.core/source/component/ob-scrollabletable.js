/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, _, $, document */

enyo.kind({
  name: 'OB.UI.ScrollableTableHeader',
  style: 'border-bottom: 1px solid #cccccc;',
  handlers: {
    onScrollableTableHeaderChanged: 'scrollableTableHeaderChanged_handler'
  }
});

enyo.kind({
  name: 'OB.UI.ScrollableTable',
  published: {
    collection: null,
    listStyle: null,
    selectionMode: 'single'
  },
  statics: {
    initScroll: function (component) {
      var moving = false;
      var isTouchDevice = function () {
          return typeof window.ontouchstart !== 'undefined' || navigator.maxTouchPoints;
          };

      if (!isTouchDevice()) {
        component.handlers = component.handlers || {};
        component.handlers.onmousedown = 'scrollMouseDown';
        component.scrollMouseDown = function (inSender, inEvent) {
          moving = true;
        };
        enyo.dispatcher.listen(document, 'mouseup', function (ev) {
          moving = false;
        });
        enyo.dispatcher.listen(document, 'mousemove', function (ev) {
          if (moving) {
            component.hasNode().scrollTop = component.hasNode().scrollTop - ev.movementY;
          }
        });
      }
    }
  },
  events: {
    onSetMultiSelection: ''
  },
  initComponents: function () {
    this.inherited(arguments);
    OB.UI.ScrollableTable.initScroll(this.$.scrollArea);
  },
  autoSelectOnReset: true,
  getValue: function (arg, lineIndex) {
    //Automated test
    if (this.$.tbody.children[lineIndex].children[0].getValue) {
      return this.$.tbody.children[lineIndex].children[0].getValue(arg);
    } else {
      OB.warn('getValue is not present in line ' + lineIndex + ' component of ' + this.name);
      return null;
    }
  },
  getNumberOfLines: function () {
    //Automated test
    return this.$.tbody.children.length;
  },
  getLineByIndex: function (lineIndex) {
    //Automated test
    if (this.$.tbody.children[lineIndex].children[0]) {
      return this.$.tbody.children[lineIndex].children[0];
    } else {
      return undefined;
    }
  },
  getLineByIndexAndClick: function (lineIndex) {
    //Automated test
    var selectedLine = this.getLineByIndex(lineIndex);
    if (!_.isUndefined(selectedLine)) {
      selectedLine.tap();
    }
    return selectedLine;
  },
  getLineByValue: function (arg, value) {
    //Automated test
    var selectedLine;
    selectedLine = _.find(this.$.tbody.children, function (line) {
      if (line.children[0].getValue && line.children[0].getValue(arg) === value) {
        return true;
      }
    });
    if (!_.isUndefined(selectedLine)) {
      return selectedLine.children[0];
    } else {
      return undefined;
    }
  },
  getLineByValueAndClick: function (arg, value) {
    var selectedLine = this.getLineByValue(arg, value);
    if (!_.isUndefined(selectedLine)) {
      selectedLine.tap();
    }
    return selectedLine;
  },
  isSelectableLine: function (model) {
    return true;
  },
  components: [{
    name: 'theader'
  }, {
    components: [{
      kind: 'Scroller',
      name: 'scrollArea',
      thumb: true,
      horizontal: 'hidden',
      components: [{
        name: 'tbody',
        tag: 'ul',
        classes: 'unstyled',
        showing: false
      }]
    }]
  }, {
    name: 'tlimit',
    showing: false,
    style: 'border-bottom: 1px solid #cccccc; padding: 15px; text-align:center; font-weight: bold; color: #a1a328'
  }, {
    name: 'tinfo',
    showing: false,
    style: 'border-bottom: 1px solid #cccccc; padding: 15px; font-weight: bold; color: #cccccc'
  }, {
    name: 'tempty'
  }],
  create: function () {
    var tableName = this.name || '';

    this.inherited(arguments);

    // helping developers
    if (!this.renderLine) {
      throw enyo.format('Your list %s needs to define a renderLine kind', tableName);
    }

    if (!this.renderEmpty) {
      throw enyo.format('Your list %s needs to define a renderEmpty kind', tableName);
    }

    if (this.collection) {
      this.collectionChanged(null);
    }

    this.$.tlimit.setContent(OB.I18N.getLabel('OBMOBC_DataLimitReached'));
  },

  listStyleChanged: function (oldSTyle) {
    if (this.listStyle === 'checkboxlist') {
      this.collection.trigger('unSelectAll');
    }
  },

  collectionChanged: function (oldCollection) {
    this.selected = null;

    if (this.renderHeader && this.$.theader.getComponents().length === 0) {
      this.$.theader.createComponent({
        kind: this.renderHeader
      });
    }

    if (this.renderEmpty && this.$.tempty.getComponents().length === 0) {
      this.$.tempty.createComponent({
        kind: this.renderEmpty
      });
    }

    if (this.scrollAreaMaxHeight) {
      this.$.scrollArea.setMaxHeight(this.scrollAreaMaxHeight);
    }

    if (!this.collection) { // set to null?
      return;
    }

    /*selected*/
    this.func_selected = function (model) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('selected', this.func_selected);
        }
        return true;
      }
      if (!model && this.listStyle && this.listStyle !== 'checkboxlist') {
        if (this.selected) {
          this.selected.addRemoveClass('selected', false);
        }
        this.selected = null;
      }
    };
    this.collection.on('selected', this.func_selected, this);

    /*unSelectAll*/
    this.func_unSelectAll = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('unSelectAll', this.func_unSelectAll);
        }
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('unselected');
      });
    };
    this.collection.on('unSelectAll', this.func_unSelectAll, this);

    /*checkAll*/
    this.func_checkAll = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('checkAll', this.func_checkAll);
        }
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('check');
      });
    };
    this.collection.on('checkAll', this.func_checkAll, this);

    /*unCheckAll*/
    this.func_unCheckAll = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('unCheckAll', this.func_unCheckAll);
        }
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('uncheck');
      });
    };
    this.collection.on('unCheckAll', this.func_unCheckAll, this);

    /*showAllCheckBtn*/
    this.func_showAllCheckBtn = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('showAllCheckBtn', this.func_showAllCheckBtn);
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('showAllCheck');
      });
    };
    this.collection.on('showAllCheckBtn', this.func_showAllCheckBtn, this);

    /*hideAllCheckBtn*/
    this.func_hideAllCheckBtn = function (col) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        this.collection.off('hideAllCheckBtn', this.func_hideAllCheckBtn);
        return true;
      }
      this.collection.each(function (model) {
        model.trigger('hideAllCheck');
      });
    };
    this.collection.on('hideAllCheckBtn', this.func_hideAllCheckBtn, this);

    /*add*/
    this.func_add = function (model, prop, options) {
      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('add', this.func_add);
        }
        return true;
      }

      this.$.tempty.hide();
      this.$.tbody.show();

      this._addModelToCollection(model, options.index);

      if (this.listStyle === 'list') {
        if (!this.selected) {
          model.trigger('selected', model);
        }
      } else if (this.listStyle === 'edit') {
        model.trigger('selected', model);
      }
      this.setScrollAfterAdd();
    };
    this.collection.on('add', this.func_add, this);

    /*remove*/
    this.func_remove = function (model, prop, options) {
      var index = options.index,
          indexToPoint = index - 1;

      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('remove', this.func_remove);
        }
        return true;
      }
      if (this.$.tbody.getComponents()[index]) {
        this.$.tbody.getComponents()[index].destroy(); // controlAtIndex ?
      }

      if (this.collection) {
        if (index >= this.collection.length) {
          if (this.collection.length === 0) {
            this.collection.trigger('selected');
          } else {
            this.collection.at(this.collection.length - 1).trigger('selected', this.collection.at(this.collection.length - 1));
          }
        } else {
          this.collection.at(index).trigger('selected', this.collection.at(index));
        }

        if (this.destroyed) {
          if (this.collection) {
            this.collection.off('remove', this.func_remove);
          }
          return true;
        }
        if (this && this.collection && this.collection.length === 0) {
          this.$.tbody.hide();
          this.$.tempty.show();
        } else {
          //Put scroller in the previous item of deleted one
          //Issue 0021835 except when the deleted is the first one.
          if (indexToPoint < 0) {
            indexToPoint = 0;
          }
          if (this.$.tbody.getComponents()[indexToPoint]) {
            this.getScrollArea().scrollToControl(this.$.tbody.getComponents()[indexToPoint]);
          }
        }
      }
    };
    this.collection.on('remove', this.func_remove, this);

    /*reset*/
    this.func_reset = function (a, b, c) {
      var modelsel, dataLimit, showTlimit = false;

      //if the same collection is used by different components and one of them has been destroyed, the event is ignored
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('reset', this.func_reset);
        }
        return true;
      }

      this.$.tlimit.hide();
      this.$.tbody.hide();
      this.$.tempty.show();

      this.$.tbody.destroyComponents();
      if (this.collection) {
        if (this.collection.size() === 0) {
          this.$.tbody.hide();
          this.$.tempty.show();
          if (this.executeAfterClear) {
            this.executeAfterClear();
          }
          this.collection.trigger('selected');
        } else {
          dataLimit = this.collection.at(0).tempDataLimit ? this.collection.at(0).tempDataLimit : this.collection.at(0).dataLimit;
          this.$.tempty.hide();
          this.$.tbody.show();
          //slice the array to show the number of items defined by the limit
          if (dataLimit !== -1 && dataLimit < this.collection.length) {
            this.collection.reset(this.collection.models.slice(0, dataLimit), {
              silent: true
            });
            showTlimit = true;
          }
          //reset value to do not affect other queries
          this.collection.at(0).tempDataLimit = undefined;
          this.collection.each(function (model) {
            this._addModelToCollection(model);
          }, this);
          // added to fix bug 25287
          this.getScrollArea().render();
          this.$.tlimit.setShowing(showTlimit);

          if (this.executeAfterRender) {
            this.executeAfterRender();
          }

          if (this.listStyle === 'list') {
            modelsel = this.collection.at(0);
            modelsel.set('_obscrollabletableautoselected', true, {
              silent: true
            });
            modelsel.trigger('selected', modelsel);
            modelsel.unset('_obscrollabletableautoselected', {
              silent: true
            });
          } else if (this.listStyle === 'edit') {
            modelsel = this.collection.at(this.collection.size() - 1);
            if (this.autoSelectOnReset) {
              modelsel.set('_obscrollabletableautoselected', true, {
                silent: true
              });
              modelsel.trigger('selected', modelsel);
              modelsel.unset('_obscrollabletableautoselected', {
                silent: true
              });
            }
          }
        }
      }
    };

    this.collection.on('reset', this.func_reset, this);

    /*info*/
    this.func_info = function (info) {
      //if the same collection is used by different components and one of them has been destroyed.
      if (this.destroyed) {
        if (this.collection) {
          this.collection.off('info', this.func_info);
        }
        return true;
      }
      if (info) {
        this.$.tinfo.setContent(OB.I18N.getLabel(info));
        this.$.tinfo.show();
      } else {
        this.$.tinfo.hide();
      }
    };
    this.collection.on('info', this.func_info, this);

    // XXX: Reseting to show the collection if registered with data
    this.collection.trigger('reset');
  },
  getScrollArea: function () {
    return this.$.scrollArea;
  },
  setScrollAfterAdd: function () {
    //Put scroller in the position of new item
    this.getScrollArea().scrollToBottom();
  },
  getHeader: function () {
    var tableName = this.name || '';
    if (this.$.theader.getComponents()) {
      if (this.$.theader.getComponents().length > 0) {
        if (this.$.theader.getComponents().length === 1) {
          return this.$.theader.getComponents()[0];
        } else {
          //developers help
          throw enyo.format('Each scrolleable table ahould have only one component as header', tableName);
        }
      }
    }
    return null;
  },
  getHeaderValue: function () {
    var header = this.getHeader();
    if (header) {
      if (header.getValue) {
        return header.getValue();
      } else {
        return header.getContent();
      }
    }
    return '';
  },

  _addModelToCollection: function (model, index) {
    var i, models = [];

    var components = this.$.tbody.getComponents();
    if (!(_.isUndefined(index)) && !(_.isNull(index)) && index < components.length) {
      //refresh components collection, inserting new model...
      // get the models from current components
      for (i = 0; i < components.length; i++) {
        models[i] = {
          renderlinemodel: components[i].renderline.model,
          checked: components[i].checked
        };
      }
      this.$.tbody.destroyComponents();
      // rebuild component
      for (i = 0; i < models.length; i++) {
        if (i === index) {
          this._createComponentForModel(model);
        }
        this._createComponentForModel(models[i].renderlinemodel, models[i].checked);
      }
    } else {
      // add to the end...
      this._createComponentForModel(model);
    }
  },

  _createComponentForModel: function (model, checked) {
    var tr, skipLineRender = false;
    if (this.skipLineRender) {
      skipLineRender = this.skipLineRender(model);
    }
    if (skipLineRender !== true) {
      tr = this.$.tbody.createComponent({
        tag: 'li'
      }).render();
      //columns added for Automated test
      tr.renderline = tr.createComponent({
        kind: this.renderLine,
        model: model,
        columns: this.columns
      }).render();
      tr.checked = checked;
    }

    model.off('change', null, this);
    model.on('change', function () {
      model.trigger('updateView');
    }, this);

    model.off('updateView', null, this);
    model.on('updateView', function () {
      var skipLineRender2 = false;
      if (!tr || (tr && tr.destroyed)) {
        return;
      }
      tr.destroyComponents();
      if (this.skipLineRender) {
        skipLineRender2 = this.skipLineRender(model);
      }
      if (skipLineRender2 !== true) {
        tr.renderline = tr.createComponent({
          kind: this.renderLine,
          model: model
        }).render();
        tr.checked = checked;
      }
    }, this);

    model.off('selected', null, this);
    model.on('selected', function () {
      if (this.listStyle && this.listStyle === 'nonselectablelist') {
        //do nothing in this case, we don't want to select anything
        return;
      } else if (this.listStyle && this.listStyle !== 'checkboxlist') {
        var selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
        if (!tr || (tr && tr.destroyed)) {
          return;
        }
        if (this.scrollableTableGroup) {
          var elems = $('.' + this.scrollableTableGroup + '_activeScrollableTable');
          if (elems.length === 0) {
            $('#' + this.id).addClass(this.scrollableTableGroup + '_activeScrollableTable');
          } else if (elems && elems.length === 1 && elems[0].id !== this.id) {
            elems.removeClass(this.scrollableTableGroup + '_activeScrollableTable');
            $('#' + this.id).addClass(this.scrollableTableGroup + '_activeScrollableTable');
          }
        }
        if (this.selectionMode === 'single' && !(OB.MobileApp.model.ctrlPressed || OB.MobileApp.model.shiftPressed)) {
          var i, selectedRows = this.getSelectedRows();
          for (i = 0; i < selectedRows.length; i++) {
            selectedRows[i].addRemoveClass(selectedCssClass, false);
          }
          this.selected = tr;
          this.selected.addRemoveClass(selectedCssClass, true);
        } else {
          // Multiple selection
          this.selected = tr;
          if (OB.MobileApp.model.shiftPressed) {
            var selectIndex = this.getMultiSelectIndex(tr);
            if (selectIndex.current <= selectIndex.first) {
              this.doMultiSelection(selectIndex.current, selectIndex.last);
            } else {
              this.doMultiSelection(selectIndex.first, selectIndex.current);
            }
          } else {
            var selection, addRemove = this.selected.attributes['class'] !== selectedCssClass;
            if (!addRemove) {
              selection = this.getSelectedModels();
              if (selection.length <= 1) {
                addRemove = true;
              }
            }
            this.selected.addRemoveClass(selectedCssClass, addRemove);
            if (!addRemove) {
              selection = this.getSelectedModels();
              if (selection.length === 1) {
                selection[0].trigger('selected', selection[0]);
              }
            }
          }
        }
        this.doSetMultiSelection({
          models: this.getSelectedModels()
        });
        // FIXME: OB.UTIL.makeElemVisible(this.node, this.selected);
      } else if (this.listStyle === 'checkboxlist') {
        if (tr.destroyed) {
          return;
        }
        var components = tr.getComponents();
        if (components.length === 1) {
          if (components[0].$.checkBoxColumn.checked) {
            model.trigger('uncheck', model);
          } else {
            model.trigger('check', model);
          }
        }
      }
    }, this);

    model.off('unselected', null, this);
    model.on('unselected', function () {
      if (this.selected) {
        this.selected.removeClass('selected', false);
      }
      // FIXME: OB.UTIL.makeElemVisible(this.node, this.selected);
      this.selected = null;
    }, this);

    model.off('check', null, this);
    model.on('check', function () {
      var me = this;
      if (!tr || (tr && tr.destroyed)) {
        return;
      }

      // this line can not be selected
      if (!me.isSelectableLine(model)) {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_LineCanNotBeSelected'));
        return;
      }

      if (this.listStyle === 'checkboxlist') {
        var components = tr.getComponents(),
            allChecked = null,
            checkedLines = [];

        if (components.length === 1) {
          components[0].$.checkBoxColumn.check();
          tr.checked = true;

          _.each(tr.getParent().getComponents(), function (comp) {
            if (comp.checked) {
              checkedLines.push(comp.getComponents()[0].model);
              if (allChecked !== false) {
                allChecked = true;
              }
            } else {
              allChecked = false;
            }
          });

          components[0].doLineChecked({
            action: 'check',
            line: model,
            checkedLines: checkedLines,
            allChecked: allChecked
          });
        }
      }
    }, this);

    model.off('hideAllCheck', null, this);
    model.on('hideAllCheck', function () {
      if (tr && tr.destroyed) {
        return;
      }
      if (this.listStyle === 'checkboxlist') {
        var components = tr.getComponents();

        if (components.length === 1) {
          components[0].$.checkBoxColumn.addStyles('visibility:hidden');
        }
      }
    }, this);

    model.off('showAllCheck', null, this);
    model.on('showAllCheck', function () {
      if (tr && tr.destroyed) {
        return;
      }
      if (this.listStyle === 'checkboxlist') {
        var components = tr.getComponents();

        if (components.length === 1) {
          components[0].$.checkBoxColumn.addStyles('visibility:false');
        }
      }
    }, this);

    model.off('uncheck', null, this);
    model.on('uncheck', function () {
      if (!tr || (tr && tr.destroyed)) {
        return;
      }
      if (this.listStyle === 'checkboxlist') {
        var components = tr.getComponents(),
            checkedLines = [];

        if (components.length === 1) {
          components[0].$.checkBoxColumn.unCheck();
          tr.checked = false;

          _.each(tr.getParent().getComponents(), function (comp) {
            if (comp.checked) {
              checkedLines.push(comp.getComponents()[0].model);
            }
          });

          components[0].doLineChecked({
            action: 'uncheck',
            line: model,
            checkedLines: checkedLines,
            allChecked: false
          });
        }
      }
    }, this);
    return tr;
  },

  getSelectedRows: function () {
    var result = [],
        tRows = Object.keys(this.$.tbody.$),
        selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
    _.each(tRows, function (tr) {
      if (this.$.tbody.$[tr].attributes['class'] === selectedCssClass) {
        result.push(this.$.tbody.$[tr]);
      }
    }, this);
    return result;
  },

  getSelectedModels: function () {
    var result = [],
        tRows = Object.keys(this.$.tbody.$),
        selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
    _.each(tRows, function (tr) {
      if (this.$.tbody.$[tr].attributes['class'] === selectedCssClass) {
        if (this.$.tbody.$[tr].renderline && this.$.tbody.$[tr].renderline.model) {
          result.push(this.$.tbody.$[tr].renderline.model);
        }
      }
    }, this);
    return result;
  },

  setSelectedModels: function (models, selectFirst) {
    var i, tRows = Object.keys(this.$.tbody.$),
        selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
    _.each(tRows, function (tr) {
      var item = _.find(models, function (model) {
        return model.id === this.$.tbody.$[tr].renderline.model.id;
      }, this);
      this.$.tbody.$[tr].addRemoveClass(selectedCssClass, item !== undefined);
      if (item !== undefined && selectFirst) {
        selectFirst = false;
        this.selected = this.$.tbody.$[tr];
      }
    }, this);
    this.doSetMultiSelection({
      models: this.getSelectedModels()
    });
  },

  setSelectionMode: function (mode) {
    this.selectionMode = mode;
  },

  selectAll: function () {
    var tRows = Object.keys(this.$.tbody.$),
        selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
    _.each(tRows, function (tr) {
      this.$.tbody.$[tr].addRemoveClass(selectedCssClass, true);
    }, this);
    this.doSetMultiSelection({
      models: this.getSelectedModels()
    });
  },

  getMultiSelectIndex: function (selected) {
    var tRows = Object.keys(this.$.tbody.$),
        first = -1,
        last = -1,
        current = -1,
        selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
    _.each(tRows, function (tr, indx) {
      if (tr === selected.name) {
        current = indx;
      }
      if (this.$.tbody.$[tr].attributes['class'] === selectedCssClass) {
        last = indx;
        if (first < 0) {
          first = indx;
        }
      }
    }, this);
    return {
      first: first,
      last: last,
      current: current
    };
  },

  doMultiSelection: function (from, to) {
    var tRows = Object.keys(this.$.tbody.$),
        selectedCssClass = this.selectedCssClass ? this.selectedCssClass : 'selected';
    _.each(tRows, function (tr, indx) {
      this.$.tbody.$[tr].addRemoveClass(selectedCssClass, from <= indx && indx <= to);
    }, this);
  }
});