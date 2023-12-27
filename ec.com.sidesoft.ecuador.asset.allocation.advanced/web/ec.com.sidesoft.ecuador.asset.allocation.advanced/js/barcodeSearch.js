// = On Change Functions =
//
// Contains on change functions that are defined in the fields.
OB = window.OB || {};
OB.OnChange = window.OB.OnChange || {};

OB.OnChange.searchBarcode = function (item, view, form, grid) {
  var organization = form.getItem('barcode');
 // var gl = form.getItem('AccSchema');
  var callbackGetBarcode;
  callbackGetBarcode = function (response, data, request) {
  /*  gl.valueMap = gl.valueMap || {};
    gl.valueMap[data.response.value] = data.response.identifier;
    gl.setValue(data.response.value);*/
  };

  OB.RemoteCallManager.call('ec.com.sidesoft.ecuador.asset.allocation.advanced.BarcodeActionHandler', {
    organization: organization.getValue()
  }, {}, callbackGetBarcode);
};