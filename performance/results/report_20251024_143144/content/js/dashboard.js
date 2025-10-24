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

    var data = {"OkPercent": 99.33333333333333, "KoPercent": 0.6666666666666666};
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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.9611666666666666, 500, 1500, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [0.965, 500, 1500, "POST /api/goods/9/view"], "isController": false}, {"data": [0.97, 500, 1500, "POST /api/goods/1/view"], "isController": false}, {"data": [0.97, 500, 1500, "POST /api/goods/6/view"], "isController": false}, {"data": [0.965, 500, 1500, "POST /api/goods/3/view"], "isController": false}, {"data": [0.96, 500, 1500, "POST /api/goods/12/view"], "isController": false}, {"data": [0.985, 500, 1500, "POST /api/auth/login"], "isController": false}, {"data": [0.97, 500, 1500, "POST /api/goods/2/view"], "isController": false}, {"data": [0.97, 500, 1500, "POST /api/goods/5/view"], "isController": false}, {"data": [0.975, 500, 1500, "POST /api/goods/10/view"], "isController": false}, {"data": [0.945, 500, 1500, "POST /api/goods/11/view"], "isController": false}, {"data": [0.935, 500, 1500, "GET /api/goods"], "isController": false}, {"data": [0.945, 500, 1500, "POST /api/goods/15/view"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 3000, 20, 0.6666666666666666, 747.5836666666678, 2, 60017, 89.0, 332.0, 564.9499999999998, 29077.649999999667, 13.849200670301311, 13.393452869727493, 6.03810722974439], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["POST /api/goods/9/view", 100, 0, 0.0, 835.1600000000004, 3, 39585, 11.5, 83.40000000000003, 259.2999999999985, 39434.49999999993, 0.6718578886193992, 0.2568150534798879, 0.31165282919356896], "isController": false}, {"data": ["POST /api/goods/1/view", 100, 0, 0.0, 982.1900000000003, 3, 46567, 11.0, 107.80000000000001, 217.59999999999968, 46422.21999999993, 0.643297801851411, 0.2462624397712433, 0.2984047420697463], "isController": false}, {"data": ["POST /api/goods/6/view", 100, 0, 0.0, 862.9000000000003, 3, 41062, 12.0, 102.00000000000006, 185.95, 40912.22999999992, 0.6655972737135669, 0.25479895634347477, 0.30874873536517994], "isController": false}, {"data": ["POST /api/goods/3/view", 100, 0, 0.0, 925.8099999999995, 3, 44063, 12.0, 102.60000000000008, 162.74999999999972, 43913.29999999992, 0.6532062629416491, 0.24968554242248073, 0.3030009520481282], "isController": false}, {"data": ["POST /api/goods/12/view", 100, 0, 0.0, 341.4899999999998, 4, 17988, 106.0, 340.5000000000001, 581.0999999999998, 17838.359999999924, 0.6918165025908528, 0.26444415570714025, 0.32158657737621676], "isController": false}, {"data": ["POST /api/auth/login", 1000, 0, 0.0, 173.8530000000001, 66, 969, 133.0, 326.79999999999995, 415.8499999999998, 573.0, 4.616592031762153, 3.1693986311804623, 2.1638471792622687], "isController": false}, {"data": ["POST /api/goods/2/view", 100, 0, 0.0, 955.99, 3, 45566, 11.0, 108.70000000000002, 253.64999999999992, 45416.20999999992, 0.6471570391271146, 0.247556527145002, 0.3001949156107221], "isController": false}, {"data": ["POST /api/goods/5/view", 100, 0, 0.0, 896.9400000000002, 2, 42575, 14.0, 101.30000000000004, 182.54999999999967, 42424.82999999992, 0.6592827004219409, 0.25182152599222046, 0.3058196120121308], "isController": false}, {"data": ["POST /api/goods/10/view", 100, 0, 0.0, 641.2499999999999, 3, 38082, 9.0, 82.90000000000006, 192.95, 37932.10999999993, 0.6783708246275745, 0.2594967145653, 0.315336438010474], "isController": false}, {"data": ["POST /api/goods/11/view", 100, 0, 0.0, 377.66999999999973, 4, 19510, 99.5, 380.4000000000004, 732.1499999999992, 19360.009999999922, 0.6849596558762688, 0.26182315283504803, 0.31839921503623436], "isController": false}, {"data": ["GET /api/goods", 1000, 20, 2.0, 1354.0480000000016, 3, 60017, 67.0, 551.5999999999999, 833.6999999999982, 60010.99, 4.626951995373048, 8.47839701417004, 1.735106998264893], "isController": false}, {"data": ["POST /api/goods/15/view", 100, 0, 0.0, 329.10000000000014, 4, 16484, 102.0, 416.20000000000005, 817.0499999999993, 16334.299999999923, 0.6987241297390964, 0.2668866883445828, 0.3247975446834081], "isController": false}]}, function(index, item){
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
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": [{"data": ["500", 20, 100.0, 0.6666666666666666], "isController": false}]}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 3000, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": ["GET /api/goods", 1000, 20, "500", 20, "", "", "", "", "", "", "", ""], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
