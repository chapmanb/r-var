/* Display and interaction code for the health selection page.
 */

// Display variations associated with a particular trait.
var display_trait_vars = function(term) {
  $("#vrn-header").html(term);
  var vrn_ol = $("#vrn-select");
  $(vrn_ol).children("li").remove();
  $.getJSON("/health/variations", {phenotype: term}, function(data) {
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
  });
};

// Selectable list for choosing health areas to explore further
$("#health-select").children().click(function() {
  $(this).siblings().removeClass("ui-state-highlight");
  $(this).toggleClass("ui-state-highlight");
  display_trait_vars($(this).html());
});
$("#health-select").children().hover(function() {
  $(this).addClass("ui-state-hover");
}, function() {
  $(this).removeClass("ui-state-hover");
});
