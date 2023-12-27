/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/*global enyo, _, Backbone */

// Create a new client model for FilterBuilderOption
var FilterBuilderOption = Backbone.Model.extend({
  defaults: {
    id: '',
    name: ''
  }
});

var FilterBuilderOptionList = Backbone.Collection.extend({
  model: FilterBuilderOption
});

//Create a new client model for FilterBuilderOption
var FilterBuilderRow = Backbone.Model.extend({
  defaults: {
    id: '',
    selected: true,
    fieldType: ''
  }
});

var FilterBuilderRowList = Backbone.Collection.extend({
  model: FilterBuilderRow
});


/*
 * Dialog Header
 */
enyo.kind({
  name: 'OB.UI.ModalSearchFilterBuilderTopHeader',
  kind: 'OB.UI.ScrollableTableHeader',
  events: {
    onHideThisPopup: '',
    onSelectFilter: '',
    onSearchAction: ''
  },
  components: [{
    style: 'display: table;',
    components: [{
      style: 'display: table-cell; width: 100%;',
      components: [{
        name: 'title',
        style: 'text-align: center; vertical-align: middle'
      }]
    }, {
      style: 'display: table-cell;',
      components: [{
        name: 'doneBrandButton',
        kind: 'OB.UI.SmallButton',
        ontap: 'doneAction'
      }]
    }, {
      style: 'display: table-cell;',
      components: [{
        classes: 'btnlink-gray',
        name: 'cancelBrandButton',
        kind: 'OB.UI.SmallButton',
        ontap: 'cancelAction'
      }]
    }]
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.doneBrandButton.setContent(OB.I18N.getLabel('OBMOBC_LblDone'));
    this.$.cancelBrandButton.setContent(OB.I18N.getLabel('OBMOBC_LblCancel'));
  },
  doneAction: function () {
    var filter = this.parent.parent.parent.filter,
        renderInfo = filter.renderInfo(),
        filters = [],
        hasError = false;
    if (renderInfo) {
      filters = renderInfo.getFilterCondition();
    } else {
      var conditions = this.parent.parent.parent.$.body.$.standardBuilderConditions.$.tbody.controls;
      // Check filters conditions
      conditions.forEach(function (item) {
        var row = item.$.modalSearchFilterBuilderRow.$;
        if (row.filterCheckBox.checked) {
          var value = row.filterValue.getValue(),
              sqlBuilder = filter.sqlBuilder();
          if (sqlBuilder.fieldType.toUpperCase() === "STRING" && !value && !value.trim()) {
            OB.UTIL.showAlert.display(OB.I18N.getLabel('OBMOBC_FilterQueryBuilderErrEmptyValue'), "Error", "alert-error", false);
            hasError = true;
          }
          if (sqlBuilder.fieldType.toUpperCase() === "NUMBER") {
            var error = true;
            if (value.match(/^\d*\.{0,1}\d*$/) !== null || value.match(/^\d*\,{0,1}\d*$/) !== null) {
              value = value.replace(',', '.');
              error = false;
            }
            if (error || isNaN(parseFloat(value))) {
              OB.UTIL.showAlert.display(OB.I18N.getLabel('OBMOBC_FilterQueryBuilderErrInvalidNumber'), "Error", "alert-error", false);
              hasError = true;
            }
          }
          filters.push({
            condition: row.filterCondition.getValue(),
            value: sqlBuilder.fieldType.toUpperCase() === "NUMBER" ? parseFloat(value) : value
          });
        }
      });
    }
    if (!hasError) {
      filter.conditions = filters;
      this.doSelectFilter({
        filter: filter
      });
      this.doHideThisPopup();
    }
  },
  cancelAction: function () {
    this.doHideThisPopup();
  }
});

/**
 * Filter condition
 */
enyo.kind({
  name: 'OB.UI.ModalSearchFilterBuilderRow',
  style: 'display: table;  width: 100%;',
  components: [{
    name: 'filterCheckBox',
    kind: 'OB.UI.CheckboxButton',
    tag: 'div',
    style: 'display: table-cell; width: 50px;'
  }, {
    style: 'display: table-cell; width: 180px; padding-top: 5px;',
    components: [{
      kind: 'OB.UI.List',
      name: 'filterCondition',
      classes: 'combo',
      style: 'width: 90%',
      renderEmpty: 'enyo.Control',
      renderLine: enyo.kind({
        kind: 'enyo.Option',
        initComponents: function () {
          this.inherited(arguments);
          this.setValue(this.model.get('id'));
          this.setContent(this.model.get('name'));
        }
      })
    }]
  }, {
    style: 'display: table-cell; width: 120px;',
    kind: 'OB.UI.SearchInput',
    name: 'filterValue'
  }],
  initComponents: function () {
    this.inherited(arguments);
    var cond = this.model.get("condition"),
        items = new FilterBuilderOptionList();
    this.$.filterCondition.setCollection(items);
    items.add(new FilterBuilderOption({
      id: 'MORE_THAN',
      name: OB.I18N.getLabel('OBMOBC_FilterQueryBuilderMoreThan')
    }));
    items.add(new FilterBuilderOption({
      id: 'LESS_THAN',
      name: OB.I18N.getLabel('OBMOBC_FilterQueryBuilderLessThan')
    }));
    items.add(new FilterBuilderOption({
      id: 'EQUALS',
      name: OB.I18N.getLabel('OBMOBC_FilterQueryBuilderEquals')
    }));
    items.add(new FilterBuilderOption({
      id: 'NOT_EQUALS',
      name: OB.I18N.getLabel('OBMOBC_FilterQueryBuilderNotEquals')
    }));
    if (this.model.get("fieldType").toUpperCase() === "STRING") {
      items.add(new FilterBuilderOption({
        id: 'CONTAINS',
        name: OB.I18N.getLabel('OBMOBC_FilterQueryBuilderContains')
      }));
      items.add(new FilterBuilderOption({
        id: 'NOT_CONTAINS',
        name: OB.I18N.getLabel('OBMOBC_FilterQueryBuilderNotContains')
      }));
    }
    this.$.filterCheckBox.check();
    this.$.filterCondition.setSelected(cond === 'MORE_THAN' ? 0 : cond === 'LESS_THAN' ? 1 : cond === 'EQUALS' ? 2 : cond === 'NOT_EQUALS' ? 3 : cond === 'CONTAINS' ? 4 : 5);
    this.$.filterValue.setValue(this.model.get("value"));
  }
});

/*
 * Modal definition
 */
enyo.kind({
  name: 'OB.UI.ModalSearchFilterBuilder',
  topPosition: '170px',
  kind: 'OB.UI.Modal',
  published: {
    characteristic: null
  },
  executeOnShow: function (args) {
    this.filter = this.args.filter;
    var me = this,
        renderInfo = this.filter.renderInfo();
    this.sqlBuilder = this.filter.sqlBuilder();
    this.$.header.parent.addStyles('padding: 0px; border-bottom: 1px solid #cccccc');
    this.$.header.$.modalSearchFilterBuilderTopHeader.$.title.setContent(this.filter.getCaption());
    if (renderInfo) {
      this.$.body.$.standardBuilder.hide();
      this.$.body.$.customBuilder.show();
      this.$.body.$.customBuilder.createComponent(renderInfo);
    } else {
      this.filterRows = new FilterBuilderRowList();
      if (this.filter.conditions && _.isArray(this.filter.conditions) && this.filter.conditions.length > 0) {
        this.filter.conditions.forEach(function (cond) {
          me.addFilterRow(cond.condition, cond.value);
        });
      } else if (this.filter.defaults && _.isArray(this.filter.defaults) && this.filter.defaults.length > 0) {
        this.filter.defaults.forEach(function (cond) {
          me.addFilterRow(cond.condition, cond.value);
        });
      } else {
        this.addFilterRow('MORE_THAN', '');
      }
      this.$.body.$.customBuilder.hide();
      this.$.body.$.standardBuilder.show();
      this.$.body.$.standardBuilderConditions.setCollection(this.filterRows);
    }
  },
  addFilterRow: function (condition, value) {
    this.filterRows.add(new FilterBuilderRow({
      id: OB.UTIL.get_UUID(),
      selected: true,
      fieldType: this.sqlBuilder.fieldType,
      condition: condition,
      value: value
    }));
  },
  i18nHeader: '',
  body: {
    components: [{
      name: 'standardBuilder',
      components: [{
        kind: 'OB.UI.ScrollableTable',
        name: 'standardBuilderConditions',
        scrollAreaMaxHeight: '300px',
        renderEmpty: 'enyo.Control',
        renderLine: 'OB.UI.ModalSearchFilterBuilderRow'
      }, {
        kind: 'Image',
        src: '../org.openbravo.mobile.core/assets/img/iconAdd.png',
        sizing: "cover",
        width: 42,
        height: 42,
        tap: function () {
          this.owner.owner.addFilterRow('MORE_THAN', '');
        }
      }]
    }, {
      name: 'customBuilder'
    }]
  },
  initComponents: function () {
    this.inherited(arguments);
    this.$.closebutton.hide();
    this.$.header.createComponent({
      kind: 'OB.UI.ModalSearchFilterBuilderTopHeader',
      style: 'border-bottom: 0px'
    });
  }
});