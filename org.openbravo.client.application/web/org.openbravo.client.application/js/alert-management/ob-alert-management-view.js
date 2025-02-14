/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

isc.ClassFactory.defineClass('OBUIAPP_AlertManagement', isc.VLayout);
isc.OBUIAPP_AlertManagement.addProperties({
  // Allow only one instance of the view.
  isSameTab: function (viewId, params) {
    return viewId === 'OBUIAPP_AlertManagement';
  },
  getBookMarkParams: function () {
    var result = {};
    result.viewId = 'OBUIAPP_AlertManagement';
    return result;
  },

  createAlertDataSource: function () {
    return OB.Datasource.create({
      createClassName: '',
      ID: 'DB9F062472294F12A0291A7BD203F922' + '_' + new Date().getTime(),
      potentiallyShared: true,
      dataURL: OB.Utilities.applicationUrl('org.openbravo.service.datasource/DB9F062472294F12A0291A7BD203F922'),
      requestProperties: {
        params: {
          _contextUrl: OB.Utilities.getLocationUrlWithoutFragment(),
          _skinVersion: 'Default',
          Constants_IDENTIFIER: OB.Constants.IDENTIFIER,
          Constants_FIELDSEPARATOR: OB.Constants.FIELDSEPARATOR,
          _new: 'true',
          _create: 'true'
        }
      },
      fields: [{
        name: 'id',
        type: '_id_13',
        primaryKey: true
      }, {
        name: 'creationDate',
        type: '_id_16'
      }, {
        name: 'description',
        type: '_id_14'
      }, {
        name: 'alertRule',
        type: '_id_19'
      }, {
        name: 'comments',
        type: '_id_14'
      }, {
        name: 'recordID',
        type: '_id_10'
      }]
    });
  },

  translatedStatus: {
    New: '',
    Acknowledged: '',
    Suppressed: '',
    Solved: ''
  },
  sectionStack: null,
  grids: {},
  sections: {},
  //Section header controls:
  NewAcknowledged: null,
  NewSuppressed: null,
  AcknowledgedNew: null,
  AcknowledgedSuppressed: null,
  SuppressedNew: null,
  SuppressedAcknowledged: null,

  initWidget: function () {
    OB.AlertManagement = this;
    OB.I18N.getLabel('AlertStatus_New', null, {
      setTranslatedStatus: function (label) {
        OB.AlertManagement.translatedStatus.New = label;
        OB.AlertManagement.checkInitializeStatus();
      }
    }, 'setTranslatedStatus');
    OB.I18N.getLabel('AlertStatus_Acknowledged', null, {
      setTranslatedStatus: function (label) {
        OB.AlertManagement.translatedStatus.Acknowledged = label;
        OB.AlertManagement.checkInitializeStatus();
      }
    }, 'setTranslatedStatus');
    OB.I18N.getLabel('AlertStatus_Suppressed', null, {
      setTranslatedStatus: function (label) {
        OB.AlertManagement.translatedStatus.Suppressed = label;
        OB.AlertManagement.checkInitializeStatus();
      }
    }, 'setTranslatedStatus');
    OB.I18N.getLabel('AlertStatus_Solved', null, {
      setTranslatedStatus: function (label) {
        OB.AlertManagement.translatedStatus.Solved = label;
        OB.AlertManagement.checkInitializeStatus();
      }
    }, 'setTranslatedStatus');

    this.refreshButton = isc.OBToolbarIconButton.create(isc.OBToolbar.REFRESH_BUTTON_PROPERTIES);

    this.addMember(isc.OBToolbar.create({
      view: this,
      leftMembers: [this.refreshButton],
      rightMembers: []
    }));

    this.sectionStack = isc.OBSectionStack.create();
    this.addMember(this.sectionStack);

    this.Super('initWidget', arguments);
    OB.AlertManagement.checkInitializeStatus();
  },

  tabSelected: function (tabNum, tabPane, ID, tab) {
    if (OB.AlertManagement.grids.New) {
      // TODO: Add a more sophisticated focus logic to ensure each time the Alert Management is selected,
      // the previous focused item is selected instead of the first grid filter editor
      setTimeout(function () {
        OB.AlertManagement.grids.New.focusInFirstFilterEditor();
      }, 10);
    }
  },

  checkInitializeStatus: function () {
    if (OB.AlertManagement.translatedStatus.New !== '' && OB.AlertManagement.translatedStatus.Acknowledged !== '' && OB.AlertManagement.translatedStatus.Suppressed !== '' && OB.AlertManagement.translatedStatus.Solved !== '' && OB.AlertManagement.sectionStack) {
      // Sections are created after status translations are set.
      // This is needed to be able to filter properly the grids of the sections.
      OB.AlertManagement.createSections();
    }
  },

  createSections: function () {
    this.grids.New = isc.OBAlertGrid.create({
      alertStatus: 'New'
    });

    this.NewAcknowledged = isc.OBAlertSectionStackControl.create({
      currentStatus: 'New',
      newStatus: 'Acknowledged',
      ID: 'NewAcknowledged'
    });
    this.NewSuppressed = isc.OBAlertSectionStackControl.create({
      currentStatus: 'New',
      newStatus: 'Suppressed',
      ID: 'NewSuppressed'
    });
    this.sections.New = {
      title: this.translatedStatus.New,
      alertStatus: 'New',
      expanded: true,
      items: [this.grids.New],
      controls: [this.NewAcknowledged, this.NewSuppressed]
    };
    this.sectionStack.addSection(this.sections.New);

    this.grids.Acknowledged = isc.OBAlertGrid.create({
      alertStatus: 'Acknowledged'
    });
    this.AcknowledgedNew = isc.OBAlertSectionStackControl.create({
      currentStatus: 'Acknowledged',
      newStatus: 'New',
      ID: 'AcknowledgedNew'
    });
    this.AcknowledgedSuppressed = isc.OBAlertSectionStackControl.create({
      currentStatus: 'Acknowledged',
      newStatus: 'Suppressed',
      ID: 'AcknowledgedSuppressed'
    });
    this.sections.Acknowledged = {
      title: this.translatedStatus.Acknowledged,
      alertStatus: 'Acknowledged',
      expanded: true,
      items: [this.grids.Acknowledged],
      controls: [this.AcknowledgedNew, this.AcknowledgedSuppressed]
    };
    this.sectionStack.addSection(this.sections.Acknowledged);

    this.grids.Suppressed = isc.OBAlertGrid.create({
      alertStatus: 'Suppressed'
    });
    this.SuppressedNew = isc.OBAlertSectionStackControl.create({
      currentStatus: 'Suppressed',
      newStatus: 'New',
      ID: 'SuppressedNew'
    });
    this.SuppressedAcknowledged = isc.OBAlertSectionStackControl.create({
      currentStatus: 'Suppressed',
      newStatus: 'Acknowledged',
      ID: 'SuppressedAcknowledged'
    });
    this.sections.Suppressed = {
      title: this.translatedStatus.Suppressed,
      alertStatus: 'Suppressed',
      expanded: false,
      items: [this.grids.Suppressed],
      controls: [this.SuppressedNew, this.SuppressedAcknowledged]
    };
    this.sectionStack.addSection(this.sections.Suppressed);

    this.grids.Solved = isc.OBAlertGrid.create({
      alertStatus: 'Solved'
    });
    this.sections.Solved = {
      title: this.translatedStatus.Solved,
      alertStatus: 'Solved',
      expanded: false,
      items: [this.grids.Solved]
    };
    this.sectionStack.addSection(this.sections.Solved);

    this.grids.New.setDataSource(this.createAlertDataSource());
    this.grids.Acknowledged.setDataSource(this.createAlertDataSource());
    this.grids.Suppressed.setDataSource(this.createAlertDataSource());
    this.grids.Solved.setDataSource(this.createAlertDataSource());
  },

  destroy: function () {
    if (this.grids) {
      if (this.grids.New) {
        this.grids.New.destroy();
        this.grids.New = null;
      }
      if (this.grids.Acknowledged) {
        this.grids.Acknowledged.destroy();
        this.grids.Acknowledged = null;
      }
      if (this.grids.Suppressed) {
        this.grids.Suppressed.destroy();
        this.grids.Suppressed = null;
      }
      if (this.grids.Solved) {
        this.grids.Solved.destroy();
        this.grids.Solved = null;
      }
    }
    if (this.NewAcknowledged) {
      this.NewAcknowledged.destroy();
      this.NewAcknowledged = null;
    }
    if (this.NewSuppressed) {
      this.NewSuppressed.destroy();
      this.NewSuppressed = null;
    }
    if (this.AcknowledgedNew) {
      this.AcknowledgedNew.destroy();
      this.AcknowledgedNew = null;
    }
    if (this.AcknowledgedSuppressed) {
      this.AcknowledgedSuppressed.destroy();
      this.AcknowledgedSuppressed = null;
    }
    if (this.SuppressedNew) {
      this.SuppressedNew.destroy();
      this.SuppressedNew = null;
    }
    if (this.SuppressedAcknowledged) {
      this.SuppressedAcknowledged.destroy();
      this.SuppressedAcknowledged = null;
    }
    this.Super('destroy', arguments);
  },

  moveToStatus: function (alertIDs, oldStatus, newStatus) {
    var post = {
      'eventType': 'moveToStatus',
      'oldStatus': oldStatus,
      'newStatus': newStatus,
      'alertIDs': alertIDs
    };

    // persist not saved editions before moving
    this.grids[oldStatus].saveAllEdits();

    OB.RemoteCallManager.call('org.openbravo.client.application.AlertManagementActionHandler', post, {}, function (rpcResponse, data, rpcRequest) {
      OB.AlertManagement.grids[data.newStatus].invalidateCache();
      // If section has not been expanded the grid is not reloaded so the total rows is not updated.
      if (!OB.AlertManagement.sections[data.newStatus].expanded) {
        OB.AlertManagement.grids[data.newStatus].getGridTotalRows();
      }
      // Old status is always expanded to be able to select the rows
      OB.AlertManagement.grids[data.oldStatus].invalidateCache();
      OB.AlertManager.call();
    });
  },

  setTotalRows: function (totalRows, status) {
    if (OB.AlertManagement.sections[status]) {
      if (OB.AlertManagement.grids[status] && OB.AlertManagement.grids[status].dataPageSize < totalRows) {
        totalRows = '>' + OB.AlertManagement.grids[status].dataPageSize;
      }
      OB.AlertManagement.sections[status].getSectionHeader().setTitle(OB.I18N.getLabel('OBUIAPP_AlertSectionHeader', [OB.AlertManagement.translatedStatus[status], totalRows]));
    }
  },

  refresh: function () {
    var i, section, alertStatus = ['New', 'Acknowledged', 'Suppressed', 'Solved'];
    for (i = 0; i < 4; i++) {
      OB.AlertManagement.grids[alertStatus[i]].invalidateCache();
      section = OB.AlertManagement.sections[alertStatus[i]];
      // force to refresh collapsed grids
      if (!section.getSectionHeader().expanded) {
        OB.AlertManagement.grids[alertStatus[i]].getTotalRowsForAlert();
      }
      if (OB.AlertManagement.grids[alertStatus[i]].isDrawn()) {
        OB.AlertManagement.grids[alertStatus[i]].isRefreshing = true;
      }
    }
  },

  notifyRefreshEnd: function () {
    var i, alertStatus = ['New', 'Acknowledged', 'Suppressed', 'Solved'];
    for (i = 0; i < 4; i++) {
      if (OB.AlertManagement.grids[alertStatus[i]].isRefreshing) {
        return;
      }
    }
    if (this.isRefreshing) {
      delete this.isRefreshing;
      this.refreshButton.setDisabled(false);
    }
  }
});

isc.ClassFactory.defineClass('OBAlertSectionStackControl', isc.OBLinkButtonItem);
isc.OBAlertSectionStackControl.addProperties({
  newStatus: null,
  currentStatus: null,

  initWidget: function () {
    this.setTitle("[ " + OB.I18N.getLabel('OBUIAPP_MoveSelectedToStatus', [OB.AlertManagement.translatedStatus[this.newStatus]]) + " ]");
    this.Super('initWidget', arguments);
  },

  action: function () {
    var i, alerts = '',
        selectedAlerts = OB.AlertManagement.grids[this.currentStatus].getSelection(),
        selAlertsLength = selectedAlerts.length;
    if (selAlertsLength === 0) {
      return;
    }
    for (i = 0; i < selAlertsLength; i++) {
      if (alerts !== '') {
        alerts += ',';
      }
      alerts += selectedAlerts[i].id;
    }
    OB.AlertManagement.moveToStatus(alerts, this.currentStatus, this.newStatus);
  }
});