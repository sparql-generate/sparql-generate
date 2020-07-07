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
var debugTemplate = true;
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
        debugTemplate: debugTemplate,
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
        debugTemplate: debugTemplate,
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
  $('#debugtemplatecheck').change(function () {
    if ($("#debugtemplatecheck").is(":checked")) {
        debugTemplate = true;
    } else {
        debugTemplate = false;
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
        $("#selectexs").val("---");
        $("#templateexs").val("---");
    }
  });

  $("#selectexs").change(function() {
    var ex = $("#selectexs")[0].value;
    if(ex!=="---") {
        load("example/select/" + ex);
        $("#generateexs").val("---");
        $("#templateexs").val("---");
    }
  });

  $("#templateexs").change(function() {
    var ex = $("#templateexs")[0].value;
    if(ex!=="---") {
        load("example/template/" + ex);
        $("#generateexs").val("---");
        $("#selectexs").val("---");
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
        if(data.debugTemplate === true || data.debugTemplate === false) {
          debugTemplate = data.debugTemplate;
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
        <legend>Queries</legend>
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
          <label><input type="checkbox" id="debugtemplatecheck" checked="checked"/> debug Template</label>
          <legend>Result</legend>
          <textarea id="generateresult"> </textarea>
          <div id="selectresult" style="display:none;" readonly><textarea> </textarea></div>
          <div id="templateresult" style="display:none;" readonly><textarea> </textarea></div>
        </div>
        <div id="log">
          <legend>Log</legend>
          <input id="loglevel" type="range" value="5" min="1" max="5"></input>
          <p>Note that reducing the log level only limits network traffic. On the server side log level is 'TRACE'. For fastest transformations, you should use the <a href="sublime.html">Sublime Text IDE</a>, the <a href="language-cli.html">Executable JAR</a>, the <a href="get-started.html">Java library</a>.</p>
          <pre></pre>
        </div>
      </div>
    </div>`);
    
    
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
  $("#debugtemplatecheck").prop("checked", debugTemplate);
  $("#loglevel").val(loglevel);

  load_queryset();
  load_dataset();
  load_documentset();
  load_result();

  validate();
}

$(document).ready(function() {
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