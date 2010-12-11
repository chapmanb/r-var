/* Javascript actions for the main landing page.
 */

$(document).ready(function() {
  $("#getting-started").find("a").button().click(function() {
    $("#nav-tabs").tabs("select", 1);
    return false;
  });
});
