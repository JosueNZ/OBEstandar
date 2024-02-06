package com.sidesoft.ecuador.asset.move.accounting;

import org.openbravo.erpCommon.ad_forms.DocLine;

public class DocLine_Alienate extends DocLine {

	public DocLine_Alienate(String DocumentType, String TrxHeader_ID,
			String TrxLine_ID) {
		super(DocumentType, TrxHeader_ID, TrxLine_ID);
		// TODO Auto-generated constructor stub
	}

	public String m_a_asset_id;
	public String m_a_asset_group_id;
	public String m_typereason;
	public String m_assetvalueamt = ZERO.toString();;
	public String m_amortizationvalue = ZERO.toString();;
	public String m_netvalue = ZERO.toString();
	
}
