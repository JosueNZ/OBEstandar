/*
 ************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 *
 * Author RAL
 *
 */

/*global OB, console, enyo, exports, document */

/*
  Information about TestRegistry

  The content is lazy loaded so it does not consum any resources until invoqued
  The generated ids are unique
  The generated id can be seen in testeable DOM objects in the 'idtest' attribute. if you can't see them, execute TestRegistry.registry()

  Good practices in the enyo components for the TestRegistry to work properly are:
    - make sure no ids are set

  Concepts used:
     - a segment is the text in between the '_' in the DOM id. e.g: if id = 'terminal_control', the segments are 'terminal' and 'control'

  Troubleshooting:
   - if the idTest you need is not in the DOM, check if its untesteable executing printUnstableIds(). if so, change the enyo component name for a unique name or use the replacementsTable or add a parent to the enyo component
     - if the algorithm is no longer viable or needs a lot of changes in the algorithm, read the futureAlgorithm() function
*/

(function () {
  var root = this;
  var TestRegistry;
  if (typeof exports !== "undefined") {
    TestRegistry = exports;
  } else {
    TestRegistry = root.TestRegistry = {};
  }
  TestRegistry.VERSION = "1.0.3";
  console.log("TestRegistry " + TestRegistry.VERSION + " is available");

  TestRegistry.writingTest = false;
  TestRegistry.testSteps = [];

  // add to this replacements table parts of the idDOM that will be replaced
  // the goal is to get generated ids without 'pointOfSale', etc; the segments of the top parent window
  var replacementsTable = [
  // ['tbody_', ''],
  // ['stockDetailList_scrollListStockDetails', 'stockList'],
  ];
  var checksTable = ["pointOfSale", "cashUp", "cashManagement"];

  var minimumSegments = 2; // the minimum segments the idTest will have, if possible. if you change this number you will not be able to reference many objects that you referenced before
  var checkForRepeated = false;

  var enyoNodes = null;
  var testeableIdTests = null;
  var untesteableIdTests = null;

  // statistics
  var totalEnyoObjects = null;
  var nullCount = null;
  var enyoObjectsCount = null;
  var validEnyoObjects = null;
  var maxSegments = null;
  var idModifications = null;
  var needsTheIdToBeRemovedInEnyoComponentCreation = 0;

  var init = function () {
      enyoNodes = [];
      testeableIdTests = [];
      untesteableIdTests = [];

      // statistics
      totalEnyoObjects = 0;
      nullCount = 0;
      enyoObjectsCount = 0;
      validEnyoObjects = 0;
      maxSegments = -1;
      idModifications = 0;
      needsTheIdToBeRemovedInEnyoComponentCreation = 0;
      };

  function EnyoNode(enyoGeneratedIdDOM, preGeneratedIdTest) {
    this.idDOM = enyoGeneratedIdDOM;
    this.enyoObject = enyo.$[enyoGeneratedIdDOM];
    // this.DOMObject = document.getElementById(enyoGeneratedIdDOM);
    var isInNeedOfNewId = preGeneratedIdTest === undefined;
    var idDOMForAlgorithm = preGeneratedIdTest;
    var firstUniqueSegment = null;
    var segments = null;
    var idTest = null;
    var resetIdTest = function () {
        idTest = null;
        };
    var initIdTest = function () {
        idTest = {
          chunkRemoved: null,
          id: null,
          isAlgorithmGenerated: null,
          isUnique: false,
          // true if unique, false if not
          isTesteable: null // true if a unit test can reference this object, false if not
        };
        };
    var checkIdTest = function () {
        if (idTest === null) {
          throw {
            name: "Fatal Error",
            message: "You must generate the idTest object first. execute getIdTest()"
          };
        }
        };
    var getSegments = function () {
        if (segments === null) {
          segments = idDOMForAlgorithm.split(/_/);
        }
        return segments;
        };
    var getLastSegmentIndex = function () {
        return getSegments().length - 1;
        };
    this.init = function () {
      if (isInNeedOfNewId) {
        idDOMForAlgorithm = this.idDOM;
        // do replacements
        var idWithReplacements = idDOMForAlgorithm;
        replacementsTable.forEach(function (element) {
          idWithReplacements = idWithReplacements.split(element[0]).join(element[1]);
        });
        // just count them for stats
        idDOMForAlgorithm = idWithReplacements;
        if (this.idDOM.length !== idDOMForAlgorithm.length) {
          idModifications += 1;
        }
      }

      // initialization of some variables
      var segmentCount = getLastSegmentIndex();
      firstUniqueSegment = segmentCount;
      if (segmentCount > maxSegments) {
        maxSegments = segmentCount;
      }
    };
    this.isSegmentsLeft = function () {
      return firstUniqueSegment < getLastSegmentIndex();
    };
    this.setFirstInvertedUniqueSegment = function (invertedIndex) {
      if (isInNeedOfNewId) {
        resetIdTest();
        var newFirstUniqueSegment = getLastSegmentIndex() - invertedIndex;
        if (newFirstUniqueSegment + minimumSegments < 0) {
          // this could happen if there are repeated ids in the DOM (search for [enyo] Duplicated DOM id warning message in the console log)
          // must be handle by the caller
          throw {
            name: "Fatal Error",
            message: "It is not possible to find a unique first segment because the DOM id is duplicated. Open the browser log and search for '[enyo] Duplicated DOM id' warnings. This error must be fixed before continuing. Tests lose their purpouse if non-unique UI objects are present"
          };
        }
        firstUniqueSegment = newFirstUniqueSegment;
      }
    };
    this.setIsUnique = function () {
      if (!checkIdTest) {
        throw {
          name: "Fatal Error",
          message: "Your are trying to set it to unique before generating it"
        };
      }
      idTest.isUnique = true;
    };
    this.getIdTest = function () {
      var i = 0;
      if (idTest === null) {
        initIdTest();
        if (this.enyoObject.idtest) {
          idTest.chunkRemoved = "";
          idTest.id = this.enyoObject.idtest;
          return idTest;
        }
        idTest.isAlgorithmGenerated = isInNeedOfNewId;
        if (isInNeedOfNewId) {
          var removed = "";
          var id = "";
          if (firstUniqueSegment === null) {
            throw {
              name: "Fatal Error",
              message: "The firstUniqueSegment variable must be set. Init() set an initial value so you have, most probably, broken the flow"
            };
          }
          var lastSegmentIndex = getLastSegmentIndex();
          for (i = 0; i < lastSegmentIndex; i++) {
            var segment = getSegments()[i];
            if (i < firstUniqueSegment) {
              removed += segment + "_";
            } else {
              id += segment + "_";
            }
          }
          idTest.chunkRemoved = removed;
          idTest.id = id + getSegments()[lastSegmentIndex];
        } else {
          idTest.chunkRemoved = "";
          idTest.id = idDOMForAlgorithm;
        }
      }
      return idTest;
    };
    this.toString = function () {
      var idTest = this.getIdTest();
      if (idTest.isTesteable) {
        return "idDOM:\t\t" + this.idDOM + " =>\n\tidTest:\t'" + idTest.id + "'";
      }
      return "idDOM:\t\t" + this.idDOM + " =>\n\tidTest:\t'" + idTest.chunkRemoved + "' / '" + idTest.id + "'" + (idTest.isTesteable ? "" : " (*)");
    };
    this.isEqual = function (to) {
      return this.getIdTest().id === to.getIdTest().id;
    };
    /**
     * Checks for untesteable string chunks in the id
     */
    this.isValidForUnitTesting = function () {
      checkIdTest();
      var isNumeric = function (s) {
          return !isNaN(s);
          };
      var isTesteable = true;
      var incompatibleString = null;
      var idTest = this.getIdTest();
      if (idTest.isUnique) {
        var id = idTest.id;
        checksTable.forEach(function (element) {
          var index = id.indexOf(element);
          if (index >= 0) {
            var nextChar = id.charAt(index + element.length);
            if (nextChar === "_" || isNumeric(nextChar)) {
              isTesteable = false;
              incompatibleString = element;
              return;
            }
          }
        });
      } else {
        isTesteable = false;
        incompatibleString = "not unique";
      }
      idTest.isTesteable = isTesteable;
      return {
        isTesteable: idTest.isTesteable,
        incompatibleString: incompatibleString
      };
    };
    this.init();
  }

  function scan() {
    /**
     * get all enyo objects in the webpage, check that they are valid and have a findable DOM id
     */
    init();

    var addNode = function (id, idTest) {
        totalEnyoObjects += 1;
        enyoObjectsCount += 1;
        if (enyo.$[id] === null) {
          nullCount += 1;
          return;
        }
        if (!document.getElementById(id)) {
          return;
        }
        if (id.indexOf('OB.UI.id') >= 0 || id.indexOf('org.openbravo') >= 0) {
          needsTheIdToBeRemovedInEnyoComponentCreation += 1;
          return;
        }
        validEnyoObjects += 1;
        enyoNodes.push(new EnyoNode(id, idTest));
        };

    // generate ids for tables
    var enyoRoot = enyo.$.terminal;
    var count = 0;
    var recurseLi;
    recurseLi = function (enyoParent, tablePrefix, liIndex) {
      count += 1;
      if (enyoParent.children.length === 0) {
        return;
      }
      enyoParent.children.forEach(function (element) {
        addNode(element.id, tablePrefix + "_row" + liIndex + "_" + element.name);
        recurseLi(element, tablePrefix, liIndex);
      });
    };
    var recurseTable;
    recurseTable = function (enyoParent, tablePrefix, liIndex) {
      count += 1;
      if (enyoParent.children.length === 0) {
        return;
      }
      enyoParent.children.forEach(function (element) {
        if (element.tag === "li") {
          liIndex += 1;
          addNode(element.id, tablePrefix + "_row" + liIndex);
          recurseLi(element, tablePrefix, liIndex);
        } else {
          addNode(element.id, tablePrefix + "_" + element.name);
          recurseTable(element, tablePrefix, liIndex);
        }
      });
    };
    var getChildren;
    getChildren = function (enyoParent) {
      count += 1;
      if (enyoParent.children.length === 0) {
        return;
      }
      enyoParent.children.forEach(function (element) {
        if (element.kind && (typeof (element.kind) !== "function") && (element.kind.indexOf("OB.UI.Table") === 0 || element.kind.indexOf("OB.UI.ScrollableTable") === 0)) {
          addNode(element.id, "table_" + element.name);
          recurseTable(element, element.name, 0);
        } else {
          addNode(element.id);
          getChildren(element);
        }
      });
    };

    getChildren(enyoRoot);

    // build the list of objects to compute with the algorithm
    var enyoNodesToProcess = [];
    enyoNodes.forEach(function (element, index) {
      enyoNodesToProcess.push(index);
    });

    var segmentIndex = minimumSegments - 1;
    var loops = 0;

    /**
     * This is the algorithm to infere ids
     */
    while (enyoNodesToProcess.length > 0) {
      // create the array of segments of the segmentIndex segment
      var segmentsToProcess = [];
      var nodeToProcessIndex;
      for (nodeToProcessIndex in enyoNodesToProcess) {
        if (enyoNodesToProcess.hasOwnProperty(nodeToProcessIndex)) {
          var nodeIndex = enyoNodesToProcess[nodeToProcessIndex];
          var enyoNode2 = enyoNodes[nodeIndex];
          if (!enyoNode2.isSegmentsLeft) {
            continue;
          }
          enyoNode2.setFirstInvertedUniqueSegment(segmentIndex);
          var tail = enyoNode2.getIdTest().id;
          var isFound = false;
          var segmentToProcessIndex;
          for (segmentToProcessIndex = 0; segmentToProcessIndex < segmentsToProcess.length; segmentToProcessIndex++) {
            var segmentToProcess = segmentsToProcess[segmentToProcessIndex];
            var segmentNameToCompare = segmentToProcess.tail;
            if (tail === segmentNameToCompare) {
              segmentToProcess.enyoNodeToProcessNodeIndex = null;
              segmentToProcess.enyoNode = null;
              segmentToProcess.count += 1;
              isFound = true;
              break;
            }
          }
          if (!isFound) {
            var segmentToProcess2 = {
              tail: tail,
              count: 1,
              enyoNodeToProcessNodeIndex: nodeIndex,
              // this is only used when its unique
              enyoNode: enyoNode2 // this is only used when its unique
            };
            segmentsToProcess.push(segmentToProcess2);
          }
        }
      }

      // set uniques as ready
      // create the exclude table
      var j;
      for (j = 0; j < segmentsToProcess.length; j++) {
        var segmentToProcess3 = segmentsToProcess[j];
        if (segmentToProcess3.count === 1) {
          segmentToProcess3.enyoNode.setIsUnique();
          var nodeIndexToRemove = enyoNodesToProcess.indexOf(segmentToProcess3.enyoNodeToProcessNodeIndex++);
          enyoNodesToProcess.splice(nodeIndexToRemove, 1);
        }
      }

      // check that the algorithm is not in an infinite loop
      // discard the remaining ids
      if (loops >= maxSegments) {
        enyoNodesToProcess = [];
        // OB.info("IMPOSIBLE TO GENERATE THIS IDs");
        // for (j = 0; j < enyoNodesToProcess; j++) {
        //   var nodeIndex3 = enyoNodesToProcess[j];
        //   var enyoNode4 = enyoNodes[nodeIndex3];
        //   OB.info(enyoNode4.toString());
        // }
        // throw {
        //   name: "Fatal Error",
        //   message: "Infinite loop"
        // };
      }

      loops += 1;
      segmentIndex += 1;
    }

    /**
     * this could be used to refactor the algorithm
     * and make it, probably, faster and more readeable
     * idea: if we build a sorted list of reversed strings, we could get the same result cycling through the chars of the list and check when the char changes
     * its the same idea as the actual one, but simplified
     */
    // var futureAlgorith = function () {
    //  var sortedIdList = [];
    //  var k;
    //  for (k = 0; k < enyoNodes.length; k++) {
    //    var id2 = enyoNodes[k].getIdTest().id;
    //    id2 = id2.split("").reverse().join("");
    //    sortedIdList.push(id2);
    //  }
    //  sortedIdList.sort();
    //  var sortedIdIndex;
    //  for (sortedIdIndex = 0; sortedIdIndex < sortedIdList.length; sortedIdIndex++) {
    //    OB.info(sortedIdList[sortedIdIndex]);
    //  }
    // };
    // check if the EnyoNode is testeable and save the state
    var o;
    for (o = 0; o < enyoNodes.length; o++) {
      var id = enyoNodes[o].getIdTest().id;
      if (enyoNodes[o].isValidForUnitTesting().isTesteable) {
        testeableIdTests.push(id);
      } else {
        untesteableIdTests.push({
          enyoNodeIndex: o,
          incompatibleStringDetected: enyoNodes[o].isValidForUnitTesting().incompatibleString,
          id: id
        });
      }
    }

    // create the list of untesteable objects and remove them from the final enyoNodes list
    if (untesteableIdTests.length > 0) {
      OB.info("found " + untesteableIdTests.length + " ids of " + validEnyoObjects + " that are not valid for unit tests. execute printUntesteableIds() to get the list\n");
      untesteableIdTests.sort(function (a, b) {
        if (a.incompatibleStringDetected !== b.incompatibleStringDetected) {
          a.incompatibleStringDetected.localeCompare(b.incompatibleStringDetected);
        }
        return a.id.localeCompare(b.id);
      });
      var enyoNodesToRemove = [];
      var notValidForUnitTestIndex;
      for (notValidForUnitTestIndex = 0; notValidForUnitTestIndex < untesteableIdTests.length; notValidForUnitTestIndex++) {
        var notValidForUnitTestRow = untesteableIdTests[notValidForUnitTestIndex];
        enyoNodesToRemove.push(notValidForUnitTestRow.enyoNodeIndex);
      }
      enyoNodesToRemove.sort(function (a, b) {
        return a - b;
      });
      enyoNodesToRemove.reverse();
      var enyoNodeToRemoveIndex;
      for (enyoNodeToRemoveIndex = 0; enyoNodeToRemoveIndex < enyoNodesToRemove.length; enyoNodeToRemoveIndex++) {
        enyoNodes.splice(enyoNodesToRemove[enyoNodeToRemoveIndex], 1);
      }
    }

    // check if all the generated ids are unique. this is not needed as the actual flow can't reach this point if there is a single repeated id
    if (checkForRepeated) {
      OB.info("");
      OB.info("Checking uniqueness...");
      var isRepeated = false;
      var i;
      for (i = 0; i < enyoNodes.length; i++) {
        var enyoNode6 = enyoNodes[i];
        var idTest = enyoNode6.getIdTest();
        if (!idTest.isUnique) {
          OB.info("REPEATED:\n\t" + enyoNode6.toString() + "\n");
          isRepeated = true;
        }
      }
      if (isRepeated) {
        throw {
          name: "Error",
          message: "all ids must be unique"
        };
      } else {
        OB.info("all the ids are unique");
      }
    }

  }

  function getList() {
    // 'lazy load' function that contains all the enyo objects in the webpage
    if (enyoNodes === null) {
      OB.info("lazy loading...");
      scan();
      OB.info("totalObjects: " + totalEnyoObjects + ", enyoObjects: " + enyoObjectsCount + ", nullCount: " + nullCount + ", enyo ids to change: " + needsTheIdToBeRemovedInEnyoComponentCreation + ", validObjects: " + validEnyoObjects + ", id modifications: " + idModifications + ", maxSegments: " + maxSegments + "\n");
      OB.info("... lazy load complete");
    }
    return enyoNodes;
  }

  function findWhere(idTest) {
    // finds the register with the specified idTest
    var list = getList();
    var enyoNode = null;
    list.some(function (element) {
      if (element.getIdTest().id === idTest) {
        enyoNode = element;
        return;
      }
    });
    if (enyoNode !== null) {
      // check if the DOM changed since the last scan
      var DOMObject = document.getElementById(enyoNode.idDOM);
      if (DOMObject) {
        return enyoNode;
      }
    }
    return null;
  }

  TestRegistry.help = function () {
    OB.info("List of TestRegistry properties:");
    var publicProperties = Object.getOwnPropertyNames(this);
    publicProperties.sort();
    publicProperties.forEach(function (element) {
      OB.info("  - " + element);
    });
  };

  TestRegistry.printTesteableIds = function () {
    getList();
    OB.info("final list of ids:");
    testeableIdTests.sort(function (a, b) {
      if (a === b) {
        a.localeCompare(b);
      }
      return a.localeCompare(b);
    });
    var index;
    for (index = 0; index < testeableIdTests.length; index++) {
      OB.info(testeableIdTests[index]);
    }
    OB.info("Total testeable ids: " + testeableIdTests.length + "\n");
  };

  TestRegistry.printUntesteableIds = function () {
    getList();
    var index;
    for (index = 0; index < untesteableIdTests.length; index++) {
      var notValidForUnitTestRow = untesteableIdTests[index];
      OB.info("'" + notValidForUnitTestRow.incompatibleStringDetected + "' in '" + notValidForUnitTestRow.id + "'");
    }
    OB.info("Total untesteable ids: " + untesteableIdTests.length + "\n");
  };

  // TestRegistry.findById = function(idDOM) {
  //   // finds the registered object with a DOM id
  //   var list = getList();
  //   var found = null;
  //   list.some(function(element, index, array) {
  //     if (element.idDOM == idDOM) {
  //       found = element;
  //       return true;
  //     }
  //   });
  //   return found;
  // };
  /**
   * Returns the EnyoNode that has the provided idTest
   * @param  {string}     idTest  the idTest as shown in the DOM
   * @param  {boolean}    silent  an error will not be shown in the console (added for better test logs)
   * @return {EnyoNode}           the EnyoNode if found
   */
  TestRegistry.registry = function (idTest, silent) {
    var enyoNode = null;
    var rescan = true;
    if (enyoNodes === null) {
      rescan = false;
    }
    enyoNode = findWhere(idTest);
    if (enyoNode === null && rescan) {
      scan();
      enyoNode = findWhere(idTest);
    }
    if (enyoNode !== null) {
      return enyoNode;
    }
    if (!silent) {
      OB.warn("Cannot find any object with idTest = '" + idTest + "'");
    }
    return null;
  };

  /**
   * Returns the EnyoNode that has the provided idTest
   * @param  {string}     idTest  the idTest as shown in the DOM
   * @param  {boolean}    silent  an error will not be shown in the console (added for better test logs)
   * @return {EnyoNode}           the EnyoNode if found
   */
  TestRegistry.registerAction = function (enyoObj) {
    var content;
    if (TestRegistry.writingTest) {
      if (enyoObj.testActions) {
        enyoObj.testActions.forEach(function (action) {
          switch (action) {
          case 'verify':
            TestRegistry.testSteps.push('verifyUI("' + enyoObj.idtest + '", "' + enyoObj.content + '");');
            console.log('verifyUI("' + enyoObj.idtest + '", "' + enyoObj.content + '");');
            break;
          case 'tap':
            TestRegistry.testSteps.push('tapUI("' + enyoObj.idtest + '");');
            console.log('tapUI("' + enyoObj.idtest + '");');
            break;
          case 'tapCatProd':
            TestRegistry.testSteps.push('tapCatProd("' + content + '");');
            console.log('tapCatProd("' + content + '");');
            break;

          default:
            console.log('Action: ' + action + ' is not defined. Review component: ' + enyoObj.idtest);
          }
        });
      } else {
        if (enyoObj.content !== '') {
          TestRegistry.testSteps.push('verifyUI("' + enyoObj.idtest + '", "' + enyoObj.content + '");');
          console.log('verifyUI("' + enyoObj.idtest + '", "' + enyoObj.content + '");');
        }
        if (enyoObj.name === 'renderCategory') {
          content = enyoObj.$.control2.children[0].getContent();
        } else if (enyoObj.name === 'renderProduct') {
          content = enyoObj.children[1].children[0].getContent();
        }
        if (enyoObj.$ && enyoObj.$.control2 && enyoObj.$.control2.children[0] && enyoObj.tap) {
          TestRegistry.testSteps.push('tapCatProd("' + content + '");');
          console.log('tapCatProd("' + content + '");');
          return;
        }

        if (enyoObj.tap) {
          TestRegistry.testSteps.push('tapUI("' + enyoObj.idtest + '");');
          console.log('tapUI("' + enyoObj.idtest + '");');
          return;
        }
      }
    }
  };
  var onmousedownFunc = function () {
      var enyoObj = TestRegistry.registry(this.getAttribute('idtest')).enyoObject;
      if (enyoObj.tap || enyoObj.content) {
        TestRegistry.registerAction(enyoObj);
      }
      };
  TestRegistry.appendIdTestToDOM = function () {
    scan();

    // append the idTest attribute to the DOM objects
    var attributeAppendErrors = 0;
    OB.info("adding idTests to the DOM");
    var enyoNodesIndex;
    for (enyoNodesIndex = 0; enyoNodesIndex < enyoNodes.length; enyoNodesIndex++) {
      var enyoNode3 = enyoNodes[enyoNodesIndex];
      var DOMnode = document.getElementById(enyoNode3.idDOM);
      if (enyoNode3.enyoObject.idtest) {
        DOMnode.setAttribute("idtest", enyoNode3.enyoObject.idtest);
        DOMnode.onmousedown = onmousedownFunc;
        continue;
      }
      var idTest2 = enyoNode3.getIdTest();
      if (idTest2.isTesteable && idTest2.isUnique) {
        if (DOMnode) {
          enyoNode3.enyoObject.idtest = idTest2.id;
          DOMnode.setAttribute("idtest", idTest2.id);
          DOMnode.onmousedown = onmousedownFunc;
        } else {
          attributeAppendErrors += 1;
          OB.info(enyoNode3.idDOM, enyoNode3);
        }
      }
    }
    if (attributeAppendErrors > 0) {
      OB.info("DOM append errors: " + attributeAppendErrors + "\n");
    }
  };

}());