OB.SSOREL = {};
OB.SSOREL.OnChangeFunctions = {};
OB.SSOREL.OnChangeFunctions.invoiceFilter = function(item, view, form, grid) {
  var invoiceLinesGrid = form.getItem('invoice_lines').canvas.viewGrid, currentCriteria = invoiceLinesGrid
	    .getCriteria(), criteria;
		
	currentCriteria.criteria[0].value = item.getSelectedRecord().id;
	invoiceLinesGrid.invalidateCache();
    form.redraw();  
}

OB.SSOREL.OnChangeFunctions.createObjectCriteria = function(value, fieldName) {
	var objectCriteria = {}, operationOnFieldValue = [];

	objectCriteria._constructor = 'AdvancedCriteria';
	objectCriteria.operator = 'and';
	operationOnFieldValue.push({
	  fieldName : fieldName,
	  operator : 'equals',
	  value : value
	});
	objectCriteria.criteria = operationOnFieldValue;
	return objectCriteria;
}

OB.SSOREL.onLoad = function(view) {
	if (view.popup) {
		OB.SSOREL.theForm = view.theForm;		
		currentCriteria = OB.SSOREL.OnChangeFunctions.createObjectCriteria('-1', "invId");		
		var invoiceLinesGrid = view.theForm.getItem('invoice_lines').canvas.viewGrid;
		invoiceLinesGrid.setCriteria(currentCriteria);// update the object criteria at the
		invoiceLinesGrid.redraw();	
	}	
}

OB.SSOREL.onLoadGrid = function (grid) {
	if (!grid.isReady) {
        var currentCriteria = "";
		if(OB.SSOREL.theForm.getItem('invoiceReference').getValue() !== undefined){
			currentCriteria = OB.SSOREL.OnChangeFunctions.createObjectCriteria(OB.SSOREL.theForm.getItem('invoiceReference').getValue(), "invId");
		} else {
			currentCriteria = OB.SSOREL.OnChangeFunctions.createObjectCriteria('-1', "invId");
		}
		
		grid.setCriteria(currentCriteria);// update the object criteria at the
		grid.redraw();
	}
	grid.isReady = true; 
}

