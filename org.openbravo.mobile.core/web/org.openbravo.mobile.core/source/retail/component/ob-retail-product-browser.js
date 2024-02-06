/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, _ */

enyo.kind({
  name: 'OB.UI.RenderProduct',
  kind: 'OB.UI.SelectButton',
  style: 'padding: 8px',
  components: [{
    style: 'float: left; width: 25%',
    components: [{
      kind: 'OB.UI.Thumbnail',
      name: 'thumbnail'
    }]
  }, {
    style: 'float: left; width: 55%;',
    components: [{
      name: 'identifier',
      style: 'padding-left: 5px;'
    }]
  }, {
    name: 'price',
    style: 'float: left; width: 20%; text-align: right; font-weight:bold;'
  }, {
    style: 'clear:both;'
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.identifier.setContent(this.model.get('_identifier'));
    this.$.price.setContent(OB.I18N.formatCurrency(this.model.get('standardPrice')));
    var image;
    if (OB.MobileApp.model.get('permissions')['OBPOS_retail.productImages']) {
      image = OB.UTIL.getImageURL(this.model.get('id'));
      this.$.thumbnail.setImgUrl(image);
    } else {
      image = this.model.get('img');
      this.$.thumbnail.setImg(image);
    }
  }
});

enyo.kind({
  name: 'OB.UI.RenderCategory',
  kind: 'OB.UI.SelectButton',
  components: [{
    style: 'float: left; width: 25%',
    components: [{
      kind: 'OB.UI.Thumbnail',
      name: 'thumbnail'
    }]
  }, {
    style: 'float: left; width: 75%;',
    components: [{
      name: 'identifier',
      style: 'padding-left: 5px;'
    }, {
      style: 'clear:both;'
    }]
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.addClass('btnselect-browse');
    this.$.identifier.setContent(this.model.get('_identifier'));
    this.$.thumbnail.setImg(this.model.get('img'));
  }
});

enyo.kind({
  name: 'OB.UI.ProductBrowser',
  useCharacteristics: true,
  classes: 'row-fluid',
  components: [{
    classes: 'span6',
    components: [{
      kind: 'OB.UI.BrowseProducts',
      name: 'browseProducts'
    }]
  }, {
    classes: 'span6',
    components: [{
      kind: 'OB.UI.BrowseCategories',
      name: 'browseCategories'
    }]
  }],
  init: function () {
    // no product catalog/remote in high volume
    if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
      return;
    }
    this.$.browseProducts.$.listProducts.useCharacteristics = this.useCharacteristics;
    this.$.browseCategories.$.listCategories.categories.on('selected', function (category) {
      this.$.browseProducts.$.listProducts.loadCategory(category);
    }, this);
  },
  executeOnShow: function () {
    this.$.browseCategories.$.listCategories.loadCategories();
  }
});

enyo.kind({
  name: 'OB.UI.BrowseCategories',
  style: 'height: 612px; margin: 5px;',
  components: [{
    style: 'background-color: #ffffff; color: black; padding: 5px',
    components: [{
      kind: 'OB.UI.ListCategories',
      name: 'listCategories'
    }]
  }]
});

enyo.kind({
  name: 'OB.UI.BrowseProducts',
  style: 'margin: 5px;',
  components: [{
    style: 'background-color: #ffffff; color: black; padding: 5px',
    components: [{
      kind: 'OB.UI.ListProducts',
      name: 'listProducts'
    }]
  }]
});

enyo.kind({
  kind: 'OB.UI.ScrollableTableHeader',
  name: 'OB.UI.CategoryListHeader',
  style: 'padding: 10px; border-bottom: 1px solid #cccccc;',
  components: [{
    style: 'line-height: 27px; font-size: 18px; font-weight: bold; height: 28px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;',
    name: 'title',
    content: ''
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.title.setContent(OB.I18N.getLabel('OBMOBC_LblCategories'));
  }
});

enyo.kind({
  name: 'OB.UI.CategoryLoadingRender',
  style: 'border-bottom: 1px solid #cccccc; padding: 20px; text-align: center; font-weight: bold; font-size: 30px; color: #cccccc',
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBPOS_CategoryLoading'));
  }
});

enyo.kind({
  name: 'OB.UI.ListCategories',
  published: {
    showBestSellers: true,
    showAllCategories: false,
    tableName: 'categoryTable'
  },
  handlers: {
    onCategoryExpandCollapse: 'categoryExpandCollapse'
  },
  initComponents: function () {
    this.inherited(arguments);
    this.createComponent({
      classes: 'product_category',
      name: this.tableName,
      scrollAreaMaxHeight: '574px',
      listStyle: 'list',
      kind: 'OB.UI.ScrollableTable',
      renderHeader: 'OB.UI.CategoryListHeader',
      renderEmpty: 'OB.UI.CategoryLoadingRender',
      renderLine: 'OB.UI.RenderCategory',
      columns: ['thumbnail', 'identifier'],
      owner: this
    });
  },
  init: function () {
    // no product catalog in high volume/remote
    if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true) && this.tableName === 'categoryTable') {
      return;
    }
    this.categories = new OB.Collection.ProductCategoryList();
    this.$[this.tableName].setCollection(this.categories);
  },

  loadCategories: function (callback) {
    var me = this;

    if (this.categories.length > 0) {
      if (callback) {
        callback();
      }
      return;
    }

    this.getCategoryTree('0', function (success, nodeCategory, tree) {
      if (success) {
        var showTree = _.some(tree.models, function (category) {
          return category.get('issummary');
        });
        if (showTree) {
          me.$[me.tableName].renderLine = 'OB.UI.RenderCategoryTree';
          if (me.showAllCategories) {
            tree.add(new OB.Model.ProductCategory({
              id: '__all__',
              display: true,
              issummary: false,
              level: 0,
              parentId: '0',
              treeNode: 'COLLAPSED',
              name: OB.I18N.getLabel('OBMOBC_SearchAllCategories'),
              _identifier: OB.I18N.getLabel('OBMOBC_SearchAllCategories')
            }), {
              at: 0
            });
          }
          _.each(me.$[me.tableName].$.tbody.children, function (tr) {
            tr.setShowing(tr.renderline.model.get('display'));
          });
        }
      }
      me.categories.reset(tree.models);
      if (callback) {
        callback();
      }
    });
  },

  loadCategoryTreeLevel: function (parentId, successCallback, errorCallback) {
    var select = "select tree.category_id, sum(case when pc.m_product_category_id is null then 0 else 1 end) as childs " // 
    + "from m_product_category_tree tree left join m_product_category_tree child on tree.category_id = child.parent_id " //
    + "left join m_product_category pc on pc.m_product_category_id = child.category_id " //
    + "where tree.parent_id = '" + parentId + "' group by tree.category_id order by tree.seqno";
    OB.Dal.query(OB.Model.ProductCategoryTreeQuery, select, [], successCallback, errorCallback);
  },

  loadCategoryLevel: function (parentId, callback) {

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
      if (callback) {
        callback(false);
      }
    }

    function successCallbackCategoryTree(categoryTree) {
      var inCategory = "";
      _.each(categoryTree.models, function (cat, index) {
        if (index !== 0) {
          inCategory += ", ";
        }
        inCategory += "'" + cat.get('categoryId') + "'";
      });
      var query = "select ";
      _.each(OB.Model.ProductCategory.propertyList, function (property, index) {
        if (index !== 0) {
          query += ", ";
        }
        query += "pc." + property.column + " as " + property.name;
      });
      query += " from m_product_category pc join m_product_category_tree pct on pc.m_product_category_id = pct.category_id";
      query += " where pc.m_product_category_id in (" + inCategory + ") order by pct.seqno";
      OB.Dal.query(OB.Model.ProductCategory, query, [], function (categories) {
        if (callback) {
          callback(true, categoryTree, categories);
        }
      }, errorCallback);
    }

    this.loadCategoryTreeLevel(parentId, successCallbackCategoryTree, errorCallback);
  },

  getCategoryTree: function (parentCategory, callback) {
    var nodeCategory = this.categories.get(parentCategory);
    if (nodeCategory && nodeCategory.get('loadedChilds')) {
      callback(true);
      return;
    }
    var me = this;
    this.loadCategoryLevel(parentCategory, function (success, categoryTree, categories) {
      var tree = new OB.Collection.ProductCategoryList();
      if (success) {
        var endProcess = function () {
            _.each(categoryTree.models, function (treeNode) {
              var category = categories.get(treeNode.get('categoryId'));
              if (category) {
                category.set('loadedChilds', false);
                category.set('issummary', treeNode.get('childs') > 0);
                category.set('level', nodeCategory ? nodeCategory.get('level') + 1 : 0);
                category.set('display', true, {
                  silent: true
                });
                category.set('parentId', parentCategory);
                category.set('treeNode', 'COLLAPSED');
                tree.add(category);
              }
            });
            if (nodeCategory) {
              nodeCategory.set('loadedChilds', true);
            }
            callback(success, nodeCategory, tree);
            };

        if (parentCategory === '0' && me.showBestSellers) {
          var virtualBestSellerCateg = new OB.Model.ProductCategory();
          virtualBestSellerCateg.createBestSellerCategory();
          categories.add(virtualBestSellerCateg, {
            at: 0
          });
          var virtualBestSellerCategTree = new OB.Model.ProductCategoryTree();
          virtualBestSellerCategTree.createBestSellerCategory();
          categoryTree.add(virtualBestSellerCategTree, {
            at: 0
          });
        }
        endProcess();
      } else {
        callback(success, nodeCategory, tree);
      }
    });
  },

  categoryUnselect: function () {
    var categoryTable = this.$[this.tableName];
    if (categoryTable.selected && categoryTable.selected.renderline && categoryTable.selected.renderline.model) {
      categoryTable.selected.addRemoveClass('selected', false);
      categoryTable.selected = null;
    }
  },

  categoryExpandSelected: function () {
    var categoryTable = this.$[this.tableName];
    if (categoryTable.selected && categoryTable.selected.renderline && categoryTable.selected.renderline.model) {
      var summaryCategory = [],
          parentId = categoryTable.selected.renderline.model.get('parentId');
      while (parentId !== '0') {
        var parent = this.categories.get(parentId);
        if (parent) {
          summaryCategory.push(parent);
          parentId = parent.get('parentId');
        }
      }
      _.each(summaryCategory, function (summary) {
        this.categoryExpandCollapse(this, {
          categoryId: summary.id,
          expand: true
        });
      }, this);
      var me = this;
      setTimeout(function () {
        if (categoryTable.selected.$.renderCategoryTree) {
          categoryTable.selected.$.renderCategoryTree.focus();
        }
      }, 200);
    }
  },

  categoryGetChildren: function (categoryId) {
    var i, j, level = -1,
        children = this.$[this.tableName].$.tbody.children,
        childrenList = [];
    for (i = 0; i < children.length; i++) {
      if (children[i].renderline.model.get('id') === categoryId) {
        level = children[i].renderline.model.get('level');
        childrenList.push(children[i].renderline.model);
        for (j = i + 1; j < children.length; j++) {
          if (level >= children[j].renderline.model.get('level')) {
            break;
          }
          childrenList.push(children[j].renderline.model);
        }
      }
      if (level >= 0) {
        break;
      }
    }
    return childrenList;
  },

  categoryGetControlKey: function (item) {
    return _.find(_.keys(item.$), function (key) {
      return key.indexOf('renderCategory') === 0;
    });
  },

  categoryExpandCollapse: function (inSender, inEvent) {
    var i, category, callGetCategoryTree = false,
        startScrollTop = this.$[this.tableName].$.scrollArea.getScrollTop(),
        children = this.$[this.tableName].$.tbody.children;
    // Do not use "_.each" for performance
    for (i = 0; i < children.length; i++) {
      category = children[i].renderline.model;
      if (category.get('id') === inEvent.categoryId) {
        if (inEvent.expand) {
          this.categoryCollapseSibling(category.get('parentId'));
          category.set('treeNode', 'EXPANDED', {
            silent: true
          });
          if (category.get('loadedChilds')) {
            this.categoryExpandChildren(category.get('id'));
            this.categoryChangeButton(children[i], inEvent.expand);
          } else {
            callGetCategoryTree = true;
          }
        } else {
          this.categoryCollapseChildren(category.get('id'));
          category.set('treeNode', 'COLLAPSED', {
            silent: true
          });
          this.categoryChangeButton(children[i], inEvent.expand);
        }
        break;
      }
    }
    if (callGetCategoryTree) {
      var me = this;
      this.getCategoryTree(category.get('id'), function (success, nodeCategory, tree) {
        if (success) {
          category.set('loadedChilds', true, {
            silent: true
          });
          var index = me.categories.indexOf(nodeCategory);
          if (index >= 0) {
            if (tree.length > 0) {
              me.owner.owner.startShowing = true;
              me.categories.models[0].tempDataLimit = -1;
              var newTree = me.categories.models;
              newTree.splice.apply(newTree, [(index + 1), 0].concat(tree.models));
              me.categories.reset(newTree);
              me.owner.owner.startShowing = false;
            }
            me.categoryAdjustScroll(inEvent.categoryId, startScrollTop, me.$[me.tableName].$.scrollArea.getScrollTop());
          }
          me.categoryChangeButton(children[i], inEvent.expand);
        }
      });
    } else {
      this.categoryAdjustScroll(inEvent.categoryId, startScrollTop, this.$[this.tableName].$.scrollArea.getScrollTop());
    }
  },

  categoryAdjustScroll: function (categoryId, startScrollTop, endScrollTop) {
    var scrollTop = 0;
    if (endScrollTop + 1 < startScrollTop) {
      var i, children = this.$[this.tableName].$.tbody.children;
      // Do not use "_.each" for performance
      for (i = 0; i < children.length; i++) {
        var category = children[i].renderline.model;
        if (category.get('id') === categoryId) {
          break;
        }
        if (category.get('display')) {
          scrollTop += 40;
        }
      }
    } else {
      scrollTop = startScrollTop;
    }
    this.$[this.tableName].$.scrollArea.scrollTo(0, scrollTop);
  },

  categoryChangeButton: function (children, expand) {
    var renderCategoryKey = this.categoryGetControlKey(children);
    children.$[renderCategoryKey].$.collapse.setShowing(expand);
    children.$[renderCategoryKey].$.expand.setShowing(!expand);
  },

  categoryCollapseSibling: function (parentId) {
    _.each(this.$[this.tableName].$.tbody.children, function (item) {
      if (item.renderline.model.get('parentId') === parentId) {
        if (item.renderline.model.get('treeNode') !== 'COLLAPSED') {
          item.renderline.model.set('treeNode', 'COLLAPSED', {
            silent: true
          });
          var renderCategoryKey = this.categoryGetControlKey(item);
          item.$[renderCategoryKey].$.collapse.setShowing(false);
          item.$[renderCategoryKey].$.expand.setShowing(true);
          this.categoryCollapseChildren(item.renderline.model.get('id'));
        }
      }
    }, this);
  },

  categoryCollapseChildren: function (parentId) {
    _.each(this.$[this.tableName].$.tbody.children, function (item) {
      if (item.renderline.model.get('parentId') === parentId) {
        item.setShowing(false);
        item.renderline.model.set('display', false, {
          silent: true
        });
        if (item.renderline.model.get('treeNode') !== 'COLLAPSED') {
          this.categoryCollapseChildren(item.renderline.model.get('id'));
        }
      }
    }, this);
  },

  categoryExpandChildren: function (parentId) {
    _.each(this.$[this.tableName].$.tbody.children, function (item) {
      if (item.renderline.model.get('parentId') === parentId) {
        item.setShowing(true);
        item.renderline.model.set('display', true, {
          silent: true
        });
        if (item.renderline.model.get('treeNode') === 'EXPANDED') {
          this.categoryExpandChildren(item.renderline.model.get('id'));
        }
      }
    }, this);
  }

});

//This header is set dynamically
//use scrollableTableHeaderChanged_handler method of scrollableTable to manage changes
//me.$.productTable.setHeaderText(category.get('_identifier'));
enyo.kind({
  kind: 'OB.UI.ScrollableTableHeader',
  name: 'OB.UI.ProductListHeader',
  style: 'padding: 10px; border-bottom: 1px solid #cccccc;',
  components: [{
    style: 'line-height: 27px; font-size: 18px; font-weight: bold; height: 28px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;',
    name: 'title'
  }],
  setHeader: function (valueToSet) {
    this.$.title.setContent(valueToSet);
  },
  getValue: function () {
    return this.$.title.getContent();
  }
});

enyo.kind({
  name: 'OB.UI.ListProducts',
  events: {
    onAddProduct: '',
    onShowLeftSubWindow: '',
    onTabChange: ''
  },
  handlers: {
    onChangePricelist: 'changePricelist'
  },
  components: [{
    kind: 'OB.UI.ScrollableTable',
    name: 'productTable',
    scrollAreaMaxHeight: '574px',
    renderHeader: 'OB.UI.ProductListHeader',
    renderEmpty: 'OB.UI.RenderEmpty',
    renderLine: 'OB.UI.RenderProduct',
    columns: ['thumbnail', 'identifier', 'price', 'generic']
  }],
  init: function () {
    if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
      return;
    }
    var me = this;
    this.inherited(arguments);
    this.products = new OB.Collection.ProductList();
    this.$.productTable.setCollection(this.products);
    this.products.on('click', function (model) {
      if (this.useCharacteristics) {
        if (!model.get('isGeneric')) {
          me.doAddProduct({
            product: model,
            options: {
              blockAddProduct: true
            }
          });
        } else {
          OB.UI.SearchProductCharacteristic.prototype.filtersCustomClear();
          me.doTabChange({
            tabPanel: 'searchCharacteristic',
            keyboard: false,
            edit: false,
            options: {
              model: model
            }
          });
        }
      } else {
        me.doAddProduct({
          product: model,
          options: {
            blockAddProduct: true
          }
        });
      }
    }, this);
  },

  changePricelist: function (inSender, inEvent) {
    this.currentPriceList = inEvent.priceList;
    this.loadCategory(this.currentCategory);
  },

  loadCategory: function (category) {
    if (OB.MobileApp.model.hasPermission('OBPOS_remote.product', true)) {
      return;
    }
    var criteria, me = this;

    function errorCallback(tx, error) {
      OB.UTIL.showError("OBDAL error: " + error);
    }

    function successCallbackProducts(dataProducts) {
      var filteredDataProducts;
      if (me.destroyed) {
        return;
      }
      if (dataProducts && dataProducts.length > 0) {
        filteredDataProducts = new OB.Collection.ProductList(dataProducts.filter(function (model) {
          return model.get('productType') !== 'S' || !model.get('isLinkedToProduct');
        }));
        me.products.reset(filteredDataProducts.models);
      } else {
        me.products.reset();
      }
      //      TODO
      me.$.productTable.getHeader().setHeader(category.get('_identifier'));
    }

    if (category) {
      this.currentCategory = category;
      var where = "where ";
      if (category.get('id') === 'OBPOS_bestsellercategory') {
        criteria = {
          'bestseller': 'true'
        };
        where += "p.bestseller = ?";
        if (this.useCharacteristics) {
          criteria.generic_product_id = null;
        }
      } else {
        criteria = {
          'productCategory': category.get('id')
        };
        where += "p.m_product_category_id = ?";
        if (this.useCharacteristics) {
          criteria.generic_product_id = null;
        }
      }
      criteria._orderByClause = 'upper(_identifier) asc';
      if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
        criteria._limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
      }
      if (OB.MobileApp.model.hasPermission('EnableMultiPriceList', true) && this.currentPriceList && this.currentPriceList !== OB.MobileApp.model.get('terminal').priceList) {
        var select = "select p.*, pp.pricestd as currentStandardPrice " //
        + "from m_product p inner join m_productprice pp on p.m_product_id = pp.m_product_id and pp.m_pricelist_id = ? " //
        + where;
        var limit = null;
        if (OB.MobileApp.model.hasPermission('OBPOS_productLimit', true)) {
          limit = OB.DEC.abs(OB.MobileApp.model.hasPermission('OBPOS_productLimit', true));
        }
        OB.Dal.query(OB.Model.Product, select, [this.currentPriceList, category.get('id') === 'OBPOS_bestsellercategory' ? 'true' : category.get('id')], successCallbackProducts, errorCallback, null, null, limit);
      } else {
        OB.Dal.find(OB.Model.Product, criteria, successCallbackProducts, errorCallback);
      }
    } else {
      this.products.reset();
      this.$.productTable.getHeader().setHeader(OB.I18N.getLabel('OBMOBC_LblNoCategory'));
    }
  }
});