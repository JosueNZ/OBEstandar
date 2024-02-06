isc.defineClass('SSIPOTM_LoadLinesPopup', isc.OBPopup);
isc.SSIPOTM_LoadLinesPopup.addProperties({
  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  view: null,
  params: null,
  actionHandler: null,

  mainform: null,
  okButton: null,
  cancelButton: null,

  initWidget: function () {

    var originalView = this.view,
    params = this.params;
    
    this.mainform = isc.Label.create({
      height: 1,
      width: 300,
      overflow: 'visible',
      align: "center",
      valign: "center",
      contents: '¿Cargar líneas? '
    });

    this.okButton = isc.OBFormButton.create({
      title: 'Aceptar',
      popup: this,
      action: function () {
        var callback = function (rpcResponse, data, rpcRequest) {

          var status = rpcResponse.status,
              view = rpcRequest.clientContext.originalView.getView('98C2400F56ED43FFB112C7EA1C988570'); //tabid

          if (data.status == 'OK') {
            view.messageBar.setMessage('success', null, 'Líneas cargadas con éxito');
          } else {
            view.messageBar.setMessage('error', null,'Error al intentar cargar líneas<br>'+data.message );
          }

          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
          // rpcRequest.clientContext.popup.params.button.contextView.viewGrid.refreshGrid();
        };

        OB.RemoteCallManager.call(this.popup.actionHandler, {
          rows: this.popup.rows,
        }, {}, callback, { 
          originalView: this.popup.view,
          popup: this.popup });
      },
    });

    this.cancelButton = isc.OBFormButton.create({
      title: 'Cancelar',
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.items = [
      isc.VLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        width: "100%",
        layoutMargin: 10,
        membersMargin: 6,
        members: [
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            layoutMargin: 30,
            membersMargin: 6,
            members: this.mainform
          }),
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            membersMargin: 10,
            members: [this.okButton, this.cancelButton]
          })
        ]
      })
    ];

    this.Super('initWidget', arguments);
  }

});

isc.defineClass('SSIPOTM_AccumulatePopup', isc.OBPopup);
isc.SSIPOTM_AccumulatePopup.addProperties({
  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  view: null,
  params: null,
  actionHandler: null,

  mainform: null,
  okButton: null,
  cancelButton: null,

  initWidget: function () {

    var originalView = this.view,
    params = this.params;
    
    this.mainform = isc.Label.create({
      height: 1,
      width: 300,
      overflow: 'visible',
      align: "center",
      valign: "center",
      contents: '¿Enviar a la pestaña de acumulados? '
    });

    this.okButton = isc.OBFormButton.create({
      title: 'Aceptar',
      popup: this,
      action: function () {
        var callback = function (rpcResponse, data, rpcRequest) {

          var status = rpcResponse.status,
              view = rpcRequest.clientContext.originalView.getView('98C2400F56ED43FFB112C7EA1C988570'); //tabid

          if (data.status == 'OK') {
            view.messageBar.setMessage('success', null, 'Líneas enviadas con éxito');
          } else {
            view.messageBar.setMessage('error', null,'Error al intentar enviar líneas<br>'+data.message );
          }

          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
          // rpcRequest.clientContext.popup.params.button.contextView.viewGrid.refreshGrid();
        };

        OB.RemoteCallManager.call(this.popup.actionHandler, {
          rows: this.popup.rows,
        }, {}, callback, { 
          originalView: this.popup.view,
          popup: this.popup });
      }
    });

    this.cancelButton = isc.OBFormButton.create({
      title: 'Cancelar',
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.items = [
      isc.VLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        width: "100%",
        layoutMargin: 10,
        membersMargin: 6,
        members: [
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            layoutMargin: 30,
            membersMargin: 6,
            members: this.mainform
          }),
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            membersMargin: 10,
            members: [this.okButton, this.cancelButton]
          })
        ]
      })
    ];

    this.Super('initWidget', arguments);
  }

});

isc.defineClass('SSIPOTM_UpdateStockPopup', isc.OBPopup);
isc.SSIPOTM_UpdateStockPopup.addProperties({
  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  view: null,
  params: null,
  actionHandler: null,

  mainform: null,
  okButton: null,
  cancelButton: null,

  initWidget: function () {

    var originalView = this.view,
    params = this.params;

    this.mainform = isc.Label.create({
      height: 1,
      width: 300,
      overflow: 'visible',
      align: "center",
      valign: "center",
      contents: '¿Actualizar Stock? '
    });

    this.okButton = isc.OBFormButton.create({
      title: 'Aceptar',
      popup: this,
      action: function () {
        var callback = function (rpcResponse, data, rpcRequest) {

          var status = rpcResponse.status,
              view = rpcRequest.clientContext.originalView.getView('98C2400F56ED43FFB112C7EA1C988570'); //tabid
          
          if (data.status == 'OK') {
            view.messageBar.setMessage('success', null, 'Stock actualizado con éxito');
          } else {
            view.messageBar.setMessage('error', null,'Error al intentar actualizar stock<br>'+data.message );
          }

          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
          // rpcRequest.clientContext.popup.params.button.contextView.viewGrid.refreshGrid();
        };

        OB.RemoteCallManager.call(this.popup.actionHandler, {
          rows: this.popup.rows,
        }, {}, callback, { 
          originalView: this.popup.view,
          popup: this.popup });
      }
    });

    this.cancelButton = isc.OBFormButton.create({
      title: 'Cancelar',
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.items = [
      isc.VLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        width: "100%",
        layoutMargin: 10,
        membersMargin: 6,
        members: [
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            layoutMargin: 30,
            membersMargin: 6,
            members: this.mainform
          }),
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            membersMargin: 10,
            members: [this.okButton, this.cancelButton]
          })
        ]
      })
    ];

    this.Super('initWidget', arguments);
  }

});

isc.defineClass('SSIPOTM_ProcessPopup', isc.OBPopup);
isc.SSIPOTM_ProcessPopup.addProperties({
  width: 320,
  height: 200,
  title: null,
  showMinimizeButton: false,
  showMaximizeButton: false,

  view: null,
  params: null,
  actionHandler: null,

  mainform: null,
  okButton: null,
  cancelButton: null,

  initWidget: function () {
    
    var originalView = this.view,
    params = this.params;

    this.mainform = isc.Label.create({
      height: 1,
      width: 300,
      overflow: 'visible',
      align: "center",
      valign: "center",
      contents: '¿Procesar consolidación de despachos? '
    });

    this.okButton = isc.OBFormButton.create({
      title: 'Aceptar',
      popup: this,
      action: function () {
        var callback = function (rpcResponse, data, rpcRequest) {

          var status = rpcResponse.status,
          view = rpcRequest.clientContext.originalView.getView('98C2400F56ED43FFB112C7EA1C988570'); //tabid
      
          if (data.status == 'OK') {
            view.messageBar.setMessage('success', null, 'Consolidación de despachos procesada con éxito');
          } else {
            view.messageBar.setMessage('error', null,'Error al intentar procesar consolidación de despachos:<br>'+data.message );
          }

          rpcRequest.clientContext.popup.closeClick();
          rpcRequest.clientContext.originalView.refresh(false, false);
          // rpcRequest.clientContext.popup.params.button.contextView.viewGrid.refreshGrid();
          
        };

        OB.RemoteCallManager.call(this.popup.actionHandler, {
          rows: this.popup.rows,
        }, {}, callback, { 
          originalView: this.popup.view,
          popup: this.popup });
      }
    });

    this.cancelButton = isc.OBFormButton.create({
      title: 'Cancelar',
      popup: this,
      action: function () {
        this.popup.closeClick();
      }
    });

    this.items = [
      isc.VLayout.create({
        defaultLayoutAlign: "center",
        align: "center",
        width: "100%",
        layoutMargin: 10,
        membersMargin: 6,
        members: [
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            layoutMargin: 30,
            membersMargin: 6,
            members: this.mainform
          }),
          isc.HLayout.create({
            defaultLayoutAlign: "center",
            align: "center",
            membersMargin: 10,
            members: [this.okButton, this.cancelButton]
          })
        ]
      })
    ];

    this.Super('initWidget', arguments);
  }

});

OB.OBEXAPP = OB.OBEXAPP || {};
OB.OBEXAPP.SSIPOTM = {
  loadLines: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(), rows = [];

    for (i = 0; i < selection.length; i++) {
      rows.push(selection[i][OB.Constants.ID]);
    };

    isc.SSIPOTM_LoadLinesPopup.create({
      view: view,
      params: params,
      actionHandler: 'ec.com.sidesoft.inventory.partial.out.movement.ad_actions.LoadLinesActionHandler',
      rows: rows,
    }).show();
  },

  accumulate: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(), rows = [];

    for (i = 0; i < selection.length; i++) {
      rows.push(selection[i][OB.Constants.ID]);
    };

    isc.SSIPOTM_AccumulatePopup.create({
      view: view,
      params: params,
      actionHandler: 'ec.com.sidesoft.inventory.partial.out.movement.ad_actions.AccumulateActionHandler',
      rows: rows,
    }).show();
  },

  updateStock: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(), rows = [];

    for (i = 0; i < selection.length; i++) {
      rows.push(selection[i][OB.Constants.ID]);
    };

    isc.SSIPOTM_UpdateStockPopup.create({
      view: view,
      params: params,
      actionHandler: 'ec.com.sidesoft.inventory.partial.out.movement.ad_actions.UpdateStockActionHandler',
      rows: rows,
    }).show();
  },

  process: function (params, view) {
    var i, selection = params.button.contextView.viewGrid.getSelectedRecords(), rows = [];

    for (i = 0; i < selection.length; i++) {
      rows.push(selection[i][OB.Constants.ID]);
    };

    isc.SSIPOTM_ProcessPopup.create({
      view: view,
      params: params,
      actionHandler: 'ec.com.sidesoft.inventory.partial.out.movement.ad_actions.ProcessActionHandler',
      rows: rows,
    }).show();
//    params.button.contextView.viewGrid.refreshGrid();
  },
};