/* Display for the personal page, handling genome uploads.
 */

// Hide non-login information in the disqus widget
var disqus_cleanup = function() {
  $('#dsq-global-toolbar').hide();
  $('.dsq-options').hide();
  $('#dsq-comments-title').hide();
  $('#dsq-comments').hide();
  $('#dsq-pagination').hide();
  $('#dsq-new-post').find('h3').hide();
  $('.dsq-autheneticate-copy').hide();
  $('#dsq-form-area').hide();
};

// Allow loading of personal information embedded with disqus widget
var personal_page_load = function() {
  disqus_cleanup();
  var user_name = $('.dsq-request-user-name > a').attr("href");
  if (user_name === undefined) {
    $("#personal-title").html("<h3>Please login to add personal genome information</h3>");
    $("#personal-details").html("");
  } else {
    $("#personal-title").html("");
    $.ajax({
      url: '/personal/upload',
      success: function (data) {
        $("#personal-details").html(data);
      }
    });
  }
  // Potentially useful login and logout hooks
  $('.dsq-login-button').find('a').click(function () {
  });
  $('.dsq-request-user-logout').click(function () {
  });
};

$(document).ready(function() {
  // If we missed the callback, like on disqus login,
  // reload the page with new content.
  // Ideally we'd have a different callback we could tie this to.
  setInterval(function() {
    if ($("#dsq-form-area").is(":visible"))
      personal_page_load()}, 4000);
});
