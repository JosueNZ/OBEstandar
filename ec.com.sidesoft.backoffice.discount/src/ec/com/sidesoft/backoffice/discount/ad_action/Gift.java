package ec.com.sidesoft.backoffice.discount.ad_action;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.provider.OBProvider;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.xmlEngine.XmlDocument;
import ec.com.sidesoft.backoffice.discount.dao.MethodsDao;
import ec.com.sidesoft.backoffice.discount.SsbodGiftInvoice;
import ec.com.sidesoft.backoffice.discount.SsbodGiftOrder;
import ec.com.sidesoft.backoffice.discount.SsbodGiftTemp;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.AttributeSetInstance;

public class Gift extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  VariablesSecureApp vars;
  OBError msg = new OBError();

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    vars = new VariablesSecureApp(request);
    Long cant = new Long(0);
    String lineId = "";
    String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Reconciliation|Org");
    String strWindowId = vars.getRequestGlobalVariable("inpwindowId", "Reconciliation|windowId");
    String strTabId = vars.getRequestGlobalVariable("inpTabId", "Reconciliation|tabId");
    if (strTabId.equals("187")) {
      lineId = vars.getSessionValue("143|C_ORDERLINE_ID");
    } else if (strTabId.equals("270")) {
      lineId = vars.getSessionValue("167|C_INVOICELINE_ID");
    }

    List<PriceAdjustment> promociones = getPromotions(lineId, strTabId);
    for (PriceAdjustment promocion : promociones) {
      if (promocion.isSsbodPerunit()) {
        Long cantLinea = new Long(1);
        if (strTabId.equals("187")) {
          OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, lineId);
          cantLinea = orderLine.getOrderedQuantity().longValue();
        } else if (strTabId.equals("270")) {
          InvoiceLine invoiceLine = OBDal.getInstance().get(InvoiceLine.class, lineId);
          cantLinea = invoiceLine.getInvoicedQuantity().longValue();
        }
        cant = cant + promocion.getSsbodGiftNumber() * cantLinea;
      } else {
        cant = cant + promocion.getSsbodGiftNumber();
      }
    }

    if (vars.commandIn("DEFAULT")) {
      clear(strTabId, lineId);
      // loadGifts(lineId, strTabId);
      printPage(response, vars, strOrgId, strWindowId, strTabId, lineId, null, null,
          getSelected(strTabId, lineId), true, "C", cant);

    } else if (vars.commandIn("ADD")) {
      boolean status;
      String strProductId = vars.getStringParameter("inpmProductId");
      String strLocatorId = vars.getStringParameter("inpmProductId_LOC");
      String strAttributeId = vars.getStringParameter("inpmProductId_ATR");
      if (strProductId == null) {
        status = false;
      } else {
        status = addProduct(strProductId, lineId, strTabId, vars.getClient(), vars.getUser(),
            strLocatorId, strAttributeId);
      }
      printPage(response, vars, strOrgId, strWindowId, strTabId, lineId, null, null,
          getSelected(strTabId, lineId), status, "A", cant);

    } else if (vars.commandIn("REMOVESTD")) {
      try {
        OBContext.setAdminMode();
        String strSelectedTransId = vars.getStringParameter("inpCurrentTransIdSelected", "");
        String strSelectedCheck = vars.getStringParameter("inpIsCurrentTransSelected", "");
        List<SsbodGiftTemp> seleccionados = getSelected(strTabId, lineId);
        if (strSelectedCheck.equals("true")) {
          seleccionados.get(Integer.parseInt(strSelectedTransId)).setChecked(true);
        } else {
          seleccionados.get(Integer.parseInt(strSelectedTransId)).setChecked(false);
        }
        OBDal.getInstance().save(seleccionados.get(Integer.parseInt(strSelectedTransId)));
        OBDal.getInstance().flush();
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        OBContext.restorePreviousMode();
      }
    } else if (vars.commandIn("PROCESS")) {
      try {
        OBContext.setAdminMode();
        List<SsbodGiftTemp> seleccionados = getSelected(strTabId, lineId);
        if (seleccionados.size() > 0) {
          if (seleccionados.size() == cant) {
            addGift(seleccionados, lineId, strTabId);
            msg = new OBError();
            msg.setType("Success");
            msg.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
            msg.setMessage(
                Utility.parseTranslation(this, vars, vars.getLanguage(), "@ecsfq_add_quotation@"));
            vars.setMessage(strTabId, msg);
            msg = null;
            clear(strTabId, lineId);
            printPageClosePopUpAndRefreshParent(response, vars);
          } else {
            throw new Exception("Cantidad seleccionada incorrecta");
          }
        } else {
          throw new Exception("Ninguno seleccionado");
        }
      } catch (Exception ex) {
        msg = new OBError();
        msg.setType("Error");
        msg.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
        msg.setMessage(
            Utility.parseTranslation(this, vars, vars.getLanguage(), "@ssbod_not_equals_numbers@"));
        printPage(response, vars, strOrgId, strWindowId, strTabId, lineId, null, null,
            getSelected(strTabId, lineId), false, "A", cant);
      } finally {
        OBContext.restorePreviousMode();
      }
    } else if (vars.commandIn("REMOVE")) {
      List<SsbodGiftTemp> seleccionados = getSelected(strTabId, lineId);
      try {
        OBContext.setAdminMode();
        if (seleccionados.size() > 0) {
          for (SsbodGiftTemp temp : seleccionados) {
            if (temp.isChecked()) {
              OBDal.getInstance().remove(temp);
            }
          }
          OBDal.getInstance().flush();
        } else {
          throw new Exception("Ninguno seleccionado");
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        OBContext.restorePreviousMode();
      }
      printPage(response, vars, strOrgId, strWindowId, strTabId, lineId, null, null,
          getSelected(strTabId, lineId), true, "A", cant);
    } else if (vars.commandIn("GRID")) {
      printGrid(response, getSelected(strTabId, lineId));
    } else if (vars.commandIn("CLOSE")) {
      clear(strTabId, lineId);
    }
  }

  private List<SsbodGiftTemp> getSelected(String strTabId, String lineId) {
    List<SsbodGiftTemp> seleccionados = new ArrayList<SsbodGiftTemp>();
    try {
      OBContext.setAdminMode();
      OBCriteria<SsbodGiftTemp> giftTempCrt = OBDal.getInstance()
          .createCriteria(SsbodGiftTemp.class);
      giftTempCrt.add(Restrictions.eq(SsbodGiftTemp.PROPERTY_TAB, strTabId));
      giftTempCrt.add(Restrictions.eq(SsbodGiftTemp.PROPERTY_RECORD, lineId));
      // giftTempCrt.add(Restrictions.eq(SsbodGiftTemp.PROPERTY_CLIENT, vars.getClient()));
      // giftTempCrt.add(Restrictions.eq(SsbodGiftTemp.PROPERTY_ORGANIZATION, strOrgId));
      if (giftTempCrt.count() > 0) {
        seleccionados = giftTempCrt.list();
      } else {
        throw new Exception("Ninguno seleccionado");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return seleccionados;
  }

  private void clear(String strTabId, String lineId) {
    try {
      OBContext.setAdminMode();
      List<SsbodGiftTemp> seleccionados = getSelected(strTabId, lineId);
      for (SsbodGiftTemp temp : seleccionados) {
        OBDal.getInstance().remove(temp);
      }
      OBDal.getInstance().flush();
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private boolean addProduct(String productId, String recordId, String tabId, String clientId,
      String userId, String strLocatorId, String attributeId) {

    try {
      OBContext.setAdminMode();
      SsbodGiftTemp temp = OBProvider.getInstance().get(SsbodGiftTemp.class);
      Product producto = OBDal.getInstance().get(Product.class, productId);
      Locator locator = OBDal.getInstance().get(Locator.class, strLocatorId);
      Warehouse warehouse = locator.getWarehouse();
      AttributeSetInstance attribute = OBDal.getInstance().get(AttributeSetInstance.class,
          attributeId);

      if (producto == null) {
        return false;
      } else {
        Client cliente = OBDal.getInstance().get(Client.class, clientId);
        temp.setProduct(producto);
        temp.setTab(tabId);
        temp.setRecord(recordId);
        temp.setClient(cliente);
        temp.setChecked(false);
        temp.setWarehouse(warehouse);
        temp.setAttributeSetValue(attribute);
        OBDal.getInstance().save(temp);
        OBDal.getInstance().flush();
      }
      return true;
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  private void addGift(List<SsbodGiftTemp> regalos, String lineId, String tabId)
      throws IOException, ServletException {
    OBContext.setAdminMode();
    if (tabId.equals("187")) {
      OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, lineId);
      OBCriteria<SsbodGiftOrder> crtGrifts = OBDal.getInstance()
          .createCriteria(SsbodGiftOrder.class);
      crtGrifts.add(Restrictions.eq(SsbodGiftOrder.PROPERTY_ORDERLINE, orderLine));
      if (crtGrifts.count() > 0) {
        List<SsbodGiftOrder> existentes = crtGrifts.list();
        for (SsbodGiftOrder temp : existentes) {
          OBDal.getInstance().remove(temp);
          OBDal.getInstance().flush();
        }
      }
      for (SsbodGiftTemp regalo : regalos) {
        SsbodGiftOrder orderGift = OBProvider.getInstance().get(SsbodGiftOrder.class);
        orderGift.setProduct(regalo.getProduct());
        orderGift.setOrderline(orderLine);
        orderGift.setWarehouse(regalo.getWarehouse());
        orderGift.setAttributeSetValue(regalo.getAttributeSetValue());
        OBDal.getInstance().save(orderGift);
        OBDal.getInstance().flush();
      }
    } else if (tabId.equals("270")) {
      InvoiceLine invoiceLine = OBDal.getInstance().get(InvoiceLine.class, lineId);
      OBCriteria<SsbodGiftInvoice> crtGrifts = OBDal.getInstance()
          .createCriteria(SsbodGiftInvoice.class);
      crtGrifts.add(Restrictions.eq(SsbodGiftInvoice.PROPERTY_INVOICELINE, invoiceLine));
      if (crtGrifts.count() > 0) {
        List<SsbodGiftInvoice> existentes = crtGrifts.list();
        for (SsbodGiftInvoice temp : existentes) {
          OBDal.getInstance().remove(temp);
          OBDal.getInstance().flush();
        }
      }
      for (SsbodGiftTemp regalo : regalos) {
        SsbodGiftInvoice invoiceGift = OBProvider.getInstance().get(SsbodGiftInvoice.class);
        invoiceGift.setProduct(regalo.getProduct());
        invoiceGift.setInvoiceline(invoiceLine);
        OBDal.getInstance().save(invoiceGift);
        OBDal.getInstance().flush();
      }
    }
  }

  public static List<PriceAdjustment> getPromotions(String lineId, String tabId) {
    List<PriceAdjustment> applicableGifts = null;
    try {
      OBContext.setAdminMode();
      BaseOBObject orderOrInvoice = null;
      Product product = null;
      BigDecimal qty = BigDecimal.ZERO;
      if (tabId.equals("187")) {
        OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, lineId);
        orderOrInvoice = OBDal.getInstance().get(Order.class, orderLine.getSalesOrder().getId());
        product = OBDal.getInstance().get(Product.class, orderLine.getProduct().getId());
        qty = orderLine.getOrderedQuantity();
      } else if (tabId.equals("270")) {
        InvoiceLine invoiceLine = OBDal.getInstance().get(InvoiceLine.class, lineId);
        orderOrInvoice = OBDal.getInstance().get(Invoice.class, invoiceLine.getInvoice().getId());
        product = OBDal.getInstance().get(Product.class, invoiceLine.getProduct().getId());
        qty = invoiceLine.getInvoicedQuantity();
      }
      applicableGifts = ec.com.sidesoft.backoffice.discount.businessUtility.PriceAdjustment
          .getApplicablePriceAdjustments(orderOrInvoice, qty, product, false,
              "999543ACA29B448E9304A23844A2F040");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return applicableGifts;
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strOrgId,
      String strWindowId, String strTabId, String lineId, String strStatementDate,
      String strEndBalance, List<SsbodGiftTemp> regalos, boolean status, String action, Long cant)
      throws IOException, ServletException {
    try {
      OBContext.setAdminMode();
      XmlDocument xmlDocument = xmlEngine
          .readXmlTemplate("ec/com/sidesoft/backoffice/discount/ad_action/Gift")
          .createXmlDocument();

      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
      xmlDocument.setParameter("theme", vars.getTheme());

      xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("mainDate", DateTimeData.today(this));
      xmlDocument.setParameter("windowId", strWindowId);
      xmlDocument.setParameter("tabId", strTabId);
      xmlDocument.setParameter("orgId", strOrgId);
      xmlDocument.setParameter("lineId", lineId);

      BigDecimal currentEndBalance = BigDecimal.ZERO;
      if (vars.commandIn("PROCESS")) {
        xmlDocument.setParameter("statementDate", strStatementDate);
        xmlDocument.setParameter("endBalance", strEndBalance);
        xmlDocument.setParameter("calcEndingBalance", strEndBalance);

      } else {
        String currentStatementDate = DateTimeData.today(this);
        xmlDocument.setParameter("statementDate", currentStatementDate);
        xmlDocument.setParameter("endBalance", null);
        xmlDocument.setParameter("calcEndingBalance", null);
      }

      BigDecimal beginBalance = BigDecimal.ZERO;

      xmlDocument.setParameter("account", "");
      xmlDocument.setParameter("beginBalance", "");

      xmlDocument.setParameter("inpRegalos", cant.toString());

      // Hidden inputs
      xmlDocument.setParameter("calcBeginningBalance", beginBalance.toString());
      xmlDocument.setParameter("calcTotalPayment", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcTotalDeposit", BigDecimal.ZERO.toString());
      xmlDocument.setParameter("calcDifferenceToClear",
          currentEndBalance.subtract(beginBalance).toString());
      xmlDocument.setParameter("calcCurrentlyCleared", "0.00");
      xmlDocument.setParameter("calcDifference", "0.00");
      OBContext.setAdminMode();
      xmlDocument.setParameter("precision", "0.00");
      // OBError msg = new OBError();;
      OBError myMessage = vars.getMessage(strWindowId);

      vars.removeMessage(strWindowId);
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }

      if (!status && action.equals("A")) {
        if (msg.getType() == null)
          msg.setType("Info");
        if (msg.getTitle() == null)
          msg.setTitle(Utility.messageBD(this, "Info", vars.getLanguage()));
        if (msg.getMessage() == null)
          msg.setMessage(
              Utility.parseTranslation(this, vars, vars.getLanguage(), "@ecsfq_payment_invalid@"));

        xmlDocument.setParameter("messageType", msg.getType());
        xmlDocument.setParameter("messageTitle", msg.getTitle());
        xmlDocument.setParameter("messageMessage", msg.getMessage());
      }
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.println(xmlDocument.print());
      out.close();
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void printGrid(HttpServletResponse response, List<SsbodGiftTemp> giftList)
      throws IOException, ServletException {
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("ec/com/sidesoft/backoffice/discount/ad_action/GiftGrid")
        .createXmlDocument();
    FieldProvider[] data = MethodsDao.getTransactionsFiltered(giftList);
    xmlDocument.setData("structure", data);
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "This servlet manages manual transactions reconciliations.";
  }

}