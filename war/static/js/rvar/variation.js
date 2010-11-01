/* Provide display widgets for stored variation data.
 */

// External links from the variation
$("#vrn-links").find("li").button();

$("#vrn-phenotypes").children().click(function() {
  $(this).siblings().removeClass("ui-state-highlight");
  $(this).toggleClass("ui-state-highlight");
});
$("#vrn-phenotypes").children().hover(function() {
  $(this).addClass("ui-state-hover");
}, function() {
  $(this).removeClass("ui-state-hover");
});



// Display details on a select variance of interest
var var_display = function (id) {
  console.info(id);
};

// Low level grid of raw variations
$('#var-grid').jqGrid({
  url: 'data/variations',
  datatype: 'json',
  jsonReader: {repeatitems: false},
  colNames: ['Id', 'Genotype'],
  colModel : [
    {name: 'id', index: 'id', width:150},
    {name: 'genotype', index: 'genotype', width: 250}
  ],
  sortname: 'id',
  sortorder: 'desc',
  caption: 'Variations',
  viewrecords: true,
  onSelectRow: var_display
});
