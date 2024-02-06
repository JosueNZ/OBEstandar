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
  name: 'OB.Modals.ModalModelsToSynch',
  myId: 'ModalModelsToSynch',
  kind: 'OB.UI.Modal',
  i18nHeader: 'OBMOBC_ModelsToSynchronize',
  body: {
    kind: 'OB.Modals.ModalModelsToSynch.Components.ListModelsToSynch',
    name: 'modelsToSynchList'
  },
  executeOnHide: function () {
    this.$.body.$.modelsToSynchList.setModelsToSynchValues(null);
  },
  isFirstTime: true,
  executeOnShow: function () {
    var me = this,
        modelsToSync = new Backbone.Collection(),
        modelsByType;
    me.$.body.$.modelsToSynchList.setModelsToSynchValues(modelsToSync);
    var callback;
    callback = function (model, dataToSync) {
      var isAdding = false;
      modelsToSync.reset();
      _.each(OB.MobileApp.model.get('dataSyncModels'), function (modelObj) {
        if (!modelObj.skipShowSynchInformation) {
          modelsByType = _.filter(dataToSync.models, function (obj) {
            return obj.get('url').replace('../../', '').split('/')[1] === modelObj.className;
          });
          if (modelsByType.length > 0) {
            var object = new Backbone.Model();
            object.set('name', modelObj.model.prototype.modelName);
            _.each(modelsByType, function (mdl) {
              mdl.set('model', modelObj);
            });
            object.set('items', new Backbone.Collection(modelsByType));
            object.set('quantity', modelsByType.length);
            object.syncModel = modelObj.model;
            modelsToSync.add(object);
            isAdding = true;
          }
        }
      });
    };
    if (this.isFirstTime) {
      this.isFirstTime = false;
      OB.MobileApp.model.on('eachModelsSynchronized', function () {
        OB.MobileApp.model.syncModelHasData(OB.Model.Message, callback);
      });
    }
    OB.MobileApp.model.syncModelHasData(OB.Model.Message, callback);
  }
});

enyo.kind({
  name: 'OB.Modals.ModalModelsToSynch.Components.ListModelsToSynch',
  classes: 'row-fluid',
  published: {
    modelsToSynchValues: null
  },
  components: [{
    classes: 'span12',
    components: [{
      style: 'border-bottom: 1px solid #cccccc;'
    }, {
      components: [{
        name: 'scrollListModelsToSynch',
        kind: 'OB.UI.ScrollableTable',
        scrollAreaMaxHeight: '400px',
        renderLine: 'OB.Modals.ModalModelsToSynch.Components.ModelsToSynchLine',
        renderEmpty: 'OB.UI.RenderEmpty'
      }]
    }]
  }],
  modelsToSynchValuesChanged: function (value) {
    this.$.scrollListModelsToSynch.setCollection(this.modelsToSynchValues);
  }
});

enyo.kind({
  name: 'OB.Modals.ModalModelsToSynch.Components.ModelsToSynchLine',
  kind: 'OB.UI.SelectButton',
  events: {
    onHideThisPopup: ''
  },
  components: [{
    name: 'line',
    style: 'line-height: 33px; width: 100%',
    components: [{
      style: 'float: left; font-weight: bold; margin-right: 5px',
      name: 'model'
    }, {
      style: 'float: left;',
      name: 'quantity'
    }]
  }],
  create: function () {
    this.inherited(arguments);
    this.$.model.setContent(this.model.get('name') + ': ');
    this.$.quantity.setContent(this.model.get('quantity') + ' ' + OB.I18N.getLabel('OBMOBC_ItemsToSynchronize'));
  },
  tap: function () {
    this.doHideThisPopup();
    OB.MobileApp.view.$.modalItemsToSynch.show({
      itemsToSynchObj: this.model
    });
  }
});