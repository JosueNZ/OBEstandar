/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB */

(function () {

  var Message = OB.Data.ExtensibleModel.extend({
    modelName: 'Message',
    tableName: 'message',
    entityName: 'Message',
    source: '',
    local: true
  });

  Message.addProperties([{
    name: 'id',
    column: 'message_id',
    primaryKey: true,
    type: 'TEXT'
  }, {
    name: 'service',
    column: 'service',
    type: 'TEXT'
  }, {
    name: 'url',
    column: 'url',
    type: 'TEXT'
  }, {
    name: 'time',
    column: 'time',
    type: 'NUMERIC'
  }, {
    name: 'status',
    column: 'status',
    type: 'TEXT'
  }, {
    name: 'messageObj',
    column: 'messageObj',
    type: 'TEXT'
  }]);


  OB.Data.Registry.registerModel(Message);

}());