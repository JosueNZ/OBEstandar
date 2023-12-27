// Definición de variables Globales de tipo  OB.CSCAMP,  OB.CSCAMP.AddMultiplePayments
// Para el funcionamiento del método onLoad
OB.CSCAMP={};

OB.CSCAMP.AddMultiplePayments = {};

OB.CSCAMP.AddMultiplePayments.onLoad = function (view) {
	

	  /**
	   * Eventos Clik de todo el Grid 
	   * **/
	  
	  // Para identificar el nombre del Grid, buscar el elemento view.theForm.items..

	 view.theForm.getItem('totalamount').setValue(0);
		  
	
	  var invoiceGrid2 = view.theForm.getItem('payments');
	  invoiceGrid2.click = function (form,item) {

 		  //Acumular valores de pagos

		  var v_totalpago=0;
		  
	  
		var itemsAll = view.theForm.getItem('payments').canvas.viewGrid.pneSelectedRecords;

		if (itemsAll.length>0) {
			for (var i=0; i< itemsAll.length;i++) {

				  var pago = itemsAll[i].amount;
			  
				  
				  if (itemsAll[i].obSelected) {
					v_totalpago+=pago;
				  }
			}
		}
		 view.theForm.getItem('totalamount').setValue(v_totalpago);

		   return true;
	  }

	  // Evento por Fila sobre el GRID
	  
	  var invoiceGrid = view.theForm.getItem('payments').canvas.viewGrid;
	  
	  invoiceGrid.onRecordClick = function (viewer, record, recordNum, field, fieldNum, value, rawValue) {
		  
			//console.log("Seleccionado: " + record.obSelected);

			if (!record.obSelected) {

			
				//Este fragmento de código deja en negativo el valor en pocas ocasiones

				var v_totalPago = view.theForm.getItem('totalamount').getValue();

				var v_recordAmountActual = record.amount; 
			
				//console.log("v_totalPago: " + v_totalPago + " - v_recordAmountActual: " + v_recordAmountActual);

				v_totalPago = v_totalPago - v_recordAmountActual;

				if (v_totalPago<0){
					
					v_totalPago=0;
					
				}
				
				view.theForm.getItem('totalamount').setValue(v_totalPago);

			}

		return true;	
	  
	  }

};


