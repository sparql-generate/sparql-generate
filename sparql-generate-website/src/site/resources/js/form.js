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
var timers = [];

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
  }
  for(var i=0;i<namedqueries_editors.length;i++) {
    var query = namedqueries[i];
    var editor = namedqueries_editors[i];
    var tag = namedqueries_tags[i];

    if(!editor.queryValid) {
      valid = false;
      tag.addClass("invalid");
      tag.children(":first").append(" <span class='invalidmsg'>This query is not valid.</span>");
    }
    for(var n of namedqueries) {
      if(query !== n && query.uri == n.uri && query.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another query has the same URI.</span>");        
      }
    }
    for(var n of documentset) {
      if(query.uri == n.uri && query.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A document has the same URI and mediatype 'application/vnd.sparql-generate'.</span>");        
      }
    }
  }

  // validating dataset
  if(!defaultgraph_editor.docValid) {
    valid = false;
    defaultgraph_tag.addClass("invalid");
    defaultgraph_tag.children(":first").append(" <span class='invalidmsg'>This graph is not valid.</span>");
  }
  for(var i=0;i<namedgraphs_editors.length;i++) {
    var graph = namedgraphs[i];
    var editor = namedgraphs_editors[i];
    var tag = namedgraphs_tags[i];

    if(!namedgraphs_editors[i].docValid) {
      valid = false;
      namedgraphs_tags[i].addClass("invalid");
      namedgraphs_tags[i].children(":first").append(" <span class='invalidmsg'>This graph is not valid.</span>");
    }
    for(var n of namedgraphs) {
      if(graph !== n && graph.uri == n.uri && graph.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another graph has the same URI.</span>");        
      }
    }
    for(var n of documentset) {
      if(graph.uri == n.uri && graph.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A document has the same URI and mediatype 'text/turtle'.</span>");        
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
    }
    for(var n of namedqueries) {
      if(doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A query has the same URI.</span>");        
      }
    }
    for(var n of namedgraphs) {
      if(doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A graph has the same URI.</span>");        
      }
    }
    for(var n of documentset) {
      if(doc !== n && doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        valid = false;
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another document has the same URI and mediatype.</span>");        
      }
    }
  }

  if(valid && open) {
    for(var timer of timers) {
      window.clearTimeout(timer);
    }
    timers = [];
    var msg = {
        defaultquery: defaultquery_string,
        namedqueries: namedqueries,
        defaultgraph: defaultgraph_string,
        namedgraphs: namedgraphs,
        documentset: documentset
    };
    timers.push(window.setTimeout(function(msg) {
      console.log("sending request");
      socket.send(JSON.stringify(msg));
    }, 100, msg));
  }
};

var resetErrors = function() {
  $(".invalid").removeClass("invalid");
  $(".invalidmsg").remove();
};

var init = function() { 
  $("#form").empty();
  $("#form").append(`
<div class="col-lg-6">
  <div id="queryset" class="fieldset">
    <legend>SPARQL-Generate Queries</legend>
    <p>See <a href="functions.html">our predefined SPARQL binding functions and SPARQL-Generate iterator functions</a>.</p>
    <div id="queryset_drop_zone" class="drop_zone">
      <strong>Click here to add a new named query, you can also drag SPARQL-Generate documents to load them ...</strong>
    </div>
  </div>
  <div id="documentset_list" class="fieldset">
    <legend>Documentset</legend>
    <div id="documentset_drop_zone" class="drop_zone">
      <strong>Click here to add a new document, you can also drag one or more files to load them ...</strong>
    </div>
    <div id="dataset" class="fieldset">
      <legend>Dataset</legend>
      <div id="dataset_drop_zone" class="drop_zone">
        <strong>Click here to add a new graph, you can also drag turtle documents to load them ...</strong>
      </div>
    </div>
  </div>
</div>
<div class="col-lg-6">
  <div class="fieldset">
    <div id="result_list">
      <legend>Result</legend>
      <textarea id="result"> </textarea>
    </div>
    <div id="log">
      <legend>Log</legend>
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

  load_queryset();
  load_dataset();
  load_documentset();
  load_result();
  validate();
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
    defaultquery_string = `PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX sgfn: <http://w3id.org/sparql-generate/fn/> 
LOOK UP <https://ci.mines-stetienne.fr/sparql-generate/cities.json> AS ?message
ITERATE sgiter:JSONListKeys( ?message ) AS ?cityName 
WHEREVER { 
  FILTER( STRSTARTS( ?cityName , "New" ) ) 
  BIND( sgfn:JSONPath( ?message, "$.['{ ?cityName }']" ) AS  ?city )
} 
CONSTRUCT {
  ITERATE sgiter:JSONListKeys( ?city ) AS ?key  
  CONSTRUCT {
    <city/{ ?cityName }> <{ ?key }> "{ sgfn:JSONPath( ?message , "$.['{ ?cityName }']['{ ?key }']" )  }"@en . 
  } .
}`;
    localStorage.setItem('defaultquery', defaultquery_string);
  }

  // show default query
  defaultquery_tag = $(`<div id='default_query'>
      <label>Default query</label>
      <textarea></textarea>
    </div>`)
  .insertBefore($("#queryset_drop_zone"));

  defaultquery_editor = YASQE.fromTextArea($('#default_query textarea')[0], {
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
      string: `PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
PREFIX sgfn: <http://w3id.org/sparql-generate/fn/> 

LOOK UP <> AS ?message
ITERATE sgiter:XPath( ?message ) AS ?var 
WHEREVER {  } 
CONSTRUCT {  }`
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

  var yasqe = YASQE.fromTextArea(tag.find("textarea")[0], 
  {
    lineNumbers: true,
  });
  yasqe.setValue(nq.string);
  yasqe.on("change", function(){
    nq.string = yasqe.getValue();
    localStorage.setItem('namedqueries', JSON.stringify(namedqueries));
    validate();
  });

  namedqueries_editors.push(yasqe);
  namedqueries_tags.push(tag);
  
  tag.find("label").next("div").hide();

  tag.find(".delete").click((function(nq, yasqe, tag) { return function() {
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
  }})(nq, yasqe, tag));

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

var result;

var load_result = function() {
  result = YATE.fromTextArea(document.getElementById('result'), {
  "readOnly": true, 
  "createShareLink": false});
}

///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  tests

var load_tests = function() {
  var http = new XMLHttpRequest();
  var url = "api/list";
  http.open("GET", url, true);
  http.onreadystatechange = function() {//Call a function when the state changes.
      if(http.readyState == 4 && http.status == 200) {
          tests = http.responseText.split("\n");
          for(var i = 0 ; i<tests.length ;i++ ) {
              if(tests[i]!=="") {
                  $("#tests").append("<option value='"+tests[i]+"'>"+tests[i]+"</option>");
              }
          }            
      }
  }
  http.send();

  $("#tests").change(function() {
    var test = $("#tests")[0].value;
    if(test!=="---") {
        load_test(test);
    }
  });

}

var load_test = function(id) {
  $.getJSON("api/list/"+id, function( data ) {
        localStorage.setItem('defaultquery', data.defaultquery);
        localStorage.setItem('namedqueries', JSON.stringify(data.namedqueries));
        localStorage.setItem('defaultgraph', data.defaultgraph);
        localStorage.setItem('namedgraphs', JSON.stringify(data.namedgraphs));
        localStorage.setItem('documentset', JSON.stringify(data.documentset));
        init();
      });
  
  $("#tabs").tabs("option", "active", 0);
}
 
$(document).ready(function() {
    
    
    
  $(".main-body").parent().empty().removeClass("container").addClass("container-fluid").append(`
  <h1>SPARQL-Generate Playground</h1>

  <p>You can <label for="test">load and try one of the unit tests:</label> <select name="test" id="tests"><option value="---">---</option></select></p>

  <div id="form" class="row"></div>`);
  init();
  load_tests();

  var websocketurl = "wss://" + window.location.hostname + (window.location.port!="" ? ":" + window.location.port : "") + "/sparql-generate-test/transformStream";
  socket = new WebSocket(websocketurl);
 
  socket.onopen = function (event) {
    open = true;
    console.log("websocket open");
    validate();
  };
   
  socket.onmessage = function (event) {
      if(event.data == "clear") {
        result.setValue("");
        $("#log pre").empty();
      } else {
        var data = JSON.parse(event.data);
        if(data.result && data.result != "") {
          result.setValue(result.getValue() + data.result);       
        }
        if(data.log && data.log != "") {
          $("#log pre").append(data.log.replace(/</g, "&lt;"));
        }
      }
  }
   
  socket.onclose = function (event) {
      console.log("websocket closed");
  }

});