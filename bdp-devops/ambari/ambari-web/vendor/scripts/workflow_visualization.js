/*
 * Example usage:
 *
 *  var dv = new DagViewer('pig_5')
 *  .setData(workflowData,jobsData)
 *  .drawDag(svgWidth,svgHeight,nodeHeight);
 */
function DagViewer(domId) {
  // initialize variables
  this._nodes = new Array();
  this._links = new Array();
  this._numNodes = 0;
  this._id = domId;
  this._SUCCESS = "SUCCESS";
}

// set workflow schema and job data
DagViewer.prototype.setData = function (wfData, jobData) {
  // create map from entity names to nodes
  var existingNodes = new Array();
  var jobData = (jobData) ? jobData : new Array();
  var minStartTime = 0;
  if (jobData.length > 0)
    minStartTime = jobData[0].submitTime;
  var maxFinishTime = 0;
  // iterate through job data
  for (var i = 0; i < jobData.length; i++) {
    jobData[i].info = "jobId:"+jobData[i].name+"  \n"+
      "nodeName:"+jobData[i].entityName+"  \n"+
      "status:"+jobData[i].status+"  \n"+
      "input:"+jobData[i].input+"  \n"+
      "output:"+jobData[i].output+"  \n"+
      "startTime:"+(new Date(jobData[i].submitTime).toString())+"  \n"+
      "duration:"+DagViewer.formatDuration(jobData[i].elapsedTime);
    minStartTime = Math.min(minStartTime, jobData[i].submitTime);
    maxFinishTime = Math.max(maxFinishTime, jobData[i].submitTime + jobData[i].elapsedTime);
    this._addNode(existingNodes, jobData[i].entityName, jobData[i]);
  }
  this._minStartTime = minStartTime;
  this._maxFinishTime = maxFinishTime;
  var dag = eval('(' + wfData + ')').dag;
  this._sourceMarker = new Array();
  this._targetMarker = new Array();
  this._sourceMap = new Array();
  // for each source node in the context, create links between it and its target nodes
  for (var source in dag) {
    var sourceNode = null;
    if (source in existingNodes)
      sourceNode = existingNodes[source];
    for (var i = 0; i < dag[source].length; i++) {
      var targetNode = null;
      if (dag[source][i] in existingNodes)
        targetNode = existingNodes[dag[source][i]];
      this._addLink(sourceNode, targetNode);
    }
  }
  return this;
}

// add a node to the nodes array and to a provided map of entity names to nodes
DagViewer.prototype._addNode = function (existingNodes, entityName, node) {
  existingNodes[entityName] = node;
  this._nodes.push(node);
  this._numNodes++;
}

// add a link between sourceNode and targetNode
DagViewer.prototype._addLink = function (sourceNode, targetNode) {
  // if source or target is null, add marker indicating unsubmitted job and return
  if (sourceNode==null) {
    if (targetNode==null)
      return;
    this._sourceMarker.push(targetNode);
    return;
  }
  if (targetNode==null) {
    this._targetMarker.push(sourceNode);
    return;
  }
  // add link between nodes
  var status = false;
  if (sourceNode.status==this._SUCCESS && targetNode.status==this._SUCCESS)
    status = true;
  this._links.push({"source":sourceNode, "target":targetNode, "status":status, "value":sourceNode.output});
  // add source to map of targets to sources
  if (!(targetNode.name in this._sourceMap))
    this._sourceMap[targetNode.name] = new Array();
  this._sourceMap[targetNode.name].push(sourceNode);
}

// display the graph
// rules of thumb: nodeHeight = 20, labelFontSize = 14, maxLabelWidth = 180
//                 nodeHeight = 15, labelFontSize = 10, maxLabelWidth = 120
//                 nodeHeight = 40, labelFontSize = 20, maxLabelWidth = 260
//                 nodeHeight = 30, labelFontSize = 16
DagViewer.prototype.drawDag = function (svgw, svgh, nodeHeight, labelFontSize, maxLabelWidth, axisPadding, numExtraSeries, extraSeriesSize) {
  this._addTimelineGraph(svgw, svgh, nodeHeight || 20, labelFontSize || 14, maxLabelWidth || 180, axisPadding || 30, numExtraSeries || 2, extraSeriesSize || 50);
  return this;
}

// draw timeline graph
DagViewer.prototype._addTimelineGraph = function (svgw, svgh, nodeHeight, labelFontSize, maxLabelWidth, axisPadding, numExtraSeries, extraSeriesSize) {
  svgw = svgw;
  this._extraSeriesSize = extraSeriesSize;
  
  var margin = {"vertical":10, "horizontal":50};
  this._margin = margin;
  var w = svgw - 2*margin.horizontal;

  var startTime = this._minStartTime;
  var elapsedTime = this._maxFinishTime - this._minStartTime;
  var x = d3.time.scale.utc()
    .domain([startTime, startTime+elapsedTime])
    .range([0, w]);
  this._x = x;
  var xrel = d3.time.scale()
    .domain([0, elapsedTime])
    .range([0, w]);

  // process nodes and determine their x and y positions, width and height
  var minNodeSpacing = nodeHeight/2;
  var ends = new Array();
  var maxIndex = 0;
  this._nodes = this._nodes.sort(function(a,b){return a.name.localeCompare(b.name);});
  for (var i = 0; i < this._numNodes; i++) {
    var d = this._nodes[i];
    d.x = x(d.submitTime);
    d.w = x(d.elapsedTime+d.submitTime) - x(d.submitTime);
    if (d.w < nodeHeight/2) {
      d.w = nodeHeight/2;
      if (d.x + d.w > w)
        d.x = w - d.w;
    }
    var effectiveX = d.x
    var effectiveWidth = d.w;
    if (d.w < maxLabelWidth) {
      effectiveWidth = maxLabelWidth;
      if (d.x + effectiveWidth > w)
        effectiveX = w - effectiveWidth;
      else if (d.x > 0)
        effectiveX = d.x+(d.w-maxLabelWidth)/2;
    }
    // select "lane" (slot for y-position) for this node
    // starting at the slot above the node's closest source node (or 0, if none exists)
    // and moving down until a slot is found that has no nodes within minNodeSpacing of this node
    // excluding slots that contain more than one source of this node
    var index = 0;
    var rejectIndices = new Array();
    if (d.name in this._sourceMap) {
      var sources = this._sourceMap[d.name];
      var closestSource = sources[0];
      var indices = new Array();
      for (var j = 0; j < sources.length; j++) {
        if (sources[j].index in indices)
          rejectIndices[sources[j].index] = true;
        indices[sources[j].index] = true;
        if (sources[j].submitTime + sources[j].elapsedTime > closestSource.submitTime + closestSource.elapsedTime)
          closestSource = sources[j];
      }
      index = Math.max(0, closestSource.index-1);
    }
    while ((index in ends) && ((index in rejectIndices) || (ends[index]+minNodeSpacing >= effectiveX))) {
      index++
    }
    ends[index] = Math.max(effectiveX + effectiveWidth);
    maxIndex = Math.max(maxIndex, index);
    d.y = index*2*nodeHeight + axisPadding;
    d.h = nodeHeight;
    d.index = index;
  }

  var h = 2*axisPadding + 2*nodeHeight*(maxIndex+1);
  var realh = svgh - 2*margin.vertical - numExtraSeries*extraSeriesSize;
  var scale = 1;
  if (h > realh)
    scale = realh / h;
  svgh = Math.min(svgh, h + 2*margin.vertical + numExtraSeries*extraSeriesSize);
  this._extraSeriesOffset = h + margin.vertical;
  var svg = d3.select("div#" + this._id).append("svg:svg")
    .attr("width", svgw+"px")
    .attr("height", svgh+"px");
    
  var svgg = svg.append("g")
    .attr("transform", "translate("+margin.horizontal+","+margin.vertical+") scale("+scale+")");
  this._svgg = svgg;
  // add an untranslated white rectangle below everything
  // so mouse doesn't have to be over nodes for panning/zooming
  svgg.append("svg:rect")
    .attr("x", 0)
    .attr("y", 0)
    .attr("width", svgw)
    .attr("height", svgh/scale)
    .attr("style", "fill:white;stroke:none");
 
  // create axes
  var topAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");
  var bottomAxis = d3.svg.axis()
    .scale(xrel)
    .orient("top")
    .tickFormat(function(x) { return DagViewer.formatDuration(x.getTime()); });
  svgg.append("g")
    .attr("class", "x axis top")
    .call(topAxis);
  svgg.append("g")
    .attr("class", "x axis bottom")
    .call(bottomAxis)
    .attr("transform", "translate(0,"+h+")");
  
  // create a rectangle for each node
  var success = this._SUCCESS;
  var boxes = svgg.append("svg:g").selectAll("rect")
    .data(this._nodes)
    .enter().append("svg:rect")
    .attr("x", function(d) { return d.x; } )
    .attr("y", function(d) { return d.y; } )
    .attr("width", function(d) { return d.w; } )
    .attr("height", function(d) { return d.h; } )
    .attr("class", function (d) {
      return "node " + (d.status==success ? " finished" : "");
    })
    .attr("id", function (d) {
      return d.name;
    })
    .append("title")
    .text(function(d) { return d.info; });
  
  // defs for arrowheads marked as to whether they link finished jobs or not
  svgg.append("svg:defs").selectAll("arrowmarker")
    .data(["finished", "unfinished"])
    .enter().append("svg:marker")
    .attr("id", String)
    .attr("viewBox", "0 -5 10 10")
    .attr("markerWidth", 6)
    .attr("markerHeight", 6)
    .attr("orient", "auto")
    .append("svg:path")
    .attr("d", "M0,-3L8,0L0,3");
  // defs for unsubmitted node marker
  svgg.append("svg:defs").selectAll("circlemarker")
    .data(["circle"])
    .enter().append("svg:marker")
    .attr("id", String)
    .attr("viewBox", "-2 -2 18 18")
    .attr("markerWidth", 10)
    .attr("markerHeight", 10)
    .attr("refX", 10)
    .attr("refY", 5)
    .attr("orient", "auto")
    .append("svg:circle")
    .attr("cx", 5)
    .attr("cy", 5)
    .attr("r", 5);

  // create dangling links representing unsubmitted jobs
  var markerWidth = nodeHeight/2;
  var sourceMarker = svgg.append("svg:g").selectAll("line")
    .data(this._sourceMarker)
    .enter().append("svg:line")
    .attr("x1", function(d) { return d.x - markerWidth; } )
    .attr("x2", function(d) { return d.x; } )
    .attr("y1", function(d) { return d.y; } )
    .attr("y2", function(d) { return d.y + 3; } )
    .attr("class", "source mark")
    .attr("marker-start", "url(#circle)");
  var targetMarker = svgg.append("svg:g").selectAll("line")
    .data(this._targetMarker)
    .enter().append("svg:line")
    .attr("x1", function(d) { return d.x + d.w + markerWidth; } )
    .attr("x2", function(d) { return d.x + d.w; } )
    .attr("y1", function(d) { return d.y + d.h; } )
    .attr("y2", function(d) { return d.y + d.h - 3; } )
    .attr("class", "target mark")
    .attr("marker-start", "url(#circle)");

  // create links between the nodes
  var lines = svgg.append("svg:g").selectAll("path")
    .data(this._links)
    .enter().append("svg:path")
    .attr("d", function(d) {
      var s = d.source;
      var t = d.target;
      var x1 = s.x + s.w;
      var x2 = t.x;
      var y1 = s.y;
      var y2 = t.y;
      if (y1==y2) {
        y1 += s.h/2;
        y2 += t.h/2;
      } else if (y1 < y2) {
        y1 += s.h;
      } else {
        y2 += t.h;
      }
      return "M "+x1+" "+y1+" L "+((x2+x1)/2)+" "+((y2+y1)/2)+" L "+x2+" "+y2;
    } )
    .attr("class", function (d) {
      return "link" + (d.status ? " finished" : "");
    })
    .attr("marker-mid", function (d) {
      return "url(#" + (d.status ? "finished" : "unfinished") + ")";
    });
  
  // create text group for each node label
  var text = svgg.append("svg:g").selectAll("g")
    .data(this._nodes)
    .enter().append("svg:g");
  
  // add a shadow copy of the node label (will have a lighter color and thicker
  // stroke for legibility)
  text.append("svg:text")
    .attr("x", function(d) {
      var goal = d.x + d.w/2;
      var halfLabel = maxLabelWidth/2;
      if (goal < halfLabel) return halfLabel;
      else if (goal > w-halfLabel) return w-halfLabel;
      return goal;
    } )
    .attr("y", function(d) { return d.y + d.h + labelFontSize; } )
    .attr("class", "joblabel shadow")
    .attr("style", "font: "+labelFontSize+"px sans-serif")
    .text(function (d) {
      return d.name;
    });
  
  // add the main node label
  text.append("svg:text")
    .attr("x", function(d) {
      var goal = d.x + d.w/2;
      var halfLabel = maxLabelWidth/2;
      if (goal < halfLabel) return halfLabel;
      else if (goal > w-halfLabel) return w-halfLabel;
      return goal;
    } )
    .attr("y", function(d) { return d.y + d.h + labelFontSize; } )
    .attr("class", "joblabel")
    .attr("style", "font: "+labelFontSize+"px sans-serif")
    .text(function (d) {
      return d.name;
    });

  svg.call(d3.behavior.zoom().on("zoom", function() {
    var left = Math.min(Math.max(d3.event.translate[0]+margin.horizontal, margin.horizontal-w*d3.event.scale*scale), margin.horizontal+w);
    var top = Math.min(Math.max(d3.event.translate[1]+margin.vertical, margin.vertical-h*d3.event.scale*scale), margin.vertical+h);
    svgg.attr("transform", "translate("+left+","+top+") scale("+(d3.event.scale*scale)+")");
  }));
}

DagViewer.prototype.addTimeSeries = function (series, position, name) {
  var offset = this._extraSeriesOffset + this._extraSeriesSize*position;
  var x = this._x;
  var ymax = d3.max(series, function(d) {return d3.max(d.values, function(d) { return d.y;} ) } );
  var y = d3.scale.linear()
    .domain([0, ymax])
    .range([this._extraSeriesSize - this._margin.vertical, 0]);

  var yAxis = d3.svg.axis()
    .scale(y)
    .ticks(ymax < 4 ? ymax : 4)
    .orient("left");
  
  var line = d3.svg.line()
     .interpolate("linear")
     .x(function(d) { return x(d.x*1000); } )
     .y(function(d) { return y(d.y); } );
  
  this._svgg.append("svg:g")
    .attr("class", "y axis")
    .call(yAxis)
    .attr("transform", "translate(0,"+offset+")")
    .append("text")
    .attr("transform", "rotate(-90)")
    .attr("x", -(this._extraSeriesSize - this._margin.vertical)/2)
    .attr("y", -this._margin.horizontal + 11)
    .attr("class", "axislabel")
    .text(name);

  var lines = this._svgg.append("svg:g").selectAll("path")
    .data(series)
    .enter().append("svg:path")
    .attr("d", function(d) { return line(d.values);})
    .attr("class", function(d) { return d.name;})
    .attr("style", function(d) {
      if (d.name.substring(0,3)=="all")
        return "";
      else
        return "stroke:"+d3.interpolateRgb(d.color, 'black')(0.125)+";fill:"+d.color; 
    })
    .attr("transform", "translate(0,"+offset+")");
}

DagViewer.formatDuration = function(d) {
  if (d==0) { return "0" }
  var seconds = Math.floor(parseInt(d) / 1000);
  if ( seconds < 60 )
    return seconds + "s";
  var minutes = Math.floor(seconds / 60);
  if ( minutes < 60 ) {
    var x = seconds - 60*minutes;
    return minutes + "m" + (x==0 ? "" : " " + x + "s");
  }
  var hours = Math.floor(minutes / 60);
  if ( hours < 24 ) {
    var x = minutes - 60*hours;
    return hours + "h" + (x==0 ? "" : " " + x + "m");
  }
  var days = Math.floor(hours / 24);
  if ( days < 7 ) {
    var x = hours - 24*days;
    return days + "d " + (x==0 ? "" : " " + x + "h");
  }
  var weeks = Math.floor(days / 7);
  var x = days - 7*weeks;
  return weeks + "w " + (x==0 ? "" : " " + x + "d");
};
