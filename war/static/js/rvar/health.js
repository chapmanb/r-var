/* Display and interaction code for the health selection page.
 */

// Selectable list for choosing health areas to explore further
$("#health-select").children().click(function() {
  $(this).siblings().removeClass("ui-state-highlight");
  $(this).toggleClass("ui-state-highlight");
});
$("#health-select").children().hover(function() {
  $(this).addClass("ui-state-hover");
}, function() {
  $(this).removeClass("ui-state-hover");
});
