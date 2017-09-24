var winW = $(window).width() * .9;
var winH = $(window).height() * .9;
CodeMirror.modeURL = "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.30.0/mode/%N/%N.js";

$("#bodyColumn").empty();
$("#bodyColumn").append(`

  <h1>Try SPARQL-Generate</h1>

  <form action="api/transform" method="post" id="form" onsubmit="submit()">
    <p> Edit the SPARQL-Generate query, an optional RDF graph, and an optional DocumentSet over which it will be evaluated.</p>
    <p>See <a href="functions.html">our predefined SPARQL binding functions and SPARQL-Generate iterator functions</a>.</p>
    <p>You can also <label for="test">load one of the unit tests:</label> <select name="test" id="tests"><option value="---">---</value></select></p>

    <div id="query">
      <div>SPARQL-Generate Query:</div>
      <textarea id="queryform" name="queryform"></textarea>
    </div>

    <details id="defaultgraph_details">
      <summary>Click here to edit the RDF Default Graph in the Dataset</summary>
      <textarea id="defaultgraph"></textarea>
    </details>
    <div id="documentset">
      <div>Documentset</div>
      <fieldset id="docs">
        
      </fieldset>
      <div id="drop_zone">
        <strong>Click here to add a new document, you can also drag one or more files to load them ...</strong>
      </div>
    </details>

    <div id="result">
      <div>Result</div>
      <textarea id="result_textarea"></textarea>
      <details id="log">
        <summary>Log</summary>
      </details>
    </div>
  </form>

`);

var get_documents = function() {
    var documents = new Array;
    var documents_str = localStorage.getItem('documents');
    if (documents_str !== null) {
        documents = JSON.parse(documents_str); 
    }
    console.log(documents);
    return documents;
}

var update_documents = function() {
    localStorage.setItem('documents', JSON.stringify(documents));
}

var documents = get_documents();
var editors = new Array;
var yate;

var show_documents = function() {
  $("#docs").empty();
  editors = new Array;
  for(var i=0; i<documents.length; i++) {
    var doc = documents[i];
    if(doc.mediatype === undefined) {
      doc.mediatype = "text/plain";
    }
    var fieldset = $("<div>")
      .attr("id","doc_" + i)
      .attr("open","open")
      .append($("<div>")
        .append("Document named by URI <")
        .append(
          $("<span>")
          .attr("id", "doc_" + i + "_uri")
          .attr("placeholder", "http://ex.org/document")
          .attr("contenteditable", true)
          .text(doc.uri)
          .blur(i, function(event) {

             var i = event.data;
             documents[i].uri = $("#doc_" + event.data + "_uri").text();
             update_documents();
          }) )
        .append("> typed with media type ")
        .append(
          $("<span>")
          .attr("id", "doc_" + i + "_mediatype")
          .attr("contenteditable", true)
          .text(doc.mediatype)
          .blur(i, function(event) {
            var i = event.data;
            var mode;
            var mediatype = $("#doc_" + i + "_mediatype").text();
            var info = CodeMirror.findModeByMIME(mediatype);
            if (info && info.mode) {
              editors[i].setOption("mode", mediatype);
              CodeMirror.autoLoadMode(editors[i], info.mode);
            }
            documents[i].mediatype = mediatype;
            update_documents();
          }) )
        .append("  (")
        .append(
          $("<a>")
          .attr("id", "doc_" + i + "_edit")
          .text("edit")
          .click(i, function(event) {
            var i = event.data;
            $("#doc_" + i + "_edit").hide();
            $("#doc_" + i + "_hide").show()
            $(editors[i].getWrapperElement()).show();
          }) )
        .append(
          $("<a>")
          .attr("id", "doc_" + i + "_hide")
          .text("hide")
          .hide()
          .click(i, function(event) {
            var i = event.data;
            $("#doc_" + i + "_hide").hide()
            $(editors[i].getWrapperElement()).hide();
            $("#doc_" + i + "_edit").show();
          }) )
        .append(" - ")
        .append(
          $("<a>")
          .attr("id", "doc_" + i + "_delete")
          .text("delete")
          .click(i, function(event) {
            var i = event.data;
            documents.splice(i,1);
            editors.splice(i,1);
            update_documents();
            show_documents();
          }) )
        .append(")") ) // end of summary
      .append(
        $("<textarea>")
        .attr("id", "doc_" + i + "_document") ); // end of fieldset
    $("#docs").append(fieldset);

    console.log(doc);
    editors[i] = CodeMirror.fromTextArea($("#doc_" + i + "_document")[0], 
    {
      lineNumbers: true,
    });
    editors[i].setValue(doc.document);
    $(editors[i].getWrapperElement()).hide();
    editors[i].on("change", (function(i) {return function(){
        documents[i].document = editors[i].getValue();
        update_documents();
      }})(i));

    $("#doc_" + i + "_mediatype").trigger("change");
  }

  $("#drop_zone")
    .click(function() {
      var doc = {
        uri: "http://example.org/doc",
        mediatype: "text/plain",
        document: "edit me"
      }
      documents.push(doc);
      update_documents();
      show_documents();
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
              }
              documents.push(doc);
              update_documents();
              show_documents();
            }})(i,f);
            reader.readAsText(f);
          }
        }
      } else {
        // Use DataTransfer interface to access the file(s)
        for (var i=0; i < dt.files.length; i++) {
          reader = new FileReader();
          reader.onload = (function(i,f){return function (event) {
            console.log("... file[" + i + "].name = " + f.name, event.target.result);

          }})(i,dt.files[i]);
          reader.readAsText(dt.files[i]);
        }  
      }
    })
    .on('dragover', function(ev) {
      ev.preventDefault();
    });
}


var show_query = function() {
  var q = localStorage.getItem('query');
  if(q==null) {
    q = `PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX sgfn: <http://w3id.org/sparql-generate/fn/> 
LOOK UP <http://ci.emse.fr/sparql-generate/cities.json> AS ?message
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
    localStorage.setItem('query', q);
  }
  yasqe = YASQE.fromTextArea(document.getElementById('queryform'), {
    createShareLink: false,
    value: q,
    lineNumbers: true
  });
  yasqe.on("change", function(){localStorage.setItem('query', yasqe.getValue());});
}

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
}
  
var load_test = function(id) {
  var http = new XMLHttpRequest();
  var url = "api/list/"+id;
  http.open("GET", url, true);
  http.onreadystatechange = function() {//Call a function when the state changes.
      if(http.readyState == 4 && http.status == 200) {
          var data = http.responseText.split("**********");
          yasqe.setValue(data[0]);
          data.splice(0,1);
          var documents = new Array;
          var j = 0;
          for(var i = 0 ; i<data.length-1 ;i++ ) {
              var message = data[i].split("%%%%%%%%%%");
              if( !message[0].endsWith("query") && !message[0].endsWith("expected_output")) {
                  documents[j]={"uri":message[0], "document":message[1]};
                  j++;
              }
          }
          localStorage.setItem('documents',JSON.stringify(documents));
          show_query();
          show_documents();
      }
  }
  yate.setValue("click on \"Result\" to send the query...");
  $("#tabs").tabs("option", "active", 0);
  http.send();
}


var show_dataset = function() {
  var g = localStorage.getItem('defaultgraph');
  if(g==null) {
    g = ``;
    localStorage.setItem('defaultgraph', q);
  }
  var defaultgraph = YATE.fromTextArea(document.getElementById('defaultgraph'), {
    createShareLink: false,
    value: g,
    lineNumbers: true
  });
  defaultgraph.on("change", function(){localStorage.setItem('defaultgraph', defaultgraph.getValue());});
  $("#defaultgraph_details summary").click(function() {
    if($("#defaultgraph_details").attr("open")==="open") {
      console.log("sf", $("#defaultgraph_details summary").attr("open")==="open");
      $("#defaultgraph_details summary").text("Click here to edit the RDF Default Graph in the Dataset:");
    } else {
      console.log("sdfkjb", $("#defaultgraph_details summary").attr("open")==="open");
      $("#defaultgraph_details summary").text("Run with this RDF Default Graph in the Dataset (click to hide):");
    }
  })
}

$(document).ready(function() {
  show_query();
  show_dataset();
  show_documents();
  load_tests();

  yate = YATE.fromTextArea(document.getElementById('result_textarea'), {
    "readOnly": true, 
    "createShareLink": false});

  $("#tests").change(function() {
      test = $("#tests")[0].value;
      if(test!=="---") {
          load_test(test);
      }
  });
        
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
//        params += "&documentset=" + encodeURIComponent(JSON.stringify(get_documents()));
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

