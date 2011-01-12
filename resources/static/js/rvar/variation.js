/* Provide display widgets for stored variation data.
 */

// Load variations for the currently logged in user.
var user_vrn_load = function() {
  var user_name = $('.dsq-request-user-name > a').attr("href");
  if (user_name !== undefined) {
    $(".user-vrn").each(function() {
      $.get("/personal/genotype",
            {user: user_name, vrn: $(this).attr("value")},
           function(data) {
             $(this).html(data);
           });
    });
  }
}

$(document).ready(function() {
  $("#vrn-accordion").accordion();
});
