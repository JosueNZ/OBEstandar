/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, _ */

enyo.kind({
  name: 'OB.Modals.ModalItemsToSynch',
  myId: 'ModalModelsToSynch',
  kind: 'OB.UI.Modal',
  body: {
    kind: 'OB.Modals.ModalItemsToSynch.Components.ListItemsToSynch',
    name: 'itemsToSynchList'
  },
  executeOnHide: function () {
    this.$.body.$.itemsToSynchList.setItemsToSynchValues(null);
    OB.MobileApp.view.$.modalModelsToSynch.show();
  },
  executeOnShow: function () {
    var itemsToSync = new Backbone.Collection();
    OB.MobileApp.model.on('eachModelsSynchronized', function () {
      OB.MobileApp.model.syncModelHasData(OB.Model.Message, function (model, dataToSync) {
        var items;
        if (itemsToSync.length > 0) {
          items = _.filter(dataToSync.models, function (obj) {
            return obj.get('url').replace('../../', '').split('/')[1] === itemsToSync.models[0].get('model').className;
          });
          _.each(items, function (itm) {
            itm.set('model', itemsToSync.models[0].get('model'));
          });
          itemsToSync.reset(items);
        }
      }, function (model, dataToSync) {
        itemsToSync.reset();
      });
    });
    itemsToSync = this.args.itemsToSynchObj.get('items');
    this.$.body.$.itemsToSynchList.setItemsToSynchValues(itemsToSync);
    this.$.header.setContent(this.args.itemsToSynchObj.get('name') + ': ' + OB.I18N.getLabel('OBMOBC_PendingToSynchronize'));
  }
});

enyo.kind({
  name: 'OB.Modals.ModalItemsToSynch.Components.ListItemsToSynch',
  classes: 'row-fluid',
  published: {
    itemsToSynchValues: null
  },
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;'
    }, {
      components: [{
        name: 'scrollListItemsToSynch',
        kind: 'OB.UI.ScrollableTable',
        scrollAreaMaxHeight: '400px',
        renderLine: 'OB.Modals.ModalItemsToSynch.Components.ItemsToSynchLine',
        renderEmpty: 'OB.UI.RenderEmpty'
      }]
    }]
  }],
  itemsToSynchValuesChanged: function (value) {
    this.$.scrollListItemsToSynch.setCollection(this.itemsToSynchValues);
  }
});

enyo.kind({
  name: 'OB.Modals.ModalItemsToSynch.Components.ItemsToSynchLine',
  classes: 'btnselect',
  style: 'height: 33px;',
  components: [{
    name: 'line',
    style: 'line-height: 33px; width: 100%',
    components: [{
      style: 'float: left; font-weight: bold; margin-right: 5px',
      name: 'model'
    }]
  }],
  create: function () {
    this.inherited(arguments);
    if (this.model.get('model').getIdentifier) {
      this.$.model.setContent(this.model.get('model').getIdentifier(JSON.parse(this.model.get('messageObj')).data[0]));
    } else {
      this.$.model.setContent(JSON.parse(this.model.get('messageObj')).data[0].id);
    }
  }
});