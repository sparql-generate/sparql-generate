var winW = $(window).width() * .9;
var winH = $(window).height() * .9;
CodeMirror.modeURL = "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.30.0/mode/%N/%N.js";

// html skeleton

///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  validate

var valid = true;
var open = false;
var auto = false;
var stream = false;
var timers = [];
var loglevel = 5;

var resetErrors = function() {
  $(".invalid").removeClass("invalid");
  $("#run").removeAttr("disabled");
  $(".invalidmsg").remove();
};

var validate = function() {

  // no two named thing with the same URI and mediatype
  // queries have mediatype application/vnd.sparql-generate
  // graphs have mediatype text/turtle

  // no query, graph or document has bad state 

  valid = true;
  resetErrors();

  if(!defaultquery_editor.queryValid) {
    valid = false;
    defaultquery_tag.addClass("invalid");
    defaultquery_tag.children(":first").append(" <span class='invalidmsg'>This query is not valid.</span>");
        $("#run").attr("disabled", "disabled");
  }
  for(var i=0;i<namedqueries_editors.length;i++) {
    var query = namedqueries[i];
    var editor = namedqueries_editors[i];
    var tag = namedqueries_tags[i];

    if(!editor.queryValid) {
      valid = false;
      tag.addClass("invalid");
      tag.children(":first").append(" <span class='invalidmsg'>This query is not valid.</span>");
        $("#run").attr("disabled", "disabled");
    }
    for(var n of namedqueries) {
      if(query !== n && query.uri == n.uri && query.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another query has the same URI.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
    for(var n of documentset) {
      if(query.uri == n.uri && query.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A document has the same URI and mediatype 'application/vnd.sparql-generate'.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
  }

  // validating dataset
  if(!defaultgraph_editor.docValid) {
    valid = false;
    defaultgraph_tag.addClass("invalid");
    defaultgraph_tag.children(":first").append(" <span class='invalidmsg'>This graph is not valid.</span>");
        $("#run").attr("disabled", "disabled");
  }
  for(var i=0;i<namedgraphs_editors.length;i++) {
    var graph = namedgraphs[i];
    var editor = namedgraphs_editors[i];
    var tag = namedgraphs_tags[i];

    if(!namedgraphs_editors[i].docValid) {
      valid = false;
      namedgraphs_tags[i].addClass("invalid");
      namedgraphs_tags[i].children(":first").append(" <span class='invalidmsg'>This graph is not valid.</span>");
        $("#run").attr("disabled", "disabled");
    }
    for(var n of namedgraphs) {
      if(graph !== n && graph.uri == n.uri && graph.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another graph has the same URI.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
    for(var n of documentset) {
      if(graph.uri == n.uri && graph.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A document has the same URI and mediatype 'text/turtle'.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
  }

  // validating documents
  for(var i=0;i<documentset_editors.length;i++) {
    var doc = documentset[i];
    var editor = documentset_editors[i];
    var tag = documentset_tags[i];

    if(!doc.mediatype.match(/\w+\/[-+.\w]+/i)) {
      valid = false;
      tag.addClass("invalid");
      tag.children(":first").append(" <span class='invalidmsg'>The mediatype is invalid.</span>");        
        $("#run").attr("disabled", "disabled");
    }
    for(var n of namedqueries) {
      if(doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A query has the same URI.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
    for(var n of namedgraphs) {
      if(doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A graph has the same URI.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
    for(var n of documentset) {
      if(doc !== n && doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another document has the same URI and mediatype.</span>");        
        $("#run").attr("disabled", "disabled");
      }
    }
  }

  if(valid && open && auto) {
    for(var timer of timers) {
      window.clearTimeout(timer);
    }
    timers = [];
    var msg = {
        defaultquery: defaultquery_string,
        namedqueries: namedqueries,
        defaultgraph: defaultgraph_string,
        namedgraphs: namedgraphs,
        documentset: documentset,
        stream: stream,
        loglevel: loglevel
    };
    timers.push(window.setTimeout(function(msg) {
      send(msg);
    }, 500, msg));
  }
};

///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
// run

var openSocket = function (callback) {
    var protocol = "ws:";
    if(window.location.protocol == "https:") {
        protocol = "wss:";
    }
  var websocketurl = protocol + "//" + window.location.hostname + (window.location.port!="" ? ":" + window.location.port : "") + "/sparql-generate/transformStream";
  socket = new WebSocket(websocketurl);

  socket.onopen = function (event) {
    open = true;
    console.log("websocket open");
    validate();
    if (callback) {
        callback();
    }
  };
   
  socket.onmessage = function (event) {
    var data = JSON.parse(event.data)
    console.log("received", data)
    processMessage(data);
  }
   
  socket.onclose = function (event) {
    open = false;
    console.log("websocket closed");
  }
}

var run = function() {
    var msg = {
        defaultquery: defaultquery_string,
        namedqueries: namedqueries,
        defaultgraph: defaultgraph_string,
        namedgraphs: namedgraphs,
        documentset: documentset,
        stream: stream,
        loglevel: loglevel
    };
    send(msg);
}

var send = function(msg) {
    console.log("sending ", msg);
    $("#run").delay(50).animate({
        "box-shadow": "none"
    }, 50, function () {
        $("#run").animate({
            "box-shadow": "0 8px 16px 0 rgba(0,0,0,0.2), 0 6px 20px 0 rgba(0,0,0,0.19)"
        }, 50);
    });
    if (open) {
        socket.send(JSON.stringify(msg));
    } else {
        openSocket(function () {
            socket.send(JSON.stringify(msg));
        });
    }
}

///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
// queryset

var defaultquery_string,
 defaultquery_tag,
 defaultquery_editor,
 namedqueries = [],
 namedqueries_editors = [],
 namedqueries_tags = [];

var load_queryset = function() {

  // load default query
  defaultquery_string = localStorage.getItem('defaultquery');
  if(defaultquery_string == null) {
    defaultquery_string = `PREFIX iter: <http://w3id.org/sparql-generate/iter/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX fun: <http://w3id.org/sparql-generate/fn/> 
GENERATE {
  GENERATE {
    <city/{ ?cityName }> <{ ?key }> "{ fun:JSONPath( ?message , "$.['{ ?cityName }']['{ ?key }']" )  }"@en . 
  } 
  ITERATOR iter:JSONListKeys( ?city ) AS ?key  .
}
SOURCE <https://ci.mines-stetienne.fr/sparql-generate/cities.json> AS ?message
ITERATOR iter:JSONListKeys( ?message ) AS ?cityName 
WHERE { 
  FILTER( STRSTARTS( ?cityName , "New" ) ) 
  BIND( fun:JSONPath( ?message, "$.['{ ?cityName }']" ) AS  ?city )
} 
`;
    localStorage.setItem('defaultquery', defaultquery_string);
  }

  // show default query
  defaultquery_tag = $(`<div id='default_query'>
      <label>Default query</label>
      <textarea></textarea>
    </div>`)
  .insertBefore($("#queryset_drop_zone"));

  defaultquery_editor = SGE.fromTextArea($('#default_query textarea')[0], {
    createShareLink: false,
    lineNumbers: true
  });
  defaultquery_editor.setValue(defaultquery_string);
  defaultquery_editor.on("change", function(){
      defaultquery_string = defaultquery_editor.getValue();
      localStorage.setItem('defaultquery', defaultquery_string);
      validate();
    }
  );

  // load named queries
  namedqueries = localStorage.getItem('namedqueries');
  if(namedqueries !== null) {
    try {
      namedqueries = JSON.parse(namedqueries); 
    }
    catch(err){
      namedqueries = [];
      localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
    }
  } else {
    namedqueries = [];
    localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
  }

  // show named queries
  for(var i=0; i<namedqueries.length; i++) {
    show_namedquery(namedqueries[i]);
  }

  // activate drop zone
  $("#queryset_drop_zone")
  .click(function() {
    var nq = {
      uri: "http://example.org/query#" + namedqueries.length,
      mediatype: "application/vnd.sparql-generate",
      string: `PREFIX iter: <http://w3id.org/sparql-generate/iter/>
PREFIX fun: <http://w3id.org/sparql-generate/fn/> 

GENERATE { }
WHERE { }`
    };
    namedqueries.push(nq);
    localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
    show_namedquery(nq);
    validate();
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
            var nq = {
              uri: f.name,
              mediatype: "application/vnd.sparql-generate",
              string: event.target.result
            };
            namedqueries.push(nq);
            localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
            show_namedquery(nq);
            validate();
          }})(i,f);
          reader.readAsText(f);
        }
      }
    } else {
      // Use DataTransfer interface to access the file(s)
      for (var i=0; i < dt.files.length; i++) {
        reader = new FileReader();
        reader.onload = (function(i,f){return function (event) {
          var nq = {
            uri: f.name,
            mediatype: "application/vnd.sparql-generate",
            query: event.target.result
          };
          namedqueries.push(nq);
          localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
          show_namedquery(nq);
          validate();
        }})(i,dt.files[i]);
        reader.readAsText(dt.files[i]);
      }  
    }
  })
  .on('dragover', function(ev) {
    ev.preventDefault();
  });  
}

var show_namedquery = function(nq) {
  var tag = $("<div>")
    .attr("class","named_query")
    .append($("<label>")
      .append("Query named by URI <")
      .append(
        $("<span>")
        .attr("contenteditable", true)
        .attr("class","name")
        .text(nq.uri)
      )
      .append("> (<a class='edit'>edit</a><a class='h' style='display:none'>hide</a>, <a class='delete'>delete</a>) ")
    )
    .append($("<textarea>"))
    .insertBefore($("#queryset_drop_zone"));

  tag.find(".name").on('blur keyup paste', [nq,tag.find(".name")], function(event) {
      event.data[0].uri = event.data[1].text();
      localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
      validate();
    }
  );

  tag.find(".edit").click(function() {
    tag.find(".edit").hide();
    tag.find(".h").show();
    tag.find("label").next("div").show();
  });
  tag.find(".h").click(function() {
    tag.find(".edit").show();
    tag.find(".h").hide();
    tag.find("label").next("div").hide();
  });

  var sge = SGE.fromTextArea(tag.find("textarea")[0], 
  {
    lineNumbers: true,
  });
  sge.setValue(nq.string);
  sge.on("change", function(){
    nq.string = sge.getValue();
    localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
    validate();
  });

  namedqueries_editors.push(sge);
  namedqueries_tags.push(tag);
  
  tag.find("label").next("div").hide();

  tag.find(".delete").click((function(nq, sge, tag) { return function() {
    if(!confirm("Permanently delete this named query?")) {
      return;
    }
    for(var i=0;i<namedqueries.length;i++) {
      if(nq == namedqueries[i]) {
        namedqueries.splice(i,1);
        namedqueries_editors.splice(i,1);
        namedqueries_tags.splice(i,1);
      }
    }
    tag.remove();
    localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
    validate();
  }})(nq, sge, tag));

}



///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
// dataset

var defaultgraph_string,
 defaultgraph_tag,
 defaultgraph_editor,
 namedgraphs = [],
 namedgraphs_editors = [],
 namedgraphs_tags = [];

var load_dataset = function() {
  defaultgraph_string = localStorage.getItem('defaultgraph');
  if(defaultgraph_string == null) {
    defaultgraph_string = "";
    localStorage.setItem('defaultgraph', defaultgraph_string);
  }

  // show default graph
  defaultgraph_tag = $(`<div id='default_graph'>
      <label>Default graph (<a class='edit'>edit</a><a class='h' style='display:none'>hide</a>)</label>
      <textarea></textarea>
    </div>`)
  .insertBefore($("#dataset_drop_zone"));

  defaultgraph_editor = YATE.fromTextArea($('#default_graph textarea')[0], {
    createShareLink: false,
    lineNumbers: true
  });
  defaultgraph_editor.setValue(defaultgraph_string);
  defaultgraph_editor.on("change", function(){
      defaultgraph_string = defaultgraph_editor.getValue();
      localStorage.setItem('defaultgraph', defaultgraph_string);
      validate();
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
  defaultgraph_tag.find("label").next("div").hide();

  // load named graphs
  namedgraphs = localStorage.getItem('namedgraphs');
  if(namedgraphs !== null) {
    try {
      namedgraphs = JSON.parse(namedgraphs); 
    }
    catch(err){
      namedgraphs = [];
      localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
    }
  } else {
    namedgraphs = [];
    localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
  }

  // show named graphs
  for(var i=0; i<namedgraphs.length; i++) {
    show_namedgraph(namedgraphs[i]);
  }

  $("#dataset_drop_zone")
  .click(function() {
    var ng = {
      uri: "http://example.org/graph#" + namedgraphs.length,
      mediatype: "text/turtle",
      string: ""
    };
    namedgraphs.push(ng);
    localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
    show_namedgraph(ng);
    validate();
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
              mediatype: "text/turtle",
              string: event.target.result
            };
            namedgraphs.push(ng);
            localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
            show_namedgraph(ng);
            validate();
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
            mediatype: "text/turtle",
            string: event.target.result
          };
          namedgraphs.push(ng);
          localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
          show_namedgraph(ng);
          validate();
        }})(i,dt.files[i]);
        reader.readAsText(dt.files[i]);
      }  
    }
  })
  .on('dragover', function(ev) {
    ev.preventDefault();
  });  
}

var show_namedgraph = function(ng) {
  var tag = $("<div>")
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

  tag.find(".name").on('blur keyup paste', [ng,tag.find(".name")], function(event) {
      event.data[0].uri = event.data[1].text();
      localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
      validate();
    }
  );

  tag.find(".edit").click(function() {
    tag.find(".edit").hide();
    tag.find(".h").show();
    tag.find("label").next("div").show();
  });
  tag.find(".h").click(function() {
    tag.find(".edit").show();
    tag.find(".h").hide();
    tag.find("label").next("div").hide();
  });

  var yate = YATE.fromTextArea(tag.find("textarea")[0], 
  {
    lineNumbers: true,
  });
  yate.setValue(ng.string);
  yate.on("change", function(){
    ng.string = yate.getValue();
    localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
    validate();
  });

  namedgraphs_editors.push(yate);
  namedgraphs_tags.push(tag);

  tag.find("label").next("div").hide();

  tag.find(".delete").click((function(ng, yate, tag) { return function() {
    if(!confirm("Permanently delete this named graph?")) {
      return;
    }
    for(var i=0;i<namedgraphs.length;i++) {
      if(ng == namedgraphs[i]) {
        namedgraphs.splice(i,1);
        namedgraphs_editors.splice(i,1);
        namedgraphs_tags.splice(i,1);
      }
    }
    tag.remove();
    localStorage.setItem('namedgraphs', JSON.stringify(namedgraphs));
    validate();
  }})(ng, yate, tag));
}



///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  documentset

var documentset,
 documentset_editors = [],
 documentset_tags = [];


var load_documentset = function() {
  // load documentset 
  documentset = localStorage.getItem('documentset');
  if(documentset !== null) {
    try {
      documentset = JSON.parse(documentset); 
    }
    catch(err){
      documentset = [];
      localStorage.setItem('documentset', JSON.stringify(documentset));
    }
  } else {
    documentset = [];
    localStorage.setItem('documentset', JSON.stringify(documentset));
  }

  // show named documents
  for(var i=0; i<documentset.length; i++) {
    show_nameddocument(documentset[i]);
  }

  // activate drop zone
  $("#documentset_drop_zone")
  .click(function() {
    var doc = {
      uri: "http://example.org/document#" + documentset.length,
      mediatype: "text/plain",
      string: ""
    };
    documentset.push(doc);
    localStorage.setItem('documentset', JSON.stringify(documentset));
    show_nameddocument(doc);
    validate();
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
            var doc = {
              uri: f.name,
              mediatype: CodeMirror.findModeByFileName(f.name).mime,
              string: event.target.result
            };
            documentset.push(doc);
            localStorage.setItem('documentset', JSON.stringify(documentset));
            show_nameddocument(doc);
            validate();
          }})(i,f);
          reader.readAsText(f);
        }
      }
    } else {
      // Use DataTransfer interface to access the file(s)
      for (var i=0; i < dt.files.length; i++) {
        reader = new FileReader();
        reader.onload = (function(i,f){return function (event) {
          var doc = {
            uri: f.name,
            mediatype: CodeMirror.findModeByFileName(f.name).mime,
            string: event.target.result
          };
          documentset.push(doc);
          localStorage.setItem('documentset', JSON.stringify(documentset));
          show_nameddocument(doc);
          validate();
        }})(i,dt.files[i]);
        reader.readAsText(dt.files[i]);
      }  
    }
  })
  .on('dragover', function(ev) {
    ev.preventDefault();
  });  
}

var show_nameddocument = function(doc) {
  var tag = $("<div>")
    .attr("class","named_document")
    .append($("<label>")
      .append("Document named by URI <")
      .append(
        $("<span>")
        .attr("contenteditable", true)
        .attr("class","name")
        .text(doc.uri)
      )
      .append("> and typed with media type \"")
      .append(
        $("<span>")
        .attr("contenteditable", true)
        .attr("class","media")
        .text(doc.mediatype)
      )
      .append("\" (<a class='edit'>edit</a><a class='h' style='display:none'>hide</a>, <a class='delete'>delete</a>) ")
    )
    .append($("<textarea>"))
    .insertBefore($("#documentset_drop_zone"));

  tag.find(".name").on('blur keyup paste', [doc,tag.find(".name")], function(event) {
      event.data[0].uri = event.data[1].text();
      localStorage.setItem('documentset', JSON.stringify(documentset));
      validate();
    }
  );

  tag.find(".edit").click(function() {
    tag.find(".edit").hide();
    tag.find(".h").show();
    tag.find("label").nextAll("div").show();
  });
  tag.find(".h").click(function() {
    tag.find(".edit").show();
    tag.find(".h").hide();
    tag.find("label").nextAll("div").hide();
  });

  var editor = CodeMirror.fromTextArea(tag.find("textarea")[0], 
  {
    lineNumbers: true,
  });
  if(doc && doc.string) {
    editor.setValue(doc.string);
  }
  editor.on("change", function(){
    doc.string = editor.getValue();
    localStorage.setItem('documentset', JSON.stringify(documentset));
    validate();
  });
  var info = CodeMirror.findModeByMIME(doc.mediatype);
  if (info && info.mode) {
    editor.setOption("mode", doc.mediatype);
    CodeMirror.autoLoadMode(editor, info.mode);
  }

  documentset_editors.push(editor);
  documentset_tags.push(tag);

  tag.find("label").nextAll("div").hide();

  tag.find(".delete").click((function(doc, editor, tag) { return function() {
    if(!confirm("Permanently delete this document?")) {
      return;
    }
    for(var i=0;i<documentset.length;i++) {
      if(doc == documentset[i]) {
        documentset.splice(i,1);
        documentset_editors.splice(i,1);
        documentset_tags.splice(i,1);
      }
    }
    tag.remove();
    localStorage.setItem('documentset', JSON.stringify(documentset));
    validate();
  };})(doc, editor, tag));


  tag.find(".media").on('blur keyup paste', (function(doc, editor, mediatype_tag) { return function() {
      doc.mediatype = mediatype_tag.text();
      var info = CodeMirror.findModeByMIME(doc.mediatype);
      if (info && info.mode) {
        editor.setOption("mode", doc.mediatype);
        CodeMirror.autoLoadMode(editor, info.mode);
      }
      localStorage.setItem('documentset', JSON.stringify(documentset));
      validate();
    };})(doc,editor,tag.find(".media")));

}

///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  result

var generateresult;
var selectresult;
var templateresult;
var levels = {"TRACE": 5,
                "DEBUG": 4,
                "INFO": 3,
                "WARN": 2,
                "ERROR":1};

var load_result = function() {
  selectresult = $("#selectresult > textarea");
  templateresult = $("#templateresult > textarea");
  $('#run').on('click',function() { 
    if(!auto) {
        run();
    }
  });
  $('#autocheck').change(function () {
    if ($("#autocheck").is(":checked")) {
        auto = true;
    } else {
        auto = false;
    }
  });
  $('#streamcheck').change(function () {
    if ($("#streamcheck").is(":checked")) {
        stream = true;
    } else {
        stream = false;
    }
  });
  generateresult = YATE.fromTextArea(document.getElementById('generateresult'), {
  "readOnly": true, 
  "createShareLink": false});
  $("#loglevel").on('input propertychange', manage_log_level);
  generateresult.setValue("");
  $("#log pre").empty();
}


var manage_log_level = function() {
    loglevel = parseInt($("#loglevel").val());
    for(var level in levels) {
        if(loglevel >= levels[level]) {
            $(".log." + level).show();          
        } else {
            $(".log." + level).hide();                        
        }
    }
    $("#log pre").scrollTop($("#log pre")[0].scrollHeight);
}

///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  tests

var load_all = function() {

  $.getJSON("api/example/generate", function( data ) {
      for(var i = 0 ; i<data.length ;i++ ) {
        var select = window.location.hash.substring(4).endsWith(data[i]);
        $("#generateexs").append("<option value='"+data[i]+"'"+(select?"selected":"")+">"+data[i]+"</option>");
      }           
  });

  $.getJSON("api/example/select", function( data ) {
      for(var i = 0 ; i<data.length ;i++ ) {
        var select = window.location.hash.substring(4).endsWith(data[i]);
        $("#selectexs").append("<option value='"+data[i]+"'"+(select?"selected":"")+">"+data[i]+"</option>");
      }            
  });

  $.getJSON("api/example/template", function( data ) {
      for(var i = 0 ; i<data.length ;i++ ) {
        var select = window.location.hash.substring(4).endsWith(data[i]);
        $("#templateexs").append("<option value='"+data[i]+"'"+(select?"selected":"")+">"+data[i]+"</option>");
      }            
  });

  $("#generateexs").change(function() {
    var ex = $("#generateexs")[0].value;
    if(ex!=="---") {
        load("example/generate/" + ex);
    }
  });

  $("#selectexs").change(function() {
    var ex = $("#selectexs")[0].value;
    if(ex!=="---") {
        load("example/select/" + ex);
    }
  });

  $("#templateexs").change(function() {
    var ex = $("#templateexs")[0].value;
    if(ex!=="---") {
        load("example/template/" + ex);
    }
  });

}

var load = function(idd) {
  $.getJSON(`api/${idd}`, function( data ) {
        location.hash="ex="+idd;
        send({ cancel: true });
        localStorage.setItem('readme', data.readme ? data.readme : "");
        localStorage.setItem('defaultquery', data.defaultquery);
        localStorage.setItem('namedqueries', JSON.stringify(data.namedqueries));
        localStorage.setItem('defaultgraph', data.defaultgraph);
        localStorage.setItem('namedgraphs', JSON.stringify(data.namedgraphs));
        localStorage.setItem('documentset', JSON.stringify(data.documentset));
        if(data.stream === true || data.stream === false) {
          stream = data.stream;
        }
        if(data.loglevel >= 1 && data.loglevel <=5) {
          loglevel = data.loglevel;
        }
        init();
      });
}

var processMessage = function(responses) {
    for(var i= 0; i<responses.length; i++) {
      data = responses[i];
      if(data.clear === true) {
        generateresult.setValue("");
        $(generateresult.getWrapperElement()).hide();
        selectresult.val("");
        selectresult.parent().hide();
        templateresult.val("");
        templateresult.parent().hide();
        $("#log pre").empty();
      } 
      if(data.result && data.result != "") {
        if(data.type == 0) {
            $(generateresult.getWrapperElement()).show();
            selectresult.parent().hide();
            templateresult.parent().hide();
            generateresult.replaceRange("\n" + data.result, CodeMirror.Pos(generateresult.lastLine()));
        } else if(data.type == 1) {
            $(generateresult.getWrapperElement()).hide();
            selectresult.parent().show();
            selectresult.val(function( index, val ) {
              return val + "\n" + data.result;
            });
            selectresult.prop("readonly", true)
            templateresult.parent().hide();
        } else if(data.type == 2) {
            $(generateresult.getWrapperElement()).hide();
            selectresult.parent().hide();
            templateresult.parent().show();
            templateresult.val(function( index, val ) {
              return val + "\n" + data.result;
            });
            templateresult.prop("readonly", true)
        }
      }
      if(data.log && data.log != "") {
        var span = $("<span>")
                .addClass("log")
                .append(data.log.replace(/</g, "&lt;"));
        $('#log pre').append(span);
        for(var level in levels) {
            if(data.log.startsWith(level)) {
                span.addClass(level);
            }
        }
      }
    }
    $("#log pre").scrollTop($("#log pre")[0].scrollHeight);
}


///////////////////////////////////////
//  getQueryStringValue

function getMsgValue () {  
  if(window.location.hash.startsWith("#msg=")) {
    var compressed = window.location.hash.substring(5);
    return JSON.parse(LZString.decompressFromEncodedURIComponent(compressed));
  }
} 

var copyPermaURL = function() {
  var perma;

  var msg = {
    defaultquery: defaultquery_string,
    namedqueries: namedqueries,
    defaultgraph: defaultgraph_string,
    namedgraphs: namedgraphs,
    documentset: documentset,
    stream: stream,
    loglevel: loglevel
  };

  var playgroundurl = window.location.protocol + "//" + window.location.hostname + (window.location.port!="" ? ":" + window.location.port : "") + "/sparql-generate/playground.html";
  var compressed = LZString.compressToEncodedURIComponent(JSON.stringify(msg));
  if(compressed.length>10000) {
    alert("Too much information to be incoded in the URL. Limit is 10000, this setting has " + compressed.length);
    return;
  }
  perma = playgroundurl + "#msg=" + compressed;
  $("#permaURL").val(perma);
  $("#permaURL").show();
  document.getElementById("permaURL").select();
  document.execCommand("copy");
  $("#permaURL").hide();
}


///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  init

var init = function() { 
  
  $("#readme").empty();
  $("#form").empty();
  $("#form").append(`
    <div class="col-lg-6">
      <div id="queryset" class="fieldset">
        <legend>Queries <button onclick="copyPermaURL()">Copy URL to share this setting</button>  <input type="text" id="permaURL" value="" style="display:none"/> </legend>
        <p>Links to the documentation of <a href="apidocs/fr/emse/ci/sparqlext/iterator/library/package-summary.html">iterator functions</a> and <a href="apidocs/fr/emse/ci/sparqlext/function/library/package-summary.html">binding functions</a>.</p>
        <div id="queryset_drop_zone" class="drop_zone">
          <strong>Click to add a query, or drag and drop files ...</strong>
        </div>
      </div>
      <div id="documentset_list" class="fieldset">
        <legend>Documentset</legend>
        <div id="documentset_drop_zone" class="drop_zone">
          <strong>Click to add a document, or drag and drop files ...</strong>
        </div>
      </div>
      <div id="dataset" class="fieldset">
        <legend>Dataset</legend>
        <div id="dataset_drop_zone" class="drop_zone">
          <strong>Click to add a graph, or drag and drop turtle files ...</strong>
        </div>
      </div>
    </div>
    <div class="col-lg-6">
      <div class="fieldset">
        <div id="result_list">
          <button id="run">Run Query</button>
          <label><input type="checkbox" id="autocheck"/> run automatically</label>
          <label><input type="checkbox" id="streamcheck"/> return stream</label>
          <legend>Result</legend>
          <textarea id="generateresult"> </textarea>
          <div id="selectresult" style="display:none;" readonly><textarea> </textarea></div>
          <div id="templateresult" style="display:none;" readonly><textarea> </textarea></div>
        </div>
        <div id="log">
          <legend>Log</legend>
          <input id="loglevel" type="range" value="5" min="1" max="5"></input>
          <p>Note that reducing the log level only limits network traffic. On the server side log level is 'TRACE'. For fastest transformations, you should use the <a href="language-cli.html">Executable JAR</a> or the <a href="get-started.html">Java library</a>.</p>
          <pre></pre>
        </div>
      </div>
    </div>`);
  buttons();
    
    
  namedqueries = [],
  namedqueries_editors = [],
  namedqueries_tags = [];
  namedgraphs = [],
  namedgraphs_editors = [],
  namedgraphs_tags = [];
  documentset = [],
  documentset_editors = [],
  documentset_tags = [];


  $("#readme").html(localStorage.getItem('readme'));
  $("#autocheck").prop("checked", auto);
  $("#streamcheck").prop("checked", stream);
  $("#loglevel").val(loglevel);

  load_queryset();
  load_dataset();
  load_documentset();
  load_result();

  validate();
}

var buttons = function() { "use strict";var e=window.document,t=e.location,o=window.encodeURIComponent,r=window.decodeURIComponent,n=window.Math,a=window.HTMLElement,i=window.XMLHttpRequest,l="https://buttons.github.io/buttons.html",c=i&&i.prototype&&"withCredentials"in i.prototype,d=c&&a&&a.prototype.attachShadow&&!a.prototype.attachShadow.prototype,s=function(e,t,o){e.addEventListener?e.addEventListener(t,o):e.attachEvent("on"+t,o)},u=function(e,t,o){e.removeEventListener?e.removeEventListener(t,o):e.detachEvent("on"+t,o)},h=function(e,t,o){var r=function(n){return u(e,t,r),o(n)};s(e,t,r)},f=function(e,t,o){var r=function(n){if(t.test(e.readyState))return u(e,"readystatechange",r),o(n)};s(e,"readystatechange",r)},p=function(e){return function(t,o,r){var n=e.createElement(t);if(o)for(var a in o){var i=o[a];null!=i&&(null!=n[a]?n[a]=i:n.setAttribute(a,i))}if(r)for(var l=0,c=r.length;l<c;l++){var d=r[l];n.appendChild("string"==typeof d?e.createTextNode(d):d)}return n}},g=p(e),b=function(e){var t;return function(){t||(t=1,e.apply(this,arguments))}},m="body{margin:0}a{color:#24292e;text-decoration:none;outline:0}.octicon{display:inline-block;vertical-align:text-top;fill:currentColor}.widget{display:inline-block;overflow:hidden;font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Helvetica, Arial, sans-serif;font-size:0;white-space:nowrap;-webkit-user-select:none;-moz-user-select:none;-ms-user-select:none;user-select:none}.btn,.social-count{display:inline-block;height:14px;padding:2px 5px;font-size:11px;font-weight:600;line-height:14px;vertical-align:bottom;cursor:pointer;border:1px solid #c5c9cc;border-radius:0.25em}.btn{background-color:#eff3f6;background-image:-webkit-linear-gradient(top, #fafbfc, #eff3f6 90%);background-image:-moz-linear-gradient(top, #fafbfc, #eff3f6 90%);background-image:linear-gradient(180deg, #fafbfc, #eff3f6 90%);background-position:-1px -1px;background-repeat:repeat-x;background-size:110% 110%;border-color:rgba(27,31,35,0.2);-ms-filter:\"progid:DXImageTransform.Microsoft.Gradient(startColorstr='#FFFAFBFC', endColorstr='#FFEEF2F5')\";*filter:progid:DXImageTransform.Microsoft.Gradient(startColorstr='#FFFAFBFC', endColorstr='#FFEEF2F5')}.btn:active{background-color:#e9ecef;background-image:none;border-color:#a5a9ac;border-color:rgba(27,31,35,0.35);box-shadow:inset 0 0.15em 0.3em rgba(27,31,35,0.15)}.btn:focus,.btn:hover{background-color:#e6ebf1;background-image:-webkit-linear-gradient(top, #f0f3f6, #e6ebf1 90%);background-image:-moz-linear-gradient(top, #f0f3f6, #e6ebf1 90%);background-image:linear-gradient(180deg, #f0f3f6, #e6ebf1 90%);border-color:#a5a9ac;border-color:rgba(27,31,35,0.35);-ms-filter:\"progid:DXImageTransform.Microsoft.Gradient(startColorstr='#FFF0F3F6', endColorstr='#FFE5EAF0')\";*filter:progid:DXImageTransform.Microsoft.Gradient(startColorstr='#FFF0F3F6', endColorstr='#FFE5EAF0')}.social-count{position:relative;margin-left:5px;background-color:#fff}.social-count:focus,.social-count:hover{color:#0366d6}.social-count b,.social-count i{position:absolute;top:50%;left:0;display:block;width:0;height:0;margin:-4px 0 0 -4px;border:solid transparent;border-width:4px 4px 4px 0;_line-height:0;_border-top-color:red !important;_border-bottom-color:red !important;_border-left-color:red !important;_filter:chroma(color=red)}.social-count b{border-right-color:#c5c9cc}.social-count i{margin-left:-3px;border-right-color:#fff}.lg .btn,.lg .social-count{height:16px;padding:5px 10px;font-size:12px;line-height:16px}.lg .social-count{margin-left:6px}.lg .social-count b,.lg .social-count i{margin:-5px 0 0 -5px;border-width:5px 5px 5px 0}.lg .social-count i{margin-left:-4px}\n",v={"mark-github":{width:16,height:16,path:'<path fill-rule="evenodd" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z"/>'},eye:{width:16,height:16,path:'<path fill-rule="evenodd" d="M8.06 2C3 2 0 8 0 8s3 6 8.06 6C13 14 16 8 16 8s-3-6-7.94-6zM8 12c-2.2 0-4-1.78-4-4 0-2.2 1.8-4 4-4 2.22 0 4 1.8 4 4 0 2.22-1.78 4-4 4zm2-4c0 1.11-.89 2-2 2-1.11 0-2-.89-2-2 0-1.11.89-2 2-2 1.11 0 2 .89 2 2z"/>'},star:{width:14,height:16,path:'<path fill-rule="evenodd" d="M14 6l-4.9-.64L7 1 4.9 5.36 0 6l3.6 3.26L2.67 14 7 11.67 11.33 14l-.93-4.74L14 6z"/>'},"repo-forked":{width:10,height:16,path:'<path fill-rule="evenodd" d="M8 1a1.993 1.993 0 0 0-1 3.72V6L5 8 3 6V4.72A1.993 1.993 0 0 0 2 1a1.993 1.993 0 0 0-1 3.72V6.5l3 3v1.78A1.993 1.993 0 0 0 5 15a1.993 1.993 0 0 0 1-3.72V9.5l3-3V4.72A1.993 1.993 0 0 0 8 1zM2 4.2C1.34 4.2.8 3.65.8 3c0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2zm3 10c-.66 0-1.2-.55-1.2-1.2 0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2zm3-10c-.66 0-1.2-.55-1.2-1.2 0-.65.55-1.2 1.2-1.2.65 0 1.2.55 1.2 1.2 0 .65-.55 1.2-1.2 1.2z"/>'},"issue-opened":{width:14,height:16,path:'<path fill-rule="evenodd" d="M7 2.3c3.14 0 5.7 2.56 5.7 5.7s-2.56 5.7-5.7 5.7A5.71 5.71 0 0 1 1.3 8c0-3.14 2.56-5.7 5.7-5.7zM7 1C3.14 1 0 4.14 0 8s3.14 7 7 7 7-3.14 7-7-3.14-7-7-7zm1 3H6v5h2V4zm0 6H6v2h2v-2z"/>'},"cloud-download":{width:16,height:16,path:'<path fill-rule="evenodd" d="M9 12h2l-3 3-3-3h2V7h2v5zm3-8c0-.44-.91-3-4.5-3C5.08 1 3 2.92 3 5 1.02 5 0 6.52 0 8c0 1.53 1 3 3 3h3V9.7H3C1.38 9.7 1.3 8.28 1.3 8c0-.17.05-1.7 1.7-1.7h1.3V5c0-1.39 1.56-2.7 3.2-2.7 2.55 0 3.13 1.55 3.2 1.8v1.2H12c.81 0 2.7.22 2.7 2.2 0 2.09-2.25 2.2-2.7 2.2h-2V11h2c2.08 0 4-1.16 4-3.5C16 5.06 14.08 4 12 4z"/>'}},w={},x=function(e,t,o){var r=p(e.ownerDocument),n=e.appendChild(r("style",{type:"text/css"}));n.styleSheet?n.styleSheet.cssText=m:n.appendChild(e.ownerDocument.createTextNode(m));var a,l,d=r("a",{className:"btn",href:t.href,target:"_blank",innerHTML:(a=t["data-icon"],l=/^large$/i.test(t["data-size"])?16:14,a=(""+a).toLowerCase().replace(/^octicon-/,""),v.hasOwnProperty(a)||(a="mark-github"),'<svg version="1.1" width="'+l*v[a].width/v[a].height+'" height="'+l+'" viewBox="0 0 '+v[a].width+" "+v[a].height+'" class="octicon octicon-'+a+'" aria-hidden="true">'+v[a].path+"</svg>"),"aria-label":t["aria-label"]||void 0},[" ",r("span",{},[t["data-text"]||""])]);/\.github\.com$/.test("."+d.hostname)?/^https?:\/\/((gist\.)?github\.com\/[^\/?#]+\/[^\/?#]+\/archive\/|github\.com\/[^\/?#]+\/[^\/?#]+\/releases\/download\/|codeload\.github\.com\/)/.test(d.href)&&(d.target="_top"):(d.href="#",d.target="_self");var u,h,g,x,y=e.appendChild(r("div",{className:"widget"+(/^large$/i.test(t["data-size"])?" lg":"")},[d]));/^(true|1)$/i.test(t["data-show-count"])&&"github.com"===d.hostname&&(u=d.pathname.replace(/^(?!\/)/,"/").match(/^\/([^\/?#]+)(?:\/([^\/?#]+)(?:\/(?:(subscription)|(fork)|(issues)|([^\/?#]+)))?)?(?:[\/?#]|$)/))&&!u[6]?(u[2]?(h="/repos/"+u[1]+"/"+u[2],u[3]?(x="subscribers_count",g="watchers"):u[4]?(x="forks_count",g="network"):u[5]?(x="open_issues_count",g="issues"):(x="stargazers_count",g="stargazers")):(h="/users/"+u[1],g=x="followers"),function(e,t){var o=w[e]||(w[e]=[]);if(!(o.push(t)>1)){var r=b(function(){for(delete w[e];t=o.shift();)t.apply(null,arguments)});if(c){var n=new i;s(n,"abort",r),s(n,"error",r),s(n,"load",function(){var e;try{e=JSON.parse(n.responseText)}catch(e){return void r(e)}r(200!==n.status,e)}),n.open("GET",e),n.send()}else{var a=this||window;a._=function(e){a._=null,r(200!==e.meta.status,e.data)};var l=p(a.document)("script",{async:!0,src:e+(/\?/.test(e)?"&":"?")+"callback=_"}),d=function(){a._&&a._({meta:{}})};s(l,"load",d),s(l,"error",d),l.readyState&&f(l,/de|m/,d),a.document.getElementsByTagName("head")[0].appendChild(l)}}}.call(this,"https://api.github.com"+h,function(e,t){if(!e){var n=t[x];y.appendChild(r("a",{className:"social-count",href:t.html_url+"/"+g,target:"_blank","aria-label":n+" "+x.replace(/_count$/,"").replace("_"," ").slice(0,n<2?-1:void 0)+" on GitHub"},[r("b"),r("i"),r("span",{},[(""+n).replace(/\B(?=(\d{3})+(?!\d))/g,",")])]))}o&&o(y)})):o&&o(y)},y=window.devicePixelRatio||1,C=function(e){return(y>1?n.ceil(n.round(e*y)/y*2)/2:n.ceil(e))||0},F=function(e,t){e.style.width=t[0]+"px",e.style.height=t[1]+"px"},k=function(t,r){if(null!=t&&null!=r)if(t.getAttribute&&(t=function(e){for(var t={href:e.href,title:e.title,"aria-label":e.getAttribute("aria-label")},o=["icon","text","size","show-count"],r=0,n=o.length;r<n;r++){var a="data-"+o[r];t[a]=e.getAttribute(a)}return null==t["data-text"]&&(t["data-text"]=e.textContent||e.innerText),t}(t)),d){var a=g("span",{title:t.title||void 0});x(a.attachShadow({mode:"closed"}),t,function(){r(a)})}else{var i=g("iframe",{src:"javascript:0",title:t.title||void 0,allowtransparency:!0,scrolling:"no",frameBorder:0});F(i,[0,0]),i.style.border="none";var c=function(){var a,d=i.contentWindow;try{a=d.document.body}catch(t){return void e.body.appendChild(i.parentNode.removeChild(i))}u(i,"load",c),x.call(d,a,t,function(e){var a=function(e){var t=e.offsetWidth,o=e.offsetHeight;if(e.getBoundingClientRect){var r=e.getBoundingClientRect();t=n.max(t,C(r.width)),o=n.max(o,C(r.height))}return[t,o]}(e);i.parentNode.removeChild(i),h(i,"load",function(){F(i,a)}),i.src=l+"#"+(i.name=function(e){var t=[];for(var r in e){var n=e[r];null!=n&&t.push(o(r)+"="+o(n))}return t.join("&")}(t)),r(i)})};s(i,"load",c),e.body.appendChild(i)}};t.protocol+"//"+t.host+t.pathname===l?x(e.body,function(e){for(var t={},o=e.split("&"),n=0,a=o.length;n<a;n++){var i=o[n];if(""!==i){var l=i.split("=");t[r(l[0])]=null!=l[1]?r(l.slice(1).join("=")):void 0}}return t}(window.name||t.hash.replace(/^#/,""))):function(t){if(/m/.test(e.readyState)||!/g/.test(e.readyState)&&!e.documentElement.doScroll)setTimeout(t);else if(e.addEventListener){var o=b(t);h(e,"DOMContentLoaded",o),h(window,"load",o)}else f(e,/m/,t)}(function(){for(var t=e.querySelectorAll?e.querySelectorAll("a.github-button"):function(){for(var t=[],o=e.getElementsByTagName("a"),r=0,n=o.length;r<n;r++)~(" "+o[r].className+" ").replace(/[ \t\n\f\r]+/g," ").indexOf(" github-button ")&&t.push(o[r]);return t}(),o=0,r=t.length;o<r;o++)!function(e){k(e,function(t){e.parentNode.replaceChild(t,e)})}(t[o])})
}

$(document).ready(function() {
  $(".main-body").parent().empty().removeClass("container").addClass("container-fluid").append(`
  <h1>SPARQL-Generate and SPARQL-Template (STTL) Playground</h1>
  <div id="gh-buttons">
    <a class="github-button" href="https://github.com/sparql-generate/sparql-generate/subscription" data-icon="octicon-eye" data-size="large" data-show-count="true" aria-label="Watch sparql-generate/sparql-generate on GitHub">Watch</a>
    <a class="github-button" href="https://github.com/sparql-generate/sparql-generate" data-icon="octicon-star" data-size="large" data-show-count="true" aria-label="Star sparql-generate/sparql-generate on GitHub">Star</a>
  </div>
  <p>Load 
    <label for="generateex">GENERATE example</label> <select name="generateex" id="generateexs"><option value="---">---</option></select>
     - <label for="selectex">SELECT example</label> <select name="selectex" id="selectexs"><option value="---">---</option></select>
     - <label for="templateex">TEMPLATE example</label> <select name="templateex" id="templateexs"><option value="---">---</option></select>
  </p>

  <div id="readme"></div>
  <div id="form" class="row"></div>`);

  var data = getMsgValue();
  if(data) {
    localStorage.setItem('readme',  data.readme ? data.readme : "");
    localStorage.setItem('defaultquery', data.defaultquery);
    localStorage.setItem('namedqueries', JSON.stringify(data.namedqueries));
    localStorage.setItem('defaultgraph', data.defaultgraph);
    localStorage.setItem('namedgraphs', JSON.stringify(data.namedgraphs));
    localStorage.setItem('documentset', JSON.stringify(data.documentset));
    if(data.stream === true || data.stream === false) {
      stream = data.stream;
    }
    if(data.loglevel >= 1 && data.Loglevel <=5) {
      loglevel = data.loglevel;
    }
    init();
  } else if(window.location.hash.startsWith("#ex=")) {
      load(window.location.hash.substring(4))
  } else {
    init();
  }

  load_all();
  openSocket();

});

$(window).on('hashchange', function() {
  var data = getMsgValue();
  if(data) {
    localStorage.setItem('readme',  data.readme ? data.readme : "");
    localStorage.setItem('defaultquery', data.defaultquery);
    localStorage.setItem('namedqueries', JSON.stringify(data.namedqueries));
    localStorage.setItem('defaultgraph', data.defaultgraph);
    localStorage.setItem('namedgraphs', JSON.stringify(data.namedgraphs));
    localStorage.setItem('documentset', JSON.stringify(data.documentset));
    if(data.stream === true || data.stream === false) {
      stream = data.stream;
    }
    if(data.loglevel >= 1 && data.Loglevel <=5) {
      loglevel = data.loglevel;
    }
    init();
  } else if(window.location.hash.startsWith("#ex=")) {
      load(window.location.hash.substring(4))
  }
});