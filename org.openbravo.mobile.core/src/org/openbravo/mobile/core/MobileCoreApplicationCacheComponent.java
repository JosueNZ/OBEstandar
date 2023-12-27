/*
 ************************************************************************************
 * Copyright (C) 2012-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;

/**
 * 
 * @author iperdomo
 */

@RequestScoped
public class MobileCoreApplicationCacheComponent extends BaseTemplateComponent {

  private static final String PATH_PREFIX = "web" + File.separatorChar;

  private static final String TEMPLATE_ID = "7A911850D921448EA5AC2E6F4D5FDE2D";

  @Inject
  private LibraryResource libraryResource;

  @Inject
  private MobileStaticResourceComponent mobileStaticResourceComponent;

  @Inject
  @Any
  private Instance<CoreAppCacheResourceProvider> resourceProviders;

  private String version = null;

  private static Map<String, String> manifestCache = new ConcurrentHashMap<String, String>();

  @Override
  protected Template getComponentTemplate() {
    return OBDal.getInstance().get(Template.class, TEMPLATE_ID);
  }

  @Override
  public String generate() {
    String appName = getParameters().containsKey("_appName") ? (String) getParameters().get(
        "_appName") : MobileCoreConstants.RETAIL_CORE;
    if (!getNotInDevelopment()
        || "true".equals(OBPropertiesProvider.getInstance().getOpenbravoProperties()
            .getProperty("test.environment")) || manifestCache.get(appName) == null) {
      String manifest = super.generate();
      manifestCache.put(appName, manifest);
    }
    return manifestCache.get(appName);
  }

  public String getVersion() {

    if (version != null) {
      return version;
    }

    StringBuffer versionString = new StringBuffer();
    final List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    for (Module module : modules) {
      versionString.append(module.getVersion());
    }
    version = DigestUtils.md5Hex(versionString.toString());

    return version;
  }

  public String getNetwork() {
    return "*";
  }

  public List<String> getAssets() {
    List<String> assets = new ArrayList<String>();
    String relativePath = "/web" + File.separatorChar + "org.openbravo.mobile.core";
    final File directory = new File(RequestContext.getServletContext().getRealPath(
        relativePath + "/assets"));

    final Iterator<File> it = FileUtils.iterateFiles(directory, null, true);
    while (it.hasNext()) {
      final File f = it.next();
      String fileCompletePath = f.getPath();
      assets.add(("../../" + fileCompletePath.substring(fileCompletePath.indexOf(relativePath))
          .replace("\\", "/")).replaceAll("//", "/"));
    }
    return assets;
  }

  @Override
  public String getContentType() {
    return "text/cache-manifest";
  }

  @Override
  public String getETag() {
    return UUID.randomUUID().toString();
  }

  @Override
  public boolean isJavaScriptComponent() {
    return false;
  }

  public List<String> getLibraryList() {
    libraryResource.setParameters(getParameters());
    return libraryResource.getAllResourceFiles();
  }

  /**
   * Gets the list of application specific resources to be cached. Override this method if your
   * application has any
   * 
   */
  public List<String> getAppList() {
    return new ArrayList<String>();
  }

  public List<String> getResources() {
    final List<String> resources = new ArrayList<String>();
    // include all resources defined by other modules
    for (CoreAppCacheResourceProvider resourceProvider : resourceProviders) {
      resources.addAll(resourceProvider.getResources());
    }
    return resources;
  }

  /**
   * Gets the list of images to be cached. Override this method if your application has any
   * 
   */
  public List<String> getImageFileList() {
    final String[] extensions = { "png", "gif" };
    return transformPath(getFileList(extensions));
  }

  /**
   * Gets the list of CSSs to be cached. Override this method if your application has any
   * 
   */
  public List<String> getcssFileList() {
    final String[] extensions = { "css", "less" };
    return transformPath(getFileList(extensions));
  }

  public static void getFileList(List<String> fileList, File folder, String[] extensions) {
    File[] filesInFolder = folder.listFiles();

    for (File f : filesInFolder) {
      if (f.isDirectory() && !f.getAbsolutePath().contains("productImages")) {
        getFileList(fileList, f, extensions);
      } else {
        for (String extension : extensions) {
          if (f.getName().endsWith("." + extension)) {
            fileList.add(f.getPath());
          }
        }
      }
    }

  }

  private List<String> getFileList(final String[] extensions) {

    final String relativePath = "/" + PATH_PREFIX + getModulePackageName();

    List<String> fileList = new ArrayList<String>();

    final File directory = new File(RequestContext.getServletContext().getRealPath(relativePath));

    getFileList(fileList, directory, extensions);

    return fileList;
  }

  private List<String> transformPath(List<String> stringFileList) {
    final List<String> resources = new ArrayList<String>();
    final String relativePath = PATH_PREFIX + getModulePackageName();

    for (final String f : stringFileList) {
      final int pos = f.indexOf(relativePath);
      resources.add("../../" + f.substring(pos));
    }
    return resources;
  }

  @Override
  public boolean bypassAuthentication() {
    return true;
  }

  public boolean getNotInDevelopment() {
    for (Module module : KernelUtils.getInstance().getModulesOrderedByDependency()) {
      if (module.isInDevelopment()) {
        return false;
      }
    }
    return true;
  }

  public String getAppName() {
    return null;
  }

  public String getGenFileName() {
    mobileStaticResourceComponent.setParameters(getParameters());
    return mobileStaticResourceComponent.getStaticResourceFileName();
  }
}
