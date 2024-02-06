/*
 ************************************************************************************
 * Copyright (C) 2016-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, OB, _ */

/* Advanced Filter Modal definition */
enyo.kind({
  kind: 'OB.UI.SmallButton',
  name: 'OB.UI.AdvancedFilterClear',
  classes: 'btnlink-gray btnlink btnlink-small',
  style: 'width: 146px; ',
  events: {
    onClearFilterSelector: ''
  },
  tap: function () {
    this.doClearFilterSelector();
  },
  initComponents: function () {
    this.setContent(OB.I18N.getLabel('OBPOS_ClearAll'));
  }
});

enyo.kind({
  kind: 'OB.UI.SmallButton',
  name: 'OB.UI.AdvancedFilterApply',
  classes: 'btnlink-yellow btnlink btnlink-small',
  style: 'width: 146px; ',
  events: {
    onApplyFilters: ''
  },
  tap: function () {
    this.doApplyFilters();
  },
  isDefaultAction: true,
  initComponents: function () {
    this.setContent(OB.I18N.getLabel('OBPOS_applyFilters'));
  }
});

enyo.kind({
  name: 'OB.UI.AdvancedFilterTable',
  kind: 'Scroller',
  maxHeight: '400px',
  style: 'width: 100%; background-color: #fff; ',
  horizontal: 'hidden',
  initComponents: function () {
    this.inherited(arguments);
    this.filters = [];
  },

  setSortNone: function () {
    _.each(this.filters, function (flt) {
      var button = flt.owner.$['order' + flt.filter.name],
          buttonClasses;
      if (button) {
        buttonClasses = button.getClassAttribute().split(' ');
        button.removeClass(buttonClasses[buttonClasses.length - 1]);
        button.addClass('iconSortNone');
      }
    });
  },

  getEntityFilterPreferences: function (modelName) {
    var preferences;
    if (this.entityPreferences) {
      preferences = this.entityPreferences;
    } else {
      if (modelName) {
        var entityKeys = _.keys(OB.MobileApp.model.get('permissions')).filter(function (key) {
          return key.indexOf('EntitySelector_' + modelName) !== -1;
        });
        preferences = _.pick(OB.MobileApp.model.get('permissions'), entityKeys);
      } else {
        preferences = {};
      }
      this.entityPreferences = preferences;
    }
    return preferences;
  },

  addFilter: function (filter) {
    var filterEditor, modelName = this.owner.owner.model && this.owner.owner.model.prototype._modelName;
    var entityPreferences = this.getEntityFilterPreferences(modelName);
    var applyUIRestrictions = entityPreferences['EntitySelector_' + modelName + '_apply_UI_restrictions'];
    var disableFiltering = entityPreferences['EntitySelector_' + modelName + '_' + filter.name + '_f'];
    var disableSorting = entityPreferences['EntitySelector_' + modelName + '_' + (filter.sortName ? filter.sortName : filter.name) + '_s'];
    var remoteEntity = this.owner.owner.model && (OB.MobileApp.model.get('permissions')[this.owner.owner.model.prototype.remote] || this.owner.owner.model.prototype.forceRemoteEntity);
    if (remoteEntity && applyUIRestrictions && disableFiltering) {
      filterEditor = {
        kind: 'enyo.Input',
        type: 'text',
        classes: 'input narrow-input',
        hasRemoveButton: false,
        name: 'input' + filter.name,
        style: 'float: left; width: calc(100% - 130px); padding: 4px; border-color: transparent; text-align: center;',
        placeholder: OB.I18N.getLabel('OBMOBC_Character')[3] + OB.I18N.getLabel('OBMOBC_FilteringDisabled') + OB.I18N.getLabel('OBMOBC_Character')[3],
        disabled: true
      };
    } else if (filter.isList) {
      filterEditor = {
        kind: 'OB.UI.FilterSelectorList',
        name: 'input' + filter.name,
        hasRemoveButton: true,
        style: 'float: left; width: calc(100% - 120px); padding: 4px; height: 40px;'
      };
    } else if (filter.isAmount) {
      filterEditor = {
        kind: 'OB.UI.FilterSelectorAmount',
        name: 'input' + filter.name,
        hasRemoveButton: true,
        style: 'float: left; width: calc(100% - 120px); padding: 0; height: 40px;'
      };
    } else if (filter.isSelector) {
      filterEditor = {
        kind: 'OB.UI.FilterSelectorButton',
        name: 'input' + filter.name,
        hasRemoveButton: true,
        selectorPopup: filter.selectorPopup,
        filterName: filter.name,
        style: 'float: left; width: calc(100% - 120px); padding: 0; height: 40px;'
      };
    } else {
      filterEditor = {
        kind: 'OB.UI.FilterSelectorText',
        hasRemoveButton: true,
        name: 'input' + filter.name,
        style: 'float: left; width: calc(100% - 130px); padding: 4px;'
      };
    }

    var innerComponents = [filterEditor,
    {
      name: 'deleteSpacer' + filter.name,
      style: 'width: 30px; height: 30px; float: left; margin: 5px 0px 5px 8px;'
    }, {
      kind: 'OB.UI.SmallButton',
      name: 'delete' + filter.name,
      classes: 'btnlink-white iconRemove',
      style: 'float: left; margin: 5px 0px 5px 8px;',
      showing: false,
      tap: function () {
        this.owner.$['input' + filter.name].setValue('');
        if (this.owner.$['input' + filter.name].kind === 'OB.UI.FilterSelectorText') {
          this.owner.$['filterEditor' + filter.name].hideRemove();
        }
      }
    }];

    if (!(remoteEntity && applyUIRestrictions && disableSorting)) {
      innerComponents.push({
        kind: 'OB.UI.SmallButton',
        name: 'order' + filter.name,
        classes: 'btnlink-white iconSortNone',
        style: 'float: left; margin: 5px 8px 5px 8px;',
        tap: function () {
          var buttonClasses = this.getClassAttribute().split(' '),
              buttonClass = buttonClasses[buttonClasses.length - 1];
          this.owner.setSortNone();
          this.addClass(buttonClass === 'iconSortAsc' ? 'iconSortDesc' : (buttonClass === 'iconSortDesc' ? 'iconSortNone' : 'iconSortAsc'));
        }
      });
    }

    var filterLine = this.createComponent({
      filter: filter,
      style: 'width: 100%; clear:both; background-color: #fff; height: 40px; padding-top: 2px; padding-left: 13px; overflow: hidden;',
      components: [{
        style: 'float: left; width: 112px;  background-color: #e2e2e2; height: 29px; padding-top: 11px; padding-right: 5px; text-align: right;font-size: 16px; color: black; text-overflow: ellipsis; white-space: nowrap; overflow: hidden;',
        name: 'label' + filter.name,
        content: OB.I18N.getLabel(filter.caption)
      }, {
        style: 'float: left; text-align: left; padding-left: 2px; width: calc(100% - 125px);',
        name: 'filterEditor' + filter.name,
        components: innerComponents,
        hideRemove: function () {
          this.owner.$['delete' + filter.name].hide();
          this.owner.$['deleteSpacer' + filter.name].show();
        },
        showRemove: function () {
          this.owner.$['deleteSpacer' + filter.name].hide();
          this.owner.$['delete' + filter.name].show();
        }
      }]
    }, {
      owner: this
    });
    filterLine.render();
    if (filter.isList && !filterEditor.disabled) {
      filterLine.owner.$['input' + filter.name].changeColumn(filter);
    }
    this.filters.push(filterLine);
  },

  clearAll: function () {
    var me = this;
    _.each(this.filters, function (flt) {
      if (flt.filter.filter && !flt.owner.$['input' + flt.filter.name].disabled) {
        if (flt.filter.preset) {
          flt.owner.$['input' + flt.filter.name].setPresetValue(flt.filter.preset);
        } else {
          flt.owner.$['input' + flt.filter.name].setValue('');
          flt.owner.$['filterEditor' + flt.filter.name].hideRemove();
        }
        flt.owner.$['input' + flt.filter.name].removeClass('error');
      }
    });
    this.owner.$.dateFormatError.hide();
    this.owner.$.numberFormatError.hide();
    this.setSortNone();
  },

  applyFilters: function () {
    var me = this,
        filterError = false,
        result = {
        filters: [],
        orderby: null
        };

    me.owner.$.dateFormatError.hide();
    me.owner.$.numberFormatError.hide();
    _.each(this.filters, function (flt) {
      var value = flt.owner.$['input' + flt.filter.name].getValue(),
          operator = flt.filter.operator,
          dateValidated, caption, orderClasses, orderClass;

      if (flt.owner.$['order' + flt.filter.name]) {
        orderClasses = flt.owner.$['order' + flt.filter.name].getClassAttribute().split(' ');
        orderClass = orderClasses[orderClasses.length - 1];
      }

      flt.owner.$['input' + flt.filter.name].removeClass('error');

      value = value ? value.trim() : '';
      if (value) {
        if (flt.filter.isAmount) {
          operator = flt.owner.$['input' + flt.filter.name].getOperator();
          if (!operator) {
            operator = flt.filter.preset && flt.filter.preset.id ? flt.filter.preset.id : 'greaterThan';
          }
        } else if (flt.filter.isDate) {
          dateValidated = OB.Utilities.Date.OBToJS(value, OB.Format.date) || OB.Utilities.Date.OBToJS(value, 'yyyy-MM-dd');
          if (dateValidated) {
            value = OB.Utilities.Date.JSToOB(dateValidated, 'yyyy-MM-dd');
            me.owner.$.dateFormatError.hide();
            flt.owner.$['input' + flt.filter.name].removeClass('error');
          } else {
            me.owner.$.dateFormatError.show();
            flt.owner.$['input' + flt.filter.name].addClass('error');
            filterError = true;
          }
        } else if (flt.filter.isNumeric) {
          if (!_.isNaN(OB.I18N.parseNumber(value))) {
            value = OB.I18N.parseNumber(value);
            flt.owner.$['input' + flt.filter.name].setValue(value);
            me.owner.$.numberFormatError.hide();
          } else {
            flt.owner.$['input' + flt.filter.name].addClass('error');
            me.owner.$.numberFormatError.show();
            filterError = true;
          }
        } else if (flt.filter.isSelector) {
          caption = flt.owner.$['input' + flt.filter.name].$.filterButton.getContent();
        } else if (flt.filter.isList) {
          caption = flt.owner.$['input' + flt.filter.name].getCaption();
        } else {
          value = OB.UTIL.unAccent(value);
        }
        result.filters.push({
          operator: operator,
          column: flt.filter.column,
          value: value,
          caption: caption,
          hqlFilter: flt.filter.hqlFilter,
          isId: flt.filter.isSelector
        });
      }
      if (orderClass && orderClass !== 'iconSortNone') {
        result.orderby = {
          name: flt.filter.name,
          column: flt.filter.column,
          serverColumn: flt.filter.serverColumn ? flt.filter.serverColumn : flt.filter.column,
          sortName: flt.filter.sortName,
          direction: orderClass === 'iconSortAsc' ? 'asc' : 'desc',
          isLocationFilter: flt.filter.location,
          isDate: flt.filter.isDate,
          caption: flt.filter.caption
        };
      }
    });
    if (filterError) {
      return false;
    } else {
      return result;
    }
  }

});

enyo.kind({
  kind: 'OB.UI.Modal',
  name: 'OB.UI.ModalAdvancedFilters',
  i18nHeader: 'OBPOS_LblAdvancedFilters',
  style: 'height: 600px',
  events: {
    onHideThisPopup: ''
  },
  published: {
    filters: null
  },
  handlers: {
    onUpdateFilterSelector: 'updateFilterSelector',
    onClearFilterSelector: 'clearFilterSelector',
    onGetAdvancedFilterSelector: 'getAdvancedFilterSelector',
    onHasPresetFilterSelector: 'hasPresetFilterSelector',
    onCloseCancelSelector: 'closeCancelSelector',
    onApplyFilters: 'applyFilters'
  },
  body: {
    components: [{
      style: 'height: 32px; color: black; font-size: 16px;',
      components: [{
        style: 'width: 50%; float: left; ',
        components: [{
          style: 'float: right; ',
          kind: 'OB.UI.AdvancedFilterClear',
          name: 'btnClear'
        }]
      }, {
        style: 'width: 50%; float: left;',
        components: [{
          kind: 'OB.UI.AdvancedFilterApply',
          name: 'btnApply'
        }]
      }]
    }, {
      style: 'height: 15px;'
    }, {
      style: 'width: 100%; float: left; text-align:center; ',
      name: 'numberFormatError',
      showing: false,
      initComponents: function () {
        this.setContent(enyo.format(OB.I18N.getLabel('OBPOS_NumberFormatError'), OB.I18N.getDateFormatLabel()));
      }
    }, {
      style: 'width: 100%; float: left; text-align:center; ',
      name: 'dateFormatError',
      showing: false,
      initComponents: function () {
        this.setContent(enyo.format(OB.I18N.getLabel('OBPOS_DateFormatError'), OB.I18N.getDateFormatLabel()));
      }
    }, {
      kind: 'OB.UI.AdvancedFilterTable',
      name: 'filters'
    }]
  },

  setFilters: function (filters) {
    _.each(filters, function (prop) {
      if (prop.filter) {
        this.$.body.$.filters.addFilter(prop);
      }
    }, this);
  },

  updateFilterValue: function (flt, value, caption, operator) {
    if (flt.filter.isSelector) {
      flt.owner.$['input' + flt.filter.name].setSelectorValue(value, caption);
    } else if (flt.filter.isAmount) {
      flt.owner.$['input' + flt.filter.name].setPresetValue({
        id: operator,
        name: value
      });
    } else {
      flt.owner.$['input' + flt.filter.name].setValue(value);
    }
  },

  closeCancelSelector: function (inSender, inEvent) {
    if (this.$.body.showSelector) {
      this.$.body.showSelector = false;
      this.show();
    }
  },

  updateFilterSelector: function (inSender, inEvent) {
    if (this.$.body.showSelector) {
      if (this.savedFilters) {
        _.each(this.savedFilters.filters, function (savedFlt) {
          var filter = _.find(this.$.body.$.filters.filters, function (flt) {
            return flt.filter.column === savedFlt.column;
          });
          if (filter) {
            this.updateFilterValue(filter, savedFlt.value, savedFlt.caption, savedFlt.operator);
          }
        }, this);
      }
      this.$.body.showSelector = false;
      this.show();
      var selector = _.find(this.$.body.$.filters.filters, function (flt) {
        return flt.filter.name === inEvent.selector.name;
      }, this);
      if (selector) {
        selector.owner.$['input' + selector.filter.name].setSelectorValue(inEvent.selector.value, inEvent.selector.text);
      }
    }
  },

  clearFilterSelector: function (inSender, inEvent) {
    if (!inEvent.name || inEvent.name === this.name) {
      this.$.body.$.filters.clearAll();
    }
  },

  getAdvancedFilterSelector: function (inSender, inEvent) {
    if (inEvent.name === this.name && inEvent.callback) {
      this.presetLoaded = true;
      this.$.body.$.filters.clearAll();
      var advancedFilters = this.$.body.$.filters.applyFilters();
      var standardFlt = _.find(this.$.body.$.filters.filters, function (flt) {
        return flt.filter.column === inEvent.filter.column;
      });
      if (standardFlt) {
        if (standardFlt.filter.isSelector) {
          standardFlt.owner.$['input' + standardFlt.filter.name].setSelectorValue(inEvent.filter.value, inEvent.caption);
        } else if (standardFlt.filter.isAmount) {
          standardFlt.owner.$['input' + standardFlt.filter.name].setPresetValue({
            id: inEvent.operator,
            name: inEvent.filter.value
          });
        } else {
          standardFlt.owner.$['input' + standardFlt.filter.name].setValue(inEvent.filter.value);
        }
      }
      var inEventFilter = _.find(advancedFilters.filters, function (advFlt) {
        return advFlt.column === inEvent.filter.column;
      });
      if (inEventFilter) {
        inEventFilter.operator = inEvent.filter.operator;
        inEventFilter.value = inEvent.filter.value;
      } else {
        advancedFilters.filters.push(inEvent.filter);
      }
      inEvent.callback(advancedFilters);
    }
  },

  hasPresetFilterSelector: function (inSender, inEvent) {
    if (inEvent.name === this.name && inEvent.callback) {
      var preset = false,
          presetEquals = true,
          tmpFilters = this.$.body.$.filters.applyFilters();

      _.each(this.$.body.$.filters.filters, function (filter) {
        if (filter.filter.preset) {
          preset = true;
          if (this.presetLoaded) {
            var appliedFlt = _.find(tmpFilters.filters, function (flt) {
              return flt.column === filter.filter.column;
            });
            if (appliedFlt === undefined) {
              presetEquals = false;
            } else {
              var value = filter.filter.isList || filter.filter.isSelector ? filter.filter.preset.id : filter.filter.preset.name;
              if (appliedFlt.text !== value) {
                presetEquals = false;
              }
            }
          }
        }
      }, this);
      inEvent.callback(preset && presetEquals);
    }
  },

  applyFilters: function () {
    var tmpFilters = this.$.body.$.filters.applyFilters();
    if (tmpFilters) {
      this.filtersToApply = tmpFilters;
      this.doHideThisPopup();
    }
  },

  executeOnShow: function () {
    if (this.args.callback) {
      this.callback = this.args.callback;
      if (!this.presetLoaded) {
        this.presetLoaded = true;
        this.$.body.$.filters.clearAll();
      }
      this.filtersToApply = null;
    }
    // Find first component to focus
    var firstFilter = _.find(this.$.body.$.filters.filters, function (filter) {
      return filter.getShowing() && !filter.children[1].children[0].disabled;
    });
    if (firstFilter) {
      // Focus component after the focus set on "Apply Filters" button
      setTimeout(function () {
        firstFilter.children[1].children[0].focus();
      }, 200);
    }
    return true;
  },

  executeOnHide: function () {
    if (!this.$.body.showSelector) {
      this.savedFilters = null;
      if (this.callback) {
        this.callback(this.filtersToApply);
      }
    } else {
      this.savedFilters = this.$.body.$.filters.applyFilters();
    }
  }

});