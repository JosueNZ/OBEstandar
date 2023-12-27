/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.client.kernel.RequestContext;

/**
 * A base class implementation of the {@link CoreAppCacheResourceProvider}. A module only needs to
 * create an empty subclass to have all its standard artifacts included in the appcache.
 * 
 * @author mtaal
 */
public abstract class BaseCoreAppCacheResourceProvider implements CoreAppCacheResourceProvider {

  private static final String PATH_PREFIX = "web" + File.separatorChar;

  @Override
  public List<String> getResources() {
    final ArrayList<String> list = new ArrayList<String>();
    list.addAll(getcssFileList());
    list.addAll(getImageFileList());
    list.addAll(getOtherResources());
    return list;
  }

  protected List<String> getOtherResources() {
    return new ArrayList<String>();
  }

  protected List<String> getImageFileList() {
    final String[] extensions = { "png", "gif" };
    return transformPath(getFileList(extensions));
  }

  protected List<String> getcssFileList() {
    final String[] extensions = { "css", "less" };
    return transformPath(getFileList(extensions));
  }

  protected List<String> getFileList(String[] extensions) {

    final String relativePath = "/" + PATH_PREFIX + getModulePackageName();

    List<String> fileList = new ArrayList<String>();

    final File directory = new File(RequestContext.getServletContext().getRealPath(relativePath));

    MobileCoreApplicationCacheComponent.getFileList(fileList, directory, extensions);
    return fileList;
  }

  protected List<String> transformPath(List<String> stringFileList) {
    final List<String> resources = new ArrayList<String>();
    final String relativePath = PATH_PREFIX + getModulePackageName();

    for (final String f : stringFileList) {
      final int pos = f.indexOf(relativePath);
      resources.add("../../" + f.substring(pos));
    }
    return resources;
  }

  protected String getModulePackageName() {
    return this.getClass().getPackage().getName();
  }

}
