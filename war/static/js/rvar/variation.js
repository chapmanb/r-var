/* Provide display widgets for stored variation data.
 */

$(document).ready(function() {
  $("#vrn-accordion").accordion();
  //console.info($("#vrn-links").find("li"));
  // External links from the variation
  //$("#vrn-links").find("li").button();
  //$("#vrn-pubs").find("li").hover(function() {
  //  $(this).css("cursor", "hand");
  //  $(this).addClass("ui-state-hover");
  //}, function() {
  //  $(this).css("cursor", "pointer");
  //  $(this).removeClass("ui-state-hover");
  //});
  //$("#vrn-pubs").find("li").click(function() {
  //  $(this).find("a").trigger("click");
  //});
});


// Display details on a select variance of interest
//var var_display = function (id) {
//  console.info(id);
//};
//
//// Low level grid of raw variations
//$('#var-grid').jqGrid({
//  url: 'data/variations',
//  datatype: 'json',
//  jsonReader: {repeatitems: false},
//  colNames: ['Id', 'Genotype'],
//  colModel : [
//    {name: 'id', index: 'id', width:150},
//    {name: 'genotype', index: 'genotype', width: 250}
//  ],
//  sortname: 'id',
//  sortorder: 'desc',
//  caption: 'Variations',
//  viewrecords: true,
//  onSelectRow: var_display
//});

