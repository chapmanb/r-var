/* Display and interaction code for the health selection page.
 */

// Display variations associated with a particular trait.
var display_trait_vars = function(term, start, limit) {
  $("#nav-tabs").tabs('url', 1, '/health?phenotype=' + term + 
                                '&start=' + start + '&limit=' + limit);
  $("#vrn-header").html(term);
  var vrn_ol = $("#vrn-select");
  $(vrn_ol).children("li").remove();
  $.getJSON("/health/variations", 
    {phenotype: term, start: start, limit: limit},
    function(data) {
      $.each(data.variations, function(index, value) {
        $("<li class='ui-widget-content'>" + value + "</li>")
        .appendTo(vrn_ol);
      });
      $(vrn_ol).children().hover(function() {
        $(this).addClass("ui-state-hover");
      }, function() {
        $(this).removeClass("ui-state-hover");
      });
      // On click, load variation info in the appropriate tab
      $(vrn_ol).children().click(function() {
        $("#nav-tabs").tabs('url', 2, '/varview?vrn=' + $(this).html());
        $("#nav-tabs").tabs('select', 2);
      });
      if (data.hasless) {
        $("#back-page").addClass("ui-widget-content").html("<<");
      } else {
        $("#back-page").removeClass("ui-widget-content").html("");
      }
      if (data.hasmore) {
        $("#for-page").addClass("ui-widget-content").html(">>").click(
          function() {
            console.info("forward")
          });
      } else {
        $("#for-page").removeClass("ui-widget-content").html("");
      }
    }
  );
};

$(document).ready(function() {
  // Selectable list for choosing health areas to explore further
  $("#health-select").children().click(function() {
    $(this).siblings().removeClass("ui-state-highlight");
    $(this).toggleClass("ui-state-highlight");
    display_trait_vars($(this).html(), 0, 10);
  });
  $("#health-select").children().hover(function() {
    $(this).addClass("ui-state-hover");
  }, function() {
    $(this).removeClass("ui-state-hover");
  });
  var cur_phn = $("#cur-phn").attr("value");
  if (cur_phn != "") {
    display_trait_vars(cur_phn, $("#cur-start").attr("value"),
                       $("#cur-limit").attr("limit"));
  }
});
