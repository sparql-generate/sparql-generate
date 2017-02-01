var winW = $(window).width() * .9;
var winH = $(window).height() * .9;


$("#bodyColumn").append(`
<form action="api/transform" method="post" id="form" onsubmit="submit()">
    <h1>Test SPARQL-Generate online</h1>

    <p>Edit the SPARQL-Generate query and the DocumentSet over which it will be evaluated, then click on Result to generate the RDF.</p>
    
    <label for="test">Load unit test:</label> <select name="test" id="tests"><option value="---">---</value></select>

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
  </div>
   </div>
	<div>
	</div>
</form>
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
            yasqe.setValue(`PREFIX sgfn: <http://w3id.org/sparql-generate/fn/>
PREFIX sgiter: <http://w3id.org/sparql-generate/iter/>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
GENERATE {
    ...
    GENERATE {
        ...
    }
    SOURCE <http://ex.org/message> AS ?message
    ITERATOR iter:JSONListKeys( ?message ) AS ?key 
    WHERE {
        ...
    }    
} 
SOURCE <http://ex.org/message> AS ?message
ITERATOR iter:JSONListKeys( ?message ) AS ?key 
WHERE {
    ...
}
`);
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

    
    load_tests();
    show_query();
    show_documents();
});

