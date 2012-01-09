$(function() {
    var scrimTable = $('#scrimmage_table').dataTable({
        "bJQueryUI": true,
        "bFilter": false,
        "bSearchable": false,
        "bLengthChange": false,
        "iDisplayLength": 1000,
    });
    scrimTable.fnSort( [ [0,'asc'] ] );

    $(window).bind('resize', function () {
        scrimTable.fnAdjustColumnSizing();
    });
});

function rowClick(scrimId) {
    $('#container').remove();
    var container = $("<div/>").attr('id', 'container')
        .attr('style', 'text-align:center; width:1000px; margin-left:-10px; top:180px;')
        .addClass("overlay-contents")
        .appendTo("body");

    $("<iframe />")
        .attr("src", 'analysis_content.html?id=' + scrimId + "&scrimmage=true")
        .attr("style", "width:1000px; height:530px; border:0;")
        .appendTo(container);

    var overlay = $("<div />")
    .addClass("overlay")
    .attr("id", "overlay")
    .click(function() {
        $("#container").remove();
            $("#overlay").remove();
        $("#scrimmage_table_wrapper").show();
    })
    .appendTo("body");
    
    var buttonContainer = $("<div />")
    .appendTo(container);

    $("<button>Close</button>")
        .attr("style", "")
        .button()
        .click(function() {
            $("#container").remove();
            $("#overlay").remove();
            $("#scrimmage_table_wrapper").show();
        })
        .appendTo(buttonContainer);
    /*
    $("<button>View in full page</button>")
        .attr("style", "")
        .button()
        .click(function() {
            document.location='analysis.html?id=' + scrimId;
        })
        .appendTo(buttonContainer);
    */
    $("#scrimmage_table_wrapper").hide();
}
