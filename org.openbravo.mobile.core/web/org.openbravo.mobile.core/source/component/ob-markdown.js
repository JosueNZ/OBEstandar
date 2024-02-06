/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, OB, markdown */

enyo.kind({
  name: 'OB.UI.MarkdownLabel',
  generateInnerHtml: function () {
    return markdown.toHTML(this.content);
  },
  initComponents: function () {
    if (this.i18nLabel) {
      this.setContent(OB.I18N.getLabel(this.i18nLabel));
    }
  }
});