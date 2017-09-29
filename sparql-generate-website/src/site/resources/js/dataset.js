
// dataset

var update_dataset = function() {
  localStorage.setItem('dataset', JSON.stringify(dataset));
}

var get_dataset = function() {
  var dataset_str = localStorage.getItem('dataset');
  if(dataset_str !== null) {
    try {
      return JSON.parse(dataset_str); 
    }
    catch(err){}
  }
  dataset = [ "<s> <p> <o> ." , [] ];
  update_dataset();
  return dataset; 
}

var dataset = get_dataset() , yates; // dataset and editors

var show_dataset = function() {
  yates = [];
  yates[1] = [];

  // show default
  show_default_graph();
  for(var i=0; i<dataset[1].length; i++) {
    show_named_graph(dataset[1][i]);
  }

  $("#dataset_drop_zone")
  .click(function() {
    var ng = {
      uri: "http://example.org/graph",
      graph: "<s> <p> <o> ."
    };
    dataset[1].push(ng);
    update_dataset();
    show_named_graph(ng);
  })
  .on('drop', function(ev) {
    ev.preventDefault();
    // If dropped items aren't files, reject them
    var dt = ev.originalEvent.dataTransfer;
    if (dt.items) {
      // Use DataTransferItemList interface to access the file(s)
      for (var i=0; i < dt.items.length; i++) {
        if (dt.items[i].kind == "file") {
          var f = dt.items[i].getAsFile();
          reader = new FileReader();
          reader.onload = (function(i,f){return function (event) {
            var ng = {
              uri: f.name,
              graph: event.target.result
            };
            dataset[1].push(ng);
            update_dataset();
            show_named_graph(ng);
          }})(i,f);
          reader.readAsText(f);
        }
      }
    } else {
      // Use DataTransfer interface to access the file(s)
      for (var i=0; i < dt.files.length; i++) {
        reader = new FileReader();
        reader.onload = (function(i,f){return function (event) {
          var ng = {
            uri: f.name,
            graph: event.target.result
          };
          dataset[1].push(ng);
          update_dataset();
          show_named_graph(ng);
        }})(i,dt.files[i]);
        reader.readAsText(dt.files[i]);
      }  
    }
  })
  .on('dragover', function(ev) {
    ev.preventDefault();
  });  
}

var show_default_graph = function() {
  var tag = $(`<fieldset id='default_graph'>
      <label>Default graph (<a class='edit'>edit</a><a class='h' style='display:none'>hide</a>)</label>
      <textarea></textarea>
    </fieldset>`)
  .insertBefore($("#dataset_drop_zone"));

  yates[0] = YATE.fromTextArea($('#default_graph textarea')[0], {
    createShareLink: false,
    lineNumbers: true
  });
  yates[0].setValue(dataset[0]);
  yates[0].on("change", function(){
      dataset[0] = yates[0].getValue();
      update_dataset();
    }
  );

  $("#default_graph").find(".edit").click(function() {
    $("#default_graph").find(".edit").hide();
    $("#default_graph").find(".h").show();
    $("#default_graph label").next("div").show();
  });
  $("#default_graph").find(".h").click(function() {
    $("#default_graph").find(".edit").show();
    $("#default_graph").find(".h").hide();
    $("#default_graph label").next("div").hide();
  });
  tag.find("label").next("div").hide();

}

var show_named_graph = function(ng) {
  var tag = $("<fieldset>")
    .attr("class","named_graph")
    .append($("<label>")
      .append("Graph named by URI <")
      .append(
        $("<span>")
        .attr("contenteditable", true)
        .attr("class","name")
        .text(ng.uri)
      )
      .append("> (<a class='edit'>edit</a><a class='h' style='display:none'>hide</a>, <a class='delete'>delete</a>) ")
    )
    .append($("<textarea>"))
    .insertBefore($("#dataset_drop_zone"));

  tag.find(".name").blur([ng,tag.find(".name")], function(event) {
      event.data[0].uri = event.data[1].text();
      update_dataset();
    }
  );

  tag.find(".edit").click(function() {
    tag.find(".edit").hide();
    tag.find(".h").show();
    tag.find("label").nextAll(".CodeMirror").show();
  });
  tag.find(".h").click(function() {
    tag.find(".edit").show();
    tag.find(".h").hide();
    tag.find("label").nextAll(".CodeMirror").hide();
  });

  var yate = CodeMirror.fromTextArea(tag.find("textarea")[0], 
  {
    lineNumbers: true,
  });
  yate.setValue(ng.graph);
  yate.on("change", function(){
    ng.graph = yate.getValue();
    update_dataset();
  });

  yates[1].push(yate);

  tag.find("label").nextAll(".CodeMirror").hide();

  tag.find(".delete").click((function(ng, yate, tag) { return function() {
    for(var i=0;i<dataset[1].length;i++) {
      console.log(i,ng, dataset[1][i], ng == dataset[1][i])
      if(ng == dataset[1][i]) {
        dataset[1].splice(i,1);
        yates[1].splice(i,1);
      }
    }
    tag.remove();
    update_dataset();
  }})(ng, yate, tag));
}
