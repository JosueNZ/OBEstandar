OB.SSFI = OB.SSFI || {};

var SSFI_popUp;

OB.SSFI.ReconciliationDetail = {
  generateReport: function (params, view) {
    var selection = params.button.contextView.viewGrid.getSelectedRecord();
    SSFI_popUp = isc.SSFI_ReconciliationDetailView.create({
      parentWindow: view,
      selectedRecord: selection
    });
    SSFI_popUp.show();
  }
};

OB.SSFI.closePopUp = function (success) {
  var messageBar = SSFI_popUp.parentWindow.view.messageBar;
  SSFI_popUp.loading.hide();
  SSFI_popUp.closeClick();
  if (success) {
    messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, 'Proceso Exitoso.', 'Reporte Generado');
  } else {
    messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, 'Error', 'Reporte No Generado');
  }
};

isc.defineClass('SSFI_ReconciliationDetailView', isc.OBPopup);

SSFI_ReconciliationDetailView.addProperties({
  title: 'Detalle de Reconciliación',
  width: '40%',
  height: '*',
  parentWindow: '',
  selectedRecord: '',
  importOrExportOptions: '',
  fileToImport: '',
  dataForm: null,
  processButton: null,
  cancelButton: null,
  toolBar: null,
  initWidget: function () {
    var view = this;
    // Create form
    this.dataForm = isc.OBViewForm.create({
      ID: 'optionsForm',
      width: '70%',
      height: '50%',
      titleOrientation: 'left',
      numCols: 1,
      fields: [
      {
        title: 'Saldo Banco',
        id: 'bankBalance',
        name: 'inpBankBalance',
        type: '_id_12' //Amount Reference
      }],
      target: 'background_target',
      action: 'com.sidesoft.localization.ecuador.finances.ad_process/ReconciliationDetailReport.html',
      redraw: function () {}
    });

    // Create process button
    this.processButton = isc.OBFormButton.create({
      title: 'Procesar',
      showFocused: true,
      align: 'center',
      showFocusedAsOver: true,
      click: function () {
        view.dataForm.submitForm();
        view.loading.show();
        OB.SSFI.closePopUp(true);
      }
    });

    // Create cancel button
    this.cancelButton = isc.OBFormButton.create({
      title: 'Cancelar',
      showFocused: true,
      showFocusedAsOver: true,
      click: function () {
        view.closeClick();
        view.parentWindow.refresh();
      }
    });

    this.firstFocusedItem = this.processButton;

    // Create button layout
    this.buttonLayout = isc.HLayout.create({
      ID: "importOrExportActionButtonsLayout",
      align: 'center',
      width: '*',
      height: '*',
      overflow: 'visible',
      height: OB.Styles.Process.PickAndExecute.buttonLayoutHeight,
      members: [isc.HLayout.create({
        width: 1,
        overflow: 'visible',
        styleName: this.buttonBarStyleName,
        height: this.buttonBarHeight,
        defaultLayoutAlign: 'center',
        members: [this.processButton, isc.LayoutSpacer.create({
          width: 32
        }), this.cancelButton]
      })]
    });

    this.loading = OB.Utilities.createLoadingLayout(OB.I18N.getLabel('OBUIAPP_PROCESSING'));
    this.loading.hide();

    // Vertical layout
    this.mainLayout = isc.VLayout.create({
      ID: "vLayoutLayoutSpacers",
      autoDraw: false,
      height: "50%",
      width: "100%",
      layoutMargin: 0,
      defaultLayoutAlign: "center",
      members: [this.dataForm, this.loading, this.buttonLayout]
    });

    this.items = [this.mainLayout];
    this.Super("initWidget", arguments);
  }
});