/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _, console, Backbone, enyo, localStorage, Promise */

OB.Dal = OB.Dal || {};

(function () {
  OB.Dal.DATALIMIT = 300;
  OB.Dal.REMOTE_DATALIMIT = 100;
  OB.Dal.EQ = '=';
  OB.Dal.NEQ = '!=';
  OB.Dal.CONTAINS = 'contains';
  OB.Dal.USESCONTAINS = '_usesContains';
  OB.Dal.STARTSWITH = 'startsWith';
  OB.Dal.ENDSWITH = 'endsWith';
  OB.Dal.FILTER = 'filter';
  OB.Dal.stackSize = 0;

  function executeSqlErrorHandler(logLevel, header, objectInvolved, txError, e, caller) {
    // arguments check
    OB.UTIL.Debug.execute(function () {
      if (!logLevel) {
        throw "executeSqlErrorHandler: missing logLevel";
      }
      if (!header) {
        throw "executeSqlErrorHandler: missing header";
      }
      if (!objectInvolved) {
        throw "executeSqlErrorHandler: missing objectInvolved";
      }
      if (!e) {
        throw "executeSqlErrorHandler: missing e";
      }
    });
    // show an error
    var link = OB.UTIL.getStackLink(3);

    // LogClient can easily spam the console
    if (objectInvolved === 'obmobc_logclient') {
      return;
    }
    var header2 = header + " '" + objectInvolved + "'";
    // log depending on the logLevel
    if (logLevel === 'warn') {
      console.warn(OB.UTIL.argumentsToStringifyed(header2, e, link));
      return;
    }
    if (caller) {
      console.error(OB.UTIL.argumentsToStringifyed(header2, e, caller));
    } else {
      console.error(OB.UTIL.argumentsToStringifyed(header2, e, link));
    }
  }

  function getCallerInfo() {
    var message = "";
    // Activate this lines to see the caller function
    // try {
    //   message = "\n" + arguments.callee.caller.caller;
    // } catch (e) {
    //   console.error("getCallerInfo.caller.caller: " + e);
    // }
    var stackTrace = OB.UTIL.getStackTrace('getCallerInfo', false);
    message += "\n" + stackTrace;
    return message;
  }

  function silentFunction(f) {
    return function () {
      if (_.isFunction(f)) {
        try {
          f(arguments);
        } catch (e) {
          OB.error('OB.Dal: a success callback threw an exception', f, "\n" + e.stack);
        }
      }
    };
  }

  /*
   * initialize the WebSQL dababase
   */
  OB.Dal.openWebSQL = function () {
    if (!window.openDatabase) {
      return; // Supported browsers check will show browsers supported information.
    }

    // do not initialize the db if it was already initialized
    if (OB.Data.localDB && OB.Data.localDB.version) {
      return OB.Data.localDB;
    }

    var dbInfo = OB.MobileApp.model.get('localDB');

    // arguments check
    if (!dbInfo) {
      OB.UTIL.Debug.execute(function () {
        throw "The database version information must be available before 'OB.Dal.openWebSQL' is called";
      });
      return;
    }

    console.info("OB.Dal.openWebSQL: initializing WebSQL");
    if (OB.I18N.labels) {
      OB.UTIL.showLoadingMessage(OB.I18N.getLabel('OBMOBC_InitializingDatabase'));
    }

    var undef;
    var wsql = window.openDatabase !== undef;
    var db;

    if (wsql === false) {
      // Support should get this error. Show it in productionn
      console.error("WebSQL error: Unable find the database engine");
    }

    try {
      db = (wsql && window.openDatabase(dbInfo.name, '', dbInfo.displayName, dbInfo.size));
    } catch (e) {
      // if the database could not be created, logging with OB.error is not available
      // Support should get this error. Show it in production
      console.error("Web SQL error: " + e);
      OB.UTIL.Debug.execute(function (e) {
        throw "Web SQL error: " + e;
      });
      return;
    }
    if (!db) {
      // if the database could not be created, logging with OB.error is not available
      // Support should get this error. Show it in production
      console.error("Web SQL error");
      OB.UTIL.Debug.execute(function () {
        throw "Web SQL error";
      });
      return;
    }

    OB.Data.localDB = db;

    return db;
  };

  /**
   * TODO: localStorage
   * This is a function to centralize TODO Dal code related to localstorage
   * As of changes in the issue 27166, this flow is now executed
   */
  OB.Dal.missingLocalStorageLogic = function () {
    OB.UTIL.Debug.execute(function () {
      throw "Not Implemented";
    });
  };

  // deprecated, use OB.UTIL.get_UUID()
  OB.Dal.get_uuid = function () {
    OB.UTIL.VersionManagement.deprecated(30768, function () {});
    return OB.UTIL.get_UUID();
  };

  OB.Dal.transform = function (model, obj) {
    var tmp = {},
        modelProto = model.prototype,
        val, properties;
    properties = model.getProperties ? model.getProperties() : modelProto.properties;
    _.each(properties, function (property) {
      var prop;
      if (_.isString(property)) {
        prop = property;
        val = !_.isUndefined(obj[modelProto.propertyMap[property]]) ? obj[modelProto.propertyMap[property]] : obj[property];

      } else {
        prop = property.name;
        val = !_.isUndefined(obj[property.column]) ? obj[property.column] : obj[property.name];
      }
      if (val === 'false') {
        tmp[prop] = false;
      } else if (val === 'true') {
        tmp[prop] = true;
      } else {
        tmp[prop] = val;
      }
    });
    return new model(tmp);
  };

  OB.Dal.getWhereClause = function (criteria, propertyMap) {
    var appendWhere = true,
        firstParam = true,
        sql = '',
        params = [],
        res = {},
        orAnd = ' AND ';
    if (criteria && !_.isEmpty(criteria)) {
      if (criteria.obdalcriteriaType) {
        orAnd = ' ' + criteria.obdalcriteriaType + ' ';
        delete criteria.obdalcriteriaType;
      }
      _.each(_.keys(criteria), function (k) {

        var undef, colName, val = criteria[k],
            operator = (val !== undef && val !== null && val.operator !== undef) ? val.operator : '=',
            value = (val !== undef && val !== null && val.value !== undef) ? val.value : val;
        if (k !== '_orderByClause' && k !== '_orderBy' && k !== '_limit') {
          if (appendWhere) {
            sql = sql + ' WHERE ';
            params = [];
            appendWhere = false;
          }

          if (_.isArray(propertyMap)) {
            if (k === '_filter') {
              colName = '_filter';
            } else {
              colName = _.find(propertyMap, function (p) {
                return k === p.name;
              }).column;
            }
          } else {
            colName = propertyMap[k];
          }

          sql = sql + (firstParam ? '' : orAnd) + ' ' + colName + ' ';

          if (value === null) {
            sql = sql + ' IS null ';
          } else {

            if (operator === OB.Dal.EQ) {
              sql = sql + ' = ? ';
            } else if (operator === OB.Dal.NEQ) {
              sql = sql + ' != ? ';
            } else {
              sql = sql + ' like ? ';
            }

            if (operator === OB.Dal.CONTAINS) {
              value = '%' + value + '%';
            } else if (operator === OB.Dal.STARTSWITH) {
              value = value + '%';
            } else if (operator === OB.Dal.ENDSWITH) {
              value = value + '%';
            }
            params.push(value);
          }

          if (firstParam) {
            firstParam = false;
          }

        }

      });
    }
    res.sql = sql;
    res.params = params;
    return res;
  };

  OB.Dal.getTableName = function (model) {
    if (model) {
      if (model.getTableName) {
        return model.getTableName();
      }
      if (model.prototype && model.prototype.tableName) {
        return model.prototype.tableName;
      }
    }
    OB.UTIL.Debug.execute(function () {
      throw "OB.Dal.getTableName: the model has not been initialized";
    });
    return null;
  };

  OB.Dal.getPropertyMap = function (model) {
    if (model) {
      if (model.getProperties) {
        return model.getProperties();
      }
      if (model.prototype && model.prototype.propertyMap) {
        return model.prototype.propertyMap;
      }
    }
    OB.UTIL.Debug.execute(function () {
      throw "OB.Dal.getPropertyMap: the model has not been initialized";
    });
    return null;
  };

  OB.Dal.transaction = function (callback, errorCallback, successCallback) {
    OB.Data.localDB.transaction(callback, errorCallback, successCallback);
  };

  OB.Dal.findUsingCache = function (cacheName, model, whereClause, success, error, args) {
    if (OB.Cache.hasItem(cacheName, whereClause, args ? args.modelsAffectedByCache : null)) {
      OB.Dal.stackSize++;
      if (OB.Dal.stackSize % 1000 === 0) {
        setTimeout(function () {
          success(OB.Cache.getItem(cacheName, whereClause, args ? args.modelsAffectedByCache : null));
        }, 0);
      } else {
        success(OB.Cache.getItem(cacheName, whereClause, args ? args.modelsAffectedByCache : null));
      }
    } else {
      OB.Dal.find(model, whereClause, function (records) {
        OB.Cache.putItem(cacheName, whereClause, records, args ? args.modelsAffectedByCache : null);
        success(records);
      }, error, args);
    }

  };

  OB.Dal.findInTransaction = function (tx, model, whereClause, success, error, args) {
    OB.Dal.find(model, whereClause, success, error, args, tx);
  };

  OB.Dal.find = function (model, whereClause, success, error, args, currentTransaction) {
    var callerInfo = getCallerInfo();
    var params = null,
        undef, colType, xhr, rr, i, criteria, j, params_where, orderBy, limit;
    if (OB.UTIL.isNullOrUndefined(model.prototype)) {
      OB.error('Model name without prototype: ' + model.getTableName());
    }
    if (model.prototype.online) {
      colType = OB && OB.Collection && OB.Collection[model.prototype.modelName + 'List'];
      if (undef === colType) {
        console.warn("OB.Dal.find: there is no collection defined at: OB.Data.Collection." + model.prototype.modelName + "List");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.find: there is no collection defined at: OB.Data.Collection." + model.prototype.modelName + "List";
        });
      }

      params = enyo.clone(whereClause);

      if (whereClause && _.isNumber(whereClause._limit)) {
        limit = whereClause._limit;
      } else {
        limit = 100;
        OB.trace('OB.Dal.find used without specific limit. Automatically set to 100.', model, whereClause);
      }
      params._noCount = true;
      params._operationType = 'fetch';
      params._startRow = (whereClause && whereClause._offset ? whereClause._offset : 0);
      params._endRow = params._startRow + limit;
      params._sortBy = (whereClause && whereClause._sortBy ? whereClause._sortBy : '');
      params.isc_dataFormat = 'json';
      params.isc_metaDataPrefix = '_';

      if (whereClause && whereClause._constructor) {
        for (i in whereClause) {
          if (whereClause.hasOwnProperty(i)) {
            if (i === 'criteria') {
              params.criteria = [];
              criteria = whereClause[i];
              for (j = 0; j < criteria.length; j++) {
                params.criteria.push(JSON.stringify(criteria[j]));
              }
            } else {
              params[i] = whereClause[i];
            }
          }
        }
      } else {
        params_where = (whereClause && whereClause._where ? whereClause._where : '');
      }

      xhr = new enyo.Ajax({
        url: model.prototype.source,
        method: 'POST',
        data: params,
        success: function (inSender, inResponse) {
          //FIXME: implement error handling
          success(new colType(inResponse.response.data), args);
        }
      });
      rr = new OB.RR.Request({
        ajaxRequest: xhr
      });
      rr.exec(xhr.url);

    } else if ((whereClause !== null && !_.isUndefined(whereClause.forceRemote) && whereClause.forceRemote) || (model.prototype.remote && OB.MobileApp.model.hasPermission(model.prototype.remote, true))) {
      var process = new OB.DS.Process(model.prototype.source);
      var result = new Backbone.Collection();
      var currentDate = new Date();
      if (_.isNull(params)) {
        params = {};
      }
      params.terminalTime = currentDate;
      params.terminalTimeOffset = currentDate.getTimezoneOffset();
      var p, l;
      var data = {};

      if (params) {
        p = {};
        for (l in params) {
          if (params.hasOwnProperty(l)) {
            if (typeof params[l] === 'string') {
              p[l] = {
                value: params[l],
                type: 'string'
              };
            } else if (typeof params[l] === 'number') {
              if (params[l] === Math.round(params[l])) {
                p[l] = {
                  value: params[l],
                  type: 'long'
                };
              } else {
                p[l] = {
                  value: params[l],
                  type: 'bigdecimal'
                };
              }
            } else if (typeof params[l] === 'boolean') {
              p[l] = {
                value: params[l],
                type: 'boolean'
              };
            } else {
              p[l] = params[l];
            }
          }
        }
        data.parameters = p;
      }

      if (_.isUndefined(data.parameters)) {
        data.parameters = {};
      }
      data.parameters.forceRemote = whereClause && whereClause.forceRemote;
      data.parameters.remoteModel = true;

      //replace _filter column with all columns marked as filterable
      if (whereClause && whereClause.remoteFilters) {
        var filter = _.find(whereClause.remoteFilters, function (filter) {
          return filter.columns[0] === '_filter';
        });
        if (filter) {
          filter.columns = model.getFilterablePropertyNames();
        }
        var startsWithFilter;
        if (model.prototype.remote && OB.MobileApp.model.hasPermission(model.prototype.remote, true)) {
          if (OB.MobileApp.model.hasPermission(model.prototype.remote + OB.Dal.USESCONTAINS, true)) {
            startsWithFilter = _.filter(whereClause.remoteFilters, function (filter) {
              return filter.operator === OB.Dal.STARTSWITH;
            });
            if (startsWithFilter.length > 0) {
              _.each(startsWithFilter, function (fil) {
                fil.operator = OB.Dal.CONTAINS;
              });
            }
          }
        } else {
          startsWithFilter = _.filter(whereClause.remoteFilters, function (filter) {
            return filter.operator === OB.Dal.STARTSWITH;
          });
          if (startsWithFilter.length > 0) {
            _.each(startsWithFilter, function (fil) {
              fil.operator = OB.Dal.CONTAINS;
            });
          }
        }
      }

      data.remoteFilters = whereClause ? whereClause.remoteFilters : null;
      data.orderByClause = whereClause && whereClause._orderByClause ? whereClause._orderByClause : null;
      data.orderByProperties = whereClause && whereClause._orderByProperties ? whereClause._orderByProperties : null;
      if (whereClause && whereClause.remoteParams) {
        data.remoteParams = whereClause ? whereClause.remoteParams : null;
      }
      if (whereClause && _.isNumber(whereClause._limit)) {
        //set in the model temporary the limit set by the query 
        model.prototype.tempDataLimit = whereClause._limit;
        // set limit + 1 to see if there are more items to retrieve and in that case show a message to narrow the query in order to less items
        data._limit = whereClause._limit + 1;
      } else if (model.prototype.remoteDataLimit) {
        // set limit + 1 to see if there are more items to retrieve and in that case show a message to narrow the query in order to less items
        data._limit = model.prototype.remoteDataLimit + 1;
        model.prototype.tempDataLimit = undefined;
      } else if (model.prototype.dataLimit) {
        // set limit + 1 to see if there are more items to retrieve and in that case show a message to narrow the query in order to less items
        data._limit = model.prototype.dataLimit + 1;
        model.prototype.tempDataLimit = undefined;
      }
      process.exec(data, function (data) {
        if (data && data.exception) {
          if (error) {
            error(null, data.exception);
          }
        } else {
          if (success) {
            for (i = 0; i < data.length; i++) {
              result.add(OB.Dal.transform(model, data[i]));
            }
            success(result, args);
          }
        }
      }, function () {
        if (error) {
          error();
        }
      });
    } else if (OB.Data.localDB) {
      var tableName = OB.Dal.getTableName(model),
          propertyMap = OB.Dal.getPropertyMap(model),
          sql = 'SELECT * FROM ' + tableName,
          synchId, processResult, processError;

      processResult = function (tr, result) {
        if (synchId) {
          OB.UTIL.SynchronizationHelper.finished(synchId, 'find');
        }
        var i, collectionType = OB.Collection[model.prototype.modelName + 'List'] || Backbone.Collection,
            collection = new collectionType(),
            len = result.rows.length;
        if (len === 0) {
          success(collection, args);
        } else {
          for (i = 0; i < len; i++) {
            collection.add(OB.Dal.transform(model, result.rows.item(i)));
          }
          success(collection, args);
        }
      };

      processError = function (txError, e) {
        if (synchId) {
          OB.UTIL.SynchronizationHelper.finished(synchId, 'find');
        }
        if (!args || !args.doNotShowErrors) {
          executeSqlErrorHandler('error', "OB.Dal.find: table", tableName, txError, e, callerInfo);
        }
        if (error) {
          error();
        }
      };

      // websql
      // arguments check
      if (tableName === null) {
        console.warn("OB.Dal.find: tableName not found");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.find: tableName not found";
        });
      }
      if (propertyMap === null) {
        console.warn("OB.Dal.find: propertyMap not found");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.find: propertyMap not found";
        });
      }

      if (whereClause && whereClause._orderByClause) {
        orderBy = whereClause._orderByClause;
      }

      if (whereClause && whereClause._orderBy) {
        _.forEach(whereClause._orderBy, function (elem) {
          if (_.isUndefined(orderBy)) {
            orderBy = '';
          } else {
            orderBy += ', ';
          }
          orderBy += elem.column;
          if (!_.isUndefined(elem.asc)) {
            if (elem.asc) {
              orderBy += ' asc';
            } else {
              orderBy += ' desc';
            }
          }
        });
      }

      if (whereClause && whereClause._limit) {
        //set in the model temporary the limit set by the query 
        model.prototype.tempDataLimit = whereClause._limit;
        // set limit + 1 to see if there are more items to retrieve and in that case show a message to narrow the query in order to less items
        limit = whereClause._limit + 1;
      } else {
        limit = model.prototype.dataLimit;
        model.prototype.tempDataLimit = undefined;
      }
      if (whereClause && whereClause._whereClause) {
        whereClause.sql = ' ' + whereClause._whereClause;
      } else {
        whereClause = OB.Dal.getWhereClause(whereClause, propertyMap);
      }
      sql = sql + whereClause.sql;
      params = _.isEmpty(whereClause.params) ? [] : whereClause.params;

      if (orderBy) {
        sql = sql + ' ORDER BY ' + orderBy + ' ';
      } else if (model.propertyList || model.prototype.propertyMap._idx) {
        sql = sql + ' ORDER BY _idx ';
      }

      if (limit) {
        sql = sql + ' LIMIT ' + limit;
      }

      if (currentTransaction) {
        currentTransaction.executeSql(sql, params, processResult, processError);
      } else {
        OB.Data.localDB.readTransaction(function (tx) {
          if (model.prototype.modelName !== 'LogClient') {
            synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('find ' + model.prototype.modelName);
          }
          tx.executeSql(sql, params, processResult, processError);
        });
      }
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.queryUsingCache = function (model, sql, params, success, error, args) {
    if (OB.Cache.hasItem(sql, params, args ? args.modelsAffectedByCache : null)) {
      OB.Dal.stackSize++;
      if (OB.Dal.stackSize % 50 === 0) {
        setTimeout(function () {
          success(OB.Cache.getItem(sql, ''), args, args ? args.modelsAffectedByCache : null);
        }, 0);
      } else {
        success(OB.Cache.getItem(sql, ''), args, args ? args.modelsAffectedByCache : null);
      }
    } else {
      OB.Dal.query(model, sql, params, function (records) {
        OB.Cache.putItem(sql, params, records, args ? args.modelsAffectedByCache : null);
        success(records, args);
      }, error, args);
    }
  };

  OB.Dal.queryInTransaction = function (tx, model, sql, params, success, error, args) {
    OB.Dal.query(model, sql, params, success, error, args, tx);
  };

  OB.Dal.query = function (model, sql, params, success, error, args, currentTransaction, limit) {
    var processResult, processError;
    processResult = function (tr, result) {
      var i, collectionType = OB.Collection[model.prototype.modelName + 'List'] || Backbone.Collection,
          collection = new collectionType(),
          len = result.rows.length;
      if (len === 0) {
        success(collection, args);
      } else {
        for (i = 0; i < len; i++) {
          collection.add(OB.Dal.transform(model, result.rows.item(i)));
        }
        success(collection, args);
      }
    };

    processError = function (txError, e) {
      executeSqlErrorHandler('error', "OB.Dal.query: table", model.prototype.modelName, txError, e);
      if (_.isFunction(error)) {
        error();
      }
    };

    if (OB.Data.localDB) {
      if (_.isNumber(limit)) {
        //set in the model temporary the limit set by the query 
        model.prototype.tempDataLimit = limit;
        // set limit + 1 to see if there are more items to retrieve and in that case show a message to narrow the query in order to less items
        sql = sql + ' LIMIT ' + limit + 1;
      } else if (model.prototype.dataLimit) {
        sql = sql + ' LIMIT ' + model.prototype.dataLimit;
        model.prototype.tempDataLimit = undefined;
      }
      if (currentTransaction) {
        currentTransaction.executeSql(sql, _.isEmpty(params) ? [] : params, processResult, processError);
      } else {
        OB.Data.localDB.transaction(function (tx) {
          tx.executeSql(sql, _.isEmpty(params) ? [] : params, processResult, processError);
        });
      }
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.saveInTransaction = function (tx, model, success, error, forceInsert) {
    OB.Dal.save(model, success, error, forceInsert, tx);
  };

  OB.Dal.save = function (model, success, error, forceInsert, currentTransaction) {
    var caller = OB.UTIL.getStackTrace('save', true);
    var callerInfo = getCallerInfo();
    var modelProto = model.constructor.prototype,
        xhr, rr, data = {};
    forceInsert = forceInsert || false;
    //Validation to avoid wrong saves
    if (!_.isUndefined(model.get('json')) && (_.isEmpty(model.get('json')) || _.isNull(model.get('json')))) {
      OB.error('[OB.Dal.save] Wrong write in ' + model.modelName + ' because json column is undefined.' + ' - Caller: ' + caller + ' - callerInfo: ' + callerInfo + '- Model to save is: ' + JSON.stringify(model.toJSON()));
    }
    //The id cannot be null
    if (_.isNull(model.get('id')) || (model.get('json') && JSON.parse(model.get('json')) && _.isNull(JSON.parse(model.get('json')).id))) {
      OB.error('[OB.Dal.save] Wrong model, it has no id. ' + ' - Caller: ' + caller + ' - callerInfo: ' + callerInfo + ' Model: ' + JSON.stringify(model.toJSON()) + '\n callerInfo: ' + OB.UTIL.getStackTrace('getCallerInfo', false));
      var uuidTmp = OB.UTIL.get_UUID();
      model.set('id', uuidTmp);
      model.set('json', JSON.stringify(model.serializeToJSON()));
      forceInsert = true;
    }
    // TODO: properly check model type
    if (modelProto && modelProto.online) {
      if (!model) {
        console.warn("OB.Dal.save: you need to pass a Model instance to save");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.save: you need to pass a Model instance to save";
        });
      }
      data.operationType = 'update';
      data.data = model.toJSON();

      xhr = new enyo.Ajax({
        url: modelProto.source,
        method: 'PUT',
        data: JSON.stringify(data),
        success: function (inSender, inResponse) {
          success(inResponse);
        }
      });
      rr = new OB.RR.Request({
        ajaxRequest: xhr
      });
      rr.exec(xhr.url);

    } else if (OB.Data.localDB) {
      var modelDefinition = OB.Model[modelProto.modelName],
          tableName = OB.Dal.getTableName(modelDefinition),
          primaryKey, primaryKeyProperty = 'id',
          primaryKeyColumn, sql = '',
          params = null,
          firstParam = true,
          uuid, propertyName, filterVal, processError, successFunction;

      successFunction = function () {
        if (success) {
          success();
        }
        OB.Cache.resetCacheForModel(modelProto.modelName);
      };

      var updateToBeChecked = false;

      processError = function (txError, e) {
        executeSqlErrorHandler('error', "OB.Dal.save: table", tableName, txError, e, callerInfo);
        if (_.isFunction(error)) {
          error();
        }
      };

      // websql
      // argument checks
      if (!tableName) {
        console.warn("OB.Dal.save: tableName not found");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.save: tableName not found";
        });
      }

      if (modelDefinition.getPrimaryKey) {
        primaryKey = modelDefinition.getPrimaryKey();
        primaryKeyProperty = primaryKey.name;
        primaryKeyColumn = primaryKey.column;
      } else {
        primaryKeyColumn = modelProto.propertyMap[primaryKeyProperty];
      }

      if (model.get(primaryKeyProperty) && forceInsert === false) {
        if (modelDefinition.getUpdateStatement) {
          sql = modelDefinition.getUpdateStatement();
          params = [];
          _.each(modelDefinition.getPropertiesForUpdate(), function (property) {
            //filter doen't have name and always is the last one
            if (property.name) {
              params.push(model.get(property.name));
            }
          });
          //filter param
          if (modelDefinition.hasFilter()) {
            filterVal = '';
            _.each(modelDefinition.getFilterProperties(), function (filterProperty) {
              filterVal = OB.UTIL.unAccent(filterVal) + (model.get(filterProperty) ? (model.get(filterProperty) + '###') : '');
            });
            params.push(filterVal);
          }
          //Where param
          params.push(model.get(primaryKeyProperty));
        } else {
          // UPDATE
          if (tableName === 'c_order') {
            updateToBeChecked = true;
          }
          sql = 'UPDATE ' + tableName + ' SET ';

          _.each(_.keys(modelProto.properties), function (attr) {
            propertyName = modelProto.properties[attr];
            if (attr === 'id') {
              return;
            }

            if (firstParam) {
              firstParam = false;
              params = [];
            } else {
              sql = sql + ', ';
            }

            sql = sql + modelProto.propertyMap[propertyName] + ' = ? ';
            params.push(model.get(propertyName));
          });

          if (modelProto.propertiesFilter) {
            filterVal = '';
            _.each(modelProto.propertiesFilter, function (prop) {
              filterVal = filterVal + (model.get(prop) ? (model.get(prop) + '###') : '');
            });
            sql = sql + ', _filter = ? ';
            params.push(filterVal);
          }
          sql = sql + ' WHERE ' + tableName + '_id = ?';
          params.push(model.get('id'));
        }
      } else {
        params = [];
        // INSERT
        sql = modelDefinition.getInsertStatement ? modelDefinition.getInsertStatement() : modelProto.insertStatement;
        if (forceInsert === false) {
          uuid = OB.UTIL.get_UUID();
          params.push(uuid);
          if (model.getPrimaryKey) {
            primaryKey = model.getPrimaryKey();
            model.set(primaryKey.name, uuid);
          } else {
            model.set('id', uuid);
          }
        }
        //Set params
        if (modelDefinition.getProperties) {
          _.each(modelDefinition.getProperties(), function (property) {
            if (forceInsert === false) {
              if (property.primaryKey) {
                return;
              }
            }
            //_filter property doesn't have name.
            //don't set the filter column. We will do it in the next step
            if (property.name === '_filter') {
              return;
            }
            if (property.name) {
              params.push(model.get(property.name) === undefined ? null : model.get(property.name));
            }
          });
        } else {
          _.each(modelProto.properties, function (property) {
            if (forceInsert === false) {
              if ('id' === property) {
                return;
              }
            }
            if (model.get(property) === '_filter') {
              return;
            }
            params.push(model.get(property) === undefined ? null : model.get(property));
          });
        }
        //set filter column
        if (modelDefinition.hasFilter) {
          if (modelDefinition.hasFilter()) {
            filterVal = '';
            _.each(modelDefinition.getFilterProperties(), function (filterProp) {
              filterVal = OB.UTIL.unAccent(filterVal) + (model.get(filterProp) ? (model.get(filterProp) + '###') : '');
            });
            //Include in the last position but before _idx
            params.splice(params.length - 1, 0, filterVal);
          }
        } else {
          if (modelProto.propertiesFilter) {
            filterVal = '';
            _.each(modelProto.propertiesFilter, function (prop) {
              filterVal = filterVal + (model.get(prop) ? (model.get(prop) + '###') : '');
            });
            //Include in the last position but before _idx
            params.splice(params.length - 1, 0, filterVal);
          }
        }
      }

      if (currentTransaction) {
        if (updateToBeChecked) {
          OB.Dal.getInTransaction(currentTransaction, modelDefinition, model.get('id'), function (result) {
            if (result) {
              if (result.get('hasbeenpaid') === 'Y') {
                OB.error('[checkBlocked][transaction][hasbeenpaid_is_yes] Wrong write in c_order avoided [' + result.get('id') + '][' + result.get('documentNo') + '] - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
                if (successFunction) {
                  successFunction();
                }
              } else {
                try {
                  currentTransaction.executeSql(sql, params, silentFunction(successFunction), processError);
                } catch (e) {
                  executeSqlErrorHandler('error', "OB.Dal.save: table", tableName, null, e, callerInfo);
                }
              }
            } else {
              //no results
              OB.warn('[checkBlocked][transaction] No result after getInTransaction for [' + model.get('id') + '][' + model.get('documentNo') + ']. This update doesnt make sense - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
              if (successFunction) {
                successFunction();
              }
            }
          }, function () {
            OB.error('[checkBlocked][transaction] Error for [' + model.get('id') + '][' + model.get('documentNo') + '] - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
            if (error) {
              error();
            }
          }, function () {
            //no results
            OB.warn('[checkBlocked][transaction] No result after getInTransaction for [' + model.get('id') + '][' + model.get('documentNo') + ']. This update doesnt make sense - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
            if (successFunction) {
              successFunction();
            }
          });
        } else {
          try {
            currentTransaction.executeSql(sql, params, silentFunction(successFunction), processError);
          } catch (e) {
            executeSqlErrorHandler('error', "OB.Dal.save: table", tableName, null, e, callerInfo);
          }
        }
      } else {
        if (updateToBeChecked) {
          OB.Data.localDB.transaction(function (tx) {
            OB.Dal.getInTransaction(tx, modelDefinition, model.get('id'), function (result) {
              if (result) {
                if (result.get('hasbeenpaid') === 'Y') {
                  OB.error('[checkBlocked][no-transaction][hasbeenpaid_is_yes] Wrong write in c_order avoided [' + result.get('id') + '][' + result.get('documentNo') + '] - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
                  if (successFunction) {
                    successFunction();
                  }
                } else {
                  try {
                    tx.executeSql(sql, params, silentFunction(successFunction), processError);
                  } catch (e) {
                    executeSqlErrorHandler('error', "OB.Dal.save: table", tableName, null, e, callerInfo);
                  }
                }
              } else {
                //no results
                OB.warn('[checkBlocked][no-transaction] No result after getInTransaction for [' + model.get('id') + '][' + model.get('documentNo') + ']. This update doesnt make sense - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
                if (successFunction) {
                  successFunction();
                }
              }
            }, function () {
              OB.error('[checkBlocked][no-transaction] Error for [' + model.get('id') + '][' + model.get('documentNo') + ']');
              if (error) {
                error();
              }
            }, function () {
              //no results
              OB.warn('[checkBlocked][no-transaction] No result after getInTransaction for [' + model.get('id') + '][' + model.get('documentNo') + ']. This update doesnt make sense - Caller: ' + caller + ' - callerInfo: ' + callerInfo);
              if (successFunction) {
                successFunction();
              }
            });
          });
        } else {
          OB.Data.localDB.transaction(function (tx) {
            try {
              tx.executeSql(sql, params, silentFunction(successFunction), processError);
            } catch (e) {
              executeSqlErrorHandler('error', "OB.Dal.save: table", tableName, null, e, callerInfo);
            }
          });
        }
      }
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.removeInTransaction = function (tx, model, success, error) {
    OB.Dal.remove(model, success, error, tx);
  };

  /*
   * This method checks if model to save is present in the database before saving it.
   * If the model is there it does nothing, if not it does an insert.
   */
  OB.Dal.saveIfNew = function (model, success, error) {
    OB.Dal.transaction(function (tx) {
      OB.Dal.getInTransaction(tx, OB.Model[model.modelName], model.get('id'), success, error, function () {
        OB.Dal.saveInTransaction(tx, model, success, error, true);
      }, true);
    });
  };

  OB.Dal.updateRecordColumn = function (record, columnName, newValue, successCallback, errorCallback) {
    if (OB.Data.localDB) {
      var modelProto = record.constructor.prototype;
      var modelDefinition = OB.Model[modelProto.modelName];
      var tableName = OB.Dal.getTableName(modelDefinition);
      var sql = "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + tableName + "_id = ?";
      var params = [newValue, record.get('id')];
      OB.Data.localDB.transaction(function (tx) {
        try {
          tx.executeSql(sql, params, function () {
            // success
            OB.info("'isbeingprocessed' has been set to 'Y' in the '" + tableName + "' table, record id: " + record.get('id'));
            successCallback();
          }, function (txError, e) {
            // error
            OB.error("'isbeingprocessed' has NOT been set to 'Y' in the '" + tableName + "' table, record id: " + record.get('id') + ". Error message: " + e.message);
            errorCallback(txError, e);
          });
        } catch (e) {
          OB.error("cannot create a transaction for the '" + tableName + "' table, record id: " + record.get('id') + ". Error message: " + e);
          errorCallback(null, e);
        }
      });
    }
  };

  OB.Dal.createDataDump = function (models, callback) {
    var result = [],
        cnt = 0,
        promises = [];

    if (models.length === 0) {
      if (callback) {
        callback();
      }
      return;
    }

    _.each(models, function (model) {
      promises.push(new Promise(function (resolve, reject) {
        OB.Dal.find(model, null, function (data) {
          cnt++;
          // arbitrary high number we can't load more in memory
          if (cnt > 5000) {
            var msg = "Creating backup of more 5000 records, this it probably not supported, last record " + JSON.stringify(data);
            OB.UTIL.showError(msg);
            OB.error(msg);
            throw msg;
          }
          result.push({
            'model': model,
            'data': data.models
          });
          resolve();
        }, function () {
          reject();
        });
      }));
    });

    Promise.all(promises).then(function () {
      if (callback) {
        callback(result);
      }
    });
  };

  OB.Dal.restoreDataDump = function (dataDump, callback) {
    if (!dataDump) {
      if (callback) {
        callback();
      }
      return;
    }

    OB.Dal.transaction(function (tx) {
      _.each(dataDump, function (dataDumpEntry) {
        OB.Dal.removeAllInTransaction(tx, dataDumpEntry.model);
        _.each(dataDumpEntry.data, function (dataEntry) {
          OB.Dal.saveInTransaction(tx, dataEntry, null, null, true);
        });
      });
    }, function () {
      OB.error("The restore dump failed failed to be commited. data: " + JSON.stringify(dataDump));
    }, function () {
      if (callback) {
        callback();
      }
    });
  };

  OB.Dal.remove = function (model, success, error, currentTransaction) {
    if (OB.Data.localDB) {
      var modelDefinition = OB.Model[model.constructor.prototype.modelName],
          modelProto = model.constructor.prototype,
          tableName = OB.Dal.getTableName(modelDefinition),
          pk, pkProperty = 'id',
          pkColumn, sql = '',
          params = [],
          processError;

      processError = function (txError, e) {
        executeSqlErrorHandler('error', "OB.Dal.remove: table", tableName, txError, e);
        if (_.isFunction(error)) {
          error();
        }
      };

      // websql
      if (!tableName) {
        console.warn("OB.Dal.remove: tableName not found");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.remove: tableName not found";
        });
      }
      if (modelDefinition.getPrimaryKey) {
        pk = modelDefinition.getPrimaryKey() ? modelDefinition.getPrimaryKey() : null;
        if (pk) {
          pkProperty = pk.name;
          pkColumn = pk.column;
        } else {
          pkColumn = modelDefinition.propertyMap[pkProperty];
        }
      }
      if (model.get(pkProperty)) {
        if (modelDefinition.getDeleteByIdStatement) {
          sql = modelDefinition.getDeleteByIdStatement();
        } else {
          sql = 'DELETE FROM ' + tableName + ' WHERE ' + modelProto.propertyMap[pkProperty] + ' = ? ';
        }
        // UPDATE
        params.push(model.get(pkProperty));
      } else {
        console.warn("OB.Dal.remove: an object without primary key cannot be deleted");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.remove: an object without primary key cannot be deleted";
        });
      }

      if (currentTransaction) {
        currentTransaction.executeSql(sql, params, silentFunction(success), processError);
      } else {
        OB.Data.localDB.transaction(function (tx) {
          tx.executeSql(sql, params, silentFunction(success), processError);
        });
      }
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.removeAllInTransaction = function (tx, model, criteria, success, error) {
    OB.Dal.removeAll(model, criteria, success, error, tx);
  };

  OB.Dal.removeAll = function (model, criteria, success, error, currentTransaction) {
    if (OB.Data.localDB) {
      var tableName = OB.Dal.getTableName(model),
          propertyMap = OB.Dal.getPropertyMap(model),
          sql, params, whereClause, processError;

      processError = function (txError, e) {
        executeSqlErrorHandler('error', "OB.Dal.removeAll: table", tableName, txError, e);
        if (_.isFunction(error)) {
          error();
        }
      };

      // websql
      if (!tableName) {
        console.warn("OB.Dal.removeAll: tableName not found");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.removeAll: tableName not found";
        });
      }

      sql = 'DELETE FROM ' + tableName;
      if (criteria && criteria.whereClause) {
        whereClause = criteria.whereClause;
        sql = sql + whereClause;
      } else {
        whereClause = OB.Dal.getWhereClause(criteria, propertyMap);
        sql = sql + whereClause.sql;
        params = _.isEmpty(whereClause.params) ? [] : whereClause.params;
      }

      if (currentTransaction) {
        currentTransaction.executeSql(sql, params, silentFunction(success), processError);
      } else {
        OB.Data.localDB.transaction(function (tx) {
          tx.executeSql(sql, params, silentFunction(success), processError);
        });
      }
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.removeRemoteModels = function () {
    var model;
    for (model in OB.Model) {
      if (OB.Model.hasOwnProperty(model) && OB.Model[model].prototype && OB.Model[model].prototype.remote && OB.Dal.getTableName(OB.Model[model]) && OB.MobileApp.model.hasPermission(OB.Model[model].prototype.remote, true)) {
        OB.Dal.removeAll(OB.Model[model]);
      }
    }
  };

  OB.Dal.getInTransaction = function (tx, model, id, success, error, empty, local) {
    OB.Dal.get(model, id, success, error, empty, tx, local);
  };

  OB.Dal.get = function (model, id, success, error, empty, currentTransaction, local) {
    OB.UTIL.Debug.execute(function () {
      var caller = OB.UTIL.getStackTrace('get', true);
      var callerInfo = getCallerInfo();
      if (!id) {
        OB.warn("[dberror] OB.Dal.get: id not found. - Caller: " + caller + " - Caller info: " + callerInfo);
      }
    });
    if (!local && model.prototype.remote && OB.MobileApp.model.hasPermission(model.prototype.remote, true)) {
      var process = new OB.DS.Process(model.prototype.source);
      var currentDate = new Date();
      var params = {};
      params.terminalTime = currentDate;
      params.terminalTimeOffset = currentDate.getTimezoneOffset();
      var p, i;
      var data = {};

      if (params) {
        p = {};
        for (i in params) {
          if (params.hasOwnProperty(i)) {
            if (typeof params[i] === 'string') {
              p[i] = {
                value: params[i],
                type: 'string'
              };
            } else if (typeof params[i] === 'number') {
              if (params[i] === Math.round(params[i])) {
                p[i] = {
                  value: params[i],
                  type: 'long'
                };
              } else {
                p[i] = {
                  value: params[i],
                  type: 'bigdecimal'
                };
              }
            } else if (typeof params[i] === 'boolean') {
              p[i] = {
                value: params[i],
                type: 'boolean'
              };
            } else {
              p[i] = params[i];
            }
          }
        }
        data.parameters = p;
      }
      var objectId = {
        columns: ['id'],
        operator: 'equals',
        value: id,
        isId: true
      };

      var remoteCriteria = [objectId];
      data.remoteFilters = remoteCriteria;
      process.exec(data, function (data) {
        if (data && data.exception) {
          if (error) {
            error(null, data.exception);
          }
        } else {
          if (success) {
            if (data.length === 0) {
              if (empty) {
                empty(null);
              }
            } else {
              success(OB.Dal.transform(model, data[0]));
            }
          }
        }
      }, function () {
        if (error) {
          error();
        }
      });
    } else if (OB.Data.localDB) {
      var tableName = OB.Dal.getTableName(model),
          sql = 'SELECT * FROM ' + tableName + ' WHERE ' + tableName + '_id = ?',
          processResult, processError;

      processResult = function (tr, result) {
        if (result.rows.length === 0) {
          if (empty) {
            empty();
          } else {
            return null;
          }
        } else {
          success(OB.Dal.transform(model, result.rows.item(0)));
        }
      };

      processError = function (txError, e) {
        executeSqlErrorHandler('error', "OB.Dal.get: table", tableName, txError, e);
        if (_.isFunction(error)) {
          error();
        }
      };

      // websql
      if (currentTransaction) {
        currentTransaction.executeSql(sql, [id], processResult, processError);
      } else {
        OB.Data.localDB.readTransaction(function (tx) {
          tx.executeSql(sql, [id], processResult, processError);
        });
      }
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.dropTable = function (model, successCallback, errorCallback) {
    if (OB.Data.localDB) {
      var sql = model.getDropStatement ? model.getDropStatement() : model.prototype.dropStatement;
      OB.Data.localDB.transaction(function (tx) {
        tx.executeSql(sql, [], function () {
          OB.debug('succesfully dropped table: ' + sql);
          successCallback();
        }, errorCallback);
      });
    } else {
      this.missingLocalStorageLogic();
    }
  };

  OB.Dal.initCache = function (model, initialData, success, error, incremental) {
    if (OB.Data.localDB) {
      // error must be defined, if not it fails in some android versions
      error = error ||
      function () {};

      if (!model.propertyList && (!model.prototype.createStatement || !model.prototype.dropStatement)) {
        console.warn("OB.Dal.initCache: model requires a create and drop statement");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.initCache: model requires a create and drop statement";
        });
      }

      if (!initialData) {
        console.warn("OB.Dal.initCache: initialData must be passed as parameter");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.initCache: initialData must be passed as parameter";
        });
      }

      if (!model.prototype.local && !incremental) {
        OB.Data.localDB.transaction(function (tx) {
          var st = model.getDropStatement ? model.getDropStatement() : model.prototype.dropStatement;
          tx.executeSql(st, [], null, function (txError, e) {
            executeSqlErrorHandler('error', "OB.Dal.initCache: table", model.prototype.modelName, txError, e);
            if (_.isFunction(error)) {
              error();
            }
          });
        }, error);
      }

      OB.Data.localDB.transaction(function (tx) {
        var createStatement = model.getCreateStatement ? model.getCreateStatement() : model.prototype.createStatement;

        // Issue 30939: Do not allow nulls in the primary key
        if (createStatement.indexOf("PRIMARY KEY NOT NULL") === -1) {
          createStatement = createStatement.replace("PRIMARY KEY", "PRIMARY KEY NOT NULL");
        }
        OB.UTIL.Debug.execute(function () {
          if (createStatement.indexOf("PRIMARY KEY NOT NULL") === -1) {
            throw "The create statement must contain a 'PRIMARY KEY NOT NULL' (createStatement: '" + createStatement + "'";
          }
        });

        var createIndexStatement;
        tx.executeSql(createStatement, [], function () {
          //Create Index
          if (model.hasIndex && model.hasIndex()) {
            _.each(model.getIndexes(), function (indexDefinition) {
              createIndexStatement = model.getCreateIndexStatement(indexDefinition);
              tx.executeSql(createIndexStatement, [], null, function (txError, e) {
                executeSqlErrorHandler('error', "OB.Dal.initCache: index for table", model.prototype.modelName, txError, e);
              });
            });
          }

          model.areTablesCreated = true;

        }, function (txError, e) {
          executeSqlErrorHandler('error', "OB.Dal.initCache: table", model.prototype.modelName, txError, e);
        });
      }, error);

      if (_.isArray(initialData)) {
        OB.Dal.insertData(model, initialData, success, error, incremental, 0);
      } else { // no initial data
        console.warn("OB.Dal.initCache: initialData must be an Array");
        OB.UTIL.Debug.execute(function () {
          throw "OB.Dal.initCache: initialData must be an Array";
        });
      }
    } else {
      this.missingLocalStorageLogic();
    }

  };

  /**
   * Inserts data into a local table
   *
   *
   */
  OB.Dal.insertData = function (model, data, success, error, incremental, offset) {
    OB.Data.localDB.transaction(function (tx) {
      var props = model.getProperties ? model.getProperties() : model.prototype.properties,
          filterVal, values, _idx = offset,
          updateRecord, handleError, insertStatement, filterProps;
      handleError = function (txError, e) {
        executeSqlErrorHandler('error', "OB.Dal.insertData: model insert", model.prototype.modelName, txError, e);
        if (_.isFunction(error)) {
          error();
        }
      };
      updateRecord = function (tx, model, values, active) {
        var deleteStatement;
        deleteStatement = model.getDeleteByIdStatement ? model.getDeleteByIdStatement() : "DELETE FROM " + model.prototype.tableName + " WHERE " + model.prototype.propertyMap.id + "=?";
        tx.executeSql(deleteStatement, [values[0]], function () {
          if (_.isUndefined(active) || active) {
            var insertSatement;
            insertSatement = model.getInsertStatement ? model.getInsertStatement() : model.prototype.insertStatement;
            tx.executeSql(insertSatement, values, null, handleError);
          }
        }, handleError);
      };
      insertStatement = model.getInsertStatement ? model.getInsertStatement() : model.prototype.insertStatement;
      filterProps = model.getFilterProperties ? model.getFilterProperties() : model.prototype.propertiesFilter;
      _.each(data, function (item) {
        values = [];
        _.each(props, function (prop) {
          var value, propName = typeof prop === 'string' ? prop : prop.name;
          if (!propName || '_idx' === propName || '_filter' === propName) {
            return;
          }
          value = item[propName];
          values.push(value === undefined ? null : value);
        });
        if ((model.hasFilter && model.hasFilter()) || model.prototype.propertiesFilter) {
          filterVal = '';
          _.each(filterProps, function (prop) {
            filterVal = OB.UTIL.unAccent(filterVal) + (item[prop] ? (item[prop] + '###') : '');
          });
          values.push(filterVal);
        }
        values.push(_idx);
        if (incremental) {
          updateRecord(tx, model, values, item.active);
        } else if (OB.UTIL.isNullOrUndefined(item.active) || (item.active && item.active === true)) {
          tx.executeSql(insertStatement, values, null, handleError);
        }
        _idx++;
      });
    }, error, function () {
      if (data.length > 0) {
        OB.Cache.resetCacheForModel(model.prototype.modelName);
      }
      if (_.isFunction(success)) {
        success();
      }
    });
  };

  /**
   * Loads a set of models
   *
   *
   */
  OB.Dal.loadModels = function (online, models, data, incremental) {

    var timestamp = 0;
    var key;

    var modelsInLocalDb = [];

    function forceLocalDatabaseLoad(models, idx, callback) {
      if (idx === models.length) {
        if (callback) {
          callback();
        }
        return;
      }

      OB.Dal.find(models[idx], {
        _limit: 100
      }, function () {
        forceLocalDatabaseLoad(models, idx + 1, callback);
      }, function () {
        forceLocalDatabaseLoad(models, idx + 1, callback);
      });
    }

    function triggerReady(models) {
      if (incremental && models._LoadQueue && OB.UTIL.queueStatus(models._LoadQueue) && OB.MobileApp.model.get('modelsToLoad').length === 0) {
        // All models have finished the incremental load process.
        // We can safely set the POSLastIncRefresh timestamp now.
        OB.UTIL.localStorage.setItem('POSLastIncRefresh', new Date().getTime());
        OB.MobileApp.model.trigger('incrementalModelsLoaded');
      }
      if (OB.MobileApp.model.get('modelsToLoad').length === 0 && OB.UTIL.queueStatus(models._LoadQueue || {})) {
        // this is only triggered when all models (online and offline) are loaded.
        // offline models are loaded first but don't trigger this, it is not till
        // windowModel is going to be rendered when online models are loaded and this
        // is triggered.
        if (!OB.MobileApp.model.get('datasourceLoadFailed')) {
          if (models._LoadQueue && !incremental) {
            // At this point we can ensure that all models have loaded correctly.
            // If we are performing a full refresh of the data, now we can set the POSLastTotalRefresh timestamp. 
            OB.UTIL.localStorage.setItem('POSLastTotalRefresh', new Date().getTime());
          }
          OB.UTIL.showLoadingMessage(OB.I18N.getLabel('OBMOBC_ForcingRefreshOfLocalDatabase'));
          forceLocalDatabaseLoad(modelsInLocalDb, 0, function () {
            OB.MobileApp.model.trigger('allModelsLoaded');
          });
        } else {
          // We know that the load of one or more models have failed.
          // Reload the page to load all the models again. 
          // We will not force a refresh if a full master data refresh was already done, and there hasn't been a failed paged request
          var hasPagedRequestError = _.find(models._failedModels, function (modelName) {
            return OB.UTIL.localStorage.getItem('lastUpdatedTimestamp' + modelName) === null;
          });
          if (OB.UTIL.localStorage.getItem('POSLastTotalRefresh') === null || hasPagedRequestError) {
            // Remove all timestamp to force a full refresh
            OB.UTIL.localStorage.removeItem('POSLastTotalRefresh');
            _.forEach(models, function (model) {
              OB.UTIL.localStorage.removeItem('lastUpdatedTimestamp' + model.modelName);
            });
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_MasterDataErrorHeader'), OB.I18N.getLabel('OBMOBC_MasterDataErrorBody', [models._failedModels.join(', ')]) + OB.I18N.getLabel('OBMOBC_LoadingErrorBody'), [{
              label: OB.I18N.getLabel('OBMOBC_Reload'),
              action: function () {
                window.location.reload();
              }
            }], {
              onShowFunction: function (popup) {
                popup.$.headerCloseButton.hide();
              },
              autoDismiss: false
            });
          } else {
            //Data might be old, but it should be enough to work
            OB.MobileApp.model.trigger('allModelsLoaded');
          }
        }
      }
    }

    function buildLoadQueue() {
      _.each(models, function (item) {
        if (item && item.generatedModel) {
          item = OB.Model[item.modelName];
        }
        if (item && ((online && item.prototype.online) || (!online && !item.prototype.online)) && !item.prototype.local) {
          models._LoadQueue = models._LoadQueue || {};
          models._LoadQueue[item.prototype.modelName] = false;
        }
      });
    }

    function processModelAtIndex(index) {
      var ds, load, item = models[index];
      var removeLoadedModelsFromArray = function (modelName) {
          var position = OB.MobileApp.model.get('modelsToLoad').indexOf(modelName);
          //var modelsList = ;
          if (position !== -1) {
            OB.MobileApp.model.get('modelsToLoad').splice(position, 1);
          }
          };

      if (index === models.length) {
        triggerReady(models);
        return;
      }

      if (item && item.generatedModel) {
        item = OB.Model[item.modelName];
      }
      load = item && ((online && item.prototype.online) || (!online && !item.prototype.online)) && (!item.prototype.remote || (item.prototype.remote && !OB.MobileApp.model.hasPermission(item.prototype.remote, true)));
      //TODO: check permissions
      if (load) {
        if (item.prototype.local) {
          OB.Dal.initCache(item, [], function () {
            // OB.info('init success: ' + item.prototype.modelName);
            processModelAtIndex(index + 1);
          }, function () {
            OB.error('init error', arguments);
            processModelAtIndex(index + 1);
          });
        } else {
          // OB.info('[sdrefresh] load model ' + item.prototype.modelName + ' ' + (incremental ? 'incrementally' : 'full'));
          if (incremental && OB.UTIL.localStorage.getItem('lastUpdatedTimestamp' + item.prototype.modelName)) {
            timestamp = OB.UTIL.localStorage.getItem('lastUpdatedTimestamp' + item.prototype.modelName);
          }
          if (!item.prototype.online) {
            modelsInLocalDb.push(item);
          }
          OB.UTIL.localStorage.removeItem('recordsFromBackendFor' + item.prototype.modelName);
          ds = new OB.DS.DataSource(new OB.DS.Request(item, timestamp));
          models._failedModels = models._failedModels || [];
          ds.on('ready', function (status) {
            var itemModelName = item.prototype.modelName;
            var finishTransaction = function () {
                models._LoadQueue[item.prototype.modelName] = true;
                if (item && item.prototype) {
                  removeLoadedModelsFromArray(item.prototype.modelName);
                }
                if (models._failedModels.length > 0) {
                  OB.MobileApp.model.set('datasourceLoadFailed', {
                    silent: true
                  });
                }
                processModelAtIndex(index + 1);
                };
            if (status === 'failed' || status === 'timeout') {
              models._failedModels.push(item.prototype.modelName);
              if (!incremental) {
                finishTransaction();
              }
            } else {
              var logMsgForTransaction = function (isSuccess, modelName, totalRecords) {
                  OB.debug(isSuccess ? '[sdreresh-' + (incremental ? 'inc' : 'full') + '] The model ' + modelName + ' has in local db ' + totalRecords + ' records' : '[sdreresh-' + (incremental ? 'inc' : 'full') + '] There  model ' + modelName + ' has an error getting the number of records from local db');
                  if (data && online) {
                    data[modelName] = new Backbone.Collection(ds.cache);
                  }
                  };
              var checkRecordsWithBackend = function (modelName, totalRecords) {
                  // If the number of records in websql is not equal to the number of recrods sent by the server, an error is raised
                  // This shloud only be done during a full refresh
                  if (!incremental) {
                    if (totalRecords !== parseInt(OB.UTIL.localStorage.getItem('recordsFromBackendFor' + modelName), 10)) {
                      models._failedModels.push(modelName);
                    }
                  }
                  };
              OB.Data.localDB.transaction(function (tx) {
                tx.executeSql("select count(1) as totalRecords from " + item.prototype.tableName, [], function (tx, result) {
                  logMsgForTransaction(true, item.prototype.modelName, result.rows.item(0).totalRecords);
                  checkRecordsWithBackend(item.prototype.modelName, result.rows.item(0).totalRecords);
                }, function () {
                  logMsgForTransaction(false, item.prototype.modelName);
                });
              }, function (errorMsg) {
                OB.error(errorMsg);
                finishTransaction();
              }, function () {
                finishTransaction();
              });
            }

            // Skip load of models if it's a incremental refresh and the request timeout has been reached
            if (incremental && (status === 'timeout' || status === 'failed')) {
              models.trigger('ready');
              return;
            }
          });

          if (item.prototype.includeTerminalDate) {
            var currentDate = new Date();
            item.params = item.params || {};
            item.params.terminalTime = currentDate;
            item.params.terminalTimeOffset = currentDate.getTimezoneOffset();
          }
          ds.load(item.params, incremental);
        }
      } else if (item && item.prototype.remote && OB.MobileApp.model.hasPermission(item.prototype.remote, true)) {
        if (models._LoadQueue) {
          models._LoadQueue[item.prototype.modelName] = true;
        }
        if (item && item.prototype) {
          removeLoadedModelsFromArray(item.prototype.modelName);
        }
        processModelAtIndex(index + 1);
      } else {
        if (item && item.prototype) {
          removeLoadedModelsFromArray(item.prototype.modelName);
        }
        processModelAtIndex(index + 1);
      }
    }

    models._LoadOnline = online;

    if (models.length === 0) {
      triggerReady(models);
      return;
    }

    buildLoadQueue();


    // Create an array of models to load to know when all the models are loaded
    for (key in models._LoadQueue) {
      if (models._LoadQueue.hasOwnProperty(key)) {
        if (OB.MobileApp.model.get('modelsToLoad').indexOf(key) === -1) {
          OB.MobileApp.model.get('modelsToLoad').push(key);
        }
      }
    }

    processModelAtIndex(0);
  };

}());