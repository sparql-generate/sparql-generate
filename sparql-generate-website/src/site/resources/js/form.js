var winW = $(window).width() * .9;
var winH = $(window).height() * .9;


$("#bodyColumn").append(`

            <h1>SPARQL-Generate Overview</h1>
            <h2>Generate RDF from heterogeneous formats</h2>
  <p><b>Query and transform web documents in XML, JSON, CSV, HTML, CBOR, and plain text with regular expressions.</b> </p>
  <p>SPARQL-Generate is <a href="language.html">an extension of SPARQL 1.1</a> for querying not only RDF datasets but also documents in arbitrary formats. It offers a simple template-based option to generate RDF Graphs from documents, and presents the following advantages:</p>

<ul>
<li>Anyone familiar with SPARQL can easily learn SPARQL-Generate;</li>
<li>SPARQL-Generate leverages the expressivity of SPARQL 1.1: Aggregates, Solution Sequences and Modifiers, SPARQL functions and their extension mechanism.</li>  
<li>It integrates seamlessly with existing standards for consuming Semantic Web data, such as SPARQL or Semantic Web programming frameworks.</li>
</ul>

<form action="api/transform" method="post" id="form" onsubmit="submit()">
    <p><b>Try it out:</b> Edit the SPARQL-Generate query and the DocumentSet over which it will be evaluated, then click on Result to generate the RDF. You can also <label for="test">Load one of the unit tests:</label> <select name="test" id="tests"><option value="---">---</value></select>
    </p>

   <div id="tabs">
  <ul>
    <li><a href="#tabs-1">SPARQL-Generate Query</a></li>
    <li><a href="#tabs-2">DocumentSet</a></li>
    <li><a href="#tabs-3" id="generate">Result</a></li>
  </ul>
  <div id="tabs-1">
    <textarea id="queryform"></textarea>
  </div>
  <div id="tabs-2">
   
   <div id="dialog-form" title="Create new document">
    <p class="validateTips">Enter the document URL and the document text.</p>
    <fieldset>
      <label for="uri">URI</label>
      <input type="text" name="uri" id="uri" placeholder="http://ex.org/document" class="text ui-widget-content ui-corner-all">
      <label for="document">Document</label>
      <textarea name="document" id="doc" class="text ui-widget-content ui-corner-all" rows="20" columns="80" style="width:95%"></textarea>
    </fieldset>
   </div>  
   <div id="docs"></div>

   <button type="button" id="add_document_button">Add new document</button>
  </div>
  <div id="tabs-3">
     <pre id="result"></pre>
     <button id="copy" type="button" data-clipboard-target="#result">Copy to clipboard</button> 
     <button id="save" type="button" value="save">Download result as file</button>
  </div>
   </div>
  <div>
  </div>
</form>


<div class="section">
<h2><a name="Implementation"></a>Implementation</h2>
<p><b>Use SPARQL-Generate as:</b></p>

<ul>  
<li><a href="language-cli.html">an executable JAR</a>;</li>
<li><a href="get-started.html">a Java library</a> with its <a href="apidocs/index.html">reference Java documentation</a>;</li>
<li><a href="language-api.html">a Web API</a>.</li>
</ul>

<p>See <a href="functions.html">our predefined SPARQL binding functions and SPARQL-Generate iterator functions</a>. You can also leverage the SPARQL 1.1 extension mechanism and implement your own functions to support any other format.</p>

<p><b>Test, evaluate, contribute</b></p>
<p>Our <a href="tests-reports.html">tests report</a> contains tests from related work and more. You can request a new unit test, a new binding function or iterator function, via the <a href="mail-lists.html">mailing list</a> or the <a href="issue-tracking.html">issue tracker</a>. We also led <a href="evaluation.html">a comparative evaluation with the RML reference implementation</a>.</p></div>



<div class="section">
<h2><a name="Publications"></a>Publications</h2>

<blockquote>
<p>Maxime Lefran&#xe7;ois, Antoine Zimmermann, Noorani Bakerally <i>A SPARQL extension for generating RDF from heterogeneous formats</i>, In Proc. Extended Semantic Web Conference, ESWC, May 2017, Portoroz, Slovenia (long paper - <a class="externalLink" href="http://www.maxime-lefrancois.info/docs/LefrancoisZimmermannBakerally-ESWC2017-Generate.pdf">PDF</a> - <a href="LefrancoisZimmermannBakerally-ESWC2017-SPARQL.bib">BibTeX</a>)</p>
<p>Maxime Lefran&#xe7;ois, Antoine Zimmermann, Noorani Bakerally <i>Flexible RDF generation from RDF and heterogeneous data sources with SPARQL-Generate</i>, In Proc. the 20th International Conference on Knowledge Engineering and Knowledge Management, EKAW, Nov 2016, Bologna, Italy (demo track - <a class="externalLink" href="http://www.maxime-lefrancois.info/docs/LefrancoisZimmermannBakerally-EKAW2016-Flexible.pdf">PDF</a> - <a href="LefrancoisZimmermannBakerally-EKAW2016-Flexible.bib">BibTeX</a>)</p>
<p>Maxime Lefran&#xe7;ois, Antoine Zimmermann, Noorani Bakerally <i>G&#xe9;n&#xe9;ration de RDF &#xe0; partir de sources de donn&#xe9;es aux formats h&#xe9;t&#xe9;rog&#xe8;nes</i>, Actes de la 17&#xe8;me conf&#xe9;rence Extraction et Gestion des Connaissances, EGC, Jan 2017, Grenoble, France - (<a class="externalLink" href="http://www.maxime-lefrancois.info/docs/LefrancoisZimmermannBakerally-EGC2017-Generation.pdf">PDF</a> - <a href="LefrancoisZimmermannBakerally-EGC2017-Generation.bib">BibTeX</a>)</p>
</blockquote></div><div class="section">


<h2><a name="Acknowledgments"></a>Acknowledgments</h2>
<p>This work has been partly funded by the ITEA2 12004 SEAS (Smart Energy Aware Systems) project, the ANR 14-CE24-0029 OpenSensingCity project, and a bilateral research convention with ENGIE R&amp;D.</p></div>

`);


var get_documents = function() {
    var documents = new Array;
    var documents_str = localStorage.getItem('documents');
    if (documents_str !== null) {
        documents = JSON.parse(documents_str); 
    }
    return documents;
}


$(document).ready(function() {
    
    $( "#tabs" ).tabs();

    yasqe = YASQE.fromTextArea(document.getElementById('queryform'));
    yasqe._handlers.change.push(function(){localStorage.setItem('query', yasqe.getValue());});
    
    updateTips = function( t ) {
        tips = $( ".validateTips" );
        tips
          .text( t )
          .addClass( "ui-state-highlight" );
        setTimeout(function() {
          tips.removeClass( "ui-state-highlight", 1500 );
        }, 500 );
    }

    dialog = $( "#dialog-form" ).dialog({
      autoOpen: false,
      height: winH,
      width: winW,
      modal: true,
      buttons: {
        "OK": function() {
            $("#uri").removeClass( "ui-state-error" );
            var uri = $("#uri")[0].value;

            var valid = /^(?:(?:(?:coaps?|https?|ftp):)?\/\/)(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})).?)(?::\d{2,5})?(?:[/?#]\S*)?$/i.test( uri );
            if(valid){
                var documents = get_documents();
                var doc = $("#doc")[0].value;
                
                documents[id]={"uri":uri, "document":doc};
                
                localStorage.setItem('documents', JSON.stringify(documents));
                
                show_documents();
                dialog.dialog( "close" );
                return true;
            } else {
                $("#uri").addClass( "ui-state-error" );
                updateTips( "In this web interface, a document URI must be a valid URL." );
                return false;
            }
        },
        Cancel: function() {
          dialog.dialog( "close" );
        }
      },
      close: function() {
        $("#doc")[0].value="";
        $("#uri")[0].value="";

      }
    });
 
/*    form = dialog.find( "form" ).on( "submit", function( event ) {
      event.preventDefault();
      dialog.dialog( "close" );
      store_documents();
    });*/
 
    $( "#add_document_button" ).button().on( "click", function() {
        id = get_documents().length;
        $("#uri")[0].value = "";
        $("#doc")[0].value = "";
        dialog.dialog( "open" );
    });
    
    edit_document = function() {
        id = this.getAttribute('id').substring(5);
        var documents = get_documents();
        $("#uri")[0].value = documents[id].uri;
        $("#doc")[0].value = documents[id].document;
        dialog.dialog( "open" );
    };
    
    remove_document = function() {
        var id = this.getAttribute('id').substring(7);
        var documents = get_documents();
        documents.splice(id, 1);
        localStorage.setItem('documents', JSON.stringify(documents));
        show_documents();
        return false;
    };

    show_query = function() {
        var query_str = localStorage.getItem('query');
        if (query_str !== null) {
             yasqe.setValue(query_str);
        }
        if(yasqe.getValue() === "") {    
            yasqe.setValue(`PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX sgfn: <http://w3id.org/sparql-generate/fn/> 
LOOK UP <http://ci.emse.fr/sparql-generate/cities.json> AS ?message
ITERATE sgiter:JSONListKeys( ?message ) AS ?cityName 
WHEREVER { 
  FILTER( STRSTARTS( ?cityName , "New" ) ) 
  BIND( sgfn:JSONPath( ?message, """$.["{ ?cityName }"]""" ) AS  ?city )
} 
CONSTRUCT {
  ITERATE sgiter:JSONListKeys( ?city ) AS ?key  
  CONSTRUCT {
    <city/{ ?cityName }> <{ ?key }> "{ sgfn:JSONPath( ?message , "$.['{ ?cityName }']['{ ?key }']" )  }"@en . 
  } .
}`);
        }
    };

    show_documents = function() {
        var documents = get_documents();
        var html = '<ul>\n';
        for(var i=0; i<documents.length; i++) {
            html += '<li><span id="document_' + i  + '">' + documents[i].uri + '</span> <button type="button" class="edit" id="edit_' + i  + '">edit</button> <button type="button" class="remove" id="remove_' + i  + '">delete</button></li>\n';
        };
        html += '</ul>';

        document.getElementById('docs').innerHTML = html;

        var buttons = document.getElementsByClassName('remove');
        for (var i=0; i < buttons.length; i++) {
            buttons[i].addEventListener('click', remove_document);
        };
        var buttons = document.getElementsByClassName('edit');
        for (var i=0; i < buttons.length; i++) {
            buttons[i].addEventListener('click', edit_document);
        };
    };
    
    load_tests = function() {
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
        $("#result").html("refreshing...");
        http.send();
    }
    
    load_test = function(id) {
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
        $("#result").html("refreshing...");
        http.send();
    }
        
    $("#generate").click(function() {
        
        var http = new XMLHttpRequest();
        var url = "api/transform";
        http.open("POST", url, true);
        
        params = "";
        params += "query=" + encodeURIComponent(yasqe.getValue());
        params += "&documentset=" + encodeURIComponent(JSON.stringify(get_documents()));

        //Send the proper header information along with the request
        http.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        http.setRequestHeader("Accept", "text/turtle");

        http.onreadystatechange = function() {//Call a function when the state changes.
            //if(http.readyState == 4 && http.status == 200) {
            if(http.readyState == 4) {
                response = http.responseText;
                response = response.replace(/</g,"&lt;");
                response = response.replace(/>/g,"&gt;");
                $("#result").html(response);
            }
        }
        $("#result").html("refreshing...");
        http.send(params);
    });    
    
    $("#tests").change(function() {
        test = $("#tests")[0].value;
        if(test!=="---") {
            load_test(test);
        }
    });
    
    new Clipboard(document.getElementById('copy'), {
        text: function(trigger) {
            return document.getElementById('result').innerHTML;
        }
    });
    
    
    function saveTextAsFile() {
        var textToWrite = document.getElementById('result').innerHTML;
        var textFileAsBlob = new Blob([ textToWrite ], { type: 'text/turtle' });
        var fileNameToSaveAs = "result.ttl";

        var downloadLink = document.createElement("a");
        downloadLink.download = fileNameToSaveAs;
        downloadLink.innerHTML = "Download File";
        if (window.URL != null) {
          // Chrome allows the link to be clicked without actually adding it to the DOM.
          downloadLink.href = window.webkitURL.createObjectURL(textFileAsBlob);
        } else {
          // Firefox requires the link to be added to the DOM before it can be clicked.
          downloadLink.href = window.URL.createObjectURL(textFileAsBlob);
          downloadLink.onclick = destroyClickedElement;
          downloadLink.style.display = "none";
          document.body.appendChild(downloadLink);
        }

        downloadLink.click();
      }

      var button = document.getElementById('save');
      button.addEventListener('click', saveTextAsFile);

      function destroyClickedElement(event) {
        // remove the link from the DOM
        document.body.removeChild(event.target);
      }
    
    load_tests();
    show_query();
    show_documents();
});

