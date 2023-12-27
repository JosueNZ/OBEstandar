/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;

/**
 * A class which finds the implementation for a service and executes it.
 * 
 * @author mtaal
 */
public class MobileServiceProcessor {

  private static final Logger log = Logger.getLogger(MobileServiceProcessor.class);

  private boolean bypassImportEntry;
  private boolean synchronizedMode;

  public boolean isSynchronizedMode() {
    return synchronizedMode;
  }

  public void setSynchronizedMode(boolean synchronizedMode) {
    this.synchronizedMode = synchronizedMode;
  }

  public MobileServiceProcessor() {
    bypassImportEntry = OBPropertiesProvider.getInstance().getBooleanProperty(
        "import.bypass.entry.logic");
  }

  /**
   * Executes the JSONProcess implementing the serviceName. The JSONProcesses are sorted by priority
   * and each one can define if it is needed to call the next JSONProcess or not. The response
   * writer is filled with the result of the last executed JSONProcess. Each JSONProcess can
   * determine if the next class has to be executed.
   * 
   * @param w
   *          Writer where the result is stored.
   * @param serviceName
   *          the name of the Mobile Service to execute.
   * @param jsonsent
   *          JSONObject with the Mobile Service input parameters.
   * 
   */
  public void execServiceName(Writer w, String serviceName, JSONObject jsonsent) throws Exception {
    List<? extends JSONProcess> processes = getServiceClassInstances(serviceName);

    for (JSONProcess process : processes) {
      execProcess(w, process, jsonsent);
      if (!(process instanceof SecuredJSONProcess && ((SecuredJSONProcess) process)
          .executeNextServiceClass())) {
        break;
      }
    }
  }

  /**
   * Executes a JSONProcess with the data sent in the JSONObject.
   * 
   * @param w
   *          Writer where the result is stored.
   * @param process
   *          JSONProcess to execute.
   * @param jsonsent
   *          JSONObject with the process input parameters.
   */
  private void execProcess(Writer w, JSONProcess process, JSONObject jsonsent) throws IOException,
      ServletException {

    if (process instanceof SecuredJSONProcess) {
      if (!synchronizedMode && !bypassImportEntry
          && process instanceof DataSynchronizationImportProcess) {
        ((DataSynchronizationImportProcess) process).executeCreateImportEntry(w, jsonsent);
      } else {
        ((SecuredJSONProcess) process).secureExec(w, jsonsent);
      }
    } else {
      log.warn("Executing unsecure process " + process.getClass());
      if (!synchronizedMode && process instanceof DataSynchronizationImportProcess) {
        ((DataSynchronizationImportProcess) process).executeCreateImportEntry(w, jsonsent);
      } else {
        process.exec(w, jsonsent);
      }
    }
  }

  /**
   * Builds a sorted List of instances that implement the serviceName. The implementation is done
   * using the MobileServiceClassSelector. To ensure backwards compatibility in case that exists a
   * class with the same name than the service name and that it does not implement the
   * MobileServiceClassSelector it is also added to the List.
   * 
   * @param serviceName
   *          The Mobile Service name that is going to be executed.
   * @return The sorted list of classes implementing the Mobile Service.
   * @throws ClassNotFoundException
   *           Exception thrown when no class implementing the service name is found.
   */
  public List<? extends JSONProcess> getServiceClassInstances(String serviceName)
      throws ClassNotFoundException {
    BeanManager bm = WeldUtils.getStaticInstanceBeanManager();
    List<JSONProcess> processes = new ArrayList<JSONProcess>();
    boolean isDefaultClassMissing = true;
    Set<Bean<?>> beans = bm.getBeans(JSONProcess.class, WeldUtils.ANY_LITERAL,
        new MobileService.MobileServiceClassSelector(serviceName));
    for (Bean<?> bean : beans) {
      processes.add((JSONProcess) bm.getReference(bean, JSONProcess.class,
          bm.createCreationalContext(bean)));
      if (bean.getBeanClass().getName().equals(serviceName)) {
        isDefaultClassMissing = false;
      }
    }
    // If no bean with classname like the service name is added try to add to the list. This is
    // added for backwards compatibility to include service classes that are not implementing the
    // qualifier.
    try {
      if (isDefaultClassMissing) {
        @SuppressWarnings("unchecked")
        Class<JSONProcess> process = (Class<JSONProcess>) Class.forName(serviceName);
        JSONProcess proc = WeldUtils.getInstanceFromStaticBeanManager(process);

        processes.add(proc);
      }
    } catch (ClassNotFoundException ignore) {
      // Only throw ClassNotFoundException if no JSONProcess is found implementing the service name.
    }

    if (processes.isEmpty()) {
      throw new ClassNotFoundException();
    }
    Collections.sort(processes, new ServiceProcessComparator());

    // set synchronized mode
    for (JSONProcess proc : processes) {
      if (proc instanceof JSONProcessSimple) {
        ((JSONProcessSimple) proc).setRunInSynchronizedMode(synchronizedMode);
      }
    }

    return processes;
  }

  private static class ServiceProcessComparator implements Comparator<JSONProcess> {
    private int getProcessPriority(JSONProcess proc) {
      if (proc instanceof SecuredJSONProcess) {
        return ((SecuredJSONProcess) proc).getPriority();
      }
      return 100;
    }

    @Override
    public int compare(JSONProcess proc1, JSONProcess proc2) {
      return getProcessPriority(proc1) - getProcessPriority(proc2);
    }

  }
}
