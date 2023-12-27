/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, enyo, _ */

enyo.kind({
  kind: 'OB.UI.SmallButton',
  name: 'OB.UI.BrandButton',
  style: 'width: 86%; padding: 0px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;',
  classes: 'btnlink-white-simple',
  events: {
    onShowPopup: ''
  },
  tap: function () {
    if (!this.disabled) {
      this.doShowPopup({
        popup: 'modalproductbrand'
      });
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMOBC_LblBrand'));
  }
});

enyo.kind({
  name: 'OB.UI.SearchProductCharacteristicFilterPanel',
  style: 'margin-bottom: 14px;',
  classes: 'filter-panel-enabled',
  events: {
    onRemoveCustomFilter: ''
  },
  handlers: {
    onRightToolbarDisabled: 'toggleStatus'
  },
  components: [{
    name: 'removeFilter',
    classes: 'btnlink-gray btn-icon-small btn-icon-clear',
    style: 'width: 30px; float: right; cursor: pointer; padding-top: 0.1em;',
    ontap: 'removeCustomFilter'
  }, {
    name: 'infoPanel',
    style: 'padding-left: 6px; padding-bottom: 0.1em; padding-top: 0.1em; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 90%;'
  }],
  removeCustomFilter: function () {
    this.doRemoveCustomFilter({
      index: this.index
    });
  },
  toggleStatus: function (inSender, inEvent) {
    if (inEvent.status) {
      this.$.removeFilter.hide();
      this.addRemoveClass('filter-panel-enabled', false);
      this.addRemoveClass('filter-panel-disabled', true);
    } else {
      this.$.removeFilter.show();
      this.addRemoveClass('filter-panel-enabled', true);
      this.addRemoveClass('filter-panel-disabled', false);
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    if (this.customContent === null) {
      this.$.infoPanel.setContent(this.caption + " " + OB.I18N.getLabel('OBMOBC_Character')[0] + " " + this.text);
    } else {
      this.$.infoPanel.createComponent(this.customContent);
    }
  }
});
enyo.kind({
  name: 'OB.UI.SearchProductCharacteristicFilter',
  published: {
    type: 'PANEL',
    caption: '',
    text: null
  },
  events: {
    onAddProduct: ''
  },
  conditions: [],

  // List of default filters
  defaults: [],

  // Get component to filter render 
  renderInfo: function () {
    return null;
    // Example: 
    // return {
    //   kind: 'FilterExample',
    //     getFilterCondition: function () {
    //       return [{
    //         condition: 'EQUALS',
    //         value: 2
    //       }];
    //   }
    // };
  },

  hqlCriteria: function () {},

  // Get SQL filter
  sqlFilter: function (alias) {
    if (this.type === 'BUTTON' && this.conditions && _.isArray(this.conditions) && this.conditions.length > 0) {
      // Standard behavior for filter with type BUTTON
      var where = "",
          filters = [],
          sqlBuilderInfo = this.sqlBuilder();
      this.conditions.forEach(function (cond) {
        var condValue = cond.value;
        if (where !== "") {
          where = where + " and ";
        }
        if (alias !== undefined) {
          where = where + alias + "." + sqlBuilderInfo.field;
        } else {
          where = where + sqlBuilderInfo.field;
        }
        if (cond.condition === 'MORE_THAN') {
          where = where + " > ?";
        }
        if (cond.condition === 'LESS_THAN') {
          where = where + " < ?";
        }
        if (cond.condition === 'EQUALS') {
          where = where + " = ?";
        }
        if (cond.condition === 'NOT_EQUALS') {
          where = where + " <> ?";
        }
        if (cond.condition === 'CONTAINS') {
          where = where + " like ?";
          condValue = "%" + cond.value + "%";
        }
        if (cond.condition === 'NOT_CONTAINS') {
          where = where + " not like ?";
          condValue = "%" + cond.value + "%";
        }
        filters.push(condValue);
      });
      return {
        where: " and (" + where + ")",
        filters: filters
      };
    } else {
      // Default behavior
      return {
        where: null,
        filters: []
      };
    }
  },

  // Get SQL Build logic
  sqlBuilder: function () {
    return {
      field: '',
      fieldType: '' // Number, String, Date
    };
  },

  // Get extra attributes for order line
  lineAttributes: function () {
    return null;
  },

  // Function to post process filtered collection 
  //
  // function (collection, callback);
  //	collection: Collection of filtered items
  //    callback: Callback function to be called when post processing finish
  postProcess: null,

  // Add attributes to show 
  addItemAttributes: function (item, attr) {
    var filterAttr = item.get("filterAttr");
    if (!filterAttr) {
      item.set("filterAttr", []);
      filterAttr = item.get("filterAttr");
    }
    filterAttr.push({
      value: attr,
      separator: this.attributesSeparator
    });
  },

  attributesSeparator: ', '
});

enyo.kind({
  name: 'OB.UI.SearchProductCharacteristicHeader',
  kind: 'OB.UI.ScrollableTableHeader',
  events: {
    onSearchAction: ''
  },
  handlers: {
    onFiltered: 'searchAction',
    onClearAllAction: 'clearAllAction',
    onFinishServiceProposal: 'finishServiceProposal',
    onSelectCategoryItem: 'selectCategoryItem'
  },
  categoryTree: false,
  selectedCategory: "'__all__'",
  components: [{
    style: 'padding: 10px 10px 5px 10px;',
    components: [{
      style: 'margin: 5px 0px 0px 0px; width: 100%',
      name: 'filterpanel'
    }, {
      style: 'display: table;  width: 100%;',
      components: [{
        style: 'display: table-cell; width: 100%;',
        components: [{
          kind: 'OB.UI.SearchInputAutoFilter',
          name: 'productFilterText',
          style: 'width: 100%;',
          minLengthToSearch: 2,
          skipAutoFilterPref: 'OBPOS_remote.product'
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          name: 'productClearButton',
          classes: 'btnlink-gray btn-icon-small btn-icon-clear',
          style: 'width: 100px; margin: 0px 5px 8px 19px;',
          ontap: 'clearAllAction'
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          name: 'productSearchButton',
          classes: 'btnlink-yellow btn-icon-small btn-icon-search',
          style: 'width: 100px; margin: 0px 0px 8px 5px;',
          ontap: 'searchAction'
        }]
      }]
    }, {
      style: 'margin: 5px 0px 0px 0px;',
      components: [{
        kind: 'OB.UI.List',
        name: 'productcategory',
        classes: 'combo',
        style: 'width: 100%',
        renderHeader: enyo.kind({
          kind: 'enyo.Option',
          initComponents: function () {
            this.inherited(arguments);
            this.setValue('__all__');
            this.setContent(OB.I18N.getLabel('OBMOBC_SearchAllCategories'));
          }
        }),
        renderLine: enyo.kind({
          kind: 'enyo.Option',
          initComponents: function () {
            this.inherited(arguments);
            this.setValue(this.model.get('id'));
            this.setContent(this.model.get('_identifier'));
          }
        }),
        renderEmpty: 'enyo.Control'
      }]
    }, {
      style: 'margin: 5px 0px 0px 0px;',
      components: [{
        style: 'width: 100%; padding-left: 5px; padding-right: 35px; text-align: left; cursor: pointer; background-image: url(../org.openbravo.mobile.core/assets/img/iconDropdown.png); background-repeat: no-repeat; background-position: calc(100% - 13px); overflow: hidden; white-space: nowrap; text-overflow: ellipsis',
        classes: 'combo',
        kind: 'enyo.Button',
        name: 'productcategorytree',
        showing: false,
        handlers: {
          onkeydown: 'keydownHandler'
        },
        keydownHandler: function (inSender, inEvent) {
          var keyCode = inEvent.keyCode;
          if (keyCode === 13 || keyCode === 32) { //Handle ENTER and SPACE keys in buttons
            this.tap();
            return true;
          }
          return false;
        },
        tap: function () {
          if (!this.disabled) {
            var selectedCategory = this.owner.selectedCategory.substring(1);
            selectedCategory = selectedCategory.substring(0, selectedCategory.indexOf("'"));
            this.bubble('onShowPopup', {
              popup: 'modalcategorytree',
              args: {
                selectCategory: selectedCategory,
                origin: 'TAB_SEARCH'
              }
            });
          }
        },
        initComponents: function () {
          this.setContent(OB.I18N.getLabel('OBMOBC_SearchAllCategories'));
        }
      }]
    }, {
      name: 'filteringBy',
      style: 'text-align: left; font-weight: bold; font-size: 15px; color: #aaaaaa'
    }]
  }],
  setHeaderCollection: function (valueToSet) {
    this.$.productcategory.setCollection(valueToSet);
  },
  getSelectedCategories: function () {
    return this.categoryTree ? this.selectedCategory : this.$.productcategory.getValue();
  },
  disableFilterButtons: function (value) {
    this.$.productSearchButton.setDisabled(value);
    this.$.productClearButton.setDisabled(value);
  },
  searchAction: function () {
    this.doSearchAction({
      categoryTree: this.categoryTree,
      productCat: this.getSelectedCategories(),
      productName: this.$.productFilterText.getValue(),
      skipProduct: false,
      skipProductCharacteristic: false
    });
    return true;
  },
  clearAllAction: function () {
    this.$.productFilterText.setValue('');
    if (this.categoryTree) {
      this.$.productcategorytree.setContent(OB.I18N.getLabel('OBMOBC_SearchAllCategories'));
      this.selectedCategory = "'__all__'";
    } else {
      this.$.productcategory.setSelected(0);
    }
    this.$.filteringBy.setContent('');
    this.parent.$.brandButton.removeClass('btnlink-yellow-bold');
    var buttons = this.parent.$.filterButtons.getComponents();
    buttons.forEach(function (btn) {
      btn.removeClass('btnlink-yellow-bold');
    });
    this.parent.filterCustomClearConditions();
    this.parent.model.set('filter', []);
    this.parent.model.set('brandFilter', []);
    this.parent.genericParent = null;
    this.parent.products.reset();
    this.doSearchAction({
      categoryTree: this.categoryTree,
      productCat: this.getSelectedCategories(),
      productName: this.$.productFilterText.getValue(),
      skipProduct: true,
      skipProductCharacteristic: false
    });
  },
  selectCategoryItem: function (inSender, inEvent) {
    if (inEvent.origin === 'TAB_SEARCH') {
      this.$.productcategorytree.setContent(inEvent.category.get('_identifier'));
      this.selectedCategory = inEvent.children;
      this.doSearchAction({
        categoryTree: this.categoryTree,
        productCat: this.getSelectedCategories(),
        productName: this.$.productFilterText.getValue(),
        skipProduct: false,
        skipProductCharacteristic: false
      });
    }
  },
  finishServiceProposal: function (inSender, inEvent) {
    var status = inEvent.status;
    this.$.productFilterText.setValue(status.filterText);
    this.$.productcategory.setSelected(status.category);
    this.$.filteringBy.setContent(status.filteringBy);
    this.parent.model.set('filter', status.filter);
    this.parent.model.set('brandFilter', status.brandFilter);
    if (status.brandFilter.length > 0) {
      this.parent.$.brandButton.addClass('btnlink-yellow-bold');
    } else {
      this.parent.$.brandButton.removeClass('btnlink-yellow-bold');
    }
    OB.UI.SearchProductCharacteristic.prototype.filtersCustomClear();
    OB.UI.SearchProductCharacteristic.prototype.filterCustomClearConditions();
    OB.UI.SearchProductCharacteristic.prototype.setFilterCustomConditions(status.customFilters);
    this.owner.filtersCustomRender();
    this.parent.genericParent = status.genericParent;
    var buttons = this.parent.$.filterButtons.getComponents();
    buttons.forEach(function (btn) {
      if (btn.filter.conditions && _.isArray(btn.filter.conditions) && btn.filter.conditions.length > 0) {
        btn.addClass('btnlink-yellow-bold');
      } else {
        btn.removeClass('btnlink-yellow-bold');
      }
    });
    this.doSearchAction({
      categoryTree: this.categoryTree,
      productCat: this.getSelectedCategories(),
      productName: this.$.productFilterText.getValue(),
      skipProduct: false,
      skipProductCharacteristic: false
    });
  },
  init: function () {
    var me = this;
    this.inherited(arguments);
    this.categories = new OB.Collection.ProductCategoryList();
    this.products = new OB.Collection.ProductList();

    //first the main collection of the component
    //    this.$.products.setCollection(this.products);
    this.setHeaderCollection(this.categories);

    this.products.on('click', function (model) {
      this.doAddProduct({
        product: model,
        options: {
          blockAddProduct: true
        }
      });
    }, this);
  },

  loadCategories: function (callback) {
    var me = this;

    if (this.categories.length > 0) {
      if (callback) {
        callback();
      }
      return;
    }

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
      if (callback) {
        callback();
      }
    }

    function successCallbackCategories(dataCategories, me) {
      if (me.destroyed) {
        return;
      }
      if (dataCategories && dataCategories.length > 0) {
        var bestSeller = new Backbone.Model({
          _identifier: OB.I18N.getLabel('OBPOS_bestSellerCategory'),
          name: OB.I18N.getLabel('OBPOS_bestSellerCategory'),
          value: OB.I18N.getLabel('OBPOS_bestSellerCategory'),
          id: 'OBPOS_bestsellercategory'
        });
        dataCategories.models.unshift(bestSeller);
        me.categories.reset(dataCategories.models);
      } else {
        me.categories.reset();
      }
      // Verify if have a category tree
      var select = "select tree.category_id, sum(case when pc.m_product_category_id is null then 0 else 1 end) as childs " // 
      + "from m_product_category_tree tree left join m_product_category_tree child on tree.category_id = child.parent_id " //
      + "left join m_product_category pc on pc.m_product_category_id = child.category_id " //
      + "where tree.parent_id = '0' group by tree.category_id order by tree.seqno";
      OB.Dal.query(OB.Model.ProductCategoryTreeQuery, select, [], function (tree) {
        var showTree = _.some(tree.models, function (category) {
          return category.get('childs') > 0;
        });
        if (showTree) {
          me.$.productcategory.hide();
          me.$.productcategorytree.show();
          me.categoryTree = true;
        } else {
          me.$.productcategory.show();
          me.$.productcategorytree.hide();
          me.categoryTree = false;
        }
        if (callback) {
          callback();
        }
      }, errorCallback, me);
    }

    OB.Dal.find(OB.Model.ProductCategory, null, successCallbackCategories, errorCallback, this);
  }
});

enyo.kind({
  name: 'OB.UI.SearchProductCharacteristic',
  classes: 'span12',
  style: 'background-color: #ffffff; color: black; height: 612px;',
  published: {
    receipt: null,
    genericParent: null
  },
  handlers: {
    onChangePricelist: 'changePricelist',
    onSearchAction: 'searchAction',
    onClearAction: 'clearAction',
    onUpdateFilter: 'filterUpdate',
    onUpdateBrandFilter: 'brandFilterUpdate',
    onRemoveCustomFilter: 'removeCustomFilter',
    onCustomFilterUpdate: 'customFilterUpdate'
  },
  events: {
    onAddProduct: '',
    onSearchAction: '',
    onClearAction: '',
    onTabChange: '',
    onManageServiceProposal: '',
    onToggleLineSelection: ''
  },
  // List of custom filters ('OB.UI.SearchProductCharacteristicFilter')
  customFilters: [],
  statics: {
    postProcessCustomFilters: []
  },
  forceRemote: false,
  productCharacteristicFilterQualifier: 'PCH_Filter',
  // Set filter conditions
  setFilterCustomConditions: function (customFilters) {
    var me = this;
    customFilters.forEach(function (filter) {
      if (filter.type === 'PANEL') {
        OB.UI.SearchProductCharacteristic.prototype.filtersCustomAdd(filter);
      } else if (filter.type === 'BUTTON') {
        me.customFilters.forEach(function (cust) {
          if (cust.id === filter.id) {
            cust.conditions = filter.conditions;
          }
        });
      }
    });
  },
  //Filter type Implementor from Pos Terminal
  nameFilterTypes: [{
    filterName: 'STD',
    filterImplementor: function (textFilter) {
      return '%' + textFilter + '%';
    }
  }, {
    filterName: 'Sameorder',
    filterImplementor: function (textFilter) {
      return '%' + textFilter.replace(/ /gi, '%') + '%';
    }
  }],
  getNameFilterValue: function (productName) {
    var i;
    if (this.nameFilterTypes && _.isArray(this.nameFilterTypes) && this.nameFilterTypes.length > 0 && !this.nameFilterDefined) {
      for (i = 0; i < this.nameFilterTypes.length; i++) {
        if (this.nameFilterTypes[i].filterName === OB.MobileApp.model.get('terminal').terminalType.productSrchConf) {
          this.nameFilterDefined = this.nameFilterTypes[i];
          break;
        }
      }
    }
    return this.nameFilterDefined ? this.nameFilterDefined.filterImplementor(productName) : productName;
  },
  // Clear custom filters
  filtersCustomClear: function () {
    var i = 0;
    while (i < this.customFilters.length) {
      if (this.customFilters[i].type === 'PANEL' || this.customFilters[i].type === 'HIDDEN') {
        this.customFilters.splice(i, 1);
      } else if (this.customFilters[i].type === 'BUTTON') {
        this.customFilters[i].conditions = null;
        this.filtersCustomCopyDefaults(this.customFilters[i]);
        i++;
      }
    }
    this.filtersCustomBuildPostProcess();
  },
  // Clear filter conditions
  filterCustomClearConditions: function () {
    this.customFilters.forEach(function (filter) {
      if (filter.type === 'BUTTON') {
        filter.conditions = [];
      }
    });
  },
  // Copy default filters conditions
  filtersCustomCopyDefaults: function (filter) {
    if (filter.defaults && _.isArray(filter.defaults) && filter.defaults.length > 0) {
      filter.conditions = [];
      filter.defaults.forEach(function (item) {
        filter.conditions.push(item);
      });
    }
  },
  // Build post process filters
  filtersCustomBuildPostProcess: function () {
    var me = this;
    OB.UI.SearchProductCharacteristic.postProcessCustomFilters = [];
    this.customFilters.forEach(function (filter) {
      if (filter.postProcess && typeof (filter.postProcess) === "function") {
        OB.UI.SearchProductCharacteristic.postProcessCustomFilters.push(filter);
      }
    });
  },
  // Add a new custom filter
  filtersCustomAdd: function (filter) {
    this.customFilters.push(filter);
    if (filter.postProcess && typeof (filter.postProcess) === "function") {
      OB.UI.SearchProductCharacteristic.postProcessCustomFilters.push(filter);
    }
  },
  // Render custom filters 
  filtersCustomRender: function () {
    var index = 0,
        me = this,
        filtersPanels = this.$.searchProductCharacteristicHeader.$.filterpanel.getComponents(),
        filtersButtons = this.$.filterButtons.getComponents();
    // Remove visual components
    filtersPanels.forEach(function (item) {
      item.destroy();
    });
    filtersButtons.forEach(function (item) {
      item.destroy();
    });
    this.customFilters.sort(function (a, b) {
      if (a.text < b.text) {
        return -1;
      } else if (a.text > b.text) {
        return 1;
      }
      return 0;
    });
    // Insert visual component of filter
    this.customFilters.forEach(function (filter) {
      filter.index = index;
      if (filter.type === 'PANEL') {
        me.$.searchProductCharacteristicHeader.$.filterpanel.createComponent({
          kind: 'OB.UI.SearchProductCharacteristicFilterPanel',
          name: filter.filterName,
          text: filter.getText(),
          caption: filter.getCaption(),
          index: index++,
          customContent: filter.renderInfo(),
          style: 'width: 99.42%;'
        });
      }
      if (filter.type === 'BUTTON') {
        me.$.filterButtons.createComponent({
          kind: 'OB.UI.SmallButton',
          style: 'width: 86%; padding: 0px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap;',
          classes: 'btnlink-white-simple',
          content: filter.getText(),
          index: index++,
          customContent: filter.renderInfo(),
          filter: filter,
          events: {
            onShowPopup: ''
          },
          tap: function () {
            this.bubble('onShowPopup', {
              popup: 'modalsearchfilterbuilder',
              args: {
                filter: this.filter
              }
            });
          }
        });
      }
    });
    this.$.searchProductCharacteristicHeader.$.filterpanel.render();
    this.$.filterButtons.render();
  },
  // Process remove filter event
  removeCustomFilter: function (inSender, inEvent) {
    this.customFilters.splice(inEvent.index, 1);
    this.filtersCustomBuildPostProcess();
    this.filtersCustomRender();
    this.doSearchAction({
      categoryTree: this.$.searchProductCharacteristicHeader.categoryTree,
      productCat: this.$.searchProductCharacteristicHeader.getSelectedCategories(),
      productName: this.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
      filter: this.model.get('filter'),
      skipProduct: false,
      skipProductCharacteristic: false
    });
  },

  filterUpdate: function (inSender, inEvent) {
    var i, j, valuesIds, index, chValue = inEvent.value.value;
    valuesIds = this.model.get('filter').map(function (e) {
      return e.id;
    });
    for (j = 0; j < chValue.length; j++) {
      index = valuesIds.indexOf(chValue[j].get('id'));
      if (index === -1) {
        if (chValue[j].get('checked')) {
          this.model.get('filter').push({
            characteristic_id: chValue[j].get('characteristic_id'),
            id: chValue[j].get('id'),
            name: chValue[j].get('name'),
            checked: chValue[j].get('checked'),
            selected: chValue[j].get('selected')
          });
        }
      } else {
        if (!chValue[j].get('checked')) {
          this.model.get('filter').splice(index, 1);
          valuesIds.splice(index, 1);
        } else {
          this.model.get('filter')[index] = {
            characteristic_id: chValue[j].get('characteristic_id'),
            id: chValue[j].get('id'),
            name: chValue[j].get('name'),
            checked: chValue[j].get('checked'),
            selected: chValue[j].get('selected')
          };
        }

      }
    }
    this.model.set('filter', _.sortBy(this.model.get('filter'), function (e) {
      return e.characteristic_id;
    }));
    this.filteringBy();
    this.doSearchAction({
      categoryTree: this.$.searchProductCharacteristicHeader.categoryTree,
      productCat: this.$.searchProductCharacteristicHeader.getSelectedCategories(),
      productName: this.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
      filter: this.model.get('filter'),
      skipProduct: false,
      skipProductCharacteristic: false
    });
    return true;
  },
  brandFilterUpdate: function (inSender, inEvent) {
    var i, j, valuesIds, index, brandValue = inEvent.value.value;
    valuesIds = this.model.get('brandFilter').map(function (e) {
      return e.id;
    });
    for (j = 0; j < brandValue.length; j++) {
      index = valuesIds.indexOf(brandValue[j].get('id'));
      if (index === -1 && brandValue[j].get('checked')) {
        this.model.get('brandFilter').push({
          id: brandValue[j].get('id'),
          name: brandValue[j].get('name')
        });
      } else if (index !== -1 && (_.isUndefined(brandValue[j].get('checked')) || !brandValue[j].get('checked'))) {
        this.model.get('brandFilter').splice(index, 1);
        valuesIds.splice(index, 1);
      }
    }
    this.model.set('filter', _.sortBy(this.model.get('filter'), function (e) {
      return e.characteristic_id;
    }));
    this.filteringBy();
    if (this.model.get('brandFilter').length > 0) {
      this.$.brandButton.addClass('btnlink-yellow-bold');
    } else {
      this.$.brandButton.removeClass('btnlink-yellow-bold');
    }
    this.doSearchAction({
      categoryTree: this.$.searchProductCharacteristicHeader.categoryTree,
      productCat: this.$.searchProductCharacteristicHeader.getSelectedCategories(),
      productName: this.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
      filter: this.model.get('filter'),
      skipProduct: false,
      skipProductCharacteristic: false
    });
    return true;
  },
  customFilterUpdate: function (inSender, inEvent) {
    var skipCharacteristic = false;
    if (inEvent.filter && inEvent.filter.index !== undefined) {
      var buttons = this.$.filterButtons.getComponents();
      buttons.forEach(function (btn) {
        if (btn.index === inEvent.filter.index) {
          if (inEvent.filter.conditions && _.isArray(inEvent.filter.conditions) && inEvent.filter.conditions.length > 0) {
            btn.addClass('btnlink-yellow-bold');
          } else {
            btn.removeClass('btnlink-yellow-bold');
          }
        }
      });
    }
    if (inEvent.params) {
      skipCharacteristic = inEvent.params.skipProductCharacteristic ? inEvent.params.skipProductCharacteristic : skipCharacteristic;
    }
    this.doSearchAction({
      categoryTree: this.$.searchProductCharacteristicHeader.categoryTree,
      productCat: this.$.searchProductCharacteristicHeader.getSelectedCategories(),
      productName: this.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
      filter: this.model.get('filter'),
      skipProduct: false,
      skipProductCharacteristic: skipCharacteristic
    });
    return true;
  },
  filteringBy: function () {
    var filteringBy = OB.I18N.getLabel('OBMOBC_FilteringBy'),
        selectedItems, i;
    selectedItems = _.compact(this.model.get('filter').map(function (e) {
      if (e.selected) {
        return e.name;
      }
    }));
    if ((_.isUndefined(this.genericParent) || _.isNull(this.genericParent)) && selectedItems.length === 0 && this.model.get('brandFilter').length === 0) {
      this.$.searchProductCharacteristicHeader.$.filteringBy.setContent('');
      return true;
    }
    if (!_.isUndefined(this.genericParent) && !_.isNull(this.genericParent)) {
      filteringBy = filteringBy + ' ' + this.genericParent.get('_identifier');
      if (selectedItems.length + this.model.get('brandFilter').length > 0) {
        filteringBy = filteringBy + ', ';
      }
    }
    for (i = 0; i < selectedItems.length; i++) {
      filteringBy = filteringBy + ' ' + selectedItems[i];
      if (i !== selectedItems.length - 1 || (i === selectedItems.length - 1 && this.model.get('brandFilter').length > 0)) {
        filteringBy = filteringBy + ', ';
      }
    }
    for (i = 0; i < this.model.get('brandFilter').length; i++) {
      filteringBy = filteringBy + ' ' + this.model.get('brandFilter')[i].name;
      if (i !== this.model.get('brandFilter').length - 1) {
        filteringBy = filteringBy + ', ';
      }
    }
    this.$.searchProductCharacteristicHeader.$.filteringBy.setContent(filteringBy);
  },
  executeOnShow: function (params) {
    this.filtersCustomRender();
    var me = this,
        criteria = {};
    this.doClearAction();
    this.genericParent = params ? params.model : null;
    this.$.searchProductCharacteristicHeader.loadCategories(function () {
      me.doSearchAction({
        categoryTree: me.$.searchProductCharacteristicHeader.categoryTree,
        productCat: me.$.searchProductCharacteristicHeader.getSelectedCategories(),
        productName: me.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
        filter: me.model.get('filter'),
        skipProduct: !me.genericParent,
        skipProductCharacteristic: true
      });
      me.filteringBy();
    });
    setTimeout(function () {
      me.parent.$.searchCharacteristicTabContent.$.searchProductCharacteristicHeader.$.productFilterText.focus();
    }, 100);
  },
  components: [{
    kind: 'OB.UI.SearchProductCharacteristicHeader',
    classes: 'span12',
    style: 'display: table-cell; '
  }, {
    style: 'display: table; width:100%',
    components: [{
      name: 'characteristicsFilterContainer',
      kind: 'Scroller',
      style: 'width: 150px;',
      maxHeight: '483px',
      horizontal: 'hidden',
      classes: 'row-fluid',
      components: [{
        components: [{
          kind: 'OB.UI.BrandButton',
          name: 'brandButton'
        }, {
          name: 'filterButtons',
          classes: 'row-fluid'
        }, {
          kind: 'OB.UI.Table',
          name: 'productsCh',
          renderEmpty: 'enyo.Control',
          renderLine: 'OB.UI.RenderProductCh',
          executeAfterClear: function () {
            var searchCharacteristicTabContent = this.parent.parent.parent.parent,
                leftAreaProductsCh = document.getElementById(searchCharacteristicTabContent.$.productsCh.$.scrollArea.id);
            if (leftAreaProductsCh) {
              leftAreaProductsCh.style.height = '0px';
            }
          }
        }, {
          name: 'renderLoadingCh',
          style: 'border-bottom: 1px solid #cccccc; padding: 20px; text-align: center; font-weight: bold; font-size: 20px; color: #cccccc',
          showing: false,
          initComponents: function () {
            this.setContent(OB.I18N.getLabel('OBMOBC_LblLoading'));
          }
        }]
      }]
    }, {
      style: 'display: table-cell; width: 100%; padding-right: 5px;',
      classes: 'row-fluid ',
      components: [{
        classes: 'row-fluid',
        components: [{
          kind: 'OB.UI.ScrollableTable',
          name: 'products',
          scrollAreaMaxHeight: '483px',
          renderEmpty: 'OB.UI.RenderEmpty',
          renderLine: 'OB.UI.RenderProduct',
          executeAfterClear: function () {
            var searchCharacteristicTabContent = this.parent.parent.parent.parent,
                rightAreaProducts = document.getElementById(searchCharacteristicTabContent.$.products.$.scrollArea.id),
                leftAreaProducts = document.getElementById(searchCharacteristicTabContent.$.characteristicsFilterContainer.id),
                totalArea = document.getElementById(searchCharacteristicTabContent.id),
                topArea = document.getElementById(searchCharacteristicTabContent.$.searchProductCharacteristicHeader.$.control.id),
                rightAreaLimit = document.getElementById(searchCharacteristicTabContent.$.products.$.tlimit.id);

            var totalAreaHeight = totalArea ? totalArea.clientHeight : 0,
                topAreaHeight = topArea ? topArea.clientHeight : 0,
                rightAreaLimitHeight = rightAreaLimit ? rightAreaLimit.clientHeight : 0,
                newHeight;

            if (rightAreaProducts) {
              rightAreaProducts.style.height = '0px';
            }

            newHeight = totalAreaHeight - topAreaHeight - rightAreaLimitHeight - 1;
            if (newHeight <= 0) {
              newHeight = 483;
            }

            if (leftAreaProducts) {
              leftAreaProducts.style.height = newHeight + 'px';
            }
          },
          executeAfterRender: function () {
            var searchCharacteristicTabContent = this.parent.parent.parent.parent,
                totalArea = document.getElementById(searchCharacteristicTabContent.id),
                topArea = document.getElementById(searchCharacteristicTabContent.$.searchProductCharacteristicHeader.$.control.id),
                rightAreaProducts = document.getElementById(searchCharacteristicTabContent.$.products.$.scrollArea.id),
                rightAreaLimit = document.getElementById(searchCharacteristicTabContent.$.products.$.tlimit.id),
                leftAreaProducts = document.getElementById(searchCharacteristicTabContent.$.characteristicsFilterContainer.id);

            setTimeout(function () {
              var totalAreaHeight = totalArea ? totalArea.clientHeight : 0,
                  totalAreaWidth = totalArea ? totalArea.clientWidth : 0,
                  topAreaHeight = topArea ? topArea.clientHeight : 0,
                  rightAreaProductsHeight = rightAreaProducts ? rightAreaProducts.clientHeight : 0,
                  rightAreaLimitHeight = rightAreaLimit ? rightAreaLimit.clientHeight : 0,
                  leftAreaProductsHeight = leftAreaProducts ? leftAreaProducts.clientHeight : 0,
                  newHeight, newLeftHeight, newWidth;

              newHeight = totalAreaHeight - topAreaHeight - rightAreaLimitHeight - 1;
              if (newHeight < 0) {
                newHeight = 0;
              }
              if (rightAreaProductsHeight > 0 && rightAreaProductsHeight !== newHeight) {
                rightAreaProducts.style.height = newHeight + 'px';
              }
              if (leftAreaProductsHeight > 0 && leftAreaProductsHeight !== newHeight) {
                leftAreaProducts.style.height = newHeight + 'px';
              }
              newWidth = totalAreaWidth - 150;
              rightAreaProducts.style.width = newWidth + 'px';

            }, 100);


          }
        }, {
          name: 'renderLoading',
          style: 'border-bottom: 1px solid #cccccc; padding: 20px; text-align: center; font-weight: bold; font-size: 30px; color: #cccccc',
          showing: false,
          initComponents: function () {
            this.setContent(OB.I18N.getLabel('OBMOBC_LblLoading'));
          }
        }]
      }]
    }]
  }],
  init: function (model) {
    this.model = model;
    var me = this,
        params = [],
        whereClause = '';
    this.inherited(arguments);
    this.categories = new OB.Collection.ProductCategoryList();
    this.products = new OB.Collection.ProductList();
    this.productsCh = new OB.Collection.ProductCharacteristicValueList();
    //first the main collection of the component
    this.$.products.setCollection(this.products);
    this.$.productsCh.setCollection(this.productsCh);
    //    this.$.products.getHeader().setHeaderCollection(this.categories);
    //this.$.characteristicsFilterContainer.addStyles('display: table-cell;');
    if (OB.MobileApp.model.hasPermission('OBPOS_HideProductCharacteristics', true)) {
      this.$.characteristicsFilterContainer.addStyles('display:none;');
    }

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
    }

    function successCallbackProductCh(dataProductCh) {
      if (me.destroyed) {
        return;
      }
      if (dataProductCh && dataProductCh.length > 0) {
        me.productsCh.reset(dataProductCh.models);
      } else {
        me.productsCh.reset();
      }
    }

    this.products.on('click', function (model) {
      if (!model.get('isGeneric')) {
        // Include filters line attributes
        var attrs = {};
        this.customFilters.forEach(function (filter) {
          var filterAttr = filter.lineAttributes();
          if (filterAttr) {
            _.each(_.keys(filterAttr), function (key) {
              attrs[key] = filterAttr[key];
            });
          }
        });
        // Add product to order
        me.doAddProduct({
          product: model,
          attrs: _.keys(attrs).length === 0 ? undefined : attrs,
          options: {
            blockAddProduct: true
          },
          context: this,
          callback: function (added) {
            if (added) {
              // If is a service remove from search list
              if (model.get('productType') === 'S' && model.get('isLinkedToProduct')) {
                this.products.remove(model);
              }
            }
          }
        });
        // Notify to filters 
        this.customFilters.forEach(function (filter) {
          filter.doAddProduct({
            product: model,
            options: {
              blockAddProduct: true
            }
          });
        });
      } else {
        me.doTabChange({
          tabPanel: 'searchCharacteristic',
          keyboard: false,
          edit: false,
          options: model
        });
      }
    }, this);

    if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
      OB.Dal.query(OB.Model.ProductCharacteristicValue, 'select distinct(m_characteristic_id), _identifier from m_product_ch_value where obposFilteronwebpos = "true" order by UPPER(_identifier) asc', [], successCallbackProductCh, errorCallback, this);
    } else {
      var criteria = {};
      OB.Dal.find(OB.Model.Characteristic, criteria, successCallbackProductCh, errorCallback);
    }
  },
  changePricelist: function (inSender, inEvent) {
    this.currentPriceList = inEvent.priceList;
    if ((this.$.searchProductCharacteristicHeader.$.productcategory.getValue() !== '__all__' && this.$.searchProductCharacteristicHeader.$.productcategory.getValue() !== "") || this.$.searchProductCharacteristicHeader.$.productFilterText.getValue() !== "" || this.model.get('filter').length !== 0) {
      this.doSearchAction({
        productCat: this.$.searchProductCharacteristicHeader.$.productcategory.getValue() || '__all__',
        productName: this.$.searchProductCharacteristicHeader.$.productFilterText.getValue() || '',
        filter: this.model.get('filter'),
        skipProduct: false,
        skipProductCharacteristic: false
      });
    }
  },
  receiptChanged: function () {
    this.receipt.on('clear', function () {
      this.$.searchProductCharacteristicHeader.$.productFilterText.setContent('');
      this.$.searchProductCharacteristicHeader.$.productcategory.setContent('');
      // A filter should be set before show products. -> Big data!!
      // this.products.exec({priceListVersion: OB.MobileApp.model.get('pricelistversion').id, product: {}});
    }, this);

    this.receipt.on('showProductList', function (line, proposalType, finalCallback) {
      var previousStatus, customFilters = [],
          me = this;
      if (proposalType && proposalType === 'mandatory') {
        if (line && !_.find(this.receipt.get('lines').models, function (l) {
          return l.get('id') === line.get('id');
        })) {
          return true;
        }
        this.customFilters.forEach(function (customFilter) {
          if (customFilter.type === 'BUTTON') {
            customFilters.push({
              type: 'BUTTON',
              id: customFilter.id,
              conditions: customFilter.conditions
            });
          } else if (customFilter.type === 'PANEL') {
            customFilters.push(customFilter);
          }
        });

        previousStatus = {
          tab: OB.MobileApp.model.get('lastPaneShown'),
          filterText: this.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
          category: this.$.searchProductCharacteristicHeader.$.productcategory.getSelected(),
          filteringBy: this.$.searchProductCharacteristicHeader.$.filteringBy.getContent(),
          filter: this.model.get('filter'),
          brandFilter: this.model.get('brandFilter'),
          customFilters: customFilters,
          genericParent: this.genericParent
        };
        OB.UI.SearchProductCharacteristic.prototype.filtersCustomClear();
        OB.UI.SearchProductCharacteristic.prototype.filterCustomClearConditions();
        OB.UI.SearchProductCharacteristic.prototype.filtersCustomAdd(new OB.UI.SearchServicesFilter({
          text: line.get('product').get("_identifier"),
          productId: line.get('product').get('id'),
          orderline: line
        }));
        OB.UI.SearchProductCharacteristic.prototype.filtersCustomAdd(new OB.UI.MandatoryServicesFilter());
        this.doToggleLineSelection({
          status: true
        });
      } else if (proposalType && proposalType === 'final') {
        previousStatus = {
          callback: finalCallback
        };
        OB.UI.SearchProductCharacteristic.prototype.filtersCustomClear();
        OB.UI.SearchProductCharacteristic.prototype.filterCustomClearConditions();
        OB.UI.SearchProductCharacteristic.prototype.filtersCustomAdd(new OB.UI.FinalMandatoryServicesFilter());
        this.doToggleLineSelection({
          status: true
        });
      }

      this.bubble('onTabChange', {
        tabPanel: 'searchCharacteristic'
      });

      if (proposalType) {
        this.doManageServiceProposal({
          proposalType: proposalType,
          previousStatus: previousStatus
        });
      }

      function serviceSearchAction() {
        me.receipt.off('calculatedReceipt', serviceSearchAction);
        if (OB.MobileApp.model.get('serviceSearchMode')) {
          me.doSearchAction({
            productCat: '__all__',
            productName: '',
            skipProduct: false,
            skipProductCharacteristic: false
          });
        }
      }

      if (me.receipt.calculatingReceipt) {
        me.$.products.hide();
        me.$.renderLoading.show();
        me.receipt.on('calculatedReceipt', serviceSearchAction);
      } else {
        serviceSearchAction();
      }

    }, this);
  },
  clearAction: function (inSender, inEvent) {
    this.waterfall('onClearAllAction');
  },
  addWhereFilter: function (values) {
    if (values.productName && !this.skipProductNameFilter) {
      this.whereClause = this.whereClause + ' and _filter like ?';
      this.params.push(this.getNameFilterValue(values.productName));
    }
    if (values.productCat) {
      if (values.productCat.indexOf('__all__') === -1 && values.productCat.indexOf('OBPOS_bestsellercategory') === -1) {
        this.whereClause = this.whereClause + ' and m_product_category_id ';
        if (values.categoryTree) {
          this.whereClause = this.whereClause + ' in (' + values.productCat + ')';
        } else {
          this.whereClause = this.whereClause + ' = ?';
          this.params.push(values.productCat);
        }
      }
      if (values.productCat.indexOf('OBPOS_bestsellercategory') !== -1) {
        this.whereClause = this.whereClause + " and bestseller = 'true'";
      }
    }
  },
  showProducts: function (dataProducts) {
    if (dataProducts && dataProducts.length > 0) {
      // Initializing combo of categories without filtering
      dataProducts.models.sort(function (a, b) {
        if (a.get('_identifier') < b.get('_identifier')) {
          return -1;
        }
        if (a.get('_identifier') > b.get('_identifier')) {
          return 1;
        }
        return 0;
      });
      this.products.reset(dataProducts.models);
    } else {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_NoProductsFound'));
      this.products.reset();
    }
    this.$.renderLoading.hide();
    this.$.products.show();
  },
  getProductCategoryFilter: function (forceRemote) {
    var productCategory = null,
        productCat = this.$.searchProductCharacteristicHeader.getSelectedCategories();
    if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) || forceRemote) {
      if (productCat === "'__all__'" || (productCat !== undefined && productCat.indexOf('OBPOS_bestsellercategory') !== -1)) {
        productCategory = {
          columns: ['productCategory'],
          operator: 'equals',
          value: productCat.indexOf('OBPOS_bestsellercategory') !== -1 ? 'OBPOS_bestsellercategory' : '__all__',
          isId: true
        };
      } else if (productCat !== undefined && productCat.indexOf("'") === 0) {
        //TODO: improve the way packs and combos are handled
        if (productCat.indexOf('7899A7A4204749AD92881133C4EE7A57') === -1 && productCat.indexOf('BE5D42E554644B6AA262CCB097753951') === -1) {
          var paramIDs = [];
          productCat.split(', ').forEach(function (catId) {
            paramIDs.push(catId.replace("'", '').replace("'", ''));
          });
          productCategory = {
            columns: [],
            operator: OB.Dal.FILTER,
            value: 'Category_Tree_Filter',
            filter: 'Category_Tree_Filter',
            params: [paramIDs]
          };
        } else {
          //pack or combo selected
          productCategory = {
            columns: ['productCategory'],
            operator: 'equals',
            value: productCat.replace("'", '').replace("'", ''),
            isId: true
          };
        }
      } else {
        productCategory = {
          columns: ['productCategory'],
          operator: 'equals',
          value: productCat,
          isId: true
        };
      }
    } else {
      if ((productCat === "'__all__'" || productCat === "__all__" || (productCat !== undefined && productCat.indexOf('OBPOS_bestsellercategory') !== -1))) {
        productCategory = productCat.indexOf('OBPOS_bestsellercategory') !== -1 ? 'OBPOS_bestsellercategory' : '__all__';
      } else if (productCat !== undefined && productCat.indexOf("'") === 0) {
        productCategory = productCat;
      } else {
        productCategory = "'" + productCat + "'";
      }
    }
    return productCategory;
  },
  disableFilters: function (value) {
    this.$.searchProductCharacteristicHeader.disableFilterButtons(value);
  },
  searchAction: function (inSender, inEvent) {
    this.$.products.hide();
    this.$.renderLoading.show();
    this.params = [];
    this.whereClause = '';

    var criteria = {},
        me = this,
        filterWhereClause = '',
        valuesString = '',
        brandString = '',
        i, j, forceRemote, doLocalIfRemoteFails = true;

    // Disable the filters button
    me.disableFilters(true);

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
    }

    // Initializing combo of categories without filtering

    function postProccessFilters(index, dataProducts, queryWasExecutedOnline) {
      var filteredDataProducts;
      if (index < OB.UI.SearchProductCharacteristic.postProcessCustomFilters.length) {
        OB.UI.SearchProductCharacteristic.postProcessCustomFilters[index].postProcess(dataProducts, function (queryWasExecutedOnline) {
          postProccessFilters(++index, dataProducts, queryWasExecutedOnline);
        }, queryWasExecutedOnline);
      } else {
        if (_.pluck(me.customFilters, 'filterName').indexOf('ServicesFilter') === -1) {
          filteredDataProducts = new OB.Collection.ProductList(dataProducts.filter(function (model) {
            return model.get('productType') !== 'S' || !model.get('isLinkedToProduct');
          }));
          me.showProducts(filteredDataProducts);
        } else {
          me.showProducts(dataProducts);
        }
      }
    }
    var synchId = null;

    function successCallbackProductCh(dataProductCh) {

      var filterWhereClause = '',
          characteristicId = '';
      if (dataProductCh && dataProductCh.length > 0) {
        for (i = 0; i < dataProductCh.length; i++) {
          for (j = 0; j < me.model.get('filter').length; j++) {
            characteristicId = dataProductCh.models[i].get('id');
            if (characteristicId === me.model.get('filter')[j].characteristic_id) {
              dataProductCh.models[i].set('filtering', true);
            }
          }
        }
        me.productsCh.reset(dataProductCh.models);
        me.$.renderLoadingCh.hide();
        me.$.productsCh.show();
      } else if (me.model.get('filter').length > 0) {
        if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && !forceRemote) {
          filterWhereClause = ' and prod_ch.m_characteristic_id in (' + "'" + me.model.get('filter')[0].characteristic_id + "'" + ')';
          for (i = 1; i < me.model.get('filter').length; i++) {
            filterWhereClause = filterWhereClause + ' or prod_ch.m_characteristic_id in (' + "'" + me.model.get('filter')[i].characteristic_id + "'" + ')';
          }
          OB.Dal.query(OB.Model.ProductCharacteristicValue, 'select distinct(m_characteristic_id), _identifier from m_product_ch_value as prod_ch where obposFilteronwebpos = "true" and 1=1' + filterWhereClause + ' order by UPPER(_identifier) asc', [], function (dataProdCh) {
            if (dataProdCh && dataProdCh.length > 0) {
              for (i = 0; i < dataProdCh.length; i++) {
                dataProdCh.models[i].set('filtering', true);
              }
              me.productsCh.reset(dataProdCh.models);
              me.$.renderLoadingCh.hide();
              me.$.productsCh.show();
            }
          }, function (error) {
            OB.UTIL.showError("OBDAL error: " + error);
          });
        } else {
          var productFilterText = inSender.$.productFilterText,
              productcategory = inSender.$.productcategory;

          var remoteCriteria = [],
              characteristicParams = "",
              characteristic = [],
              characteristicValue = [],
              brandparams = [];
          var criteria = {},
              characteristicfilter = {},
              brandfilter = {},
              chFilter = {};
          var productText;
          if (productFilterText !== undefined && productcategory !== undefined) {
            if (me.model.get('filter').length > 0) {
              for (i = 0; i < me.model.get('filter').length; i++) {
                if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
                  characteristic.push(me.model.get('filter')[i].characteristic_id);
                }
              }
              for (i = 0; i < characteristic.length; i++) {
                for (j = 0; j < me.model.get('filter').length; j++) {
                  if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                    characteristicValue.push(me.model.get('filter')[j].id);
                  }
                }
                if (i > 0) {
                  characteristicParams += ";";
                }
                characteristicParams += characteristicValue;
                characteristicValue = [];
              }
            }
            characteristicfilter.columns = [];
            characteristicfilter.operator = OB.Dal.FILTER;
            characteristicfilter.value = me.productCharacteristicFilterQualifier;
            if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
              productText = (OB.MobileApp.model.hasPermission('OBPOS_remote.product' + OB.Dal.USESCONTAINS, true) ? '%' : '') + productFilterText.getValue() + '%';
            } else {
              productText = '%' + productFilterText.getValue() + '%';
            }
            characteristicfilter.params = [productText, productcategory.value, characteristicParams];
            remoteCriteria.push(characteristicfilter);
          }
          if (me.model.get('brandFilter').length > 0) {
            for (i = 0; i < me.model.get('brandFilter').length; i++) {
              brandparams.push(me.model.get('brandFilter')[i].id);
            }
            if (brandparams.length > 0) {
              brandfilter = {
                columns: [],
                operator: OB.Dal.FILTER,
                value: 'BChar_Filter',
                params: [brandparams]
              };
              remoteCriteria.push(brandfilter);
            }
          }
          criteria.hqlCriteria = [];
          me.customFilters.forEach(function (hqlFilter) {
            if (!_.isUndefined(hqlFilter.hqlCriteriaCharacteristics)) {
              var hqlCriteriaFilter = hqlFilter.hqlCriteriaCharacteristics();
              if (!_.isUndefined(hqlCriteriaFilter)) {
                hqlCriteriaFilter.forEach(function (filter) {
                  if (filter) {
                    remoteCriteria.push(filter);
                  }
                });
              }
            }
          });
          criteria.remoteFilters = remoteCriteria;
          criteria.forceRemote = forceRemote;
          OB.Dal.find(OB.Model.Characteristic, criteria, function (dataProdCh) {
            if (dataProdCh && dataProdCh.length > 0) {
              for (i = 0; i < dataProdCh.length; i++) {
                dataProdCh.models[i].set('filtering', true);
              }
              me.productsCh.reset(dataProdCh.models);
              me.$.renderLoadingCh.hide();
              me.$.productsCh.show();
            }
          }, function (error) {
            OB.UTIL.showError("OBDAL error: " + error);
          });
        }
      } else {
        me.productsCh.reset();
        me.$.renderLoadingCh.hide();
        me.$.productsCh.show();
      }
    }

    function filterProductCharacterisctics() {
      synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('filterProductCharacterisctics');
      var productFilterText, brandparams = [],
          remoteCriteria = [],
          characteristic = [],
          characteristicValue = [],
          characteristicParams = "",
          criteria = {},
          characteristicfilter = {},
          brandfilter = {},
          chFilter = {},
          productCategory, productText;

      if (inSender.$.searchProductCharacteristicHeader !== undefined) {
        productFilterText = inSender.$.searchProductCharacteristicHeader.$.productFilterText;
      } else {
        productFilterText = inSender.$.productFilterText;
      }
      productCategory = me.getProductCategoryFilter(forceRemote);

      if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && !forceRemote) {
        var BChar_Filter = "",
            productCharacteristicFilter = "",
            params = [],
            num, brandStr;
        // brand filter
        if (me.model.get('brandFilter').length > 0) {
          num = 0;
          brandStr = "";
          for (i = 0; i < me.model.get('brandFilter').length; i++) {
            if (me.model.get('brandFilter')[i]) {
              num++;
              if (num > 1) {
                brandStr += ',';
              }
              brandStr += "'" + me.model.get('brandFilter')[i].id + "'";
            }
          }
          BChar_Filter = " and ( exists (select 1 from m_product_ch_value pc join m_product mp where pc.m_product_id = mp.m_product_id and ch.id = pc.m_characteristic_id and ( mp.brand in ( " + brandStr + " ) )) ) ";

        }
        // product name and category
        if (productFilterText !== undefined && productCategory !== undefined) {
          if (productFilterText !== "" || productCategory !== "__all__" || productCategory !== "'__all__'") {
            if (productFilterText !== "") {
              params.push("%" + productFilterText.getValue() + "%");
            }
            productCharacteristicFilter = "AND (EXISTS (SELECT 1 FROM m_product_ch_value pchvf, m_product pf WHERE  pchvf.m_product_id = pf.m_product_id AND ch.id = pchvf.m_characteristic_id AND ";
            if ((productCategory === "__all__") || (productCategory === "'__all__'") || (productCategory === "") || (productCategory === "''")) {
              productCharacteristicFilter += " (Upper(pf._filter) LIKE Upper(?)) ";
            } else if (productCategory === "OBPOS_bestsellercategory") {
              productCharacteristicFilter += "pf.bestseller = 'true' AND ( Upper(pf._filter) LIKE Upper(?) ) ";
            } else {
              productCharacteristicFilter += " (Upper(pf._filter) LIKE Upper(?)) AND(pf.m_product_category_id IN ( " + productCategory + " )) ";
            }
          }
        }
        // characteristic filter
        if (me.model.get('filter').length > 0) {
          for (i = 0; i < me.model.get('filter').length; i++) {
            if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
              characteristic.push(me.model.get('filter')[i].characteristic_id);
            }
          }
          for (i = 0; i < characteristic.length; i++) {
            var characteristicsValuesStr = "";
            num = 0;
            for (j = 0; j < me.model.get('filter').length; j++) {
              if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                if (num > 0) {
                  characteristicsValuesStr += ',';
                }
                characteristicsValuesStr += "'" + me.model.get('filter')[j].id + "'";
                num++;
              }

            }
            productCharacteristicFilter += "AND ( EXISTS (SELECT 1 FROM   m_product_ch_value ppchv WHERE  ppchv.m_product_id = pchvf.m_product_id AND ( ppchv.m_ch_value_id IN (" + characteristicsValuesStr + ") ))  ) ";
          }
        }
        if (productFilterText !== undefined && productCategory !== undefined) {
          productCharacteristicFilter += "))";
        }
        // external modules filter
        var sqlCriteriaFilter = "";
        me.customFilters.forEach(function (sqlFilter) {
          if (!_.isUndefined(sqlFilter.sqlFilterQueryCharacteristics)) {
            var criteriaFilter = sqlFilter.sqlFilterQueryCharacteristics();
            if (criteriaFilter.query !== null) {
              params = params.concat(criteriaFilter.filters);
              sqlCriteriaFilter += criteriaFilter.query;
            }
          }
        });
        OB.UTIL.SynchronizationHelper.finished(synchId, 'filterProductCharacterisctics');
        OB.Dal.query(OB.Model.Characteristic, "select distinct(ch.id), ch._identifier from m_characteristic as ch where 1=1  " + BChar_Filter + productCharacteristicFilter + sqlCriteriaFilter + " order by UPPER(ch._identifier) asc", params, successCallbackProductCh, errorCallback, this);
      } else {
        //brand filter
        if (me.model.get('brandFilter').length > 0) {
          for (i = 0; i < me.model.get('brandFilter').length; i++) {
            if (me.model.get('brandFilter')[i]) {
              brandparams.push(me.model.get('brandFilter')[i].id);
            }
          }
          if (brandparams.length > 0) {
            brandfilter = {
              columns: [],
              operator: OB.Dal.FILTER,
              value: 'BChar_Filter',
              params: [brandparams]
            };
            remoteCriteria.push(brandfilter);
          }
        }
        // product name and category 
        if (productFilterText !== undefined && productCategory) {
          // characteristic filter
          if (me.model.get('filter').length > 0) {
            for (i = 0; i < me.model.get('filter').length; i++) {
              if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
                characteristic.push(me.model.get('filter')[i].characteristic_id);
              }
            }
            for (i = 0; i < characteristic.length; i++) {
              for (j = 0; j < me.model.get('filter').length; j++) {
                if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                  characteristicValue.push(me.model.get('filter')[j].id);
                }
              }
              if (i > 0) {
                characteristicParams += ";";
              }
              characteristicParams += characteristicValue;
              characteristicValue = [];
            }
          }
          if (!_.isEmpty(productFilterText.getValue()) || (productCategory.value)) {
            var category = inEvent.productCat.indexOf('OBPOS_bestsellercategory') >= 0 ? 'OBPOS_bestsellercategory' : (inEvent.productCat.indexOf('__all__') >= 0 ? '__all__' : [productCategory.value]);
            characteristicfilter.columns = [];
            characteristicfilter.operator = OB.Dal.FILTER;
            characteristicfilter.value = me.productCharacteristicFilterQualifier;
            if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
              productText = (OB.MobileApp.model.hasPermission('OBPOS_remote.product' + OB.Dal.USESCONTAINS, true) ? '%' : '') + productFilterText.getValue() + '%';
            } else {
              productText = '%' + productFilterText.getValue() + '%';
            }
            characteristicfilter.params = [productText, productCategory.filter ? productCategory.params[0] : category, characteristicParams];
            remoteCriteria.push(characteristicfilter);
          }
        }

        //external modules filter
        criteria.hqlCriteria = [];
        me.customFilters.forEach(function (hqlFilter) {
          if (!_.isUndefined(hqlFilter.hqlCriteriaCharacteristics)) {
            var hqlCriteriaFilter = hqlFilter.hqlCriteriaCharacteristics();
            if (!_.isUndefined(hqlCriteriaFilter)) {
              hqlCriteriaFilter.forEach(function (filter) {
                if (filter) {
                  remoteCriteria.push(filter);
                }
              });
            }
          }
        });
        criteria.remoteFilters = remoteCriteria;
        criteria.forceRemote = forceRemote;
        OB.UTIL.SynchronizationHelper.finished(synchId, 'filterProductCharacterisctics');
        OB.Dal.find(OB.Model.Characteristic, criteria, successCallbackProductCh, errorCallback);
      }
    }
    var synchIdSearchAction = null;

    function successCallbackProducts(dataProducts, online) {
      postProccessFilters(0, dataProducts, online);
      if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) || forceRemote) {
        OB.UTIL.SynchronizationHelper.finished(synchIdSearchAction, 'searchAction');
        if (!inEvent.skipProductCharacteristic) {
          filterProductCharacterisctics();
        }
      }
      me.disableFilters(false);
    }

    function errorCallbackProducts(tx, error) {
      var message = error ? error.message : undefined;
      OB.UTIL.HookManager.executeHooks('OBPOS_PostFailureRemoteProductSearch', {
        tx: tx,
        error: error,
        message: message,
        doLocalIfRemoteFails: doLocalIfRemoteFails
      }, function (args) {
        var message;
        OB.UTIL.SynchronizationHelper.finished(synchIdSearchAction, 'searchAction');
        me.disableFilters(false);
        message = args.message && args.message.className ? 'OBPOS_ErrorServerGeneric' : args.message;
        if (!error && !OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && doLocalIfRemoteFails) {
          OB.UTIL.showError(OB.I18N.getLabel(message ? message : 'OBMOBC_RemoteConnectionFail'));
          me.doSearchAction({
            categoryTree: me.$.searchProductCharacteristicHeader.categoryTree,
            productCat: me.$.searchProductCharacteristicHeader.getSelectedCategories(),
            productName: me.$.searchProductCharacteristicHeader.$.productFilterText.getValue(),
            filter: me.model.get('filter'),
            skipProduct: false,
            skipProductCharacteristic: false,
            forceOffline: true
          });
        } else {
          if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
            me.doClearAction();
          }
          OB.UTIL.showWarning(OB.I18N.getLabel(message ? message : 'OBMOBC_ConnectionFail'));
          me.$.renderLoading.hide();
          me.$.products.collection.reset();
          me.$.products.show();
        }
      });
    }

    me.whereClause = ' where 1 = 1 ';
    OB.UTIL.HookManager.executeHooks('OBPOS_PreSearchProducts', {
      context: me,
      inEvent: inEvent
    }, function (args) {
      if (args && args.cancelOperation && args.cancelOperation === true) {
        me.disableFilters(false);
        return;
      }
      if (!inEvent.skipProduct && OB.UI.SearchProductCharacteristic.prototype.forceRemote) {
        forceRemote = inEvent.forceOffline ? false : true;
        if (forceRemote) {
          me.customFilters.forEach(function (hqlFilter) {
            if (!_.isUndefined(hqlFilter.hqlCriteria) && !_.isUndefined(hqlFilter.forceRemote)) {
              hqlFilter.hqlCriteria().forEach(function (filter) {
                if (filter !== "" && hqlFilter.forceRemote) {
                  doLocalIfRemoteFails = false;
                }
              });
            }
          });
        }
      } else if (!inEvent.skipProduct) {
        forceRemote = false;
        me.customFilters.forEach(function (hqlFilter) {
          if (!_.isUndefined(hqlFilter.hqlCriteria) && !_.isUndefined(hqlFilter.forceRemote)) {
            hqlFilter.hqlCriteria().forEach(function (filter) {
              if (filter !== "" && !forceRemote && hqlFilter.forceRemote) {
                forceRemote = true;
                doLocalIfRemoteFails = false;
              }
            });
          }
        });
      }

      if (!OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && !forceRemote) {
        me.whereClause = me.whereClause + " and isGeneric = 'false'";

        me.addWhereFilter(inEvent);

        if (me.genericParent) {
          me.whereClause = me.whereClause + ' and generic_product_id = ?';
          me.params.push(me.genericParent.get('id'));
        }
        if (me.model.get('filter').length > 0) {
          for (i = 0; i < me.model.get('filter').length; i++) {
            if (i !== 0 && (me.model.get('filter')[i].characteristic_id !== me.model.get('filter')[i - 1].characteristic_id)) {
              filterWhereClause = filterWhereClause + ' and exists (select * from m_product_ch_value as char where m_ch_value_id in (' + valuesString + ') and char.m_product_id = product.m_product_id)';
              valuesString = '';
            }
            if (valuesString !== '') {
              valuesString = valuesString + ', ' + "'" + me.model.get('filter')[i].id + "'";
            } else {
              valuesString = "'" + me.model.get('filter')[i].id + "'";
            }
            if (i === me.model.get('filter').length - 1) { //last iteration
              filterWhereClause = filterWhereClause + ' and exists (select * from m_product_ch_value as char where m_ch_value_id in (' + valuesString + ') and char.m_product_id = product.m_product_id)';
              valuesString = '';
            }
          }
        }
        if (me.model.get('brandFilter').length > 0) {
          for (i = 0; i < me.model.get('brandFilter').length; i++) {
            brandString = brandString + "'" + me.model.get('brandFilter')[i].id + "'";
            if (i !== me.model.get('brandFilter').length - 1) {
              brandString = brandString + ', ';
            }
          }
          filterWhereClause = filterWhereClause + ' and product.brand in (' + brandString + ')';
        }
        // Add custom parameters
        var customParams = [];
        me.params.forEach(function (param) {
          customParams.push(param);
        });
        if (!inEvent.skipProduct) {
          // Add custom filters
          me.customFilters.forEach(function (filter) {
            var sqlFilter = filter.sqlFilter();
            if (sqlFilter && sqlFilter.where) {
              filterWhereClause = filterWhereClause + sqlFilter.where;
              if (sqlFilter.filters && sqlFilter.filters.length > 0) {
                sqlFilter.filters.forEach(function (item) {
                  customParams.push(item);
                });
              }
            }
          });
          var limit = null;
          if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
            limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
          }
          if (OB.MobileApp.model.hasPermission('EnableMultiPriceList', true) && me.currentPriceList && me.currentPriceList !== OB.MobileApp.model.get('terminal').priceList) {
            var select = "select product. * , pp.pricestd as currentStandardPrice " //
            + " from m_product product join m_productprice pp on product.m_product_id = pp.m_product_id and pp.m_pricelist_id = '" + me.currentPriceList + "'" //
            + me.whereClause + filterWhereClause;
            OB.Dal.query(OB.Model.Product, select, customParams, function (dataProducts) {
              successCallbackProducts(dataProducts, false);
            }, errorCallbackProducts, me, null, limit);
          } else {
            OB.Dal.query(OB.Model.Product, 'select * from m_product as product' + me.whereClause + filterWhereClause, customParams, function (dataProducts) {
              successCallbackProducts(dataProducts, false);
            }, errorCallbackProducts, me, null, limit);
          }
        } else {
          me.$.renderLoading.hide();
          me.$.products.show();
          me.disableFilters(false);
        }
        if (!inEvent.skipProductCharacteristic) {
          me.$.renderLoadingCh.show();
          me.$.productsCh.hide();
          filterProductCharacterisctics();
        }
      } else {
        var characteristicparams = [],
            brandparams = [],
            selectedcharacteristic = [];

        if (me.model.get('filter').length > 0) {
          for (i = 0; i < me.model.get('filter').length; i++) {
            if (me.model.get('filter')[i].characteristic_id) {
              selectedcharacteristic.push(me.model.get('filter')[i].characteristic_id);
            }
          }
        }
        if (!inEvent.skipProduct) {
          if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) || forceRemote) {
            synchIdSearchAction = OB.UTIL.SynchronizationHelper.busyUntilFinishes('searchAction');
          }
          var remoteCriteria = [],
              characteristic = [],
              characteristicValue = [];
          var brandfilter = {},
              productCategory = me.getProductCategoryFilter(forceRemote),
              characteristicfilter = {
              columns: [],
              operator: OB.Dal.FILTER,
              value: 'Characteristic_Filter',
              params: [characteristicparams.toString()]
              },
              productName = {
              columns: ['_filter'],
              operator: OB.Dal.STARTSWITH,
              value: inEvent.productName
              },
              ispack = {
              columns: ['ispack'],
              operator: 'equals',
              value: 'false',
              fieldType: 'forceString'
              };

          if (inEvent.productCat !== "'__all__'") {
            if (inEvent.productCat.indexOf('OBPOS_bestsellercategory') !== -1) {
              var bestsellers = {
                columns: ['bestseller'],
                operator: 'equals',
                value: true,
                boolean: true
              };
              productCategory = null;
              remoteCriteria.push(bestsellers);
            } else if (inEvent.productCat.indexOf("'") === 0) {
              //TODO: improve the way packs and combos are handled
              if (inEvent.productCat.indexOf('7899A7A4204749AD92881133C4EE7A57') === -1 && inEvent.productCat.indexOf('BE5D42E554644B6AA262CCB097753951') === -1) {
                remoteCriteria.push(ispack);
              }
            }
          }
          if (me.model.get('brandFilter').length > 0) {
            for (i = 0; i < me.model.get('brandFilter').length; i++) {
              brandparams.push(me.model.get('brandFilter')[i].id);
            }
            if (brandparams.length > 0) {
              brandfilter = {
                columns: [],
                operator: OB.Dal.FILTER,
                value: 'Brand_Filter',
                params: [brandparams]
              };
              remoteCriteria.push(brandfilter);
            }
          }
          if (me.model.get('filter').length > 0) {
            for (i = 0; i < me.model.get('filter').length; i++) {
              if (!characteristic.includes(me.model.get('filter')[i].characteristic_id)) {
                characteristic.push(me.model.get('filter')[i].characteristic_id);
              }
            }
            for (i = 0; i < characteristic.length; i++) {
              for (j = 0; j < me.model.get('filter').length; j++) {
                if (characteristic[i] === me.model.get('filter')[j].characteristic_id) {
                  characteristicValue.push(me.model.get('filter')[j].id);
                }
              }
              if (characteristicValue.length > 0) {
                characteristicfilter = {
                  columns: [],
                  operator: OB.Dal.FILTER,
                  value: 'Characteristic_Filter',
                  filter: characteristic[i],
                  params: [characteristicValue]
                };
                remoteCriteria.push(characteristicfilter);
                characteristicValue = [];
              }
            }
          }
          if (me.model.get('filter').length > 0) {
            remoteCriteria.push(ispack);
          }
          if (me.model.get('brandFilter').length > 0) {
            remoteCriteria.push(ispack);
          }
          criteria.hqlCriteria = [];
          me.customFilters.forEach(function (hqlFilter) {
            if (!_.isUndefined(hqlFilter.hqlCriteria)) {
              var hqlCriteriaFilter = hqlFilter.hqlCriteria();
              if (hqlCriteriaFilter) {
                hqlCriteriaFilter.forEach(function (filter) {
                  if (filter) {
                    remoteCriteria.push(filter);
                  }
                });
              }
            }
          });
          if (productCategory) {
            remoteCriteria.push(productCategory);
          }
          remoteCriteria.push(productName);
          criteria.remoteFilters = remoteCriteria;
          criteria.forceRemote = forceRemote;
          if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
            criteria._limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
          }
          if (OB.MobileApp.model.hasPermission('EnableMultiPriceList', true) && me.currentPriceList && me.currentPriceList !== OB.MobileApp.model.get('terminal').priceList) {
            var currentPriceList = {
              'currentPriceList': me.currentPriceList
            };
            criteria.remoteParams = currentPriceList;
            OB.Dal.find(OB.Model.Product, criteria, function (dataProducts) {
              successCallbackProducts(dataProducts, true);
            }, errorCallbackProducts, me);

          } else {
            OB.Dal.find(OB.Model.Product, criteria, function (dataProducts) {
              successCallbackProducts(dataProducts, true);
            }, errorCallbackProducts, me);
          }
        } else {
          me.$.renderLoading.hide();
          me.$.products.show();
          me.disableFilters(false);
        }
        // when (!inEvent.skipProduct) filterProductCharacterisctics  funtion is called in successCallbackProducts
        if (!inEvent.skipProductCharacteristic && inEvent.skipProduct) {
          me.$.products.collection.reset();
          filterProductCharacterisctics();
        }
      }
    });
  }
});