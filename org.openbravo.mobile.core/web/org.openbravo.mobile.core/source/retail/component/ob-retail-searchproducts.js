/*
 ************************************************************************************
 * Copyright (C) 2012-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, _ */

enyo.kind({
  name: 'OB.UI.SearchProductHeader',
  kind: 'OB.UI.ScrollableTableHeader',
  events: {
    onSearchAction: '',
    onClearAction: ''
  },
  handlers: {
    onFiltered: 'searchAction',
    onSelectCategoryItem: 'selectCategoryItem'
  },
  categoryTree: false,
  selectedCategory: "'__all__'",
  components: [{
    style: 'padding: 10px 10px 5px 10px;',
    components: [{
      style: 'display: table;',
      components: [{
        style: 'display: table-cell; width: 100%;',
        components: [{
          kind: 'OB.UI.SearchInputAutoFilter',
          name: 'productname',
          style: 'width: 100%;',
          minLengthToSearch: 2
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
          classes: 'btnlink-gray btn-icon-small btn-icon-clear',
          style: 'width: 100px; margin: 0px 5px 8px 19px;',
          ontap: 'clearAction'
        }]
      }, {
        style: 'display: table-cell;',
        components: [{
          kind: 'OB.UI.SmallButton',
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
    }]
  }],
  setHeaderCollection: function (valueToSet) {
    this.$.productcategory.setCollection(valueToSet);
  },
  getSelectedCategories: function () {
    return this.categoryTree ? this.selectedCategory : this.$.productcategory.getValue();
  },
  searchAction: function () {
    this.doSearchAction({
      categoryTree: this.categoryTree,
      productCat: this.getSelectedCategories(),
      productName: this.$.productname.getValue()
    });
    return true;
  },
  selectCategoryItem: function (inSender, inEvent) {
    if (inEvent.origin === 'TAB_SEARCH') {
      this.$.productcategorytree.setContent(inEvent.category.get('_identifier'));
      this.selectedCategory = inEvent.children;
      this.doSearchAction({
        categoryTree: this.categoryTree,
        productCat: this.getSelectedCategories(),
        productName: this.$.productname.getValue(),
        skipProduct: false,
        skipProductCharacteristic: false
      });
    }
  },
  loadCategories: function (callback) {
    var me = this;

    if (me.$.productcategory.getCollection().length > 0) {
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
        me.$.productcategory.getCollection().reset(dataCategories.models);
      } else {
        me.$.productcategory.getCollection().reset();
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
  },
  clearAction: function () {
    this.$.productname.setValue('');
    if (this.categoryTree) {
      this.$.productcategorytree.setContent(OB.I18N.getLabel('OBMOBC_SearchAllCategories'));
      this.selectedCategory = "'__all__'";
    } else {
      this.$.productcategory.setSelected(0);
    }
    this.doClearAction();
  }
});

enyo.kind({
  name: 'OB.UI.SearchProduct',
  style: 'margin: 5px; background-color: #ffffff; color: black; padding: 5px; height: 612px;',
  published: {
    receipt: null
  },
  handlers: {
    onSearchAction: 'searchAction',
    onClearAction: 'clearAction'
  },
  events: {
    onAddProduct: ''
  },
  executeOnShow: function () {
    var me = this;
    this.$.products.$.theader.$.searchProductHeader.loadCategories(function () {
      setTimeout(function () {
        me.$.products.$.theader.$.searchProductHeader.$.productname.focus();
      }, 400);
    });
  },
  components: [{
    classes: 'row-fluid',
    components: [{
      classes: 'span12',
      components: [{
        classes: 'row-fluid',
        style: 'border-bottom: 1px solid #cccccc;',
        components: [{
          classes: 'row-fluid',
          components: [{
            classes: 'span12',
            components: [{
              kind: 'OB.UI.ScrollableTable',
              name: 'products',
              scrollAreaMaxHeight: '482px',
              renderHeader: 'OB.UI.SearchProductHeader',
              renderEmpty: 'OB.UI.RenderEmpty',
              renderLine: 'OB.UI.RenderProduct'
            }]
          }]
        }]
      }]
    }]
  }],
  init: function () {
    var me = this;
    this.inherited(arguments);
    this.categories = new OB.Collection.ProductCategoryList();
    this.products = new OB.Collection.ProductList();

    //first the main collection of the component
    this.$.products.setCollection(this.products);
    this.$.products.getHeader().setHeaderCollection(this.categories);

    this.products.on('click', function (model) {
      this.doAddProduct({
        product: model,
        options: {
          blockAddProduct: true
        }
      });
    }, this);
  },
  receiptChanged: function () {
    this.receipt.on('clear', function () {
      this.$.products.$.theader.$.searchProductHeader.$.productname.setContent('');
      this.$.products.$.theader.$.searchProductHeader.$.productcategory.setContent('');
    }, this);
  },
  clearAction: function (inSender, inEvent) {
    this.products.reset();
  },
  searchAction: function (inSender, inEvent) {
    var me = this,
        whereClause = 'where ',
        params = [];

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
    }

    // Initializing combo of categories without filtering

    function successCallbackProducts(dataProducts) {
      if (me.destroyed) {
        return;
      }
      if (dataProducts && dataProducts.length > 0) {
        me.products.reset(dataProducts.models);
        me.products.trigger('reset');
      } else {
        OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_NoProductsFound'));
        me.products.reset();
      }
    }

    if (inEvent.productName) {
      inEvent.productName = '%' + inEvent.productName + '%';
      whereClause += '_filter like ?';
      params.push(inEvent.productName);
    } else {
      whereClause += '1 = 1';
    }

    if (inEvent.productCat && inEvent.productCat.indexOf('__all__') === -1) {
      whereClause = whereClause + ' and m_product_category_id ';
      if (inEvent.categoryTree) {
        whereClause = whereClause + ' in (' + inEvent.productCat + ')';
      } else {
        whereClause = whereClause + ' = ?';
        params.push(inEvent.productCat);
      }
    }
    OB.Dal.query(OB.Model.Product, 'select * from m_product ' + whereClause, params, successCallbackProducts, errorCallback);
  }
});