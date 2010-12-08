/* Display and interaction code for the health selection page.
 */

// Display variations associated with a particular trait.
var display_trait_vars = function(term, start, limit) {
  $("#nav-tabs").tabs('url', 1, '/health?phenotype=' + term + 
                                '&start=' + start + '&limit=' + limit);
  $("#cur-phn").attr("value", term);
  $("#cur-start").attr("value", start);
  $("#cur-limit").attr("value", limit);
  $("#vrn-header").html(term);
  if (start > 0) {
    $("#vrn-header-page").html("(Page " + (start / limit + 1) + ")");
  } else {
    $("#vrn-header-page").html("");

  }
  $("#vrn-less-button").show();
  $("#vrn-more-button").show();
  var vrn_ol = $("#vrn-select");
  $(vrn_ol).children("li").remove();
  $.getJSON("/health/variations", 
    {phenotype: term, start: start, limit: limit},
    function(data) {
      $.each(data.variations, function(index, value) {
        console.info(value);
        var vrn_info = "<div class='group_vrns'>" + value.variations.join(", ")
                       + "</div>";
        $("<li class='ui-widget-content'>" + vrn_info + "</li>")
        .appendTo(vrn_ol);
      });
      $(vrn_ol).children().hover(function() {
        $(this).css("cursor", "hand");
        $(this).addClass("ui-state-hover");
      }, function() {
        $(this).css("cursor", "pointer");
        $(this).removeClass("ui-state-hover");
      });
      // On click, load variation info in the appropriate tab
      $(vrn_ol).children().click(function() {
        $("#nav-tabs").tabs('url', 2, '/varview?vrns=' + $(this).find(".group_vrns").html());
        $("#nav-tabs").tabs('select', 2);
      });
      $("#vrn-more-button").button("option", "disabled", !data.hasmore);
      $("#vrn-less-button").button("option", "disabled", !data.hasless);
    });
};
// Show variations based on our current phenotype and position
// adjust handles paging: 1 for forward, -1 for back, 0 for current
var display_cur_vrns_with_adjust = function(adjust) {
  var cur_phn = $("#cur-phn").attr("value"),
      limit = parseInt($("#cur-limit").attr("value")),
      cur_start = parseInt($("#cur-start").attr("value")) + adjust * limit;
  if (cur_phn != "") {
    display_trait_vars(cur_phn, cur_start, limit);
  }
};

$(document).ready(function() {
  // Selectable list for choosing health areas to explore further
  $("#health-select").children().click(function() {
    $(this).siblings().removeClass("ui-state-highlight");
    $(this).toggleClass("ui-state-highlight");
    display_trait_vars($(this).html(), 0, 10);
  });
  $("#vrn-less-button").button({
    icons: {primary: "ui-icon-arrowthick-1-w"},
    text: false
  }).hide().click(function() {
    display_cur_vrns_with_adjust(-1);
  });
  $("#vrn-more-button").button({
    icons: {primary: "ui-icon-arrowthick-1-e"},
    text: false
  }).hide().click(function() {
    display_cur_vrns_with_adjust(1);
  });
  $("#health-select").children().hover(function() {
    $(this).css("cursor", "hand");
    $(this).addClass("ui-state-hover");
  }, function() {
    $(this).css("cursor", "pointer");
    $(this).removeClass("ui-state-hover");
  });
  display_cur_vrns_with_adjust(0);
});
