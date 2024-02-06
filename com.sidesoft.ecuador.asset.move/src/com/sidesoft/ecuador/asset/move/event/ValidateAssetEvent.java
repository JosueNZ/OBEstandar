/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.sidesoft.ecuador.asset.move.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.assetmgmt.AmortizationLine;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;

public class ValidateAssetEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Asset AssetObj = (Asset) event.getTargetInstance();

    OBCriteria<AmortizationLine> ObjAssetList = OBDal.getInstance()
        .createCriteria(AmortizationLine.class);
    ObjAssetList.add(Restrictions.eq(AmortizationLine.PROPERTY_ASSET, AssetObj));
    int intCount = ObjAssetList.count();
    if (intCount > 0) {
      throw new OBException("El Activo no puede ser eliminado porque tiene líneas de Depreciación");
    }

  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Asset AssetObj = (Asset) event.getTargetInstance();
    OBCriteria<AmortizationLine> ObjAssetList = OBDal.getInstance()
        .createCriteria(AmortizationLine.class);
    ObjAssetList.add(Restrictions.eq(AmortizationLine.PROPERTY_ASSET, AssetObj));
    int intCount = ObjAssetList.count();
    if (intCount > 0) {

      final Entity assetEntity = ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME);
      final Property valueProperty = assetEntity.getProperty(Asset.PROPERTY_SSAMSTATUSDEP);

      event.setCurrentState(valueProperty, "PR");

    } else {

      final Entity assetEntity = ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME);
      final Property valueProperty = assetEntity.getProperty(Asset.PROPERTY_SSAMSTATUSDEP);

      event.setCurrentState(valueProperty, "UP");

    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Asset AssetObj = (Asset) event.getTargetInstance();
    OBCriteria<AmortizationLine> ObjAssetList = OBDal.getInstance()
        .createCriteria(AmortizationLine.class);
    ObjAssetList.add(Restrictions.eq(AmortizationLine.PROPERTY_ASSET, AssetObj));
    int intCount = ObjAssetList.count();
    if (intCount > 0) {

      final Entity assetEntity = ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME);
      final Property valueProperty = assetEntity.getProperty(Asset.PROPERTY_SSAMSTATUSDEP);

      event.setCurrentState(valueProperty, "PR");

    } else {

      final Entity assetEntity = ModelProvider.getInstance().getEntity(Asset.ENTITY_NAME);
      final Property valueProperty = assetEntity.getProperty(Asset.PROPERTY_SSAMSTATUSDEP);

      event.setCurrentState(valueProperty, "UP");

    }
  }

}
