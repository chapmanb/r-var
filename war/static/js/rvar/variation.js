/* Provide display widgets for stored variation data.
 */

// External links from the variation
$("#vrn-links").find("li").button();

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
