/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 86.56666666666666, "KoPercent": 13.433333333333334};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.8306666666666667, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.77, 500, 1500, "POST /api/goods/9/view"], "isController": false}, {"data": [0.77, 500, 1500, "POST /api/goods/1/view"], "isController": false}, {"data": [0.77, 500, 1500, "POST /api/goods/6/view"], "isController": false}, {"data": [0.765, 500, 1500, "POST /api/goods/3/view"], "isController": false}, {"data": [0.775, 500, 1500, "POST /api/goods/12/view"], "isController": false}, {"data": [0.988, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.765, 500, 1500, "POST /api/goods/2/view"], "isController": false}, {"data": [0.77, 500, 1500, "POST /api/goods/5/view"], "isController": false}, {"data": [0.78, 500, 1500, "POST /api/goods/10/view"], "isController": false}, {"data": [0.78, 500, 1500, "POST /api/goods/11/view"], "isController": false}, {"data": [0.7335, 500, 1500, "GET /api/goods"], "isController": false}, {"data": [0.76, 500, 1500, "POST /api/goods/15/view"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 3000, 403, 13.433333333333334, 8568.432333333367, 3, 61534, 111.0, 60007.0, 60014.0, 60020.99, 2.307078269937386, 2.04709413037953, 0.8151376152962365], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["POST /api/goods/9/view", 100, 20, 20.0, 12540.880000000005, 3, 60021, 39.5, 60010.0, 60016.0, 60020.98, 0.08113122895151678, 0.032373261864833754, 0.03763411499215867], "isController": false}, {"data": ["POST /api/goods/1/view", 100, 20, 20.0, 12561.849999999991, 3, 61534, 21.5, 60013.8, 60016.95, 61518.99999999999, 0.08073990044770274, 0.032217112620050135, 0.037452590539705864], "isController": false}, {"data": ["POST /api/goods/6/view", 100, 20, 20.0, 12534.57999999999, 3, 60065, 28.0, 60008.9, 60016.0, 60064.56, 0.08104673478914882, 0.03231659402515367, 0.037594920922700864], "isController": false}, {"data": ["POST /api/goods/3/view", 100, 20, 20.0, 12555.509999999986, 4, 60224, 40.5, 60013.9, 60017.95, 60221.96, 0.08085028629086376, 0.03223826210490486, 0.03750379491031278], "isController": false}, {"data": ["POST /api/goods/12/view", 100, 20, 20.0, 12131.700000000008, 8, 60057, 159.5, 60007.9, 60015.8, 60056.76, 0.08141505885494604, 0.03248651664856539, 0.037845281264603826], "isController": false}, {"data": ["POST /api/auth/login", 1000, 0, 0.0, 144.9059999999998, 65, 1247, 110.5, 250.89999999999998, 332.7999999999997, 576.99, 0.769078728282178, 0.5277509861992667, 0.16973807870290258], "isController": false}, {"data": ["POST /api/goods/2/view", 100, 20, 20.0, 12557.700000000006, 4, 61219, 30.0, 60012.0, 60016.95, 61207.009999999995, 0.08075744018296405, 0.03220124062608013, 0.03746072664737102], "isController": false}, {"data": ["POST /api/goods/5/view", 100, 20, 20.0, 12537.160000000003, 3, 60024, 27.5, 60012.8, 60018.85, 60023.97, 0.08094793267074753, 0.03227719764530559, 0.037549089861919016], "isController": false}, {"data": ["POST /api/goods/10/view", 100, 20, 20.0, 12370.6, 3, 60021, 19.5, 60008.9, 60013.0, 60020.95, 0.08122559678476599, 0.03241091684204627, 0.03775721100541856], "isController": false}, {"data": ["POST /api/goods/11/view", 100, 20, 20.0, 12134.629999999996, 5, 60075, 155.0, 60014.0, 60020.0, 60074.68, 0.08132038279130586, 0.03244873868020272, 0.03780127168814609], "isController": false}, {"data": ["GET /api/goods", 1000, 203, 20.3, 13150.599000000013, 4, 60366, 145.5, 60010.0, 60015.0, 60027.99, 0.7695024627926321, 1.213358096243212, 0.2885634235472371], "isController": false}, {"data": ["POST /api/goods/15/view", 100, 20, 20.0, 12173.31, 13, 60026, 189.0, 60014.0, 60018.95, 60025.96, 0.0814996589239274, 0.032520274058903065, 0.03788460707791938], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 403, 100.0, 13.433333333333334], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 3000, 403, "500", 403, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": ["POST /api/goods/9/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/1/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/6/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/3/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/12/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}, {"data": ["POST /api/goods/2/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/5/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/10/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/11/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["GET /api/goods", 1000, 203, "500", 203, "", "", "", "", "", "", "", ""], "isController": false}, {"data": ["POST /api/goods/15/view", 100, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
