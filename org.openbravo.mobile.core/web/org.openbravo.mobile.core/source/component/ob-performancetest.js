/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone */
enyo.kind({
  name: 'OB.UI.ModalBenchmark',
  kind: 'OB.UI.ModalAction',
  autoDismiss: false,
  closeOnEscKey: false,
  bodyContent: {
    components: [{
      name: 'divBenchmark'
    }, {
      kind: 'enyo.Control',
      name: 'mainLabel'
    }, {

      classes: 'POSLoadingProgressBarImg',
      style: 'text-align:left;',
      components: [{
        style: 'width: 0%; margin-bottom: 11px;',
        name: 'containerLoading_BenchmarkProgressBar',
        classes: 'POSLoadingBarFilling'
      }]
    }, {
      classes: 'POSLoadingProgressBar',
      components: [{
        name: 'containerLoading_currentTest',
        showing: true,
        classes: 'POSLoadingBarMessage'
      }]
    }]
  },
  bodyButtons: [],
  executeOnShow: function () {
    var me = this;
    setTimeout(function () {
      me.$.bodyContent.$.divBenchmark.node.innerHTML = "<iframe id='testframe' src='../../web/org.openbravo.mobile.core/lib/sunspider/sunspiderOB.html'>";
    }, 200);
  },
  initComponents: function () {
    var me = this;
    this.inherited(arguments);
    this.$.headerCloseButton.hide();
    OB.Benchmark = {};
    OB.Benchmark.ModelResults = new Backbone.Model();
    this.setHeader(OB.I18N.getLabel('OBMOBC_CheckingSystemPerformanceTitle'));
    this.$.bodyContent.$.mainLabel.setContent(OB.I18N.getLabel('OBMOBC_CheckingSystemPerformanceMessage'));
    this.$.bodyContent.$.divBenchmark.setShowing(false);
    OB.Benchmark.ModelResults.on('change:currentTestName', function () {
      me.$.bodyContent.$.containerLoading_currentTest.setContent(OB.Benchmark.ModelResults.get('currentTestName'));
    });

    OB.Benchmark.ModelResults.on('change:currentPosition', function () {
      var width = (OB.Benchmark.ModelResults.get('currentPosition') / OB.Benchmark.ModelResults.get('maxLength')) * 100 + '%';
      if (me.$.bodyContent.$.containerLoading_BenchmarkProgressBar.hasNode()) {
        me.$.bodyContent.$.containerLoading_BenchmarkProgressBar.node.style.width = width;
      }
    });

    OB.Benchmark.ModelResults.on('change:finalResult', function () {
      var finalScore = Math.round(1000000 / OB.Benchmark.ModelResults.get('finalResult'));
      OB.Benchmark.ModelResults.set('finalScore', finalScore);
      OB.UTIL.localStorage.setItem('benchmarkScore', finalScore);
      me.hide();
      if (finalScore > 1000) {
        OB.UTIL.showSuccess(OB.I18N.getLabel('OBMOBC_SystemPerformanceIsGood', [finalScore]));
        OB.UTIL.localStorage.setItem('doNotExecuteBenchmark', true);
      } else {
        me.destroy();
        this.dialog = OB.MobileApp.view.$.confirmationContainer.createComponent({
          kind: 'OB.UI.ModalBenchmarkResult',
          name: 'modalBenchmark',
          context: this
        });
        this.dialog.show();
      }
    });

  }
});

enyo.kind({
  name: 'OB.UI.ModalBenchmarkResult',
  kind: 'OB.UI.ModalInfo',
  bodyContent: {
    components: [{
      name: 'scoremessage',
      style: 'float: left;font-size: 40px;word-wrap: break-word;width: 100%; padding-bottom:20px;',
      content: ''
    }, {
      name: 'message',
      style: 'float: left;',
      content: ''
    }, {
      name: 'link',
      style: 'float: left; width: 100%;',
      tag: 'a href="http://wiki.openbravo.com/wiki/Retail:PerformanceTesting" target="_blank"',
      content: '',
      showing: false
    }]
  },
  bodyButtons: {
    components: [{
      style: 'height: 40px; width: 100%; float:left;',
      components: [{
        style: 'height: 40px; width: 30%; float:left;'
      }, {
        kind: 'OB.UI.CheckboxButton',
        classes: 'modal-dialog-btn-check',
        style: 'float: left; width: 10%; background-color: rgb(226, 226, 226);',
        name: 'doNotShowAgainCheck'
      }, {
        style: 'height: 28px; padding-top: 10px; float: left; width: 30%; background-color: rgb(226, 226, 226);',
        initComponents: function () {
          this.setContent(OB.I18N.getLabel('OBMOBC_PerfCheckDoNotShowAgain'));
        }
      }]
    }, {
      style: 'clear: both;'
    }, {
      kind: 'OB.UI.ModalDialogButton',
      i18nLabel: 'OBMOBC_LblOk',
      name: 'acceptDialogButton',
      tap: function () {
        if (this.parent.parent.$.doNotShowAgainCheck.checked) {
          OB.UTIL.localStorage.setItem('doNotExecuteBenchmark', true);
        }
        this.parent.parent.parent.parent.hide();
      }
    }]
  },
  executeOnShow: function () {
    this.$.bodyContent.$.message.setContent(OB.I18N.getLabel('OBMOBC_SystemPerformanceIsNotGood'));
    this.$.bodyContent.$.scoremessage.setContent(OB.I18N.getLabel('OBMOBC_SystemPerformanceFinalScore', [OB.Benchmark.ModelResults.get('finalScore')]));
    this.$.bodyContent.$.link.setContent(OB.I18N.getLabel('OBMOBC_link'));
    this.$.bodyContent.$.link.setShowing(true);
  }
});