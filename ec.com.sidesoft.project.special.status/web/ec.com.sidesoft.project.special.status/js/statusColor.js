isc.defineClass("ProjectStaus_Field", isc.Label);
isc.ProjectStaus_Field.addProperties({
  height: 1,
  width: "100%",
  initWidget: function () {
    if (this.record) {
      console.log(this.record);
      this.createField(this.record.smspProjectstatus);
    }
    this.Super("initWidget", arguments);
  },
  createField: function (code) {
    // Gris
    var backGroundColor = "#e9f0ef",
      text,
      align = "center";
    if (code === "COLLPPS") {
      // Verde
      backGroundColor = "#00FF00";
      text = "";
    } else if (code === "INV") {
      // Amarillo
      backGroundColor = "#ffdf00";
      text = "";
    } else if (code === "COLLPS") {
      // Rojo
      backGroundColor = "#ff0000";
      text = "";
    } else if (code === "COLL") {
      // Naranja
      backGroundColor = "#ff6a00";
      text = "";
    } else if (code === "PCOLL") {
      // Morado
      backGroundColor = "#8b32a8";
      text = "";
    } else if (code === "PINV") {
      // Celeste
      backGroundColor = "#5792A1";
      text = "";
    }
    this.setBackgroundColor(backGroundColor);
    this.setAlign(align);
  },
});
