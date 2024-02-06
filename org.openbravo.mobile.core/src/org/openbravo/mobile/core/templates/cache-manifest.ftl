CACHE MANIFEST

NETWORK:
${data.network}

<#if data.notInDevelopment>
# Version: ${data.version}
CACHE:

# Core 3rd party libraries

../../org.openbravo.mobile.core/OBMOBC_Main/Lib?_id=Enyo
../../org.openbravo.mobile.core/OBMOBC_Main/Lib?_id=Deps

<#if data.appName != "">
# Static resources
../../org.openbravo.mobile.core/OBMOBC_Main/StaticResources?_appName=${data.appName}
../../web/js/gen/${data.genFileName}.js
</#if>

<#list data.assets as asset>
${asset}
</#list>

<#list data.libraryList as lib>
${lib}
</#list>

# App specific files
<#list data.appList as app>
${app}
</#list>

# Other resource provided by modules
<#list data.resources as res>
${res}
</#list>

# Images
<#list data.imageFileList as imageFile>
${imageFile}
</#list>

# CSS
<#list data.cssFileList as cssFile>
${cssFile}
</#list>

</#if>