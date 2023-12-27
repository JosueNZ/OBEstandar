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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.mobile.core.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.MimeTypeUtil;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.plm.Product;

/**
 * 
 * This utility class implements a servlet that shows a product image, stored in database based on
 * the product id:
 * http://localhost:8080/openbravo/org.openbravo.mobile.core.productimageprovider?id=x
 * 
 * @author guilleaer
 */
public class ShowProductImage extends HttpSecureAppServlet {

  private static final Logger log = Logger.getLogger(ShowProductImage.class);
  private static final long serialVersionUID = 1L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      ServletException {
    Image img = null;
    VariablesSecureApp vars = new VariablesSecureApp(req);
    String productId = vars.getStringParameter("id");
    try {
      OBContext.setAdminMode(false);
      Product curProduct = OBDal.getInstance().get(Product.class, productId);
      if (curProduct.getImage() != null) {
        img = Utility.getImageObject(curProduct.getImage().getId());

        // read the image data
        byte[] imgByte = img.getBindaryData();

        // write the mimetype which should be JPEG
        String mimeType = img.getMimetype();// write the mimetype
        if (mimeType == null) {
          mimeType = MimeTypeUtil.getInstance().getMimeTypeName(img.getBindaryData());
        }

        if (!mimeType.equals("")) {
          resp.setContentType(mimeType);
        }

        // write the image
        OutputStream out = resp.getOutputStream();
        resp.setContentLength(imgByte.length);
        Calendar inOneMonth = Calendar.getInstance();
        inOneMonth.add(Calendar.MONTH, 1);
        resp.setDateHeader("Expires", inOneMonth.getTimeInMillis());
        out.write(imgByte);
        out.close();
      } else {
        // Not found
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (Exception e) {
      log.error("An error happened when the image for product " + productId + " was retrieved: "
          + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
