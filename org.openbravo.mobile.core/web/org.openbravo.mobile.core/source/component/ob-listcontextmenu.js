/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, _ */

enyo.kind({
  name: 'OB.UI.ListContextMenuItem',
  kind: 'onyx.MenuItem',
  selectItem: function (model) {

  }
});

enyo.kind({
  name: 'OB.UI.ListButtonContextMenu',
  classes: 'listcontextmenu-icon',
  active: true,
  tap: function () {
    this.bubble("onActivate");
    return true;
  },
  setActive: function (act) {
    this.active = true;
  }
});

enyo.kind({
  name: 'OB.UI.ListContextDynamicMenu',
  kind: 'onyx.Menu',
  classes: 'dropdown',
  maxHeight: 600,
  scrolling: false,
  floating: true,
  itemsCount: 0,
  adjustPosition: function () {
    this.inherited(arguments);
    if (this.owner.rightAlign && this.node) {
      var activator = this.activatorOffset,
          menu = this.node.getBoundingClientRect(),
          left = activator.left + activator.width - menu.width;
      this.applyPosition({
        left: left < 0 ? 0 : left
      });
    }
  },
  setItems: function (menuItems) {
    // If you want to remove old items, you'll need to get the child
    // components and remove everything but the scroller
    var allowedItems = [];
    _.each(menuItems, function (item) {
      if (!item.permission || OB.MobileApp.model.hasPermission(item.permission, true)) {
        allowedItems.push(item);
      }
    }, this);
    this.itemsCount = allowedItems.length;
    this.createComponents(allowedItems, {
      owner: this
    });
    this.render();
  }
});

enyo.kind({
  name: 'OB.UI.ListContextMenu',
  rightAlign: true,
  handlers: {
    onSelect: 'itemSelected',
    onShow: 'addMenuToList',
    onHide: 'removeMenuFromList'

  },
  published: {
    model: null
  },
  components: [{
    kind: 'onyx.MenuDecorator',
    name: 'btnListContextMenu',
    components: [{
      kind: 'OB.UI.ListButtonContextMenu',
      name: 'listButton'
    }, {
      kind: 'OB.UI.ListContextDynamicMenu',
      name: 'menu'
    }]
  }],
  itemSelected: function (sender, event) {
    this.owner.contextMenuItemSelected = true;
    event.originator.selectItem(this.model);
    return true;
  },
  addMenuToList: function () {
    if (OB.UTIL.isNullOrUndefined(OB.MobileApp.view.activeMenus)) {
      OB.MobileApp.view.activeMenus = [];
    }
    OB.MobileApp.view.activeMenus.push(this.$.menu);
  },
  removeMenuFromList: function () {
    var index;
    index = OB.MobileApp.view.activeMenus.indexOf(this.$.menu);
    if (index !== -1) {
      OB.MobileApp.view.activeMenus.splice(index, 1);
    }
  }
});