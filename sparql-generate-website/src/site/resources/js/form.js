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

var validate = function() {

  // no two named thing with the same URI and mediatype
  // queries have mediatype application/vnd.sparql-generate
  // graphs have mediatype text/turtle

  // no query, graph or document has bad state 

  valid = true;
  resetErrors();

  var names = [];

  if(!queryset_editors[0].queryValid) {
    valid = false;
    queryset_tags[0].addClass("invalid");
    queryset_tags[0].children(":first").append(" <span class='invalidmsg'>This query is not valid.</span>");
  }
  for(var i=0;i<queryset_editors[1].length;i++) {
    var query = queryset[1][i];
    var editor = queryset_editors[1][i];
    var tag = queryset_tags[1][i];

    if(!editor.queryValid) {
      valid = false;
      tag.addClass("invalid");
      tag.children(":first").append(" <span class='invalidmsg'>This query is not valid.</span>");
    }
    for(var n of queryset[1]) {
      if(query !== n && query.uri == n.uri && query.mediatype == n.mediatype ) {
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another query has the same URI.</span>");        
      }
    }
    for(var n of documentset) {
      if(query.uri == n.uri && query.mediatype == n.mediatype ) {
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A document has the same URI and mediatype 'application/vnd.sparql-generate'.</span>");        
      }
    }
  }

  // validating dataset
  if(!dataset_editors[0].docValid) {
    valid = false;
    dataset_tags[0].addClass("invalid");
    dataset_tags[0].children(":first").append(" <span class='invalidmsg'>This graph is not valid.</span>");
  }
  for(var i=0;i<dataset_editors[1].length;i++) {
    var graph = dataset[1][i];
    var editor = dataset_editors[1][i];
    var tag = dataset_tags[1][i];

    if(!dataset_editors[1][i].docValid) {
      valid = false;
      dataset_tags[1][i].addClass("invalid");
      dataset_tags[1][i].children(":first").append(" <span class='invalidmsg'>This graph is not valid.</span>");
    }
    for(var n of dataset[1]) {
      if(graph !== n && graph.uri == n.uri && graph.mediatype == n.mediatype ) {
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another graph has the same URI.</span>");        
      }
    }
    for(var n of documentset) {
      if(graph.uri == n.uri && graph.mediatype == n.mediatype ) {
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
      tag.addClass("invalid");
      tag.children(":first").append(" <span class='invalidmsg'>The mediatype is invalid.</span>");        
    }
    for(var n of queryset[1]) {
      if(doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A query has the same URI.</span>");        
      }
    }
    for(var n of dataset[1]) {
      if(doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>A graph has the same URI.</span>");        
      }
    }
    for(var n of documentset) {
      if(doc !== n && doc.uri == n.uri && doc.mediatype == n.mediatype ) {
        tag.addClass("invalid");
        tag.children(":first").append(" <span class='invalidmsg'>Another document has the same URI and mediatype.</span>");        
      }
    }
  }

  


}

var resetErrors = function() {
  $(".invalid").removeClass("invalid");
  $(".invalidmsg").remove();
}

 
var init = function() {
  $("#bodyColumn").empty();
  $("#bodyColumn").append(`
 
  <h1>Try SPARQL-Generate</h1>

  <p> Edit the SPARQL-Generate query, an optional Dataset, and an optional Documentset over which it will be evaluated.</p>
  <p>See <a href="functions.html">our predefined SPARQL binding functions and SPARQL-Generate iterator functions</a>.</p>
  <p>You can also <label for="test">load one of the unit tests:</label> <select name="test" id="tests"><option value="---">---</value></select></p>

  <form></form>`);
  show_queryset();
  show_dataset();
  show_documentset();
  show_result();
  validate();
}


///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
// queryset

var update_queryset = function() {
  localStorage.setItem('queryset', JSON.stringify(queryset));
}

var get_queryset = function() {
  var queryset_str = localStorage.getItem('queryset');
  if(queryset_str !== null) {
    try {
      return JSON.parse(queryset_str); 
    }
    catch(err){}
  }
  queryset = [ `PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
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
}` , [] ];
  update_queryset();
  return queryset; 
}

var queryset = get_queryset() , queryset_editors, queryset_tags; // queryset and editors

var show_queryset = function() {

  $("form").append(`<fieldset id="queryset">
      <legend>SPARQL-Generate Queries</legend>
      <div id="queryset_drop_zone" class="drop_zone">
        <strong>Click here to add a new named query, you can also drag SPARQL-Generate documents to load them ...</strong>
      </div>
    </fieldset>`);

  queryset_editors = [];
  queryset_editors[1] = [];
  queryset_tags = [];
  queryset_tags[1] = [];

  // show default
  show_default_query();
  for(var i=0; i<queryset[1].length; i++) {
    show_named_query(queryset[1][i]);
  }

  $("#queryset_drop_zone")
  .click(function() {
    var nq = {
      uri: "http://example.org/query#" + queryset[1].length,
      mediatype: "application/vnd.sparql-generate",
      query: `PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
PREFIX sgfn: <http://w3id.org/sparql-generate/fn/> 

LOOK UP <> AS ?message
ITERATE sgiter:XPath( ?message ) AS ?var 
WHEREVER {  } 
CONSTRUCT {  }`
    };
    queryset[1].push(nq);
    update_queryset();
    show_named_query(nq);
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
              query: event.target.result
            };
            queryset[1].push(nq);
            update_queryset();
            show_named_query(nq);
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
          queryset[1].push(nq);
          update_queryset();
          show_named_query(nq);
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

var show_default_query = function() {
  queryset_tags[0] = $(`<fieldset id='default_query'>
      <label>Default query</label>
      <textarea></textarea>
    </fieldset>`)
  .insertBefore($("#queryset_drop_zone"));

  queryset_editors[0] = YASQE.fromTextArea($('#default_query textarea')[0], {
    createShareLink: false,
    lineNumbers: true
  });
  queryset_editors[0].setValue(queryset[0]);
  queryset_editors[0].on("change", function(){
      queryset[0] = queryset_editors[0].getValue();
      update_queryset();
      validate();
    }
  );
}

var show_named_query = function(nq) {
  var tag = $("<fieldset>")
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
      update_queryset();
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
  yasqe.setValue(nq.query);
  yasqe.on("change", function(){
    nq.query = yasqe.getValue();
    update_queryset();
    validate();
  });

  queryset_editors[1].push(yasqe);
  queryset_tags[1].push(tag);
  
  tag.find("label").next("div").hide();

  tag.find(".delete").click((function(nq, yasqe, tag) { return function() {
    if(!confirm("Permanently delete this named query?")) {
      return;
    }
    for(var i=0;i<queryset[1].length;i++) {
      if(nq == queryset[1][i]) {
        queryset[1].splice(i,1);
        queryset_editors[1].splice(i,1);
        queryset_tags[1].splice(i,1);
      }
    }
    tag.remove();
    update_queryset();
    validate();
  }})(nq, yasqe, tag));

}



///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
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

var dataset = get_dataset() , dataset_editors, dataset_tags; // dataset and editors

var show_dataset = function() {

  $("form").append(`<fieldset id="dataset">
    <legend>Dataset</legend>
    <div id="dataset_drop_zone" class="drop_zone">
      <strong>Click here to add a new graph, you can also drag turtle documents to load them ...</strong>
    </div>
  </fieldset>`);

  dataset_editors = [];
  dataset_editors[1] = [];
  dataset_tags = [];
  dataset_tags[1] = [];

  // show default
  show_default_graph();
  for(var i=0; i<dataset[1].length; i++) {
    show_named_graph(dataset[1][i]);
  }

  $("#dataset_drop_zone")
  .click(function() {
    var ng = {
      uri: "http://example.org/graph#" + dataset[1].length,
      mediatype: "text/turtle",
      graph: "<s> <p> <o> ."
    };
    dataset[1].push(ng);
    update_dataset();
    show_named_graph(ng);
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
              graph: event.target.result
            };
            dataset[1].push(ng);
            update_dataset();
            show_named_graph(ng);
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
            graph: event.target.result
          };
          dataset[1].push(ng);
          update_dataset();
          show_named_graph(ng);
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

var show_default_graph = function() {
  var tag = $(`<fieldset id='default_graph'>
      <label>Default graph (<a class='edit'>edit</a><a class='h' style='display:none'>hide</a>)</label>
      <textarea></textarea>
    </fieldset>`)
  .insertBefore($("#dataset_drop_zone"));

  dataset_tags[0] = tag;
  dataset_editors[0] = YATE.fromTextArea($('#default_graph textarea')[0], {
    createShareLink: false,
    lineNumbers: true
  });
  dataset_editors[0].setValue(dataset[0]);
  dataset_editors[0].on("change", function(){
      dataset[0] = dataset_editors[0].getValue();
      update_dataset();
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

  tag.find(".name").on('blur keyup paste', [ng,tag.find(".name")], function(event) {
      event.data[0].uri = event.data[1].text();
      update_dataset();
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
  yate.setValue(ng.graph);
  yate.on("change", function(){
    ng.graph = yate.getValue();
    update_dataset();
    validate();
  });

  dataset_editors[1].push(yate);
  dataset_tags[1].push(tag);

  tag.find("label").next("div").hide();

  tag.find(".delete").click((function(ng, yate, tag) { return function() {
    if(!confirm("Permanently delete this named graph?")) {
      return;
    }
    for(var i=0;i<dataset[1].length;i++) {
      if(ng == dataset[1][i]) {
        dataset[1].splice(i,1);
        dataset_editors[1].splice(i,1);
        dataset_tags[1].splice(i,1);
      }
    }
    tag.remove();
    update_dataset();
    validate();
  }})(ng, yate, tag));
}



///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
///////////////////////////////////////
//  documentset

var update_documentset = function() {
  localStorage.setItem('documentset', JSON.stringify(documentset));
}

var get_documentset = function() {
  var documentset_str = localStorage.getItem('documentset');
  if(documentset_str !== null) {
    try {
      return JSON.parse(documentset_str); 
    }
    catch(err){}
  }
  documentset = [];
  update_documentset();
  return documentset; 
}

var documentset = get_documentset() , documentset_editors , documentset_tags; // documentset and editors

var show_documentset = function() {
 
 $("form").append(`<fieldset id="documentset_list">
    <legend>Documentset</legend>
    <div id="documentset_drop_zone" class="drop_zone">
      <strong>Click here to add a new document, you can also drag one or more files to load them ...</strong>
    </div>
  </fieldset>`);

  documentset_editors = [];
  documentset_tags = [];

  for(var i=0; i<documentset.length; i++) {
    show_named_document(documentset[i]);
  }

  $("#documentset_drop_zone")
  .click(function() {
    var doc = {
      uri: "http://example.org/document#" + documentset.length,
      mediatype: "text/plain",
      document: ""
    };
    documentset.push(doc);
    update_documentset();
    show_named_document(doc);
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
              document: event.target.result
            };
            documentset.push(doc);
            update_documentset();
            show_named_document(doc);
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
            document: event.target.result
          };
          documentset.push(doc);
          update_documentset();
          show_named_document(doc);
        }})(i,dt.files[i]);
        reader.readAsText(dt.files[i]);
      }  
    }
  })
  .on('dragover', function(ev) {
    ev.preventDefault();
  });  
}

var show_named_document = function(doc) {
  var tag = $("<fieldset>")
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
      update_documentset();
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
  editor.setValue(doc.document);
  editor.on("change", function(){
    doc.document = editor.getValue();
    update_documentset();
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
    update_documentset();
    validate();
  };})(doc, editor, tag));


  tag.find(".media").on('blur keyup paste', (function(doc, editor, mediatype_tag) { return function() {
      doc.mediatype = mediatype_tag.text();
      var info = CodeMirror.findModeByMIME(doc.mediatype);
      if (info && info.mode) {
        editor.setOption("mode", doc.mediatype);
        CodeMirror.autoLoadMode(editor, info.mode);
      }
      update_documentset();
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

var show_result = function() {

 $("form").append(`<fieldset id="result_list">
    <legend>Result</legend>
    <textarea id="result"> </textarea>
    <fieldset id="log">
      <legend>Log</legend>
    </fieldset>
  </fieldset>`);

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
  $("#tabs").tabs("option", "active", 0);
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
        console.log(data);
        queryset = data.queryset;
        dataset = data.dataset;
        documentset = data.documentset;
        init();

          // var data = http.responseText.split("**********");
          // yasqe.setValue(data[0]);
          // data.splice(0,1);
          // var documents = new Array;
          // var j = 0;
          // for(var i = 0 ; i<data.length-1 ;i++ ) {
          //     var message = data[i].split("%%%%%%%%%%");
          //     if( !message[0].endsWith("query") && !message[0].endsWith("expected_output")) {
          //         documents[j]={"uri":message[0], "document":message[1]};
          //         j++;
          //     }
          // }
          // localStorage.setItem('documents',JSON.stringify(documents));
          // show_query();
          // show_documents();
      });
  
  $("#tabs").tabs("option", "active", 0);
}
 
$(document).ready(function() {
  init();
  load_tests();
 
         
    $("#generate").click(function() {
        var websocketurl = "wss://" + window.location.hostname + (window.location.port!="" ? ":" + window.location.port : "") + window.location.pathname + "transformStream";
 
        var exampleSocket = new WebSocket(websocketurl);
         
        exampleSocket.onopen = function (event) {
            var msg = {
                type: "message",
                query: "hello query",
                queryurl: "hello query",
                defaultGraph: " ",
                documentset: { "one" : "onevalue" , "two": "twovalue"}
            };
            exampleSocket.send(JSON.stringify(msg));
 
 
        };
         
        exampleSocket.onmessage = function (event) {
            console.log(event.data);
        }
         
        exampleSocket.onclose = function (event) {
            console.log("closed");
        }
 
         
//        var http = new XMLHttpRequest();
//        var url = "api/transform";
//        http.open("POST", url, true);
//        
//        params = "";
//        params += "query=" + encodeURIComponent(yasqe.getValue());
//        params += "&documentset=" + encodeURIComponent(JSON.stringify(get_documentset()));
//
//        //Send the proper header information along with the request
//        http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
//        http.setRequestHeader("Accept", "application/json");
//
//        http.onreadystatechange = function() {//Call a function when the state changes.
//            if(http.readyState == 4) {
//                response = JSON.parse(http.responseText);
//                console.log(response);
//                yate.setValue(response.output);
//                response.log = response.log.replace(/</g,"&lt;");
//                response.log = response.log.replace(/>/g,"&gt;");
//                
//                $("#log").html(response.log);
//            }
//        }
//        // https://github.com/thesmartenergy/sparql-generate/issues/new?title=unexpected%20output&body=urlencoded%20log%20trace
//        yate.setValue("pending result...");
//        http.send(params);
    });    
     
});