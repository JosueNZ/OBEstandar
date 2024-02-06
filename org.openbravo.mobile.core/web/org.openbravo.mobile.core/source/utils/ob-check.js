/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 *
 * Author JGA
 *
 */
/*global console */
/**
 * This class check if the url set by the user is a correct terminal url
 *
 */

// Fix the url
(function () {
  var pathname, newpath;
  var origUrl = window.location.href;
  pathname = window.location.pathname;
  newpath = window.location.pathname.replace(new RegExp('[/]+', 'g'), '/');
  if (pathname !== newpath) {
    console.warn("Malformed URL fixed: " + origUrl);
    window.location.href = origUrl.replace(pathname, newpath);
  }
}());